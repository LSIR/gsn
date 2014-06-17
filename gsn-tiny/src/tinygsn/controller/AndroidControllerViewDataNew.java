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
import tinygsn.gui.android.ActivityViewDataNew;
import tinygsn.model.vsensor.VirtualSensor;
import tinygsn.storage.StorageManager;
import tinygsn.storage.db.SqliteStorageManager;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class AndroidControllerViewDataNew extends AbstractController {

	private ActivityViewDataNew view = null;
	private Handler handlerData = null;
	private Handler handlerVS = null;
	private Handler handlerField = null;
	private SqliteStorageManager storage = null;
	private ArrayList<VirtualSensor> vsList = new ArrayList<VirtualSensor>();

	// private VSensorLoader vSensorLoader = null;

	private static final String TAG = "AndroidControllerViewData";

	public AndroidControllerViewDataNew(ActivityViewDataNew originalView) {
		this.view = originalView;
		Log.v(TAG, "Start");

		storage = new SqliteStorageManager(view);
		// vSensorLoader = new VSensorLoader(this, storage);
	}

	public void startLoadVSList() {
		// vSensorLoader.start();
		// loadListVS();
	}

	public void loadListVS() {
		vsList = storage.getListofVS();
		ArrayList<String> vsListName = new ArrayList<String>();
		for (VirtualSensor vs : vsList) {
			vsListName.add(vs.getConfig().getName());
		}

		Message msg = new Message();
		msg.obj = vsListName;
		handlerVS.sendMessage(msg);
	}

	public void loadListFields(String vsName) {
		ArrayList<String> fieldList = new ArrayList<String>();
		DataField[] fields = null;
		for (VirtualSensor vs : vsList) {
			if (vs.getConfig().getName().endsWith(vsName)) {
				fields = vs.getConfig().getInputStreams()[0].getSources()[0]
						.getWrapper().getOutputStructure();
				break;
			}
		}
		for (DataField f : fields) {
			fieldList.add(f.getName());
			Log.v(TAG, f.getName());
		}

		Message msg = new Message();
		msg.obj = fieldList;
		handlerField.sendMessage(msg);
	}

	public void loadData(int numLatest, String vsName) {
		for (VirtualSensor vs : vsList) {
			if (vs.getConfig().getName().endsWith(vsName)) {
				String[] fieldList = vs.getConfig().getInputStreams()[0].getSources()[0]
						.getWrapper().getFieldList();
				Byte[] fieldType = vs.getConfig().getInputStreams()[0].getSources()[0]
						.getWrapper().getFieldType();

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
		for (VirtualSensor vs : vsList) {
			if (vs.getConfig().getName().endsWith(vsName)) {
				String[] fieldList = vs.getConfig().getInputStreams()[0].getSources()[0]
						.getWrapper().getFieldList();
				Byte[] fieldType = vs.getConfig().getInputStreams()[0].getSources()[0]
						.getWrapper().getFieldType();

				ArrayList<StreamElement> result = storage.executeQueryGetRangeData(
						"vs_" + vsName, start, end, fieldList, fieldType);

				Message msg = new Message();
				msg.obj = result;
				handlerData.sendMessage(msg);

				break;
			}
		}

	}

	public ArrayList<StreamElement> loadData2() {
		return storage.executeQuery();
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