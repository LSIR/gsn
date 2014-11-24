/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
*
* This file is part of GSN.
*
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with GSN. If not, see <http://www.gnu.org/licenses/>.
*
* File: gsn-tiny/src/tinygsn/model/wrappers/AndroidRotationVectorWrapper.java
*
* @author Do Ngoc Hoan
*/


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

public class AndroidRotationVectorWrapper extends AbstractWrapper implements
		SensorEventListener {

	private static final String[] FIELD_NAMES = new String[] { "x", "y", "z", "scalar" };

	private static final Byte[] FIELD_TYPES = new Byte[] { DataTypes.DOUBLE,
			DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.DOUBLE };

	private static final String[] FIELD_DESCRIPTION = new String[] { "x", "y",
			"z", "scalar"};

	private static final String[] FIELD_TYPES_STRING = new String[] { "double",
			"double", "double", "double" };

	private static final String TAG = "AndroidRotationVectorWrapper";

	private SensorManager mSensorManager;
	private Sensor mSensor;

	private StreamElement theLastStreamElement = null;

	public AndroidRotationVectorWrapper() {
		super();
	}

	public AndroidRotationVectorWrapper(Queue queue) {
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
		mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
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
		double scalar = event.values[3];
		
		StreamElement streamElement = new StreamElement(FIELD_NAMES, FIELD_TYPES,
				new Serializable[] { x, y, z, scalar });

		theLastStreamElement = streamElement;
	}

}