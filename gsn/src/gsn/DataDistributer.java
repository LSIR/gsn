package gsn;

import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.http.rest.DeliverySystem;
import gsn.http.rest.DistributionRequest;
import gsn.storage.DataEnumerator;
import gsn.storage.SQLValidator;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import gsn.storage.StorageManager;
import org.apache.log4j.Logger;

public class DataDistributer implements VirtualSensorDataListener, VSensorStateChangeListener, Runnable {

    public static final int KEEP_ALIVE_PERIOD =  15 * 1000;  // 15 sec.
    public static final int DATA_UPDATE_THREAD_POOL_SIZE = 3;

    private static int keepAlivePeriod = -1;

    private Timer keepAliveTimer = null;

    private static transient Logger logger = Logger.getLogger(DataDistributer.class);

    private static HashMap<Class<? extends DeliverySystem>, DataDistributer> singletonMap = new HashMap<Class<? extends DeliverySystem>, DataDistributer>();
    private Thread distributer_thread;
    
    private ArrayList<ListenerEntry> MyListeners = new ArrayList<ListenerEntry>();
    private LinkedBlockingQueue<ListenerEntry> DataUpdateQueue = new LinkedBlockingQueue<ListenerEntry>();
    private LinkedBlockingQueue<ListenerEntry> DataDistributerQueue = new LinkedBlockingQueue<ListenerEntry>();
    
    private ArrayList<Thread> DataUpdateThreadPool;
    private String delivery_system_name;

