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


public class AsyncDeploymentClient extends Thread  {
	/** Timeout in seconds to pass until trying to reconnect
    to the deployment in case of a connection loss. */
	public static final int RECONNECT_TIMEOUT_SEC = 3;

	private static final int BUFFER_SIZE = 65536;
	
	protected final transient Logger logger = Logger.getLogger( AsyncDeploymentClient.class );
	
	private static AsyncDeploymentClient singletonObject = null;

	protected Selector selector;
	protected List<ChangeRequest> changeRequests = new LinkedList<ChangeRequest>();

	// Maps a SocketChannel to a list of ByteBuffer instances
	private Map<SocketChannel, ArrayList<byte[]>> pendingData = new HashMap<SocketChannel, ArrayList<byte[]>>();

	protected Map<SocketChannel,DeploymentListener> socketToListenerList = new HashMap<SocketChannel,DeploymentListener>();
	protected Map<DeploymentListener,SocketChannel> listenerToSocketList = new HashMap<DeploymentListener,SocketChannel>();

	private ByteBuffer writeBuffer;
	private ByteBuffer readBuffer;
	
	private boolean dispose = false;
	
	
	private AsyncDeploymentClient() throws IOException {
		this.selector = Selector.open();

		writeBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		writeBuffer.flip();
		
		setName("AsyncDeploymentClient-Thread");
	}
	
	
	public synchronized static AsyncDeploymentClient getSingletonObject() throws Exception {
		if( RECONNECT_TIMEOUT_SEC <= 0 )
			throw new Exception("RECONNECT_TIMEOUT_SEC must be a positive integer");
		
		if (singletonObject == null)
			singletonObject = new AsyncDeploymentClient();
		
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
	    					DeploymentListener listener;
	    					synchronized (socketToListenerList) {
	    						listener = socketToListenerList.get(change.socket);
	    						socketToListenerList.remove(change.socket);
	    					}
	    					if (listener != null) {
	    						synchronized (listenerToSocketList) {
			    					listenerToSocketList.remove(listener);
								}
		    					logger.debug("trying to reconnect to " + listener.getDeploymentName() + " deployment in " + RECONNECT_TIMEOUT_SEC + " seconds");
		    					Timer timer = new Timer("Reconnect" + listener.getDeploymentName() + "Timer");
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
			DeploymentListener listener;
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

		DeploymentListener listener;
		synchronized (socketToListenerList) {
			listener = socketToListenerList.get((SocketChannel)key.channel());
		}
		listener.connectionEstablished();
	  
		// Register an interest in reading on this channel
		key.interestOps(SelectionKey.OP_READ);
	}

	
	public void registerListener(DeploymentListener listener) throws IOException
	{
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
	
	
	public void deregisterListener(DeploymentListener listener)
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


	private void dispose() {
		dispose = true;
		selector.wakeup();
		try {
			this.join();
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
	}


	public boolean send(DeploymentListener listener, byte[] data) throws IOException {
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
	        		ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length + 4);
	        		baos.write(AbstractPlugin.uint2arr(data.length));
					baos.write(data);
					queue.add(baos.toByteArray());
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
	
	
	public void reconnect(DeploymentListener listener) {
		writeBuffer.clear();
    	writeBuffer.flip();
		synchronized (changeRequests) {
			logger.debug("add reconnect request");
			// Indicate we want the interest ops set changed
			changeRequests.add(new ChangeRequest(listenerToSocketList.get(listener), ChangeRequest.TYPE_RECONNECT, -1));
		}
		selector.wakeup();
	}
	
	
	public boolean isConnected(DeploymentListener listener) {
		return listenerToSocketList.get(listener).isConnected();
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
	private AsyncDeploymentClient parent;
	private DeploymentListener listener;
	
	public ReconnectTimerTask(AsyncDeploymentClient parent, DeploymentListener listener)
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