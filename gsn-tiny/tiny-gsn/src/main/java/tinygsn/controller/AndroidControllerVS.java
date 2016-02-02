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
* File: gsn-tiny/src/tinygsn/controller/AndroidControllerListVSNew.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.controller;


import java.util.ArrayList;

import tinygsn.beans.DataField;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.model.vsensor.AbstractVirtualSensor;
import tinygsn.storage.db.SqliteStorageManager;
import android.util.Log;

public class AndroidControllerVS extends AbstractController {


	private SqliteStorageManager storage = new SqliteStorageManager();;
	private ArrayList<AbstractVirtualSensor> vsList = new ArrayList<AbstractVirtualSensor>();
	private static final String TAG = "AndroidControllerListVSNew";

	public AndroidControllerVS() {
		Log.v(TAG, "Construction.");
	}

	public ArrayList<AbstractVirtualSensor> loadListVS() {
		vsList = storage.getListofVS();
		return vsList;
	}

	public StreamElement loadLatestData(String vsName) {
		ArrayList<StreamElement> result = loadLatestData(1, vsName);
		if ((result != null) && (result.size() != 0))
			return result.get(0);
		return null;
	}

	public ArrayList<StreamElement> loadLatestData(int numLatest, String vsName) {
		ArrayList<StreamElement> latest = null;
		AbstractVirtualSensor vs = StaticData.getProcessingClassByName(vsName);
		if (vs != null){
			DataField[] df = vs.getConfig().getOutputStructure();
			if (df != null) {
				String[] fieldList = new String[df.length];
				Byte[] fieldType = new Byte[df.length];
				for (int i = 0; i < df.length; i++) {
					fieldList[i] = df[i].getName();
					fieldType[i] = df[i].getDataTypeID();
				}
				latest = storage.executeQueryGetLatestValues("vs_" + vsName, fieldList, fieldType, numLatest);
			}
		}
		return latest;
	}

	public ArrayList<StreamElement> loadRangeData(String vsName, long start, long end) {
		ArrayList<StreamElement> result = null;
		AbstractVirtualSensor vs = StaticData.getProcessingClassByName(vsName);
		if (vs != null) {
			DataField[] df = vs.getConfig().getOutputStructure();
			String[] fieldList = new String[df.length];
			Byte[] fieldType = new Byte[df.length];
			for(int i=0;i<df.length;i++){
				fieldList[i] = df[i].getName();
				fieldType[i] = df[i].getDataTypeID();
			}
			result = storage.executeQueryGetRangeData("vs_" + vsName, start, end, fieldList, fieldType);
		}
		return result;
	}

	public ArrayList<String> loadListFields(String vsName) {
		ArrayList<String> fieldList = new ArrayList<String>();
		DataField[] fields = null;
		AbstractVirtualSensor vs = StaticData.getProcessingClassByName(vsName);
		if (vs != null) {
			fields = vs.getConfig().getOutputStructure();
		}
		if (fields != null) {
			for (DataField f : fields) {
				fieldList.add(f.getName());
			}
		}
		return fieldList;
	}


	public void startStopVS(String vsName, boolean running) {
		AbstractVirtualSensor vs = StaticData.getProcessingClassByName(vsName);
		if (vs != null){
			if (running == true) {
				vs.start();
				storage.update("vsList", vsName, "running", "1");
			}
			else {
				storage.update("vsList", vsName, "running", "0");
				vs.stop();
			}
		}
	}


	public void deleteVS(String vsName) {
		AbstractVirtualSensor vs = StaticData.getProcessingClassByName(vsName);
		if (vs != null){
			vs.delete();
			storage.deleteVS(vsName);
			storage.deleteSS(vsName);
			storage.deleteTable("vs_" + vsName);
		}
	}
}