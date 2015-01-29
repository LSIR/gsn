package tinygsn.services;


import tinygsn.beans.InputStream;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamSource;
import tinygsn.beans.VSensorConfig;
import tinygsn.controller.AndroidControllerListVSNew;
import tinygsn.model.vsensor.VirtualSensor;
import tinygsn.model.wrappers.AbstractWrapper;
import tinygsn.storage.db.SqliteStorageManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

/**
 * BackgroundLocationService used for tracking user location in the background.
 *
 * @author cblack
 */public class LocationService extends Service {
	
	 private VSensorConfig config = null;
		private static final String TAG = "GPSService";
		public AndroidControllerListVSNew VSNewController;
		public AbstractWrapper w;
	 
	 public static final String BROADCAST_ACTION = "Hello World";
	 private static final int TWO_MINUTES = 1000 * 60 * 2;
	 public LocationManager locationManager;
	 public MyLocationListener listener;
	 public Location previousBestLocation = null;
	 SqliteStorageManager storage = null;
	 
	 Intent intent;
	 int counter = 0;

	 @Override
	 public void onCreate() {
	     super.onCreate();
	     intent = new Intent(BROADCAST_ACTION);      
	 }

	 @Override
	 public int onStartCommand(Intent intent,int flags, int startId) {      
		
		 
		 Bundle b = intent.getExtras();
			config = (VSensorConfig) b.get("tinygsn.beans.config");
			storage = new SqliteStorageManager(config.getController().getActivity());
			VirtualSensor vs = new VirtualSensor(config, config.getController().getActivity());
			int samplingRate = storage.getSamplingRateByName("tinygsn.model.wrappers.AndroidGPSWrapper");
			
	     locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	     listener = new MyLocationListener();        

	      if(samplingRate == 0)
	    {
	    	locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 12000, 10, listener);
	    	locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 12000, 10, listener);
	    }
	    else if(samplingRate == 1)
	    {
	    	locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 6000, 2, listener);
	    	locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 6000, 2, listener);
	    }
	    else if(samplingRate == 2)
	    {
	    	locationManager.removeUpdates(listener);
	    	return 0;
	    }
	    	
	     
	     
	     for (InputStream inputStream : config.getInputStreams()) {
				for (StreamSource streamSource : inputStream.getSources()) {
					w = streamSource.getWrapper();
					listener.setW(w);
					
				}
			}
	     	    
		return  START_REDELIVER_INTENT;
	 }

	 @Override
	 public IBinder onBind(Intent intent) {
	     return null;
	 }

	 protected boolean isBetterLocation(Location location, Location currentBestLocation) {
	     if (currentBestLocation == null) {
	         return true;
	     }

	     long timeDelta = location.getTime() - currentBestLocation.getTime();
	     boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
	     boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
	     boolean isNewer = timeDelta > 0;

	     if (isSignificantlyNewer) {
	         return true;
	     } else if (isSignificantlyOlder) {
	         return false;
	     }

	     int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
	     boolean isLessAccurate = accuracyDelta > 0;
	     boolean isMoreAccurate = accuracyDelta < 0;
	     boolean isSignificantlyLessAccurate = accuracyDelta > 200;

	     boolean isFromSameProvider = isSameProvider(location.getProvider(),
	             currentBestLocation.getProvider());

	     if (isMoreAccurate) {
	         return true;
	     } else if (isNewer && !isLessAccurate) {
	         return true;
	     } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
	         return true;
	     }
	     return false;
	 }

	 /** Checks whether two providers are the same */
	 private boolean isSameProvider(String provider1, String provider2) {
	     if (provider1 == null) {
	       return provider2 == null;
	     }
	     return provider1.equals(provider2);
	 }

	 @Override
	 public void onDestroy() {       
	    // handler.removeCallbacks(sendUpdatesToUI);     
	     super.onDestroy();
	     if (locationManager != null)
	    	 locationManager.removeUpdates(listener);        
	 }   

	 public static Thread performOnBackgroundThread(final Runnable runnable) {
	     final Thread t = new Thread() {
	         @Override
	         public void run() {
	             try {
	                 runnable.run();
	             } finally {

	             }
	         }
	     };
	     t.start();
	     return t;
	 }
 }