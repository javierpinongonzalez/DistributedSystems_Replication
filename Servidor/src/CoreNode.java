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


/*
 *
 * Classe que implementa els nodes core
 *
 */
public class CoreNode extends Thread{
	public int id;
	public LamportClock ts;
	public ArrayList<LamportQueueNode> q;
	
	public int [] values;
	
	public boolean debug;
	
	public Queue shared = new LinkedList<>();
	Semaphore getMessageSemaphore = new Semaphore(0);
	

	public Socket sock1; 
	public Socket sock2; 
	public PrintWriter stdOut1;
	public PrintWriter stdOut2;
	public BufferedReader stdIn1;
	public BufferedReader stdIn2;
	
	public Socket sockLayer1; 
	public PrintWriter stdOutLayer1;
	public BufferedReader stdInLayer1;

	
	public Socket sockClient; 
	public PrintWriter stdOutClient;
	public BufferedReader stdInClient;
	
	public int replyCounter=0;
	Semaphore fileMutex = new Semaphore(1);
	public int totalUpdates=0;
	public LinkedList<String> updatesList;


	/*
	 *
	 * Constructor
	 *
	 */
	public CoreNode (int id, LamportClock ts){
		this.id = id;
		this.ts = ts;
		this.q = new ArrayList<LamportQueueNode>();
		this.values = new int [100];
		this.updatesList = new LinkedList<>();
	}
	

	/*
	 *
	 * Sobreescriu el metode Run de la classe Thread
	 *
	 */
	public void run(){
		initValues();
		
		initConfig();
		
		initStdInListeners();
		
		doIterations();
	}
	
	/*
	 *
	 * Inicialitza els valors de la capa core
	 *
	 */
	private void initValues(){
		String line;
		int i = 0;
		
		try {
			FileReader fileReader = new FileReader("CoreValues"+id+".log");
			
            BufferedReader bufferedReader = 
                new BufferedReader(fileReader);

            while((line = bufferedReader.readLine()) != null) {
            	values[i] = Integer.parseInt(line);
            	i++;
            }
            
            bufferedReader.close(); 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}catch(IOException e) {
            System.out.println("Error reading file 'executableCommands'");                  
        }
	}

