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
* File: gsn-tiny/src/tinygsn/controller/VSensorLoader.java
*
* @author Do Ngoc Hoan
*/


//TODO vsconfig darim vali meghdar dehi nemishe 

package tinygsn.controller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import tinygsn.beans.VSensorConfig;
import tinygsn.gui.android.ActivityHome;
import tinygsn.model.vsensor.VirtualSensor;
import tinygsn.storage.db.SqliteStorageManager;
import android.content.Context;
import android.util.Log;

public class VSensorLoader extends Thread {

	private ArrayList<VirtualSensor> vsList = new ArrayList<VirtualSensor>();

	private SqliteStorageManager storage;
	private static final String TAG = "VSensorLoader";
	private AbstractController controller = null;
	private VSensorConfig config = null;
	private VirtualSensor vs = null;
	Context context = null;
	
	// public static final String VSENSOR_POOL = "VSENSOR-POOL";
	// public static final String STREAM_SOURCE = "STREAM-SOURCE";
	// public static final String INPUT_STREAM = "INPUT-STREAM";
	// private final List<AbstractWrapper> activeWrappers = new
	// ArrayList<AbstractWrapper>();
	// private StorageManager sm = StorageManager.getInstance ( );
	// private String pluginsDir;
	// private boolean isActive = true;
	// private static int VSENSOR_LOADER_THREAD_COUNTER = 0;
	// private static VSensorLoader singleton = null;
	// private ArrayList<VSensorStateChangeListener> changeListeners = new
	// ArrayList<VSensorStateChangeListener>();

	public VSensorLoader(AbstractController androidController, Context context) {
		this.controller = androidController;
		this.context = context;
	}

	public VSensorLoader(AbstractController androidControllerListVS,
			SqliteStorageManager storage, Context context) {
		this.controller = androidControllerListVS;
		this.storage = storage;
		this.context = context;
	}

	public void run() {
//		 runDemo();
//		 getListofVS();
		// createVSTable();
//		insertNewVS();
	}

	private ArrayList<VirtualSensor> getListofVS() {
		vsList = controller.getStorageManager().getListofVS();
		for (VirtualSensor vs:vsList){
			Log.v(TAG, vs.getConfig().toString());
		}
		return vsList;
	}

	public ArrayList<VirtualSensor> getVsList() {
		return vsList;
	}

	public void setVsList(ArrayList<VirtualSensor> vsList) {
		this.vsList = vsList;
	}
	
	public void startAllActiveVS(){
		for (VirtualSensor vs: vsList){
			if (vs.getConfig().getRunning() == true)
				vs.start();
		}
	}
/*	
	private void runDemo() {
		// config = new VSensorConfig(VSensorConfig.PROCESSING_CLASS_BRIDGE, "gps",
		// "tinygsn.wrappers.AndroidFakeGPSWrapper", 200, 1, 1, controller);
//		config = new VSensorConfig(VSensorConfig.PROCESSING_CLASS_BRIDGE, "temp",
//				"tinygsn.wrappers.AndroidFakeTemperatureWrapper", 200, 1, 1, 0, true);
		config.setController(controller);

		vs = new VirtualSensor(config, context);
		vs.start();
	}

	private void createVSTable() {
		ArrayList<String> fields = new ArrayList<String>();
		fields.addAll(Arrays.asList("running", "vsname", "vstype", "sswindowsize",
				"ssstep", "sssamplingrate", "ssaggregator", "wrappername"));
		controller.getStorageManager().createTable("vsList", fields);
		Log.v(TAG, "OK");
	}

	
	//Is this code usefull??
	private void insertNewVS() {
		try {
			controller.getStorageManager().executeInsert(
					"vsList",
					new ArrayList<String>(Arrays.asList("running", "vsname", "vstype",
							"sswindowsize", "ssstep", "sssamplingrate", "wrappername")),
					new ArrayList<String>(Arrays.asList("0", "gps1", "1",
							"10", "2", "700", "tinygsn.wrappers.AndroidFakeGPSWrapper")));
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}
*/	
	public void unloadVirtualSensor(VirtualSensor vs) {
		if (vs != null)
			vs.stop();
	}

}
