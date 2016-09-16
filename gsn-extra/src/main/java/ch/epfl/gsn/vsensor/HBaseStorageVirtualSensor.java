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
* File: src/ch/epfl/gsn/vsensor/HBaseStorageVirtualSensor.java
*
* @author Ivo Dimitrov
*
*/

package ch.epfl.gsn.vsensor;

import org.slf4j.LoggerFactory;

import ch.epfl.gsn.beans.StreamElement;
import ch.epfl.gsn.beans.VSensorConfig;
import ch.epfl.gsn.vsensor.AbstractVirtualSensor;
import ch.epfl.gsn.wrappers.storext.HBaseConnector;

import org.slf4j.Logger;

import java.io.File;
import java.util.TreeMap;

/** STORES THE OUTPUT OF THE VS TO HBASE **/
/** BASED ON THE BRIDGEVIRTUALSENSOR PROCESSING CLASS **/
public class HBaseStorageVirtualSensor extends AbstractVirtualSensor {
    File file;
    private static final transient Logger logger = LoggerFactory.getLogger(HBaseStorageVirtualSensor.class);
    private boolean allow_nulls = true; // by default allow nulls
    private HBaseConnector hbase;
    private Long timestamp;
    private int sensorNumber;
    private String tablename;

    private String entries;      //
    private long startTime;
    private long estimatedTime;
    private int counter = 0;
    private int limit;


    public boolean initialize() {
        //System.out.println("***The Virtual Sensor is initialized");
        VSensorConfig vsensor = getVirtualSensorConfiguration();
        try {
            estimatedTime = 0;
            //sensorNumber = globalSensorNumber;
            //System.out.println("@@@The sensor is = "+sensorNumber);
            hbase = new HBaseConnector();
            tablename = "measurements_"+vsensor.getName();
            String[] family = { "columnFamily" };
           // if (hbase.tableExists(tablename))
           //     hbase.deleteTable(tablename);
           // hbase.createTable(tablename, family);       // initialize the table that is going to be created

           // globalSensorNumber++;
           // rowNumber=1;
        } catch (Exception e) {
        	logger.error(e.getMessage(), e);
        }

        TreeMap<String, String> params = vsensor.getMainClassInitialParams();
        entries = params.get("entries");   //
        limit = Integer.parseInt(entries);       //
      //System.out.println("The limit is" + limit);
        String allow_nulls_str = params.get("allow-nulls");
        if (allow_nulls_str != null)
            allow_nulls = allow_nulls_str.equalsIgnoreCase("true");

        return true;
    }

    public void dataAvailable(String inputStreamName, StreamElement data) {
        counter++;        //

        if (allow_nulls)  {
            dataProduced(data);
            //System.out.println("***DATA AVAILABLE");
            //System.out.println("***DATA = "+data.toString());
            timestamp = Long.MAX_VALUE - System.currentTimeMillis();     // get the time in a decreasing order
            String[] fieldNames = data.getFieldNames();
            for(String field : fieldNames) {
                //System.out.println("@@@ Table is"+sensorNumber+"The row number is = " + rowNumber);
                //System.out.println("***The field = "+field+" has value = "+data.getData(field));
                try {
                    startTime = System.nanoTime();
                    hbase.addRecord(tablename, timestamp.toString(), "columnFamily", field, data.getData(field).toString());
                    estimatedTime += (System.nanoTime() - startTime);
                } catch (Exception e) {
                	logger.error(e.getMessage(), e);
                }
            }
            if (counter >= limit) {
                double seconds = (double)estimatedTime / 1000000000.0;
                logger.trace("The estimated time (sec) is = "+seconds);
            }
            //if ((counter % 1000) == 0) System.out.println("Entry = "+counter);
            if ((counter % 1000) == 0) {
            	logger.trace("Up until the Entry = "+counter);
                double seconds = (double)estimatedTime / 1000000000.0;
                logger.info("*** ESTIMATED TIME (SEC) for counter = "+counter+" IS "+seconds);
            }
           // rowNumber++;
        } else {
            if (!areAllFieldsNull(data))
                dataProduced(data);
            else {
                 logger.debug("Nulls received for timestamp (" + data.getTimeStamp() + "), discarded");
            }
        }
        logger.debug("Data received under the name: " + inputStreamName);
    }

    public boolean areAllFieldsNull(StreamElement data) {
        boolean allFieldsNull = false;
        for (int i = 0; i < data.getData().length; i++)
            if (data.getData()[i] == null) {
                allFieldsNull = true;
                break;
            }

        return allFieldsNull;
    }

    public void dispose() {

    }

}
