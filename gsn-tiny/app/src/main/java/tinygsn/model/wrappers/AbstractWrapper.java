/**
 * Global Sensor Networks (GSN) Source Code
 * Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
 * <p/>
 * This file is part of GSN.
 * <p/>
 * GSN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * GSN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with GSN. If not, see <http://www.gnu.org/licenses/>.
 * <p/>
 * File: gsn-tiny/src/tinygsn/model/wrappers/AbstractWrapper.java
 *
 * @author Do Ngoc Hoan
 */
package tinygsn.model.wrappers;

import android.content.Context;
import android.content.Intent;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import tinygsn.beans.DataField;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.beans.StreamSource;
import tinygsn.beans.WrapperConfig;
import tinygsn.services.WrapperService;
import tinygsn.storage.db.SqliteStorageManager;

public abstract class AbstractWrapper {

	public static final int DEFAULT_DUTY_CYCLE_DURATION = 2;
	public static final int DEFAULT_DUTY_CYCLE_INTERVAL = 15;
	protected final List<StreamSource> listeners = Collections
			                                               .synchronizedList(new ArrayList<StreamSource>());
	protected int dcDuration = DEFAULT_DUTY_CYCLE_DURATION;
	protected int dcInterval = DEFAULT_DUTY_CYCLE_INTERVAL;
	private WrapperConfig config = null;

	public AbstractWrapper(WrapperConfig wc) {
		config = wc;
	}

	public AbstractWrapper() {

	}

	public static Properties getWrapperList(Context context) {
		Properties wrapperList = new Properties();
		Properties wrapperListTemp = new Properties();
		try {
			InputStream is = context.getAssets().open("wrapper_list.properties");
			wrapperList.load(is);

			SensorManager deviceSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
			List<android.hardware.Sensor> sensorList = deviceSensorManager.getSensorList(Sensor.TYPE_ALL);

			Iterator it = sensorList.iterator();
			while (it.hasNext()) {
				Sensor sensorObject = (Sensor) it.next();
				String property = wrapperList.getProperty(sensorObject.getStringType());
				if (property != null) {
					wrapperListTemp.put(sensorObject.getStringType(), property);
				}
			}

			PackageManager pm = context.getPackageManager();
			FeatureInfo[] featureInfos = pm.getSystemAvailableFeatures();
			for (FeatureInfo featureInfo : featureInfos) {
				if (featureInfo.name != null) {
					String property = wrapperList.getProperty(featureInfo.name);
					if (property != null) {
						wrapperListTemp.put(featureInfo.name, property);
					}
				}
			}
			if (isPlayServiceAvailable()) {
				wrapperListTemp.put("activityrecognition", wrapperList.getProperty("activityrecognition"));
			}
			wrapperList = wrapperListTemp;
		} catch (IOException e) {
		}

		return wrapperList;
	}

	//Check for Google play services available on device
	private static boolean isPlayServiceAvailable() {
		return GooglePlayServicesUtil.isGooglePlayServicesAvailable(StaticData.globalContext) == ConnectionResult.SUCCESS;
	}

	public WrapperConfig getConfig() {
		return config;
	}

	public void setConfig(WrapperConfig config) {
		this.config = config;
	}

	public abstract Class<? extends WrapperService> getSERVICE();

	public int getDcDuration() {
		return dcDuration;
	}

	public int getDcInterval() {
		return dcInterval;
	}

	public void registerListener(StreamSource s) {
		synchronized (listeners) {
			Log.i(getWrapperName(), "registered");
			listeners.add(s);
		}
	}

	public void unregisterListener(StreamSource s) {
		synchronized (listeners) {
			Log.i(getWrapperName(), "unregistered");
			listeners.remove(s);
		}
	}

	/**
	 * The output structure should be specified in the XML config file. However,
	 * for the simplicity of this tinygsn version, we return it from wrapper.
	 *
	 * @return
	 */
	public abstract DataField[] getOutputStructure();

	/**
	 * This method gets the generated stream element and notifies the input
	 * streams if needed. The return value specifies if the newly provided stream
	 * element generated at least one input stream notification or not.
	 *
	 * @param streamElement
	 * @return If the method returns false, it means the insertion doesn't
	 * effected any input stream.
	 */

	public Boolean postStreamElement(StreamElement streamElement) {
		synchronized (listeners) {
			for (StreamSource s : listeners) {
				if (s.getInputStream().getVirtualSensor().getConfig().getRunning()) {
					s.add(streamElement);
				}
			}
		}
		return true;
	}

	public void releaseResources() {
		config.setRunning(false);
	}

	public abstract String[] getFieldList();

	public abstract Byte[] getFieldType();

	public abstract void runOnce();

	public String getWrapperName() {
		return this.getClass().getName();
	}

	public void updateWrapperInfo() {
		SqliteStorageManager storage = new SqliteStorageManager();
		int[] info = storage.getWrapperInfo(getWrapperName());
		if (info != null) {
			dcInterval = info[0];
			dcDuration = info[1];
		}
	}

	synchronized public boolean start() {
		try {
			Intent serviceIntent = StaticData.getRunningIntentByName(getWrapperName());
			if (serviceIntent == null) {
				serviceIntent = new Intent(StaticData.globalContext, getSERVICE());
				config.setRunning(true);
				Bundle bundle = new Bundle();
				bundle.putParcelable("tinygsn.beans.config", config);
				serviceIntent.putExtra("tinygsn.beans.config", bundle);
				StaticData.addRunningService(getWrapperName(), serviceIntent);
				StaticData.globalContext.startService(serviceIntent);
				return true;
			}
		} catch (Exception e) {
			// release anything?
		}
		return false;
	}

	synchronized public boolean stop() {
		try {
			Intent serviceIntent = StaticData.getRunningIntentByName(getWrapperName());
			if (serviceIntent != null) {
				serviceIntent.removeExtra("tinygsn.beans.config");
				config.setRunning(false);
				Bundle bundle = new Bundle();
				bundle.putParcelable("tinygsn.beans.config", config);
				serviceIntent.putExtra("tinygsn.beans.config", bundle);
				StaticData.globalContext.startService(serviceIntent);
				StaticData.IntentStopped(getWrapperName());
				return true;
			}
		} catch (Exception e) {
			// release anything?
		}
		return false;
	}

	public void initialize_wrapper() {
		SqliteStorageManager storage = new SqliteStorageManager();
		HashMap<String, String> param = storage.getSetting("wrapper:" + config.getWrapperName() + ":");
		for (Entry<String, String> e : param.entrySet()) {
			initParameter(e.getKey(), e.getValue());
		}
		initialize();
	}

	protected void initParameter(String key, String value) {
	}

	public String[] getParameters() {
		return new String[]{};
	}

	public void initialize() {
	}


}
