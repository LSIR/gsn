package gsn;

import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.http.rest.DeliverySystem;
import gsn.http.rest.DistributionRequest;
import gsn.storage.DataEnumerator;
import gsn.storage.SQLValidator;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import gsn.storage.StorageManager;
import org.apache.log4j.Logger;

public class DataDistributer implements VirtualSensorDataListener, VSensorStateChangeListener, Runnable {

    public static final int KEEP_ALIVE_PERIOD =  15 * 1000;  // 15 sec.
    public static final int DATA_UPDATE_THREAD_POOL_SIZE = 3;

    private static int keepAlivePeriod = -1;

    private javax.swing.Timer keepAliveTimer = null;

    private static transient Logger logger = Logger.getLogger(DataDistributer.class);

    private static HashMap<Class<? extends DeliverySystem>, DataDistributer> singletonMap = new HashMap<Class<? extends DeliverySystem>, DataDistributer>();
    private Thread thread;
    
    private ArrayList<Thread> DataUpdateThreadPool;
    private ArrayList<DistributionRequest> FetchList = new ArrayList<DistributionRequest>();
    private LinkedBlockingQueue<DistributionRequest> DataUpdateQueue = new LinkedBlockingQueue<DistributionRequest>();
    private ConcurrentHashMap<DistributionRequest, Integer> DeliveryCount = new ConcurrentHashMap<DistributionRequest, Integer>();

