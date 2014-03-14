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
	    subscriberX.bind("inproc://stream");
	    publisherX = ctx.socket(ZMQ.XPUB);
	    publisherX.setXpubVerbose(true);
	    publisherX.bind("tcp://*:"+portOUT);
	    
	    //for simple proxy
	   // ZMQ.proxy(subscriberX, publisherX, null);
	    
	    Thread pubToSub = new Thread(new Runnable(){

			@Override
			public void run() {
				while (true) {   
					System.out.println("pubTosub");
		            subscriberX.send(publisherX.recv(), 0);         
		        }
				
			}});
	    Thread subToPub = new Thread(new Runnable(){

			@Override
			public void run() {
				while (true) {   
					System.out.println("subTopub");
		            publisherX.send(subscriberX.recv(), 0);         
		        }
				
			}});
	    pubToSub.start();
	    subToPub.start();
	    
	}
	
	public void listenTo(String vsName){
		subscriberX.bind("inproc://stream/"+vsName);
	}
}

	
    

    
    

