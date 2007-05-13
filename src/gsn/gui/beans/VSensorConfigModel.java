package gsn.gui.beans;

import gsn.beans.DataField;
import gsn.beans.InputStream;
import gsn.beans.VSensorConfig;
import gsn.beans.WebInput;
import gsn.utils.KeyValueImp;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections.KeyValue;

import com.jgoodies.binding.beans.Model;
import com.jgoodies.binding.list.ArrayListModel;

public class VSensorConfigModel extends Model {

	public static final String PROPERTY_NAME = "name";

	public static final String PROPERTY_PRIORITY = "priority";

	public static final String PROPERTY_MAIN_CLASS = "mainClass";

	public static final String PROPERTY_DESCRIPTION = "description";

	public static final String PROPERTY_LIFECYCLE_POOL_SIZE = "lifeCyclePoolSize";

	public static final String PROPERTY_OUTPUT_STREAM_RATE = "outputStreamRate";

	public static final String PROPERTY_WEB_PARAMETER_PASSWORD = "webParameterPassword";

	public static final String PROPERTY_STORAGE_HISTORY_SIZE = "storageHistorySize";

	private String name;

	private int priority;

	private String mainClass;

	private String description;

	private int lifeCyclePoolSize;

	private int outputStreamRate;

	private ArrayListModel addressing;

	private ArrayListModel outputStructure;

	private String webParameterPassword;

	private String storageHistorySize;

	private ArrayListModel inputStreams;

	private ArrayListModel mainClassInitialParams;

	private ArrayListModel webinput;

	public VSensorConfigModel() {
		priority = VSensorConfig.DEFAULT_PRIORITY;
		lifeCyclePoolSize = 10;
		addressing = new ArrayListModel();
		outputStructure = new ArrayListModel();
		inputStreams = new ArrayListModel();
		mainClassInitialParams = new ArrayListModel();
		webinput = new ArrayListModel();
	}
	
	public VSensorConfigModel(VSensorConfig vSensorConfig){
		name = vSensorConfig.getName();
		priority = vSensorConfig.getPriority();
		mainClass = vSensorConfig.getProcessingClass();
		description = vSensorConfig.getDescription();
		lifeCyclePoolSize = vSensorConfig.getLifeCyclePoolSize();
		outputStreamRate = vSensorConfig.getOutputStreamRate();
		webParameterPassword = vSensorConfig.getWebParameterPassword();
		storageHistorySize = vSensorConfig.getStorageHistorySize();
		addressing = new ArrayListModel();
		addAddressingList(vSensorConfig.getAddressing());
		outputStructure = new ArrayListModel();
		addOutputStructureList(vSensorConfig.getOutputStructure());
		inputStreams = new ArrayListModel();
		addInputStreamList(vSensorConfig.getInputStreams());
		mainClassInitialParams = new ArrayListModel();
		addMainClassInitialParamsList(vSensorConfig.getMainClassInitialParams());
		webinput = new ArrayListModel();
		addWebInputList(vSensorConfig.getWebinput());
	}

	private void addWebInputList(WebInput[] webInputs) {
		for (int i = 0; i < webInputs.length; i++) {
			addWebInputModel(new WebInputModel(webInputs[i]));
		}
	}

	public void addWebInputModel(WebInputModel webInputModel) {
		webinput.add(webInputModel);
	}

	private void addMainClassInitialParamsList(TreeMap<String, String> initialParams) {
		for (Map.Entry<String, String> entry : initialParams.entrySet()) {
			addMainClassInitialParam(new KeyValueImp(entry.getKey(), entry.getValue()));
		}
	}

	public void addMainClassInitialParam(KeyValue keyValue) {
		mainClassInitialParams.add(keyValue);
	}

	private void addInputStreamList(Collection<InputStream> inputStreams) {
		for (InputStream stream : inputStreams) {
			addInputStreamModel(new InputStreamModel(stream));
		}
	}

	public void addInputStreamModel(InputStreamModel inputStreamModel) {
		inputStreams.add(inputStreamModel);
	}

	private void addOutputStructureList(DataField[] dataFields) {
		for (int i = 0; i < dataFields.length; i++) {
			addOutputStructure(new DataFieldModel(dataFields[i]));
		}
	}

	public void addOutputStructure(DataFieldModel dataFieldModel) {
		outputStructure.add(dataFieldModel);
	}

	private void addAddressingList(KeyValue[] keyValues) {
		for (int i = 0; i < keyValues.length; i++) {
			addAddressing(keyValues[i]);
		}
	}

	public void addAddressing(KeyValue keyValue) {
		addressing.add(keyValue);
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getLifeCyclePoolSize() {
		return lifeCyclePoolSize;
	}

	public void setLifeCyclePoolSize(int lifeCyclePoolSize) {
		this.lifeCyclePoolSize = lifeCyclePoolSize;
	}

	public String getMainClass() {
		return mainClass;
	}

	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getOutputStreamRate() {
		return outputStreamRate;
	}

	public void setOutputStreamRate(int outputStreamRate) {
		this.outputStreamRate = outputStreamRate;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public String getStorageHistorySize() {
		return storageHistorySize;
	}

	public void setStorageHistorySize(String storageHistorySize) {
		this.storageHistorySize = storageHistorySize;
	}

	public String getWebParameterPassword() {
		return webParameterPassword;
	}

	public void setWebParameterPassword(String webParameterPassword) {
		this.webParameterPassword = webParameterPassword;
	}

	public ArrayListModel getAddressing() {
		return addressing;
	}

	public void setAddressing(ArrayListModel addressing) {
		this.addressing = addressing;
	}

	public ArrayListModel getInputStreams() {
		return inputStreams;
	}

	public void setInputStreams(ArrayListModel inputStreams) {
		this.inputStreams = inputStreams;
	}

	public ArrayListModel getMainClassInitialParams() {
		return mainClassInitialParams;
	}

	public void setMainClassInitialParams(ArrayListModel mainClassInitialParams) {
		this.mainClassInitialParams = mainClassInitialParams;
	}

	public ArrayListModel getOutputStructure() {
		return outputStructure;
	}

	public void setOutputStructure(ArrayListModel outputStructure) {
		this.outputStructure = outputStructure;
	}

	public ArrayListModel getWebinput() {
		return webinput;
	}

	public void setWebinput(ArrayListModel webinput) {
		this.webinput = webinput;
	}
}
