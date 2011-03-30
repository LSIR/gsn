package gsn.wrappers.backlog.plugins;


import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

import javax.naming.OperationNotSupportedException;

import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.wrappers.BackLogWrapper;
import gsn.wrappers.backlog.BackLogMessage;
import gsn.wrappers.backlog.BackLogMessageListener;


/**
 * The PluginInterface specifies the functionality a plugin used by
 * {@link BackLogWrapper} has to offer.
 * 
 * @author Tonio Gsell
 */
public abstract class AbstractPlugin extends Thread implements BackLogMessageListener {
	
	private static final int DEFAULT_PACKET_SEND_PRIORITY = 10;
	private static final int DEFAULT_ACK_PRIORITY = 90;

	protected BackLogWrapper activeBackLogWrapper = null;
	protected Integer priority = null;

    private long recvMsgCounter = 0;
    private long recvMsgVolume = 0;
    private long sendMsgCounter = 0;
    private long sendMsgVolume = 0;
    private long sendAckCounter = 0;


	/**
	 * Initialize the plugin.
	 * <p>
	 * This function will be called once at the initialization time
	 * of GSN by the {@link BackLogWrapper}. If it is overwritten by the
	 * plugin, 'activeBackLogWrapper' has to be set by the plugins initialize
	 * function and a listener has to be registered if messages should be
	 * received.
	 * 
	 * @param backLogWrapper points to the calling
	 * 			BackLogWrapper. It can be used
	 * 			to access its functionality.
	 * 
	 * @return true if the initialization was successful otherwise
	 * 			 false
	 */
	public boolean initialize ( BackLogWrapper backLogWrapper, String coreStationName, String deploymentName ) {
		activeBackLogWrapper = backLogWrapper;
		String p = getActiveAddressBean().getPredicateValue("priority");
		if (p == null)
			priority = null;
		else
			priority = Integer.valueOf(p);
		registerListener();
		return true;
	}
	
	
	public void registerListener(BackLogMessageListener listener) {
		activeBackLogWrapper.getBLMessageMultiplexer().registerListener(getMessageType(), listener, true);
	}
	
	
	public void registerListener() {
		activeBackLogWrapper.getBLMessageMultiplexer().registerListener(getMessageType(), this, true);
	}
	
	
	public void deregisterListener() {
		activeBackLogWrapper.getBLMessageMultiplexer().deregisterListener(getMessageType(), this, true);
	}
	
	
	public void deregisterListener(BackLogMessageListener listener) {
		activeBackLogWrapper.getBLMessageMultiplexer().deregisterListener(getMessageType(), listener, true);
	}
	
	
	public void dispose() {
		deregisterListener();
	}
	
	
	public abstract String getPluginName();
	
	
	public DataField[] getOutFormat() {
		return concat(getOutputFormat(), new DataField[] {
			new DataField("recv_msg_counter", DataTypes.BIGINT),
			new DataField("recv_msg_volume", DataTypes.BIGINT),
			new DataField("send_msg_counter", DataTypes.BIGINT),
			new DataField("send_msg_volume", DataTypes.BIGINT),
			new DataField("send_ack_counter", DataTypes.BIGINT)});
	}
	
	
	public void messageReceived(int volume) {
		recvMsgCounter += 1;
		recvMsgVolume += volume;
	}
	

    public boolean messageReceived(int deviceId, BackLogMessage msg) {
		recvMsgCounter += 1;
		recvMsgVolume += msg.getSize();
		return messageReceived(deviceId, msg.getTimestamp(), msg.getPayload());
    }


	/**
	 * This function is called if the remote connection to the deployment has
	 * been established.
     * 
     * @param deviceID the device ID of the connecting CoreStation
	 */
	public void remoteConnEstablished(Integer deviceID) { }


	/**
	 * This function is called if the remote connection to the deployment has
	 * been lost.
	 */
	public void remoteConnLost() { }


