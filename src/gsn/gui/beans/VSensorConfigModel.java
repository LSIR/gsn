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

@SuppressWarnings("serial")
public class VSensorConfigModel extends Model {

	public static final String PROPERTY_NAME = "name";

	public static final String PROPERTY_PRIORITY = "priority";

	public static final String PROPERTY_GENERAL_PASSWORD = "generalPassword";

	public static final String PROPERTY_MAIN_CLASS = "mainClass";

	public static final String PROPERTY_DESCRIPTION = "description";

	public static final String PROPERTY_LIFECYCLE_POOL_SIZE = "lifeCyclePoolSize";

	public static final String PROPERTY_OUTPUT_STREAM_RATE = "outputStreamRate";

	public static final String PROPERTY_WEB_PARAMETER_PASSWORD = "webParameterPassword";

	public static final String PROPERTY_STORAGE_HISTORY_SIZE = "storageHistorySize";

	public static final String PROPERTY_RATE_UNLIMITED = "rateUnlimited";

	private String name;

	private int priority;

	private String generalPassword;

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
	
	private boolean rateUnlimited;

	public VSensorConfigModel() {
		priority = VSensorConfig.DEFAULT_PRIORITY;
		lifeCyclePoolSize = VSensorConfig.DEFAULT_POOL_SIZE;
		addressing = new ArrayListModel();
		outputStructure = new ArrayListModel();
		inputStreams = new ArrayListModel();
		mainClassInitialParams = new ArrayListModel();
		webinput = new ArrayListModel();
	}

	public VSensorConfigModel(VSensorConfig vSensorConfig) {
		name = vSensorConfig.getName();
		priority = vSensorConfig.getPriority();
		generalPassword = vSensorConfig.getGeneralPassword();
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
		if (webInputs != null) {
			for (int i = 0; i < webInputs.length; i++) {
				addWebInputModel(new WebInputModel(webInputs[i]));
			}
		}
	}

	public void addWebInputModel(WebInputModel webInputModel) {
		webinput.add(webInputModel);
	}

	private void addMainClassInitialParamsList(TreeMap<String, String> initialParams) {
		if (initialParams != null) {
			for (Map.Entry<String, String> entry : initialParams.entrySet()) {
				addMainClassInitialParam(new KeyValueImp(entry.getKey(), entry.getValue()));
			}
		}
	}

	public void addMainClassInitialParam(KeyValue keyValue) {
		mainClassInitialParams.add(keyValue);
	}

	private void addInputStreamList(Collection<InputStream> inputStreams) {
		if (inputStreams != null) {
			for (InputStream stream : inputStreams) {
				addInputStreamModel(new InputStreamModel(stream));
			}
		}
	}

	public void addInputStreamModel(InputStreamModel inputStreamModel) {
		inputStreams.add(inputStreamModel);
	}

	private void addOutputStructureList(DataField[] dataFields) {
		if (dataFields != null) {
			for (int i = 0; i < dataFields.length; i++) {
				addOutputStructure(new DataFieldModel(dataFields[i]));
			}
		}
	}

	public void addOutputStructure(DataFieldModel dataFieldModel) {
		outputStructure.add(dataFieldModel);
	}

	private void addAddressingList(KeyValue[] keyValues) {
		if (keyValues != null) {
			for (int i = 0; i < keyValues.length; i++) {
				addAddressing(keyValues[i]);
			}
		}
	}

	public void addAddressing(KeyValue keyValue) {
		addressing.add(keyValue);
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		String oldDescription = getDescription();
		this.description = description;
		firePropertyChange(PROPERTY_DESCRIPTION, oldDescription, description);
	}

	public int getLifeCyclePoolSize() {
		return lifeCyclePoolSize;
	}

	public void setLifeCyclePoolSize(int lifeCyclePoolSize) {
		int oldLifecyclePoolSize = getLifeCyclePoolSize();
		this.lifeCyclePoolSize = lifeCyclePoolSize;
		firePropertyChange(PROPERTY_LIFECYCLE_POOL_SIZE, oldLifecyclePoolSize, lifeCyclePoolSize);
	}

	public String getMainClass() {
		return mainClass;
	}

