import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;


public class Client extends Thread{
	public int id;
	public int type;
	public Socket sock; //Client socket
	public PrintWriter stdOut;
	public BufferedReader stdIn;
	
	public Client(int id){
		this.id = id;
		
		if (id <= 3){
			type = 0;
		}else if (id <= 5){
			type = 1;
		}
	}
	
}
