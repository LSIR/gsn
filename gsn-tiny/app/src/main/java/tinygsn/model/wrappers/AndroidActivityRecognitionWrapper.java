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
 * @author Marc Schaer
 */

package tinygsn.model.wrappers;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.io.Serializable;
import java.util.ArrayList;

import tinygsn.beans.DataField;
import tinygsn.beans.DataTypes;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.beans.WrapperConfig;
import tinygsn.services.WrapperService;

public class AndroidActivityRecognitionWrapper extends AbstractWrapper implements ConnectionCallbacks, OnConnectionFailedListener {

	private static final String[] FIELD_NAMES = new String[]{"type", "confidence"};

	private static final Byte[] FIELD_TYPES = new Byte[]{DataTypes.DOUBLE,
			                                                    DataTypes.DOUBLE};

	private static final String[] FIELD_DESCRIPTION = new String[]{"Type", "Confidence"};

	private static final String[] FIELD_TYPES_STRING = new String[]{"double", "double"};

	private static final String TAG = "ActivityRecognitionWrap";

	private StreamElement theLastStreamElement = null;

	private Context context = StaticData.globalContext;
	private GoogleApiClient mGoogleApiClient = null;
	private AbstractWrapper w = null;

	public AndroidActivityRecognitionWrapper() {
	}

	public AndroidActivityRecognitionWrapper(WrapperConfig wc) {
		super(wc);
	}

	private void connectGoogleAPI() {
		mGoogleApiClient = new GoogleApiClient.Builder(context)
				                   .addApi(ActivityRecognition.API)
				                   .addConnectionCallbacks(this)
				                   .addOnConnectionFailedListener(this)
				                   .build();
		mGoogleApiClient.connect();
		Log.d(TAG, "Connected to google API");
	}

	private void disconnectGoogleAPI() {
		mGoogleApiClient.disconnect();
		mGoogleApiClient = null;
		Log.d(TAG, "Disconnected from google API");
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
	public void runOnce() {
		if (!ActivityRecognitionService.isRunning) {
			disconnectGoogleAPI();
		} else {
			if (mGoogleApiClient == null) {
				connectGoogleAPI();
			}
			updateWrapperInfo();

			if (ActivityRecognitionResult.hasResult(ActivityRecognitionService.getIntent())) {
				// Get the update
				ActivityRecognitionResult result =
						ActivityRecognitionResult.extractResult(ActivityRecognitionService.getIntent());

				DetectedActivity mostProbableActivity
						= result.getMostProbableActivity();

				// Get the confidence % (probability)
				double confidence = mostProbableActivity.getConfidence();

				// Get the type
				double activityType = mostProbableActivity.getType();
		   /* types:
		    * DetectedActivity.IN_VEHICLE
            * DetectedActivity.ON_BICYCLE
            * DetectedActivity.ON_FOOT
            * DetectedActivity.STILL
            * DetectedActivity.UNKNOWN
            * DetectedActivity.TILTING
            */
				StreamElement streamElement = new StreamElement(FIELD_NAMES, FIELD_TYPES,
						                                               new Serializable[]{activityType, confidence});
				postStreamElement(streamElement);
			} else {
				Log.d(TAG, "No results for AndroidActivityRecognition");
			}
		}
	}

	@Override
	public void onConnected(Bundle bundle) {
		PendingIntent mActivityReconPendingIntent = PendingIntent
				                                            .getService(context, 0, ActivityRecognitionService.getIntent(), PendingIntent.FLAG_UPDATE_CURRENT);

		Log.d(TAG, "connected to ActivityRecognition");
		ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, 0, mActivityReconPendingIntent);

	}

	@Override
	public void onConnectionSuspended(int cause) {
		Log.d(TAG, "Suspended to ActivityRecognition");
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		Log.d(TAG, "Not connected to ActivityRecognition");
	}


	public static class ActivityRecognitionService extends WrapperService {
		private static Intent mIntent = null;
		private static boolean isRunning = true;


		/*
		// FIXME : From old version. What to do with that variables ?
		private VSensorConfig config = null;
		//private ActivityRecognitionClient arclient;
		private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss.SSSZ";
		private SimpleDateFormat mDateFormat;
		SqliteStorageManager storage = null;
		int samplingRate = -1;
		*/

		public ActivityRecognitionService() {
			super("activityRecognitionService");
		}


		@Override
		protected void onHandleIntent(Intent intent) {
			mIntent = intent;
			//FIXME: better solution ? Used for disconnecting GoogleAPI
			Bundle b = intent.getBundleExtra("tinygsn.beans.config");
			config = b.getParcelable("tinygsn.beans.config");
			isRunning = config.isRunning();
			if (!isRunning) {
				try {
					w = StaticData.getWrapperByName(config.getWrapperName());
					w.runOnce();
				} catch (Exception e1) {
				}
			}

			super.onHandleIntent(intent);
		}

		private static Intent getIntent() {
			return ActivityRecognitionService.mIntent;
		}

		public static String getType(double type) {
			if (type == DetectedActivity.UNKNOWN)
				return "Unknown";
			else if (type == DetectedActivity.IN_VEHICLE)
				return "In Vehicle";
			else if (type == DetectedActivity.ON_BICYCLE)
				return "On Bicycle";
			else if (type == DetectedActivity.ON_FOOT)
				return "On Foot";
			else if (type == DetectedActivity.STILL)
				return "Still";
			else if (type == DetectedActivity.TILTING)
				return "Tilting";
			else
				return "";
		}



/*
		@Override
		public int onStartCommand(Intent intent, int flags, int startId) {
			if (intent == null) {
				return START_REDELIVER_INTENT;
			}
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
			while (resp != ConnectionResult.SUCCESS)
				resp = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
			if (resp == ConnectionResult.SUCCESS) {
				arclient = new ActivityRecognitionClient(this, this, this);
				arclient.connect();


			}

			if (ActivityRecognitionResult.hasResult(intent)) {

				ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
				DetectedActivity mostProbableActivity = result.getMostProbableActivity();
				Double confidence = (double) mostProbableActivity.getConfidence();
				Double activityType = (double) mostProbableActivity.getType();

				StreamElement streamElement = new StreamElement(w.getFieldList(),
						                                               w.getFieldType(), new Serializable[]{activityType, confidence});

				((AndroidActivityRecognitionWrapper) w).setTheLastStreamElement(streamElement);
				((AndroidActivityRecognitionWrapper) w).getLastKnownData();
			}
			return START_NOT_STICKY; //START_REDELIVER_INTENT;

		}

		//@Override
		public void onConnectionFailed(ConnectionResult arg0) {
			// TODO Auto-generated method stub

		}

		//@Override
		public void onConnected(Bundle arg0) {
			Intent intent = new Intent(this, ActivityRecognitionService.class);
			pIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			/*if(samplingRate > 0)	
				arclient.requestActivityUpdates(100, pIntent); 
			else 
				arclient.removeActivityUpdates(pIntent);
		}

		@Override
		public void onDisconnected() {
			// TODO Auto-generated method stub
		}
	*/
	}

	@Override
	public Class<? extends WrapperService> getSERVICE() {
		return ActivityRecognitionService.class;
	}


}
