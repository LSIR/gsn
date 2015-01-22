
package tinygsn.services;

import java.io.Serializable;
import tinygsn.beans.InputStream;
import tinygsn.beans.StreamElement;
import tinygsn.beans.StreamSource;
import tinygsn.beans.VSensorConfig;
import tinygsn.controller.AndroidControllerListVSNew;
import tinygsn.model.vsensor.VirtualSensor;
import tinygsn.model.wrappers.AbstractWrapper;
import tinygsn.model.wrappers.AndroidGPSWrapper;
import tinygsn.storage.db.SqliteStorageManager;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;


public class GPSService extends IntentService implements LocationListener {

	public GPSService(String name) {
		super(name);
	}
	
	public GPSService() {
		super("GPSService");
	}

	private VSensorConfig config = null;
	public AndroidControllerListVSNew VSNewController;
	public AbstractWrapper w;
	
    private Context mContext = null;
    
    private int timeToShutdown = -1;
    
    boolean isGPSEnabled = false;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0; 
    private static final long MIN_TIME_BW_UPDATES =  15000;
    protected LocationManager locationManager;

    public void startGPS() {
        try {
            locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);
        //    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER )) {
            //	if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER )) {
            	//	locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0, this);
            	//}
            if (!isGPSEnabled){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,MIN_TIME_BW_UPDATES,MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                 isGPSEnabled = true;   
            }
           // }
                 Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                 if (location != null){
                 StreamElement streamElement = new StreamElement(w.getFieldList(),
         				w.getFieldType(), new Serializable[] {location.getLatitude(),location.getLongitude()});
                 streamElement.setTimeStamp(location.getTime());
         		((AndroidGPSWrapper) w).postStreamElement(streamElement);
                 }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
   
    public void stopGPS(){
        if(locationManager != null){
            locationManager.removeUpdates(GPSService.this);
        } 
        isGPSEnabled = false; 
    }
      
	@Override
	public void onLocationChanged(Location location) {
		Log.d("tinygsn-gps", "got location");
		Toast.makeText(config.getController().getActivity(), location.getLatitude()+","+location.getLongitude(),Toast.LENGTH_SHORT).show();
		StreamElement streamElement = new StreamElement(w.getFieldList(),
				w.getFieldType(), new Serializable[] {location.getLatitude(),location.getLongitude()});
		((AndroidGPSWrapper) w).postStreamElement(streamElement);
	}
		
	@Override
	public void onProviderDisabled(String provider) {}
	
	@Override
	public void onProviderEnabled(String provider) {}
	
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.d("tinygsn-gps", "status : "+status+" ("+extras.describeContents()+")");
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
		
		while(w.isActive())
		{
			int samplingRate = storage.getSamplingRateByName("tinygsn.model.wrappers.AndroidGPSWrapper");
			if (samplingRate > 0){
				timeToShutdown = 8;
				startGPS();
				try {
					Thread.sleep(15*1000);
				}catch (InterruptedException e) {}
			}else{
				timeToShutdown--;
				if (timeToShutdown < 0){
					stopGPS();
					break;
				}
			}
		}
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+15000,PendingIntent.getService(this, 0, intent,PendingIntent.FLAG_UPDATE_CURRENT));
	}	  
}

