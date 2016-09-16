package ch.epfl.gsn.networking.zeromq;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.zeromq.ZContext;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.zeromq.ZMQ;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;

import ch.epfl.gsn.Main;
import ch.epfl.gsn.beans.AddressBean;
import ch.epfl.gsn.beans.DataField;
import ch.epfl.gsn.beans.StreamElement;
import ch.epfl.gsn.delivery.StreamElement4Rest;
import ch.epfl.gsn.wrappers.AbstractWrapper;

public class ZeroMQWrapperAsync extends AbstractWrapper {
	
	private transient Logger logger = LoggerFactory.getLogger( this.getClass() );
	private DataField[] structure;
	private String remoteContactPoint_DATA;
	private String remoteContactPoint_META;
	private String vsensor;
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
		
		try {
            isLocal = new URI(address).getScheme().equals("inproc");
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
		if (! isLocal ){
			remoteContactPoint_DATA = address.trim() + ":" + dport;
			remoteContactPoint_META = address.trim() + ":" + mport;
		}else{
			if(!Main.getContainerConfig().isZMQEnabled()){
				throw new IllegalArgumentException("The \"inproc\" communication can only be used if the current GSN server has zeromq enabled. Please add <zmq-enable>true</zmq-enable> to conf/ch.epfl.gsn.xml.");
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
        subscriber.subscribe((vsensor+":").getBytes());
		
		while (isActive()) {
			try{
				byte[] rec = subscriber.recv();
				if (rec != null){
					ByteArrayInputStream bais = new ByteArrayInputStream(rec);
					bais.skip(vsensor.getBytes().length + 2);
					StreamElement se = kryo.readObjectOrNull(new Input(bais),StreamElement.class);
			        postStreamElement(se);
				}else{
					if (isLocal && !connected){
						subscriber.disconnect(remoteContactPoint_DATA);
						connected = subscriber.base().connect(remoteContactPoint_DATA);
					}
					subscriber.subscribe((vsensor+":").getBytes());
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
