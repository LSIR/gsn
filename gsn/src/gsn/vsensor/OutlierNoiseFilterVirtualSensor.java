package gsn.vsensor;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.Map;
import java.lang.String;

import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.lang.*;

import gsn.Main;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.InputStream;
import gsn.beans.StreamElement;
import gsn.beans.StreamSource;

import org.apache.log4j.Logger;

/**
 * This virtual sensor does outlier and noise filtering.
 * The filters are applied on the column 'raw_value_in' from the wrapper/stream 
 * query. Use aliasing for renaming the column name of interest. The column
 * 'raw_value_in' has to be a number, but can be of one of the following types: 
 * TINYINT, SMALLINT, INTEGER, DOUBLE or VARCHAR.
 * 
 * This processing class does not alter the data in the stream element but 
 * extend the data stream with additional information such as the filter outputs. 
 * The data extension as well as the filters to be apply and their settings can 
 * be configured in the parameter section of the Virtual Sensor Description file 
 * (xml-file).
 * 
 * @author Daniel Burgener
 */
public class OutlierNoiseFilterVirtualSensor extends BridgeVirtualSensorPermasense 
{
	private static final transient Logger logger = Logger.getLogger(OutlierNoiseFilterVirtualSensor.class);

	private Map<Long, RawValue> mRawValueBuffer; // hashmap as buffer containing <generation_time, raw value> pairs

	private static int iBufferSize;
	private static int iOutlierFilterWindowWidthInMinutes;
	private static int iOutlierFilterMinNbOfValuesInWindow;
	private static double dOutlierFilterThreshold;
	private static OutlierFilter eOutlierFilter;
	
	private static int iNoiseFilterWindowWidthInMinutes;
	private static NoiseFilter eNoiseFilter;
	
	
	private static DataField[] dataField = {	
        			new DataField("RAW_VALUE_OUT", "DOUBLE"),
					new DataField("OUTLIER_FILTER_MEDIAN", "DOUBLE"),
					new DataField("OUTLIER_FILTER_LOWER_BOUND", "DOUBLE"),
					new DataField("OUTLIER_FILTER_UPPER_BOUND", "DOUBLE"),
					new DataField("OUTLIER_FILTER_CLASSIFICATION", "INTEGER"),
					new DataField("OUTLIER_FILTER_VALUE", "DOUBLE"),
        			new DataField("NOISE_FILTER_VALUE", "DOUBLE")
    };
	
    private enum OutlierFilter{
		// list with outlier filter enumerations and according code and string
		ONE_SIDED_MEDIAN_MENOLD(1, "one_sided_median_menold");
		
		private final short code;	 	// code representing task
		private final String string;	// string representing task
		
		private OutlierFilter(int code, String string){
			this.code = (short)code;
			this.string = string;
		}
		
		public short getCode(){
			return this.code;
		}
		
		public String getString(){
			return this.string;
		}
		
		// translate the custom string representation back to the corresponding enum
		public static OutlierFilter fromString(String text) {
		    if (text != null) {
		    	for (OutlierFilter b : OutlierFilter.values()) {
		    		if (text.equalsIgnoreCase(b.string)) {
		    			return b;
		    		}
		    	}
		    }
		    return null;
		}
	}
    
    private enum NoiseFilter{
		// list with noise filter enumerations and according code and string
		NOISE_FILTER_MEAN(1, "mean");
		
		private final short code;	 	// code representing task
		private final String string;	// string representing task
		
		private NoiseFilter(int code, String string){
			this.code = (short)code;
			this.string = string;
		}
		
		public short getCode(){
			return this.code;
		}
		
		public String getString(){
			return this.string;
		}
		
		// translate the custom string representation back to the corresponding enum
		public static NoiseFilter fromString(String text) {
		    if (text != null) {
		    	for (NoiseFilter b : NoiseFilter.values()) {
		    		if (text.equalsIgnoreCase(b.string)) {
		    			return b;
		    		}
		    	}
		    }
		    return null;
		}
	}
    
