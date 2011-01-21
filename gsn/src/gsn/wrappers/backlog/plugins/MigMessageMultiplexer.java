package gsn.wrappers.backlog.plugins;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;

import net.tinyos.message.SerialPacket;
import net.tinyos.packet.Serial;
import net.tinyos1x.message.TOSMsg;
import gsn.wrappers.backlog.BackLogMessage;
import gsn.wrappers.backlog.BackLogMessageListener;
import gsn.wrappers.backlog.BackLogMessageMultiplexer;
import gsn.wrappers.backlog.sf.SFListen;
import gsn.wrappers.backlog.sf.SFv1Listen;

public class MigMessageMultiplexer implements BackLogMessageListener {

	private static final String SF_LOCAL_PORT = "local-sf-port";
	protected static final String TINYOS1X_PLATFORM = "tinyos1x-platform";
	
	

	private final transient Logger logger = Logger.getLogger( MigMessageMultiplexer.class );

	private Map<Integer,Vector<MigMessagePlugin>> msgTypeListener = new HashMap<Integer,Vector<MigMessagePlugin>> (); // Mapping from type to Listener

	private static Map<String,MigMessageMultiplexer> migMsgMultiplexerMap = new HashMap<String,MigMessageMultiplexer>();
	
	private String tinyos1x_platform = null;
	private SFListen sfListen = null;
	private SFv1Listen sfv1Listen = null;
	
