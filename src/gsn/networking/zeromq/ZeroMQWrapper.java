package gsn.networking.zeromq;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;
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
	
	private transient Logger logger = Logger.getLogger( this.getClass() );
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

		String address = addressBean.getPredicateValue ( "address" );
		int dport = addressBean.getPredicateValueAsInt("data_port",Main.getContainerConfig().getZMQProxyPort());
		int mport = addressBean.getPredicateValueAsInt("meta_port", Main.getContainerConfig().getZMQMetaPort());
		vsensor = addressBean.getPredicateValue ( "vsensor" );
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
				throw new IllegalArgumentException("The \"inproc\" communication can only be used if the current GSN server has zeromq enabled. Please add <zmq-enable>true</zmq-enable> to conf/gsn.xml.");
			}
			remoteContactPoint_DATA = address.trim();
			remoteContactPoint_META = "tcp://127.0.0.1:" + Main.getContainerConfig().getZMQMetaPort();
		}
		
		ZMQ.Context ctx = Main.getZmqContext();
		requester = ctx.socket(ZMQ.REQ);
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
		ZMQ.Context context = Main.getZmqContext();
		ZMQ.Socket subscriber = context.socket(ZMQ.SUB);
		
        boolean connected = subscriber.base().connect(remoteContactPoint_DATA);
		subscriber.setReceiveTimeOut(3000);

		subscriber.subscribe(vsensor.getBytes());
		//System.out.println("connected to Queue: "+ remoteContactPoint + " and subscribe to " + vsensor);

		while (isActive()) {
			try{
				byte[] rec = subscriber.recv();
				if (rec != null){
					//System.out.println("read from wrapper");
					ByteArrayInputStream bais = new ByteArrayInputStream(rec);
					bais.skip(vsensor.getBytes().length + 1);
					//StreamElement se = ((StreamElement4Rest)(StreamElement4Rest.getXstream().fromXML(bais))).toStreamElement();
					StreamElement se = kryo.readObjectOrNull(new Input(bais),StreamElement.class);
			        //maybe queuing would be better here...
			        boolean status = postStreamElement(se);
				}else{
					if (isLocal && !connected){
						subscriber.disconnect(remoteContactPoint_DATA);
						connected = subscriber.base().connect(remoteContactPoint_DATA);
					}
					//System.out.println("timeout on wrapper, subscribing to "+ vsensor);
					subscriber.subscribe(vsensor.getBytes());
				}
			}catch (Exception e)
			{
				logger.error(e.getMessage());
			}
		}
		subscriber.close();
	}
	
	   @Override
	   public boolean isTimeStampUnique(){
		   return false;
	   }

}
