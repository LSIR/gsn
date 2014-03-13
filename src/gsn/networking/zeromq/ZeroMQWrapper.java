package gsn.networking.zeromq;

import java.io.ByteArrayInputStream;

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
		subscriber.connect(remoteContactPoint);
		subscriber.subscribe(vsensor.getBytes());
		
		System.out.println("connected to Queue: "+ remoteContactPoint);

		while (isActive()) {
			try{
				ByteArrayInputStream bais = new ByteArrayInputStream(subscriber.recv());
				bais.skip(vsensor.getBytes().length + 1);
				System.out.println("read");
				StreamElement se = kryo.readObjectOrNull(new Input(bais),StreamElement.class);
		        //maybe queuing would be better here...
		        boolean status = postStreamElement(se);
			}catch (Exception e)
			{
				logger.error(e.getMessage());
			}
		}
		subscriber.close();
	}

}
