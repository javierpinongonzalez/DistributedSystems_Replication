import java.io.PrintWriter;

/*
 *
 * Classe que emula un procés que espera N segons abans de realitzar una acció
 *
 */
public class Layer2Waiter extends Thread{
	public int id;
	public PrintWriter stdOut;
	
	/*
 	*
	* Constructor
 	*
 	*/
	public Layer2Waiter(int id, PrintWriter stdOut){
		this.id = id;
		this.stdOut = stdOut;
	}
	
	/*
	 *
	 * Sobreescriu el metode Run de la classe Thread
	 *
	 */
	public void run(){
		while (true){	
			try {
				sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			stdOut.println("r"+id);
		}
	}
	
}
