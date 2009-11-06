package gsn.wrappers.backlog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;


/**
 * Offers the functionality to connect to a backlog deployment,
 * send messages to it and register listeners which receive
 * incoming messages with a specific message type.
 * <p>
 * It implements the backlog protocol, a life ping mechanism to check
 * the availability of the deployment and automatically tries to
 * reconnect to the deployment if a connection lost has happened.
 * 
 * Listener registering to a DeploymentClient should implement the
 * {@link BackLogMessageListener} class.
 *
 * @author	Tonio Gsell
 */
public class DeploymentClient extends Thread {

	/** Timeout in seconds to pass until trying to reconnect
	    to the deployment in case of a connection loss. */
	public static final int RECONNECT_TIMEOUT_SEC = 30;
	/** Ping request interval in seconds. */
	public static final int PING_INTERVAL_SEC = 60;
	/** Time in seconds in which at least one ping acknowledge
	    message should have been received. If no acknowledge has
	    been received, the connection is considered broken. */
	public static final int PING_ACK_CHECK_INTERVAL_SEC = 300;
	
	
	
	private final transient Logger logger = Logger.getLogger( DeploymentClient.class );
	private static int bsClientThreadCounter = 1;
	
	private String remoteHost = null;
	private int remotePort = -1;

	private boolean remoteConnected = false;
	private boolean firstReconnect = true;
	private Socket remoteSocket = null;
	private InputStream remoteInputStream = null;
	private OutputStream remoteOutputStream = null;
	private Semaphore remoteOutputStreamSemaphore = null;
	private Semaphore connStatSemaphore = null;

	private Map<Integer,Vector<BackLogMessageListener>> msgTypeListener; // Mapping from type to Listener
	
	private Timer pingTimer = null;
	private Timer pingCheckerTimer = null;
	private boolean pingACKreceived = false;
	private int pingThreadNumber = -1;
	
	private boolean tRun = false;



	/**
	 * Sends a ping to the deployment.
	 * 
	 * @author Tonio Gsell
	 */
	class Ping extends TimerTask {
		public void run() {
			sendPing();
		}
	}

	/**
	 * Checks if a ping has been received since last call.
	 * <p>
	 * If no ping has been received, report connection loss.
	 * 
	 * @author Tonio Gsell
	 */
	class PingChecker extends TimerTask {
		public void run() {
			if(pingACKreceived) {
				pingACKreceived = false;
			}
			else {
		        // close the socket
				try {
					closeSocket();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}
				
		}
	}


	/**
	 * Tries to open a connection to the destination.
	 * 
	 * @param destination
	 *          The destination (deployment) we want to connect to.
	 *          It must have the form >host:port<.
	 * @throws IOException
	 * 			If it can not connect to the destination. This can be because
	 * 			of a malformed destination string, a unknown destination
	 * 			or the destination does not accept a connection.
	 */
	public DeploymentClient(String destination) throws Exception {
	    msgTypeListener = new HashMap<Integer,Vector<BackLogMessageListener>> ();
		remoteOutputStreamSemaphore = new Semaphore(1);
		connStatSemaphore = new Semaphore(1);
		
		if( RECONNECT_TIMEOUT_SEC <= 0 )
			throw new Exception("RECONNECT_TIMEOUT_SEC must be a positive integer");
		
		if( PING_ACK_CHECK_INTERVAL_SEC <= PING_INTERVAL_SEC )
			throw new Exception("PING_ACK_CHECK_INTERVAL_SEC must be bigger than PING_INTERVAL_SEC");
		
		// a first pattern match test for >host:port<
    	Matcher m = Pattern.compile("(.*):(.*)").matcher(destination);
    	if ( m.find() ) {
			try {
				remoteHost = m.group(1);
				remotePort = new Integer(m.group(2));
			} catch(Exception e) {
				throw new IOException("Remote BackLog host string does not match >host:port<");
			}
    	}
    	else {
			throw new IOException("Remote BackLog host string does not match >host:port<");
    	}

    	pingThreadNumber = bsClientThreadCounter;
    	
    	setName("DeploymentClient-Thread:" + bsClientThreadCounter++);
	}


	/**
	 * Send the message to the basestation this client is connected to.
	 * The timestamp field in the BackLogMessage will be overwritten
	 * with System.currentTimeMillis() in this function!
	 * 
	 * @param message
	 *          a BackLogMessage to be sent to the basestation
	 * @return if the message has been sent successfully true will be returned
	 * 			 else false (no working connection)
	 */
	public boolean sendMessage(BackLogMessage message) {
		if(isConnected()) {
			message.setTimestamp(System.currentTimeMillis());
			return remoteWrite(message.getMessage());
		}
		else
			return false;
	}


