package gsn.wrappers.backlog.plugins;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;

import gsn.wrappers.backlog.BackLogMessage;
import gsn.wrappers.backlog.BackLogMessageListener;
import gsn.wrappers.backlog.BackLogMessageMultiplexer;

public class DPPMessageMultiplexer implements BackLogMessageListener {
	
	private final transient Logger logger = Logger.getLogger( DPPMessageMultiplexer.class );

	private Map<Integer,Vector<DPPMessagePlugin>> msgTypeListener = new HashMap<Integer,Vector<DPPMessagePlugin>> (); // Mapping from type to Listener

	private static Map<String,DPPMessageMultiplexer> dppMsgMultiplexerMap = new HashMap<String,DPPMessageMultiplexer>();
	
	private String coreStationName = null;
	private BackLogMessageMultiplexer blMessageMultiplexer;
	
	
	public DPPMessageMultiplexer() throws Exception {
		throw new Exception("use 'getInstance(String deployment)' function for instantiation");
	}
	
	
	private DPPMessageMultiplexer(String coreStationName, BackLogMessageMultiplexer blMsgMulti) throws Exception {
		this.coreStationName = coreStationName;
		blMessageMultiplexer = blMsgMulti;
		
		blMsgMulti.registerListener(BackLogMessage.DPP_MESSAGE_TYPE, this, true);
	}
	
	
	public synchronized static DPPMessageMultiplexer getInstance(String coreStationName, BackLogMessageMultiplexer blMsgMulti) throws Exception {
		if(dppMsgMultiplexerMap.containsKey(coreStationName)) {
			return dppMsgMultiplexerMap.get(coreStationName);
		}
		else {
			DPPMessageMultiplexer blMulti = new DPPMessageMultiplexer(coreStationName, blMsgMulti);
			dppMsgMultiplexerMap.put(coreStationName, blMulti);
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
	public synchronized void registerListener(int msgType, DPPMessagePlugin listener) {
		Integer msgTypeInt = new Integer(msgType);
	    Vector<DPPMessagePlugin> vec = msgTypeListener.get(msgTypeInt);
	    if (vec == null) {
	      vec = new Vector<DPPMessagePlugin>();
	    }
	    vec.addElement(listener);
	    msgTypeListener.put(msgTypeInt, vec);

		if (logger.isDebugEnabled())
			logger.debug("Listener for DPP message type " + msgType + " registered");
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
	public synchronized boolean deregisterListener(int msgType, DPPMessagePlugin listener) {
		Integer msgTypeInt = new Integer(msgType);
		Vector<DPPMessagePlugin> vec = msgTypeListener.get(msgTypeInt);
		if (vec == null) {
			throw new IllegalArgumentException( "No listeners registered for DPP message type " + msgType);
		}
		// Remove all occurrences
		while (vec.removeElement(listener));
		if (vec.size() == 0)
			msgTypeListener.remove(msgTypeInt);

		if (logger.isDebugEnabled())
			logger.debug("Listener for DPP message type " + msgTypeInt + " deregistered");
		
		if (msgTypeListener.size() == 0) {
			dispose();
			return false;
		}
	    
	    return true;
	}
	
	
	public void dispose() {
		logger.info("dispose called");
		dppMsgMultiplexerMap.remove(coreStationName);
		
		blMessageMultiplexer.deregisterListener(BackLogMessage.DPP_MESSAGE_TYPE, this, true);
	}
	
	
	@Override
	public boolean messageRecv(int deviceID, BackLogMessage message) {
		try {
			int type = gsn.wrappers.backlog.plugins.AbstractPlugin.toInteger(message.getPayload()[2]);
		
			int ReceiverCount = 0;
			Vector<DPPMessagePlugin> vec = msgTypeListener.get(type);
			if (vec == null) {
				logger.warn("Received message with type " + type + ", but no listeners registered. Skip message.");
				return false;
			}
		
			Enumeration<DPPMessagePlugin> en = vec.elements();
			// send the message to all listeners
			while (en.hasMoreElements()) {
				DPPMessagePlugin temp = en.nextElement();
	
				// create statistics event
				try {
					temp.activeBackLogWrapper.inputEvent(System.currentTimeMillis(), temp.activeBackLogWrapper.getRemoteConnectionPoint(), message.getSize());
				} catch (IOException e) {
					logger.error(e.getMessage());
				}
				// send the message to the listener
				if (temp.messageReceived(deviceID, message.getTimestamp(), message.getPayload()) == true)
					ReceiverCount++;
			}
			if (ReceiverCount == 0) {
				logger.warn("Received message with type " + type + ", but none of the registered listeners did process it. Skip message.");
				return false;
			}
			return true;
		} catch (Exception e) {
			logger.error(e.getMessage());
			return false;
		}
	}

	@Override
	public void remoteConnEstablished(Integer deviceID) { }

	@Override
	public void remoteConnLost() {	}

}
