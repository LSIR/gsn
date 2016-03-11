package gsn.networking.zeromq;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.zeromq.ZContext;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.zeromq.ZMQ;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;

import gsn.Main;
import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.wrappers.AbstractWrapper;
import gsn.http.delivery.StreamElement4Rest;

public class ZeroMQWrapper extends AbstractWrapper {
	
	private transient Logger logger = LoggerFactory.getLogger( this.getClass() );
	private DataField[] structure;
	private String remoteContactPoint_DATA;
	private String remoteContactPoint_META;
	private String vsensor;
	private boolean isLive = true;
	private long startTime = -1;
	private Kryo kryo = new Kryo();
	private boolean isLocal = false;
	ZMQ.Socket requester = null;

	@Override
	public DataField[] getOutputFormat() {
		if (structure == null){
				if (requester.send(vsensor)){
				    byte[] rec = requester.recv();
				    if (rec != null){
				        structure =  kryo.readObjectOrNull(new Input(new ByteArrayInputStream(rec)),DataField[].class);
				        if (structure != null)
				            requester.close();
				        return structure;
				    }
				}
		}
    	return structure;
	}

	@Override
	public boolean initialize() {
		
		kryo.register(StreamElement4Rest.class);
		kryo.register(DataField[].class);

		AddressBean addressBean = getActiveAddressBean();

		String address = addressBean.getPredicateValue ( "address" ).toLowerCase();
		int dport = addressBean.getPredicateValueAsInt("data_port",Main.getContainerConfig().getZMQProxyPort());
		int mport = addressBean.getPredicateValueAsInt("meta_port", Main.getContainerConfig().getZMQMetaPort());
		vsensor = addressBean.getPredicateValue ( "vsensor" ).toLowerCase();
		if ( address == null || address.trim().length() == 0 ) 
			throw new RuntimeException( "The >address< parameter is missing from the ZeroMQWrapper wrapper." );
		String time = addressBean.getPredicateValueWithDefault("starting","-1");
		startTime = Long.parseLong(time);
		if (startTime >= 0) isLive = false;  
		
		try {
            isLocal = new URI(address).getScheme().equals("inproc");
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
		if (! isLocal ){
			remoteContactPoint_DATA = address.trim() + ":" + dport;
			remoteContactPoint_META = address.trim() + ":" + mport;
		}else{
			if(!isLive){
				throw new IllegalArgumentException("The \"inproc\" communication can only be used for live streams.");
			}
			if(!Main.getContainerConfig().isZMQEnabled()){
				throw new IllegalArgumentException("The \"inproc\" communication can only be used if the current GSN server has zeromq enabled. Please add <zmq-enable>true</zmq-enable> to conf/gsn.xml.");
			}
			remoteContactPoint_DATA = address.trim();
			remoteContactPoint_META = "tcp://127.0.0.1:" + Main.getContainerConfig().getZMQMetaPort();
		}
		
		ZContext ctx = Main.getZmqContext();
		requester = ctx.createSocket(ZMQ.REQ);
		requester.setReceiveTimeOut(1000);
		requester.setSendTimeOut(1000);
		requester.connect((remoteContactPoint_META).trim());
		
		if (requester.send(vsensor)){
		    byte[] rec = requester.recv();
		    if (rec != null){
		        structure =  kryo.readObjectOrNull(new Input(new ByteArrayInputStream(rec)),DataField[].class);
		        if (structure != null)
		            requester.close();
		    }
		}
		
        return true;
	}

	@Override
	public void dispose() {
		
	}

	@Override
	public String getWrapperName() {
		return "ZeroMQ wrapper";
	}
	
	@Override
	public void run(){
		ZContext context = Main.getZmqContext();
		ZMQ.Socket subscriber = context.createSocket(ZMQ.SUB);
		
        boolean connected = subscriber.base().connect(remoteContactPoint_DATA);
		subscriber.setReceiveTimeOut(1000);
		subscriber.setRcvHWM(0); // no limit
		System.out.println("subscribed");
		if (isLive) {
			subscriber.subscribe((vsensor+":").getBytes());
		} else {
			subscriber.subscribe(("?"+vsensor+"?"+startTime+":").getBytes());
		}
		
		long lastKeepAlive = 0;
		
		while (isActive()) {
			try{
				byte[] rec = subscriber.recv();
				if (rec != null){
					System.out.println("read from wrapper");
					ByteArrayInputStream bais = new ByteArrayInputStream(rec);
					if(isLive){
					    bais.skip(vsensor.getBytes().length + 2);
					}else{
						bais.skip(("?"+vsensor+"?"+startTime+":").getBytes().length + 1);
					}
					StreamElement se = kryo.readObjectOrNull(new Input(bais),StreamElement.class);
			        boolean status = postStreamElement(se);
				}else{
					if (isLocal && !connected){
						subscriber.disconnect(remoteContactPoint_DATA);
						connected = subscriber.base().connect(remoteContactPoint_DATA);
					}
					if (isLive) {
						System.out.println("subscribed");
						subscriber.subscribe((vsensor+":").getBytes());
					}
				}
				if (System.currentTimeMillis() - lastKeepAlive > 30000){
					if (!isLive) {
						System.out.println("subscribed");
						subscriber.subscribe(("?"+vsensor+"?"+startTime+":").getBytes());
						lastKeepAlive = System.currentTimeMillis();
					}
				}
			}catch (Exception e)
			{
				logger.error("ZMQ wrapper error: ",e);
			}
		}
		subscriber.unsubscribe((vsensor+":").getBytes());
		try {
			Thread.sleep(4000);
		} catch (InterruptedException e) {}
		subscriber.close();
	}
	
	   @Override
	   public boolean isTimeStampUnique(){
		   return false;
	   }

}
