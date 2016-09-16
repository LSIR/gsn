/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2016, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
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
* File: src/ch/epfl/gsn/VirtualSensor.java
*
* @author Timotee Maret
*
*/

package ch.epfl.gsn;

import org.slf4j.LoggerFactory;

import ch.epfl.gsn.Main;
import ch.epfl.gsn.VirtualSensor;
import ch.epfl.gsn.VirtualSensorInitializationFailedException;
import ch.epfl.gsn.beans.InputStream;
import ch.epfl.gsn.beans.StreamSource;
import ch.epfl.gsn.beans.VSensorConfig;
import ch.epfl.gsn.vsensor.AbstractVirtualSensor;
import ch.epfl.gsn.wrappers.AbstractWrapper;

import org.slf4j.Logger;

import java.io.File;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;

public class VirtualSensor {

    private static final transient Logger logger = LoggerFactory.getLogger(VirtualSensor.class);
    private static final int GARBAGE_COLLECTOR_INTERVAL = 2;

    private AbstractVirtualSensor virtualSensor = null;
    private VSensorConfig config = null;
    private long lastModified = -1;
    private int noOfCallsToReturnVS = 0;

    public VirtualSensor(VSensorConfig config) {
        this.config = config;
        this.lastModified = new File(config.getFileName()).lastModified();
    }

    public synchronized AbstractVirtualSensor borrowVS() throws VirtualSensorInitializationFailedException {
        if (virtualSensor == null) {
            try {
                virtualSensor = (AbstractVirtualSensor) Class.forName(config.getProcessingClass()).newInstance();
                virtualSensor.setVirtualSensorConfiguration(config);
            } catch (Exception e) {
                throw new VirtualSensorInitializationFailedException(e.getMessage(), e);
            }
            if (virtualSensor.initialize_wrapper() == false) {
                virtualSensor = null;
                throw new VirtualSensorInitializationFailedException();
            }
            logger.debug("Created a new instance for VS " + config.getName());
        }
        return virtualSensor;
    }

    /**
     * The method ignores the call if the input is null
     *
     * @param o
     */
    public synchronized void returnVS(AbstractVirtualSensor o) {
        if (o == null) return;
        if (++noOfCallsToReturnVS % GARBAGE_COLLECTOR_INTERVAL == 0)
            DoUselessDataRemoval();
    }

    public synchronized void closePool() {
        if (virtualSensor != null) {
            virtualSensor.dispose_decorated();
            logger.debug("VS " + config.getName() + " is now released.");
        } else {
            logger.debug("VS " + config.getName() + " was already released.");
        }
    }

    public void start() throws VirtualSensorInitializationFailedException {
        
        /*
         *  Starting wrapper threads and storing their ids and names in AbstractVirtualSensor's 
         *  HashMap threads for monitoring
         */
        
        Map <Long, String> threads = new HashMap <Long, String>();
        for (InputStream inputStream : config.getInputStreams()) {
            for (StreamSource streamSource : inputStream.getSources()) {
                AbstractWrapper wrapper = streamSource.getWrapper();
                wrapper.start();
                threads.put(wrapper.getId(),wrapper.getName());
            }
        }
        borrowVS();

        virtualSensor.setThreads(threads);
    }

    /**
     * @return the config
     */
    public VSensorConfig getConfig() {
        return config;
    }

    /**
     * @return the lastModified
     */
    public long getLastModified() {
        return lastModified;
    }

    public void dispose() {
    }

    // apply the storage size parameter to the virtual sensor table
    public void DoUselessDataRemoval() {
        if (config.getParsedStorageSize() == VSensorConfig.STORAGE_SIZE_NOT_SET) return;
        StringBuilder query;

        if (config.isStorageCountBased()) {
            query = Main.getStorage(config.getName()).getStatementRemoveUselessDataCountBased(config.getName(), config.getParsedStorageSize());
        }
        else {
            query = Main.getStorage(config.getName()).getStatementRemoveUselessDataTimeBased(config.getName(), config.getParsedStorageSize());
        }

        int effected = 0;
        try {
            logger.debug("Enforcing the limit size on the VS table by : " + query);
            effected = Main.getStorage(config.getName()).executeUpdate(query);
        } catch (SQLException e) {
            logger.error("Error in executing: " + query + ". "+ e.getMessage());
        }
        logger.debug("There were " + effected + " old rows dropped from " + config.getName());
    }
}

