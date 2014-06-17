package tinygsn.controller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import tinygsn.beans.VSensorConfig;
import tinygsn.model.vsensor.VirtualSensor;
import tinygsn.storage.db.SqliteStorageManager;
import android.util.Log;

public class VSensorLoader extends Thread {

	private ArrayList<VirtualSensor> vsList = new ArrayList<VirtualSensor>();

	private SqliteStorageManager storage;
	private static final String TAG = "VSensorLoader";
	private AbstractController controller = null;
	private VSensorConfig config = null;
	private VirtualSensor vs = null;

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

	public VSensorLoader(AbstractController androidController) {
		this.controller = androidController;
	}

	public VSensorLoader(AbstractController androidControllerListVS,
			SqliteStorageManager storage) {
		this.controller = androidControllerListVS;
		this.storage = storage;
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
	
	private void runDemo() {
		// config = new VSensorConfig(VSensorConfig.PROCESSING_CLASS_BRIDGE, "gps",
		// "tinygsn.wrappers.AndroidFakeGPSWrapper", 200, 1, 1, controller);
//		config = new VSensorConfig(VSensorConfig.PROCESSING_CLASS_BRIDGE, "temp",
//				"tinygsn.wrappers.AndroidFakeTemperatureWrapper", 200, 1, 1, 0, true);
		config.setController(controller);

		vs = new VirtualSensor(config);
		vs.start();
	}

	private void createVSTable() {
		ArrayList<String> fields = new ArrayList<String>();
		fields.addAll(Arrays.asList("running", "vsname", "vstype", "sswindowsize",
				"ssstep", "sssamplingrate", "ssaggregator", "wrappername"));
		controller.getStorageManager().createTable("vsList", fields);
		Log.v(TAG, "OK");
	}

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
	
	public void unloadVirtualSensor(VirtualSensor vs) {
		if (vs != null)
			vs.stop();
	}

}
