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
* File: gsn-tiny/src/tinygsn/controller/AndroidControllerPublishData.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import tinygsn.beans.DataField;
import tinygsn.beans.DeliveryRequest;
import tinygsn.beans.StreamElement;
import tinygsn.model.vsensor.AbstractVirtualSensor;
import tinygsn.storage.db.SqliteStorageManager;


public class AndroidControllerPublish extends AbstractController {

	private ArrayList<AbstractVirtualSensor> vsList = null;

	private SqliteStorageManager storage = null;
	
	public AndroidControllerPublish() {
		storage = new SqliteStorageManager();
	}

	public ArrayList<String> loadListVS() {
		if (vsList==null) vsList = storage.getListofVS();
		return storage.getListofVSName();
	}

	public StreamElement[] loadRangeData(String vsName, long start, long end) {
		StreamElement[] ret = new StreamElement[0];
		if (vsList==null) vsList = storage.getListofVS();

		for (AbstractVirtualSensor vs : vsList) {
			if (vs.getConfig().getName().endsWith(vsName)) {
				DataField[] df = vs.getConfig().getOutputStructure();

				String[] fieldList = new String[df.length];
				Byte[] fieldType = new Byte[df.length];
				for(int i=0;i<df.length;i++){
					fieldList[i] = df[i].getName();
					fieldType[i] = df[i].getDataTypeID();
				}

				ArrayList<StreamElement> result = storage.executeQueryGetRangeData(
						"vs_" + vsName, start,end ,fieldList, fieldType);

				if ((result != null) && (result.size() != 0))
					ret = result.toArray(ret);
				break;
			}
		}
		return ret;
	}

	public ArrayList<DeliveryRequest> loadList() {
		ArrayList<DeliveryRequest> a = storage.getPublishList();
		return a;
	}
}