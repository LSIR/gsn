package tinygsn.services;

import java.io.Serializable;

import com.google.android.gms.drive.internal.RemoveEventListenerRequest;

import tinygsn.beans.StreamElement;
import tinygsn.model.wrappers.AbstractWrapper;
import tinygsn.model.wrappers.AndroidAccelerometerWrapper;
import tinygsn.model.wrappers.AndroidGPSWrapper;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

public class MyLocationListener implements LocationListener{

	public AbstractWrapper w;
	public AbstractWrapper getW() {
		return w;
	}

	public void setW(AbstractWrapper w) {
		this.w = w;
	}

	@Override
	public void onLocationChanged(Location location) {
		
		
		StreamElement streamElement = new StreamElement(w.getFieldList(),
				w.getFieldType(), new Serializable[] {location.getLatitude(),location.getLongitude()});
		
		((AndroidGPSWrapper) w).setTheLastStreamElement(streamElement);
		((AndroidGPSWrapper) w).getLastKnownLocation();
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

}
