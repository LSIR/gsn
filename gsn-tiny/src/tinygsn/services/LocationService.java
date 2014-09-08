package tinygsn.services;


import tinygsn.beans.InputStream;
import tinygsn.beans.StreamSource;
import tinygsn.beans.VSensorConfig;
import tinygsn.controller.AndroidControllerListVSNew;
import tinygsn.model.wrappers.AbstractWrapper;
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

	 Intent intent;
	 int counter = 0;

	 @Override
	 public void onCreate() {
	     super.onCreate();
	     Log.i("oncreat","oncreate");
	     intent = new Intent(BROADCAST_ACTION);      
	 }

	 @Override
	 public int onStartCommand(Intent intent,int flags, int startId) {      
		 Log.i("onstart", "Onstart");
		 
		 Bundle b = intent.getExtras();
			config = (VSensorConfig) b.get("tinygsn.beans.config");
			
			
			Log.i("Service", config.getInputStreams().toString());
			
		 
			
		
			
	     locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	     listener = new MyLocationListener();        
	    while(true)
	    {
	    	
	     locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 4000, 0, listener);
	     locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 4000, 0, listener);
	     
	     for (InputStream inputStream : config.getInputStreams()) {
				for (StreamSource streamSource : inputStream.getSources()) {
					w = streamSource.getWrapper();
					listener.setW(w);
					Log.v(TAG, w.toString());
					
				}
			}
	     
	     try {
				Thread.sleep(1000*60);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	    
		//return startId;
	 }

	 @Override
	 public IBinder onBind(Intent intent) {
		 Log.i("onBind","onBind");
	     return null;
	 }

	 protected boolean isBetterLocation(Location location, Location currentBestLocation) {
	     if (currentBestLocation == null) {
	         // A new location is always better than no location
	         return true;
	     }

	     // Check whether the new location fix is newer or older
	     long timeDelta = location.getTime() - currentBestLocation.getTime();
	     boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
	     boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
	     boolean isNewer = timeDelta > 0;

	     // If it's been more than two minutes since the current location, use the new location
	     // because the user has likely moved
	     if (isSignificantlyNewer) {
	         return true;
	     // If the new location is more than two minutes older, it must be worse
	     } else if (isSignificantlyOlder) {
	         return false;
	     }

	     // Check whether the new location fix is more or less accurate
	     int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
	     boolean isLessAccurate = accuracyDelta > 0;
	     boolean isMoreAccurate = accuracyDelta < 0;
	     boolean isSignificantlyLessAccurate = accuracyDelta > 200;

	     // Check if the old and new location are from the same provider
	     boolean isFromSameProvider = isSameProvider(location.getProvider(),
	             currentBestLocation.getProvider());

	     // Determine location quality using a combination of timeliness and accuracy
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
	     Log.v("STOP_SERVICE", "DONE");
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