	private enum OutlierClassification{
		// list with outlier classification enumerations and according code and string
		OUTLIER_CLASSIFIED(1, "outlier_classified"),
		NON_OUTLIER_CLASSIFIED(2, "non_outlier_classified"),
		NOT_CLASSIFIED(3, "not_classified");
		
		private final short code;	 	// code representing task
		private final String string;	// string representing task
		
		private OutlierClassification(int code, String string){
			this.code = (short)code;
			this.string = string;
		}
		
		public short getCode(){
			return this.code;
		}
		
		public String getString(){
			return this.string;
		}
		
		// translate the custom string representation back to the corresponding enum
		public static OutlierClassification fromString(String text) {
		    if (text != null) {
		    	for (OutlierClassification b : OutlierClassification.values()) {
		    		if (text.equalsIgnoreCase(b.string)) {
		    			return b;
		    		}
		    	}
		    }
		    return null;
		}
	}
	
	@Override
	public boolean initialize() {
		/* This function initializes the buffer with raw values.
		 * */
		boolean ret = super.initialize();
		
		mRawValueBuffer = new HashMap<Long, RawValue>(); // integer is device id, object is decacon mux
		
		// read configuration from xml file
		TreeMap <  String , String > params = getVirtualSensorConfiguration( ).getMainClassInitialParams( );
		
		iOutlierFilterWindowWidthInMinutes = Integer.parseInt(params.get("outlier_filter_window_width_in_minutes"));
		iOutlierFilterMinNbOfValuesInWindow = Integer.parseInt(params.get("outlier_filter_min_nb_of_values_in_window"));
		dOutlierFilterThreshold = Double.parseDouble(params.get("outlier_filter_threshold"));
		eOutlierFilter = OutlierFilter.fromString(params.get("outlier_filter"));
		
		iNoiseFilterWindowWidthInMinutes = Integer.parseInt(params.get("noise_filter_window_width_in_minutes"));
		eNoiseFilter = NoiseFilter.fromString(params.get("noise_filter"));
		
		iBufferSize = Integer.parseInt(params.get("buffer_size"));
		
		// init hash map by reading data from db
		Connection conn = null;
		ResultSet rs = null;
		double dRawValue;
		long lGenTime;
		RawValue oRawValue;
		
		
		Collection < InputStream > cInputStreams = getVirtualSensorConfiguration().getInputStreams();
		StreamSource streamSource;
		if( cInputStreams.size() == 1 ){
			if( cInputStreams.iterator().next().getSources().length == 1) {
				streamSource = cInputStreams.iterator().next().getSources()[0];
				String sWrapperName = streamSource.getAddressing()[0].getWrapper();
				String sSqlStreamQuery = streamSource.getSqlQuery();
				String sSqlWrapperQuery = streamSource.getAddressing()[0].getPredicateValue("query");

				logger.info("The wrapper name is: " + sWrapperName);
				logger.info("The sql stream query is: " + sSqlStreamQuery);
				logger.info("The sql wrapper query is: " + sSqlWrapperQuery);
				
				if(sWrapperName.contains("remote-rest")) {
					logger.info("remote-rest buffer initialization not implemented yet");
				
				}
				else if( sWrapperName.contains("local") ) {
					try {
						logger.info("Init buffer of outlier/noise filter with " + iBufferSize + "entries");
						conn = Main.getStorage(getVirtualSensorConfiguration().getName()).getConnection();
						StringBuilder query = new StringBuilder();
						// query is done according parameters in configuration file (xml)
						// query only takes:
						// - valid values, i.e. values != null
						// - values within the window width
						// furthermore the number of values is limited by the buffer size.
						// if more values are available in the specified window width than
						// the buffer is big, then the most recent values are used
						
						// build query: 
						// 1. wrapper query is placed into source query (by replacing 'wrapper')
						// 2. new source query (see 1.) is extended
						query.append("select raw_value_in, generation_time from (");
						query.append(sSqlStreamQuery.replace("wrapper", "(" + sSqlWrapperQuery + ")"));	// replace wrapper by query
						query.append(") where raw_value_in is not null and generation_time >= ");
						query.append(System.currentTimeMillis() - iOutlierFilterWindowWidthInMinutes * 60 * 1000);
						query.append(" order by generation_time desc limit ").append(iBufferSize);
						logger.info("query: " + query); // output query
						
						// execute query
						rs = Main.getStorage(getVirtualSensorConfiguration().getName()).executeQueryWithResultSet(query, conn);
						
						// read query result as long as buffer is not full and results are available
						while(rs.next() & mRawValueBuffer.size() < iBufferSize){
							dRawValue = rs.getDouble("raw_value_in"); 		// read raw value
							lGenTime = rs.getLong("generation_time"); 	// read generation time
							oRawValue = new RawValue(dRawValue, OutlierClassification.NOT_CLASSIFIED);
							// verify if raw value already in buffer
							if( mRawValueBuffer.containsKey(lGenTime) == false){
								mRawValueBuffer.put(lGenTime, oRawValue);
								logger.info("buffer init data: genTime: " + lGenTime + ", value: " + oRawValue.getRawValue());
							}
						}
						
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
				}
				else{
					logger.warn("The wrapper name >" + sWrapperName + "< is not supported");
				}
			}
			else{
				logger.warn("Number of stream sources has to be exactly 1! " + cInputStreams.iterator().next().getSources().length + " number of stream sources is not supported");
			}
		}
		else{
			logger.warn("Number of input streams has to be exactly 1! " + cInputStreams.size() + " number of input streams is not supported");
		}
		
		return ret;
	}

	@Override
	public void dataAvailable(String inputStreamName, StreamElement data) {
		double dNewRawValue;
		long lNewGenTime;
		RawValue oNewRawValue; // temporary storage for raw value and filter results/settings
		
		logger.info("New streaming data from " + inputStreamName + ". Raw value: " + data.getData("raw_value_in"));
		
		// check if data is valid, i.e. not null
		if(data.getData("raw_value_in") == null){
			logger.info("New streaming data from " + inputStreamName + " discarded. Raw value: " + data.getData("raw_value_in"));
			return;
		}
		
		// read and convert raw value and generation time from data stream
		byte bType = data.getType("raw_value_in");
		switch( bType ){
			case DataTypes.TINYINT:
			case DataTypes.SMALLINT:
			case DataTypes.INTEGER:
			case DataTypes.DOUBLE:
				dNewRawValue = (Double) data.getData("raw_value_in");
				break;
			case DataTypes.VARCHAR:
				dNewRawValue = (double)Double.parseDouble((String)data.getData("raw_value_in"));
				break;
			default:
				logger.info("The OutlierNoseFilterVirtualSensor does not support data of type: " + bType);
				return;
		}
		
		oNewRawValue = new RawValue(dNewRawValue);
		lNewGenTime = (Long) data.getData("generation_time");
		
		// sort raw values according generation time in ascending order (smallest (oldest) values first)
		TreeMap<Long, RawValue> ascendingSortedMap = new TreeMap<Long, RawValue>(mRawValueBuffer);
		
		// sort raw values according generation time in descending order (biggest (latest) values first)
		NavigableMap<Long, RawValue> descendingSortedMap = ascendingSortedMap.descendingMap();
		
		/* OUTLIER FILTER */
		/* -------------- */
		
		// create sub map of data within window
		SortedMap<Long, RawValue> descendingOutlierFilterWindowMap = descendingSortedMap.subMap(lNewGenTime, lNewGenTime - iOutlierFilterWindowWidthInMinutes * 60 * 1000);

		// check if enough data points inside window
		if( descendingOutlierFilterWindowMap.size() >= iOutlierFilterMinNbOfValuesInWindow ){
			// enough values in window
			
			// select outlier filter
			switch( eOutlierFilter ){
				case ONE_SIDED_MEDIAN_MENOLD:
					double[] adRawValue; // array with raw values
					OneSidedMedianMenoldReturn oneSidedMedianMenoldReturn;
					//StringBuilder sWindowData = new StringBuilder();
					
					// get raw values inside window as array
					int i=0;
					adRawValue = new double[descendingOutlierFilterWindowMap.size()];
					for( Map.Entry<Long, RawValue> entry: descendingOutlierFilterWindowMap.entrySet()){
						adRawValue[i] = entry.getValue().getRawValue(); // copy value from map into array
						i++;
						//sWindowData.append(entry.getKey()).append(" ").append(entry.getValue().getRawValue()).append("\n\r");
					}
					//logger.info("window data: \n\r" + sWindowData);
					
					// check if new value is outlier
					oneSidedMedianMenoldReturn = oneSidedMedianMenold(adRawValue, oNewRawValue.getRawValue(), dOutlierFilterThreshold);
					oNewRawValue.setMedianAndBounds(oneSidedMedianMenoldReturn.getMedian(), oneSidedMedianMenoldReturn.getLowerBound(), oneSidedMedianMenoldReturn.getUpperBound());
					
					if( oneSidedMedianMenoldReturn.getOutlier() == true){
						// outlier
						oNewRawValue.setOutlierClassification(OutlierClassification.OUTLIER_CLASSIFIED);
						
						logger.info("New streaming data from " + inputStreamName + " is an outlier (median: " + oneSidedMedianMenoldReturn.getMedian() + ", raw value: " + oNewRawValue.getRawValue() + ")");
						
						// replace outlier with last valid value
						oNewRawValue.setOutlierFilteredValue(Double.NaN);
						for(Map.Entry<Long, RawValue> entry: descendingOutlierFilterWindowMap.entrySet()){
							
							logger.info("find last non outlier: gentime " + entry.getKey());
							
							if( entry.getValue().getOutlierClassification() == OutlierClassification.NON_OUTLIER_CLASSIFIED){
								oNewRawValue.setOutlierFilteredValue(entry.getValue().getRawValue());
								break;
							}
						}
						
						// set median if no value inside window was classified as non outlier
						if(Double.isNaN(oNewRawValue.getOutlierFilteredValue())){
							oNewRawValue.setOutlierFilteredValue(oneSidedMedianMenoldReturn.getMedian());
						}
					}
					else {
						// not outlier
						oNewRawValue.setOutlierClassification(OutlierClassification.NON_OUTLIER_CLASSIFIED);
						oNewRawValue.setOutlierFilteredValue(oNewRawValue.getRawValue());
					}
					
					break;
					
				default:
					logger.warn("invalid outlier filter:" + eOutlierFilter.getString());
			}
		}
		else {
			// not enough values in window
			oNewRawValue.setOutlierClassification(OutlierClassification.NOT_CLASSIFIED);
			logger.info("New streaming data from " + inputStreamName + " not classified, not enough values in window (only " + descendingOutlierFilterWindowMap.size() + " instead of " + iOutlierFilterMinNbOfValuesInWindow + ")");
		}
		
		/* NOISE FILTER */
		/* ------------ */
		
		// create sub map of data within window
		SortedMap<Long, RawValue> descendingNoiseFilterWindowMap = descendingSortedMap.subMap(lNewGenTime, lNewGenTime - iNoiseFilterWindowWidthInMinutes * 60 * 1000);

		// select outlier filter
		switch( eNoiseFilter ){
			case NOISE_FILTER_MEAN:
				/* Noise filter is only applied if data point was outlier classified
				 * For the noise filtering all outlier classified data within 
				 * window size is used.
				 * */
				double dMean;
				
				if( oNewRawValue.getOutlierClassification() != OutlierClassification.NOT_CLASSIFIED) {
					// sum over outlier filtered values inside window and the new
					int i=1;
					double dSum = oNewRawValue.getOutlierFilteredValue();
					for( Map.Entry<Long, RawValue> entry: descendingNoiseFilterWindowMap.entrySet()){
						// only use classified outlier filtered values
						if( entry.getValue().getOutlierClassification() != OutlierClassification.NOT_CLASSIFIED ) {
							dSum += entry.getValue().getOutlierFilteredValue();
							i++;
						}
					}
					
					// calculate mean
					dMean = dSum / i;
					//logger.info("mean: " + dMean);
				} 
				else {
					dMean = Double.NaN;
				}
				
				oNewRawValue.setNoiseFilteredValue(dMean);
				
				break;
		}
		
		// add/replace new data point only if either enough space is available in buffer
		// or buffer contains an older data point which can be removed 
		if( mRawValueBuffer.size() < iBufferSize ){
			// enough space in buffer
			mRawValueBuffer.put(lNewGenTime, oNewRawValue);
		}
		else {
			// not enough space in buffer
			long lOldestDataPoint = ascendingSortedMap.firstKey(); // get oldest data point
		
			if( lOldestDataPoint <= lNewGenTime ) {
				// remove oldest data point
				if( mRawValueBuffer.remove(lOldestDataPoint) == null){
					logger.warn("Removing data point from buffer failed. Key >" + lOldestDataPoint + "< not found." );
				}
				else{
					logger.info("Remove oldest data point with gentime: " + lOldestDataPoint);
				}
				
				// add new data point
				mRawValueBuffer.put(lNewGenTime, oNewRawValue);
			}
		}
		//logger.info("Data point >" + lNewGenTime + "< added to buffer (raw value: " + oNewRawValue.getRawValue() +", filtered value: "+ oNewRawValue.getOutlierFilteredValue() + ", OutlierClassification: " + oNewRawValue.getOutlierClassification().getString() +")");

		// check buffer size
		if(mRawValueBuffer.size() > iBufferSize){
			logger.warn("Buffer size exceeded. " + mRawValueBuffer.size() + " instead of " + iBufferSize + "elements in buffer.");
		}
		else {
			//logger.info("Buffer size: " + mRawValueBuffer.size());
		}
		
		// generate output stream
		data = new StreamElement(data, dataField, new Serializable[] { oNewRawValue.getRawValue(), (Double.isNaN(oNewRawValue.getMedian())) ? null:oNewRawValue.getMedian() , (Double.isNaN(oNewRawValue.getLowerBound()))? null:oNewRawValue.getLowerBound(),  (Double.isNaN(oNewRawValue.getUpperBound()))?null:oNewRawValue.getUpperBound(), oNewRawValue.getOutlierClassification().getCode(), (Double.isNaN(oNewRawValue.getOutlierFilteredValue())) ? null:oNewRawValue.getOutlierFilteredValue(), (Double.isNaN(oNewRawValue.getNoiseFilteredValue())) ? null:oNewRawValue.getNoiseFilteredValue()});
		//logger.info("data stream: " + data + ", timestamp: " + data.getTimeStamp());
		
		super.dataAvailable(inputStreamName, data);
	}
	
	private OneSidedMedianMenoldReturn oneSidedMedianMenold(double[] adWindow, double dNewRawValue, double dThreshold){
		// determine median of window data and new value
		double dMedian = StatUtils.percentile(ArrayUtils.add(adWindow, dNewRawValue), 50);
		OneSidedMedianMenoldReturn returnValue;
		
		//logger.info("median: " + dMedian);
		
		if( Math.abs(dNewRawValue - dMedian) > dThreshold ){
			// outlier
			returnValue = new OneSidedMedianMenoldReturn(dMedian, dMedian - dThreshold, dMedian + dThreshold, true);
		}
		else {
			// not outlier
			returnValue =  new OneSidedMedianMenoldReturn(dMedian, dMedian - dThreshold, dMedian + dThreshold, false);
		}
		
		return returnValue;
	}
	
	private class OneSidedMedianMenoldReturn{
		private double dMedian;
		private double dUpperBound;
		private double dLowerBound;
		private boolean bOutlier;
		
		public OneSidedMedianMenoldReturn(double dMedian, double dLowerBound, double dUpperBound, boolean bOutlier){
			this.dMedian = dMedian;
			this.dLowerBound = dLowerBound;
			this.dUpperBound = dUpperBound;
			this.bOutlier = bOutlier;
		}
		
		public double getMedian(){
			return this.dMedian;
		}
		
		public double getLowerBound(){
			return this.dLowerBound;
		}
		
		public double getUpperBound(){
			return this.dUpperBound;
		}
		
		public boolean getOutlier(){
			return this.bOutlier;
		}
	}


	private void startGumstix(int iDestination) {
		String[] paramNames = {"destination", "cmd", "arg", "repetitioncnt"};
		Serializable[] paramValues = new Serializable [] {Integer.toString(iDestination), "14", "1", "3"};
		super.dataFromWeb("tosmsg", paramNames, paramValues);
	}
	
	private void clearWakeUpBeacon(int iDestination) {
		String[] paramNames = {"destination", "cmd", "arg", "repetitioncnt"};
		Serializable[] paramValues = new Serializable [] {Integer.toString(iDestination), "14", "4", "3"};
		super.dataFromWeb("tosmsg", paramNames, paramValues);
	}
	
	@Override
	public synchronized void dispose() {

	}
	

	public class RawValue{
		private double dRawValue; // raw value
		private double dOutlierFilteredValue; // outlier filtered value
		private double dNoiseFilteredValue; // noise filtered value
		private OutlierClassification cOutlierClassification;	  // OutlierClassification
		private double dUpperBound;
		private double dLowerBound;
		private double dMedian;
		
		// constructor
		public RawValue(double dRawValue) {
			this.dRawValue = dRawValue;  // raw value
			this.dOutlierFilteredValue = Double.NaN; // outlier filtered value
			this.dNoiseFilteredValue = Double.NaN ; // noise filtered value
			this.cOutlierClassification = OutlierClassification.NOT_CLASSIFIED; // OutlierClassification of raw value
			this.dUpperBound = Double.NaN;
			this.dLowerBound = Double.NaN;
			this.dMedian = Double.NaN;
		}
		
		public RawValue(double dRawValue, OutlierClassification cOutlierClassification) {
			this.dRawValue = dRawValue;  // raw value
			this.dOutlierFilteredValue = Double.NaN; // filtered value
			this.dOutlierFilteredValue = Double.NaN; // outlier filtered value
			this.cOutlierClassification = cOutlierClassification; // OutlierClassification of raw value
			this.dUpperBound = Double.NaN;
			this.dLowerBound = Double.NaN;
			this.dMedian = Double.NaN;
		}
		
		public double getRawValue() {
			return this.dRawValue;
		}
		
		public void setMedianAndBounds(double dMedian, double dLowerBound, double dUpperBound){
			this.dMedian = dMedian;
			this.dUpperBound = dUpperBound;
			this.dLowerBound = dLowerBound;
		}
		
		public double getMedian(){
			return this.dMedian;
		}
		
		public double getLowerBound(){
			return this.dLowerBound;
		}
		
		public double getUpperBound(){
			return this.dUpperBound;
		}
		
		public void setOutlierClassification(OutlierClassification outlierClassification){
			this.cOutlierClassification = outlierClassification;
		}
		
		public OutlierClassification getOutlierClassification(){
			return this.cOutlierClassification;
		}
		
		public void setOutlierFilteredValue(double dFilteredValue){
			this.dOutlierFilteredValue = dFilteredValue;
		}
		
		public double getOutlierFilteredValue(){
			return this.dOutlierFilteredValue;
		}
		
		public void setNoiseFilteredValue(double dNoiseFilteredValue){
			this.dNoiseFilteredValue = dNoiseFilteredValue;
		}
		
		public double getNoiseFilteredValue(){
			return this.dNoiseFilteredValue;
		}
	}
}

