import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;


public class Layer1Node extends Thread{
	public int id;
	
	public int [] values;
	
	public boolean debug;

	
	public Queue shared = new LinkedList<>();
	Semaphore getMessageSemaphore = new Semaphore(0);
	
	public Socket sock; //forward/listen socket
	public PrintWriter stdOut;
	public BufferedReader stdIn;
	
	public Socket sockLayer2_1; //forward/listen socket
	public PrintWriter stdOutLayer2_1;
	public BufferedReader stdInLayer2_1;
	
	public Socket sockLayer2_2; //forward/listen socket
	public PrintWriter stdOutLayer2_2;
	public BufferedReader stdInLayer2_2;
	
	
	public Socket sockClient; //Client socket
	public PrintWriter stdOutClient;
	public BufferedReader stdInClient;
	
	Semaphore fileMutex = new Semaphore(1);
	
	public LinkedList<String> updatesList;
	
	public int updates =0 ;

	


	
	public Layer1Node(int id) { 
		this.id = id;
		updatesList = new LinkedList<>();
		this.values = new int [100];

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
			FileReader fileReader = new FileReader("Layer1Values"+id+".log");
			
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
		if (debug) System.out.println("[DEBUG] Layer1Node " + id + " starting initConfig()");
		
		switch (id) {
		case 1:
			try{
				ServerSocket serverSock = new ServerSocket(6904);
				if (debug) System.out.println("[DEBUG] Layer1Node " + id + " listening...");
				sock = serverSock.accept();
				if (debug) System.out.println("[DEBUG] Layer1Node " + id + " connected");

				stdOut = new PrintWriter(sock.getOutputStream(), true);
				stdIn = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				
				serverSock.close(); //????
				
				configSockClient();
				
			}catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
			
		case 2:
			try{
				ServerSocket serverSock = new ServerSocket(6905);
				if (debug) System.out.println("[DEBUG] Layer1Node " + id + " listening...");
				sock = serverSock.accept();
				if (debug) System.out.println("[DEBUG] Layer1Node " + id + " connected");

				stdOut = new PrintWriter(sock.getOutputStream(), true);
				stdIn = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				
				serverSock.close(); //????
				
				configSockLayer2();
				
				configSockClient();

				
			}catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
			
		default:
			System.out.println("[ERROR] Unknown id process " + id);
			break;
		}
	}	
	
	public void configSockLayer2(){
		try {
			if (debug) System.out.println("[DEBUG] Layer1 " + id + " connecting to Layer2Node...");
			sockLayer2_1 = new Socket("127.0.0.1", 6906);
			sockLayer2_2 = new Socket("127.0.0.1", 6907);
			
			stdOutLayer2_1 = new PrintWriter(sockLayer2_1.getOutputStream(), true);
			stdInLayer2_1 = new BufferedReader(new InputStreamReader(sockLayer2_1.getInputStream()));
			stdOutLayer2_2 = new PrintWriter(sockLayer2_2.getOutputStream(), true);
			stdInLayer2_2 = new BufferedReader(new InputStreamReader(sockLayer2_2.getInputStream()));
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
		int idClient = 3+id;
		try{
			ServerSocket serverSock = new ServerSocket(9600+idClient);
			if (debug) System.out.println("[DEBUG] Layer1Node " + idClient + " listening to sockClient...");
			sockClient = serverSock.accept();
			if (debug) System.out.println("[DEBUG] Layer1Node " + idClient + " connected to sockClient");

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
		
		if (debug) System.out.println("[DEBUG] Layer1Node " + id + " initializing stdListeners");
		
		
		StdListener stdListener1 = new StdListener(stdIn, shared, getMessageSemaphore, id, 1);
		stdListener1.start();
		/*if (id != 1){
			StdListener stdListenerLayer1 = new StdListener(stdInLayer1, shared, getMessageSemaphore, id, 3);
			stdListenerLayer1.start();
		}*/
		StdListener stdListenerClient = new StdListener(stdInClient, shared, getMessageSemaphore, id, 4);
		stdListenerClient.start();
		
		if (id == 2){
			StdListener stdListenerLayer2_1 = new StdListener(stdInLayer2_1, shared, getMessageSemaphore, id, 1);
			stdListenerLayer2_1.start();
			
			StdListener stdListenerLayer2_2 = new StdListener(stdInLayer2_2, shared, getMessageSemaphore, id, 1);
			stdListenerLayer2_2.start();
		}
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
			
		if (debug) System.out.println("[DEBUG] Layer1Node " + id + " waiting...");
		
		try {
			getMessageSemaphore.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		synchronized (shared) {
			recievedMessage = (String) shared.poll();
		}
		
		if (debug) System.out.println("[DEBUG] Layer1Node " + id + " recieved message: "+recievedMessage);

		return recievedMessage;
	}
	
	
	private void processMessage (String message){
		String [] parts;
		String response;
		int i=0;
		
	
		if (message.substring(0, 1).equals("b")){
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

						if (debug) System.out.println("[DEBUG] Layer1Node "+id+" writing->  key:"+partsWrite[0]+" value:"+partsWrite[1]);
						
						values[Integer.parseInt(partsWrite[0])] = Integer.parseInt(partsWrite[1]);
						
						updatesList.add(Integer.parseInt(partsWrite[0])+":"+Integer.parseInt(partsWrite[1]));
						i++;
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
			}else{
				//read frame
				parts = message.split(",");
				i++;
				response = "";
				
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
		}else if (message.substring(0, 1).equals("r")){
			sendUpdate(message.substring(1, 2));
		}
	}	
	
	public void sendUpdate(String layer2Node){
		if (updates == 1){
			String frameString = "b,";
			while (!updatesList.isEmpty()){
				frameString += "w("+ updatesList.removeFirst()+"),";
			}
			frameString += "c";
			
			if (layer2Node.equals("1") ){
				stdOutLayer2_1.println(frameString);
			}else{
				stdOutLayer2_2.println(frameString);
			}
			updates = 0;
		}else{
			
			String frameString = "b,";
			for (int i = 0; i< updatesList.size(); i++){
				frameString += "w("+ updatesList.get(i)+"),";
			}
			frameString += "c";
			
			if (layer2Node.equals("1") ){
				stdOutLayer2_1.println(frameString);
			}else{
				stdOutLayer2_2.println(frameString);
			}
			
			updates ++;
		}
	}
	
	public void writeFile(){	
		if (debug) System.out.println("[DEBUG] Layer1Node "+id+" writing file...");

		
		PrintWriter writer;
		try {
			writer = new PrintWriter("Layer1Values"+id+".log", "UTF-8");
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
}
