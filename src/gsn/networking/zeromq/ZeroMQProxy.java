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
	    publisherX = ctx.socket(ZMQ.XPUB);
	    publisherX.setXpubVerbose(true);
	    publisherX.bind("tcp://*:"+portOUT);
	    //System.out.println("Proxy binding to tcp://*:"+portOUT);
	    

	    Thread proxy = new Thread(new Runnable(){

			@Override
			public void run() {
	            ZMQ.proxy(subscriberX, publisherX,null);
			}
	    });
	    
	    proxy.start();
	    
	}
	
	public void connectTo(String vsName){
		//System.out.println("Proxy connect to inproc://stream/"+vsName);
		subscriberX.connect("inproc://stream/"+vsName);
	}
}

	
    

    
    

