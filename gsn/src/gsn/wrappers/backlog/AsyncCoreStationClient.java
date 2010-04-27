package gsn.wrappers.backlog;

import gsn.wrappers.backlog.plugins.AbstractPlugin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;


public class AsyncCoreStationClient extends Thread  {
	/** Timeout in seconds to pass until trying to reconnect
    to the CoreStation in case of a connection loss. */
	public static final int RECONNECT_TIMEOUT_SEC = 30;

	private static final int BUFFER_SIZE = 65536;
	
	protected final transient Logger logger = Logger.getLogger( AsyncCoreStationClient.class );
	
	private static AsyncCoreStationClient singletonObject = null;

	protected Selector selector;
	protected List<ChangeRequest> changeRequests = new LinkedList<ChangeRequest>();

	// Maps a SocketChannel to a list of ByteBuffer instances
	private Map<SocketChannel, ArrayList<byte[]>> pendingData = new HashMap<SocketChannel, ArrayList<byte[]>>();

	protected Map<SocketChannel,CoreStationListener> socketToListenerList = new HashMap<SocketChannel,CoreStationListener>();
	protected Map<CoreStationListener,SocketChannel> listenerToSocketList = new HashMap<CoreStationListener,SocketChannel>();
	private static Map<String,Map<Integer,CoreStationListener>> deploymentToIdListenerMapList = new HashMap<String,Map<Integer,CoreStationListener>>();

	private ByteBuffer writeBuffer;
	private ByteBuffer readBuffer;
	
