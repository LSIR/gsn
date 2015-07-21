package tinygsn.model.wrappers;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import tinygsn.beans.DataField;
import tinygsn.beans.DataTypes;
import tinygsn.beans.StreamSource;
import tinygsn.beans.StreamElement;
import tinygsn.beans.VSensorConfig;
import tinygsn.services.WrapperService;
import tinygsn.storage.db.SqliteStorageManager;

public class AndroidActivityRecognitionWrapper extends AbstractWrapper {

	private static final String[] FIELD_NAMES = new String[] { "type", "confidence" };

	private static final Byte[] FIELD_TYPES = new Byte[] { DataTypes.DOUBLE,
			DataTypes.DOUBLE };

	private static final String[] FIELD_DESCRIPTION = new String[] { "type", "confidenct" };

	private static final String[] FIELD_TYPES_STRING = new String[] { "double", "double" };

	private static final String TAG = "AndroidActivityRecognitionWrapper";

	private StreamElement theLastStreamElement = null;
	
	public AndroidActivityRecognitionWrapper()
	{
		super();
	}
	

	
	public void getLastKnownData() {
		if (getTheLastStreamElement() == null) {
			Log.e(TAG, "There is no signal!");
		}
		else {
			postStreamElement(getTheLastStreamElement());
		}
	}


	public String getWrapperName() {
		return this.getClass().getSimpleName();
	}
	
	

	public StreamElement getTheLastStreamElement() {
		return theLastStreamElement;
	}

	public void setTheLastStreamElement(StreamElement theLastStreamElement) {
		this.theLastStreamElement = theLastStreamElement;
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
	public void runOnce() {
		// TODO Auto-generated method stub
		
	}
	
	public class ActivityRecognitionService extends IntentService implements GooglePlayServicesClient.ConnectionCallbacks,GooglePlayServicesClient.OnConnectionFailedListener {
		
		AbstractWrapper w = null;
		
		private PendingIntent pIntent;
		
		private VSensorConfig config = null;
		
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

	@Override
	public Class<? extends WrapperService> getSERVICE() {
		// TODO Auto-generated method stub
		return null;
	}


}
