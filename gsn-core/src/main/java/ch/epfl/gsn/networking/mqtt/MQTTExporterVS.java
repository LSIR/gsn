package ch.epfl.gsn.networking.mqtt;

import java.util.TreeMap;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.epfl.gsn.beans.StreamElement;
import ch.epfl.gsn.vsensor.AbstractVirtualSensor;

public class MQTTExporterVS extends AbstractVirtualSensor {
	
	private static transient Logger logger = LoggerFactory.getLogger(MQTTExporterVS.class);
	
	private MqttClient client;
	private String serverURI;
	private String clientID;
	private String topic;
	private MqttConnectOptions options = new MqttConnectOptions();

	public MQTTExporterVS() {}

	@Override
	public boolean initialize() {
		
		TreeMap<String, String> params = getVirtualSensorConfiguration().getMainClassInitialParams();

        serverURI = params.get("uri");
        if (serverURI == null) {
            logger.error("Parameter uri not provided in Virtual Sensor file.");
            return false;
        }
        clientID = params.get("client_id");
        if (clientID == null) {
            logger.warn("Parameter client_id not provided in Virtual Sensor file, using random one.");
            clientID = MqttClient.generateClientId();
        }
        topic = params.get("topic");
        if (topic == null) {
            logger.warn("Parameter topic not provided in Virtual Sensor file, using virtual sensor name.");
            topic = getVirtualSensorConfiguration().getName();
        }
		
		try{
		    client = new MqttClient(serverURI, clientID);
		    options.setAutomaticReconnect(true);
		    client.connect(options);
		}catch (Exception e){
			logger.error("Unable to connect to MQTT server.", e);
			return false;
		}
		return true;
	}

	@Override
	public void dispose() {
		try {
			client.disconnect();
			client.close();
		} catch (MqttException e) {
			logger.warn("Error while closing the MQTT client.", e);
		}
	}

	@Override
	public void dataAvailable(String inputStreamName, StreamElement streamElement) {
		try {
			//to adapt according to content to be sent...
			client.publish(topic, ((byte[])streamElement.getData("raw_packet")), 0, false);
		} catch (MqttException e) {
			logger.warn("Error while sending stream element to the MQTT server.", e);
		}
		dataProduced(streamElement);
		

	}

}