	private boolean dispose = false;
	
	
	private AsyncCoreStationClient() throws IOException {
		this.selector = Selector.open();

		writeBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		writeBuffer.flip();
		
		setName("AsyncCoreStationClient-Thread");
	}
	
	
	public synchronized static AsyncCoreStationClient getSingletonObject() throws Exception {
		if( RECONNECT_TIMEOUT_SEC <= 0 )
			throw new Exception("RECONNECT_TIMEOUT_SEC must be a positive integer");
		
		if (singletonObject == null)
			singletonObject = new AsyncCoreStationClient();
		
		return singletonObject;
	}
	
	
	public void run()
	{
		logger.debug("thread started");
		SelectionKey key;

	    while (!dispose) {
	    	try {
	    		synchronized(changeRequests) {
	    			Iterator<ChangeRequest> changes = changeRequests.iterator();
	    			while (changes.hasNext()) {
	    				ChangeRequest change = changes.next();
	    				switch(change.type) {
	    				case ChangeRequest.TYPE_CHANGEOPS:
	    					key = change.socket.keyFor(selector);
	    					if (key == null || !key.isValid())
	    						continue;
	    					if (!change.socket.isConnectionPending()) {
		    					logger.debug("Selector:changeops");
	    						key.interestOps(change.ops);
	    						key.attach(change);
	    					}
	    					break;
	    				case ChangeRequest.TYPE_REGISTER:
	    					logger.debug("Selector:register");
    						change.socket.register(selector, change.ops, change);
	    					break;
	    				case ChangeRequest.TYPE_RECONNECT:
	    					logger.debug("Selector:reconnect");
	    					if (change.socket.keyFor(selector).isValid())
	    						closeConnection(change.socket.keyFor(selector), change.socket);
	    					CoreStationListener listener;
	    					synchronized (socketToListenerList) {
	    						listener = socketToListenerList.get(change.socket);
	    						socketToListenerList.remove(change.socket);
	    					}
	    					if (listener != null) {
	    						synchronized (listenerToSocketList) {
			    					listenerToSocketList.remove(listener);
								}
		    					logger.debug("trying to reconnect to " + listener.getCoreStationName() + " CoreStation in " + RECONNECT_TIMEOUT_SEC + " seconds");
		    					Timer timer = new Timer("ReconnectTimer-" + listener.getCoreStationName());
		    					timer.schedule(new ReconnectTimerTask(this, listener), RECONNECT_TIMEOUT_SEC*1000);
	    					}
	    					break;
	    				}
	    			}
	    			changeRequests.clear();
	    		}
	    		
	    		if (selector.select() == 0)
	    			continue;
	    		
	    		Set<SelectionKey> readyKeys = selector.selectedKeys();
	    		Iterator<SelectionKey> iterator = readyKeys.iterator();
	    		while (iterator.hasNext()) {
	    			key = iterator.next();
	    			iterator.remove();
	    			if (!key.isValid()) {
	    				logger.warn("Selector:invalid");
	    				continue;
	    			}
	    			if (key.channel() instanceof SocketChannel) {
		    			try {
		    				if (key.isReadable()) {
		    					this.read(key);
		    				} else if (key.isWritable()) {
		    					this.write(key);
		    				} else if (key.isConnectable()) {
		    					logger.debug("Selector:connect");
		    					this.finishConnection(key);
		    				}
		    			} catch (IOException e) {
		    	    		logger.error(e.getMessage(), e);
		    			}
	    			} else {
	    				logger.debug("no handler for " + key.channel().getClass().getName());
	    			}
	    		}
	    	} catch (IOException e) {
	    		logger.error(e.getMessage(), e);
	    	}
	    }
	    
		logger.debug("thread stoped");
	}
	
	
	private void read(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();

	    try {
	        // Clear out our read buffer so it's ready for new data
	        readBuffer.clear();

	        int numRead = socketChannel.read(readBuffer);
	        if (numRead == -1) {
		    	logger.debug("connection closed");
	        	// Remote entity shut the socket down cleanly. Do the
	        	// same from our end and cancel the channel.
				synchronized (socketToListenerList) {
					reconnect(socketToListenerList.get(socketChannel));
				}
				return;
	        }
			
			synchronized(socketToListenerList) {
			    // Hand the data over to our listener thread
				socketToListenerList.get(socketChannel).processData(readBuffer.array(), numRead);
			}
	    } catch (IOException e) {
	    	logger.debug("connection closed: " + e.getMessage());
	    	// The remote forcibly closed the connection
			synchronized (socketToListenerList) {
				reconnect(socketToListenerList.get(socketChannel));
			}
	    }
	}
	
	
	private void closeConnection(SelectionKey key, SocketChannel sc) {
		if (key != null)
			key.cancel();
    	if (sc != null) {
	    	try {
				sc.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
			CoreStationListener listener;
			synchronized (socketToListenerList) {
				listener = socketToListenerList.get(sc);
			}
			listener.connectionLost();
    	}
    	writeBuffer.clear();
    	writeBuffer.flip();
	}
	
	
	private void write(SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		try {
			synchronized (this.pendingData) {
				ArrayList<byte[]> queue = this.pendingData.get(socketChannel);

				while (writeBuffer.hasRemaining()) {
					if (socketChannel.write(writeBuffer) == 0)
						return;
				}
		      
				// Write until there's not more data ...
				while (!queue.isEmpty()) {
					writeBuffer.clear();
					try {
						writeBuffer.put(queue.get(0));
					} catch (BufferOverflowException e) {
						logger.error(e.getMessage(), e);
					} finally {
						queue.remove(0);
					}
					
					writeBuffer.flip();
					while (writeBuffer.hasRemaining()) {
						if (socketChannel.write(writeBuffer) == 0)
							return;
					}
				}
				
				if (queue.isEmpty()) {
					// We wrote away all data, so we're no longer interested
					// in writing on this socket. Switch back to waiting for
					// data.
					key.interestOps(SelectionKey.OP_READ);
				}
			}
	    } catch (IOException e) {
	    	logger.debug("connection closed: " + e.getMessage());
	    	// The remote forcibly closed the connection
			synchronized (socketToListenerList) {
				reconnect(socketToListenerList.get(socketChannel));
			}
	    	return;
	    }
	}
	
	
	private void finishConnection(SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
	  
		// Finish the connection. If the connection operation failed
		// this will raise an IOException.
		try {
			socketChannel.finishConnect();
		} catch (IOException e) {
			synchronized (socketToListenerList) {
				logger.error("could not connect to " + socketToListenerList.get(socketChannel).getHostAddress() + ": " + e.getMessage());
				reconnect(socketToListenerList.get(socketChannel));
			}
			return;
		}

		CoreStationListener listener;
		synchronized (socketToListenerList) {
			listener = socketToListenerList.get((SocketChannel)key.channel());
		}
		listener.connectionEstablished();
	  
		// Register an interest in reading on this channel
		key.interestOps(SelectionKey.OP_READ);
	}

	
	public synchronized void registerListener(CoreStationListener listener) throws IOException
	{
		if (!this.isAlive()) {
			dispose = false;
			this.start();
		}
		
		SocketChannel socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);
		socketChannel.connect(new InetSocketAddress(listener.getHostAddress(), listener.getPort()));
		
		synchronized(socketToListenerList) {
			socketToListenerList.put(socketChannel, listener);
		}
		synchronized (listenerToSocketList) {
			listenerToSocketList.put(listener, socketChannel);
		}
		
		synchronized(changeRequests) {
			changeRequests.add(new ChangeRequest(socketChannel, ChangeRequest.TYPE_REGISTER, SelectionKey.OP_CONNECT));
		}
		selector.wakeup();
	}
	
	
	public synchronized void deregisterListener(CoreStationListener listener)
	{
		SocketChannel sc = listenerToSocketList.get(listener);
		try {
			sc.close();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

		sc.keyFor(selector).cancel();
		
		synchronized(socketToListenerList) {
			socketToListenerList.remove(sc);
		}
		
		synchronized(listenerToSocketList) {
			listenerToSocketList.remove(listener);
		}
		
		if (socketToListenerList.isEmpty())
			dispose();
	}


	public void addDeviceId(String deployment, Integer id, CoreStationListener listener) {
		logger.debug("adding DeviceId " + id + "for " + deployment + " deployment");

		synchronized (deploymentToIdListenerMapList) {
			if (!deploymentToIdListenerMapList.containsKey(deployment)) {
				deploymentToIdListenerMapList.put(deployment, new HashMap<Integer, CoreStationListener>());
			}
			
			deploymentToIdListenerMapList.get(deployment).put(id, listener);
		}
	}


	public void removeDeviceId(String deployment, Integer id) {
		logger.debug("removing DeviceId: " + id + "for " + deployment + " deployment");
		synchronized (deploymentToIdListenerMapList) {
			deploymentToIdListenerMapList.get(deployment).remove(id);
			
			if (deploymentToIdListenerMapList.get(deployment).isEmpty())
				deploymentToIdListenerMapList.remove(deployment);
		}
	}


	private void dispose() {
		dispose = true;
		selector.wakeup();
		try {
			this.join();
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	
	private byte[] pktStuffing(byte[] message) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (int i=0; i<message.length; i++) {
			baos.write(message[i]);
			if (message[i] == BackLogMessageMultiplexer.STUFFING_BYTE)
				baos.write(message[i]);
		}
		return baos.toByteArray();
	}


	public boolean send(String deployment, Integer id, CoreStationListener listener, byte[] data) throws IOException {
		boolean ret = false;
		if (id != null) {
			if (id == 65535) {
				synchronized (deploymentToIdListenerMapList) {
					Iterator<Integer> iter = deploymentToIdListenerMapList.get(deployment).keySet().iterator();
					while (iter.hasNext()) {
						if(send(deploymentToIdListenerMapList.get(deployment).get(iter.next()), data, true))
							ret = true;
					}
				}
			}
			else {
				synchronized (deploymentToIdListenerMapList) {
					listener = deploymentToIdListenerMapList.get(deployment).get(id);
				}
				if (listener == null)
					throw new IOException("The DeviceId " + id + " is not connected or does not exist for the " + deployment + " deployment");
				
				ret = send(listener, data, true);
			}
		}
		else {
			ret = send(listener, data, true);
		}
		return ret;
	}


	private boolean send(CoreStationListener listener, byte[] data, boolean stuff) throws IOException {
		if (data.length > BUFFER_SIZE-4) 
			throw new IOException("data limited to " + (BUFFER_SIZE-4) + " bytes");
		
		SocketChannel socketChannel;
		synchronized (listenerToSocketList) {
			socketChannel = listenerToSocketList.get(listener);
		}
		if (socketChannel != null && socketChannel.isConnected()) {
			synchronized (this.changeRequests) {
				// Indicate we want the interest ops set changed
				this.changeRequests.add(new ChangeRequest(socketChannel, ChangeRequest.TYPE_CHANGEOPS, SelectionKey.OP_WRITE | SelectionKey.OP_READ));
		      
				// And queue the data we want written
				synchronized (this.pendingData) {
					ArrayList<byte[]> queue = this.pendingData.get(socketChannel);
					if (queue == null) {
						queue = new ArrayList<byte[]>();
						this.pendingData.put(socketChannel, queue);
					}
					if (stuff) {
		        		ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length + 4);
		        		baos.write(AbstractPlugin.uint2arr(data.length));
						baos.write(data);
						queue.add(pktStuffing(baos.toByteArray()));
					}
					else
						queue.add(data);
				}
			}
		    
		    // Finally, wake up our selecting thread so it can make the required changes
			this.selector.wakeup();
			return true;
		}
		else {
			logger.debug("not connected");
			return false;
		}
	}


	public boolean sendHelloMsg(CoreStationListener listener) throws IOException {
		logger.debug("send hello message");
		byte[] data = {BackLogMessageMultiplexer.STUFFING_BYTE, BackLogMessageMultiplexer.HELLO_BYTE};
		
		return send(listener, data, false);
	}
	
	
	public void reconnect(CoreStationListener listener) {
		writeBuffer.clear();
    	writeBuffer.flip();
		synchronized (changeRequests) {
			logger.debug("add reconnect request");
			// Indicate we want the interest ops set changed
			changeRequests.add(new ChangeRequest(listenerToSocketList.get(listener), ChangeRequest.TYPE_RECONNECT, -1));
		}
		selector.wakeup();
	}
	
	
	public boolean isConnected(CoreStationListener listener) {
		SocketChannel sc = listenerToSocketList.get(listener);
		if (sc == null)
			return false;
		else
			return sc.isConnected();
	}
}


class ChangeRequest
{
	public static final int TYPE_REGISTER = 1;
	public static final int TYPE_CHANGEOPS = 2;
	public static final int TYPE_RECONNECT = 3;

	public SocketChannel socket;
	public int type;
	public int ops;

	public ChangeRequest(SocketChannel socket, int type, int ops)
	{
		this.socket = socket;
		this.type = type;
		this.ops = ops;
	}
}


class ReconnectTimerTask extends TimerTask
{
	private AsyncCoreStationClient parent;
	private CoreStationListener listener;
	
	public ReconnectTimerTask(AsyncCoreStationClient parent, CoreStationListener listener)
	{
		super();
		this.parent = parent;
		this.listener = listener;
	}
	
	public void run()
	{
		try {
			parent.registerListener(listener);
		} catch (IOException e) {
			parent.logger.error(e.getMessage(), e);
		}
	}
}