	/**
	 * With this function any command can be sent to the plugin.
	 * <p>
	 * A virtual sensor can send a command to this plugin using
	 * {@link BackLogWrapper#sendToWrapper(String, String[], Object[]) sendToWrapper},
	 * which will just forward the command to the plugin.
	 * 
	 * This can be either a command directed at the plugin itself
	 * or a command which can be used to send something to the
	 * remote sensor.
	 * 
	 * @param action the action name
	 * @param paramNames the name of the different parameters
	 * @param paramValues the different parameter values
	 * 
	 * @return true if the plugin could successfully process the
	 * 			 data otherwise false
	 * @throws OperationNotSupportedException 
	 */
	public boolean sendToPlugin ( String action , String [ ] paramNames , Object [ ] paramValues ) throws OperationNotSupportedException {
		throw new OperationNotSupportedException( "This plugin doesn't support sending data back to the source." );
	}


	/**
	 * With this function any object can be sent to the plugin.
	 * <p>
	 * A virtual sensor can send an object to this plugin using the
	 * {@link BackLogWrapper#sendToWrapper(Object) sendToWrapper}, which
	 * will just forward the object to the plugin.
	 * 
	 * This can be either an object directed at the plugin itself
	 * or an object which can be used to send something to the
	 * remote sensor.
	 * 
	 * @param dataItem to be processed
	 * 
	 * @return true if the plugin could successfully process the
	 * 			 data otherwise false
	 */
	public boolean sendToPlugin(Object dataItem) throws OperationNotSupportedException {
		throw new OperationNotSupportedException( "This plugin doesn't support sending data back to the source." );
	}


	/**
	 * Get the {@link gsn.wrappers.backlog.BackLogMessage BackLogMessage} type this plugin is using.
	 * <p>
	 * This function should be implemented as following:
	 * <ul>
	 *  public byte getMessageType() {
	 *  <ul>
	 *   return gsn.wrappers.backlog.BackLogMessage.<i>MESSAGENAME</i>_MESSAGE_TYPE;
	 *  </ul>
	 *  }
	 * </ul>
	 * 
	 * where <i>MESSAGENAME</i> should be a unique name of the plugin.
	 * 
	 * <i>MESSAGENAME</i>_MESSAGE_TYPE has to be implemented and documented in
	 * BackLogMessage.
	 * <p>
	 * @return the message type
	 */
	public abstract byte getMessageType();


	/**
	 * This function is used to specify the output structure of
	 * the data this plugin produces.
	 * <p>
	 * The output structure must agree with the data produced by
	 * this plugin, meaning that the data passed to
	 * {@link BackLogWrapper#dataProcessed(long, java.io.Serializable...) dataProcessed} must
	 * agree with it.
	 * <p>
	 * For further information about the output structure please
	 * refer to GSN-Wrapper's <i>getOutputFormat</i> documentation.
	 * 
	 * @return the output structure of the plugin in a DataField
	 */
	public abstract DataField[] getOutputFormat();
    

	
    /**
     * This method is called to signal message reception. It must be
     * implemented by any plugin.
	 *
     * @param deviceId the DeviceId the message has been received from
     * @param timestamp contained in the message {@link BackLogMessage}
     * @param data of the message
     * 
     * @return true, if the plugin did acknowledge the message
     */
    public abstract boolean messageReceived(int deviceId, long timestamp, Serializable[] data);




	/**
	 * This function can be called by the plugin, if it has processed
	 * the received data from the deployment.
	 * The data will be forwarded to the corresponding
	 * virtual sensor by GSN and will be put into the database.
	 * <p>
	 * The data format must correspond to the one specified by
	 * the plugin's getOutputFormat() function.
	 * 
	 * @param timestamp
	 * 			The timestamp in milliseconds this data has been
	 * 			generated.
	 * @param nrProcessedMsg
     *          The number of received backlog messages which have been
	 *          processed to generate this stream element.
	 * @param bytesrecv
     *          The number of received bytes which have been processed
	 *          to generate this stream element.
	 * @param data 
	 * 			The data to be processed. Its format must correspond
	 * 			to the one specified by the plugin's getOutputFormat()
	 * 			function.
	 * @return false if storing the new item fails otherwise true
	 */
	public boolean dataProcessed(long timestamp, Serializable... data) {
		return activeBackLogWrapper.dataProcessed(timestamp, concat(data, new Serializable[]{recvMsgCounter, recvMsgVolume, sendMsgCounter, sendMsgVolume, sendAckCounter}));
	}



