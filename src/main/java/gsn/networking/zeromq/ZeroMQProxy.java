package gsn.networking.zeromq;

import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;

import gsn.DataDistributer;
import gsn.Main;
import gsn.Mappings;
import gsn.beans.DataField;
import gsn.http.DataDownload;
import gsn.http.rest.DefaultDistributionRequest;
import gsn.http.rest.PushDelivery;
import gsn.http.rest.RestStreamHanlder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZThread.IAttachedRunnable;
import org.zeromq.*;

import zmq.Pub;
import zmq.SocketBase;
import zmq.XPub;
import zmq.ZError;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;


public class ZeroMQProxy extends Thread implements Runnable {
		
	private ZContext ctx;
	private ZMQ.Socket subscriberX;
	private ZMQ.Socket publisherX;
	private ZMQ.Socket clients;
	private ZMQ.Socket monitor;
	private Kryo kryo = new Kryo();
	private HashMap<String,DataField[]> structures = new HashMap<String,DataField[]>(); //maybe put into mappings...
	private HashMap<String,DefaultDistributionRequest> distributers = new HashMap<String, DefaultDistributionRequest>();
	

	public ZeroMQProxy (final int portOUT,final int portMETA){
		kryo.register(DataField[].class);
		ctx = Main.getZmqContext();
		
		subscriberX = ctx.createSocket(ZMQ.XSUB);
	    publisherX = ctx.createSocket(ZMQ.XPUB);
	    publisherX.setXpubVerbose(true);
	    publisherX.setHWM(0);
	    subscriberX.setHWM(0);
	    publisherX.bind("tcp://*:"+portOUT);
	    publisherX.monitor("inproc://monitor/proxy", ZMQ.EVENT_UNSUBSCRIPTION);
	    
	    
	    clients = ctx.createSocket(ZMQ.REP);
	    clients.bind ("tcp://*:"+portMETA);
	   // System.out.println("Proxy binding to tcp://*:"+portOUT+" and tcp://*:"+portMETA);
	    
	    Thread monitoring = new Thread(new Runnable(){
	    	private transient Logger logger = LoggerFactory.getLogger(ZeroMQProxy.class);
	    	@Override
			public void run() {
	    		Socket monit = ctx.createSocket(ZMQ.PAIR);
		    	   monit.connect("inproc://monitor/proxy");
	           while (true)
	        	   {
		        	   ZMQ.Event event = ZMQ.Event.recv(monit);
		        	   if (event == null && monit.base().errno() == ZError.ETERM) {
		        	   break;
		        	   }
		        	   String topic = ((String)event.getValue()).substring(1);
		        	   logger.warn("["+event.getAddress()+"] removing unused publisher " + topic);
		        	   DataDistributer.getInstance(ZeroMQDelivery.class).removeListener(distributers.get(topic));
		        	   distributers.remove(topic);
	        	   }
	           monit.close();
			}
	    });
	    monitoring.setName("PUB-monitoring");
	    monitoring.start();
	    monitor = ZThread.fork(ctx,new IAttachedRunnable(){
	    	@Override
			public void run(Object[] args, ZContext ctx, Socket pipe) {
	           while (true)
	        	   {
	        	   ZFrame f = ZFrame.recvFrame(pipe);
       	   			if (f == null)
       	   				break;
       	   			f.print("");
       	   			f.destroy();
	        	   }
	    	}
	    });

	    Thread dataProxy = new Thread(new Runnable(){

			@Override
			public void run() {
		           ZMQ.proxy(subscriberX, publisherX,null);
			}
	    });
	    dataProxy.setName("ZMQ-PROXY-Thread");
	    dataProxy.start();
	    
	    Thread metaResponder =  new Thread(new Runnable(){
	    	private transient Logger logger = LoggerFactory.getLogger ( ZeroMQProxy.class );
			@Override
			public void run() {
				while (true) {
					String request = clients.recvStr (0);
					logger.debug("ZMQ request received: "+request);
					byte[] b=new byte[0];
					if (request.startsWith("?")){
						try{
							String[] r = request.split("\\?");
							if (Mappings.getVSensorConfig(r[1]) != null){
								Connection conn = Main.getStorage(r[1]).getConnection();
								ResultSet resultMax = Main.getStorage(r[1]).executeQueryWithResultSet(new StringBuilder("select MAX(timed) from ").append(r[1]), conn);
								resultMax.next();
								long m_time = resultMax.getLong(1);
								long r_time = Long.parseLong(r[2]);
								if (m_time > r_time){
									int rand = (int) Math.round(Math.random()*100000);
									ZeroMQDelivery d = new ZeroMQDelivery(rand+"@"+r[1]);
									b = (rand+"@"+r[1]).getBytes();
									clients.send(b, 0);
									Thread.sleep(1000);
									DefaultDistributionRequest distributionReq = DefaultDistributionRequest.create(d, Mappings.getVSensorConfig(r[1]), "select * from "+r[1], r_time);
									distributers.put(rand + "@" + r[1], distributionReq);
									logger.debug("ZMQ request received: "+distributionReq.toString());
									DataDistributer.getInstance(d.getClass()).addListener(distributionReq);
									logger.debug("Streaming request received and registered:"+distributionReq.toString());
								}
							}
						}catch(Exception e){}
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

	
    

    
    

