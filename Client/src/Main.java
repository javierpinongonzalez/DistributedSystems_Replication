import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;


public class Main {
	
	static Socket [] sock = new Socket[7]; //Client socket
	static PrintWriter [] stdOut = new PrintWriter[7];
	static BufferedReader [] stdIn = new BufferedReader[7];
	
	public static void main(String [ ] args)
	{
		
		
		for (int i = 0 ; i<7 ; i++){
			configSock (i+1);
		}
		while(true)	{
			try {
				System.in.read();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			readFile();
			
			System.out.println("END OF FILE");
		}
	}
	
	private static void configSock(int id){
		//Connect
		try {
			//if (debug) System.out.println("[DEBUG] Process " + id + " connecting...");
			
			sock[id-1] = new Socket("127.0.0.1", 9600 + id);
			stdOut[id-1] = new PrintWriter(sock[id-1].getOutputStream(), true);
			stdIn[id-1] = new BufferedReader(new InputStreamReader(sock[id-1].getInputStream()));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void readFile(){
		String line;
		String response;
		String[] parts;
		
		
		try {
			FileReader fileReader = new FileReader("executableCommands");
			
			// Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader = 
                new BufferedReader(fileReader);

            while((line = bufferedReader.readLine()) != null) {
                //System.out.println(line);
            	//Tractar linia
            	line = line.replaceAll(" ", "");
    			System.out.println(line);

        		parts = line.split(",");
        		
        		if (parts[0].equals("b")){
        			//write
        			Random rand = new Random(); 
        			int randValue = rand.nextInt(3);
        			
        			stdOut[randValue].println(line);
        			
        			response = stdIn[randValue].readLine();
        			
        			System.out.println("Response from server: "+response);
        			
        		} else if (parts[0].equals("b<0>")){
        			//read Core layer
        			Random rand = new Random(); 
        			int randValue = rand.nextInt(3);
        			
        			
        			stdOut[randValue].println(line);
        			
        			response = stdIn[randValue].readLine();
        			
        			System.out.println("Response from server: "+response);
        			
        		} else if (parts[0].equals("b<1>")){
        			//read Layer 1
        			Random rand = new Random(); 
        			int randValue = rand.nextInt(2) + 3;
        			
        			stdOut[randValue].println(line);
        			
        			response = stdIn[randValue].readLine();
        			
        			System.out.println("Response from server: "+response);
        			
        		} else if (parts[0].equals("b<2>")){
        			//read Layer 2
        			Random rand = new Random(); 
        			int randValue = rand.nextInt(2) + 5;
        			
        			stdOut[randValue].println(line);
        			
        			response = stdIn[randValue].readLine();
        			
        			System.out.println("Response from server: "+response);
        		}
        		
        		try {
        			System.in.read();
    			} catch (IOException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}

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
	
	
}
