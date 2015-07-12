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

import java.util.ArrayList;

import tinygsn.beans.DataField;
import tinygsn.beans.DataTypes;
import tinygsn.beans.StreamElement;
import tinygsn.model.wrappers.utils.MICSensor;
import tinygsn.model.wrappers.utils.MICSensor.VirtualSensorDataListener;
import tinygsn.services.WrapperService;
import tinygsn.storage.db.SqliteStorageManager;
import android.app.Activity;


public class USBplugO3Wrapper extends AbstractWrapper implements VirtualSensorDataListener{

	private static final String[] FIELD_NAMES = new String[] { "resistanceo",
			"resistancev", "humidity", "temperature", "ozonecalibrated",
			"voccalibrated" };

	private static final Byte[] FIELD_TYPES = new Byte[] { DataTypes.DOUBLE,
			DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.DOUBLE,
			DataTypes.DOUBLE };

	private static final String[] FIELD_DESCRIPTION = new String[] {
			"resistanceO", "resistanceV", "humidity", "temperature",
			"ozoneCalibrated", "vocCalibrated" };

	private static final String[] FIELD_TYPES_STRING = new String[] { "double",
			"double", "double", "double", "double", "double" };
	
	public static final Class<USBplugService> SERVICE = USBplugService.class;

	private MICSensor sensor;

	public void runOnce() {
		Activity activity = getConfig().getController().getActivity();
		SqliteStorageManager storage = new SqliteStorageManager(activity);
		int samplingPeriod = storage.getSamplingRateByName("tinygsn.model.wrappers.USBplugO3Wrapper");
		if(samplingPeriod>0){
			sensor = new MICSensor(activity);
			sensor.initSensor();
			sensor.setListener(this);
			try {
				Thread.sleep(samplingRate / 2);
				sensor.getMeasurement();
				Thread.sleep(samplingRate / 2);
			}
			catch (InterruptedException e) {}
		}
	}


	@Override
	public void consume(StreamElement se) {
	    postStreamElement(se);
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

	
	public static class USBplugService extends WrapperService{

		public USBplugService() {
			super("usbPlugService");

		}
	}

}