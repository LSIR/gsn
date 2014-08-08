/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
* 
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: src/gsn/ModelDistributer.java
*
* @author Julien Eberle
*
*/

package gsn;

import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.http.rest.DeliverySystem;
import gsn.http.rest.DistributionRequest;
import gsn.storage.DataEnumerator;
import gsn.storage.DataEnumeratorIF;
import gsn.storage.ModelEnumerator;
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

public class ModelDistributer implements VirtualSensorDataListener, VSensorStateChangeListener, Runnable {

    public static final int KEEP_ALIVE_PERIOD =  15 * 1000;  // 15 sec.

    private static int keepAlivePeriod = -1;

    private javax.swing.Timer keepAliveTimer = null;

    private static transient Logger logger = Logger.getLogger(ModelDistributer.class);

    private static HashMap<Class<? extends DeliverySystem>, ModelDistributer> singletonMap = new HashMap<Class<? extends DeliverySystem>, ModelDistributer>();
    private Thread thread;

    private ModelDistributer() {
        try {
            thread = new Thread(this);
            thread.start();
            // Start the keep alive Timer -- Note that the implementation is backed by one single thread for all the Delivery instances.
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
        //} catch (SQLException e) {
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ModelDistributer getInstance(Class<? extends DeliverySystem> c) {
        ModelDistributer toReturn = singletonMap.get(c);
        if (toReturn == null)
            singletonMap.put(c, (toReturn = new ModelDistributer()));
        return toReturn;
    }

    public static int getKeepAlivePeriod() {
        if (keepAlivePeriod == -1)
            keepAlivePeriod = System.getProperty("remoteKeepAlivePeriod") == null ? KEEP_ALIVE_PERIOD : Integer.parseInt(System.getProperty("remoteKeepAlivePeriod"));
        return keepAlivePeriod;
    }

    private ArrayList<DistributionRequest> listeners = new ArrayList<DistributionRequest>();

    private LinkedBlockingQueue<DistributionRequest> locker = new LinkedBlockingQueue<DistributionRequest>();
    
    private ConcurrentHashMap<DistributionRequest, DataEnumeratorIF> candidateListeners = new ConcurrentHashMap<DistributionRequest, DataEnumeratorIF>();

    private ConcurrentHashMap<DistributionRequest, Boolean> candidatesForNextRound = new ConcurrentHashMap<DistributionRequest, Boolean>();

    public void addListener(DistributionRequest listener) {
        synchronized (listeners) {
            if (!listeners.contains(listener)) {
                logger.warn("Adding a listener to ModelDistributer:" + listener.toString());
                
                listeners.add(listener);
                addListenerToCandidates(listener);

            } else {
                logger.warn("Adding a listener to ModelDistributer failed, duplicated listener! " + listener.toString());
            }
        }
    }


    private void addListenerToCandidates(DistributionRequest listener) {
        /**
         * Locker variable should be modified EXACTLY like candidateListeners variable.
         */
        logger.debug("Adding the listener: " + listener.toString() + " to the candidates.");
        DataEnumeratorIF dataEnum = makeDataEnum(listener);
        if (dataEnum.hasMoreElements()) {
            candidateListeners.put(listener, dataEnum);
            locker.add(listener);
        }
    }

    private void removeListenerFromCandidates(DistributionRequest listener) {
        /**
         * Locker variable should be modified EXACTLY like candidateListeners variable.
         */
        logger.debug("Updating the candidate list [" + listener.toString() + " (removed)].");
        if (candidatesForNextRound.contains(listener)) {
            candidateListeners.put(listener, makeDataEnum(listener));
            candidatesForNextRound.remove(listener);
        } else {
            locker.remove(listener);
            candidateListeners.remove(listener);
        }
    }

    /**
     * This method only flushes one single stream element from the provided data enumerator.
     * Returns false if the flushing the stream element fails. This method also cleans the prepared statements by removing the listener completely.
     *
     * @param dataEnum
     * @param listener
     * @return
     */
    private boolean flushStreamElement(DataEnumeratorIF dataEnum, DistributionRequest listener) {
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
            logger.warn("FLushing an stream element failed, delivery failure [Listener: " + listener.toString() + "]");
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
                    listener.close();
                    logger.debug("Removing listener completely from Distributer [Listener: " + listener.toString() + "]");
            }
        }
    }

    public void consume(StreamElement se, VSensorConfig config) {
        synchronized (listeners) {
            for (DistributionRequest listener : listeners)
                if (listener.getVSensorConfig() == config) {
                    logger.debug("sending stream element " + (se == null ? "second-chance-se" : se.toString()) + " produced by " + config.getName() + " to listener =>" + listener.toString());
                    if (!candidateListeners.containsKey(listener)) {
                        addListenerToCandidates(listener);
                    } else {
                        candidatesForNextRound.put(listener, Boolean.TRUE);
                    }
                }
        }
    }

    public void run() {
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


            for (Entry<DistributionRequest, DataEnumeratorIF> item : candidateListeners.entrySet()) {
                boolean success = flushStreamElement(item.getValue(), item.getKey());
                if (success == false)
                    removeListener(item.getKey());
                else {
                    if (!item.getValue().hasMoreElements()) {
                        removeListenerFromCandidates(item.getKey());
                        // As we are limiting the number of elements returned by the JDBC driver
                        // we consume the eventual remaining items.
                       // consume(null, item.getKey().getVSensorConfig()); //do not activate for models !!!!!
                    }
                }
            }
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

    private DataEnumeratorIF makeDataEnum(DistributionRequest listener) {
        ModelEnumerator mEnum = new ModelEnumerator(listener.getQuery(),listener.getModel());
        return mEnum;
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


