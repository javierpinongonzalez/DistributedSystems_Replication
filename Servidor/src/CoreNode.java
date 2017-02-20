import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;


public class CoreNode extends Thread{
	public int id;
	public LamportClock ts;
	public ArrayList<LamportQueueNode> q;
	
	public int [] values;
	
	public boolean debug;
	
	public Queue shared = new LinkedList<>();
	Semaphore getMessageSemaphore = new Semaphore(0);
	
	//Core communication
	public Socket sock1; //forward/listen socket
	public Socket sock2; //backward/talk socket
	public PrintWriter stdOut1;
	public PrintWriter stdOut2;
	public BufferedReader stdIn1;
	public BufferedReader stdIn2;
	
	//Layer1 communication
	public Socket sockLayer1; //forward/listen socket
	public PrintWriter stdOutLayer1;
	public BufferedReader stdInLayer1;

	
	//Client communication
	public Socket sockClient; //Client socket
	public PrintWriter stdOutClient;
	public BufferedReader stdInClient;
	
	public int replyCounter=0;
	Semaphore fileMutex = new Semaphore(1);
	public int totalUpdates=0;
	public LinkedList<String> updatesList;


	
	public CoreNode (int id, LamportClock ts){
		this.id = id;
		this.ts = ts;
		this.q = new ArrayList<LamportQueueNode>();
		this.values = new int [100];
		this.updatesList = new LinkedList<>();
	}
	
	public void run(){
		initValues();
		
		initConfig();
		
		initStdInListeners();
		
		doIterations();
		
		//closeConfig();
		//System.out.println("Bye from thread num: "+id);
	}
	