	/**
	 * This function can be called by the plugin, if it has processed
	 * the data received from GSN or on any other occasion which asks
	 * for sending data to the deployment. The data will be sent to
	 * the same CoreStation this plugin receives data from.
	 * 
	 * 
	 * @param timestamp
	 * 			The timestamp in milliseconds this data has been
	 * 			generated.
	 * @param data 
	 * 			The data to be processed. Its format must correspond
	 * 			to the one specified by the plugin's getOutputFormat()
	 * 			function.
	 * @param priority
	 *          the priority this message has. The smaller the number the higher the
	 *          priority to send this message as soon as possible is. It should be somewhere
	 *          between 10 and 1000. If it is set null, the default priority will be
	 *          used.
	 * @return false if not connected to the deployment
	 * 
	 * @throws IOException if the message length exceeds MAX_PAYLOAD_SIZE+9
	 */
	public boolean sendRemote(long timestamp, Serializable[] data, Integer priority) throws IOException {
		boolean ret;
		BackLogMessage msg = new BackLogMessage(getMessageType(), timestamp, data);
		if (priority == null)
			ret = activeBackLogWrapper.getBLMessageMultiplexer().sendMessage(new BackLogMessage(getMessageType(), timestamp, data), null, DEFAULT_PACKET_SEND_PRIORITY);
		else
			ret = activeBackLogWrapper.getBLMessageMultiplexer().sendMessage(new BackLogMessage(getMessageType(), timestamp, data), null, priority);
		
		if (ret) {
			sendMsgCounter += 1;
			sendMsgVolume += msg.getSize();
		}
		return ret;
	}



	/**
	 * This function can be called by the plugin, if it has processed
	 * the data received from GSN or on any other occasion which asks
	 * for sending data to the deployment. The data can be sent to
	 * any CoreStation at the deployment this plugin is connected
	 * to.
	 * 
	 * 
	 * @param timestamp
	 * 			The timestamp in milliseconds this data has been
	 * 			generated.
	 * @param data 
	 * 			The data to be processed. Its format must correspond
	 * 			to the one specified by the plugin's getOutputFormat()
	 * 			function.
	 * @param id 
	 * 			The id of the CoreStation the message should be sent to.
	 * @param priority
	 *          the priority this message has. The smaller the number the higher the
	 *          priority to send this message as soon as possible is. It should be somewhere
	 *          between 10 and 1000. If it is set null, the default priority will be
	 *          used.
	 * 
	 * @return false if not connected to the deployment
	 * 
	 * @throws IOException if the message length exceeds MAX_PAYLOAD_SIZE+9
	 * 			or the DeviceId is not connected or does not exist.
	 */
	public boolean sendRemote(long timestamp, Serializable[] data, Integer id, Integer priority) throws IOException {
		boolean ret;
		BackLogMessage msg = new BackLogMessage(getMessageType(), timestamp, data);
		if (priority == null)
			ret = activeBackLogWrapper.getBLMessageMultiplexer().sendMessage(msg, id, DEFAULT_PACKET_SEND_PRIORITY);
		else
			ret = activeBackLogWrapper.getBLMessageMultiplexer().sendMessage(msg, id, priority);
		
		if (ret) {
			sendMsgCounter += 1;
			sendMsgVolume += msg.getSize();
		}
		return ret;
	}




	/**
	 * This function must be called by the plugin, to acknowledge
	 * incoming messages if it is using the backlog functionality
	 * on the deployment side.
	 * 
	 * The timestamp will be used at the deployment to remove the
	 * corresponding message backloged in the database. If messages
	 * are not acknowledged by this plugin but its counterpart on
	 * the deployment side will backlog them, we endanger to
	 * overflow the backlog database!
	 * 
	 * @param timestamp
	 * 			The timestamp is used to acknowledge a message. Thus
	 * 			it has to be equal to the timestamp from the received
	 * 			message we want to acknowledge.
	 * @param priority
	 *          the priority this message has. The smaller the number the higher the
	 *          priority to send this message as soon as possible is. It should be somewhere
	 *          between 10 and 1000. If it is set null, the default priority will be
	 *          used.
	 * 
	 * @return false if not connected to the deployment
	 */
	public boolean ackMessage(long timestamp, Integer priority) {
		boolean ret;
		if (priority == null)
			ret = activeBackLogWrapper.getBLMessageMultiplexer().sendAck(timestamp, getMessageType(), DEFAULT_ACK_PRIORITY);
		else
			ret = activeBackLogWrapper.getBLMessageMultiplexer().sendAck(timestamp, getMessageType(), priority);
		
		if (ret)
			sendAckCounter += 1;
		
		return ret;
	}



