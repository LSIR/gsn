package tinygsn.controller;

import tinygsn.beans.StreamElement;
import tinygsn.beans.VSensorConfig;
import tinygsn.gui.android.ActivityAndroidViewer;
import tinygsn.storage.StorageManager;
import tinygsn.storage.db.SqliteStorageManager;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class AndroidControllerDemo extends AbstractController {

	private ActivityAndroidViewer view = null;
	private VSensorLoader vSensorLoader;
	private Handler handler = null;
	private SqliteStorageManager storage = null;

	private static final String TAG = "AndroidController";

	public AndroidControllerDemo(ActivityAndroidViewer androidViewer) {
		this.view = androidViewer;
		Log.v(TAG, "AndroidController");
		
		storage = new SqliteStorageManager(view);
	}

	public void consume(StreamElement streamElement) {
		// TODO Auto-generated method stub
		// view.showDataDemo(streamElement);
		Message msg = new Message();
		msg.obj = streamElement;
		handler.sendMessage(msg);
	}

	public void startLoadVSList() {
		loadVSConfigLastUsed();
	}

	public void loadVSConfigLastUsed() {
		vSensorLoader = new VSensorLoader(this);
		vSensorLoader.start();
	}

	public void loadVSConfig(VSensorConfig config) {
		vSensorLoader = new VSensorLoader(this);
		vSensorLoader.start();
	}

	public void tinygsnStop() {
		// TODO Auto-generated method stub

	}

	public Handler getHandler() {
		return handler;
	}

	public void setHandler(Handler handler) {
		this.handler = handler;
	}

	@Override
	public StorageManager getStorageManager() {
		return storage;
	}

	@Override
	public Activity getActivity() {
		// TODO Auto-generated method stub
		return null;
	}

}