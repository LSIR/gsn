
package tinygsn.services;

import java.io.Serializable;

import com.google.android.gms.location.LocationRequest;

import tinygsn.beans.InputStream;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.beans.StreamSource;
import tinygsn.beans.VSensorConfig;
import tinygsn.controller.AndroidControllerListVSNew;
import tinygsn.model.vsensor.VirtualSensor;
import tinygsn.model.wrappers.AbstractWrapper;
import tinygsn.model.wrappers.AndroidAccelerometerWrapper;
import tinygsn.model.wrappers.AndroidGPSWrapper;
import tinygsn.storage.db.SqliteStorageManager;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;



public class GPSService extends IntentService implements LocationListener {

	
	public GPSService(Context context) {
		super("GPSService");
        this.mContext = context;
        getLocation();
    }
	
	public GPSService(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}
	
	public GPSService() {
		super("GPSService");
		// TODO Auto-generated constructor stub
	}

	private VSensorConfig config = null;

	private static final String TAG = "GPSService";
	public AndroidControllerListVSNew VSNewController;
	public AbstractWrapper w;
	
	
    private Context mContext = null;

    // flag for GPS status
    boolean isGPSEnabled = false;

    // flag for network status
    boolean isNetworkEnabled = false;

    // flag for GPS status
    boolean canGetLocation = false;

    Location location; // location
    double latitude; // latitude
    double longitude; // longitude

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES =   60 * 1; // 1 minute

    // Declaring a Location Manager
    protected LocationManager locationManager;

    public Location getLocation() {
        try {
        	Log.i("getLocation", "getLocation");
            locationManager = (LocationManager) mContext
                    .getSystemService(LOCATION_SERVICE);

            // getting GPS status
            isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting network status
            isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
            } else {
                this.canGetLocation = true;
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    Log.d("Network", "Network");
                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        }
                    }
                }
                // if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
                    if (location == null) {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                        Log.d("GPS Enabled", "GPS Enabled");
                        if (locationManager != null) {
                            location = locationManager
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        
        //TODO enja mishe parameter haro paas dad 
        return location;
    }
   
    /**
     * Stop using GPS listener
     * Calling this function will stop using GPS in your app
     * */
    public void stopUsingGPS(){
        if(locationManager != null){
            locationManager.removeUpdates(GPSService.this);
        }       
    }
   
    /**
     * Function to get latitude
     * */
    public double getLatitude(){
        if(location != null){
            latitude = location.getLatitude();
        }
       
        // return latitude
        return latitude;
    }
   
    /**
     * Function to get longitude
     * */
    public double getLongitude(){
        if(location != null){
            longitude = location.getLongitude();
        }
       
        // return longitude
        return longitude;
    }
   
    /**
     * Function to check GPS/wifi enabled
     * @return boolean
     * */
    public boolean canGetLocation() {
        return this.canGetLocation;
    }
   
	@Override
	public void onLocationChanged(Location location) {
	// TODO Auto-generated method stub
		Log.i("onLocationChanged","onLocationChangedddddddddddddddddddddddddddddddddd");
		StreamElement streamElement = new StreamElement(w.getFieldList(),
				w.getFieldType(), new Serializable[] {location.getLatitude(),location.getLongitude()});

		((AndroidGPSWrapper) w).setTheLastStreamElement(streamElement);
	}
	
	@Override
	public void onProviderDisabled(String provider) {
	// TODO Auto-generated method stub
	
		Log.i("onProviderDisabled", "onProviderDisabled");
	}
	
	@Override
	public void onProviderEnabled(String provider) {
	// TODO Auto-generated method stub
	
		Log.i("onProviderEnabled", "onProviderEnabled");
	}
	
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	Log.i("onStatusChanged", "onStatusChanged");
	
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle b = intent.getExtras();
		SqliteStorageManager storage = null;
		config = (VSensorConfig) b.get("tinygsn.beans.config");
		storage = new SqliteStorageManager(config.getController().getActivity());
		VirtualSensor vs = new VirtualSensor(config, config.getController().getActivity());
		for (InputStream inputStream : config.getInputStreams()) {
			for (StreamSource streamSource : inputStream.getSources()) {
				w = streamSource.getWrapper();
			}
		}
		
		mContext = config.getController().getActivity();
	    locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
			              MIN_TIME_BW_UPDATES,
			              MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
		
				
		while(true)
		{
			int samplingRate = storage.getSamplingRateByName("tinygsn.model.wrappers.AndroidGPSWrapper");
			if (samplingRate > 0){
				getLocation();
			}else{
				stopUsingGPS();
			}
			try {
				Thread.sleep(w.getSamplingRate()*10);
				if (samplingRate > 0){
					Location l = getLocation();
					if (l != null){
						onLocationChanged(l);
					}
					((AndroidGPSWrapper) w).getLastKnownLocation();
				}
			}
			catch (InterruptedException e) {
				Log.e(e.getMessage(), e.toString());
			}
		}
	}	  
}

