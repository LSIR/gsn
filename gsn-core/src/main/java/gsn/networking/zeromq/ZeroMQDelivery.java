package gsn.networking.zeromq;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.zeromq.ZContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;

import gsn.Main;
import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.http.delivery.DeliverySystem;

public class ZeroMQDelivery implements DeliverySystem{
	
	private ZContext context;
	private Socket publisher;
	private boolean closed = true;
	private Kryo kryo = new Kryo();
	private String name;

	public static transient Logger logger = LoggerFactory.getLogger ( ZeroMQDelivery.class );
	
	public ZeroMQDelivery(String name){
        this.name = name;
		context = Main.getZmqContext();
		// Socket to talk to clients
		publisher = context.createSocket(ZMQ.PUB);
		publisher.setLinger(5000);
		publisher.setSndHWM(0); // no limit
		publisher.bind("inproc://stream/"+name);
		Main.getZmqProxy().connectTo(name);
		closed = false;

	}

	@Override
	public void writeStructure(DataField[] fields) throws IOException {
        Main.getZmqProxy().registerStructure(name,fields);
	}

	@Override
	public boolean writeStreamElement(StreamElement se) {
		try {
			ByteArrayOutputStream bais = new ByteArrayOutputStream();
            bais.write((name + ": ").getBytes());
            Output o = new Output(bais);
            kryo.writeObjectOrNull(o,se,StreamElement.class);
            o.close();
            byte[] b = bais.toByteArray();
            return publisher.send(b);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return false;
	}

	@Override
	public boolean writeKeepAliveStreamElement() {
		return true;
	}

	@Override
	public void close() {
		publisher.close();
		closed = true;	
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

}