	/**
	 * Send an acknowledge message for a specific timestamp to the basestation
	 * this client is connected to.
	 * 
	 * @param timestamp
	 *          the timestamp from the message which should be acknowledged
	 * @return if the ACK has been sent successfully true will be returned
	 * 			 else false (no working connection)
	 */
	public boolean sendAck(long timestamp) {
		// send ACK with corresponding timestamp
		BackLogMessage ack = new BackLogMessage(BackLogMessage.ACK_MESSAGE_TYPE, timestamp);
		logger.debug("Ack sent: timestamp: " + timestamp);
		return remoteWrite(ack.getMessage());
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
	public void registerListener(int msgType, BackLogMessageListener listener) {
		Integer msgTypeInt = new Integer(msgType);
	    Vector<BackLogMessageListener> vec = msgTypeListener.get(msgTypeInt);
	    if (vec == null) {
	      vec = new Vector<BackLogMessageListener>();
	    }
	    vec.addElement(listener);
	    msgTypeListener.put(msgTypeInt, vec);
	    logger.debug("Listener for message type " + msgType + " registered");
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
	public int deregisterListener(int msgType, BackLogMessageListener listener) {
		Integer msgTypeInt = new Integer(msgType);
		Vector<BackLogMessageListener> vec = msgTypeListener.get(msgTypeInt);
		if (vec == null) {
			throw new IllegalArgumentException(
				"No listeners registered for message type "
				+ msgType);
		}
		// Remove all occurrences
		while (vec.removeElement(listener));
		if (vec.size() == 0)
			msgTypeListener.remove(msgTypeInt);
		
	    logger.debug("Listener for message type " + msgTypeInt + " deregistered");
		
		return msgTypeListener.size();
	}

	
	/**
	 * Retruns true if this client is connected to the deployment.
	 * 
	 * @return true if this client is connected otherwise false
	 */
	public boolean isConnected() {
		try {
			connStatSemaphore.acquire();
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
		boolean conn = remoteConnected;
		connStatSemaphore.release();
		return conn;
	}

	
	/**
	 * Finalize this client: stop the reading thread, cancel
	 * all timers and close the socket.
	 */
	public void finalize() {
		tRun = false;
		try {
			this.join(0);
			closeSocket();
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		
    	// stop ping timer
        pingTimer.cancel();
    	// stop ping checker timer
        pingCheckerTimer.cancel();
		
		if(!msgTypeListener.isEmpty())
			logger.warn("There are still active listeners!");
		msgTypeListener.clear();
		bsClientThreadCounter--;
	}

	@Override
	public void run() {
    	logger.debug("BasestationClient-Thread:" + bsClientThreadCounter + " started...");
		tRun = true;
		// constantly read packets from the basestation
    	while (tRun) {
    		if( !isConnected() ) {
    			if( firstReconnect ) {
					logger.warn("trying to reconnect to >" + remoteHost + ":" + remotePort + "< every " + RECONNECT_TIMEOUT_SEC + " seconds");
					firstReconnect = false;
    			}
				try {
					Thread.sleep(RECONNECT_TIMEOUT_SEC * 1000);
				} catch (InterruptedException e1) {
					logger.error(e1.getMessage(), e1);
				}
				if( !connectRemote() )
					logger.debug("trying again to reconnect to >" + remoteHost + ":" + remotePort + "< in " + RECONNECT_TIMEOUT_SEC + " seconds");
			}
    		else {
	    		byte[] read = remoteRead();
	    		if( read != null ) {
		    		BackLogMessage msg = null;
					try {
						msg = new BackLogMessage(read);
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
		    		logger.debug("Message received: with timestamp " + msg.getTimestamp() + " and type " + msg.getType());
		    		if( msg.getType() == BackLogMessage.PING_MESSAGE_TYPE ) {
		    			sendPingAck();
		    		}
		    		else if( msg.getType() == BackLogMessage.PING_ACK_MESSAGE_TYPE ) {
		    			pingACKreceived = true;
		    		}
		    		else
		    			messageReceived(msg);
	    		}
    		}
		}
	}
	

	/**
	 * Send a ping message to the deployment.
	 * 
	 * @return true if successfully transmitted otherwise false
	 */
	private boolean sendPing() {
		// send ping
		return sendMessage(new BackLogMessage(BackLogMessage.PING_MESSAGE_TYPE));
	}
	

	/**
	 * Send a ping acknowledge message to the deployment.
	 * 
	 * @return true if successfully transmitted otherwise false
	 */
	private boolean sendPingAck() {
		// send ping ACK
		return sendMessage(new BackLogMessage(BackLogMessage.PING_ACK_MESSAGE_TYPE));
	}
	

	/**
	 * Distribute the received message to the corresponding listeners.
	 * 
	 * @param message to be distributed
	 */
	private void messageReceived(BackLogMessage message) {
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
			if (temp.messageReceived(received) == true)
				ReceiverCount++;
		}
		if (ReceiverCount == 0)
			logger.warn("Received message with type " + message.getType() + ", but none of the registered listeners did process it. Skip message.");
	}
	

	/**
	 * Read a remote message.
	 * 
	 * This function is blocking till a message has been received.
	 * 
	 * @return the message as a byte array or null if any error
	 * 			 occurred
	 */
    private byte[] remoteRead() {
    	byte [] msg = null;
		try {
			// read the first 4 bytes containing the length of the message
			int l = 0;
			byte[] length_buf = new byte  [4];
			int l_remaining = length_buf.length;
			while(l_remaining > 0) {
				l = remoteInputStream.read(length_buf, length_buf.length-l_remaining, l_remaining);
				if( l == -1 )
					break;
				else if( l == 0) {
					logger.error("read returns zero while reading message length");
					break;
				}
				l_remaining -= l;
			}
			if( l == -1 ) {
				// end-of-stream reached
				connectionLost();
				return null;
			}
			// read the message
			int len = arr2int(length_buf, 0);
			if( len <= 0 ) {
				logger.error("The incoming message length (" + len + ") is not a positiv integer. Skip this message!");
				return null;
			}
			else {
				msg = new byte [len];
				l_remaining = len;
				logger.debug("incoming message with length: " + len);
				while(l_remaining > 0) {
					l = remoteInputStream.read(msg, msg.length-l_remaining, l_remaining);
					if( l == -1 )
						break;
					else if( l == 0) {
						logger.error("read returns zero while reading message payload");
						break;
					}
					l_remaining -= l;
				}
			}
			if( l == -1 ) {
				// end-of-stream reached
				connectionLost();
				return null;
			}
		} catch (IOException e) {
			// something went wrong
			connectionLost();
			return null;
		}
		return msg;
    }
	

	/**
	 * Write a remote message.
	 * 
	 * @param the message as byte array
	 * 
	 * @return true if the message has been successfully written
	 * 			 otherwise false
	 */
    private boolean remoteWrite(byte[] message) {
		try {
			remoteOutputStreamSemaphore.acquire();
		} catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
				return false;
		}
    	try {
    		remoteOutputStream.write(int2arr(message.length));
			remoteOutputStream.write(message);
		} catch (IOException e) {
			remoteOutputStreamSemaphore.release();
			connectionLost();
			return false;
		}
		remoteOutputStreamSemaphore.release();
		return true;
    }
	

	/**
	 * Connect to the deployment and start the life ping functionality.
	 * 
	 * @return true if the connection was successful otherwise false
	 */
	private boolean connectRemote() {
    	// try to open the connection to the basestation
		try {
			InetSocketAddress socketAddress = new InetSocketAddress(remoteHost, remotePort);
			remoteSocket = new Socket();
			remoteSocket.setReuseAddress(true);
			remoteSocket.connect(socketAddress, 10000);
			remoteInputStream = remoteSocket.getInputStream();
	        remoteOutputStream = remoteSocket.getOutputStream();
		} catch (UnknownHostException e) {
			logger.error("The ip address of <" + remoteHost + ":" + remotePort + "< could not be determined");
			return false;
		} catch (SocketTimeoutException e) {
			if( firstReconnect || logger.isDebugEnabled() )
				logger.error("Could not connect to <" + remoteHost + ":" + remotePort + "<: " + e.getMessage());
			return false;
		} catch (IOException e) {
			if( firstReconnect || logger.isDebugEnabled() )
				logger.error("Could not connect to <" + remoteHost + ":" + remotePort + "<: " + e.getMessage());
			return false;
		}
    	
		try {
			connStatSemaphore.acquire();
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
			return false;
		}
		
    	logger.info("Connected to " + remoteHost + ":" + remotePort);
    	
		remoteConnected = true;
		connStatSemaphore.release();

    	// start ping timer
        pingTimer = new Timer("BasestationClient-Thread:" + pingThreadNumber + ":PingTimer");
        pingTimer.schedule( new Ping(), PING_INTERVAL_SEC * 1000, PING_INTERVAL_SEC * 1000 );
    	// start ping checker timer
        pingCheckerTimer = new Timer("BasestationClient-Thread:" + pingThreadNumber + ":PingCheckerTimer");
        pingCheckerTimer.schedule( new PingChecker(), PING_ACK_CHECK_INTERVAL_SEC * 1000, PING_ACK_CHECK_INTERVAL_SEC * 1000 );
        
        firstReconnect = true;
        return true;
	}
	

	/**
	 * Stop the life ping and close the socket.
	 */
	private void connectionLost() {
		logger.error("Connection to basestation >" + remoteHost + ":" + remotePort + "< lost");
		try {
			connStatSemaphore.acquire();
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
		remoteConnected = false;
		connStatSemaphore.release();
    	// stop ping timer
        pingTimer.cancel();
    	// stop ping checker timer
        pingCheckerTimer.cancel();
        
		pingACKreceived = false;

        // close the socket
		try {
			closeSocket();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}
	

	/**
	 * Close any open socket.
	 * 
	 * @throws IOException if an I/O error occurs
	 */
	private void closeSocket() throws IOException {
		if ( remoteInputStream != null )
			remoteInputStream.close();
		if ( remoteOutputStream != null )
			remoteOutputStream.close();
		if ( remoteSocket != null )
			remoteSocket.close();
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
	
	
	private static byte[] int2arr (int l) {
		int len = 4;
		byte[] arr = new byte[len];

		int i = 0;
		for ( int shiftBy = 0; shiftBy < 32; shiftBy += 8 ) {
			arr[i] = (byte)( l >> shiftBy);
			i++;
		}
		return arr;
	}
}
