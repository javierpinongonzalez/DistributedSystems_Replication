
public class Main {
	public static void main(String [ ] args)
	{
		boolean debug = true;
		
		CoreNode [] lamportList = new CoreNode[3];
		Layer1Node [] layer1List = new Layer1Node[2];
		Layer2Node [] layer2List = new Layer2Node[2];

		//System.out.println("Hello World");
		
		for (int i=1 ; i<=3 ; i++){
			lamportList[i-1] = new CoreNode(i, new LamportClock());
			lamportList[i-1].debug = debug;
		}
		
		for (int i=1 ; i<=2 ; i++){
			layer1List[i-1] = new Layer1Node(i);
			layer1List[i-1].debug = debug;
		}
		
		for (int i=1 ; i<=2 ; i++){
			layer2List[i-1] = new Layer2Node(i);
			layer2List[i-1].debug = debug;
		}
				
		for (int i=0; i<3 ; i++){
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			lamportList[i].start();
		}
		
		for (int i=0; i<2 ; i++){
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			layer1List[i].start();
		}
		
		for (int i=0; i<2 ; i++){
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			layer2List[i].start();
		}
	}
}
