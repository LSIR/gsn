package gsn.wrappers.backlog;

import gsn.wrappers.backlog.plugins.AbstractPlugin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class BackLogMessageMultiplexer extends Thread implements CoreStationListener {

	/** Ping request interval in seconds. */
	public static final int PING_INTERVAL_SEC = 10;
	/** Time in seconds in which at least one ping acknowledge
	    message should have been received. If no acknowledge has
	    been received, the connection is considered broken. */
	public static final int PING_ACK_CHECK_INTERVAL_SEC = 60;
	
	public static final int PLUGIN_MESSAGE_QUEUE_SIZE = 1000;
	
	public static final byte STUFFING_BYTE = 0x7e;
	
	public static final byte HELLO_BYTE = 0x7d;
	
	protected final transient Logger logger = Logger.getLogger( BackLogMessageMultiplexer.class );
	

	private static Map<String,BackLogMessageMultiplexer> blMultiplexerMap = new HashMap<String,BackLogMessageMultiplexer>();

	private Map<Integer,Vector<BackLogMessageListener>> msgTypeListener; // Mapping from type to Listener

	protected AsyncCoreStationClient asyncCoreStationClient = null;
	private BlockingQueue<byte []> recvQueue = new LinkedBlockingQueue<byte[]>();
	private PluginMessageHandler pluginMessageHandler;
	private boolean dispose = false;
	private Integer activPluginCounter = 0;
	private Integer coreStationDeviceId = null;

	private Timer pingTimer = null;
	private Timer pingWatchDogTimer = null;

	private InetAddress inetAddress;
	private int hostPort;
	private String deploymentName;
	boolean stuff = false;
	
	
	public BackLogMessageMultiplexer() throws Exception {
		throw new Exception("use 'getInstance(String CoreStation)' function for instantiation");
	}
	
	
	private BackLogMessageMultiplexer(String deployment, String coreStationAddress) throws Exception {
		msgTypeListener = Collections.synchronizedMap(new HashMap<Integer,Vector<BackLogMessageListener>> ());
		
		// a first pattern match test for >host:port<
    	Matcher m = Pattern.compile("(.*):(.*)").matcher(coreStationAddress);
    	if ( m.find() ) {
			try {
				inetAddress = InetAddress.getByName(m.group(1));
				hostPort = new Integer(m.group(2));
			} catch(Exception e) {
				throw new IOException("Remote BackLog host string does not match >host:port<");
			}
    	}
    	else {
			throw new IOException("Remote BackLog host string does not match >host:port<");
    	}
    	deploymentName = deployment;
    	
    	pluginMessageHandler = new PluginMessageHandler(this, PLUGIN_MESSAGE_QUEUE_SIZE);
    	
    	asyncCoreStationClient = AsyncCoreStationClient.getSingletonObject();
		
		setName("BackLogMessageMultiplexer-" + getCoreStationName() + "-Thread");
	}
	
	
	public synchronized static BackLogMessageMultiplexer getInstance(String deployment, String coreStationAddress) throws Exception {
		if( PING_ACK_CHECK_INTERVAL_SEC <= PING_INTERVAL_SEC )
			throw new Exception("PING_ACK_CHECK_INTERVAL_SEC must be bigger than PING_INTERVAL_SEC");
		
		if(blMultiplexerMap.containsKey(coreStationAddress)) {
			return blMultiplexerMap.get(coreStationAddress);
		}
		else {
			BackLogMessageMultiplexer blMulti = new BackLogMessageMultiplexer(deployment, coreStationAddress);
			blMultiplexerMap.put(coreStationAddress, blMulti);
			return blMulti;
		}
	}
	  
	
	public void processData(byte[] data, int count) {
		byte[] dataCopy = new byte[count];
		System.arraycopy(data, 0, dataCopy, 0, count);
		recvQueue.offer(dataCopy);
	}
	
	
	public void run() {
		logger.info("thread started");
		
		pluginMessageHandler.start();
    	
		try {
			asyncCoreStationClient.registerListener(this);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

		ByteArrayOutputStream pkt = new ByteArrayOutputStream();
		boolean newPacket = true;
		boolean conn = false;
		boolean connecting = true;
		long packetLength = -1;
		while(!dispose) {
			try {
				byte [] in;
				try {
					in = recvQueue.take();
				} catch (InterruptedException e) {
					if (logger.isDebugEnabled())
						logger.debug(e.getMessage());
					break;
				}
				if (dispose)
					break;
				
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				if (!pktDestuffing(in, baos)) {
					logger.warn("stuffing mark reached");
					connecting = true;
					conn = false;
					newPacket = true;
					packetLength = -1;
					pkt = new ByteArrayOutputStream();
					
					if (baos.size() == 0) {
						continue;
					}
				}

				pkt.write(baos.toByteArray());
				
				if (connecting) {
					if (pkt.size() >= 5) {
						byte[] tmp = pkt.toByteArray();
						pkt = new ByteArrayOutputStream();
						
						if (tmp.length > 5)
							pkt.write(java.util.Arrays.copyOfRange(tmp, (int) (5), tmp.length));

						coreStationDeviceId = AbstractPlugin.arr2int(tmp, 1);

						connecting = false;
						if (tmp[0] != HELLO_BYTE) {
							logger.error("connection hello message does not match -> reconnect");
							asyncCoreStationClient.reconnect(this);
							recvQueue.clear();
						}
						else {
							asyncCoreStationClient.addDeviceId(deploymentName, coreStationDeviceId, this);
							connectionFinished();
							conn = true;
						}
					}
				}
				
				boolean hasMorePkt = true;
				while(hasMorePkt && conn) {
					if (newPacket) {
						if (pkt.size() >= 4) {
							if (logger.isDebugEnabled())
								logger.debug("rcv...");
							packetLength = AbstractPlugin.arr2uint(pkt.toByteArray(), 0);
							newPacket = false;
						}
						else
							hasMorePkt = false;
					}
	
					if (!newPacket && pkt.size() >= packetLength+4) {
						byte[] tmp = pkt.toByteArray();
						pkt = new ByteArrayOutputStream();
						newPacket = true;
						
						if (tmp.length > packetLength+4)
							pkt.write(java.util.Arrays.copyOfRange(tmp, (int) (packetLength+4), tmp.length));
	
			    		BackLogMessage msg = null;
						msg = new BackLogMessage(java.util.Arrays.copyOfRange(tmp, 4, (int) (packetLength+4)));
						if (logger.isDebugEnabled())
							logger.debug("rcv (" + msg.getType() + "," + msg.getTimestamp() + "," + msg.getMessage().length + ")");
			    		if( msg.getType() == BackLogMessage.PING_MESSAGE_TYPE ) {
			    			sendPingAck(msg.getTimestamp());
			    		}
			    		else if( msg.getType() == BackLogMessage.PING_ACK_MESSAGE_TYPE )
			    	        resetWatchDog();
			    		else
			    			pluginMessageHandler.newPluginMessage(msg);
					}
					else
						hasMorePkt = false;
				}
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
		logger.info("thread stoped");
	}
	
	
	private boolean pktDestuffing(byte[] in, ByteArrayOutputStream destuffed) {
		for(int i=0; i<in.length; i++) {
			if (in[i] == STUFFING_BYTE && !stuff)
				stuff = true;
			else if (stuff) {
				if (in[i] == STUFFING_BYTE) {
					destuffed.write(in[i]);
					stuff = false;
				}
				else {
					try {
						destuffed.flush();
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
					stuff = false;
					if (in.length-i != 0) {
						pktDestuffing(java.util.Arrays.copyOfRange(in, i, in.length), destuffed);
					}
					
					return false;
				}
			}
			else
				destuffed.write(in[i]);
		}
		return true;
	}


	private void dispose() {
		logger.info("dispose");
    	// stop ping timer
		if (pingTimer != null)
			pingTimer.cancel();
    	// stop ping checker timer
		if (pingWatchDogTimer != null)
			pingWatchDogTimer.cancel();
        
		dispose = true;
		try {
			this.join();
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
		
		blMultiplexerMap.remove(getCoreStationName());
		
		asyncCoreStationClient.deregisterListener(this);
		
		pluginMessageHandler.dispose();
		recvQueue.clear();
	}


	/**
	 * Send the message to the CoreStation with the specific id.
	 * The timestamp field in the BackLogMessage will be overwritten
	 * with System.currentTimeMillis() in this function!
	 * 
	 * @param message
	 *          a BackLogMessage to be sent to the CoreStation
	 *          
	 * @param id
	 *          the id of the CoreStation the message should be sent to or null
	 *          if the message should be sent to the CoreStation this BackLogMessageMultiplexer
	 *          is connected to
	 *          
	 * @param priority
	 *          the priority this message has. The smaller the number the higher the
	 *          priority to send this message as soon as possible is. It should be somewhere
	 *          between 10 and 1000.
	 *          
	 * @return false if the connection to the CoreStation is not established
	 * 
	 * @throws IOException if the message is too long or the DeviceId does not exist
	 */
	public synchronized boolean sendMessage(BackLogMessage message, Integer id, int priority) throws IOException {
		if (logger.isDebugEnabled())
			logger.debug("snd (" + message.getType() + "," + message.getTimestamp() + "," + message.getMessage().length + ")");
		if (id == null) {
			return asyncCoreStationClient.send(deploymentName, coreStationDeviceId, this, priority, message.getMessage());
		}
		else {
			return asyncCoreStationClient.send(deploymentName, id, this, priority, message.getMessage());
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
	public synchronized void registerListener(int msgType, BackLogMessageListener listener, boolean isPlugin) {
		Integer msgTypeInt = new Integer(msgType);
	    Vector<BackLogMessageListener> vec = msgTypeListener.get(msgTypeInt);
	    if (vec == null) {
	      vec = new Vector<BackLogMessageListener>();
	    }

	    vec.addElement(listener);
	    msgTypeListener.put(msgTypeInt, vec);
		if (logger.isDebugEnabled())
			logger.debug("Listener for message type " + msgType + " registered");
	    
	    if (isPlugin)
	    	activPluginCounter++;
	}

	
	/**
	 * Stop listening for messages of the given type with the given listener.
	 * 
	 * @param msgType
	 *          specify message type we're listening for
	 * @param listener
	 *          the listener we want to deregister
	 * @return number of different message types which are still listened to
	 */
	public synchronized void deregisterListener(int msgType, BackLogMessageListener listener, boolean isPlugin) throws IllegalArgumentException {
		Integer msgTypeInt = new Integer(msgType);
		Vector<BackLogMessageListener> vec = msgTypeListener.get(msgTypeInt);
		
		if (vec == null)
			throw new IllegalArgumentException( "No listeners registered for message type " + msgType);
		
		// Remove all occurrences
		while (vec.removeElement(listener));

		if (logger.isDebugEnabled())
			logger.debug("Listener for message type " + msgTypeInt + " deregistered");

		if (vec.size() == 0)
			msgTypeListener.remove(msgTypeInt);
		
		if (isPlugin)
			activPluginCounter--;
		
		if (activPluginCounter == 0)
			dispose();
	}
	

	/**
	 * Distribute the received message to the corresponding listeners.
	 * 
	 * @param message to be distributed
	 */
	protected void multiplexMessage(BackLogMessage message) {
		int ReceiverCount = 0;
		Integer msgTypeInt = new Integer(message.getType());
		Vector<BackLogMessageListener> vec = msgTypeListener.get(msgTypeInt);
		if (vec == null) {
			logger.warn("Received message with type " + message.getType() + ", but no listeners registered. Skip message.");
			return;
		}
	
		Enumeration<BackLogMessageListener> en = vec.elements();
		// send the message to all listeners
		while (en.hasMoreElements()) {
			BackLogMessageListener temp = en.nextElement();
		
			// clone the message
			BackLogMessage received = message.clone();
			
			// send the message to the listener
			if (temp.messageReceived(coreStationDeviceId, received.getTimestamp(), received.getPayload()) == true)
				ReceiverCount++;
		}
		if (ReceiverCount == 0)
			logger.warn("Received message with type " + message.getType() + ", but none of the registered listeners did process it. Skip message.");
	}


	@Override
	public InetAddress getInetAddress() {
		return inetAddress;
	}


	@Override
	public int getPort() {
		return hostPort;
	}


	public void sendAck(long timestamp, int priority) {
		// send ACK with corresponding timestamp
		BackLogMessage ack = new BackLogMessage(BackLogMessage.ACK_MESSAGE_TYPE, timestamp);
		if (logger.isDebugEnabled())
			logger.debug("Ack sent: timestamp: " + timestamp);
		try {
			sendMessage(ack, null, priority);
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}
	
	
	private void connectionFinished() {
		if (logger.isDebugEnabled())
			logger.debug("connection finished");
		
    	// start ping timer
        pingTimer = new Timer("PingTimer-" + getCoreStationName());
        pingTimer.schedule( new PingTimer(this), PING_INTERVAL_SEC * 1000, PING_INTERVAL_SEC * 1000 );

        resetWatchDog();

		Collection<Vector<BackLogMessageListener>> val = msgTypeListener.values();
		synchronized (msgTypeListener) {
			Iterator<Vector<BackLogMessageListener>> iter = val.iterator();
			while (iter.hasNext()) {
				Enumeration<BackLogMessageListener> en = iter.next().elements();
				// send the message to all listeners
				while (en.hasMoreElements()) {
					en.nextElement().remoteConnEstablished();
				}
			}
		}
	}


	@Override
	public void connectionEstablished() {
		if (logger.isDebugEnabled())
			logger.debug("connection established");
		
		resetWatchDog();

		try {
			asyncCoreStationClient.sendHelloMsg(this);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}


	@Override
	public void connectionLost() {
		if (logger.isDebugEnabled())
			logger.debug("connection lost");
		
    	// stop ping timer
		if (pingTimer != null)
			pingTimer.cancel();
    	// stop ping checker timer
		if (pingWatchDogTimer != null)
			pingWatchDogTimer.cancel();

		Collection<Vector<BackLogMessageListener>> val = msgTypeListener.values();
		synchronized (msgTypeListener) {
		Iterator<Vector<BackLogMessageListener>> iter = val.iterator();
			while (iter.hasNext()) {
				Enumeration<BackLogMessageListener> en = iter.next().elements();
				// send the message to all listeners
				while (en.hasMoreElements()) {
					en.nextElement().remoteConnLost();
				}
			}
		}
		
		asyncCoreStationClient.removeDeviceId(deploymentName, coreStationDeviceId);
	}
	
	
	private void resetWatchDog() {
    	// reset watch dog timer
		if (logger.isDebugEnabled())
			logger.debug("reset ping watchdog");
		if (pingWatchDogTimer != null)
			pingWatchDogTimer.cancel();
        pingWatchDogTimer = new Timer("PingWatchDog-" + getCoreStationName());
        pingWatchDogTimer.schedule( new PingWatchDog(this), PING_ACK_CHECK_INTERVAL_SEC * 1000 );
	}
	

	/**
	 * Send a ping message to the CoreStation.
	 */
	protected void sendPing() {
		// send ping
		try {
			sendMessage(new BackLogMessage(BackLogMessage.PING_MESSAGE_TYPE, System.currentTimeMillis()), null, 1);
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}
	

	/**
	 * Send a ping acknowledge message to the CoreStation.
	 * 
	 * @return false if not connected to the CoreStation
	 */
	private boolean sendPingAck(long timestamp) {
		// send ping ACK
		try {
			return sendMessage(new BackLogMessage(BackLogMessage.PING_ACK_MESSAGE_TYPE, timestamp), null, 1);
		} catch (IOException e) {
			logger.error(e.getMessage());
			return false;
		}
	}


	@Override
	public String getCoreStationName() {
		return inetAddress.getHostName();
	}
	
	
	public boolean isConnected() {
		return asyncCoreStationClient.isConnected(this);
	}


	public Integer getDeviceID() {
		return coreStationDeviceId;
	}
}



class PluginMessageHandler extends Thread {
	protected final transient Logger logger = Logger.getLogger( PluginMessageHandler.class );

	private BlockingQueue<BackLogMessage> plugMsgQueue;
	private boolean dispose = false;
	BackLogMessageMultiplexer blMsgMulti;
	
	public PluginMessageHandler(BackLogMessageMultiplexer parent, int maxQueueSize) {
		plugMsgQueue = new LinkedBlockingQueue<BackLogMessage>(maxQueueSize);
		blMsgMulti = parent;
		
		setName("PluginMessageHandler-" + blMsgMulti.getCoreStationName() + "-Thread");
	}
	
	public boolean newPluginMessage(BackLogMessage msg) {
		boolean ret = plugMsgQueue.offer(msg);
		if (!ret)
			logger.warn("message queue is full");
		return ret;
	}
	
	public void run() {
		logger.info("thread started");
		BackLogMessage msg = null;
		while (!dispose) {
			try {
				msg = plugMsgQueue.take();
				if (dispose)
					break;
				
				blMsgMulti.multiplexMessage(msg);
			} catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
			}
		}
		logger.info("thread died");
	}
	
	public void dispose() {
		logger.info("dispose thread");
		dispose = true;
		plugMsgQueue.offer(new BackLogMessage((byte)'a'));
	}
}



/**
 * Sends a ping to the CoreStation.
 * 
 * @author Tonio Gsell
 */
class PingTimer extends TimerTask {
	private BackLogMessageMultiplexer parent;
	protected final transient Logger logger = Logger.getLogger( PingTimer.class );
	
	public PingTimer(BackLogMessageMultiplexer parent) {
		this.parent = parent;
	}
	
	public void run() {
		if (logger.isDebugEnabled())
			logger.debug("ping");
		parent.sendPing();
	}
}

/**
 * Checks if a ping has been received since last call.
 * <p>
 * If no ping has been received, report connection loss.
 * 
 * @author Tonio Gsell
 */
class PingWatchDog extends TimerTask {
	private BackLogMessageMultiplexer parent;
	protected final transient Logger logger = Logger.getLogger( PingWatchDog.class );
	
	public PingWatchDog(BackLogMessageMultiplexer parent) {
		this.parent = parent;
	}
	
	public void run() {
		if (logger.isDebugEnabled())
			logger.debug("connection lost");
		parent.asyncCoreStationClient.reconnect(parent);
	}
}
