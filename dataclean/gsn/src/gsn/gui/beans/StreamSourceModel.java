package gsn.gui.beans;

import java.util.Iterator;
import gsn.beans.AddressBean;
import gsn.beans.StreamSource;
import com.jgoodies.binding.beans.Model;
import com.jgoodies.binding.list.ArrayListModel;

public class StreamSourceModel extends Model {
	public static final String PROPERTY_ALIAS = "alias";

	public static final String PROPERTY_SAMPLING_RATE = "samplingRate";

	public static final String PROPERTY_RAW_HISTORY_SIZE = "rawHistorySize";

	public static final String PROPERTY_DISCONNECTED_BUFFER_SIZE = "disconnectedBufferSize";

	public static final String PROPERTY_SQL_QUERY = "sqlQuery";

	public static final String PROPERTY_RAW_SLIDE_VALUE = "rawSlideValue";

	private String alias;

	private float samplingRate;

	private String rawHistorySize;
	
	private String rawSlideValue;

	private int disconnectedBufferSize;

	private String sqlQuery;
	
	private ArrayListModel addressing;
	
	public StreamSourceModel(){
		addressing = new ArrayListModel();
	}
	
	public StreamSourceModel(StreamSource streamSource){
		alias = (String) streamSource.getAlias();
		samplingRate = streamSource.getSamplingRate();
		rawHistorySize = streamSource.getRawHistorySize();
		rawSlideValue = streamSource.getSlideValue();
		disconnectedBufferSize = streamSource.getDisconnectedBufferSize();
		sqlQuery = streamSource.getSqlQuery();
		addressing = new ArrayListModel();
		addAddressing(streamSource.getAddressing());		
	}

	private void addAddressing(AddressBean[] addressBeans) {
		for (int i = 0; i < addressBeans.length; i++) {
			addAddressBeanModel(new AddressBeanModel(addressBeans[i]));
		}
	}

	public void addAddressBeanModel(AddressBeanModel addressBeanModel){
		addressing.add(addressBeanModel);
	}
	
	public void removeAddressBeanModel(AddressBeanModel addressBeanModel){
		addressing.remove(addressBeanModel);
	}
	
	public ArrayListModel getAddressing() {
		return addressing;
	}

	public void setAddressing(ArrayListModel addressing) {
		this.addressing = addressing;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		String oldAlias = getAlias();
		this.alias = alias;
		firePropertyChange(PROPERTY_ALIAS, oldAlias, alias);
	}

	public int getDisconnectedBufferSize() {
		return disconnectedBufferSize;
	}

	public void setDisconnectedBufferSize(int disconnectedBufferSize) {
		int oldDisconnectedBufferSize = getDisconnectedBufferSize();
		this.disconnectedBufferSize = disconnectedBufferSize;
		firePropertyChange(PROPERTY_DISCONNECTED_BUFFER_SIZE, oldDisconnectedBufferSize, disconnectedBufferSize);
	}

	public String getRawHistorySize() {
		return rawHistorySize;
	}

	public void setRawHistorySize(String rawHistorySize) {
		String oldRawHistorySize = getRawHistorySize();
		this.rawHistorySize = rawHistorySize;
		firePropertyChange(PROPERTY_RAW_HISTORY_SIZE, oldRawHistorySize, rawHistorySize);
	}
	
	public String getRawSlideValue() {
		return rawSlideValue;
	}

	public void setRawSlideValue(String rawSlideValue) {
		String oldRawSlideValue = getRawSlideValue();
		this.rawSlideValue = rawSlideValue;
		firePropertyChange(PROPERTY_RAW_SLIDE_VALUE, oldRawSlideValue, rawSlideValue);
	}
	
	public float getSamplingRate() {
		return samplingRate;
	}

	public void setSamplingRate(float samplingRate) {
		float oldSamplingRate = getSamplingRate();
		this.samplingRate = samplingRate;
		firePropertyChange(PROPERTY_SAMPLING_RATE, oldSamplingRate, samplingRate);
	}

	public String getSqlQuery() {
		return sqlQuery;
	}

	public void setSqlQuery(String sqlQuery) {
		String oldSqlQuery = getSqlQuery();
		this.sqlQuery = sqlQuery;
		firePropertyChange(PROPERTY_SQL_QUERY, oldSqlQuery, sqlQuery);
	}

	public StreamSource getStreamSource() {
		StreamSource streamSource = new StreamSource();
		streamSource.setAlias(getAlias());
		streamSource.setRawHistorySize(getRawHistorySize());
		streamSource.setRawSlideValue(getRawSlideValue());
		streamSource.setSamplingRate(getSamplingRate());
		streamSource.setSqlQuery(getSqlQuery());
		streamSource.setDisconnectedBufferSize(getDisconnectedBufferSize());
		AddressBean[] addressBeans = new AddressBean[addressing.size()];
		for (int i = 0; i < addressing.size(); i++) {
			addressBeans[i] = ((AddressBeanModel)addressing.get(i)).getAddressBean();
		}
		streamSource.setAddressing(addressBeans);
		return streamSource;
	}

	public ArrayListModel cloneAddressing() {
		ArrayListModel copyOfAddressing = new ArrayListModel();
		for (Iterator iter = addressing.iterator(); iter.hasNext();) {
			AddressBeanModel addressBeanModel = (AddressBeanModel) iter.next();
			copyOfAddressing.add(addressBeanModel.clone());
		}
		return copyOfAddressing;
	}
	
	
}
