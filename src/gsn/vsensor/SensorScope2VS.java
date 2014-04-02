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
* File: src/gsn/vsensor/SensorScope2VS.java
*
* @author Sofiane Sarni
*
*/

package gsn.vsensor;

import org.apache.log4j.Logger;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;

import java.util.TreeMap;
import java.text.NumberFormat;

public class SensorScope2VS extends AbstractVirtualSensor {

    private static final transient Logger logger = Logger.getLogger(SensorScope2VS.class);

    private Double[] buffer;

    private int length = -1;
    private NumberFormat nf = NumberFormat.getInstance();

    private boolean allowNulls = false;

    public boolean initialize() {
        VSensorConfig vsensor = getVirtualSensorConfiguration();
        TreeMap<String, String> params = vsensor.getMainClassInitialParams();

        String allow_nulls_str = params.get("allow-nulls");

        if (allow_nulls_str != null)
            if (allow_nulls_str.trim().equalsIgnoreCase("true"))
                allowNulls = true;


        logger.warn("Allow nulls => " + allowNulls);

        length = vsensor.getOutputStructure().length;

        buffer = new Double[length];

        return true;
    }

    public void dataAvailable(String inputStreamName, StreamElement data) {


        logger.debug("Data => " + data.toString());

        // verify if all tuples are null, avoids duplicating data using buffer
        boolean nullpacket = true;
        for (int i = 0; i < length; i++) {
            if ((Double) data.getData()[i] != null)
                nullpacket = false;
        }
        if (nullpacket) {
            logger.debug("Completely empty packet (all nulls). Discarded" + "\nData: " + data);
            return;
        }

        // if nulls are allowed
        if (allowNulls) {
            dataProduced(data);
            if (logger.isDebugEnabled())
                logger.debug("Data received under the name: " + inputStreamName + "\nData: " + data);
            return;
        }

        // if nulls are not allowed, using buffer

        for (int i = 0; i < length; i++) {
            Double d = (Double) data.getData()[i];
            if (d != null)
                buffer[i] = d;
        }

        /*
        * check if buffer contains any null values
        * */

        boolean publish = true;

        for (int i = 0; i < length; i++) {
            data.setData(i, buffer[i]);
            if (buffer[i] == null) publish = false;
        }

        //logger.debug("Pub => " + data.toString());

        if (publish) {
            dataProduced(data);
            if (logger.isDebugEnabled())
                logger.debug("Data received under the name: " + inputStreamName + "\nData: " + data);
        } else {
            logger.debug("null values, not published (" + this.getVirtualSensorConfiguration().getName() + ")");
        }
    }

    public void dispose() {

    }

}
