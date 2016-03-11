package gsn.networking.zeromq;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import gsn.DataDistributer;
import gsn.Main;
import gsn.Mappings;
import gsn.beans.DataField;
import gsn.http.delivery.DefaultDistributionRequest;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZThread;
import org.zeromq.ZThread.IAttachedRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;


public class ZeroMQProxy extends Thread implements Runnable {
	
	private static transient Logger logger = LoggerFactory.getLogger(ZeroMQProxy.class);
		
	private ZContext ctx;
	private ZMQ.Socket subscriberX;
	private ZMQ.Socket publisherX;
	private ZMQ.Socket clients;
	private Kryo kryo = new Kryo();
	private HashMap<String,DataField[]> structures = new HashMap<String,DataField[]>(); //maybe put into mappings...
	private HashMap<String,DefaultDistributionRequest> distributers = new HashMap<String, DefaultDistributionRequest>();
	private HashMap<String,Timer> timers = new HashMap<String,Timer>();

	public ZeroMQProxy (final int portOUT,final int portMETA){
		kryo.register(DataField[].class);
		ctx = Main.getZmqContext();
		
		subscriberX = ctx.createSocket(ZMQ.XSUB);
	    publisherX = ctx.createSocket(ZMQ.XPUB);
	    publisherX.setXpubVerbose(true);
	    publisherX.setHWM(0);
	    subscriberX.setHWM(0);
	    publisherX.bind("tcp://*:"+portOUT);
	    
	    clients = ctx.createSocket(ZMQ.REP);
	    clients.bind ("tcp://*:"+portMETA);
	   // System.out.println("Proxy binding to tcp://*:"+portOUT+" and tcp://*:"+portMETA);
	    
	    final ZMQ.Socket monitor = ZThread.fork(ctx,new IAttachedRunnable(){
	    	@Override
			public void run(Object[] args, ZContext ctx, Socket pipe) {
	           while (true)
	        	   {
	        	    ZFrame f = ZFrame.recvFrame(pipe);
       	   			if (f == null)
       	   				break;
       	   			byte[] msg = f.getData();
       	            if (msg.length > 0 && (msg[0] == 0 || msg[0] == 1)) {
       	            	byte[] _topic = new byte[msg.length-1];
       	            	System.arraycopy(msg, 1, _topic, 0, _topic.length);
       	            	final String topic = new String(_topic);
       	            	if (topic.startsWith("?")){  //subscriptions can be either <vsname>: or ?<vsname>?<fromtime>:
       	            		String[] r = topic.split("(\\?|:)");
	       	            	if (msg[0] == 0 && distributers.containsKey(topic)) {
	       	            		logger.warn("removing unused publisher " + topic);
	         	        	    DataDistributer.getInstance(ZeroMQDelivery.class).removeListener(distributers.get(topic));
	         	        	    distributers.remove(topic);
	         	        	    timers.remove(topic).cancel();
	       	            	} else if (msg[0] == 1 && !distributers.containsKey(topic)) {
	       	            		logger.warn("new subscription " + topic);
								long r_time = Long.parseLong(r[2]);
								ZeroMQDelivery d = new ZeroMQDelivery(topic);
								try {
									final DefaultDistributionRequest  distributionReq = DefaultDistributionRequest.create(d, Mappings.getVSensorConfig(r[1]), "select * from "+r[1], r_time);
									distributers.put(topic, distributionReq);
									logger.warn("ZMQ request received: "+distributionReq.toString());
									DataDistributer.getInstance(d.getClass()).addListener(distributionReq);
									Timer t = new Timer(topic);
									t.schedule(new TimerTask() {
										@Override
										public void run() {
											if (distributers.containsKey(topic)){
												logger.warn("timeout unused publisher " + topic);
					         	        	    DataDistributer.getInstance(ZeroMQDelivery.class).removeListener(distributionReq);
					         	        	    distributers.remove(topic);
					         	        	    timers.remove(topic);
											}
										}
									}, 70*1000);
									timers.put(topic, t); 
								} catch (Exception e) {
									logger.error("Unable to register new publisher for subscription",e);
								}
							} else if (msg[0] == 1) {
								Timer t = timers.get(topic);
								logger.warn("keepalive " + topic);
								t.cancel();
								t = new Timer(topic);
								t.schedule(new TimerTask() {
									@Override
									public void run() {
										if (distributers.containsKey(topic)){
											logger.warn("timeout unused publisher " + topic);
					         	       	    DataDistributer.getInstance(ZeroMQDelivery.class).removeListener(distributers.get(topic));
					         	       	    distributers.remove(topic);
					         	       	    timers.remove(topic);
										}
									}
								}, 70*1000);
								timers.put(topic, t);
	       	            	}  
       	            	}
       	            }
       	            f.destroy();
	        	 }   
	         }
	    });
	    
	    Thread dataProxy = new Thread(new Runnable(){

			@Override
			public void run() {
		           ZMQ.proxy(subscriberX, publisherX, monitor);
			}
	    });
	    dataProxy.setName("ZMQ-PROXY-Thread");
	    dataProxy.start();
	    
	    Thread metaResponder =  new Thread(new Runnable(){
			@Override
			public void run() {
				while (true) {
					String request = clients.recvStr (0);
					byte[] b=new byte[0];
					if (request.startsWith("?")){
                        //zmq legacy parameters
					}else{
						ByteArrayOutputStream bais = new ByteArrayOutputStream();
			            Output o = new Output(bais);
			            kryo.writeObjectOrNull(o,structures.get(request),DataField[].class);
			            o.close();
			            b = bais.toByteArray();
			            clients.send(b, 0);
					}
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

	
    

    
    

