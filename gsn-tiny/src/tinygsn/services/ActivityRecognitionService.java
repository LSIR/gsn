
package tinygsn.services;

import java.io.Serializable;
import java.text.DateFormat;
//import java.text.SimpleDateFormat;
//
//import tinygsn.beans.InputStream;
//import tinygsn.beans.StreamElement;
//import tinygsn.beans.StreamSource;
//import tinygsn.beans.VSensorConfig;
//import tinygsn.controller.AndroidControllerListVSNew;
//import tinygsn.model.wrappers.AbstractWrapper;
//import tinygsn.model.wrappers.AndroidActivityRecognitionWrapper;
//import tinygsn.model.wrappers.AndroidGPSWrapper;
//
//import com.google.android.gms.common.ConnectionResult;
//import com.google.android.gms.common.GooglePlayServicesClient;
//import com.google.android.gms.common.GooglePlayServicesUtil;
//import com.google.android.gms.location.ActivityRecognitionClient;
//import com.google.android.gms.location.ActivityRecognitionResult;
//import com.google.android.gms.location.DetectedActivity;
//
//import android.app.PendingIntent;
//import android.app.Service;
//import android.content.Intent;
//import android.os.Bundle;
//import android.os.IBinder;
//import android.util.Log;
//
//public class ActivityRecognitionService extends Service implements GooglePlayServicesClient.ConnectionCallbacks,GooglePlayServicesClient.OnConnectionFailedListener {
//
//	private static VSensorConfig config = null;
//	private static final String TAG = "ActivityRecognitionService";
//	public AndroidControllerListVSNew VSNewController;
//	public AbstractWrapper w;
//	private PendingIntent pIntent;
//	
//	private ActivityRecognitionClient arclient;
//    // Formats the timestamp in the log
//    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss.SSSZ";
//
//    // Delimits the timestamp from the log info
//    private static final String LOG_DELIMITER = ";;";
//
//    // A date formatter
//    private SimpleDateFormat mDateFormat;
//	
//    Intent intent;
//	 public void onCreate() {
//	     super.onCreate();
//	     Log.i("oncreat","oncreate");
//	     intent = new Intent();      
//	 }
//	
//	 public int onStartCommand(Intent intent,int flags, int startId) {      
//		 Log.i("onstart", "Onstart");
//		 
//		 VSensorConfig tempConfig = null;
//		 Bundle b = intent.getExtras();
//		tempConfig = (VSensorConfig) b.get("config");
//		if(tempConfig != null)
//			config = tempConfig;
//			
//			 for (InputStream inputStream : config.getInputStreams()) {
//		 			for (StreamSource streamSource : inputStream.getSources()) {
//		 				w = streamSource.getWrapper();
//		 				break;
//		 			}
//		 			break;
//		         }
//				//config = config.clone();
//				
//		        // Get a handle to the repository
//		      //  mPrefs = getApplicationContext().getSharedPreferences(
//		       //         ActivityUtils.SHARED_PREFERENCES, Context.MODE_PRIVATE);
//
//		
//		        // Get a date formatter, and catch errors in the returned timestamp
//		        try {
//		            mDateFormat = (SimpleDateFormat) DateFormat.getDateTimeInstance();
//		        } catch (Exception e) {
//		           // Log.e(ActivityUtils.APPTAG, getString(R.string.date_format_error));
//		        }
//		
//		        // Format the timestamp according to the pattern, then localize the pattern
//		        mDateFormat.applyPattern(DATE_FORMAT_PATTERN);
//		        mDateFormat.applyLocalizedPattern(mDateFormat.toLocalizedPattern());
//		       
//		        	
//		        int resp = 0;
//		        while(resp != ConnectionResult.SUCCESS)  resp =GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
//		        if(resp == ConnectionResult.SUCCESS){
//		        	   arclient = new ActivityRecognitionClient(this, this, this);
//		        	   arclient.connect();  
//		        	    
//		        	    
//		        	 }
//		        	
//		        if (ActivityRecognitionResult.hasResult(intent)) {
//		   
//		    		
//		        	ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
//					Log.i("Activity Recived :DDDD", getType(result.getMostProbableActivity().getType()) +"\t" + result.getMostProbableActivity().getConfidence());
//		            DetectedActivity mostProbableActivity = result.getMostProbableActivity();
//		            Double confidence = (double) mostProbableActivity.getConfidence();
//		            Double activityType = (double) mostProbableActivity.getType();
//		              
//		    		StreamElement streamElement = new StreamElement(w.getFieldList(),
//		    				w.getFieldType(), new Serializable[] {activityType, confidence});
//
//		    		((AndroidActivityRecognitionWrapper) w).setTheLastStreamElement(streamElement);
//		            
//					((AndroidActivityRecognitionWrapper) w).getLastKnownData();
//		        
//		        }
//				return resp;
//			 
//	 }
//	@Override
//	public void onConnectionFailed(ConnectionResult arg0) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	public void onConnected(Bundle arg0) {
//		Log.i("Onconnect", "Onconnect");
//		Intent intent = new Intent(this, ActivityRecognitionService.class);
//		pIntent = PendingIntent.getService(this, 0, intent,PendingIntent.FLAG_UPDATE_CURRENT);
//		arclient.requestActivityUpdates(100, pIntent);   
//		
//	}
//
//	@Override
//	public void onDisconnected() {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	public IBinder onBind(Intent intent) {
//		// TODO Auto-generated method stub
//		return null;
//	}



