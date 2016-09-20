package controllers.gsn.api;

import java.io.ByteArrayInputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;

import ch.epfl.gsn.beans.DataField;
import ch.epfl.gsn.beans.StreamElement;
import ch.epfl.gsn.delivery.StreamElement4Rest;

public class StreamElementDeserializer {
	
	private Kryo kryo = new Kryo();
	
	public StreamElementDeserializer(){
		kryo.register(StreamElement4Rest.class);
		kryo.register(DataField[].class);
	}
	
	
	public StreamElement deserialize(String vsensor, byte[] input){
		ByteArrayInputStream bais = new ByteArrayInputStream(input);
		bais.skip(vsensor.getBytes().length + 2);
		return kryo.readObjectOrNull(new Input(bais),StreamElement.class);
	}

}
