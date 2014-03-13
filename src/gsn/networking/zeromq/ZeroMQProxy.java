package gsn.networking.zeromq;

import gsn.Main;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;


public class ZeroMQProxy extends Thread implements Runnable {
	
	
	private Context ctx;
	private ZMQ.Socket subscriberX;
	private ZMQ.Socket publisherX;
	

	public ZeroMQProxy (final int portOUT){
		ctx = Main.getZmqContext();
		
		subscriberX = ctx.socket(ZMQ.XSUB);
	    subscriberX.connect("inproc://stream");
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

	
    

    
    

