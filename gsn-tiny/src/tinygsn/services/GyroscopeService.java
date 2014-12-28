package tinygsn.services;

import java.io.Serializable;

import tinygsn.beans.InputStream;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.beans.StreamSource;
import tinygsn.beans.VSensorConfig;
import tinygsn.controller.AndroidControllerListVSNew;
import tinygsn.model.vsensor.VirtualSensor;
import tinygsn.model.wrappers.AbstractWrapper;
import tinygsn.model.wrappers.AndroidGyroscopeWrapper;
import tinygsn.storage.db.SqliteStorageManager;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
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
	public AbstractWrapper w;
	private SensorManager mSensorManager;
	private Sensor mSensor;
	SqliteStorageManager storage = null;
		
	
	public GyroscopeService()
	{
		super("GyroscopeService");
	}
	public GyroscopeService(String name) {
		super(name);
	}
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		double x = event.values[0];
		double y = event.values[1];
		double z = event.values[2];

		StreamElement streamElement = new StreamElement(w.getFieldList(), w.getFieldType(),
				new Serializable[] { x, y, z });

		((AndroidGyroscopeWrapper) w).postStreamElement(streamElement);
	}
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}

	  @Override
	    protected void onHandleIntent(Intent intent) 
	  {
			Bundle b = intent.getExtras();
			config = (VSensorConfig) b.get("tinygsn.beans.config");
			storage = new SqliteStorageManager(config.getController().getActivity());
			VirtualSensor vs = new VirtualSensor(config, config.getController().getActivity());
			
			for (InputStream inputStream : config.getInputStreams()) {
				for (StreamSource streamSource : inputStream.getSources()) {
					w = streamSource.getWrapper();	
					Activity activity = config.getController().getActivity();
					mSensorManager = (SensorManager) activity
							.getSystemService(Context.SENSOR_SERVICE);
					mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

					//while(w.isActive())
					{
						try {
							int samplingRate = storage.getSamplingRateByName("tinygsn.model.wrappers.AndroidGyroscopeWrapper");
							
							if (samplingRate > 0){
								mSensorManager.registerListener(this, mSensor,60000); //around 16Hz
								Thread.sleep(samplingRate*1000);
								mSensorManager.unregisterListener(this);
							}
						//	Thread.sleep(15*1000);
						}
						catch (InterruptedException e) {
							Log.e(e.getMessage(), e.toString());
						}
					}
					AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
					am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+15000,PendingIntent.getService(activity, 0, intent,PendingIntent.FLAG_UPDATE_CURRENT));
				}
			}
	  }

	
}