import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;


import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;


import java.text.SimpleDateFormat;

import tinygsn.beans.InputStream;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.beans.StreamSource;
import tinygsn.beans.VSensorConfig;
import tinygsn.model.vsensor.VirtualSensor;
import tinygsn.model.wrappers.AbstractWrapper;
import tinygsn.model.wrappers.AndroidActivityRecognitionWrapper;
import tinygsn.storage.db.SqliteStorageManager;

/**
 * Service that receives ActivityRecognition updates. It receives updates
 * in the background, even if the main Activity is not visible.
 */
public class ActivityRecognitionService extends IntentService implements GooglePlayServicesClient.ConnectionCallbacks,GooglePlayServicesClient.OnConnectionFailedListener {
	
	AbstractWrapper w = null;
	
	private PendingIntent pIntent;
	
	private static VSensorConfig config = null;
	
	private ActivityRecognitionClient arclient;

	private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss.SSSZ";

    private SimpleDateFormat mDateFormat;
    
    SqliteStorageManager storage = null;
    int samplingRate = -1;
	

    public ActivityRecognitionService() {
        super("ActivityRecognitionIntentService");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId)  {
    
    	/*
    	if (intent == null){return START_REDELIVER_INTENT;}
		Bundle b = intent.getExtras();
		config = (VSensorConfig) b.get("tinygsn.beans.config");
		storage = new SqliteStorageManager(config.getController().getActivity());
		VirtualSensor vs = new VirtualSensor(config, config.getController().getActivity());
    	
    	
		samplingRate = storage.getSamplingRateByName("tinygsn.model.wrappers.AndroidActivityRecognitionWrapper");
		

		 for (InputStream inputStream : config.getInputStreams()) {
 			for (StreamSource streamSource : inputStream.getSources()) {
 				w = streamSource.getWrapper();
 				break;
 			}
 			break;
         }
	        try {
            mDateFormat = (SimpleDateFormat) DateFormat.getDateTimeInstance();
        } catch (Exception e) {

        }

        mDateFormat.applyPattern(DATE_FORMAT_PATTERN);
        mDateFormat.applyLocalizedPattern(mDateFormat.toLocalizedPattern());
       
        	
        int resp = 0;
        while(resp != ConnectionResult.SUCCESS)  resp =GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
       	if(resp == ConnectionResult.SUCCESS){
        	   arclient = new ActivityRecognitionClient(this, this, this);
        	   arclient.connect();  
        	    
        	    
        	 }
        
        if (ActivityRecognitionResult.hasResult(intent)) {
   
        	ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
		    DetectedActivity mostProbableActivity = result.getMostProbableActivity();
            Double confidence = (double) mostProbableActivity.getConfidence();
            Double activityType = (double) mostProbableActivity.getType();
              
            StreamElement streamElement = new StreamElement(w.getFieldList(),
    				w.getFieldType(), new Serializable[] {activityType, confidence});

    		((AndroidActivityRecognitionWrapper) w).setTheLastStreamElement(streamElement);
			((AndroidActivityRecognitionWrapper) w).getLastKnownData();
        }*/
		return START_NOT_STICKY; //START_REDELIVER_INTENT;
        
    }    
    private String getType(int type){
		if(type == DetectedActivity.UNKNOWN)
			return "Unknown";
		else if(type == DetectedActivity.IN_VEHICLE)
			return "In Vehicle";
		else if(type == DetectedActivity.ON_BICYCLE)
			return "On Bicycle";
		else if(type == DetectedActivity.ON_FOOT)
			return "On Foot";
		else if(type == DetectedActivity.STILL)
			return "Still";
		else if(type == DetectedActivity.TILTING)
			return "Tilting";
		else
			return "";
	}
    
	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnected(Bundle arg0) {
		Intent intent = new Intent(this, ActivityRecognitionService.class);
		pIntent = PendingIntent.getService(this, 0, intent,PendingIntent.FLAG_UPDATE_CURRENT);
		/*if(samplingRate > 0)	
			arclient.requestActivityUpdates(100, pIntent); 
		else 
			arclient.removeActivityUpdates(pIntent);*/
	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		// TODO Auto-generated method stub
		
	}
}

