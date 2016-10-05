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
 * File: gsn-tiny/src/tinygsn/model/wrappers/AndroidGPSWrapper.java
 *
 * @author Do Ngoc Hoan
 */


package tinygsn.model.wrappers;

import java.io.Serializable;
import java.util.ArrayList;

import tinygsn.beans.DataField;
import tinygsn.beans.DataTypes;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.beans.WrapperConfig;
import tinygsn.model.utils.Parameter;
import tinygsn.model.utils.ParameterType;
import tinygsn.services.WrapperService;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import org.epfl.locationprivacy.util.Utils;

import static android.os.Debug.startMethodTracing;

public class AndroidGPSWrapper extends AbstractWrapper implements LocationListener {

	public AndroidGPSWrapper(WrapperConfig wc) {
		super(wc);
	}

	public AndroidGPSWrapper() {
	}

	// A GPS position is represented as a rectangle (top left and bottom right points)
	private static final String[] FIELD_NAMES = new String[]{"latitudeTopLeft", "longitudeTopLeft", "latitudeBottomRight", "longitudeBottomRight"};
	private static final Byte[] FIELD_TYPES = new Byte[]{DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.DOUBLE};
	private static final String[] FIELD_DESCRIPTION = new String[]{"TopLeftLatitude", "TopLeftLongitude", "BottomRightLatitude", "BottomRightLongitude"};
	private static final String[] FIELD_TYPES_STRING = new String[]{"double", "double", "double", "double"};

	public final Class<? extends WrapperService> getSERVICE() {
		return GPSService.class;
	}

	private LocationManager locationManager;
	private int timeToShutdown = -1;

	boolean isGPSEnabled = false;
	private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0;
	private static final long MIN_TIME_BW_UPDATES = 5000;

	@Override
	public void runOnce() {
		updateWrapperInfo();
		while (getConfig().isRunning()) {
			if (dcDuration > 0) {
				timeToShutdown = 40;
				startGPS();
				try {
					Thread.sleep(dcInterval * 1000);
				} catch (InterruptedException e) {
				}
			} else {
				timeToShutdown--;
				if (timeToShutdown < 0) {
					break;
				} else {
					startGPS();
					try {
						Thread.sleep(dcInterval * 1000);
					} catch (InterruptedException e) {
					}
				}
			}
			try {
				getConfig().setRunning(StaticData.getWrapperByName(getWrapperName()).getConfig().isRunning());
			} catch (Exception e) {
			}
		}
		stopGPS();
	}

	public void startGPS() throws SecurityException{
		try {
			//if ((boolean) Utils.getBuildConfigValue(StaticData.globalContext, "GPSPERFORMANCE")) {
			//	startMethodTracing("Android/data/tinygsn.gui.android/whole_gps_process" + System.currentTimeMillis());
			//}
			locationManager = (LocationManager) StaticData.globalContext.getSystemService(StaticData.globalContext.LOCATION_SERVICE);

			boolean isGPSUsageEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

			// getting network status
			boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

			if (!isGPSEnabled) {
				// getting GPS status

				isGPSEnabled = true;

				if (isNetworkEnabled) {
					locationManager.requestLocationUpdates(
						LocationManager.NETWORK_PROVIDER,
						MIN_TIME_BW_UPDATES,
						MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
				}
				// if GPS Enabled get lat/long using GPS Services
				if (isGPSUsageEnabled) {
					locationManager.requestLocationUpdates(
						locationManager.GPS_PROVIDER,
						MIN_TIME_BW_UPDATES,
						MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
				}
			}

			Location location = null;
			StreamElement streamElement = null;
			if (isGPSEnabled && isNetworkEnabled) {
				Location tempLocation = locationManager.getLastKnownLocation(locationManager.NETWORK_PROVIDER);
				if (tempLocation != null) {
					location = tempLocation;
				}
			}
			if (isGPSEnabled && isGPSUsageEnabled) {
				Location tempLocation = locationManager.getLastKnownLocation(locationManager.GPS_PROVIDER);
				if (tempLocation != null && tempLocation.getTime() >= location.getTime()) {
					location = tempLocation;
				}
			}
			if (location != null) {
				streamElement = new StreamElement(FIELD_NAMES,
					FIELD_TYPES,
					new Serializable[]{location.getLatitude(), location.getLongitude(),
						location.getLatitude(), location.getLongitude()});
				streamElement.setTimeStamp(location.getTime());
				postStreamElement(streamElement);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void stopGPS() throws SecurityException{
		if (locationManager != null) {
			locationManager.removeUpdates(this);
		}
		isGPSEnabled = false;
	}

	@Override
	public DataField[] getOutputStructure() {
		ArrayList<DataField> output = new ArrayList<DataField>();
		for (int i = 0; i < FIELD_NAMES.length; i++)
			output.add(new DataField(FIELD_NAMES[i], FIELD_TYPES_STRING[i],
				FIELD_DESCRIPTION[i]));

		return output.toArray(new DataField[]{});
	}

	@Override
	public String[] getFieldList() {
		return FIELD_NAMES;
	}

	@Override
	public Byte[] getFieldType() {
		return FIELD_TYPES;
	}

	@Override
	public void onLocationChanged(Location location) {
		StreamElement streamElement = new StreamElement(FIELD_NAMES, FIELD_TYPES,
			new Serializable[]{location.getLatitude(), location.getLongitude(),
				location.getLatitude(), location.getLongitude()});

		postStreamElement(streamElement);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	public static class GPSService extends WrapperService {

		public GPSService() {
			super("gpsService");

		}
	}

}