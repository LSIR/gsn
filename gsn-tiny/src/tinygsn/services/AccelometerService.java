package tinygsn.services;

import java.io.Serializable;

import tinygsn.beans.InputStream;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.beans.StreamSource;
import tinygsn.beans.VSensorConfig;
import tinygsn.controller.AndroidControllerListVSNew;
import tinygsn.gui.android.ActivityListVSNew;
import tinygsn.model.vsensor.VirtualSensor;
import tinygsn.model.wrappers.AbstractWrapper;
import tinygsn.model.wrappers.AndroidAccelerometerWrapper;
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
import android.renderscript.Sampler;
import android.util.Log;

public class AccelometerService extends IntentService implements SensorEventListener {

	private VSensorConfig config = null;
	private static final String TAG = "AccelometerService";
	public AndroidControllerListVSNew VSNewController;
	public AbstractWrapper w;
	private SensorManager mSensorManager;
	private Sensor mSensor;
	SqliteStorageManager storage = null;
	
	public AccelometerService() {
		super("AccelometerService");
	}
	public AccelometerService(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	
	
	@Override
	public void onSensorChanged(SensorEvent event) {
	//	Log.i("onSensorChanged", "OnSensorChanged");
		double x = event.values[0];
		double y = event.values[1];
		double z = event.values[2];
		StreamElement streamElement = new StreamElement(w.getFieldList(), w.getFieldType(),
				new Serializable[] { x, y, z });

		((AndroidAccelerometerWrapper) w).setTheLastStreamElement(streamElement);
		
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle b = intent.getExtras();
		config = (VSensorConfig) b.get("tinygsn.beans.config");
		storage = new SqliteStorageManager(config.getController().getActivity());
		
		VirtualSensor vs = new VirtualSensor(config, config.getController().getActivity());
		
		if(config.getRunning() == false)
			return;
		
		for (InputStream inputStream : config.getInputStreams()) {
			for (StreamSource streamSource : inputStream.getSources()) {
				w = streamSource.getWrapper();
				Activity activity = config.getController().getActivity();
				mSensorManager = (SensorManager) activity
						.getSystemService(Context.SENSOR_SERVICE);
				mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
				mSensorManager.registerListener(this, mSensor,
						SensorManager.SENSOR_DELAY_NORMAL);
				
				
				while (w.isActive()) 
				{
					storage = new SqliteStorageManager(config.getController().getActivity());
					int samplingRate = storage.getSamplingRateByName("tinygsn.model.wrappers.AndroidAccelerometerWrapper");
					try {
						Thread.sleep(w.getSamplingRate()*(1+samplingRate));
						if (samplingRate > 0){
							((AndroidAccelerometerWrapper) w).getLastKnownData();
						}
					}
					catch (InterruptedException e) {
						Log.e(e.getMessage(), e.toString());
					}
				}
			}
		}
		
	}

}