	private String coreStationName = null;
	private String deploymentName = null;
	private BackLogMessageMultiplexer blMessageMultiplexer;
	
	
	public MigMessageMultiplexer() throws Exception {
		throw new Exception("use 'getInstance(String deployment, String tinyosplatform)' function for instantiation");
	}
	
	
	private MigMessageMultiplexer(String coreStationName, String deploymentName, Properties props, BackLogMessageMultiplexer blMsgMulti) throws Exception {
		this.coreStationName = coreStationName;
		this.deploymentName = deploymentName;
		blMessageMultiplexer = blMsgMulti;
		tinyos1x_platform = props.getProperty(TINYOS1X_PLATFORM);
		String sflocalport = props.getProperty(SF_LOCAL_PORT);
		
		// start optional local serial forwarder
		if (tinyos1x_platform == null) {
			if (sflocalport != null) {
				int port = -1;
				try {
					port = Integer.parseInt(sflocalport);
					logger.info("initializing local serial forwarder on port " + port + " for deployment: >" + coreStationName + "<");
					sfListen = SFListen.getInstance(port, blMsgMulti, coreStationName, deploymentName);
				} catch (Exception e) {
					logger.error("Could not start serial forwarder on port " + port + " for deployment: >" + coreStationName + "<");							
				}
			}
			
			blMsgMulti.registerListener(BackLogMessage.TOS_MESSAGE_TYPE, this, true);
		} else {
			if (sflocalport != null) {
				int port = -1;
				try {
					port = Integer.parseInt(sflocalport);
					logger.info("initializing local serial forwarder 1.x on port " + port + " for deployment: >" + coreStationName + "<");
					sfv1Listen = SFv1Listen.getInstance(port, blMsgMulti, tinyos1x_platform, coreStationName, deploymentName);
				} catch (Exception e) {
					logger.error("Could not start serial forwarder 1.x on port " + port + " for deployment: >" + coreStationName + "<");							
				}
			}
			
			blMsgMulti.registerListener(BackLogMessage.TOS1x_MESSAGE_TYPE, this, true);
		}
		
		if (sfListen != null && !sfListen.isAlive()) {
			sfListen.start();
		}
		else if (sfv1Listen != null && !sfv1Listen.isAlive()) {
			sfv1Listen.start();
		}
	}
	
	
	public synchronized static MigMessageMultiplexer getInstance(String coreStationName, String deploymentName, Properties props, BackLogMessageMultiplexer blMsgMulti) throws Exception {
		if(migMsgMultiplexerMap.containsKey(coreStationName)) {
			return migMsgMultiplexerMap.get(coreStationName);
		}
		else {
			MigMessageMultiplexer blMulti = new MigMessageMultiplexer(coreStationName, deploymentName, props, blMsgMulti);
			migMsgMultiplexerMap.put(coreStationName, blMulti);
			return blMulti;
		}
	}
	
	
	/**
	 * Register a particular listener for a specific message type. More than one
	 * listener can be registered for each message type.
	 * 
	 * @param msgType
	 *          specify message type we're listening for
	 * @param listener
	 *          destination for received messages
	 */
	public synchronized void registerListener(int msgType, MigMessagePlugin listener) {
		Integer msgTypeInt = new Integer(msgType);
	    Vector<MigMessagePlugin> vec = msgTypeListener.get(msgTypeInt);
	    if (vec == null) {
	      vec = new Vector<MigMessagePlugin>();
	    }
	    vec.addElement(listener);
	    msgTypeListener.put(msgTypeInt, vec);

		if (logger.isDebugEnabled())
			logger.debug("Listener for mig message type " + msgType + " registered");
	}

	
	/**
	 * Stop listening for messages of the given type with the given listener.
	 * 
	 * @param msgType
	 *          specify message type we're listening for
	 * @param listener
	 *          the listener we want to deregister
	 * @return false if no more listeners are available
	 */
	public synchronized boolean deregisterListener(int msgType, MigMessagePlugin listener) {
		Integer msgTypeInt = new Integer(msgType);
		Vector<MigMessagePlugin> vec = msgTypeListener.get(msgTypeInt);
		if (vec == null) {
			throw new IllegalArgumentException( "No listeners registered for mig message type " + msgType);
		}
		// Remove all occurrences
		while (vec.removeElement(listener));
		if (vec.size() == 0)
			msgTypeListener.remove(msgTypeInt);

		if (logger.isDebugEnabled())
			logger.debug("Listener for mig message type " + msgTypeInt + " deregistered");
		
		if (msgTypeListener.size() == 0) {
			dispose();
			return false;
		}
	    
	    return true;
	}
	
	
	public void dispose() {
		logger.info("dispose called");
		migMsgMultiplexerMap.remove(coreStationName);
		
		logger.info("close SF listener for " + deploymentName + " deployment");
		if (sfListen != null)
			sfListen.dispose(coreStationName);
		else if (sfv1Listen != null)
			sfv1Listen.dispose(coreStationName);

		if (tinyos1x_platform == null)
			blMessageMultiplexer.deregisterListener(BackLogMessage.TOS_MESSAGE_TYPE, this, true);
		else
			blMessageMultiplexer.deregisterListener(BackLogMessage.TOS1x_MESSAGE_TYPE, this, true);
	}
	
	
	@Override
	public boolean messageReceived(int deviceID, long timestamp, Serializable[] data) {
		// which TinyOS messages are we looking for?
		if (tinyos1x_platform != null) {
			// the following functionality has been extracted from net.tinyos1x.message.Receiver
			// from the packetReceived function
			
			// create a TOS message (TinyOS1.x)
			final TOSMsg msg = createTOSMsg ( (byte[]) data[0] ) ;

			Integer type = new Integer ( msg.get_type () );

			net.tinyos1x.message.Message received ;
			int length = msg.get_length () ;

			try {
				// clone the message for further processing
				received = msg.clone( length ) ;
				received.dataSet ( msg.dataGet () , msg.offset_data ( 0 ) , 0 , length ) ;
			}
			catch ( ArrayIndexOutOfBoundsException e ) {
				/*
				 * Note: this will not catch messages whose length is incorrect,
				 * but less than DATA_LENGTH (see AM.h) + 2
				 */
				logger.error( "invalid length message received (too long)" ) ;
				return false;
			}
			catch ( Exception e ) {
				logger.error( "couldn't clone message!, TinyOS 1x" ) ;
				return false;
			}
			
			int ReceiverCount = 0;
			Vector<MigMessagePlugin> vec = msgTypeListener.get(type);
			if (vec == null) {
				logger.warn("Received message with type " + type + ", but no listeners registered. Skip message.");
				return false;
			}
		
			Enumeration<MigMessagePlugin> en = vec.elements();
			// send the message to all listeners
			while (en.hasMoreElements()) {
				MigMessagePlugin temp = en.nextElement();
				
				// send the message to the listener
				if (temp.messageReceived(deviceID, timestamp, new Serializable[] {received.dataGet()}) == true)
					ReceiverCount++;
			}
			if (ReceiverCount == 0)
				logger.warn("Received message with type " + type + ", but none of the registered listeners did process it. Skip message.");
			return true;
		}
		else {
			// the following functionality has been extracted from net.tinyos.message.Receiver
			// from the packetReceived function
			
			if (((byte[]) data[0])[0] != Serial.TOS_SERIAL_ACTIVE_MESSAGE_ID)
				return false; // not for us.
	
			// create a SerialPacket message
			SerialPacket msg = new SerialPacket((byte[]) data[0], 1);
			int type = msg.get_header_type();

		    int length = msg.get_header_length();
		    net.tinyos.message.Message received;
		    try {
				// clone the message for further processing
			    received = msg.clone(length);
			    received.dataSet(msg.dataGet(), SerialPacket.offset_data(0) + msg.baseOffset(), 0, length);
		    } catch (ArrayIndexOutOfBoundsException e) {
		    	logger.error("invalid length message received (too long)");
		    	return false;
		    } catch (Exception e) {
		    	logger.error("couldn't clone message!");
		    	return false;
		    }
			
			int ReceiverCount = 0;
			Vector<MigMessagePlugin> vec = msgTypeListener.get(type);
			if (vec == null) {
				logger.warn("Received message with type " + type + ", but no listeners registered. Skip message.");
				return false;
			}
		
			Enumeration<MigMessagePlugin> en = vec.elements();
			// send the message to all listeners
			while (en.hasMoreElements()) {
				MigMessagePlugin temp = en.nextElement();
				
				// send the message to the listener
				if (temp.messageReceived(deviceID, timestamp, new Serializable[] {received.dataGet()}) == true)
					ReceiverCount++;
			}
			if (ReceiverCount == 0)
				logger.warn("Received message with type " + type + ", but none of the registered listeners did process it. Skip message.");
			return true;
		}
	}

