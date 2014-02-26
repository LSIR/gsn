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
* File: src/gsn/ContainerImpl.java
*
* @author Jerome Rousselot
* @author gsn_devs
* @author Ali Salehi
* @author Timotee Maret
*
*/

package gsn;

import gsn.beans.StreamElement;
import gsn.storage.StorageManager;
import gsn.vsensor.AbstractVirtualSensor;

import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class ContainerImpl  {

	private static transient Logger                                      logger                             = Logger.getLogger( ContainerImpl.class );

	/**
	 * The <code> waitingVirtualSensors</code> contains the virtual sensors that
	 * recently produced data. This variable is useful for batch processing timed
	 * couple virtual sensor produce data.
	 */
	/*
	 * In the <code>registeredQueries</code> the key is the local virtual
	 * sensor name.
	 */

	
	private static ContainerImpl singleton;
	
	private static final Object                                          psLock                             = new Object( );

	private ContainerImpl() {

	}

	public static ContainerImpl getInstance() {
		if (singleton == null)
			singleton = new ContainerImpl();
		return singleton;
	}


	public void publishData ( AbstractVirtualSensor sensor ,StreamElement data) throws SQLException {
		String name = sensor.getVirtualSensorConfiguration( ).getName( ).toLowerCase();
		StorageManager storageMan = Main.getStorage(sensor.getVirtualSensorConfiguration().getName());
		synchronized ( psLock ) {
			storageMan.executeInsert( name ,sensor.getVirtualSensorConfiguration().getOutputStructure(), data );
		}
		
		for (VirtualSensorDataListener listener : dataListeners) {
			listener.consume(data, sensor.getVirtualSensorConfiguration());
		}
	}

	private ArrayList<VirtualSensorDataListener> dataListeners = new ArrayList<VirtualSensorDataListener>();

	public synchronized void addVSensorDataListener(VirtualSensorDataListener listener) {
		if (!dataListeners.contains(listener))
			dataListeners.add(listener);
	}

	public synchronized void removeVSensorDataListener(VirtualSensorDataListener listener) {
		dataListeners.remove(listener);
	}

}
