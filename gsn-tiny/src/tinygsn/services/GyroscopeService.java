package tinygsn.services;

import java.io.Serializable;

import tinygsn.beans.InputStream;
import tinygsn.beans.StreamElement;
import tinygsn.beans.StreamSource;
import tinygsn.beans.VSensorConfig;
import tinygsn.controller.AndroidControllerListVSNew;
import tinygsn.model.wrappers.AbstractWrapper;
import tinygsn.model.wrappers.AndroidGyroscopeWrapper;
import tinygsn.storage.db.SqliteStorageManager;
import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

public class GyroscopeService  extends IntentService implements SensorEventListener{

	private VSensorConfig config = null;
	private static final String TAG = "ServiceVS";
	public AbstractWrapper w;
	private SensorManager mSensorManager;
	private Sensor mSensor;
	SqliteStorageManager storage = null;
	int samplingRate = -1;
		
	
	public GyroscopeService()
	{
		super("service");
	}
	public GyroscopeService(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	  @Override
	    protected void onHandleIntent(Intent intent) 
	  {
		  
			Bundle b = intent.getExtras();
			config = (VSensorConfig) b.get("tinygsn.beans.config");
			storage = new SqliteStorageManager(config.getController().getActivity());
			samplingRate = storage.getSamplingRateByName("tinygsn.model.wrappers.AndroidGPSWrapper");
			
			for (InputStream inputStream : config.getInputStreams()) {
				for (StreamSource streamSource : inputStream.getSources()) {
					w = streamSource.getWrapper();	
					Activity activity = config.getController().getActivity();
					mSensorManager = (SensorManager) activity
							.getSystemService(Context.SENSOR_SERVICE);
					mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
					mSensorManager.registerListener(this, mSensor,
							SensorManager.SENSOR_DELAY_NORMAL);

					while(w.isActive())
					{
						try {
							Thread.sleep(w.getSamplingRate()/(1+ samplingRate));
							((AndroidGyroscopeWrapper) w).getLastKnownData();
						}
						catch (InterruptedException e) {
							Log.e(e.getMessage(), e.toString());
						}
					}
				}
			}
	  }

	@Override
	public void onSensorChanged(SensorEvent event) {
		double x = event.values[0];
		double y = event.values[1];
		double z = event.values[2];

		StreamElement streamElement = new StreamElement(w.getFieldList(), w.getFieldType(),
				new Serializable[] { x, y, z });

		((AndroidGyroscopeWrapper) w).theLastStreamElement = streamElement;
	}
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
}
