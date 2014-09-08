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

import tinygsn.beans.InputStream;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.beans.StreamSource;
import tinygsn.beans.VSensorConfig;
import tinygsn.services.AccelometerService;
import tinygsn.services.ActivityRecognitionService;
import tinygsn.services.GyroscopeService;
import tinygsn.services.LightSensorService;
import tinygsn.services.LocationService;
import tinygsn.services.WifiService;
import android.content.Intent;
import android.util.Log;
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
		
		Log.i("start Called", "startCalled");
		StackTraceElement test[] = Thread.currentThread().getStackTrace();
//		for(int i = 0; i < test.length;  i++)
//		{
//			Log.e("stack trace", test[i].getMethodName());
//			Log.e("stack trace class", test[i].getClassName());
//		}
		Log.i("In VSensor put data", "startiiiiiiiiing");
		Log.i("wrapper type:::::  ",config.getWrapperName());
		// Context sContext =  getApplicationContext();
		Intent serviceIntent = null;
		if(config.getWrapperName().equals("tinygsn.model.wrappers.AndroidGyroscopeWrapper"))
			serviceIntent = new Intent(this.context, GyroscopeService.class);
		else if(config.getWrapperName().equals("tinygsn.model.wrappers.AndroidAccelerometerWrapper"))
			serviceIntent = new Intent(this.context, AccelometerService.class);
		else if(config.getWrapperName().equals("tinygsn.model.wrappers.AndroidGPSWrapper"))
			serviceIntent = new Intent(this.context, LocationService.class);
		else if(config.getWrapperName().equals("tinygsn.model.wrappers.AndroidLightWrapper"))
			serviceIntent = new Intent(this.context, LightSensorService.class);
		else if(config.getWrapperName().equals("tinygsn.model.wrappers.AndroidActivityRecognitionWrapper"))
			serviceIntent = new Intent(this.context, ActivityRecognitionService.class);
		else if(config.getWrapperName().equals("tinygsn.model.wrappers.WifiWrapper"))
			serviceIntent = new Intent(this.context, WifiService.class);
		
		
		
		config.setRunning(true);
		Log.v(TAG, "Starts VS: controller" + config.getController().toString());
		
		
		serviceIntent.putExtra("tinygsn.beans.config", config);
		
		StaticData.addRunningService(config.getName(), serviceIntent);
		context.startService(serviceIntent);
	}

	public void stop() {
		
		config = StaticData.findConfig(config.getId());
		config.setRunning(false);
		
	
		
		for (InputStream inputStream : config.getInputStreams()) {
			for (StreamSource streamSource : inputStream.getSources()) {
				Log.i("qqqqqqqqqqqqq", streamSource.getWrapper().toString());
				streamSource.getWrapper().releaseResources();
			}
		}
		
		Log.e("name", config.getName());
		Intent serviceIntent = StaticData.getRunningIntentByName(config.getName());
		Log.d("to compare", "cmp "+ serviceIntent);
		if(serviceIntent != null)
		{
			Log.i("serviceIntent", serviceIntent.getClass().getName());
			//context.startService(serviceIntent);
			serviceIntent.removeExtra("config");
			serviceIntent.putExtra("config", config);
			context.startService(serviceIntent);
			//StaticData.IntentStopped(config.getName());
			//TODO why do we need to clone the config here??
			//config = config.clone();
			Log.v(TAG, "VS: " + config.toString() + " stopped.");
		}
	}

	public void dataAvailable(StreamElement se) {
		//Log.i("in data Avale",se.toString());
		virtualSensor.dataAvailable(null, se);
//		showLog(a, "VirtualSensor: " + se.toString());
	}

//	void showLog(final Activity a, final String text) {
//		a.runOnUiThread(new Runnable() {
//			public void run() {
//				Toast.makeText(a, text, Toast.LENGTH_SHORT).show();
//			}
//		});
//	}
	
	//
	// public synchronized AbstractVirtualSensor borrowVS() throws
	// VirtualSensorInitializationFailedException {
	// if (virtualSensor == null) {
	// try {
	// virtualSensor = (AbstractVirtualSensor)
	// Class.forName(config.getProcessingClass()).newInstance();
	// virtualSensor.setVirtualSensorConfiguration(config);
	// } catch (Exception e) {
	// throw new VirtualSensorInitializationFailedException(e.getMessage(), e);
	// }
	// if (virtualSensor.initialize() == false) {
	// throw new VirtualSensorInitializationFailedException();
	// }
	// if (logger.isDebugEnabled())
	// logger.debug(new
	// StringBuilder().append("Created a new instance for VS ").append(config.getName()));
	// }
	// return virtualSensor;
	// }
	//
	// /**
	// * The method ignores the call if the input is null
	// *
	// * @param o
	// */
	// public synchronized void returnVS(AbstractVirtualSensor o) {
	// if (o == null) return;
	// if (++noOfCallsToReturnVS % GARBAGE_COLLECTOR_INTERVAL == 0)
	// DoUselessDataRemoval();
	// }
	//
	// public synchronized void closePool() {
	// if (virtualSensor != null) {
	// virtualSensor.dispose();
	// if (logger.isDebugEnabled())
	// logger.debug(new
	// StringBuilder().append("VS ").append(config.getName()).append(" is now released."));
	// } else if (logger.isDebugEnabled())
	// logger.debug(new
	// StringBuilder().append("VS ").append(config.getName()).append(" was already released."));
	// }

	//
	// /**
	// * @return the config
	// */
	// public VSensorConfig getConfig() {
	// return config;
	// }
	//
	// /**
	// * @return the lastModified
	// */
	// public long getLastModified() {
	// return lastModified;
	// }
	//
	// public void dispose() {
	// }
	//
	// // apply the storage size parameter to the virtual sensor table
	// public void DoUselessDataRemoval() {
	// if (config.getParsedStorageSize() == VSensorConfig.STORAGE_SIZE_NOT_SET)
	// return;
	// StringBuilder query;
	//
	// if (config.isStorageCountBased()) {
	// query =
	// Main.getStorage(config.getName()).getStatementRemoveUselessDataCountBased(config.getName(),
	// config.getParsedStorageSize());
	// }
	// else {
	// query =
	// Main.getStorage(config.getName()).getStatementRemoveUselessDataTimeBased(config.getName(),
	// config.getParsedStorageSize());
	// }
	//
	// int effected = 0;
	// try {
	// if (logger.isDebugEnabled())
	// logger.debug(new
	// StringBuilder().append("Enforcing the limit size on the VS table by : ").append(query).toString());
	// effected = Main.getStorage(config.getName()).executeUpdate(query);
	// } catch (SQLException e) {
	// logger.error("Error in executing: " + query);
	// logger.error(e.getMessage(), e);
	// }
	// if (logger.isDebugEnabled())
	// logger.debug(new
	// StringBuilder().append(effected).append(" old rows dropped from ").append(config.getName()).toString());
	// }

}
