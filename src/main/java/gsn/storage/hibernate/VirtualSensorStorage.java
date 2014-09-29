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
* File: src/gsn/storage/hibernate/VirtualSensorStorage.java
*
* @author Timotee Maret
*
*/

package gsn.storage.hibernate;

import gsn.beans.StreamElement;
import gsn.utils.GSNRuntimeException;

import java.io.Serializable;

public interface VirtualSensorStorage {


    public boolean init();

    /**
     * @param se The {@link gsn.beans.StreamElement} to be stored.
     * @return The the generated identifier for the primary key.
     * @throws GSNRuntimeException if the {gsn.beans.StreamElement} could not be stored (for instance due to a constraint violation).
     */
    public Serializable saveStreamElement(StreamElement se) throws GSNRuntimeException ;

    /**
     * @param pk the primary key.
     * @return the StreamElement associated to the pk primary key or null if it does not exists.
     * @throws GSNRuntimeException
     */
    public StreamElement getStreamElement(Serializable pk) throws GSNRuntimeException ;

    /**
     * @return The number of {@link gsn.beans.StreamElement} in the storage.
     * @throws GSNRuntimeException
     */
    public long countStreamElement() throws GSNRuntimeException ;

    //public void getStreamElements() throws GSNRuntimeException ;


    //TODO native SQL query.

}
