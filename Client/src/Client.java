import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

/*
 *
 * No s'utilitza aquesta classe
 *
 */
public class Client extends Thread{
	public int id;
	public int type;
	public Socket sock;
	public PrintWriter stdOut;
	public BufferedReader stdIn;
	

	/*
     *
     * Constructor
     *
     */
	public Client(int id){
		this.id = id;
		
		if (id <= 3){
			type = 0;
		}else if (id <= 5){
			type = 1;
		}
	}
	
}
