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

/*
 *
 * Classe que implementa els nodes de la capa 2
 *
 */
public class Layer2Node extends Thread{
	public int id;
	
	public int [] values;
	
	public boolean debug;

	public Queue shared = new LinkedList<>();
	Semaphore getMessageSemaphore = new Semaphore(0);
	
	public Socket sock; 
	public PrintWriter stdOut;
	public BufferedReader stdIn;
	
	public Socket sockClient; 
	public PrintWriter stdOutClient;
	public BufferedReader stdInClient;
	
	Semaphore fileMutex = new Semaphore(1);

	/*
	 *
	 * Constructor
	 *
	 */
	public Layer2Node (int id){
		this.id = id;
		this.values = new int [100];
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
	 * Inicialitza els valors de la capa 2
	 *
	 */
	private void initValues(){
		String line;
		int i = 0;
		
		try {
			FileReader fileReader = new FileReader("Layer2Values"+id+".log");
			
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
            e.printStackTrace();                
        }
	}
	
	/*
	 *
	 * Inicialitza la configuració dels sockets
	 *
	 */
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
				
				serverSock.close();
				
				configSockClient();
				
			}catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
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
				
				serverSock.close();
								
				configSockClient();

				
			}catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
			
		default:
			System.out.println("[ERROR] Unknown id process " + id);
			break;
		}
	}	

	/*
	 *
	 * Configura el socket amb el client
	 *
	 */
	private void configSockClient(){
		int idClient = 5+id;
		try{
			ServerSocket serverSock = new ServerSocket(9600+idClient);
			if (debug) System.out.println("[DEBUG] Layer2Node " + idClient + " listening to sockClient...");
			sockClient = serverSock.accept();
			if (debug) System.out.println("[DEBUG] Layer2Node " + idClient + " connected to sockClient");

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
		
		if (debug) System.out.println("[DEBUG] Layer2Node " + id + " initializing stdListeners");
		
		
		StdListener stdListener1 = new StdListener(stdIn, shared, getMessageSemaphore, id, 1);
		stdListener1.start();
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
		
		Layer2Waiter waiter = new Layer2Waiter(id, stdOut);
		waiter.start();
		
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
			
		if (debug) System.out.println("[DEBUG] Layer2Node " + id + " waiting...");
		
		try {
			getMessageSemaphore.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		synchronized (shared) {
			recievedMessage = (String) shared.poll();
		}
		
		if (debug) System.out.println("[DEBUG] Layer2Node " + id + " recieved message: "+recievedMessage);

		return recievedMessage;
	}
	
	/*
	 *
	 * Processa el missatge rebut pel socket
	 *
	 */
	private void processMessage (String message){
		String [] parts;
		String response;
		int i=0;
		
	
		if (message.substring(0, 1).equals("b")){
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
					e.printStackTrace();
				}
			}else{
				parts = message.split(",");
				i++;
				response = "";
				
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
		}
	}	
	
	/*
	 *
	 * Escriu al fitxer per fer persistir les dades
	 *
	 */
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
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	
}
