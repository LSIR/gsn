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
 * File: src/gsn/http/restapi/RequestHandler.java
 *
 * @author Milos Stojanovic
 *
 */


package gsn.http.restapi;


import java.util.HashMap;
import java.util.List;

public class Caching {
    private static final long TIME_DIFFERENCE = 3600000; //after 1 hour cach is old
    private static HashMap<String, List<Double>> sensorToLatestVals = new HashMap<String, List<Double>>();
    private static HashMap<String, Long> sensorToTimestamp = new HashMap<String, Long>();
    //TODO use specific diff per sensor
    //private static HashMap<String, Long> sensorToTimeDiff = new HashMap<String, Long>();
    private static boolean isEnabled = false;

    public static List<Double> getLatestValsForSensor(String sensor){
        return sensorToLatestVals.get(sensor);
    }

    public static void setLatestValsForSensor(String sensor, List<Double> latestVals){
        sensorToLatestVals.put(sensor, latestVals);
        setTimestamp(sensor, System.currentTimeMillis());
    }

    public static boolean isEnabled(){
        return isEnabled;
    }

    public static void enable(){
        isEnabled = true;
    }

    public static void disable(){
        isEnabled = false;
    }

    public static void setTimestamp(String sensor, long ts){
        sensorToTimestamp.put(sensor, ts);
    }

    public static boolean isCacheValid(String sensor){
        if (!sensorToTimestamp.containsKey(sensor)) return false;
        return (System.currentTimeMillis() - sensorToTimestamp.get(sensor)) < TIME_DIFFERENCE;
    }
}
