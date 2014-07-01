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
* File: gsn-tiny/src/tinygsn/model/wrappers/AndroidGPSWrapper.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.model.wrappers;

import java.io.Serializable;
import java.util.ArrayList;
import tinygsn.beans.DataField;
import tinygsn.beans.DataTypes;
import tinygsn.beans.Queue;
import tinygsn.beans.StreamElement;
import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

public class AndroidGPSWrapper extends AbstractWrapper {

	private static final String[] FIELD_NAMES = new String[] { "latitude",
			"longitude" };

	private static final Byte[] FIELD_TYPES = new Byte[] { DataTypes.DOUBLE,
			DataTypes.DOUBLE };

	private static final String[] FIELD_DESCRIPTION = new String[] {
			"Latitude Reading", "Longitude Reading" };

	private static final String[] FIELD_TYPES_STRING = new String[] { "double",
			"double" };

	private static final String TAG = "AndroidGPSWrapper";

	private static int threadCounter = 0;

	private LocationManager myLocationManager;
	private LocationListener myLocationListener;
	private StreamElement theLastStreamElement = null;
	
	public AndroidGPSWrapper() {
		super();
	}
	
	public AndroidGPSWrapper(Queue queue) {
		super(queue);
		initialize();
	}

	public boolean initialize() {
		return true;
	}

	public void run() {
		Log.v(TAG, TAG + " is waiting for data");

		Activity activity = getConfig().getController().getActivity();
		Looper.prepare();
		
		// Get Location service
		myLocationManager = (LocationManager) activity
				.getSystemService(Context.LOCATION_SERVICE);
		// Register a LocationListener
		myLocationListener = new MyLocationListener();
		// Update GPS data every 0 ms and 0 meter
		myLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
				0, myLocationListener);

		while (isActive()) {
			try {
				Thread.sleep(samplingRate);
				getLastKnownLocation();
			}
			catch (InterruptedException e) {
				Log.e(e.getMessage(), e.toString());
			}
		}
		
		Looper.loop();
	}

//	protected void getLastKnownLocation() {
//		try {
//			// Get the current location
//			Location location = myLocationManager
//					.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//			if (location == null) {
//				Log.e(TAG, "There is no signal!");
//			}
//			else {
//				double latitude = location.getLatitude();
//				double longitude = location.getLongitude();
//
//				StreamElement streamElement = new StreamElement(FIELD_NAMES,
//						FIELD_TYPES, new Serializable[] { latitude, longitude });
//				
//				if (isTheSameWithTheLastStreamElement(streamElement)){
//					return;
//				}
//				postStreamElement(streamElement);
//			}
//		}
//		catch (Exception e) {
//			Log.e(TAG, "There is an error! \n" + e.getMessage().toString());
//		}
//	}

	private void getLastKnownLocation() {
		if (theLastStreamElement == null){
			Log.e(TAG, "There is no signal!");
		}
		else{
			postStreamElement(theLastStreamElement);
		}
	}

	public void dispose() {
		threadCounter--;
	}

	public String getWrapperName() {
		return "AndroidGPSWrapper";
	}

	private class MyLocationListener implements LocationListener {

		public void onLocationChanged(Location location) {
			double latitude = location.getLatitude();
			double longitude = location.getLongitude();

			StreamElement streamElement = new StreamElement(FIELD_NAMES,
					FIELD_TYPES, new Serializable[] { latitude, longitude });
			
			theLastStreamElement = streamElement;
		}

		public void onProviderDisabled(String provider) {
		}

		public void onProviderEnabled(String provider) {
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	}

//	private boolean isTheSameWithTheLastStreamElement(StreamElement se){
//		if (theLastStreamElement == null){
//			theLastStreamElement = se;
//			return false;
//		}
//		if (theLastStreamElement.isTheSame(se)){
//			theLastStreamElement = se;
//			return true;
//		}
//		theLastStreamElement = se;
//		return false;
//	}
	
	@Override
	public DataField[] getOutputStructure() {
		ArrayList<DataField> output = new ArrayList<DataField>();
		for (int i = 0; i < FIELD_NAMES.length; i++)
			output.add(new DataField(FIELD_NAMES[i], FIELD_TYPES_STRING[i],
					FIELD_DESCRIPTION[i]));

		return output.toArray(new DataField[] {});
	}

	@Override
	public String[] getFieldList() {
		return FIELD_NAMES;
	}

	@Override
	public Byte[] getFieldType() {
		return FIELD_TYPES;
	}

}