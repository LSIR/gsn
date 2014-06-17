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