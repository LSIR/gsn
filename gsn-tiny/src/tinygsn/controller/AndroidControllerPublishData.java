package tinygsn.controller;

import java.util.ArrayList;
import tinygsn.beans.StreamElement;
import tinygsn.gui.android.ActivityPublishData;
import tinygsn.model.vsensor.VirtualSensor;
import tinygsn.storage.StorageManager;
import tinygsn.storage.db.SqliteStorageManager;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;

public class AndroidControllerPublishData extends AbstractController {

	private ActivityPublishData view = null;

	private Handler handlerVS = null;

	private ArrayList<VirtualSensor> vsList = new ArrayList<VirtualSensor>();

	private static final String TAG = "AndroidControllerPublishData";
	private SqliteStorageManager storage = null;
	
	public AndroidControllerPublishData(ActivityPublishData androidViewer) {
		this.view = androidViewer;
		storage = new SqliteStorageManager(view);
	}

	public void loadListVS() {
		SqliteStorageManager storage = new SqliteStorageManager(view);
		vsList = storage.getListofVS();
		for (VirtualSensor vs : vsList) {
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
	
	@Override
	public void startLoadVSList() {
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
}