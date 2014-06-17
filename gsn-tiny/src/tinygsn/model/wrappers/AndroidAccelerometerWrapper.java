package tinygsn.model.wrappers;

import java.io.Serializable;
import java.util.ArrayList;
import tinygsn.beans.DataField;
import tinygsn.beans.DataTypes;
import tinygsn.beans.Queue;
import tinygsn.beans.StreamElement;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class AndroidAccelerometerWrapper extends AbstractWrapper implements
		SensorEventListener {

	private static final String[] FIELD_NAMES = new String[] { "x", "y", "z" };

	private static final Byte[] FIELD_TYPES = new Byte[] { DataTypes.DOUBLE,
			DataTypes.DOUBLE, DataTypes.DOUBLE };

	private static final String[] FIELD_DESCRIPTION = new String[] { "x", "y",
			"z" };

	private static final String[] FIELD_TYPES_STRING = new String[] { "double",
			"double", "double" };

	private static final String TAG = "AndroidAccelerometerWrapper";

	private SensorManager mSensorManager;
	private Sensor mSensor;

	private StreamElement theLastStreamElement = null;

	public AndroidAccelerometerWrapper() {
		super();
	}

	public AndroidAccelerometerWrapper(Queue queue) {
		super(queue);
		initialize();
	}

	public boolean initialize() {
		return true;
	}

	public void run() {
		Activity activity = getConfig().getController().getActivity();
		mSensorManager = (SensorManager) activity
				.getSystemService(Context.SENSOR_SERVICE);
		mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensorManager.registerListener(this, mSensor,
				SensorManager.SENSOR_DELAY_NORMAL);

		while (isActive()) {
			try {
				Thread.sleep(samplingRate);
				getLastKnownData();
			}
			catch (InterruptedException e) {
				Log.e(e.getMessage(), e.toString());
			}
		}
	}

	private void getLastKnownData() {
		if (theLastStreamElement == null) {
			Log.e(TAG, "There is no signal!");
		}
		else {
			postStreamElement(theLastStreamElement);
		}
	}

	public void dispose() {
	}

	public String getWrapperName() {
		return this.getClass().getSimpleName();
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
	public void onAccuracyChanged(Sensor arg0, int arg1) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		double x = event.values[0];
		double y = event.values[1];
		double z = event.values[2];

		StreamElement streamElement = new StreamElement(FIELD_NAMES, FIELD_TYPES,
				new Serializable[] { x, y, z });

		theLastStreamElement = streamElement;
	}

}