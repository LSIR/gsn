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
import tinygsn.beans.StreamElement;
import tinygsn.gui.android.ActivityListVSNew;
import tinygsn.model.vsensor.VirtualSensor;
import tinygsn.storage.StorageManager;
import tinygsn.storage.db.SqliteStorageManager;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class AndroidControllerListVSNew extends AbstractController {

	private ActivityListVSNew view = null;

	private Handler handlerVS = null;
	private Handler handlerData = null;
	private SqliteStorageManager storage = null;
	
	private ArrayList<VirtualSensor> vsList = new ArrayList<VirtualSensor>();

	private static final String TAG = "AndroidControllerListVSNew";

	public AndroidControllerListVSNew(ActivityListVSNew androidViewer) {
		this.view = androidViewer;
		storage = new SqliteStorageManager(view);
		
		Log.v(TAG, "Construction.");
	}

	public void consume(StreamElement streamElement) {
		// view.showDataDemo(streamElement);
		// Message msg = new Message();
		// msg.obj = streamElement;
		// handlerData.sendMessage(msg);
	}

	public void loadListVS() {
		SqliteStorageManager storage = new SqliteStorageManager(view);
		vsList = storage.getListofVS();
//		ArrayList<String> vsListName = new ArrayList<String>();
		for (VirtualSensor vs : vsList) {
			vs.getConfig().setController(this);
//			vsListName.add(vs.getConfig().getName());
		}

		Message msg = new Message();
		msg.obj = vsList;
		handlerVS.sendMessage(msg);
	}

	public boolean getRunningState(String vsName) {
		for (VirtualSensor vs : vsList) {
			if (vs.getConfig().getName().equals(vsName))
				return vs.getConfig().getRunning();
		}
		return false;
	}

	public StreamElement loadLatestData(String vsName) {
		return loadLatestData(1, vsName);
	}
	
	public StreamElement loadLatestData(int numLatest, String vsName) {
		StreamElement latest = null;
		
		for (VirtualSensor vs : vsList) {
			if (vs.getConfig().getName().endsWith(vsName)) {
				String[] fieldList = vs.getConfig().getInputStreams()[0].getSources()[0]
						.getWrapper().getFieldList();
				Byte[] fieldType = vs.getConfig().getInputStreams()[0].getSources()[0]
						.getWrapper().getFieldType();

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
	
	public void startActiveVS() {
		for (VirtualSensor vs : vsList) {
			if (vs.getConfig().getRunning() == true) {
				vs.start();
			}
		}
	}

	public void tinygsnStop() {
		for (VirtualSensor vs : vsList) {
			if (vs.getConfig().getRunning() == true) {
				vs.stop();
			}
		}
	}

	public StorageManager getStorageManager() {
		SqliteStorageManager storage = new SqliteStorageManager(view);
		return storage;
	}

	public Handler getHandlerVS() {
		return handlerVS;
	}

	public void setHandlerVS(Handler handlerVS) {
		this.handlerVS = handlerVS;
	}

	public void startStopVS(String vsName, boolean running) {
		for (VirtualSensor vs : vsList) {
			if (vs.getConfig().getName().equals(vsName)) {
				if (running == true) {
					vs.start();
					SqliteStorageManager storage = new SqliteStorageManager(view);
					storage.update("vsList", vsName, "running", "1");
				}
				else {
					SqliteStorageManager storage = new SqliteStorageManager(view);
					storage.update("vsList", vsName, "running", "0");
					vs.stop();
					vs = new VirtualSensor(vs.getConfig());
				}
				break;
			}
		}
	}

	@Override
	public void startLoadVSList() {
		
	}

	public void deleteVS(String vsName) {
		for (VirtualSensor vs : vsList) {
			if (vs.getConfig().getName().equals(vsName)) {
				vs.stop();
				SqliteStorageManager storage = new SqliteStorageManager(view);
				storage.deleteVS(vsName);
				storage.deleteTable("vs_" + vsName);
				break;
			}
		}
	}

	@Override
	public Activity getActivity() {
		return view;
	}
}