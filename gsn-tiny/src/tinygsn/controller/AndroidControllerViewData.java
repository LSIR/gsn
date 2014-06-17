package tinygsn.controller;

import java.util.ArrayList;
import tinygsn.beans.DataField;
import tinygsn.beans.StreamElement;
import tinygsn.gui.android.ActivityViewData;
import tinygsn.model.vsensor.VirtualSensor;
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
	private ArrayList<VirtualSensor> vsList = new ArrayList<VirtualSensor>();

	// private VSensorLoader vSensorLoader = null;

	private static final String TAG = "AndroidControllerViewData";

	public AndroidControllerViewData(ActivityViewData originalView) {
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