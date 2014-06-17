package tinygsn.beans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import tinygsn.model.wrappers.AbstractWrapper;
import tinygsn.utils.GSNRuntimeException;
import android.util.Log;

public class StreamSource implements Serializable, QueueListener {
	private static final String TAG = "StreamSource";
	public static String[] AGGREGATOR = { "Average", "Max", "Min" };

	private int samplingRate;
	private int windowSize;
	private int step;
	private int aggregator;

	private Queue queue = null;
	private transient AbstractWrapper wrapper;
	private InputStream inputStream;

	// ================================================
	private static final long serialVersionUID = 5222853537667420098L;
	public static final String DEFAULT_QUERY = "select * from wrapper";
	// private static final transient Logger logger = Logger.getLogger(
	// StreamSource.class );
	private String alias;
	private String rawHistorySize = null;
	private String rawSlideValue = null;
	private int disconnectedBufferSize;
	private String sqlQuery;
	protected int uid;
	protected StringBuilder uidS;
	private static final String[] dateFormats = new String[] {
			"yyyy/MM/dd 'at' HH:mm:ss z", "h:mm:ss a", "h:mm a" };
	private transient Date startDate;
	private transient Date endDate;
	public static final AddressBean[] EMPTY_ADDRESS_BEAN = new AddressBean[] {};
	private AddressBean addressing[] = EMPTY_ADDRESS_BEAN;
	private AddressBean activeAddressBean; // To be used by the gui

	public StreamSource(Queue queue) {
		queue.registerListener(this);
		this.queue = queue;
	}

	@Override
	public void notifyMe(ArrayList<StreamElement> data) {
		// Use Aggregator to process data s
		StreamElement se = data.get(0);
		if (inputStream == null) {
			Log.e(TAG, "inputStream is null");
			return;
		}
		switch (aggregator) {
		case 0:
			// Average
			
			//TODO replace with constant. Add SQL filter 
			
			// 1. Get sum
			for (int i = 1; i < data.size(); i++) {
				for (int j = 0; j < se.getFieldNames().length; j++) {
					StreamElement se_i = data.get(i);
					se.setData(j, getDouble(se.getData()[j]) + getDouble(se_i.getData()[j]));
				}
			}
			// 2. Average
			for (int j = 0; j < se.getFieldNames().length; j++) {
				se.setData(j, getDouble(se.getData()[j]) / data.size());
			}
		case 1:
			// Max
			for (int i = 1; i < data.size(); i++) {
				if (getDouble(data.get(i).getData()[0]) > getDouble(se.getData()[0])) {
					se = data.get(i);
				}
			}
		case 2:
			// Min
			for (int i = 1; i < data.size(); i++) {
				if (getDouble(data.get(i).getData()[0]) < getDouble(se.getData()[0])) {
					se = data.get(i);
				}
			}
		}
		;

		Log.v(TAG, se.toString());
		inputStream.getVirtualSensor().dataAvailable(se);
		
	}

	private double getDouble(Serializable s){
		double d = ((Number) s).doubleValue() ;
		return d;
	}
	
	public String getRawHistorySize() {
		return rawHistorySize;
	}

	public StreamSource setRawHistorySize(String rawHistorySize) {
		this.rawHistorySize = rawHistorySize;
		return this;
	}

	public StreamSource setRawSlideValue(String rawSlideValue) {
		this.rawSlideValue = rawSlideValue;
		return this;
	}

	public StreamSource setAddressing(AddressBean[] addressing) {
		this.addressing = addressing;
		return this;
	}

	public StreamSource setAlias(String alias) {
		this.alias = alias;
		return this;
	}

	public StreamSource setSqlQuery(String sqlQuery) {
		this.sqlQuery = sqlQuery;
		return this;
	}

	public AddressBean[] getAddressing() {
		if (addressing == null)
			addressing = EMPTY_ADDRESS_BEAN;
		return addressing;
	}

	/**
	 * @return Returns the alias.
	 */
	public CharSequence getAlias2() {
		return alias.toLowerCase();
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	/**
	 * @return Returns the bufferSize.
	 */
	public int getDisconnectedBufferSize() {
		return disconnectedBufferSize;
	}

	public void setDisconnectedBufferSize(int disconnectedBufferSize) {
		this.disconnectedBufferSize = disconnectedBufferSize;
	}

	public int getSamplingRate() {
		return samplingRate;
	}

	/**
	 * @return Returns the storageSize.
	 */
	public String getStorageSize() {
		return this.rawHistorySize;
	}

	/**
	 * @return the slide value
	 */
	public String getSlideValue() {
		return "";
	}

	/**
	 * @return Returns the sqlQuery.
	 */
	public String getSqlQuery() {
		if (sqlQuery == null || sqlQuery.trim().length() == 0)
			sqlQuery = DEFAULT_QUERY;
		return sqlQuery;
	}

	public void setWrapper(AbstractWrapper wrapper) throws SQLException {
		if (validate() == false)
			throw new GSNRuntimeException(
					"Can't set the wrapper when the stream source is invalid.");
		this.wrapper = wrapper;
		wrapper.addListener(this);
	}

	/**
	 * @return Returns the activeSourceProducer.
	 */
	public AbstractWrapper getWrapper() {
		if (wrapper == null)
			throw new GSNRuntimeException(
					"The wrapper for stream source is not set !.");
		return wrapper;
	}

	/**
	 * ; Note that the validate method doesn't case if the wrapper variable or
	 * input stream variable are set or not.
	 * 
	 * @return
	 */
	public boolean validate() {
		return true;
	}

	public StringBuilder toSql() {
		return new StringBuilder();

	}

	public StreamSource setInputStream(InputStream is) throws GSNRuntimeException {
		// if (alias == null)
		// throw new NullPointerException("Alias can't be null!");
		// if (this.inputStream != null && is != this.inputStream)
		// throw new
		// GSNRuntimeException("Can't reset the input stream variable !.");
		// this.inputStream = is;
		// if (validate() == false)
		// throw new GSNRuntimeException(
		// "You can't set the input stream on an invalid stream source. ");
		inputStream = is;

		return this;
	}

	public AddressBean getActiveAddressBean() {
		return activeAddressBean;
	}

	public String toString() {
		StringBuilder toReturn = new StringBuilder();
		toReturn.append(" Stream Source object: ");
		toReturn.append(" Alias: ").append(alias);
		toReturn.append(" uidS: ").append(uidS);
		toReturn.append(" Active source: ").append(activeAddressBean);

		return toReturn.toString();
	}

	public Queue getQueue() {
		return queue;
	}

	public void setQueue(Queue queue) {
		this.queue = queue;
	}

	public int getWindowSize() {
		return windowSize;
	}

	public void setWindowSize(int windowSize) {
		this.windowSize = windowSize;
	}

	public int getStep() {
		return step;
	}

	public void setStep(int step) {
		this.step = step;
	}

	public int getAggregator() {
		return aggregator;
	}

	public void setAggregator(int aggregator) {
		this.aggregator = aggregator;
	}

	public void setSamplingRate(int samplingRate) {
		this.samplingRate = samplingRate;
	}
}