	public void setMainClass(String mainClass) {
		String oldMainClass = getMainClass();
		this.mainClass = mainClass;
		firePropertyChange(PROPERTY_MAIN_CLASS, oldMainClass, mainClass);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		String oldName = getName();
		this.name = name;
		firePropertyChange(PROPERTY_NAME, oldName, name);
	}

	public int getOutputStreamRate() {
		return outputStreamRate;
	}

	public void setOutputStreamRate(int outputStreamRate) {
		int oldOutputStreamRate = getOutputStreamRate();
		this.outputStreamRate = outputStreamRate;
		firePropertyChange(PROPERTY_OUTPUT_STREAM_RATE, oldOutputStreamRate, outputStreamRate);
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		int oldPriority = getPriority();
		this.priority = priority;
		firePropertyChange(PROPERTY_PRIORITY, oldPriority, priority);
	}

	public String getStorageHistorySize() {
		return storageHistorySize;
	}

	public void setStorageHistorySize(String storageHistorySize) {
		String oldStorageHistorySize = getStorageHistorySize();
		this.storageHistorySize = storageHistorySize;
		firePropertyChange(PROPERTY_STORAGE_HISTORY_SIZE, oldStorageHistorySize, storageHistorySize);
	}

	public String getWebParameterPassword() {
		return webParameterPassword;
	}

	public void setWebParameterPassword(String webParameterPassword) {
		String oldWebParameterPassword = getWebParameterPassword();
		this.webParameterPassword = webParameterPassword;
		firePropertyChange(PROPERTY_WEB_PARAMETER_PASSWORD, oldWebParameterPassword, webParameterPassword);
	}

	public String getGeneralPassword() {
		return generalPassword;
	}

	public void setGeneralPassword(String generalPassword) {
		String oldGeneralPassword = getGeneralPassword();
		this.generalPassword = generalPassword;
		firePropertyChange(PROPERTY_GENERAL_PASSWORD, oldGeneralPassword, generalPassword);
	}

	public boolean isRateUnlimited() {
		return rateUnlimited;
	}

	public void setRateUnlimited(boolean rateUnlimited) {
		boolean oldRateUnlimited = isRateUnlimited();
		this.rateUnlimited = rateUnlimited;
		firePropertyChange(PROPERTY_RATE_UNLIMITED, oldRateUnlimited, rateUnlimited);
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

	public VSensorConfig getVSensorConfig() {
		VSensorConfig vSensorConfig = new VSensorConfig();
		vSensorConfig.setName(getName());
		vSensorConfig.setDescription(getDescription());
		vSensorConfig.setGeneralPassword(getGeneralPassword());
		vSensorConfig.setLifeCyclePoolSize(getLifeCyclePoolSize());
		vSensorConfig.setMainClass(getMainClass());
		vSensorConfig.setOutputStreamRate(getOutputStreamRate());
		vSensorConfig.setPriority(getPriority());
		vSensorConfig.setAddressing((KeyValue[]) getAddressing().toArray(new KeyValue[0]));
		vSensorConfig.setStorageHistorySize(getStorageHistorySize());
		
		System.out.println("input stream size : " + getInputStreams().getSize());
		InputStream[] inputStreams = new InputStream[getInputStreams().size()];
		for (int i = 0; i < getInputStreams().size(); i++) {
			inputStreams[i] = ((InputStreamModel)getInputStreams().get(i)).getInputStream();
		}
		vSensorConfig.setInputStreams(inputStreams);
		
		vSensorConfig.setMainClassInitialParams(getMainClassInitialParams());
		
		DataField[] outputStructure = new DataField[getOutputStructure().size()];
		for (int i = 0; i < getOutputStructure().size(); i++) {
			outputStructure[i] = ((DataFieldModel)getOutputStructure().get(i)).getDataField();
		}
		vSensorConfig.setOutputStructure(outputStructure);
		
		WebInput[] webInputs = new WebInput[getWebinput().size()];
		for (int i = 0; i < getWebinput().size(); i++) {
			webInputs[i] = ((WebInputModel)getWebinput().get(i)).getWebInput();
		}
		vSensorConfig.setWebInput(webInputs);
		if(vSensorConfig.validate() == false)
			System.out.println("VSensor validation failed");
		return vSensorConfig;
	}
}
