package ch.epfl.gsn.networking.zeromq;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;

import org.zeromq.ZMQ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;

import ch.epfl.gsn.DataDistributer;
import ch.epfl.gsn.Main;
import ch.epfl.gsn.Mappings;
import ch.epfl.gsn.beans.DataField;
import ch.epfl.gsn.delivery.DefaultDistributionRequest;


public class ZeroMQProxy extends Thread implements Runnable {
	
	private static transient Logger logger = LoggerFactory.getLogger(ZeroMQProxy.class);
		
	private ZContext ctx;
	private ZMQ.Socket subscriberX;
	private ZMQ.Socket publisherX;
	private ZMQ.Socket clients;
	private Kryo kryo = new Kryo();
	private HashMap<String,DataField[]> structures = new HashMap<String,DataField[]>(); //maybe put into mappings...

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

	    Thread dataProxy = new Thread(new Runnable(){

			@Override
			public void run() {
		           ZMQ.proxy(subscriberX, publisherX, null);
			}
	    });
	    dataProxy.setName("ZMQ-PROXY-Thread");
	    dataProxy.start();
	    
	    Thread metaResponder =  new Thread(new Runnable(){
			@Override
			public void run() {
				while (true) {
					String request = clients.recvStr (0);
					String [] parts = request.split("\\?");
					if (parts.length > 1){
						try{
							long startTime = System.currentTimeMillis();
							if (parts.length > 2){
								startTime = Long.parseLong(parts[2]);
							}
							ZeroMQDeliverySync d = new ZeroMQDeliverySync(parts[0], parts[1]);
							final DefaultDistributionRequest  distributionReq = DefaultDistributionRequest.create(d, Mappings.getVSensorConfig(parts[0]), "select * from "+parts[0], startTime);
							logger.info("ZMQ request received: "+distributionReq.toString());
							DataDistributer.getInstance(d.getClass()).addListener(distributionReq);
						}catch (Exception e){
							logger.warn("ZMQ request parsing error: " + request, e);
						}
					}
					byte[] b=new byte[0];
					ByteArrayOutputStream bais = new ByteArrayOutputStream();
		            Output o = new Output(bais);
		            kryo.writeObjectOrNull(o,structures.get(parts[0]),DataField[].class);
		            o.close();
		            b = bais.toByteArray();
		            clients.send(b, 0);
				}
			}
		});
	    metaResponder.setName("ZMQ-META-Thread");
        metaResponder.start();
	}
	
	public void connectTo(String vsName){
		subscriberX.connect("inproc://stream/"+vsName);
	}

	public void registerStructure(String name, DataField[] fields) {
		structures.put(name, fields);		
	}
	

}

	
    

    
    