    private DataDistributer() {
        try {
            thread = new Thread(this);
            thread.start();
            // Start the keep alive Timer -- Note that the implementation is backed by one single thread for all the RestDelivery instances.
            keepAliveTimer = new  javax.swing.Timer(getKeepAlivePeriod(), new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // write the keep alive message to the stream
                    synchronized (listeners) {
                        ArrayList<DistributionRequest> clisteners = (ArrayList<DistributionRequest>) listeners.clone();
                        for (DistributionRequest listener : clisteners) {
                            if ( ! listener.deliverKeepAliveMessage()) {
                                logger.debug("remove the listener.");
                                removeListener(listener);
                            }
                        }
                    }
                }
            });
            keepAliveTimer.start();
            DataUpdateThreadPool = new ArrayList<Thread>(DATA_UPDATE_THREAD_POOL_SIZE);
            for (int i = 0; i<DATA_UPDATE_THREAD_POOL_SIZE;i++) {
            	DataUpdateThreadPool.add(new Thread("DataUpdateThread "+i) {
            		
            	    private HashMap<StorageManager, Connection> connections = new HashMap<StorageManager, Connection>();
            	    private Connection getPersistantConnection(VSensorConfig config) throws Exception {
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
            				DistributionRequest listener= DataUpdateQueue.take();
            				logger.debug("Fetching data for listener: " + listener.toString()+".");
            				synchronized (listener) {
            					if (listener.isClosed())
            						continue;
            					// make statement
            	                try {
            	                    prepareStatement = getPersistantConnection(listener.getVSensorConfig()).prepareStatement(queries.get(listener)); //prepareStatement = StorageManager.getInstance().getConnection().prepareStatement(query);
            	                    String maxRows = listener.getVSensorConfig().getMainClassInitialParams().get("maxrows");
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
            			            prepareStatement.setLong(1, listener.getStartTime());
            			            //prepareStatement.setLong(1, listener.getLastVisitedPk());
            			            DeliveryCount.put(listener, new Integer(prepareStatement.getMaxRows()));
            	                } catch (Exception e) {
            			            logger.error(e.getMessage(), e);
            	                    throw new RuntimeException(e);
            	                }
            			        dataEnum = new DataEnumerator(Main.getStorage(listener.getVSensorConfig().getName()), prepareStatement, false, true);
            				}
            				synchronized (listeners) {
            					if (dataEnum.hasMoreElements()) {
			        				if (listeners.contains(listener)) {
			        					// an element is only put to DataUpdateQueue if it is not in candidateListeners
			        					logger.debug("Fetching data done for listener: " + listener.toString()+".");
			        					candidateListeners.put(listener, dataEnum);
	            			            preparedStatements.put(listener, prepareStatement);
			        					locker.add(listener);
			        					FetchList.remove(listener);
			        				}
								}
            					else {
            						prepareStatement.close();
            						FetchList.remove(listener);
            						logger.debug("Fetching data done, empty resultset. listener: " + listener.toString()+".");
            					}
			        		}
						} catch (InterruptedException e) {
							logger.error(e.getMessage(), e);
						} catch (SQLException e) {
							logger.error(e.getMessage());
						}
            			}catch (RuntimeException e) {
            	    		logger.error(e.getMessage());
            	    	}
            		}
            	});
            	DataUpdateThreadPool.get(DataUpdateThreadPool.size()-1).start();
            }
        //} catch (SQLException e) {
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static DataDistributer getInstance(Class<? extends DeliverySystem> c) {
        DataDistributer toReturn = singletonMap.get(c);
        if (toReturn == null)
            singletonMap.put(c, (toReturn = new DataDistributer()));
        return toReturn;
    }

    public static int getKeepAlivePeriod() {
        if (keepAlivePeriod == -1)
            keepAlivePeriod = System.getProperty("remoteKeepAlivePeriod") == null ? KEEP_ALIVE_PERIOD : Integer.parseInt(System.getProperty("remoteKeepAlivePeriod"));
        return keepAlivePeriod;
    }
    
    private HashMap<DistributionRequest, PreparedStatement> preparedStatements = new HashMap<DistributionRequest, PreparedStatement>();
    private HashMap<DistributionRequest, String> queries = new HashMap<DistributionRequest, String>();
    private ArrayList<DistributionRequest> listeners = new ArrayList<DistributionRequest>();

    private ConcurrentHashMap<DistributionRequest, DataEnumerator> candidateListeners = new ConcurrentHashMap<DistributionRequest, DataEnumerator>();

    private LinkedBlockingQueue<DistributionRequest> locker = new LinkedBlockingQueue<DistributionRequest>();

    private ConcurrentHashMap<DistributionRequest, Boolean> candidatesForNextRound = new ConcurrentHashMap<DistributionRequest, Boolean>();
    
    public void addListener(DistributionRequest listener) {
        synchronized (listeners) {
            if (!listeners.contains(listener)) {
                logger.warn("Adding a listener to Distributer:" + listener.toString());
                boolean needsAnd = SQLValidator.removeSingleQuotes(SQLValidator.removeQuotes(listener.getQuery())).indexOf(" where ") > 0;
                //String query = SQLValidator.addPkField(listener.getQuery());
                String query = listener.getQuery();
                if (needsAnd)
                    query += " AND ";
                else
                    query += " WHERE ";
                //query += " timed > " + listener.getStartTime() + " and pk > ? order by timed asc ";
                query += " timed > ? order by timed asc ";
                queries.put(listener, query);
                listeners.add(listener);
                addListenerToCandidates(listener);
            } else {
                logger.warn("Adding a listener to Distributer failed, duplicated listener! " + listener.toString());
            }
        }
    }


    private boolean addListenerToCandidates(DistributionRequest listener) {
        /**
         * Locker variable should be modified EXACTLY like candidateListeners variable.
         */
       	if (!DataUpdateQueue.contains(listener) && !candidateListeners.containsKey(listener) && !FetchList.contains(listener)) {
            logger.debug("Adding the listener: " + listener.toString() + " to the candidates.");
            FetchList.add(listener);
       		try {
       			DataUpdateQueue.put(listener);
       			logger.debug(DataUpdateQueue.size() + " Entries in the update queue.");
       		} catch (InterruptedException e) {
       			FetchList.remove(listener);
       			logger.error(e.getMessage(), e);
       		}
       		return true;
        }
       	else {
       		if (DataUpdateQueue.contains(listener))
       			logger.debug("Listener in update queue, not added. listener: " + listener.toString());
       		if (candidateListeners.containsKey(listener))
       			logger.debug("Listener in candidateListeners, not added. listener: " + listener.toString());
       		if (FetchList.contains(listener))
       			logger.debug("Listener in FetchList, not added. listener: " + listener.toString());
       		return false;
       	}
    }

    private void removeListenerFromCandidates(DistributionRequest listener) {
        /**
         * Locker variable should be modified EXACTLY like candidateListeners variable.
         */
        logger.debug("Updating the candidate list [" + listener.toString() + " (removed)].");
        locker.remove(listener);
        try {
        	preparedStatements.get(listener).close();
        } catch (SQLException e) {
        	logger.error(e.getMessage(), e);
        } finally {
        	preparedStatements.remove(listener);
        }
        candidateListeners.remove(listener);
        DeliveryCount.remove(listener);
        if (candidatesForNextRound.containsKey(listener)) {
        	addListenerToCandidates(listener);
        	candidatesForNextRound.remove(listener);
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
    private boolean flushStreamElement(DataEnumerator dataEnum, DistributionRequest listener) {
        if (listener.isClosed()) {
            logger.debug("Flushing an stream element failed, isClosed=true [Listener: " + listener.toString() + "]");
            return false;
        }

        if (!dataEnum.hasMoreElements()) {
            logger.debug("Nothing to flush to [Listener: " + listener.toString() + "]");
            return true;
        }

        StreamElement se = dataEnum.nextElement();
        //		boolean success = true;
        boolean success = listener.deliverStreamElement(se);
        if (!success) {
            logger.debug("FLushing an stream element failed, delivery failure [Listener: " + listener.toString() + "]");
            return false;
        }
        logger.debug("Flushing an stream element succeed [Listener: " + listener.toString() + "]");
        return true;
    }

    public void removeListener(DistributionRequest listener) {
    	synchronized (listeners) {
    		if (listeners.remove(listener)) {
    			candidatesForNextRound.remove(listener);
    			removeListenerFromCandidates(listener);
    			FetchList.remove(listener);
    			synchronized (listener) {
    				listener.close();						
    			}
    			logger.warn("Removing listener completely from Distributer [Listener: " + listener.toString() + "]");
    		}
    	}
    }

    public void consume(StreamElement se, VSensorConfig config) {
        synchronized (listeners) {
            for (DistributionRequest listener : listeners)
                if (listener.getVSensorConfig() == config) {
                    logger.debug("sending stream element " + (se == null ? "second-chance-se" : se.toString()) + " produced by " + config.getName() + " to listener =>" + listener.toString());
                    if (!addListenerToCandidates(listener))
                    	candidatesForNextRound.put(listener, Boolean.TRUE);
                }
        }
    }

    public void run() {
    	try {
        while (true) {
            try {
                if (locker.isEmpty()) {
                    logger.debug("Waiting(locked) for requests or data items, Number of total listeners: " + listeners.size());
                    locker.put(locker.take());
                    logger.debug("Lock released, trying to find interest listeners (total listeners:" + listeners.size() + ")");
                }
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }


            for (Entry<DistributionRequest, DataEnumerator> item : candidateListeners.entrySet()) {
            	boolean success = flushStreamElement(item.getValue(), item.getKey());
                if (success == false)
                    removeListener(item.getKey());
                else {
                	DeliveryCount.put(item.getKey(), DeliveryCount.get(item.getKey()).intValue()-1);
                    if (!item.getValue().hasMoreElements()) {
                    	logger.debug("deliverycount = "+ DeliveryCount.get(item.getKey()).intValue());
                    	if (DeliveryCount.get(item.getKey()).intValue()==0) {
                    		logger.debug("reached maxrows, look for new data for [Listener: "+item.getKey().toString()+"]");
                    		candidatesForNextRound.put(item.getKey(), Boolean.TRUE);
                    	}
                    	synchronized(listeners) {
                        	removeListenerFromCandidates(item.getKey());
                    	}
                    }
                }
            }
        }
    	}catch (RuntimeException e) {
    		logger.error(e.getMessage());
    	}
    }

    public boolean vsLoading(VSensorConfig config) {
        return true;
    }

    public boolean vsUnLoading(VSensorConfig config) {
        synchronized (listeners) {
            logger.debug("Distributer unloading: " + listeners.size());
            ArrayList<DistributionRequest> toRemove = new ArrayList<DistributionRequest>();
            for (DistributionRequest listener : listeners) {
                if (listener.getVSensorConfig() == config)
                    toRemove.add(listener);
            }
            for (DistributionRequest listener : toRemove) {
                try {
                    removeListener(listener);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        return true;
    }

    public void release() {
        synchronized (listeners) {
            while (!listeners.isEmpty())
                removeListener(listeners.get(0));
        }
        if (keepAliveTimer != null)
            keepAliveTimer.stop();
    }

    public boolean contains(DeliverySystem delivery) {
        synchronized (listeners) {
            for (DistributionRequest listener : listeners)
                if (listener.getDeliverySystem().equals(delivery))
                    return true;
            return false;
		}

	}

}


