package tinygsn.services;

import java.io.Serializable;

import tinygsn.beans.InputStream;
import tinygsn.beans.StreamElement;
import tinygsn.beans.StreamSource;
import tinygsn.beans.VSensorConfig;
import tinygsn.controller.AndroidControllerListVSNew;
import tinygsn.model.wrappers.AbstractWrapper;
import tinygsn.model.wrappers.AndroidLightWrapper;
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

public class LightSensorService extends IntentService implements SensorEventListener{

	private VSensorConfig config = null;
	private static final String TAG = "LightSensorService";
	public AndroidControllerListVSNew VSNewController;
	public AbstractWrapper w;
	private SensorManager mSensorManager;
	private Sensor mSensor;
	
	public LightSensorService()
	{
		super("LightSensorService");
	}
	public LightSensorService(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
	double distance = event.values[0];
		
		StreamElement streamElement = new StreamElement(w.getFieldList(), w.getFieldType(),
				new Serializable[] { distance });

		((AndroidLightWrapper)w).setTheLastStreamElement(streamElement);
 	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle b = intent.getExtras();
		config = (VSensorConfig) b.get("tinygsn.beans.config");
		
		for (InputStream inputStream : config.getInputStreams()) {
			for (StreamSource streamSource : inputStream.getSources()) {
				w = streamSource.getWrapper();
				
				Activity activity = config.getController().getActivity();
				mSensorManager = (SensorManager) activity
						.getSystemService(Context.SENSOR_SERVICE);
				mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
				mSensorManager.registerListener(this, mSensor,
						SensorManager.SENSOR_DELAY_NORMAL);

//				while (w.isActive()) {
				while(true)
				{
					try {
						Thread.sleep(w.getSamplingRate());
						((AndroidLightWrapper) w).getLastKnownData();
					}
					catch (InterruptedException e) {
						Log.e(e.getMessage(), e.toString());
					}
				}
			}
		}
		
	}

}
