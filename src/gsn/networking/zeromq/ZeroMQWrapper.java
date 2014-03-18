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
	private String remoteContactPoint;
	private String vsensor;
	private Kryo kryo = new Kryo();

	@Override
	public DataField[] getOutputFormat() {
		DataField[] s = getActiveAddressBean().getOutputStructure();
    	if (s == null){throw new RuntimeException("ZeroMQ wrapper has an undefined output structure.");}
    	return s;
	}

	@Override
	public boolean initialize() {
		
		kryo.register(StreamElement4Rest.class);

		AddressBean addressBean = getActiveAddressBean();

		String address = addressBean.getPredicateValue ( "address" );
		vsensor = addressBean.getPredicateValue ( "vsensor" );
		if ( address == null || address.trim().length() == 0 ) 
			throw new RuntimeException( "The >address< parameter is missing from the ZeroMQWrapper wrapper." );
		remoteContactPoint = (address).trim();

		/*try{
			ZMQ.Context context = ZMQ.context(1);
			Socket syncclient = context.socket(ZMQ.REQ);
			syncclient.connect(("tcp://" + host +":"+syncport).trim());
			syncclient.send("structure");
			structure = (DataField[]) XSTREAM.fromXML(syncclient.recvStr());
			syncclient.close();
			context.term();
		}catch(Exception e){
			
		}
*/
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
		
        boolean isLocal = false;
        boolean connected = true;
        try {
            isLocal = new URI(remoteContactPoint).getScheme().equals("inproc");
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }

        connected = subscriber.base().connect(remoteContactPoint);

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
					StreamElement se = kryo.readObjectOrNull(new Input(bais),StreamElement.class);
			        //maybe queuing would be better here...
			        boolean status = postStreamElement(se);
				}else{
					if (isLocal && !connected){
						subscriber.disconnect(remoteContactPoint);
						connected = subscriber.base().connect(remoteContactPoint);
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

}
