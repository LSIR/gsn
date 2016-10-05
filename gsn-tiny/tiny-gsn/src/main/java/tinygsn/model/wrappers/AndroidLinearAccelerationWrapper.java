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
* File: gsn-tiny/src/tinygsn/model/wrappers/AndroidLinearAccelerationWrapper.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.model.wrappers;

import java.io.Serializable;
import java.util.ArrayList;

import tinygsn.beans.DataField;
import tinygsn.beans.DataTypes;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.beans.WrapperConfig;
import tinygsn.services.WrapperService;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class AndroidLinearAccelerationWrapper extends AbstractWrapper implements
		SensorEventListener {

	public AndroidLinearAccelerationWrapper(WrapperConfig wc) {
		super(wc);
	}
	public AndroidLinearAccelerationWrapper() {
	}

	private static final String[] FIELD_NAMES = new String[] { "x", "y", "z" };
	private static final Byte[] FIELD_TYPES = new Byte[] { DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.DOUBLE };
	private static final String[] FIELD_DESCRIPTION = new String[] { "x", "y", "z" };
	private static final String[] FIELD_TYPES_STRING = new String[] { "double", "double", "double" };

	public final Class<? extends WrapperService> getSERVICE(){ return LinearAccService.class;}
	
	private SensorManager mSensorManager;
	private Sensor mSensor;

	@Override
	public void runOnce() {
		mSensorManager = (SensorManager) StaticData.globalContext.getSystemService(Context.SENSOR_SERVICE);
		mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		updateWrapperInfo();
		try {
			if (dcDuration > 0){
				mSensorManager.registerListener(this, mSensor,SensorManager.SENSOR_DELAY_NORMAL);  
				Thread.sleep(dcDuration*1000);
				mSensorManager.unregisterListener(this);
			}
		}
		catch (InterruptedException e) {
			Log.e(e.getMessage(), e.toString());
		}
	}

	@Override
	public DataField[] getOutputStructure() {
		ArrayList<DataField> output = new ArrayList<DataField>();
		for (int i = 0; i < FIELD_NAMES.length; i++)
			output.add(new DataField(FIELD_NAMES[i], FIELD_TYPES_STRING[i],
					FIELD_DESCRIPTION[i]));

		return output.toArray(new DataField[output.size()]);
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
	public void onAccuracyChanged(Sensor arg0, int arg1) {}

	@Override
	public void onSensorChanged(SensorEvent event) {
		double x = event.values[0];
		double y = event.values[1];
		double z = event.values[2];

		StreamElement streamElement = new StreamElement(FIELD_NAMES, FIELD_TYPES,
				new Serializable[] { x, y, z });

		postStreamElement(streamElement);
	}
	
	public static class LinearAccService extends WrapperService{

		public LinearAccService() {
			super("linearAccService");

		}
	}

}