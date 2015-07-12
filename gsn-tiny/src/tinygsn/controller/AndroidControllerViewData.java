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
* File: gsn-tiny/src/tinygsn/controller/AndroidControllerViewDataNew.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.controller;

import java.util.ArrayList;

import tinygsn.beans.DataField;
import tinygsn.beans.StreamElement;
import tinygsn.gui.android.ActivityViewData;
import tinygsn.model.vsensor.AbstractVirtualSensor;
import tinygsn.storage.StorageManager;
import tinygsn.storage.db.SqliteStorageManager;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class AndroidControllerViewData extends AbstractController {

	private ActivityViewData view = null;
	private Handler handlerData = null;
	private Handler handlerVS = null;
	private Handler handlerField = null;
	private SqliteStorageManager storage = null;
	private ArrayList<AbstractVirtualSensor> vsList = new ArrayList<AbstractVirtualSensor>();

	// private VSensorLoader vSensorLoader = null;

	private static final String TAG = "AndroidControllerViewData";

	public AndroidControllerViewData(ActivityViewData originalView) {
		this.view = originalView;
		Log.v(TAG, "Start");
		storage = new SqliteStorageManager(view);
	}

	public void loadListVS() {
		vsList = storage.getListofVS();
		ArrayList<String> vsListName = new ArrayList<String>();
		for (AbstractVirtualSensor vs : vsList) {
			vsListName.add(vs.getConfig().getName());
		}
		Message msg = new Message();
		msg.obj = vsListName;
		handlerVS.sendMessage(msg);
	}

	public void loadListFields(String vsName) {
		ArrayList<String> fieldList = new ArrayList<String>();
		DataField[] fields = null;
		for (AbstractVirtualSensor vs : vsList) {
			if (vs.getConfig().getName().endsWith(vsName)) {
				fields = vs.getConfig().getOutputStructure();
				break;
			}
		}
		for (DataField f : fields) {
			fieldList.add(f.getName());
		}
		Message msg = new Message();
		msg.obj = fieldList;
		handlerField.sendMessage(msg);
	}

	public void loadData(int numLatest, String vsName) {
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

				Message msg = new Message();
				msg.obj = result;
				handlerData.sendMessage(msg);

				break;
			}
		}
	}

	public void loadRangeData(String vsName, long start, long end) {
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
						"vs_" + vsName, start, end, fieldList, fieldType);

				Message msg = new Message();
				msg.obj = result;
				handlerData.sendMessage(msg);

				break;
			}
		}

	}

	public StorageManager getStorageManager() {
		return storage;
	}

	@Override
	public void consume(StreamElement streamElement) {

	}

	public Handler getHandlerData() {
		return handlerData;
	}

	public void setHandlerData(Handler handlerData) {
		this.handlerData = handlerData;
	}

	public Handler getHandlerVS() {
		return handlerVS;
	}

	public void setHandlerVS(Handler handlerVS) {
		this.handlerVS = handlerVS;
	}

	public Handler getHandlerField() {
		return handlerField;
	}

	public void setHandlerField(Handler handlerField) {
		this.handlerField = handlerField;
	}

	@Override
	public Activity getActivity() {
		return view;
	}

}