package ch.epfl.gsn.networking.mqtt;

import java.io.IOException;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.epfl.gsn.beans.DataField;
import ch.epfl.gsn.beans.StreamElement;
import ch.epfl.gsn.delivery.DeliverySystem;

public class MQTTDelivery implements DeliverySystem {
	
	private final transient Logger logger = LoggerFactory.getLogger(MQTTDelivery.class);
	
	private MqttClient client;
	private String serverURI;
	private String topic;
	private String vsname;
	private boolean closed = false;
	private MqttConnectOptions options  = new MqttConnectOptions();

	public MQTTDelivery(String serverURI, String clientID, String topic, String vsname) {
		try{
		    client = new MqttClient(serverURI, clientID);
		    options.setAutomaticReconnect(true);
		    client.connect(options);
		}catch (Exception e){
			logger.error("Unable to instanciate delivery system MQTT.", e);
		}
	}


	@Override
	public void writeStructure(DataField[] fields) throws IOException {
		StreamElement se = new StreamElement(fields, new Integer[fields.length]);
		try {
			client.publish(topic, se.toJSON(vsname).getBytes(), 0, true);
		} catch (MqttException e) {
			logger.error("Unable to publish stream element to topic " + topic + " on "+ serverURI);
		}
	}

	@Override
	public boolean writeStreamElement(StreamElement se) {
		try {
			client.publish(topic, se.toJSON(vsname).getBytes(), 0, false);
		} catch (MqttException e) {
			logger.error("Unable to publish stream element to topic " + topic + " on "+ serverURI);
			return false;
		}
		return true;
	}

	@Override
	public boolean writeKeepAliveStreamElement() {
		//The client takes care of keep-alive
		return true;
	}

	@Override
	public void close() {
		try {
			client.disconnect();
			client.close();
			closed = true;
		} catch (MqttException e) {
			logger.warn("Error while closing the MQTT client.", e);
		}	
	}

	@Override
	public boolean isClosed() {
		return closed;
	}
}
