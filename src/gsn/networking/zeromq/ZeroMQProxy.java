package gsn.networking.zeromq;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;

import gsn.Main;
import gsn.beans.DataField;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;


public class ZeroMQProxy extends Thread implements Runnable {
		
	private Context ctx;
	private ZMQ.Socket subscriberX;
	private ZMQ.Socket publisherX;
	private ZMQ.Socket clients;
	private Kryo kryo = new Kryo();
	private HashMap<String,DataField[]> structures = new HashMap<String,DataField[]>(); //maybe put into mappings...
	

	public ZeroMQProxy (final int portOUT,final int portMETA){
		kryo.register(DataField[].class);
		ctx = Main.getZmqContext();
		
		subscriberX = ctx.socket(ZMQ.XSUB);
	    publisherX = ctx.socket(ZMQ.XPUB);
	    publisherX.setXpubVerbose(true);
	    publisherX.bind("tcp://*:"+portOUT);
	    
	    clients = ctx.socket(ZMQ.REP);
	    clients.bind ("tcp://*:"+portMETA);
	   // System.out.println("Proxy binding to tcp://*:"+portOUT+" and tcp://*:"+portMETA);
	    
	    Thread dataProxy = new Thread(new Runnable(){

			@Override
			public void run() {
	            ZMQ.proxy(subscriberX, publisherX,null);
			}
	    });
	    dataProxy.setName("ZMQ-PROXY-Thread");
	    dataProxy.start();
	    
	    Thread metaResponder =  new Thread(new Runnable(){
			@Override
			public void run() {
				while (true) {
					String request = clients.recvStr (0);
					ByteArrayOutputStream bais = new ByteArrayOutputStream();
		            Output o = new Output(bais);
		            kryo.writeObjectOrNull(o,structures.get(request),DataField[].class);
		            o.close();
		            byte[] b = bais.toByteArray();
					clients.send(b, 0);
				}
			}
		});
	    metaResponder.setName("ZMQ-META-Thread");
        metaResponder.start();
	}
	
	public void connectTo(String vsName){
		//System.out.println("Proxy connect to inproc://[stream|meta]/"+vsName);
		subscriberX.connect("inproc://stream/"+vsName);
	}

	public void registerStructure(String name, DataField[] fields) {
		structures.put(name, fields);		
	}

}

	
    

    
    

