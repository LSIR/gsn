/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
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
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with GSN. If not, see <http://www.gnu.org/licenses/>.
*
* File: gsn-tiny/src/tinygsn/model/vsensor/VirtualSensor.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.model.vsensor;
import java.io.Serializable;
import java.util.ArrayList;

import tinygsn.beans.InputStream;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.beans.StreamSource;
import tinygsn.beans.VSensorConfig;
import tinygsn.services.AccelometerService;
import tinygsn.services.ActivityRecognitionService;
import tinygsn.services.GPSService;
import tinygsn.services.GyroscopeService;
import tinygsn.services.LightSensorService;
import tinygsn.services.LocationService;
import tinygsn.services.WifiService;
import android.content.Intent;
import android.content.Context;

public class VirtualSensor implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5273593293715944940L;

	public Context context;
	
	private AbstractVirtualSensor virtualSensor = null;

	private VSensorConfig config = null;

	public VSensorConfig getConfig() {
		return config;
	}
	
	public void setConfig(VSensorConfig config) {
		this.config = config;
		
	}

	private static final String TAG = "VirtualSensor";

	public VirtualSensor(VSensorConfig originalConfig, Context context) {
		this.config = originalConfig;
		this.context = context;
		
		for (InputStream is : config.getInputStreams()) {
			is.setPool(this);
		}

		try {
			virtualSensor = (AbstractVirtualSensor) Class.forName(
					config.getProcessingClassName()).newInstance();
			virtualSensor.setVirtualSensorConfiguration(originalConfig);
			
//			if (config.getProcessingClassName().equals(AbstractVirtualSensor.PROCESSING_CLASS_NOTIFICATION)){
//				
//			}
		}
		catch (InstantiationException e) {
			e.printStackTrace();
		}
		catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void start() {
		
		StackTraceElement test[] = Thread.currentThread().getStackTrace();
//		for(int i = 0; i < test.length;  i++)
//		{
//			Log.e("stack trace", test[i].getMethodName());
//			Log.e("stack trace class", test[i].getClassName());
//		}
		// Context sContext =  getApplicationContext();
		Intent serviceIntent = null;
		if(config.getWrapperName().equals("tinygsn.model.wrappers.AndroidGyroscopeWrapper"))
			serviceIntent = new Intent(this.context, GyroscopeService.class);
		else if(config.getWrapperName().equals("tinygsn.model.wrappers.AndroidAccelerometerWrapper"))
			serviceIntent = new Intent(this.context, AccelometerService.class);
		else if(config.getWrapperName().equals("tinygsn.model.wrappers.AndroidGPSWrapper"))
			serviceIntent = new Intent(this.context, GPSService.class);
		else if(config.getWrapperName().equals("tinygsn.model.wrappers.AndroidLightWrapper"))
			serviceIntent = new Intent(this.context, LightSensorService.class);
		else if(config.getWrapperName().equals("tinygsn.model.wrappers.AndroidActivityRecognitionWrapper"))
			serviceIntent = new Intent(this.context, ActivityRecognitionService.class);
		else if(config.getWrapperName().equals("tinygsn.model.wrappers.WifiWrapper"))
			serviceIntent = new Intent(this.context, WifiService.class);
		
		
		
		config.setRunning(true);
		
		serviceIntent.putExtra("tinygsn.beans.config", config);
		
		StaticData.addRunningService(config.getName(), serviceIntent);
		context.startService(serviceIntent);
	}

	public void stop() {
		
		config = StaticData.findConfig(config.getId());
		config.setRunning(false);
		
	
		
		for (InputStream inputStream : config.getInputStreams()) {
			for (StreamSource streamSource : inputStream.getSources()) {
				streamSource.getWrapper().releaseResources();
			}
		}
		
		Intent serviceIntent = StaticData.getRunningIntentByName(config.getName());
		if(serviceIntent != null)
		{
			serviceIntent.removeExtra("tinygsn.beans.config");
			serviceIntent.putExtra("tinygsn.beans.config", config);
			context.startService(serviceIntent);
		}
	}

	public void dataAvailable(StreamElement se) {
		virtualSensor.dataAvailable(null, se);
	}
	
	public void dataAvailable(ArrayList<StreamElement> se) {
		virtualSensor.dataAvailable(null, se);
	}

}
