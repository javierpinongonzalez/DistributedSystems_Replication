import java.io.PrintWriter;


public class Layer2Waiter extends Thread{
	public int id;
	public PrintWriter stdOut;
	
	
	public Layer2Waiter(int id, PrintWriter stdOut){
		this.id = id;
		this.stdOut = stdOut;
	}
	
	public void run(){
		while (true){	
			//getMessage
			try {
				sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			stdOut.println("r"+id);
		}
	}
	
}
