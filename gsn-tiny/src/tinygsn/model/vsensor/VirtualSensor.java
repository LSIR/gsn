package tinygsn.model.vsensor;

import tinygsn.beans.InputStream;
import tinygsn.beans.StreamElement;
import tinygsn.beans.StreamSource;
import tinygsn.beans.VSensorConfig;
import tinygsn.model.wrappers.AbstractWrapper;
import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

public class VirtualSensor {

	private AbstractVirtualSensor virtualSensor = null;

	private VSensorConfig config = null;

	public VSensorConfig getConfig() {
		return config;
	}

	public void setConfig(VSensorConfig config) {
		this.config = config;
	}

	private static final String TAG = "VirtualSensor";

	public VirtualSensor(VSensorConfig originalConfig) {
		this.config = originalConfig;

		for (InputStream is : config.getInputStreams()) {
			is.setPool(this);
		}

		try {
			virtualSensor = (AbstractVirtualSensor) Class.forName(
					config.getProcessingClassName()).newInstance();
			virtualSensor.setVirtualSensorConfiguration(originalConfig);
			
//			if (config.getProcessingClassName().equals(AbstractVirtualSensor.PROCESSING_CLASS_NOTIFICATION)){
//				
//			}
		}
		catch (InstantiationException e) {
			e.printStackTrace();
		}
		catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void start() {
		Log.v(TAG, "Starts VS: " + config.toString());

		config.setRunning(true);
		
		for (InputStream inputStream : config.getInputStreams()) {
			for (StreamSource streamSource : inputStream.getSources()) {
				AbstractWrapper w = streamSource.getWrapper();
				Log.v(TAG, w.toString());
				w.start();
			}
		}
		Log.v(TAG, config.toString() + " started.");
	}

	public void stop() {
		config.setRunning(false);
		
		for (InputStream inputStream : config.getInputStreams()) {
			for (StreamSource streamSource : inputStream.getSources()) {
				streamSource.getWrapper().releaseResources();
			}
		}
		
		config = config.clone();
		Log.v(TAG, "VS: " + config.toString() + " stopped.");
	}

	public void dataAvailable(StreamElement se) {
		virtualSensor.dataAvailable(null, se);
		Activity a = getConfig().getController().getActivity();
//		showLog(a, "VirtualSensor: " + se.toString());
	}

//	void showLog(final Activity a, final String text) {
//		a.runOnUiThread(new Runnable() {
//			public void run() {
//				Toast.makeText(a, text, Toast.LENGTH_SHORT).show();
//			}
//		});
//	}
	
	//
	// public synchronized AbstractVirtualSensor borrowVS() throws
	// VirtualSensorInitializationFailedException {
	// if (virtualSensor == null) {
	// try {
	// virtualSensor = (AbstractVirtualSensor)
	// Class.forName(config.getProcessingClass()).newInstance();
	// virtualSensor.setVirtualSensorConfiguration(config);
	// } catch (Exception e) {
	// throw new VirtualSensorInitializationFailedException(e.getMessage(), e);
	// }
	// if (virtualSensor.initialize() == false) {
	// virtualSensor = null;
	// throw new VirtualSensorInitializationFailedException();
	// }
	// if (logger.isDebugEnabled())
	// logger.debug(new
	// StringBuilder().append("Created a new instance for VS ").append(config.getName()));
	// }
	// return virtualSensor;
	// }
	//
	// /**
	// * The method ignores the call if the input is null
	// *
	// * @param o
	// */
	// public synchronized void returnVS(AbstractVirtualSensor o) {
	// if (o == null) return;
	// if (++noOfCallsToReturnVS % GARBAGE_COLLECTOR_INTERVAL == 0)
	// DoUselessDataRemoval();
	// }
	//
	// public synchronized void closePool() {
	// if (virtualSensor != null) {
	// virtualSensor.dispose();
	// if (logger.isDebugEnabled())
	// logger.debug(new
	// StringBuilder().append("VS ").append(config.getName()).append(" is now released."));
	// } else if (logger.isDebugEnabled())
	// logger.debug(new
	// StringBuilder().append("VS ").append(config.getName()).append(" was already released."));
	// }

	//
	// /**
	// * @return the config
	// */
	// public VSensorConfig getConfig() {
	// return config;
	// }
	//
	// /**
	// * @return the lastModified
	// */
	// public long getLastModified() {
	// return lastModified;
	// }
	//
	// public void dispose() {
	// }
	//
	// // apply the storage size parameter to the virtual sensor table
	// public void DoUselessDataRemoval() {
	// if (config.getParsedStorageSize() == VSensorConfig.STORAGE_SIZE_NOT_SET)
	// return;
	// StringBuilder query;
	//
	// if (config.isStorageCountBased()) {
	// query =
	// Main.getStorage(config.getName()).getStatementRemoveUselessDataCountBased(config.getName(),
	// config.getParsedStorageSize());
	// }
	// else {
	// query =
	// Main.getStorage(config.getName()).getStatementRemoveUselessDataTimeBased(config.getName(),
	// config.getParsedStorageSize());
	// }
	//
	// int effected = 0;
	// try {
	// if (logger.isDebugEnabled())
	// logger.debug(new
	// StringBuilder().append("Enforcing the limit size on the VS table by : ").append(query).toString());
	// effected = Main.getStorage(config.getName()).executeUpdate(query);
	// } catch (SQLException e) {
	// logger.error("Error in executing: " + query);
	// logger.error(e.getMessage(), e);
	// }
	// if (logger.isDebugEnabled())
	// logger.debug(new
	// StringBuilder().append(effected).append(" old rows dropped from ").append(config.getName()).toString());
	// }

}
