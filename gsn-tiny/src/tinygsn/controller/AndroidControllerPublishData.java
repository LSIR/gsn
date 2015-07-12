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

import tinygsn.beans.DataField;
import tinygsn.beans.StreamElement;
import tinygsn.gui.android.ActivityPublishData;
import tinygsn.model.vsensor.AbstractVirtualSensor;
import tinygsn.storage.StorageManager;
import tinygsn.storage.db.SqliteStorageManager;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;

public class AndroidControllerPublishData extends AbstractController {

	private ActivityPublishData view = null;

	private Handler handlerVS = null;

	private ArrayList<AbstractVirtualSensor> vsList = new ArrayList<AbstractVirtualSensor>();

	private SqliteStorageManager storage = null;
	
	public AndroidControllerPublishData(ActivityPublishData androidViewer) {
		this.view = androidViewer;
		storage = new SqliteStorageManager(view);
	}

	public void loadListVS() {
		SqliteStorageManager storage = new SqliteStorageManager(view);
		vsList = storage.getListofVS();
		for (AbstractVirtualSensor vs : vsList) {
			vs.getConfig().setController(this);
		}

		Message msg = new Message();
		msg.obj = vsList;
		handlerVS.sendMessage(msg);
	}

	public Handler getHandlerVS() {
		return handlerVS;
	}

	public void setHandlerVS(Handler handlerVS) {
		this.handlerVS = handlerVS;
	}
	
	public StreamElement loadLatestData(String vsName) {
		return loadLatestData(1, vsName);
	}
	
	public StreamElement loadLatestData(int numLatest, String vsName) {
		StreamElement latest = null;
		
		for (AbstractVirtualSensor vs : vsList) {
			if (vs.getConfig().getName().endsWith(vsName)) {
				DataField[] df = vs.getConfig().getOutputStructure();

				String[] fieldList = new String[df.length];
				Byte[] fieldType = new Byte[df.length];
				for(int i=0;i<df.length;i++){
					fieldList[i] = df[i].getName();
					fieldType[i] = df[i].getDataTypeID();
				}
				ArrayList<StreamElement> result = storage.executeQueryGetLatestValues(
						"vs_" + vsName, fieldList, fieldType, numLatest);

				if ((result != null) && (result.size() != 0))
					latest = result.get(0);
				else 
					return null;
				
				break;
			}
		}
		
		return latest;
	}
	
	public void consume(StreamElement streamElement) {
	}

	@Override
	public Activity getActivity() {
		return view;
	}

	@Override
	public StorageManager getStorageManager() {
		return null;
	}

	public StreamElement[] loadRangeData(String vsName, String fromdate,
			String fromtime, String todate, String totime) {
		StreamElement[] ret = new StreamElement[0];
		try{
			long start = new SimpleDateFormat("dd.MM.yyyy HH:mm").parse(fromdate + " " + fromtime).getTime();
			long end = new SimpleDateFormat("dd.MM.yyyy HH:mm").parse(todate + " " + totime).getTime();
		
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
		}catch(ParseException p){}
		return ret;
	}
}