	/**
	 * Returns true if the connection to the deployment is established.
	 * 
	 * @return true if the connection to the deployment is established
	 */
	public boolean isConnected() {
		return activeBackLogWrapper.getBLMessageMultiplexer().isConnected();
	}
	
	
//	/**
//	 * Retruns true if the deploymentClient is connected to the deployment.
//	 * 
//	 * @return true if the client is connected otherwise false
//	 */
//	public boolean isConnected() {
//		return activeBackLogWrapper.getBLMessageMultiplexer().isConnected();
//	}
	
	
	public final AddressBean getActiveAddressBean ( ) {
		return activeBackLogWrapper.getActiveAddressBean();
	}


	/**
	 * This function returns the DeviceId of the CoreStation this plugin is connected to
	 * or null if it has not yet been connected to a CoreStation.
	 * 
	 * @return the DeviceId of the connected CoreStation or null if not yet connected.
	 */
	public Integer getDeviceID() {
		return activeBackLogWrapper.getBLMessageMultiplexer().getDeviceID();
	}
	

	public boolean isTimeStampUnique() {
	  return activeBackLogWrapper.isTimeStampUnique();
	}
	
	
	protected static Serializable[] checkAndCastData(Serializable[] data, int dataoffset, DataField[] datafields, int datafieldoffset) throws Exception {
		if (data.length-dataoffset != datafields.length-datafieldoffset)
			throw new Exception("data length does not correspond with the datafield length");
		
		Serializable [] ret = new Serializable [data.length-dataoffset];
		for (int i=dataoffset; i<data.length; i++) {
			int type = datafields[i-dataoffset+datafieldoffset].getDataTypeID();
			switch (type) {
			case DataTypes.INTEGER:
				ret[i-dataoffset] = toInteger(data[i]);
				break;
			case DataTypes.BIGINT:
				ret[i-dataoffset] = toLong(data[i]);
				break;
			case DataTypes.SMALLINT:
				ret[i-dataoffset] = toShort(data[i]);
				break;
			case DataTypes.TINYINT:
				ret[i-dataoffset] = toShort(data[i]);
				break;
			case DataTypes.DOUBLE:
				ret[i-dataoffset] = (Double)data[i];
				break;
			default:
				ret[i-dataoffset] = data[i];
				break;
			}
		}
		
		return ret;
	}
	
	
	public static <T> T[] concat(T[] first, T[] second) {
		T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}
	
	
	protected static <T> Long toLong(T value) throws Exception {
		if (value == null)
			return null;
		else if (value instanceof Byte)
			return new Long((Byte)value & 0xFF);
		else if (value instanceof Short)
			return new Long((Short)value);
		else if (value instanceof Integer)
			return new Long((Integer)value);
		else if (value instanceof Long)
			return (Long) value;
		else
			throw new Exception("value can not be cast to Long.");
	}
	
	
	protected static <T> Integer toInteger(T value) throws Exception {
		if (value == null)
			return null;
		else if (value instanceof Byte)
			return new Integer((Byte)value & 0xFF);
		else if (value instanceof Short)
			return new Integer((Short)value);
		else if (value instanceof Integer)
			return (Integer) value;
		else
			throw new Exception("value can not be cast to Integer.");
	}
	
	
	protected static <T> Short toShort(T value) throws Exception {
		if (value == null)
			return null;
		else if (value instanceof Byte)
			return (new Integer(((Byte)value & 0xFF))).shortValue();
		else if (value instanceof Short)
			return (Short) value;
		else
			throw new Exception("value can not be cast to Short.");
	}
}

class NameDataFieldPair {
	protected Integer typeNumber;
	protected DataField[] dataField;
	
	NameDataFieldPair(Integer typeNumber, DataField[] dataField) {
		this.typeNumber = typeNumber;
		this.dataField = dataField;
	}
}