	/*
	 *
	 * Inicialitza la configuració dels sockets
	 *
	 */
	private void initConfig(){
		if (debug) System.out.println("[DEBUG] CoreNode " + id + " starting initConfig()");
		try {
			sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		switch (id) {
		case 1:
			configSock1();
			try {
				sleep(1000);
			} catch (InterruptedException e) {
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

	/*
	 *
	 * Configura el socket amb un altre node de la capa core
	 *
	 */	
	private void configSock1(){	
		try{
			ServerSocket serverSock = new ServerSocket(6900+id);
			if (debug) System.out.println("[DEBUG] CoreNode " + id + " listening...");
			sock1 = serverSock.accept();
			if (debug) System.out.println("[DEBUG] CoreNode " + id + " connected");

			stdOut1 = new PrintWriter(sock1.getOutputStream(), true);
			stdIn1 = new BufferedReader(new InputStreamReader(sock1.getInputStream()));
			
			serverSock.close();
		}catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 *
	 * Configura el socket amb un altre node de la capa core
	 *
	 */	
	private void configSock2(){
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
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 *
	 * Configura el socket amb un node de la capa 1
	 *
	 */	
	private void configSockLayer1(){
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
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 *
	 * Configura el socket amb el client
	 *
	 */	
	private void configSockClient(){
		
		try{
			ServerSocket serverSock = new ServerSocket(9600+id);
			if (debug) System.out.println("[DEBUG] CoreNode " + id + " listening to sockClient...");
			sockClient = serverSock.accept();
			if (debug) System.out.println("[DEBUG] CoreNode " + id + " connected to sockClient");

			stdOutClient = new PrintWriter(sockClient.getOutputStream(), true);
			stdInClient = new BufferedReader(new InputStreamReader(sockClient.getInputStream()));
			
			serverSock.close();
			
		}catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 *
	 * Inicialitza els sockets listeners per evitar congelar el thread principal
	 *
	 */
	private void initStdInListeners(){
		try {
			sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if (debug) System.out.println("[DEBUG] CoreNode " + id + " initializing stdListeners");
		
		
		StdListener stdListener1 = new StdListener(stdIn1, shared, getMessageSemaphore, id, 1);
		stdListener1.start();
		StdListener stdListener2 = new StdListener(stdIn2, shared, getMessageSemaphore, id, 2);
		stdListener2.start();
		StdListener stdListenerClient = new StdListener(stdInClient, shared, getMessageSemaphore, id, 4);
		stdListenerClient.start();
	}
	

	/*
	 *
	 * Bucle infinit, obté missatge i processa missatge
	 *
	 */
	private void doIterations(){
		String message;

		while (true){	
			message = getMessage();
			processMessage(message);
				
		}
	}
	
	/*
	 *
	 * Obté missatge del socket
	 *
	 */
	private String getMessage(){
		String recievedMessage = "initialValue";
			
		if (debug) System.out.println("[DEBUG] CoreNode " + id + " waiting...");
		
		try {
			getMessageSemaphore.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		synchronized (shared) {
			recievedMessage = (String) shared.poll();
		}
		
		if (debug) System.out.println("[DEBUG] CoreNode " + id + " recieved message: "+recievedMessage);

		return recievedMessage;
	}
	

	/*
	 *
	 * Processa el missatge rebut pel socket
	 *
	 */
	private void processMessage (String message){
		String[] parts;
		int i = 0;
		String response = "";
		String update="u,";
		
		if (message.substring(0, 1).equals("u")){
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
				e.printStackTrace();
			}
			
			
		}else if (message.substring(0, 1).equals("b")){
			parts = message.split(",");
			
			if (parts[0].equals("b")){
				i++;
				while (!parts[i].equals("c"))
				{
					if (parts[i].substring(0, 1).equals("w")){
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
						
						if(totalUpdates >= 10){
							sendUpdate();
							totalUpdates = 0;
						}
						
					}else{
						parts[i] = parts[i].replaceAll("r", "");
						parts[i] = parts[i].replaceAll("[()]", "");
				
						if(response.equals("")){
							response += Integer.parseInt(parts[i])+":"+values[Integer.parseInt(parts[i])];
						}else{
							response += ","+Integer.parseInt(parts[i])+":"+values[Integer.parseInt(parts[i])];
						}
					}
					i++;
				}
					
				update += "c";
				
				requestBroadcast();
				
				waitReplies();
				
				updateBroadcast(update);
				
				releaseBroadcast();
				
				try {
					fileMutex.acquire();
					writeFile();
					fileMutex.release();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				writeFile();
				
				stdOutClient.println(response);
				
			}else{
				
				i++;
				
				while (!parts[i].equals("c"))
				{
					
					parts[i] = parts[i].replaceAll("r", "");
					parts[i] = parts[i].replaceAll("[()]", "");
			
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
			processLamportMessage(message);
		}
	}
	
	/*
	 *
	 * Allibera la zona crítica
	 *
	 */
	public void releaseBroadcast(){
		ts.sendAction();
		stdOut1.println("RELEASE-"+ts.ticks+"-"+this.id);
		stdOut2.println("RELEASE-"+ts.ticks+"-"+this.id);
	}
	
	/*
	 *
	 * Actualitza la zona crítica
	 *
	 */
	public void updateBroadcast(String update){
		q.remove(0);
		replyCounter = 0;
		
		stdOut1.println(update);
		stdOut2.println(update);
		
		if (debug) System.out.println("[DEBUG] CoreNode "+id+" sending update frame: "+update);

	}
	
	/*
	 *
	 * Fa una petició de la zona crítica
	 *
	 */
	public void requestBroadcast(){
		ts.sendAction();
		stdOut1.println("REQUEST-"+ts.ticks+"-"+id);
		stdOut2.println("REQUEST-"+ts.ticks+"-"+id);
		q.add(new LamportQueueNode(ts.ticks, id));
	}
	
	/*
	 *
	 * Espera un missatge Lamport
	 *
	 */
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
	
	/*
	 *
	 * Processa el missatge Lamport
	 *
	 */
	private void processLamportMessage(String message){
		String[] parts;
		
		parts = message.split("-");
		
		ts.receiveAction(Integer.parseInt(parts[1]));
		
		if (parts[0].equals("REQUEST")){
			q.add(new LamportQueueNode(Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));			
			Collections.sort(q);
			sendReply(Integer.parseInt(parts[2]));
		}else if (parts[0].equals("RELEASE")){
			q.remove(0);
		}
	}

	/*
	 *
	 * Envia una resposta Lamport
	 *
	 */
	private void sendReply(int id){
		switch (this.id){
			case 1:
				if (id == 2){
					stdOut1.println("REPLY-"+ts.ticks+"-"+this.id);
					if (debug) System.out.println("[DEBUG] CoreNode " + 1 + " sending REPLY to " +2);

				}else{ 
					stdOut2.println("REPLY-"+ts.ticks+"-"+this.id);
					if (debug) System.out.println("[DEBUG] CoreNode " + 1 + " sending REPLY to " +3);

				}
				break;
			case 2:
				if (id == 1){
					stdOut2.println("REPLY-"+ts.ticks+"-"+this.id);
					if (debug) System.out.println("[DEBUG] CoreNode " + 2 + " sending REPLY to " +1);

				}else{
					stdOut1.println("REPLY-"+ts.ticks+"-"+this.id);
					if (debug) System.out.println("[DEBUG] CoreNode " + 2 + " sending REPLY to " +3);

				}
				break;
			case 3:
				if (id == 1){
					stdOut1.println("REPLY-"+ts.ticks+"-"+this.id);
					if (debug) System.out.println("[DEBUG] CoreNode " + 3+ " sending REPLY to " +1);

				}else{ 
					stdOut2.println("REPLY-"+ts.ticks+"-"+this.id);
					if (debug) System.out.println("[DEBUG] CoreNode " + 3 + " sending REPLY to " +2);

				}
				break;
		}
	}
	
	/*
	 *
	 * Escriu al fitxer per fer persistir les dades
	 *
	 */
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
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 *
	 * Envia una actualització de les dades 
	 *
	 */
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