    private DataDistributer(String delivery_system_name) {
    	this.delivery_system_name = delivery_system_name;
    	try {
    		distributer_thread = new Thread(this, "DataDistributer["+this.delivery_system_name+"]");
    		distributer_thread.start();
    		// keep alive timer
    		keepAliveTimer = new Timer("keepAliveTimer-" + delivery_system_name);
    		keepAliveTimer.scheduleAtFixedRate( new TimerTask() {

    			@SuppressWarnings("unchecked")
				@Override
				public void run() {
    				ArrayList<ListenerEntry> MyListenersCopy;
    				// write the keep alive message to the stream
    				synchronized (MyListeners) {
    					MyListenersCopy = (ArrayList<ListenerEntry>) MyListeners.clone();
    				}
    				logger.debug("keep alive event, "+MyListenersCopy.size()+" listeners.");
    				try {
    					Iterator<ListenerEntry> i = MyListenersCopy.iterator();
    					while (i.hasNext()) {
    						ListenerEntry listener = i.next();
    						logger.debug("about to send keep alive to listener: "+listener.request.toString());
    						if ( ! listener.request.deliverKeepAliveMessage()) {
    							synchronized (MyListeners) {
   									removeListenerEntry(listener);
    							}
    						}
    						else {
    							logger.debug("sent keep alive to listener: "+listener.request.toString());
    						}
    					}
    				} catch (RuntimeException re) {
    					ByteArrayOutputStream baos = new ByteArrayOutputStream();
    					re.printStackTrace(new PrintStream(baos));
    					logger.error(baos.toString());
    				} 
    			}
    		},   getKeepAlivePeriod(), getKeepAlivePeriod());
    		
    		DataUpdateThreadPool = new ArrayList<Thread>(DATA_UPDATE_THREAD_POOL_SIZE);
    		for (int i = 0; i<DATA_UPDATE_THREAD_POOL_SIZE;i++) {
    			DataUpdateThreadPool.add(new Thread("DataUpdateThread "+i) {

    				private HashMap<StorageManager, Connection> connections = new HashMap<StorageManager, Connection>();
    				private Connection getPersistantConnection(VSensorConfig config) throws SQLException {
    					StorageManager sm = Main.getStorage(config);

    					Connection c = connections.get(sm);
    					if (c == null) {
    						logger.debug("get new connection.");
    						c = sm.getConnection();
    						c.setReadOnly(true);
    						connections.put(sm, c);
    					}
    					return c;
    				}

    				public void run() {
    					DataEnumerator dataEnum;
    					PreparedStatement prepareStatement = null;
    					try {
    						while  (true)
    							try {
    								ListenerEntry listener= DataUpdateQueue.take();
    								logger.debug("Fetching data for listener: " + listener.request.toString()+".");
    								synchronized (MyListeners) {
    									if (listener.removed || listener.request.isClosed()) {
    										logger.debug("Listener was removed: " + listener.request.toString()+".");
    										continue;
    									}
    								}
    								// make statement
    								try {
    									prepareStatement = getPersistantConnection(listener.request.getVSensorConfig()).prepareStatement(listener.query); //prepareStatement = StorageManager.getInstance().getConnection().prepareStatement(query);
    									String maxRows = listener.request.getVSensorConfig().getMainClassInitialParams().get("maxrows");
    									if (maxRows == null) {
    										prepareStatement.setMaxRows(1000); // Limit the number of rows loaded in memory.
    									} else {
    										try {
    											prepareStatement.setMaxRows(Integer.parseInt(maxRows));
    										} catch (NumberFormatException nfe) {
    											logger.warn("maxrows init-param is unparsable, set to default (1000)");
    											prepareStatement.setMaxRows(1000);
    										}
    									}
    									prepareStatement.setLong(1, listener.request.getStartTime());
    									//prepareStatement.setLong(1, listener.getLastVisitedPk());
    									listener.delivery_count =  new Integer(prepareStatement.getMaxRows());
    								} catch (SQLException e) {
    									Main.getStorage(listener.request.getVSensorConfig()).close(prepareStatement);
    									logger.error(e.getMessage(), e);
    									throw new RuntimeException(e);
    								}
    								dataEnum = new DataEnumerator(Main.getStorage(listener.request.getVSensorConfig().getName()), prepareStatement, false, true);
    								synchronized (MyListeners) {
    									listener.setResources(dataEnum, prepareStatement);
    									if (!listener.removed) {
    										if (dataEnum.hasMoreElements()) {
    											logger.debug("Fetching data done for listener: " + listener.request.toString()+".");
    											DataDistributerQueue.add(listener);
    											listener.current_queue = DataDistributerQueue;
    										}
    										else { // no new data found
    											listener.releaseResources();
    											logger.debug("Fetching data done, empty resultset. listener: " + listener.request.toString()+".");
    											// try again if there was an error or when the check flag is set
    											if (dataEnum.hadError() || listener.check_for_new_data) {
    												DataUpdateQueue.put(listener);
    												listener.check_for_new_data = false;
    												listener.current_queue = DataUpdateQueue;
    											}
    											else {
    												listener.current_queue = null;
    											}    											
    										}
    									}
    									else {
    										listener.releaseResources();
    									}
    								}
    							} catch (InterruptedException e) {
    								logger.error(e.getMessage(), e);
    							}
    					}catch (RuntimeException e) {
    						ByteArrayOutputStream baos = new ByteArrayOutputStream();
    						e.printStackTrace(new PrintStream(baos));
    						logger.error(baos.toString());
    					}
    				}
    			});
    			DataUpdateThreadPool.get(DataUpdateThreadPool.size()-1).start();
    		}
    	} catch (Exception e) {
    		ByteArrayOutputStream baos = new ByteArrayOutputStream();
			e.printStackTrace(new PrintStream(baos));
			logger.error(baos.toString());
    		throw new RuntimeException(e);
    	}
    }

    public static DataDistributer getInstance(Class<? extends DeliverySystem> c) {
        DataDistributer toReturn = singletonMap.get(c);
        if (toReturn == null) {
            singletonMap.put(c, (toReturn = new DataDistributer(c.getName())));
        }
        return toReturn;
    }

    public static int getKeepAlivePeriod() {
        if (keepAlivePeriod == -1)
            keepAlivePeriod = System.getProperty("remoteKeepAlivePeriod") == null ? KEEP_ALIVE_PERIOD : Integer.parseInt(System.getProperty("remoteKeepAlivePeriod"));
        return keepAlivePeriod;
    }

