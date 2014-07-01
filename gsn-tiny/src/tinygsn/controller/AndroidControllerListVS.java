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
* File: gsn-tiny/src/tinygsn/controller/AndroidControllerListVS.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.controller;

import java.util.ArrayList;
import tinygsn.beans.StreamElement;
import tinygsn.gui.android.ActivityListVS;
import tinygsn.model.vsensor.VirtualSensor;
import tinygsn.storage.StorageManager;
import tinygsn.storage.db.SqliteStorageManager;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class AndroidControllerListVS extends AbstractController {

	private ActivityListVS view = null;

	private Handler handlerVS = null;

	private ArrayList<VirtualSensor> vsList = new ArrayList<VirtualSensor>();

	private static final String TAG = "AndroidControllerListVS";

	public AndroidControllerListVS(ActivityListVS androidViewer) {
		this.view = androidViewer;
		Log.v(TAG, "Construction.");
	}

	public void consume(StreamElement streamElement) {
		// view.showDataDemo(streamElement);
		// Message msg = new Message();
		// msg.obj = streamElement;
		// handlerData.sendMessage(msg);
	}

	public void loadListVSName() {
		SqliteStorageManager storage = new SqliteStorageManager(view);
		vsList = storage.getListofVS();
		ArrayList<String> vsListName = new ArrayList<String>();
		for (VirtualSensor vs : vsList) {
			vs.getConfig().setController(this);
			vsListName.add(vs.getConfig().getName());
		}

		Message msg = new Message();
		msg.obj = vsListName;
		handlerVS.sendMessage(msg);
	}

	public boolean getRunningState(String vsName) {
		for (VirtualSensor vs : vsList) {
			if (vs.getConfig().getName().equals(vsName))
				return vs.getConfig().getRunning();
		}
		return false;
	}

	public void startActiveVS() {
		for (VirtualSensor vs : vsList) {
			if (vs.getConfig().getRunning() == true) {
				vs.start();
			}
		}
	}

	public void tinygsnStop() {

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