	private void initValues(){
		String line;
		int i = 0;
		
		try {
			FileReader fileReader = new FileReader("CoreValues"+id+".log");
			
			// Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader = 
                new BufferedReader(fileReader);

            while((line = bufferedReader.readLine()) != null) {
            	values[i] = Integer.parseInt(line);
            	i++;
            }
            
         // Always close files.
            bufferedReader.close(); 
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch(IOException e) {
            System.out.println("Error reading file 'executableCommands'");                  
            // Or we could just do this: 
            // ex.printStackTrace();
        }
	}

	
	private void initConfig(){
		if (debug) System.out.println("[DEBUG] CoreNode " + id + " starting initConfig()");
		try {
			sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		switch (id) {
		case 1:
			configSock1();
			try {
				sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			configSock2();
			configSockClient();
			break;
			
		case 2:
			configSock2();
			configSock1();
			configSockLayer1();
			configSockClient();
			break;
			
		case 3:
			configSock2();
			configSock1();
			configSockLayer1();
			configSockClient();
			break;
			
		default:
			System.out.println("[ERROR] Unknown id process " + id);
			break;
		}
	}	
	private void configSock1(){
		//Listen
		
		try{
			ServerSocket serverSock = new ServerSocket(6900+id);
			if (debug) System.out.println("[DEBUG] CoreNode " + id + " listening...");
			sock1 = serverSock.accept();
			if (debug) System.out.println("[DEBUG] CoreNode " + id + " connected");

			stdOut1 = new PrintWriter(sock1.getOutputStream(), true);
			stdIn1 = new BufferedReader(new InputStreamReader(sock1.getInputStream()));
			
			serverSock.close(); //????
			
		}catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void configSock2(){
		//Connect
		try {
			if (debug) System.out.println("[DEBUG] CoreNode " + id + " connecting...");
			
			if (id == 1)
			{
				sock2 = new Socket("127.0.0.1", 6903);
			}else {
				sock2 = new Socket("127.0.0.1", 6900+id-1);
			}
			
			stdOut2 = new PrintWriter(sock2.getOutputStream(), true);
			stdIn2 = new BufferedReader(new InputStreamReader(sock2.getInputStream()));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void configSockLayer1(){
		//Connect
		try {
			if (debug) System.out.println("[DEBUG] CoreNode " + id + " connecting to Layer1...");
			
			if (id == 2)
			{
				sockLayer1 = new Socket("127.0.0.1", 6904);
			}else if (id == 3){
				sockLayer1 = new Socket("127.0.0.1", 6905);
			}
			
			stdOutLayer1 = new PrintWriter(sockLayer1.getOutputStream(), true);
			stdInLayer1 = new BufferedReader(new InputStreamReader(sockLayer1.getInputStream()));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void configSockClient(){
		//Listen
		
		try{
			ServerSocket serverSock = new ServerSocket(9600+id);
			if (debug) System.out.println("[DEBUG] CoreNode " + id + " listening to sockClient...");
			sockClient = serverSock.accept();
			if (debug) System.out.println("[DEBUG] CoreNode " + id + " connected to sockClient");

			stdOutClient = new PrintWriter(sockClient.getOutputStream(), true);
			stdInClient = new BufferedReader(new InputStreamReader(sockClient.getInputStream()));
			
			serverSock.close(); //????
			
		}catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private void initStdInListeners(){
		try {
			sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (debug) System.out.println("[DEBUG] CoreNode " + id + " initializing stdListeners");
		
		
		StdListener stdListener1 = new StdListener(stdIn1, shared, getMessageSemaphore, id, 1);
		stdListener1.start();
		StdListener stdListener2 = new StdListener(stdIn2, shared, getMessageSemaphore, id, 2);
		stdListener2.start();
		/*if (id != 1){
			StdListener stdListenerLayer1 = new StdListener(stdInLayer1, shared, getMessageSemaphore, id, 3);
			stdListenerLayer1.start();
		}*/
		StdListener stdListenerClient = new StdListener(stdInClient, shared, getMessageSemaphore, id, 4);
		stdListenerClient.start();
	}
	
	private void doIterations(){
		String message;
		
		//stdOut1.println("RELEASE");
		//stdOut2.println("RELEASE");
		while (true){	
			//getMessage
			message = getMessage();
			//processMessage
			processMessage(message);
				
		}
		
		
	}
	
	private String getMessage(){
		String recievedMessage = "initialValue";
			
		if (debug) System.out.println("[DEBUG] CoreNode " + id + " waiting...");
		
		try {
			getMessageSemaphore.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		synchronized (shared) {
			recievedMessage = (String) shared.poll();
		}
		
		if (debug) System.out.println("[DEBUG] CoreNode " + id + " recieved message: "+recievedMessage);

		return recievedMessage;
	}
	
	private void processMessage (String message){
		String[] parts;
		int i = 0;
		String response = "";
		String update="u,";
		
		if (message.substring(0, 1).equals("u")){
			//Update
			parts = message.split(",");
			
			
			i++;
			while (!parts[i].equals("c"))
			{
				String[] partsWrite;
				
				partsWrite = parts[i].split(":");
				partsWrite[0] = partsWrite[0].replaceAll("w", "");
				partsWrite[0] = partsWrite[0].replaceAll("[()]", "");
				partsWrite[1] = partsWrite[1].replaceAll("[()]", "");

				if (debug) System.out.println("[DEBUG] CoreNode "+id+" writing->  key:"+partsWrite[0]+" value:"+partsWrite[1]);
				
				values[Integer.parseInt(partsWrite[0])] = Integer.parseInt(partsWrite[1]);
				
				i++;
				
				totalUpdates ++;
				updatesList.add(Integer.parseInt(partsWrite[0])+":"+Integer.parseInt(partsWrite[1]));
				
				if(totalUpdates >= 10){
					sendUpdate();
					totalUpdates = 0;
				}
			}
			try {
				fileMutex.acquire();
				writeFile();
				fileMutex.release();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}else if (message.substring(0, 1).equals("b")){
			//Read or Write
			parts = message.split(",");
			
			if (parts[0].equals("b")){
				//write frame
				i++;
				while (!parts[i].equals("c"))
				{
					if (parts[i].substring(0, 1).equals("w")){
						//write
						String[] partsWrite;
						
						partsWrite = parts[i].split(":");
						partsWrite[0] = partsWrite[0].replaceAll("w", "");
						partsWrite[0] = partsWrite[0].replaceAll("[()]", "");
						partsWrite[1] = partsWrite[1].replaceAll("[()]", "");

						if (debug) System.out.println("[DEBUG] CoreNode "+id+" writing->  key:"+partsWrite[0]+" value:"+partsWrite[1]);
						
						values[Integer.parseInt(partsWrite[0])] = Integer.parseInt(partsWrite[1]);
						update += "w("+ partsWrite[0] + ":" + partsWrite[1] + "),";
						
						totalUpdates ++;
						updatesList.add(Integer.parseInt(partsWrite[0])+":"+Integer.parseInt(partsWrite[1]));
						
						//send update
						if(totalUpdates >= 10){
							sendUpdate();
							totalUpdates = 0;
						}
						
					}else{
						//read
						parts[i] = parts[i].replaceAll("r", "");
						parts[i] = parts[i].replaceAll("[()]", "");
						//parts[i] = parts[i].replaceAll("//)", "");
				
						if(response.equals("")){
							response += Integer.parseInt(parts[i])+":"+values[Integer.parseInt(parts[i])];
						}else{
							response += ","+Integer.parseInt(parts[i])+":"+values[Integer.parseInt(parts[i])];
						}
					}
					i++;
				}
					
				update += "c";
				
				//SEND REQUEST
				requestBroadcast();
				
				//WAIT REPLY
				waitReplies();
				
				//SEND UPDATE
				updateBroadcast(update);
				
				//SEND RELEASE
				releaseBroadcast();
				
				try {
					fileMutex.acquire();
					writeFile();
					fileMutex.release();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				writeFile();
				
				stdOutClient.println(response);
				
			}else{
				//read frame
				
				i++;
				
				while (!parts[i].equals("c"))
				{
					//read
					
					parts[i] = parts[i].replaceAll("r", "");
					parts[i] = parts[i].replaceAll("[()]", "");
					//parts[i] = parts[i].replaceAll("//)", "");
			
					if(response.equals("")){
						response += Integer.parseInt(parts[i])+":"+values[Integer.parseInt(parts[i])];
					}else{
						response += ","+Integer.parseInt(parts[i])+":"+values[Integer.parseInt(parts[i])];
					}
					
					i++;
				}
				
				stdOutClient.println(response);
			}
			
		}else{
			//Lamport message
			processLamportMessage(message);
		}
	}
	
	public void releaseBroadcast(){
		ts.sendAction();
		stdOut1.println("RELEASE-"+ts.ticks+"-"+this.id);
		stdOut2.println("RELEASE-"+ts.ticks+"-"+this.id);
	}
	
	public void updateBroadcast(String update){
		q.remove(0);
		replyCounter = 0;
		
		stdOut1.println(update);
		stdOut2.println(update);
		
		if (debug) System.out.println("[DEBUG] CoreNode "+id+" sending update frame: "+update);

	}
	
	public void requestBroadcast(){
		ts.sendAction();
		stdOut1.println("REQUEST-"+ts.ticks+"-"+id);
		stdOut2.println("REQUEST-"+ts.ticks+"-"+id);
		q.add(new LamportQueueNode(ts.ticks, id));
	}
	
	public void waitReplies(){
		String msg1= "initValue";
		String msg2 = "initValue"; 
		String [] partsMsg1;
		String [] partsMsg2;
		

		
		if (debug) System.out.println("[DEBUG] CoreNode " + this.id + " waiting for REPLY ");
		msg2 = getMessage();
		if (debug) System.out.println("[DEBUG] CoreNode " + this.id + " waiting for REPLY ");
		msg1 = getMessage();
		
		partsMsg1 = msg1.split("-");
		partsMsg2 = msg2.split("-");
		ts.receiveAction(Integer.parseInt(partsMsg1[1]));
		if (partsMsg1[0].equals("REPLY")) replyCounter++;
		ts.receiveAction(Integer.parseInt(partsMsg2[1]));
		if (partsMsg2[0].equals("REPLY")) replyCounter++;
	}
	
	private void processLamportMessage(String message){
		String[] parts;
		
		parts = message.split("-");
		
		ts.receiveAction(Integer.parseInt(parts[1]));
		
		if (parts[0].equals("REQUEST")){
			//DO REQUEST
			q.add(new LamportQueueNode(Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));			
			Collections.sort(q);
			
			//send REPLY
			sendReply(Integer.parseInt(parts[2]));
		}else if (parts[0].equals("RELEASE")){
			//DO RELEASE
			q.remove(0);
		}
		//if (debug) System.out.println("[DEBUG] Process " + id + " Parts[0] "+parts[0]);
	}
	private void sendReply(int id){
		//ts.sendAction();
		switch (this.id){
			case 1:
				if (id == 2){
					stdOut1.println("REPLY-"+ts.ticks+"-"+this.id);
					if (debug) System.out.println("[DEBUG] CoreNode " + 1 + " sending REPLY to " +2);

				}else{ //id == 3
					stdOut2.println("REPLY-"+ts.ticks+"-"+this.id);
					if (debug) System.out.println("[DEBUG] CoreNode " + 1 + " sending REPLY to " +3);

				}
				break;
			case 2:
				if (id == 1){
					stdOut2.println("REPLY-"+ts.ticks+"-"+this.id);
					if (debug) System.out.println("[DEBUG] CoreNode " + 2 + " sending REPLY to " +1);

				}else{ //id == 3
					stdOut1.println("REPLY-"+ts.ticks+"-"+this.id);
					if (debug) System.out.println("[DEBUG] CoreNode " + 2 + " sending REPLY to " +3);

				}
				break;
			case 3:
				if (id == 1){
					stdOut1.println("REPLY-"+ts.ticks+"-"+this.id);
					if (debug) System.out.println("[DEBUG] CoreNode " + 3+ " sending REPLY to " +1);

				}else{ //id == 2
					stdOut2.println("REPLY-"+ts.ticks+"-"+this.id);
					if (debug) System.out.println("[DEBUG] CoreNode " + 3 + " sending REPLY to " +2);

				}
				break;
		}
	}
	
	public void writeFile(){	
		if (debug) System.out.println("[DEBUG] CoreNode "+id+" writing file...");

		
		PrintWriter writer;
		try {
			writer = new PrintWriter("CoreValues"+id+".log", "UTF-8");
			for (int i = 0; i<100; i++){
				writer.println(values[i]);
			}
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void sendUpdate(){
		if(id != 1){
			String frameString = "b,";
			while (!updatesList.isEmpty()){
				frameString += "w("+ updatesList.removeFirst()+"),";
			}
			frameString += "c";
			
			stdOutLayer1.println(frameString);
		}
	}
}