    public void addListener(DistributionRequest request) {
        synchronized (MyListeners) {
            if (getListenerEntry(request)==null) {
            	ListenerEntry newListener = new ListenerEntry(request);
                logger.warn("Adding a listener to Distributer:" + request.toString());
                boolean needsAnd = SQLValidator.removeSingleQuotes(SQLValidator.removeQuotes(request.getQuery())).indexOf(" where ") > 0;
                //String query = SQLValidator.addPkField(listener.getQuery());
                String query = request.getQuery();
                if (needsAnd)
                    query += " AND ";
                else
                    query += " WHERE ";
                //query += " timed > " + listener.getStartTime() + " and pk > ? order by timed asc ";
                query += " timed > ? order by timed asc ";
                newListener.query = query;
                MyListeners.add(newListener);
                newListener.request.getDeliverySystem().setTimeout(getKeepAlivePeriod() * 2);
                moveListenerToQueue(newListener, DataUpdateQueue);
            } else {
                logger.warn("Adding a listener to Distributer failed, duplicated listener! " + request.toString());
            }
        }
    }

    /**
     * This method only flushes one single stream element from the provided data enumerator.
     * Returns false if the flushing the stream element fails.
     *
     * @param dataEnum
     * @param listener
     * @return
     */
    private boolean flushStreamElement(DataEnumerator dataEnum, DistributionRequest request) {
        if (request.isClosed()) {
            logger.debug("Flushing an stream element failed, isClosed=true [Listener: " + request.toString() + "]");
            return false;
        }
        StreamElement se = dataEnum.nextElement();
        //		boolean success = true;
        boolean success = request.deliverStreamElement(se); // This could take some time if db tables are locked (Local delivery)
        if (!success) {
            logger.debug("FLushing an stream element failed, delivery failure [Listener: " + request.toString() + "]");
            return false;
        }
        logger.debug("Flushing an stream element succeed [Listener: " + request.toString() + "]");
        return true;
    }

    public void removeListener(DistributionRequest request) {
    	synchronized (MyListeners) {
    		ListenerEntry listener = getListenerEntry(request);
    		if (listener==null) {
    			logger.warn("Can't remove unregistered listener from distributer [Listener: " + request.toString() + "]");
    		}
    		else {
    			removeListenerEntry(listener);
    		}
    	}
    }

    public void consume(StreamElement se, VSensorConfig config) {
        synchronized (MyListeners) {
            for ( ListenerEntry listener : MyListeners)
                if (listener.request.getVSensorConfig() == config) {
                    logger.debug("sending stream element " + (se == null ? "second-chance-se" : se.toString()) + " produced by " + config.getName() + " to listener =>" + listener.request.toString());
                    if (listener.current_queue == null)
                    	moveListenerToQueue(listener, DataUpdateQueue);
                    else
                        listener.check_for_new_data = true;
                }
        }
    }

    public void run() {
    	try {
    		while (true) {
    			try {
    				if (DataDistributerQueue.isEmpty()) {
    					logger.debug("Waiting(locked) for requests or data items, Number of total listeners: " + MyListeners.size()+" ["+delivery_system_name+"]");
    					DataDistributerQueue.put(DataDistributerQueue.take());
    					logger.debug("Lock released, trying to find interest listeners (total listeners:" + MyListeners.size() + " ["+delivery_system_name+"])");
    				}
    			} catch (InterruptedException e) {
    				logger.error(e.getMessage(), e);
    			}

    			Iterator<ListenerEntry> i = DataDistributerQueue.iterator();
    			while (i.hasNext()) {
    				ListenerEntry listener = i.next();
    				synchronized (MyListeners) {
    					if (listener.removed) {
    						i.remove();
    						listener.releaseResources();
    						continue;
    					}
    				}
    				if (!listener.dataEnum.hasMoreElements()) {
    					synchronized (MyListeners) {
    						i.remove();
    						listener.releaseResources();
       						if (listener.removed)
    							continue;
       						listener.current_queue = null;
       						if (logger.isDebugEnabled()) {
       							logger.debug("deliverycount = "+ listener.delivery_count);
       							if (listener.delivery_count==0) {
       								logger.debug("reached maxrows, look for new data for [Listener: "+listener.request.toString()+"]");
       							}
       							if (listener.check_for_new_data) {
       								logger.debug("new data available, look for new data for [Listener: "+listener.request.toString()+"]");
       							}    						
       						}
    						if (listener.delivery_count==0 || listener.check_for_new_data) {
    							listener.check_for_new_data = false;
    							listener.current_queue = DataUpdateQueue;
    							DataUpdateQueue.add(listener);
    						}
    					}
    				}
    				else {
    					boolean success = flushStreamElement(listener.dataEnum, listener.request);
    					if (success == false) {
    						i.remove();
    						synchronized (MyListeners) {
   								removeListenerEntry(listener);
   								listener.releaseResources();
    						}
    					}
    					else {
    						listener.delivery_count--;
    					}
    				}
    			}
    		}

    	} catch (RuntimeException e) {
    		ByteArrayOutputStream baos = new ByteArrayOutputStream();
    		e.printStackTrace(new PrintStream(baos));
    		logger.error(baos.toString());
    	} 
    }

