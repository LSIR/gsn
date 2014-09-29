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
* File: src/gsn/msr/sensormap/ListAndDeleteRegisteredSensors.java
*
* @author Ali Salehi
*
*/

package gsn.msr.sensormap;

import gsn.Main;
import gsn.beans.ContainerConfig;
import gsn.msr.sensormap.sensorman.ServiceStub.ArrayOfSensorInfo;
import gsn.msr.sensormap.sensorman.ServiceStub.GetSensorsByPublisher;
import gsn.msr.sensormap.sensorman.ServiceStub.Guid;
import gsn.msr.sensormap.sensorman.ServiceStub.SensorInfo;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;

import org.apache.log4j.Logger;

public class ListAndDeleteRegisteredSensors {
	
    private static transient Logger logger = Logger.getLogger ( ListAndDeleteRegisteredSensors.class );
    
	public static void main(String args[]) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, KeyStoreException, CertificateException, SecurityException, SignatureException, IOException {
		ContainerConfig conf = Main.loadContainerConfiguration();
		String username = conf.getMsrMap().get("user");
		Guid code = LoginToMSRSense.login_to_sensor_map(username, conf.getMsrMap().get("password"));
		gsn.msr.sensormap.sensorman.ServiceStub stub = new gsn.msr.sensormap.sensorman.ServiceStub();
		GetSensorsByPublisher sensors_list = new gsn.msr.sensormap.sensorman.ServiceStub.GetSensorsByPublisher();
		sensors_list.setPublisherName(username);
		sensors_list.setOriginalPublisherName(username);
		ArrayOfSensorInfo getSensorsByPublisherResult = stub.GetSensorsByPublisher(sensors_list).getGetSensorsByPublisherResult();
		if (getSensorsByPublisherResult ==null) {
			logger.info("There is no sensor registered under username: "+username);
			return;//Nothing to show
		}
		
		SensorInfo[] list = getSensorsByPublisherResult.getSensorInfo();
		for(SensorInfo si : list) {
			System.out.println(si.getSensorName());
		}
		
	}
}
