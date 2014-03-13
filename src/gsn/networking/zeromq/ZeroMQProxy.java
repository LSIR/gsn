package gsn.networking.zeromq;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;


public class ZeroMQProxy extends Thread implements Runnable {
	
	
	private Context ctx;
	private ZMQ.Socket subscriberX;
	private ZMQ.Socket publisherX;
	

	public ZeroMQProxy (final int portIN, final int portOUT){
		ctx =  ZMQ.context(1);
		
		subscriberX = ctx.socket(ZMQ.XSUB);
	    subscriberX.connect("tcp://127.0.0.1:"+portIN);
	    publisherX = ctx.socket(ZMQ.XPUB);
	    publisherX.bind("tcp://*:"+portOUT);
	    
	    Thread pubToSub = new Thread(new Runnable(){

			@Override
			public void run() {
				while (true) {       
		            subscriberX.send(publisherX.recv(), 0);         
		        }
				
			}});
	    Thread subToPub = new Thread(new Runnable(){

			@Override
			public void run() {
				while (true) {   
		            publisherX.send(subscriberX.recv(), 0);         
		        }
				
			}});
	    pubToSub.start();
	    subToPub.start();
	}
}

	
    

    
    

