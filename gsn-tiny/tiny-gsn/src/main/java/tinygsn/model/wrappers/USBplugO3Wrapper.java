/**
 * Global Sensor Networks (GSN) Source Code
 * Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
 * <p/>
 * This file is part of GSN.
 * <p/>
 * GSN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * GSN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with GSN. If not, see <http://www.gnu.org/licenses/>.
 * <p/>
 * File: gsn-tiny/src/tinygsn/model/wrappers/USBplugO3Wrapper.java
 *
 * @author Do Ngoc Hoan
 */
package tinygsn.model.wrappers;

//import java.io.Serializable;

import java.util.ArrayList;

import tinygsn.beans.DataField;
import tinygsn.beans.DataTypes;
import tinygsn.beans.StreamElement;
import tinygsn.beans.WrapperConfig;
import tinygsn.model.wrappers.utils.MICSensor;
import tinygsn.model.wrappers.utils.MICSensor.VirtualSensorDataListener;
import tinygsn.services.WrapperService;


public class USBplugO3Wrapper extends AbstractWrapper implements VirtualSensorDataListener {

	public USBplugO3Wrapper(WrapperConfig wc) {
		super(wc);
	}

	public USBplugO3Wrapper() {
	}


	private static final String[] FIELD_NAMES = new String[]{"resistanceo",
			                                                        "resistancev", "humidity", "temperature", "ozonecalibrated",
			                                                        "voccalibrated"};

	private static final Byte[] FIELD_TYPES = new Byte[]{DataTypes.DOUBLE,
			                                                    DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.DOUBLE,
			                                                    DataTypes.DOUBLE};

	private static final String[] FIELD_DESCRIPTION = new String[]{
			                                                              "resistanceO", "resistanceV", "humidity", "temperature",
			                                                              "ozoneCalibrated", "vocCalibrated"};

	private static final String[] FIELD_TYPES_STRING = new String[]{"double",
			                                                               "double", "double", "double", "double", "double"};

	public final Class<? extends WrapperService> getSERVICE() {
		return USBplugService.class;
	}

	private MICSensor sensor;

	@Override
	public void runOnce() {
		updateWrapperInfo();
		if (dcDuration > 0) {
			sensor = MICSensor.getInstance();
			sensor.initSensor();
			sensor.setListener(this);
			try {
				Thread.sleep(12000);
				sensor.getMeasurement();
				Thread.sleep(12000);
			} catch (InterruptedException e) {
			}
			//StreamElement se = new StreamElement(getOutputStructure(), new Serializable[] {10, 20, 60, 30, 20.3,10.3 });
			//consume(se);
		}
	}


	@Override
	public void consume(StreamElement se) {
		postStreamElement(se);
	}

	@Override
	public DataField[] getOutputStructure() {
		ArrayList<DataField> output = new ArrayList<DataField>();
		for (int i = 0; i < FIELD_NAMES.length; i++)
			output.add(new DataField(FIELD_NAMES[i], FIELD_TYPES_STRING[i],
					                        FIELD_DESCRIPTION[i]));

		return output.toArray(new DataField[]{});
	}

	@Override
	public String[] getFieldList() {
		return FIELD_NAMES;
	}

	@Override
	public Byte[] getFieldType() {
		return FIELD_TYPES;
	}


	public static class USBplugService extends WrapperService {

		public USBplugService() {
			super("usbPlugService");

		}
	}

}