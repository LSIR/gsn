package gsn.networking.zeromq;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;

import gsn.Main;
import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.http.rest.DeliverySystem;
import gsn.http.rest.StreamElement4Rest;

public class ZeroMQDelivery implements DeliverySystem{
	
	private Context context;
	private Socket publisher;
	private boolean closed = true;
	private Kryo kryo = new Kryo();
	private VSensorConfig config;
	
	public ZeroMQDelivery(VSensorConfig config){
        this.config = config;
        
		context = Main.getZmqContext();
		// Socket to talk to clients
		publisher = context.socket(ZMQ.PUB);
		publisher.setLinger(5000);
		publisher.setSndHWM(0);
		publisher.bind("inproc://stream/"+config.getName());
		//System.out.println("Delivery bind on inproc://stream/"+config.getName());
		Main.getZmqProxy().connectTo(config.getName());
		closed = false;
		
	}

	@Override
	public void writeStructure(DataField[] fields) throws IOException {
        Main.getZmqProxy().registerStructure(config.getName(),fields);
	}

	@Override
	public boolean writeStreamElement(StreamElement se) {
		try {
			ByteArrayOutputStream bais = new ByteArrayOutputStream();
            bais.write((config.getName() + " ").getBytes());
            //StreamElement4Rest.getXstream().toXML(se.toRest(),bais);
            Output o = new Output(bais);
            kryo.writeObjectOrNull(o,se,StreamElement.class);
            o.close();
            byte[] b = bais.toByteArray();
            //System.out.println(config.getName()+" sending on Delivery");
            return publisher.send(b);
		} catch (IOException e) {
			e.printStackTrace();
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
