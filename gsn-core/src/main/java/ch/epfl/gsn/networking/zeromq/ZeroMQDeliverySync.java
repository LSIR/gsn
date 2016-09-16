package ch.epfl.gsn.networking.zeromq;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.zeromq.ZContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;

import ch.epfl.gsn.Main;
import ch.epfl.gsn.beans.DataField;
import ch.epfl.gsn.beans.StreamElement;
import ch.epfl.gsn.delivery.DeliverySystem;

public class ZeroMQDeliverySync implements DeliverySystem{
	
	private ZContext context;
	private Socket sender;
	private boolean closed = true;
	private Kryo kryo = new Kryo();
	private String name;

	public static transient Logger logger = LoggerFactory.getLogger ( ZeroMQDeliverySync.class );
	
	public ZeroMQDeliverySync(String name, String remoteContactPoint){
		if (name.endsWith(":")){
			name = name.substring(0, name.length()-1);
		}
        this.name = name;
		context = Main.getZmqContext();
		sender = context.createSocket(ZMQ.REQ);
		sender.connect(remoteContactPoint);
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
            Output o = new Output(bais);
            kryo.writeObjectOrNull(o,se,StreamElement.class);
            o.close();
            byte[] b = bais.toByteArray();
            if(sender.send(b)){
            	byte[] rec = sender.recv();
		        return rec != null && rec.length == 1 && rec[0] == 0;
            }
		} catch (Exception e) {
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
		sender.close();
		closed = true;	
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

}
