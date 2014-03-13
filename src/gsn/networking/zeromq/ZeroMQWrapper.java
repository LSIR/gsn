package gsn.networking.zeromq;

import java.io.ByteArrayInputStream;

import org.apache.log4j.Logger;
import org.zeromq.ZMQ;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
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
    	if (s == null){throw new RuntimeException("Direct Push wrapper has an undefined output structure.");}
    	return s;
	}

	@Override
	public boolean initialize() {
		
		kryo.register(StreamElement4Rest.class);

		AddressBean addressBean = getActiveAddressBean();

		String host = addressBean.getPredicateValue ( "host" );
		vsensor = addressBean.getPredicateValue ( "vsensor" );
		if ( host == null || host.trim().length() == 0 ) 
			throw new RuntimeException( "The >host< parameter is missing from the ZeroMQWrapper wrapper." );
		int port = addressBean.getPredicateValueAsInt("port",6001);
		int syncport = addressBean.getPredicateValueAsInt("syncport",6002);
		if ( port > 65000 || port <= 0 || syncport > 65000 || syncport <= 0 ) 
			throw new RuntimeException("ZeroMQ wrapper initialization failed, bad port number: "+port);
		remoteContactPoint = ("tcp://" + host +":"+port).trim();
		
		System.out.println("init");

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
		ZMQ.Context context = ZMQ.context(1);

		ZMQ.Socket subscriber = context.socket(ZMQ.SUB);
		subscriber.connect(remoteContactPoint);
		subscriber.subscribe(vsensor.getBytes());
		
		System.out.println("connected to Queue: "+ remoteContactPoint);

		while (isActive()) {
			try{
				ByteArrayInputStream bais = new ByteArrayInputStream(subscriber.recv());
				bais.skip(vsensor.getBytes().length + 1);
				StreamElement4Rest se = kryo.readObjectOrNull(new Input(bais),StreamElement4Rest.class);
		        StreamElement streamElement = se.toStreamElement();
		        //maybe queuing would be better here...
		        boolean status = postStreamElement(streamElement);
			}catch (Exception e)
			{
				logger.error(e.getMessage());
			}
		}
		subscriber.close();
		context.term();
	}

}
