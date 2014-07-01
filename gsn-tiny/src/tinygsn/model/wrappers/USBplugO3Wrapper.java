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
* File: gsn-tiny/src/tinygsn/model/wrappers/USBplugO3Wrapper.java
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
import tinygsn.model.wrappers.utils.MICSensor;
import tinygsn.model.wrappers.utils.MICSensor.VirtualSensorDataListener;
import android.app.Activity;
import android.os.Looper;
import android.util.Log;

public class USBplugO3Wrapper extends AbstractWrapper {

	private static final String[] FIELD_NAMES = new String[] { "resistanceo",
			"resistancev", "humidity", "temperature", "ozonecalibrated",
			"voccalibrated" };

	// df[0] = new DataField("resistanceO", "int");
	// df[1] = new DataField("resistanceV", "int");
	// df[2] = new DataField("humidity", "double");
	// df[3] = new DataField("temperature", "double");
	// df[4] = new DataField("ozoneCalibrated", "double");
	// df[5] = new DataField("vocCalibrated", "double");

	private static final Byte[] FIELD_TYPES = new Byte[] { DataTypes.DOUBLE,
			DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.DOUBLE,
			DataTypes.DOUBLE };

	private static final String[] FIELD_DESCRIPTION = new String[] {
			"resistanceO", "resistanceV", "humidity", "temperature",
			"ozoneCalibrated", "vocCalibrated" };

	private static final String[] FIELD_TYPES_STRING = new String[] { "double",
			"double", "double", "double", "double", "double" };

	private static final String TAG = "USBplugO3Wrapper";

	private MICSensor sensor;

	private StreamElement theLastStreamElement = null;
	private Activity activity;

	public USBplugO3Wrapper() {
		super();
	}

	public USBplugO3Wrapper(Queue queue) {
		super(queue);
	}

	public boolean initialize() {
		activity = getConfig().getController().getActivity();
		sensor = new MICSensor(activity);
		return true;
	}

	public void run() {
		l(" is waiting for data");
		l("initSensor");
		
		Looper.prepare();
		initialize();

		sensor.initSensor();

		sensor.setListener(new VirtualSensorDataListener() {
			@Override
			public void consume(StreamElement se) {
				
//				StreamElement se = new StreamElement(df, new Serializable[] {
//						resistanceO, resistanceV, humidity, temperature, ozoneCalculated,
//						vocCalculated });

//				 Serializable[] fv = se.getData();
//				 StreamElement streamElement = new StreamElement(FIELD_NAMES,
//							FIELD_TYPES, new Serializable[] { fv });
				 
				// Serializable[] fv2 = new Serializable[fv.length];
				// for (int i = 0; i < fv.length; i++){
				// fv2[i] = new Serializable(getDouble(fv[i]));
				// }
				
				double resistanceO = getDouble(se.getData()[0]);
				double resistanceV = getDouble(se.getData()[1]);
				double humidity = getDouble(se.getData()[2]);
				double temperature = getDouble(se.getData()[3]);
				double ozoneCalculated = getDouble(se.getData()[4]);
				double vocCalculated = getDouble(se.getData()[5]);
				
				StreamElement streamElement = new StreamElement(FIELD_NAMES,
						FIELD_TYPES, new Serializable[] { resistanceO, resistanceV,
								humidity, temperature, ozoneCalculated, vocCalculated });
				
				theLastStreamElement = streamElement;
//				showLog(TAG + ": " + theLastStreamElement.toString());
//				Log.v(TAG, streamElement.toString());
			}
		});

		while (isActive()) {
			try {
				Thread.sleep(samplingRate / 2);
				sensor.getMeasurement();
				Thread.sleep(samplingRate / 2);
				getLastKnownValues();
			}
			catch (InterruptedException e) {
				Log.e(e.getMessage(), e.toString());
			}
		}
		// sensor.Pause();
		Looper.loop();
	}

	private double getDouble(Serializable s) {
		double d = ((Number) s).doubleValue();
		return d;
	}

	private void getLastKnownValues() {
//		StreamElement se = new StreamElement(FIELD_NAMES,
//				FIELD_TYPES, new Serializable[] { 1d,
//				1d, 1d, 1d, 1d, 1d });
//		theLastStreamElement = se;
		
		if (theLastStreamElement == null) {
			Log.e(TAG, "There is no signal!");
		}
		else {
			postStreamElement(theLastStreamElement);
			l("Received: " + theLastStreamElement.toString());
		}
	}

	public String getWrapperName() {
		return "USBplugO3Wrapper";
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

	void l(String text){
		Log.v(TAG, text);
	}
	
//	void showLog(final String text) {
//		activity.runOnUiThread(new Runnable() {
//			public void run() {
//				Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
//			}
//		});
//	}
}