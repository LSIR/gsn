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
import tinygsn.beans.StreamElement;
import tinygsn.services.WrapperService;
import tinygsn.storage.db.SqliteStorageManager;
import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class AndroidGPSWrapper extends AbstractWrapper  implements LocationListener {

	private static final String[] FIELD_NAMES = new String[] { "latitude", "longitude" };
	private static final Byte[] FIELD_TYPES = new Byte[] { DataTypes.DOUBLE, DataTypes.DOUBLE };
	private static final String[] FIELD_DESCRIPTION = new String[] {"Latitude", "Longitude" };
	private static final String[] FIELD_TYPES_STRING = new String[] { "double", "double" };
	
	public static final Class<GPSService> SERVICE = GPSService.class;

	private LocationManager locationManager;
    private int timeToShutdown = -1;
    
    boolean isGPSEnabled = false;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0; 
    private static final long MIN_TIME_BW_UPDATES =  15000;
	
	public void runOnce() {
		Activity activity = getConfig().getController().getActivity();
		SqliteStorageManager storage = new SqliteStorageManager(activity);
		int samplingPeriod = storage.getSamplingRateByName("tinygsn.model.wrappers.AndroidAccelerometerWrapper");
		while(isActive())
		{
			if (samplingPeriod > 0){
				timeToShutdown = 40;
				startGPS();
				try {
					Thread.sleep(15*1000);
				}catch (InterruptedException e) {}
			}else{
				timeToShutdown--;
				if (timeToShutdown < 0){
					stopGPS();
					break;
				}else{
					startGPS();
					try {
						Thread.sleep(15*1000);
					}catch (InterruptedException e) {}
				}
			}
		}
	}
	
	public void startGPS() {
        try {
        	Activity activity = getConfig().getController().getActivity();
            locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

            if (!isGPSEnabled){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,MIN_TIME_BW_UPDATES,MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                 isGPSEnabled = true;   
            }
                 Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                 if (location != null){
                 StreamElement streamElement = new StreamElement(FIELD_NAMES,
         				FIELD_TYPES, new Serializable[] {location.getLatitude(),location.getLongitude()});
                 streamElement.setTimeStamp(location.getTime());
         		 postStreamElement(streamElement);
                 }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
   
    public void stopGPS(){
        if(locationManager != null){
            locationManager.removeUpdates(this);
        } 
        isGPSEnabled = false; 
    }

    public String getWrapperName() {
		return this.getClass().getSimpleName();
	}
	
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

	@Override
	public void onLocationChanged(Location location) {
		StreamElement streamElement = new StreamElement(FIELD_NAMES, FIELD_TYPES,
				new Serializable[] {location.getLatitude(),location.getLongitude()});
		
		postStreamElement(streamElement);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {}

	@Override
	public void onProviderEnabled(String provider) {}

	@Override
	public void onProviderDisabled(String provider) {}
	
	public static class GPSService extends WrapperService{

		public GPSService() {
			super("gpsService");

		}
	}
	
}