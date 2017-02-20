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


public class Layer2Node extends Thread{
	public int id;
	
	public int [] values;
	
	public boolean debug;

	public Queue shared = new LinkedList<>();
	Semaphore getMessageSemaphore = new Semaphore(0);
	
	public Socket sock; //forward/listen socket
	public PrintWriter stdOut;
	public BufferedReader stdIn;
	
	public Socket sockClient; //Client socket
	public PrintWriter stdOutClient;
	public BufferedReader stdInClient;
	
	Semaphore fileMutex = new Semaphore(1);

	
	public Layer2Node (int id){
		this.id = id;
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
			FileReader fileReader = new FileReader("Layer2Values"+id+".log");
			
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
		if (debug) System.out.println("[DEBUG] Layer2Node " + id + " starting initConfig()");
		
		switch (id) {
		case 1:
			try{
				ServerSocket serverSock = new ServerSocket(6906);
				if (debug) System.out.println("[DEBUG] Layer2Node " + id + " listening...");
				sock = serverSock.accept();
				if (debug) System.out.println("[DEBUG] Layer2Node " + id + " connected");

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
				ServerSocket serverSock = new ServerSocket(6907);
				if (debug) System.out.println("[DEBUG] Layer2Node " + id + " listening...");
				sock = serverSock.accept();
				if (debug) System.out.println("[DEBUG] Layer2Node " + id + " connected");

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
			
		default:
			System.out.println("[ERROR] Unknown id process " + id);
			break;
		}
	}	
	private void configSockClient(){
		//Listen
		int idClient = 5+id;
		try{
			ServerSocket serverSock = new ServerSocket(9600+idClient);
			if (debug) System.out.println("[DEBUG] Layer2Node " + idClient + " listening to sockClient...");
			sockClient = serverSock.accept();
			if (debug) System.out.println("[DEBUG] Layer2Node " + idClient + " connected to sockClient");

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
		
		if (debug) System.out.println("[DEBUG] Layer2Node " + id + " initializing stdListeners");
		
		
		StdListener stdListener1 = new StdListener(stdIn, shared, getMessageSemaphore, id, 1);
		stdListener1.start();
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
		
		Layer2Waiter waiter = new Layer2Waiter(id, stdOut);
		waiter.start();
		
		while (true){	
			//getMessage
			message = getMessage();
			//processMessage
			processMessage(message);
				
		}
		
		
	}
	
	private String getMessage(){
		String recievedMessage = "initialValue";
			
		if (debug) System.out.println("[DEBUG] Layer2Node " + id + " waiting...");
		
		try {
			getMessageSemaphore.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		synchronized (shared) {
			recievedMessage = (String) shared.poll();
		}
		
		if (debug) System.out.println("[DEBUG] Layer2Node " + id + " recieved message: "+recievedMessage);

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

						if (debug) System.out.println("[DEBUG] Layer2Node "+id+" writing->  key:"+partsWrite[0]+" value:"+partsWrite[1]);
						
						values[Integer.parseInt(partsWrite[0])] = Integer.parseInt(partsWrite[1]);	
					}
					i++;
				}
				try {
					if (!parts[1].equals("c")){
						fileMutex.acquire();
						writeFile();
						fileMutex.release();
					}else{
						if (debug) System.out.println("[DEBUG] Layer2Node "+id+" has noting to update");
					}
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
		}
	}	
	
	public void writeFile(){	
		if (debug) System.out.println("[DEBUG] Layer2Node "+id+" writing file...");

		
		PrintWriter writer;
		try {
			writer = new PrintWriter("Layer2Values"+id+".log", "UTF-8");
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
