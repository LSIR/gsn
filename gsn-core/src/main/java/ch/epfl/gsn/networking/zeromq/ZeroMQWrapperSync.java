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

public class ZeroMQWrapperSync extends AbstractWrapper {
	
	private transient Logger logger = LoggerFactory.getLogger( this.getClass() );
	private DataField[] structure;
	private String remoteContactPoint_META;
	private String vsensor;
	private Kryo kryo = new Kryo();
	private boolean isLocal = false;
	private ZContext ctx;
	private ZMQ.Socket requester = null;
	private ZMQ.Socket receiver = null;
	private int lport = 0;
	private String laddress;

	@Override
	public DataField[] getOutputFormat() {
		if (structure == null){
			requester = ctx.createSocket(ZMQ.REQ);
			requester.setReceiveTimeOut(1000);
			requester.setSendTimeOut(1000);
			requester.setLinger(0);
			requester.connect(remoteContactPoint_META);
			if (requester.send(vsensor + "?tcp://" + laddress + ":" + lport)){
			    byte[] rec = requester.recv();
			    if (rec != null){
			        structure =  kryo.readObjectOrNull(new Input(new ByteArrayInputStream(rec)),DataField[].class);
			        if (structure != null)
			            requester.close();
			        return structure;
			    }
			}
			requester.close();
		}
    	return structure;
	}

	@Override
	public boolean initialize() {
		
		kryo.register(StreamElement4Rest.class);
		kryo.register(DataField[].class);

		AddressBean addressBean = getActiveAddressBean();

		String address = addressBean.getPredicateValue ( "address" ).toLowerCase();
		int mport = addressBean.getPredicateValueAsInt("meta_port", Main.getContainerConfig().getZMQMetaPort());
		String _lport = addressBean.getPredicateValue("local_port");
		laddress = addressBean.getPredicateValue("local_address");
		vsensor = addressBean.getPredicateValue ( "vsensor" ).toLowerCase();
		if ( address == null || address.trim().length() == 0 ) 
			throw new RuntimeException( "The >address< parameter is missing from the ZeroMQ wrapper." );
		if ( laddress == null || laddress.trim().length() == 0 ) 
			throw new RuntimeException( "The >local_address< parameter is missing from the ZeroMQ wrapper." );
		if (_lport != null){
			lport = Integer.parseInt(_lport); 
			if ( lport < 0 || lport > 65535 ) 
				throw new RuntimeException( "The >local_port< parameter must be a valid port number." );
		}
		try {
            isLocal = new URI(address).getScheme().equals("inproc");
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
		if (! isLocal ){
			remoteContactPoint_META = address.trim() + ":" + mport;
		}else{
			if(!Main.getContainerConfig().isZMQEnabled()){
				throw new IllegalArgumentException("The \"inproc\" communication can only be used if the current GSN server has zeromq enabled. Please add <zmq-enable>true</zmq-enable> to conf/ch.epfl.gsn.xml.");
			}
			remoteContactPoint_META = "tcp://127.0.0.1:" + Main.getContainerConfig().getZMQMetaPort();
		}
	    remoteContactPoint_META = remoteContactPoint_META.trim();
		ctx = Main.getZmqContext();
		receiver = ctx.createSocket(ZMQ.REP);
		if (lport == 0){
		lport = receiver.bindToRandomPort("tcp://*", 50000, 60000);
		} else {
			receiver.bind("tcp://*:"+lport);
		}
		requester = ctx.createSocket(ZMQ.REQ);
		requester.setReceiveTimeOut(1000);
		requester.setSendTimeOut(1000);
		requester.setLinger(0);
		requester.connect(remoteContactPoint_META);
		if (requester.send(vsensor + "?tcp://" + laddress + ":" + lport)){
		    byte[] rec = requester.recv();
		    if (rec != null){
		        structure =  kryo.readObjectOrNull(new Input(new ByteArrayInputStream(rec)),DataField[].class);  
		    }
		}
		requester.close();
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
		
		while (isActive()) {
			try{
				byte[] rec = receiver.recv();
				if (rec != null){
					ByteArrayInputStream bais = new ByteArrayInputStream(rec);
					StreamElement se = kryo.readObjectOrNull(new Input(bais),StreamElement.class);
			        boolean success = postStreamElement(se);
			        receiver.send(success ? new byte[]{(byte)0} : new byte[]{(byte)1});
				}
			}catch (Exception e)
			{
				logger.error("ZMQ wrapper error: ",e);
			}
		}
		receiver.close();
	}
	
	   @Override
	   public boolean isTimeStampUnique(){
		   return false;
	   }

}
