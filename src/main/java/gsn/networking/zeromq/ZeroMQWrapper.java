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
import gsn.http.rest.StreamElement4Rest;
import gsn.wrappers.AbstractWrapper;

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
			if(isLive){
				if (requester.send(vsensor)){
				    byte[] rec = requester.recv();
				    if (rec != null){
				        structure =  kryo.readObjectOrNull(new Input(new ByteArrayInputStream(rec)),DataField[].class);
				        if (structure != null)
				            requester.close();
				        return structure;
				    }
				}
			}else{
				if (requester.send(vsensor)){
				    byte[] rec = requester.recv();
				    if (rec != null){
				        structure =  kryo.readObjectOrNull(new Input(new ByteArrayInputStream(rec)),DataField[].class);
				        requester.send("?"+vsensor + "?" + startTime);
				        rec = requester.recv();
				        if (structure != null)
				            requester.close();
					    if (rec != null){
					        vsensor = new String(rec);
					        return structure;
					    }else{
					    	return null;
					    }
				    }
				}
			}
		}
		//if user defined structure is used... maybe
		//DataField[] s = getActiveAddressBean().getOutputStructure();
    	//if (s == null){throw new RuntimeException("ZeroMQ wrapper has an undefined output structure.");}
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
		
		if(isLive){
			if (requester.send(vsensor)){
			    byte[] rec = requester.recv();
			    if (rec != null){
			        structure =  kryo.readObjectOrNull(new Input(new ByteArrayInputStream(rec)),DataField[].class);
			        if (structure != null)
			            requester.close();
			    }
			}
		}else{
			if (requester.send(vsensor)){
			    byte[] rec = requester.recv();
			    if (rec != null){
			        structure =  kryo.readObjectOrNull(new Input(new ByteArrayInputStream(rec)),DataField[].class);
			        requester.send("?"+vsensor + "?" + startTime);
			        rec = requester.recv();
			        if (structure != null)
			            requester.close();
				    if (rec != null){
				        vsensor = new String(rec);
				    }else{
				    	return false; //can get customized pub
				    }
			    }
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
		subscriber.setReceiveTimeOut(3000);
		subscriber.setRcvHWM(0); // no limit

		subscriber.subscribe((vsensor+":").getBytes());
		//System.out.println("connected to Queue: "+ remoteContactPoint + " and subscribe to " + vsensor);

		while (isActive()) {
			try{
				byte[] rec = subscriber.recv();
				if (rec != null){
					//System.out.println("read from wrapper");
					ByteArrayInputStream bais = new ByteArrayInputStream(rec);
					bais.skip(vsensor.getBytes().length + 2);
					//StreamElement se = ((StreamElement4Rest)(StreamElement4Rest.getXstream().fromXML(bais))).toStreamElement();
					StreamElement se = kryo.readObjectOrNull(new Input(bais),StreamElement.class);
			        //maybe queuing would be better here...
			        boolean status = postStreamElement(se);
			        //System.out.println("receiving :" + se);
				}else{
					if (isLocal && !connected){
						subscriber.disconnect(remoteContactPoint_DATA);
						connected = subscriber.base().connect(remoteContactPoint_DATA);
					}
					//System.out.println("timeout on wrapper, subscribing to "+ vsensor);
					subscriber.subscribe((vsensor+":").getBytes());
				}
			}catch (Exception e)
			{
				logger.error(e.getMessage());
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
