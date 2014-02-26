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
* File: src/gsn/utils/TestLSM.java
*
* @author Sofiane Sarni
*
*/

package gsn.utils;

import java.util.Date;

import lsm.beans.Place;
import lsm.beans.Sensor;
import lsm.beans.User;
import lsm.server.LSMTripleStore;



public class TestLSM {

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

        try{
            /*
            * add new sensor to lsm store. For example: Air quality sensor from Lausanne
            * Sensor name: lausanne_1057
            */

            System.out.println("Testing LSM...");

            // 1. Create an instanse of Sensor class and set the sensor metadata
            Sensor sensor  = new Sensor();
            sensor.setName("lausanne_1057");
            sensor.setAuthor("sofiane");
            sensor.setSensorType("gsn");
            sensor.setSourceType("lausanne");
            sensor.setInfor("Air Quality Sensors from Lausanne");
            sensor.setSource("http://opensensedata.epfl.ch:22002/gsn?REQUEST=113&name=lausanne_1057");
            sensor.setTimes(new Date());

            // set sensor location information (latitude, longitude, city, country, continent...)
            Place place = new Place();
            place.setLat(46.529838);
            place.setLng(6.596818);
            sensor.setPlace(place);

            /*
            * Set sensor's author
            * If you don't have LSM account, please visit LSM Home page (http://lsm.deri.ie) to sign up
            */
            User user = new User();
            user.setUsername("sofiane");
            user.setPass("sofiane");
            sensor.setUser(user);

            // create LSMTripleStore instance
            LSMTripleStore lsmStore = new LSMTripleStore();

            //set user information for authentication
            lsmStore.setUser(user);

            //call sensorAdd method
            lsmStore.sensorAdd(sensor);

        }catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("cannot send the data to server");
        }
    }
}
