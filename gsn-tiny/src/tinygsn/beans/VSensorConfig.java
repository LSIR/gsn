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
* File: gsn-tiny/src/tinygsn/beans/VSensorConfig.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.beans;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.TimeZone;
import tinygsn.controller.AbstractController;
import tinygsn.controller.AndroidControllerListVSNew;
import tinygsn.gui.android.ActivityListVSNew;
import tinygsn.model.wrappers.AbstractWrapper;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;


public class VSensorConfig implements Parcelable  {

	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}

	private int id;
	private String name;
	private AbstractController controller = null;
	private static final String TAG = "VSensorConfig";
	private InputStream inputStreams[] = null;
	private DataField[] outputStructure;
	private String processingClassName;
	private boolean running;
	private String wrapperName;
	private String notify_field, notify_condition, notify_action, notify_contact;

	private Double notify_value;
	private boolean save_to_db;
	
	
	public ActivityListVSNew view = null;
	//public AndroidControllerListVSNew VSNewController;
	// ========================================================
	public static final int DEFAULT_PRIORITY = 100;
	public static final int NO_FIXED_RATE = 0;
	public static final int DEFAULT_POOL_SIZE = 10;
	private int priority = DEFAULT_PRIORITY;
	private String description;
	private int outputStreamRate;
	// private KeyValue[] addressing;
	private String storageHistorySize = null;
	private final HashMap<String, InputStream> inputStreamNameToInputStreamObjectMapping = new HashMap<String, InputStream>();
	// private ArrayList<KeyValue> mainClassInitialParams = new
	// ArrayList<KeyValue>();
	private transient Long lastModified;
	private String fileName;
	private StorageConfig storage;
	private String timeZone;
	private SimpleDateFormat sdf = null;
	private String directoryQuery;
	private String access_protected = "false";

	public VSensorConfig() {

	}
	public VSensorConfig(Parcel source)
	{
		int idid = Integer.parseInt(source.readString());
		String processingClass = source.readString();
		String vsName = source.readString();
		String wrapperN = source.readString();
		int samplingRate = Integer.parseInt(source.readString());
		int windowSize = Integer.parseInt(source.readString());
		int step = Integer.parseInt(source.readString());
		boolean timeBased = Boolean.parseBoolean(source.readString());
		int aggregator = Integer.parseInt(source.readString());
		boolean runningState = Boolean.parseBoolean(source.readString());
		String notify_field_par = source.readString();
		String notify_condition_par = source.readString();
		Double notify_value_par = Double.parseDouble(source.readString());
		String notify_action_par = source.readString();
		String notify_contact_par = source.readString();
		boolean save_to_db_par = Boolean.parseBoolean(source.readString());
		
		setController(StaticData.findController(idid));
		
		
		
		this.id = idid;
		this.name = vsName;
		this.processingClassName = processingClass;
		this.running = runningState;

		// notify_field, notify_condition, notify_value,
		// notify_action, notify_contact, save_to_db
		this.notify_field = notify_field_par;
		this.notify_condition = notify_condition_par;
		this.notify_value = notify_value_par;
		this.notify_action = notify_action_par;
		this.notify_contact = notify_contact_par;
		this.save_to_db = save_to_db_par;

		Queue queue = new Queue(windowSize, step, timeBased);
		StreamSource s = new StreamSource(queue);

		AbstractWrapper w;

		try {
			w = (AbstractWrapper) Class.forName(wrapperN).newInstance();
			w.setQueue(s.getQueue());
			w.setConfig(this);
			w.setSamplingRate(samplingRate);
			
			s.setWrapper(w);
			s.setAggregator(aggregator);
			s.setSamplingRate(samplingRate);
			outputStructure = w.getOutputStructure();
		}
		catch (InstantiationException e1) {
			e1.printStackTrace();
			Log.e(TAG, "Error: " + e1.getMessage());
		}
		catch (IllegalAccessException e1) {
			e1.printStackTrace();
			Log.e(TAG, "Error: " + e1.getMessage());
		}
		catch (ClassNotFoundException e1) {
			e1.printStackTrace();
			Log.e(TAG, "Error: " + e1.getMessage());
		}
		catch (SQLException e) {
			e.printStackTrace();
			Log.e(TAG, "Error: " + e.getMessage());
		}

		InputStream is = new InputStream(s);
		s.setInputStream(is);

		this.inputStreams = new InputStream[1];
		this.inputStreams[0] = is;
		this.wrapperName = wrapperN;
			
	}

	public VSensorConfig(int id, String processingClass, String vsName,
			String wrapperName, int samplingRate, int windowSize, int step, boolean timeBased,
			int aggregator, boolean running, String notify_field,
			String notify_condition, Double notify_value, String notify_action,
			String notify_contact, boolean save_to_db) {

		this.id = id;
		this.name = vsName;
		this.processingClassName = processingClass;
		this.running = running;

		// notify_field, notify_condition, notify_value,
		// notify_action, notify_contact, save_to_db
		this.notify_field = notify_field;
		this.notify_condition = notify_condition;
		this.notify_value = notify_value;
		this.notify_action = notify_action;
		this.notify_contact = notify_contact;
		this.save_to_db = save_to_db;

		Queue queue = new Queue(windowSize, step, timeBased);
		StreamSource s = new StreamSource(queue);

		AbstractWrapper w;

		try {
			w = (AbstractWrapper) Class.forName(wrapperName).newInstance();
			w.setQueue(s.getQueue());
			w.setConfig(this);
			w.setSamplingRate(samplingRate);
			
			s.setWrapper(w);
			s.setAggregator(aggregator);
			s.setSamplingRate(samplingRate);
			outputStructure = w.getOutputStructure();
		}
		catch (InstantiationException e1) {
			e1.printStackTrace();
			Log.e(TAG, "Error: " + e1.getMessage());
		}
		catch (IllegalAccessException e1) {
			e1.printStackTrace();
			Log.e(TAG, "Error: " + e1.getMessage());
		}
		catch (ClassNotFoundException e1) {
			e1.printStackTrace();
			Log.e(TAG, "Error: " + e1.getMessage());
		}
		catch (SQLException e) {
			e.printStackTrace();
			Log.e(TAG, "Error: " + e.getMessage());
		}

		InputStream is = new InputStream(s);
		s.setInputStream(is);

		this.inputStreams = new InputStream[1];
		this.inputStreams[0] = is;
		this.wrapperName = wrapperName;

	}

	// public void createTableIfNotExist(){
	// controller.getStorageManager().createTable(name, outputStructure);
	// }
	// /(String processingClass, String vsName,
	// String wrapperName, int samplingRate, int windowSize,
	// int step, int aggregator, boolean running) {
	// }

	public String getWrapperName() {
		return wrapperName;
	}
	public void setWrapperName(String wrapperName) {
		this.wrapperName = wrapperName;
	}
	public VSensorConfig clone() {
		VSensorConfig vsConfig = new VSensorConfig(id, processingClassName, name,
				wrapperName, getInputStreams()[0].getSources()[0].getSamplingRate(),
				getInputStreams()[0].getSources()[0].getQueue().getWindowSize(),
				getInputStreams()[0].getSources()[0].getQueue().getStep(),
				getInputStreams()[0].getSources()[0].getQueue().isTimeBased(),
				getInputStreams()[0].getSources()[0].getAggregator(), running,
				notify_field, notify_condition, notify_value, notify_action,
				notify_contact, save_to_db);
		vsConfig.setController(controller);
		
		StaticData.addConfig(id, vsConfig);
		StaticData.saveNameID(id, name);


		return vsConfig;
	}

	public String getProcessingClassName() {
		return processingClassName;
	}

	public void setProcessingClassName(String processingClassName) {
		this.processingClassName = processingClassName;
	}

	public boolean getRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	// /**
	// * @return Returns the addressing.
	// */
	// public KeyValue[] getAddressing() {
	// return this.addressing;
	// }

	/**
	 * @return Returns the description.
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * @return Returns the inputStreams.
	 */
	public InputStream[] getInputStreams() {
		return inputStreams;
		// return this.inputStreamNameToInputStreamObjectMapping.values();
	}

	public InputStream getInputStream(final String inputStreamName) {
		return this.inputStreamNameToInputStreamObjectMapping.get(inputStreamName);
	}

	/**
	 * The <code>nameInitialized</code> is used to cache the virtual sensor's name
	 * for preformance.
	 */
	private boolean nameInitialized = false;

	public String getName() {
		// if (this.nameInitialized == false) {
		// this.name = this.name.replace(" ", "").trim().toLowerCase();
		// this.nameInitialized = true;
		// }
		return this.name;
	}

	/**
	 * @return Returns the outputStreamRate.
	 */
	public int getOutputStreamRate() {
		return this.outputStreamRate;
	}

	/**
	 * @return Returns the outputStructure.
	 */
	public DataField[] getOutputStructure() {
		return this.outputStructure;
	}

	/**
	 * @return Returns the priority.
	 */
	public int getPriority() {
		return this.priority;
	}

	public Long getLastModified() {
		return this.lastModified;
	}

	/**
	 * @param addressing
	 *          The addressing to set.
	 */
	// public void setAddressing(KeyValue[] addressing) {
	// this.addressing = addressing;
	// }

	/**
	 * @param description
	 *          The description to set.
	 */
	public void setDescription(final String description) {
		this.description = description;
	}

	/**
	 * @param lastModified
	 *          The lastModified to set.
	 */
	public void setLastModified(final Long lastModified) {
		this.lastModified = lastModified;
	}

	/**
	 * @param virtualSensorName
	 *          The name to set.
	 */
	public void setName(final String virtualSensorName) {
		this.name = virtualSensorName;
	}

	/**
	 * @param outputStreamRate
	 *          The outputStreamRate to set.
	 */
	public void setOutputStreamRate(final int outputStreamRate) {
		this.outputStreamRate = outputStreamRate;
	}

	/**
	 * @param outputStructure
	 *          The outputStructure to set.
	 */
	public void setOutputStructure(DataField[] outputStructure) {
		this.outputStructure = outputStructure;
	}

	/**
	 * @param priority
	 *          The priority to set.
	 */
	public void setPriority(final int priority) {
		this.priority = priority;
	}

	// public String[] getAddressingKeys() {
	// final String result[] = new String[this.getAddressing().length];
	// int counter = 0;
	// for (final KeyValue predicate : this.getAddressing())
	// result[counter++] = (String) predicate.getKey();
	// return result;
	// }
	//
	// public String[] getAddressingValues() {
	// final String result[] = new String[this.getAddressing().length];
	// int counter = 0;
	// for (final KeyValue predicate : this.getAddressing())
	// result[counter++] = (String) predicate.getValue();
	// return result;
	// }

	private boolean isGetMainClassInitParamsInitialized = false;

	// private final TreeMap<String, String> mainClassInitParams = new
	// TreeMap<String, String>(
	// new CaseInsensitiveComparator());

	/**
	 * Note that the key and value both are trimmed before being inserted into the
	 * data strcture.
	 * 
	 * @return
	 */
	// public TreeMap<String, String> getMainClassInitialParams() {
	// if (!this.isGetMainClassInitParamsInitialized) {
	// this.isGetMainClassInitParamsInitialized = true;
	// for (final KeyValue param : this.mainClassInitialParams) {
	// this.mainClassInitParams.put(param.getKey().toString().toLowerCase(),
	// param.getValue().toString());
	// }
	// }
	// return this.mainClassInitParams;
	// }

	// public void setMainClassInitialParams(
	// final ArrayList<KeyValue> mainClassInitialParams) {
	// this.mainClassInitialParams = mainClassInitialParams;
	// }

	public String getFileName() {
		return this.fileName;
	}

	public void setFileName(final String fileName) {
		this.fileName = fileName;
	}

	private boolean isStorageCountBased = true;

	public static final int STORAGE_SIZE_NOT_SET = -1;

	private long parsedStorageSize = STORAGE_SIZE_NOT_SET;

	/**
	 * @return Returns the storageHistorySize.
	 */
	public String getStorageHistorySize() {
		if (storageHistorySize == null) {
			if (storage == null || storage.getStorageSize() == null
					|| storage.getStorageSize().trim().equals(""))
				storageHistorySize = "0";
			else
				storageHistorySize = storage.getStorageSize();
		}
		return storageHistorySize;
	}

	/**
	 * Checks whether the virtual sensor needs storage or not (checks the variable
	 * <code>storageHistorySize</code>
	 */
	public boolean needsStorage() {
		if (this.getStorageHistorySize().equals("0"))
			return false;
		return true;
	}

	public boolean validate() {
		String storageHistorySize = this.getStorageHistorySize();
		storageHistorySize = storageHistorySize.replace(" ", "").trim()
				.toLowerCase();
		for (final InputStream inputStream : this.inputStreams)
			this.inputStreamNameToInputStreamObjectMapping.put(
					inputStream.getInputStreamName(), inputStream);

		if (storageHistorySize.equalsIgnoreCase("0"))
			return true;
		final int second = 1000;
		final int minute = second * 60;
		final int hour = minute * 60;

		final int mIndex = storageHistorySize.indexOf("m");
		final int hIndex = storageHistorySize.indexOf("h");
		final int sIndex = storageHistorySize.indexOf("s");
		if (mIndex < 0 && hIndex < 0 && sIndex < 0) {
			try {
				this.parsedStorageSize = Integer.parseInt(storageHistorySize);
				this.isStorageCountBased = true;
			}
			catch (final NumberFormatException e) {
				// this.logger.error(
				// new StringBuilder().append("The storage size, ")
				// .append(storageHistorySize)
				// .append(", specified for the virtual sensor : ")
				// .append(this.name).append(" is not valid.").toString(), e);
				return false;
			}
		}
		else {
			try {
				final StringBuilder shs = new StringBuilder(storageHistorySize);
				if (mIndex >= 0 && mIndex == shs.length() - 1)
					this.parsedStorageSize = Integer.parseInt(shs.deleteCharAt(mIndex)
							.toString()) * minute;
				else if (hIndex >= 0 && hIndex == shs.length() - 1)
					this.parsedStorageSize = Integer.parseInt(shs.deleteCharAt(hIndex)
							.toString()) * hour;
				else if (sIndex >= 0 && sIndex == shs.length() - 1)
					this.parsedStorageSize = Integer.parseInt(shs.deleteCharAt(sIndex)
							.toString()) * second;
				else
					Integer.parseInt("");
				this.isStorageCountBased = false;
			}
			catch (final NumberFormatException e) {
				// this.logger.error(
				// new StringBuilder().append("The storage size, ")
				// .append(storageHistorySize)
				// .append(", specified for the virtual sensor : ")
				// .append(this.name).append(" is not valid.").toString(), e);
				return false;
			}
		}
		return true;
	}

	public StorageConfig getStorage() {
		return storage;
	}

	public boolean isStorageCountBased() {
		return this.isStorageCountBased;
	}

	public long getParsedStorageSize() {
		return this.parsedStorageSize;
	}

	public String getDirectoryQuery() {
		return directoryQuery;
	}

	public String toString() {
		return "VSensorConfig{" + "name='" + this.name + '\'' + ", mainClass='"
				+ this.processingClassName + '\'' + ", wrapperName='"
				+ this.wrapperName + '\'' + ", SamplingRate="
				+ this.getInputStreams()[0].getSources()[0].getSamplingRate()
				+ ", WindowSize="
				+ this.getInputStreams()[0].getSources()[0].getQueue().getWindowSize() + ", Step="
				+ this.getInputStreams()[0].getSources()[0].getQueue().getStep() + ", Running="
				+ this.getRunning();
	}

	public boolean equals(Object obj) {
		if (obj instanceof VSensorConfig) {
			VSensorConfig vSensorConfig = (VSensorConfig) obj;
			return name.equals(vSensorConfig.getName());
		}
		return false;
	}

	public int hashCode() {
		if (name != null) {
			return name.hashCode();
		}
		else {
			return super.hashCode();
		}
	}

	// time zone

	public SimpleDateFormat getSDF() {
		if (timeZone == null)
			return null;
		else {
			if (sdf == null) {
				// sdf = new
				// SimpleDateFormat(Main.getContainerConfig().getTimeFormat());
				sdf.setTimeZone(TimeZone.getTimeZone(timeZone));
			}
		}
		return sdf;
	}

	
	public void setInputStreams(InputStream... inputStreams) {
		this.inputStreams = inputStreams;
	}

	public void setStorageHistorySize(String storageHistorySize) {
		this.storageHistorySize = storageHistorySize;
	}

	/**
	 * Addressing Helper methods.
	 */
	private transient Double cached_altitude = null;
	private transient Double cached_longitude = null;
	private transient Double cached_latitude = null;
	private boolean addressing_processed = false;

	private boolean isTimestampUnique = false;

	public void preprocess_addressing() {
		// if (!addressing_processed) {
		// for (KeyValue kv : getAddressing())
		// if (kv.getKey().toString().equalsIgnoreCase("altitude"))
		// cached_altitude = Double.parseDouble(kv.getValue().toString());
		// else if (kv.getKey().toString().equalsIgnoreCase("longitude"))
		// cached_longitude = Double.parseDouble(kv.getValue().toString());
		// else if (kv.getKey().toString().equalsIgnoreCase("latitude"))
		// cached_latitude = Double.parseDouble(kv.getValue().toString());
		// addressing_processed = true;
		// }
	}

	public Double getAltitude() {
		preprocess_addressing();
		return cached_altitude;
	}

	public Double getLatitude() {
		preprocess_addressing();
		return cached_latitude;
	}

	public Double getLongitude() {
		preprocess_addressing();
		return cached_longitude;
	}

	public boolean getIsTimeStampUnique() {
		return isTimestampUnique;
	}

	public boolean isAccess_protected() {
		try {
			return Boolean.parseBoolean(access_protected.trim());
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public AbstractController getController() {
		return controller;
	}

	public void setController(AbstractController controller) {
		this.controller = controller;
		this.controller.setId(id);
		
	}

	public String getNotify_field() {
		return notify_field;
	}

	public void setNotify_field(String notify_field) {
		this.notify_field = notify_field;
	}

	public String getNotify_condition() {
		return notify_condition;
	}

	public void setNotify_condition(String notify_condition) {
		this.notify_condition = notify_condition;
	}

	public String getNotify_action() {
		return notify_action;
	}

	public void setNotify_action(String notify_action) {
		this.notify_action = notify_action;
	}

	public String getNotify_contact() {
		return notify_contact;
	}

	public void setNotify_contact(String notify_contact) {
		this.notify_contact = notify_contact;
	}

	public Double getNotify_value() {
		return notify_value;
	}

	public void setNotify_value(Double notify_value) {
		this.notify_value = notify_value;
	}

	public boolean isSave_to_db() {
		return save_to_db;
	}

	public void setSave_to_db(boolean save_to_db) {
		this.save_to_db = save_to_db;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(Integer.toString(id));
		dest.writeString(getProcessingClassName());
		dest.writeString(getName());
		dest.writeString(getWrapperName());
		dest.writeString(Integer.toString(getInputStreams()[0].getSources()[0].getSamplingRate()));
		dest.writeString(Integer.toString(getInputStreams()[0].getSources()[0].getQueue().getWindowSize()));
		dest.writeString(Integer.toString(getInputStreams()[0].getSources()[0].getQueue().getStep()));
		dest.writeString(Boolean.toString(getInputStreams()[0].getSources()[0].getQueue().isTimeBased()));
		dest.writeString(Integer.toString(getInputStreams()[0].getSources()[0].getAggregator()));
		dest.writeString(Boolean.toString(getRunning()));
		dest.writeString(getNotify_field());
		dest.writeString(getNotify_condition());
		dest.writeString(Double.toString(getNotify_value()));
		dest.writeString(getNotify_action());
		dest.writeString(getNotify_contact());
		dest.writeString(Boolean.toString(isSave_to_db()));
		StaticData.addController((AndroidControllerListVSNew) getController());
		StaticData.is = getInputStreams()[0];
	}
	public static final Parcelable.Creator<VSensorConfig> CREATOR  = new Creator<VSensorConfig>() {

	    public VSensorConfig createFromParcel(Parcel source) {

	    	VSensorConfig vs = new VSensorConfig(source);
	    	StaticData.addConfig(vs.id, vs);
	    	StaticData.saveNameID(vs.id, vs.getName());
			return vs;
	    }

	    public VSensorConfig[] newArray(int size) {

	        return new VSensorConfig[size];
	    }

	};

}

