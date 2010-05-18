package gsn.wrappers.backlog.plugins;


import java.io.IOException;
import java.io.Serializable;

import javax.naming.OperationNotSupportedException;

import gsn.beans.AddressBean;
import gsn.beans.DataField;
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
	
	private static final int DEFAULT_PRIORITY = 99;

	protected BackLogWrapper activeBackLogWrapper = null;
	protected Integer priority = null;
	private volatile Thread pluginThread;


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
		priority = Integer.valueOf(getActiveAddressBean().getPredicateValue("priority"));
		registerListener();
		return true;
	}

	
	public void start() {
        pluginThread = new Thread(this);
        pluginThread.start();
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
	
	
	public void run() { }
	
	
	public void dispose() {
		deregisterListener();
	}
	
	
	public abstract String getPluginName();


	/**
	 * This function is called if the remote connection to the deployment has
	 * been established.
	 */
	public void remoteConnEstablished() { }


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
	 * @param data 
	 * 			The data to be processed. Its format must correspond
	 * 			to the one specified by the plugin's getOutputFormat()
	 * 			function.
	 * @return false if storing the new item fails otherwise true
	 */
	public boolean dataProcessed(long timestamp, Serializable... data) {
		return activeBackLogWrapper.dataProcessed(timestamp, data);
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
	public boolean sendRemote(long timestamp, byte[] data, Integer priority) throws Exception {
		if (priority == null)
			return activeBackLogWrapper.getBLMessageMultiplexer().sendMessage(new BackLogMessage(getMessageType(), timestamp, data), null, DEFAULT_PRIORITY);
		else
			return activeBackLogWrapper.getBLMessageMultiplexer().sendMessage(new BackLogMessage(getMessageType(), timestamp, data), null, priority);
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
	 * 			or the DeviceId does not exist.
	 */
	public boolean sendRemote(long timestamp, byte[] data, Integer id, Integer priority) throws Exception {
		if (priority == null)
			return activeBackLogWrapper.getBLMessageMultiplexer().sendMessage(new BackLogMessage(getMessageType(), timestamp, data), id, DEFAULT_PRIORITY);
		else
			return activeBackLogWrapper.getBLMessageMultiplexer().sendMessage(new BackLogMessage(getMessageType(), timestamp, data), id, priority);
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
	 */
	public void ackMessage(long timestamp, Integer priority) {
		if (priority == null)
			activeBackLogWrapper.getBLMessageMultiplexer().sendAck(timestamp, DEFAULT_PRIORITY);
		else
			activeBackLogWrapper.getBLMessageMultiplexer().sendAck(timestamp, priority);
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
	 * This function returns the DeviceId of the CoreStation this plugin is connected to.
	 * 
	 * @return the DeviceId of the connected CoreStation
	 */
	public Integer getDeviceID() {
		return activeBackLogWrapper.getBLMessageMultiplexer().getDeviceID();
	}
	

	public boolean isTimeStampUnique() {
	  return activeBackLogWrapper.isTimeStampUnique();
	}
		
	public static byte[] uint2arr (long l) {
		int len = 4;
		byte[] arr = new byte[len];

		int i = 0;
		for ( int shiftBy = 0; shiftBy < 32; shiftBy += 8 ) {
			arr[i] = (byte)( l >> shiftBy);
			i++;
		}
		return arr;
	}
	
	public static long arr2uint (byte[] arr, int start) {
		int i = 0;
		int len = 4;
		int cnt = 0;
		byte[] tmp = new byte[len];
		for (i = start; i < (start + len); i++) {
			tmp[cnt] = arr[i];
			cnt++;
		}
		long accum = 0;
		i = 0;
		for ( int shiftBy = 0; shiftBy < 32; shiftBy += 8 ) {
			accum |= ( (long)( tmp[i] & 0xff ) ) << shiftBy;
			i++;
		}
		return accum;
	}
	
	public static int arr2int (byte[] arr, int start) {
		int i = 0;
		int len = 4;
		int cnt = 0;
		byte[] tmp = new byte[len];
		for (i = start; i < (start + len); i++) {
			tmp[cnt] = arr[i];
			cnt++;
		}
		int accum = 0;
		i = 0;
		for ( int shiftBy = 0; shiftBy < 32; shiftBy += 8 ) {
			accum |= ( (int)( tmp[i] & 0xff ) ) << shiftBy;
			i++;
		}
		return accum;
	}
		
	public static byte[] int2arr (int l) {
		int len = 4;
		byte[] arr = new byte[len];

		int i = 0;
		for ( int shiftBy = 0; shiftBy < 32; shiftBy += 8 ) {
			arr[i] = (byte)( l >> shiftBy);
			i++;
		}
		return arr;
	}

	
	public static long arr2long (byte[] arr, int start) {
		int i = 0;
		int len = 8;
		int cnt = 0;
		byte[] tmp = new byte[len];
		for (i = start; i < (start + len); i++) {
			tmp[cnt] = arr[i];
			cnt++;
		}
		long accum = 0;
		i = 0;
		for ( int shiftBy = 0; shiftBy < 64; shiftBy += 8 ) {
			accum |= ( (long)( tmp[i] & 0xff ) ) << shiftBy;
			i++;
		}
		return accum;
	}

	
	public static byte[] long2arr (long l) {
		int len = 8;
		byte[] arr = new byte[len];

		int i = 0;
		for ( int shiftBy = 0; shiftBy < 64; shiftBy += 8 ) {
			arr[i] = (byte)( l >> shiftBy);
			i++;
		}
		return arr;
	}
}
