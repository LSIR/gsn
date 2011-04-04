package gsn.wrappers.backlog;

import gsn.wrappers.backlog.statistics.CoreStationStatistics;
import gsn.wrappers.backlog.statistics.StatisticsMain;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
	public static final int PLUGIN_MESSAGE_QUEUE_WARN = 800;
	public static final int PLUGIN_MESSAGE_QUEUE_READY = 400;
	
	public static final byte STUFFING_BYTE = 0x7e;
	
	public static final byte HELLO_BYTE = 0x7d;
	
	static final transient Logger logger = Logger.getLogger( BackLogMessageMultiplexer.class );
	

	private static Map<String,BackLogMessageMultiplexer> blMultiplexerMap = new HashMap<String,BackLogMessageMultiplexer>();

	private Map<Integer,Vector<BackLogMessageListener>> msgTypeListener; // Mapping from type to Listener

	protected AsyncCoreStationClient asyncCoreStationClient = null;
	private CoreStationStatistics coreStationStatistics = null;
	private BlockingQueue<byte []> recvQueue = new LinkedBlockingQueue<byte[]>();
	private PluginMessageHandler pluginMessageHandler;
	private boolean dispose = false;
	private Integer activPluginCounter = 0;
	private Integer coreStationDeviceId = null;

	private Timer pingTimer = null;
	private Timer pingWatchDogTimer = null;

	private String coreStationAddress;
	private String hostName;
	private int hostPort;
	private String deploymentName;
	boolean stuff = false;
	boolean connected = false;
	
	
	public BackLogMessageMultiplexer() throws Exception {
		throw new Exception("use 'getInstance(String CoreStation)' function for instantiation");
	}
	
	
	private BackLogMessageMultiplexer(String deployment, String hostName, Integer port) throws Exception {
		msgTypeListener = Collections.synchronizedMap(new HashMap<Integer,Vector<BackLogMessageListener>> ());
		
		coreStationAddress = hostName + ":" + port;
		this.hostName = hostName;
		this.hostPort = port;
    	this.deploymentName = deployment;
    	
    	pluginMessageHandler = new PluginMessageHandler(this, PLUGIN_MESSAGE_QUEUE_SIZE);
    	
    	asyncCoreStationClient = AsyncCoreStationClient.getSingletonObject();
    	
    	coreStationStatistics = StatisticsMain.getCoreStationStatsInstance(deployment, coreStationAddress);
		
		setName("BackLogMessageMultiplexer-" + getCoreStationName() + "-Thread");
	}
	
	
	@SuppressWarnings("unused")
	public synchronized static BackLogMessageMultiplexer getInstance(String deployment, String coreStationAddress) throws Exception {
		String coreStationAddress_noIp;
		InetAddress inetAddress;
		Integer hostPort;
		if( PING_ACK_CHECK_INTERVAL_SEC <= PING_INTERVAL_SEC )
			throw new Exception("PING_ACK_CHECK_INTERVAL_SEC must be bigger than PING_INTERVAL_SEC");
		
		// a first pattern match test for >host:port<
    	Matcher m = Pattern.compile("(.*):(.*)").matcher(coreStationAddress);
    	if ( m.find() ) {
			inetAddress = InetAddress.getByName(m.group(1));
            hostPort = new Integer(m.group(2));
			coreStationAddress_noIp = inetAddress.getHostName() + ":" + hostPort;
			if(blMultiplexerMap.containsKey(coreStationAddress_noIp)) {
				return blMultiplexerMap.get(coreStationAddress_noIp);
			}
			else {
				BackLogMessageMultiplexer blMulti = new BackLogMessageMultiplexer(deployment, inetAddress.getHostName(), hostPort);
				blMultiplexerMap.put(coreStationAddress_noIp, blMulti);
				return blMulti;
			}
    	}
    	else {
			throw new IOException("Remote BackLog host string (" + coreStationAddress + ") does not match >host:port<");
    	}
	}
	
	
	public long getStartTime() {
		return asyncCoreStationClient.getStartTime();
	}
	  
	
	public void processData(byte[] data, int count) {
		byte[] dataCopy = new byte[count];
		System.arraycopy(data, 0, dataCopy, 0, count);
		recvQueue.offer(dataCopy);
	}
	
	
	public void run() {
		logger.info("thread started");
		
		//TODO: use DataInputStream to handle data
		
		pluginMessageHandler.start();
    	
		try {
			asyncCoreStationClient.registerListener(this);
		} catch (Exception e) {
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
					coreStationStatistics.bytesReceived(in.length);
				} catch (InterruptedException e) {
					if (logger.isDebugEnabled())
						logger.debug(e.getMessage());
					break;
				}
				if (dispose)
					break;
				
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				if (!pktDestuffing(in, baos)) {
					logger.debug("stuffing mark reached");
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

						connecting = false;
						if (tmp[0] != HELLO_BYTE) {
							logger.error("connection hello message does not match -> reconnect");
							asyncCoreStationClient.reconnect(this);
							recvQueue.clear();
						}
						else {
							coreStationDeviceId = arr2int(tmp, 1);
							logger.info("connected successfully to core station with device id " + coreStationDeviceId + " at " + deploymentName + " deployment");
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
							packetLength = arr2uint(pkt.toByteArray(), 0);
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
						try {
							msg = new BackLogMessage(java.util.Arrays.copyOfRange(tmp, 4, (int) (packetLength+4)));
						} catch (Exception e) {
							logger.error(e.getMessage(), e);
							continue;
						}
						if (logger.isDebugEnabled())
							logger.debug("rcv (" + msg.getType() + "," + msg.getTimestamp() + "," + msg.getBinaryMessage().length + ")");
						coreStationStatistics.msgReceived(msg.getType(), msg.getSize());
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
		recvQueue.clear();
		byte[] end = {'e','n','d'};
		recvQueue.offer(end);
		msgTypeListener.clear();
		
		pluginMessageHandler.dispose();
		
		if (blMultiplexerMap.remove(coreStationAddress) == null)
			logger.error("there is no " + coreStationAddress + " available in the map");
		
		asyncCoreStationClient.deregisterListener(this);
		
		try {
			StatisticsMain.removeCoreStationStatsInstance(deploymentName, coreStationAddress);
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
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
	public boolean sendMessage(BackLogMessage message, Integer id, int priority) throws IOException {
		if (logger.isDebugEnabled())
			logger.debug("snd (" + message.getType() + "," + message.getTimestamp() + "," + message.getBinaryMessage().length + ")");
		if (id == null) {
			Serializable [] ret = asyncCoreStationClient.send(deploymentName, coreStationDeviceId, this, priority, message.getBinaryMessage());
			if ((Boolean) ret[0]) {
				coreStationStatistics.msgSent(message.getType(), message.getSize());
				coreStationStatistics.bytesSent((Long)ret[1]);
				return true;
			}
		}
		else {
			Serializable [] ret = asyncCoreStationClient.send(deploymentName, id, this, priority, message.getBinaryMessage());
			if ((Boolean) ret[0]) {
				coreStationStatistics.msgSent(message.getType(), message.getSize());
				coreStationStatistics.bytesSent((Long)ret[1]);
				return true;
			}
		}
		return false;
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
		
		if (isPlugin) {
			activPluginCounter--;
		
			if (activPluginCounter == 0)
				dispose();
		}
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
			// send the message to the listener
			if (temp.messageRecv(coreStationDeviceId, message) == true)
				ReceiverCount++;
		}
		if (ReceiverCount == 0)
			logger.warn("Received message with type " + message.getType() + ", but none of the registered listeners did process it. Skip message.");
	}


	@Override
	public InetAddress getInetAddress() throws UnknownHostException {
		return InetAddress.getByName(hostName);
	}


	@Override
	public int getPort() {
		return hostPort;
	}


	public void sendAck(long timestamp, int msgType, int priority) {
		// send ACK with corresponding timestamp and message type
		BackLogMessage ack;
		try {
			Serializable [] type = {msgType};
			ack = new BackLogMessage(BackLogMessage.ACK_MESSAGE_TYPE, timestamp, type);
			if (logger.isDebugEnabled())
				logger.debug("Ack sent: (timestamp=" + timestamp + "/messageType=" + msgType + ")");

				sendMessage(ack, null, priority);
		} catch (Exception e) {
			logger.warn(e.getMessage());
		}
	}
	
	
	private void connectionFinished() {
		if (logger.isDebugEnabled())
			logger.debug("connection finished");
		
    	// start ping timer
        pingTimer = new Timer("PingTimer-" + getCoreStationName());
        pingTimer.schedule( new PingTimer(this), PING_INTERVAL_SEC * 1000, PING_INTERVAL_SEC * 1000 );

        resetWatchDog();
		
		if (pluginMessageHandler.isMsgQueueLimitReached()) {
			sendQueueLimitMsg();
			logger.warn("message queue limit reached => sending queue limit message");
		}
		else if (pluginMessageHandler.isMsgQueueReady()) {
			sendQueueReadyMsg();
			logger.warn("message queue ready => sending queue ready message");
		}

		coreStationStatistics.setDeviceId(coreStationDeviceId);
		connected = true;
		coreStationStatistics.setConnected(true);
		try {
			StatisticsMain.connectionStatusChanged(deploymentName, coreStationDeviceId);
		} catch (IOException e) {
			logger.error(e.getMessage());
		}

		Collection<Vector<BackLogMessageListener>> val = msgTypeListener.values();
		synchronized (msgTypeListener) {
			Iterator<Vector<BackLogMessageListener>> iter = val.iterator();
			while (iter.hasNext()) {
				Enumeration<BackLogMessageListener> en = iter.next().elements();
				// send the message to all listeners
				while (en.hasMoreElements()) {
					en.nextElement().remoteConnEstablished(coreStationDeviceId);
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
		connected = false;
		coreStationStatistics.setConnected(false);
		try {
			StatisticsMain.connectionStatusChanged(deploymentName, coreStationDeviceId);
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
		logger.info("connection to core station with device id " + coreStationDeviceId + " at " + deploymentName + " deployment lost");
		
		recvQueue.clear();
		pluginMessageHandler.clearMsgQueue();
		
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
		return hostName;
	}
	
	
	public boolean isConnected() {
		return connected;
	}


	public Integer getDeviceID() {
		return coreStationDeviceId;
	}

	protected void sendQueueLimitMsg() {
		try {
			sendMessage(new BackLogMessage(BackLogMessage.MESSAGE_QUEUE_LIMIT_MESSAGE_TYPE, System.currentTimeMillis()), null, 1);
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}

	protected void sendQueueReadyMsg() {
		try {
			sendMessage(new BackLogMessage(BackLogMessage.MESSAGE_QUEUE_READY_MESSAGE_TYPE, System.currentTimeMillis()), null, 1);
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}
	
	private static long arr2uint (byte[] arr, int start) {
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
	
	private static int arr2int (byte[] arr, int start) {
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
}



class PluginMessageHandler extends Thread {
	protected final transient Logger logger = Logger.getLogger( PluginMessageHandler.class );

	private BlockingQueue<BackLogMessage> plugMsgQueue;
	private boolean dispose = false;
	private boolean queueLimitReached = false;
	BackLogMessageMultiplexer blMsgMulti;
	
	public PluginMessageHandler(BackLogMessageMultiplexer parent, int maxQueueSize) {
		plugMsgQueue = new LinkedBlockingQueue<BackLogMessage>(maxQueueSize);
		blMsgMulti = parent;
		
		setName("PluginMessageHandler-" + blMsgMulti.getCoreStationName() + "-Thread");
	}
	
	public boolean newPluginMessage(BackLogMessage msg) {
		boolean ret = plugMsgQueue.offer(msg);
		if (isMsgQueueLimitReached()) {
			blMsgMulti.sendQueueLimitMsg();
			if (!queueLimitReached)
				logger.warn("message queue limit reached => sending queue limit message");
			queueLimitReached = true;
		}
		if (!ret)
			logger.warn("message queue is full");
		return ret;
	}
	
	protected boolean isMsgQueueReady() {
		return plugMsgQueue.size() <= BackLogMessageMultiplexer.PLUGIN_MESSAGE_QUEUE_READY;
	}
	
	protected boolean isMsgQueueLimitReached() {
		return plugMsgQueue.size() >= BackLogMessageMultiplexer.PLUGIN_MESSAGE_QUEUE_WARN;
	}
	
	public void run() {
		logger.info("thread started");
		BackLogMessage msg = null;
		while (!dispose) {
			try {
				msg = plugMsgQueue.take();
				if (queueLimitReached && isMsgQueueReady()) {
					//TODO: send queue ready msg is only sent once... what if it does not reach the Core Station?
					blMsgMulti.sendQueueReadyMsg();
					logger.warn("message queue ready again => sending queue ready message");
					queueLimitReached = false;
				}
				if (dispose)
					break;
				
				blMsgMulti.multiplexMessage(msg);
			} catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
			}
		}
		logger.info("thread died");
	}
	
	public void clearMsgQueue() {
		plugMsgQueue.clear();
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
