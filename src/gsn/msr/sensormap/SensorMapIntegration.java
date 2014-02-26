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
* File: src/gsn/msr/sensormap/SensorMapIntegration.java
*
* @author Ali Salehi
*
*/

package gsn.msr.sensormap;

import gsn.Main;
import gsn.VSensorStateChangeListener;
import gsn.beans.VSensorConfig;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;

import org.apache.log4j.Logger;

public class SensorMapIntegration implements VSensorStateChangeListener{
	private static transient Logger                                logger                              = Logger.getLogger ( SensorMapIntegration.class );
	
	public boolean vsLoading(VSensorConfig config) {
		if (config.getPublishToSensorMap())
			register_to_microsoft_sensor_map(config);
		return true;
	}

	public boolean vsUnLoading(VSensorConfig config) {
		if (config.getPublishToSensorMap())
			unregister_from_microsoft_sensor_map(config);
		return true;
	}

	private void register_to_microsoft_sensor_map(VSensorConfig config) {
		logger.warn("Virtual Sensor: "+config.getName()+" wants to publish its data to SensorMap.");
		Double lat = config.getLatitude();
		Double lng = config.getLongitude();
		Double alt = config.getAltitude();
		if (lat==null || lng ==null || alt==null) {
			logger.warn("Err, Virtual Sensor cannot be registered to sensor map as it doesn't have latitude, longitude and/or altitude addressing attributes.");
			return;
		}
		HashMap<String,String> params = Main.getContainerConfig().getMsrMap();
		String userName = params.get("user");
		String password = params.get("password");
		String host = params.get("host");
		if (userName==null||password==null||host==null) {
			logger.warn("user and/or password and/or host parameters are missing from the gsn.xml file from the microsoft-research-sensormap path.");
			logger.warn("registeration to microsoft research sensormap failed.");
			logger.warn("Not that the host has the format of machineName:PortNumber , missing PortNumber implies port 80.");
			return;
		}
		try {
			LoginToMSRSense.register_sensor(userName, password, config, host);
			//TestPublicToMSR.register_to_sensor_map(userName, password, host, Main.getContainerConfig(), config);
		} catch (RemoteException e) {
			logger.error(e.getMessage(),e);
		} catch (IOException e) {
			logger.error(e.getMessage(),e);
		}
	}

	private void unregister_from_microsoft_sensor_map(VSensorConfig config) {
		logger.warn("Virtual Sensor: "+config.getName()+" wants to unregister from SensorMap.");

		HashMap<String,String> params = Main.getContainerConfig().getMsrMap();
		String userName = params.get("user");
		String password = params.get("password");

		if (userName==null||password==null) {
			logger.warn("user and/or password parameters are missing from the gsn.xml file from the microsoft-research-sensormap path.");
			logger.warn("unregistering from microsoft research sensormap failed.");
			return;
		}
		try {
			LoginToMSRSense.delete_sensor(userName, password, config);
		} catch (RemoteException e) {
			logger.error(e.getMessage(),e);
		} catch (IOException e) {
			logger.error(e.getMessage(),e);
		}
	}

	public void release() {
		
	}

}