	@Override
	public void remoteConnEstablished() { }

	@Override
	public void remoteConnLost() {	}
	
//	
// following functions are only used for TinyOS1.x messages	
//
	
	TOSMsg instantiateTOSMsg ( Class<?> [] cArgs , Object [] args ) {
	   	try {
		   	Class<?> msgCls ;
         
		   	msgCls = Class.forName ( "net.tinyos1x.message." + tinyos1x_platform + ".TOSMsg" ) ;
         
		   	Constructor<?> c = msgCls.getConstructor ( cArgs ) ;
		   	return (TOSMsg) c.newInstance ( args ) ;
	   	}
	   	catch ( ClassNotFoundException e ) {
		   	System.err.println ( "Could not find a platform specific version of TOSMsg" ) ;
		   	System.err.println ( e ) ;
		   	e.printStackTrace () ;
	   	}
	   	catch ( NoSuchMethodException e ) {
		   	System.err.println ( "Could not locate the appropriate constructor; check the class " + "net.tinyos1x.message." + tinyos1x_platform
                              + ".TOSMsg" ) ;
		   	e.printStackTrace () ;
	   	}
	   	catch ( InstantiationException e ) {
		   	System.err.println ( "Could not instantiate the class: " + e ) ;
		   	e.printStackTrace () ;
      	}
      	catch ( IllegalAccessException e ) {
    	  	System.err.println ( "Illegal access: " + e ) ;
         	e.printStackTrace () ;
      	}
      	catch ( InvocationTargetException e ) {
      		System.err.println ( "Reflection problems: " + e ) ;
         	e.printStackTrace () ;
      	}
      	return null ;
   	}

   	public TOSMsg createTOSMsg ( byte data[] ) {
   		Object initArgs[] = new Object [ 1 ] ;
   		Class<?> cArgs[] = new Class [ 1 ] ;
   		cArgs [ 0 ] = data.getClass () ;
   		initArgs [ 0 ] = data ;

   		return instantiateTOSMsg ( cArgs , initArgs ) ;
   	}
}
