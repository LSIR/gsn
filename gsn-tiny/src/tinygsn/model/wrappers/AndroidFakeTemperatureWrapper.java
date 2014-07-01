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
* File: gsn-tiny/src/tinygsn/model/wrappers/AndroidFakeTemperatureWrapper.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.model.wrappers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import tinygsn.beans.DataField;
import tinygsn.beans.DataTypes;
import tinygsn.beans.Queue;
import tinygsn.beans.StreamElement;
import android.util.Log;

public class AndroidFakeTemperatureWrapper extends AbstractWrapper {

	private static final int DEFAULT_SAMPLING_RATE = 500;

	private static final String[] FIELD_NAMES = new String[] { "temp" };

	private static final Byte[] FIELD_TYPES = new Byte[] { DataTypes.DOUBLE };

	private static final String[] FIELD_DESCRIPTION = new String[] { "Temp Reading" };

	private static final String[] FIELD_TYPES_STRING = new String[] { "double" };

	private int samplingRate = DEFAULT_SAMPLING_RATE;

	private final String TAG = this.getClass().getSimpleName();

	private static int threadCounter = 0;

	public AndroidFakeTemperatureWrapper() {
		super();
	}

	public AndroidFakeTemperatureWrapper(Queue queue) {
		super(queue);
	}

	public boolean initialize() {
		return true;
	}

	public void run() {
		Log.v(TAG, " waiting for data");
		double temp = 23;

		while (isActive()) {
			temp = tinygsn.utils.MathUtils.getNextRandomValue(temp, -10, 35, 1);
			try {
				Thread.sleep(samplingRate);
			}
			catch (InterruptedException e) {
				Log.e(e.getMessage(), e.toString());
			}
			Date time = new Date();
			StreamElement streamElement = new StreamElement(FIELD_NAMES, FIELD_TYPES,
					new Serializable[] { temp }, time.getTime());
			postStreamElement(streamElement);
		}
	}

	public void dispose() {
		threadCounter--;
	}

	public String getWrapperName() {
		return TAG;
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

}