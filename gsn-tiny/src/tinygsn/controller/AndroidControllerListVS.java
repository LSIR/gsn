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
import tinygsn.beans.StreamElement;
import tinygsn.gui.android.ActivityListVS;
import tinygsn.model.vsensor.AbstractVirtualSensor;
import tinygsn.storage.StorageManager;
import tinygsn.storage.db.SqliteStorageManager;
import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class AndroidControllerListVS extends AbstractController {

	/**
	 * 
	 */

	private Activity view = null;

	private Handler handlerVS = null;
	private SqliteStorageManager storage = null;
	
	private ArrayList<AbstractVirtualSensor> vsList = new ArrayList<AbstractVirtualSensor>();

	private static final String TAG = "AndroidControllerListVSNew";

	public AndroidControllerListVS(Activity androidViewer) {
		this.view = androidViewer;
		storage = new SqliteStorageManager(view);
		
		Log.v(TAG, "Construction.");
	}

//	public AndroidControllerListVSNew() {
//		ActivityListVS activity = new ActivityListVS();
//		this.view = activity;
//		Log.v(TAG, "Construction.");
//		// TODO Auto-generated constructor stub
//	}

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
		for (AbstractVirtualSensor vs : vsList) {
			vs.getConfig().setController(this);
//			vs.getConfig().VSNewController = this;
//			vsListName.add(vs.getConfig().getName());
		}

		Message msg = new Message();
		msg.obj = vsList;
		handlerVS.sendMessage(msg);
	}

	public boolean getRunningState(String vsName) {
		for (AbstractVirtualSensor vs : vsList) {
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
	
	public void startActiveVS() {
		SqliteStorageManager storage = new SqliteStorageManager(view);
		vsList = storage.getListofVS();
		for (AbstractVirtualSensor vs : vsList) {
			vs.getConfig().setController(this);
		}
		for (AbstractVirtualSensor vs : vsList) {
			if (vs.getConfig().getRunning() == true) {
				vs.start();
			}
		}
	}

	public void tinygsnStop() {
		for (AbstractVirtualSensor vs : vsList) {
			if (vs.getConfig().getRunning() == true) {
				//vs.stop();
				//TODO En jahaye baD call mishod felan cmt shode ta bad 
				
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

	public void startStopVS(String vsName, boolean running, Context context) {
		for (AbstractVirtualSensor vs : vsList) {
			if (vs.getConfig().getName().equals(vsName)) {
				AndroidControllerListVS controllerListVSNew  = new AndroidControllerListVS((ActivityListVS) this.getActivity());
				vs.getConfig().setController(controllerListVSNew);
				
				if (running == true) {
					vs.start();
					SqliteStorageManager storage = new SqliteStorageManager(view);
					storage.update("vsList", vsName, "running", "1");
				}
				else {
					SqliteStorageManager storage = new SqliteStorageManager(view);
					storage.update("vsList", vsName, "running", "0");
					vs.stop();
					//TODO check if this needs change or check why do we need new VS after stopping one
					//vs = new VirtualSensor(vs.getConfig(), context);
				}
				break;
			}
		}
	}


	public void deleteVS(String vsName) {
		for (AbstractVirtualSensor vs : vsList) {
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