    public boolean vsLoading(VSensorConfig config) {
        return true;
    }

    public boolean vsUnLoading(VSensorConfig config) {
        synchronized (MyListeners) {
            logger.debug("Distributer unloading: " + MyListeners.size() +" ["+delivery_system_name+"]");
            Iterator<ListenerEntry> i = MyListeners.iterator();
            while (i.hasNext()) {
            	ListenerEntry listener = i.next();
            	if (listener.request.getVSensorConfig() == config) {
            		i.remove();
            		listener.removed = true;
            		listener.request.close();
            		logger.debug("remove the listener: "+listener.request.toString());
            	}            		
            }
        }
        return true;
    }

    public void release() {
    }

    public boolean contains(DeliverySystem delivery) {
        synchronized (MyListeners) {
            for (ListenerEntry listener : MyListeners)
                if (listener.request.getDeliverySystem().equals(delivery))
                    return true;
            return false;
		}
	}
    
    
    /* Each listener entry traverses following steps:
     * 
     *  newDataAvailable -- [DataUpdateQueue] --> get data -- [DataDistributerQueue] --> distribute data --\
     *                                                                                                     |
     *       ^---------------------------------------------------------------------------------------------/
     * 
     * Available queues in the delivery system:
     * 
     * 
     * 
     */
    
    // queue moving methods for listeners
    
    /*
     * remove a ListenerEntry from the list. There should only be synchronized calls to this method
     */
    private void removeListenerEntry(ListenerEntry listener) {
    	if (!listener.removed) {
    		MyListeners.remove(listener);
    		listener.removed = true;
    		listener.request.close();
    		logger.debug("remove the listener: "+listener.request.toString());
    	}
    }
    
    private ListenerEntry getListenerEntry(DistributionRequest request) {
    	for (ListenerEntry listener: MyListeners) {
    		if (listener.request.equals(request))
    			return listener;
    	}
    	return null;
    }
    
    private void moveListenerToQueue(ListenerEntry listener, Collection<ListenerEntry> c) {
    	if (listener.current_queue != null) {
    		logger.warn("current queue was not null while assigning new queue.");
    	}
    	if (listener.removed) {
    		listener.current_queue = null;
    	}
    	else {
    	   	listener.current_queue = c;
   	   		c.add(listener);
    	}
    }
    
    private class ListenerEntry {
    	DistributionRequest request;
    	Integer delivery_count = 0;
    	private PreparedStatement statement = null;
    	String query = null;
    	private DataEnumerator dataEnum = null;
    	Boolean check_for_new_data = false; // indicates that new data may had become available during fetching or delivering data
    	Boolean removed = false; // indicates whether this listener has been removed 
    	Collection<ListenerEntry> current_queue = null;    	
    	
    	public ListenerEntry(DistributionRequest request) {
    		this.request = request;
    	}
    	
    	public void setResources(DataEnumerator dataEnum,
				PreparedStatement prepareStatement) {
    		this.dataEnum = dataEnum;
    		this.statement = prepareStatement;
		}

		public void releaseResources() {
    		// close datenum
    		if (dataEnum != null)
    			dataEnum.close();
    		// close statement
    		Main.getStorage(request.getVSensorConfig()).close(statement);
    	}
    }
}
