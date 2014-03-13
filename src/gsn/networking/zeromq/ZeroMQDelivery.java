package gsn.networking.zeromq;

import java.io.IOException;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

import com.thoughtworks.xstream.XStream;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.http.rest.DeliverySystem;
import gsn.http.rest.StreamElement4Rest;

public class ZeroMQDelivery implements DeliverySystem{
	
	private Context context;
	private Socket publisher;
	private Socket syncservice;
	private boolean closed = true;
	private DataField[] structure;
	private final XStream XSTREAM = StreamElement4Rest.getXstream();
	
	public ZeroMQDelivery(){
		
context = ZMQ.context(1);
		
		// Socket to talk to clients
		publisher = context.socket(ZMQ.PUB);
		publisher.setLinger(5000);
		// In 0MQ 3.x pub socket could drop messages if sub can follow the generation of pub messages
		publisher.setSndHWM(0);
		publisher.bind("inproc://stream");
		String xml = XSTREAM.toXML(new StreamElement4Rest(data));
		publisher.send("vsensor "+xml);
		
		context = ZMQ.context(1);
		
		// Socket to talk to clients
		publisher = context.socket(ZMQ.PUB);
		publisher.setLinger(5000);
		// In 0MQ 3.x pub socket could drop messages if sub can follow the generation of pub messages
		publisher.setSndHWM(0);
		publisher.bind("tcp://*:6000");

		// Socket to receive signals
		syncservice = context.socket(ZMQ.REP);
		syncservice.bind("tcp://*:6002");
		
		closed = false;
	}

	@Override
	public void writeStructure(DataField[] fields) throws IOException {
		structure = fields;
		String xml = XSTREAM.toXML(fields);
		syncservice.send(xml);
	}

	@Override
	public boolean writeStreamElement(StreamElement se) {
		String xml = XSTREAM.toXML(new StreamElement4Rest(se));
		return publisher.send(xml, 0);
	}

	@Override
	public boolean writeKeepAliveStreamElement() {
		return true;
	}

	@Override
	public void close() {
		publisher.close();
		syncservice.close();
		context.term();
		closed = true;	
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

}
