/**
 * Global Sensor Networks (GSN) Source Code
 * Copyright (c) 2006-2015, Ecole Polytechnique Federale de Lausanne (EPFL)
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
 * File: gsn-tiny/src/tinygsn/model/wrappers/AndroidGyroscopeWrapper.java
 *
 * @author Marc Schaer
 */

package tinygsn.model.wrappers;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.zip.DataFormatException;

import tinygsn.beans.DataField;
import tinygsn.beans.DataTypes;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.beans.WrapperConfig;
import tinygsn.model.utils.Parameter;
import tinygsn.model.utils.ParameterType;
import tinygsn.services.WrapperService;

/**
 * The GPSFileWrapper is only used to read a file as a wrapper get data from physical sensors.
 * This is used for CHUV GPS data
 * The file should be a CSV file with construction : UnixTime,latitude,longitude
 */
public class GPSFileWrapper extends AbstractWrapper {
	public GPSFileWrapper(WrapperConfig wc) {
		super(wc);
	}

	public GPSFileWrapper() {
	}

	private static final String[] FIELD_NAMES = new String[]{"latitudeTopLeft", "longitudeTopLeft", "latitudeBottomRight", "longitudeBottomRight"};
	private static final Byte[] FIELD_TYPES = new Byte[]{DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.DOUBLE};
	private static final String[] FIELD_DESCRIPTION = new String[]{"TopLeftLatitude", "TopLeftLongitude", "BottomRightLatitude", "BottomRightLongitude"};
	private static final String[] FIELD_TYPES_STRING = new String[]{"double", "double", "double", "double"};
	private static final String LOGTAG = "GPSFileWrapper";
	// Max wait time between processing two measurement (in ms)
	private static final Long MAX_WAIT_TIME = Long.valueOf(30000);

	public final Class<? extends WrapperService> getSERVICE() {
		return GPSFileWrapperService.class;
	}

	@Override
	public void runOnce() {
		if (getConfig().isRunning()) {
			getConfig().setRunning(false);
			readGPSFile();
		}
	}

	private void readGPSFile() {
		try {
			String path = parameters.get("wrapper:tinygsn.model.wrappers.GPSFileWrapper:File");
			if (path != null) {
				log(StaticData.globalContext, "Path: " + path);
				InputStream is = StaticData.globalContext.getAssets().open(path);
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));

				String mLine;
				LinkedHashMap<Long, LatLng> map = new LinkedHashMap<>();
				while ((mLine = reader.readLine()) != null) {
					String[] elements = mLine.split(",");
					if (elements.length != 3) {
						throw new DataFormatException("Entries for file " + path + " does not have 3 elements as expected");
					} else {
						log(StaticData.globalContext, "Read from " + path + " : " + mLine);
						if (elements[0].matches("[0-9]+")) {
							map.put(Long.valueOf(elements[0]), new LatLng(Double.valueOf(elements[1]), Double.valueOf(elements[2])));
						}
					}
				}

				Long currentTime = Long.valueOf(0);
				Long nextTime;
				for (Long time : map.keySet()) {
					if (currentTime == Long.valueOf(0)) {
						currentTime = time;
					} else {
						nextTime = time;
						LatLng location = map.get(currentTime);
						StreamElement streamElement = new StreamElement(getFieldList(),
							getFieldType(),
							new Serializable[]{location.latitude, location.longitude,
								location.latitude, location.longitude});
						streamElement.setTimeStamp(currentTime);
						log(StaticData.globalContext, "Post streamElement : " + streamElement.toString());
						postStreamElement(streamElement);
						Thread.sleep(Math.min(nextTime - currentTime, Long.valueOf(MAX_WAIT_TIME)));
						currentTime = nextTime;
					}
				}
				LatLng location = map.get(currentTime);
				StreamElement streamElement = new StreamElement(getFieldList(),
					getFieldType(),
					new Serializable[]{location.latitude, location.longitude,
						location.latitude, location.longitude});
				streamElement.setTimeStamp(currentTime);
				postStreamElement(streamElement);
				log(StaticData.globalContext, "End of Processing file " + path + " for " + LOGTAG);
			}
		} catch (Exception e) {
			Log.e(LOGTAG, e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public DataField[] getOutputStructure() {
		ArrayList<DataField> output = new ArrayList<>();
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

	@Override
	public ArrayList<Parameter> getParameters() {
		ArrayList<Parameter> params = new ArrayList<>();
		ArrayList<String> files = new ArrayList<>();
		String[] filesArray = {};
		try {
			filesArray = StaticData.globalContext.getAssets().list("chuv_data");
		} catch (Exception e) {
			Log.e(LOGTAG, e.getMessage());
			e.printStackTrace();
		}
		for (String file : filesArray) {
			files.add("chuv_data/" + file);
		}
		Parameter directory = new Parameter("File", files, ParameterType.SPINNER);
		params.add(directory);
		return params;
	}

	public static class GPSFileWrapperService extends WrapperService {

		public GPSFileWrapperService() {
			super("fileWrapperService");

		}
	}
}
