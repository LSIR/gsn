package gsn.vsensor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.lang.String;
import java.io.File;
import javax.swing.Timer;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

import gsn.http.ClientHttpRequest;
import gsn.Main;
import gsn.beans.DataField;
//import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.beans.StreamSource;


import org.apache.log4j.Logger;

/**
 * 
 * @author Daniel Burgener
 */
public class Sampler6712ControlAlgorithmVS extends BridgeVirtualSensorPermasense implements ActionListener
{
	private static final transient Logger logger = Logger.getLogger(Sampler6712ControlAlgorithmVS.class);
	private static boolean bControlAlgorithmEnableState; // true=enabled
	private static ScheduleStateMachine sm;
	private static int iHashCodeOfLastSentScheduleString;
	private Map<Long, Double> mSensorValueBuffer; // hashmap as buffer containing <generation_time, sensor_value> pairs
	private Timer timerGenerateInitialControlDataStream;
	
	/* START data from xml-file
	 */
	private static double dStartConditionThreshold;
	private static int iStartConditionHoldTimeInMin;
	private static int iWindowSizeInMin;
	private static int iMinNbOfDataPointsWithinHoldTime;
	private static int iDestinationDeviceId;
	
	private static int iCoreStationWakeupTimeoutInMin;
	private static int iCoreStationWakeupRepetitionCnt;
	
	private static int iSamplingStartDelayInMin;
	private static SamplingScheme samplingScheme;
	
	private static String sScheduleHostName;
	private static String sScheduleVsName;
	
	private static String sDozerCommandHostName;
	private static String sDozerCommandVsName;
	/*  END read data from xml-file
	 */
		
	private static int iBufferSize;
	
	private static final int I_SCHEDULE_ACK_TIMEOUT_IN_SEC = 30;
	private static final int I_SCHEDULE_ACK_MAX_NB_OF_TIMEOUT = 3;
	
	private static final byte BT_SENSOR_DATA_TYPE = 0;
	private static final byte BT_CONTROL_TYPE = 1;
	
	// states of state machine
	private enum ScheduleType {	NONE, 
								STATIC_SCHEDULE, 
								SINGLE_SAMPLING_DEBUG_SCHEDULE
	}
	
	// states of state machine
	private enum State {START,
						IDLE, 
						SCHEDULE_GENERATION, 
						WAIT_FOR_TRANSMISSION, 
						SCHEDULE_TRANSMITTED
	}
	
	// events for state machine
	private enum Event {NONE,
						GENERATE_SCHEDULE, 
						SCHEDULE_ACK, 
						SCHEDULE_ACK_TIMEOUT_EXPIRED, 
						SCHEDULE_TRANSMITTED_TIMEOUT_EXPIRED,
						SCHEDULE_TRANSMITTED, 
						SCHEDULE_RESET,
						MANUAL_SCHEDULE_GENERATION,
						INIT_STATE_MACHINE}
	
	private enum SensorValueEvaluationResult{
		// list with outlier filter enumerations and according code and string
		START_CONDITION_NOT_FULFILLED((byte)0),
		START_CONDITION_FULFILLED((byte)1),
		NOT_ENOUGH_DATA_POINTS_WITHIN_HOLD_TIME((byte)2),
		DATA_POINT_OUTSIDE_WINDOW((byte)3);
		
		private final byte code;	 	// code representing task
		
		private SensorValueEvaluationResult(byte code){
			this.code = (byte)code;
		}
		
		public byte getCode(){
			return this.code;
		}
	}
	
	private static DataField[] dataField = {	
		new DataField("GENERATION_TIME", "BIGINT"),
		new DataField("TIMESTAMP", "BIGINT"),
		new DataField("DEVICE_ID", "INTEGER"),
		new DataField("DATA_TYPE", "TINYINT"),
		
		// SENSOR DATA
		new DataField("START_CONDITION_THRESHOLD", "DOUBLE"),
		new DataField("START_CONDITION_HOLD_TIME_IN_MIN", "INTEGER"),
		new DataField("WINDOW_SIZE_IN_MIN", "INTEGER"),
		new DataField("MINIMUM_NB_OF_DATA_POINTS_WITHIN_HOLD_TIME", "INTEGER"),
		new DataField("START_CONDITION_FULFILLED", "TINYINT"),
		new DataField("MIN", "DOUBLE"),
		new DataField("MAX", "DOUBLE"),
		new DataField("MIN_MAX_DELTA", "DOUBLE"),
		
		// CONTROL DATA
		new DataField("CONTROL_ALGORITHM_ENABLE_STATE", "TINYINT"),
		new DataField("SCHEDULE_GENERATION_EVENT", "VARCHAR(128)"),
		new DataField("SCHEDULE_GENERATION_STATE", "VARCHAR(128)"),
		new DataField("AUTO_SCHEDULE_GENERATION_RESET_TIME_IN_MIN", "INTEGER")
		};
	
	@Override
	public boolean initialize() {
		boolean ret = super.initialize();
		
		// variables for db access
		Connection conn = null;
		ResultSet rs = null;
		double dSensorValue;
		long lGenTimeToVerify;
		
		mSensorValueBuffer = new HashMap<Long, Double>(); // key is generation time, value is sensor value
		
		// read configuration from xml file
		TreeMap <  String , String > params = getVirtualSensorConfiguration( ).getMainClassInitialParams( );
		
		dStartConditionThreshold = Double.parseDouble(params.get("threshold"));
		iStartConditionHoldTimeInMin = Integer.parseInt(params.get("hold_time_in_min"));
		iWindowSizeInMin = Integer.parseInt(params.get("window_size_in_min"));
		iMinNbOfDataPointsWithinHoldTime = Integer.parseInt(params.get("min_nb_of_data_points_within_hold_time"));
		iDestinationDeviceId = Integer.parseInt(params.get("destination_device_id"));
		
		// buffer has size so that it can hold sensor values with a rate of 
		// 1 value per minute (current rate is 1 value per 2 minutes)
		iBufferSize = (iStartConditionHoldTimeInMin + iWindowSizeInMin); 
		
		iSamplingStartDelayInMin = Integer.parseInt(params.get("sampling_start_delay_in_min"));

		try {
			// read and convert sampling scheme
			samplingScheme = new SamplingScheme(params.get("sampling_scheme"));
			logger.info("Sampling scheme: \n" + samplingScheme.getSamplingSchemeAsString());
		}
		catch (Exception e) {
			logger.warn("Reading the sampling scheme from the xml-file failed: " + e);
		}
		 
		//String sSamplingScheme[] = s.split("|");
		
		iCoreStationWakeupTimeoutInMin = Integer.parseInt(params.get("core_station_wake_up_timeout_in_min"));
		iCoreStationWakeupRepetitionCnt = Integer.parseInt(params.get("core_station_wake_up_repetition_cnt"));
		
		sScheduleHostName = (String) params.get("schedule_host_name");
		sScheduleVsName = (String) params.get("schedule_vs_name");
		sDozerCommandHostName = (String) params.get("dozer_command_host_name");
		sDozerCommandVsName = (String) params.get("dozer_command_vs_name");
		
		iHashCodeOfLastSentScheduleString = 0;
		
		// init bControlAlgorithmEnableState
		// based on DB
		try {
			logger.info("Get control algorithm enable state and set schedule generation reset time from DB ");
			conn = Main.getStorage(getVirtualSensorConfiguration().getName()).getConnection();
			StringBuilder query = new StringBuilder();
	
			query.append("select * from ");
			query.append(getVirtualSensorConfiguration().getName()); // get name of this vs
			query.append(" where device_id = ");
			query.append(iDestinationDeviceId);
			query.append(" and data_type = ");
			query.append(BT_CONTROL_TYPE);
			query.append(" order by timed desc limit 1");
			
			logger.debug("query: " + query.toString());

			// execute query
			rs = Main.getStorage(getVirtualSensorConfiguration().getName()).executeQueryWithResultSet(query, conn);
			
			if( rs.next() ){
				if( rs.getByte("control_algorithm_enable_state") == 0 ){
					bControlAlgorithmEnableState = false;
				}
				else{
					bControlAlgorithmEnableState = true;
				}
			}
			else{
				bControlAlgorithmEnableState = false;
			}
		} catch (Exception e) {
			logger.error("Reading DB for init of control algorithm enable state failed: " + e.getMessage());
			bControlAlgorithmEnableState = false;
		}
		
		logger.info("Control algorithm enable state initialized to: " + bControlAlgorithmEnableState);
		
		// init sensor value buffer
		Collection < gsn.beans.InputStream > cInputStreams = getVirtualSensorConfiguration().getInputStreams();
		StreamSource streamSource;
		gsn.beans.InputStream inputStreamSource;

		// check all input streams if they are called 'sensor'
		for( Iterator<gsn.beans.InputStream> i = cInputStreams.iterator(); i.hasNext(); ) {
			inputStreamSource = i.next();
			
			logger.debug("stream name: " + inputStreamSource.getInputStreamName());
			
			// verify if stream name is correct
			if( inputStreamSource.getInputStreamName().contains("sensor") ) {
				// verify if input stream has only one source
				if(inputStreamSource.getSources().length == 1) {
					streamSource = inputStreamSource.getSources()[0]; // get stream source
					
					String sWrapperName = streamSource.getAddressing()[0].getWrapper();
					String sSqlStreamQuery = streamSource.getSqlQuery();
					String sSqlWrapperQuery = streamSource.getAddressing()[0].getPredicateValue("query");
		
					//logger.info("The wrapper name is: " + sWrapperName);
					//logger.info("The sql stream query is: " + sSqlStreamQuery);
					//logger.info("The sql wrapper query is: " + sSqlWrapperQuery);
					
					if(sWrapperName.contains("remote-rest")) {
						logger.info("remote-rest buffer initialization not implemented yet");
					
					}
					else if( sWrapperName.contains("local") ) {
						try {
							logger.info("Init buffer of control algorithm with maximal " + iBufferSize + " entries");
							conn = Main.getStorage(getVirtualSensorConfiguration().getName()).getConnection();
							StringBuilder query = new StringBuilder();
							// query is done according parameters in configuration file (xml)
							// query only takes:
							// - valid values, i.e. values != null
							// - values within the window width + hold time
							// furthermore the number of values is limited by the buffer size.
							// if more values are available in the specified window width than
							// the buffer is big, then the most recent values are used
							
							// build query: 
							// 1. wrapper query is placed into source query (by replacing 'wrapper')
							// 2. new source query (see 1.) is extended
							query.append("select sensor_value, generation_time from (");
							query.append(sSqlStreamQuery.replace("wrapper", "(" + sSqlWrapperQuery + ")"));	// replace wrapper by query
							query.append(") where sensor_value is not null and generation_time >= ");
							query.append(System.currentTimeMillis() - (iWindowSizeInMin + iStartConditionHoldTimeInMin) * 60 * 1000);
							query.append(" order by generation_time desc limit ").append(iBufferSize);
							//logger.info("query: " + query); // output query
							
							// execute query
							rs = Main.getStorage(getVirtualSensorConfiguration().getName()).executeQueryWithResultSet(query, conn);
							
							// read query result as long as buffer is not full and results are available
							// most current sensor values are read first
							while(rs.next() & mSensorValueBuffer.size() < iBufferSize){
								
								lGenTimeToVerify = rs.getLong("generation_time"); 	// read generation time
								dSensorValue = rs.getDouble("sensor_value"); 	// read raw value
								// check if last column read was NULL (shouldn't happen because of query, see above)
								// last column read is 'sensor_value'
								if( rs.wasNull() == false){
									// sensor value is not NULL
									// verify if sensor value already in buffer
									if( mSensorValueBuffer.containsKey(lGenTimeToVerify) == false){
										logger.debug("buffer init data: genTime: " + lGenTimeToVerify + ", value: " + dSensorValue);
										mSensorValueBuffer.put(lGenTimeToVerify, dSensorValue);										
									}
									else{
										logger.info("Sensor value " + dSensorValue + " with genTime: " + lGenTimeToVerify + " occurred multiple times in database");
									}
								}
								else{
									// sensor value is NULL
									// this case should not occur because the query above only selects
									// sensor data which are not NULL
									logger.debug("sensor value with genTime " + lGenTimeToVerify + " was NULL");
								}
							}
							
							logger.info("Buffer of control algorithm initialized with " + mSensorValueBuffer.size() + " entries.");
							
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
				
				break; // leave for loop
			}
		}

		
		// create and init state machine
		sm = new ScheduleStateMachine(iCoreStationWakeupRepetitionCnt, 
									  iCoreStationWakeupTimeoutInMin,
									  sScheduleHostName,
									  sScheduleVsName,
									  sDozerCommandHostName,
									  sDozerCommandVsName);
		
		// send initial state machine state in 10s
		this.timerGenerateInitialControlDataStream = new Timer( 10 * 1000 , this );
		this.timerGenerateInitialControlDataStream.setActionCommand("GENERATE_CONTROL_DATA_STREAM"); 
		this.timerGenerateInitialControlDataStream.setRepeats(false); // use timer as one shot timer
		this.timerGenerateInitialControlDataStream.start( );
				
		return ret;
	}

	/**
	 * This function is called by the timeout timer when it has expired.
	 * 
	 * @param actionEvent: event to send to the state machine
	 * 
	 */
	public void actionPerformed ( ActionEvent actionEvent ) {
		// init state machine and send its initial state as data stream
		// this is a work around because sending the initial state in the 
		// initialize() function does not work!

		// init state machine
		Event event = Event.INIT_STATE_MACHINE;
		logger.info("Initialize schedule state machine (event: " + event + ")");
		sm.putEvent(event); // send event
	}
	
	/**
	 * This function is called when new streaming data is available. This 
	 * virtual sensor consumes streaming data of different type. Therefore
	 * the names of the streams (<stream name="xxx"> in *.xml) has to be
	 * set appropriate:
	 * - stream name of data source: "source"
	 * - stream name of schedule: "schedule"
	 * - stream name of dozer command: "dozer_command"
	 */
	@Override
	public void dataAvailable(String inputStreamName, StreamElement data) {
		double dMinValue = Double.NaN;
		double dMaxValue = Double.NaN;
		double dMinMaxDelta = Double.NaN;
		long lCurrentTime;
		
		// data of new sensor value 
		long lGenerationTimeOfNewSensorValue;
		double dNewSensorValue;
		
		SensorValueEvaluationResult eSensorValueEvaluationResult;
		
		TreeMap<Long, Double> tmAscendingSortedSensorValueBuffer;

		Event event = Event.NONE;

		lCurrentTime = System.currentTimeMillis();
		
		// The virtual sensor can receive data from different sources. Therefore
		// the input streams have to be handled differently. The differentiation
		// is done based on the input stream name.
		
		logger.debug("data available from inputStreamName: " + inputStreamName);
		
		if( inputStreamName.toLowerCase().contentEquals("sensor")){
			// new sensor data received
			
			// determine trigger
			// -----------------
			// The start condition is fulfilled if the min-max delta is larger than
			// the threshold for all sensor values within the hold time window. 
			
			//logger.info("New streaming data from " + inputStreamName + ". gentime: " + data.getData("generation_time") + ", sensor value: " + data.getData("sensor_value"));
			
			// the sensor value is not evaluated if it is null
			if(data.getData("sensor_value") == null){
				logger.debug("New streaming data from " + inputStreamName + " discarded. sensor value: " + data.getData("sensor_value"));
				return;
			}
			
			// read generation time and sensor value
			lGenerationTimeOfNewSensorValue = (Long) data.getData("generation_time");
			dNewSensorValue = (Double) data.getData("sensor_value");
			
			// check if generation time is within window + hold time (with respect to current time)
			if( lGenerationTimeOfNewSensorValue < lCurrentTime - (iWindowSizeInMin + iStartConditionHoldTimeInMin) * 60 * 1000 ){
				// skip sensor value because it is not within window
				eSensorValueEvaluationResult = SensorValueEvaluationResult.DATA_POINT_OUTSIDE_WINDOW;
				logger.debug("Skip new sensor value because not within window. Gen time: " + lGenerationTimeOfNewSensorValue + ", Sensor value: " + dNewSensorValue);
			}
			else {
				// sensor value within window + hold time
				logger.debug("New sensor value within window. Gen time: " + lGenerationTimeOfNewSensorValue + ", Sensor value: " + dNewSensorValue);
				
				// sort sensor values according generation time in ascending order (smallest (oldest) values first)
				tmAscendingSortedSensorValueBuffer = new TreeMap<Long, Double>(mSensorValueBuffer);
				
				// store sensor value in buffer
				// add/replace new sensor value only if either enough space is available in buffer
				// or buffer contains an older sensor value which can be removed 
				if( mSensorValueBuffer.size() < iBufferSize ){
					// enough space in buffer
					mSensorValueBuffer.put(lGenerationTimeOfNewSensorValue, dNewSensorValue);
				}
				else {
					// not enough space in buffer
					long lGenerationTimeOfOldestSensorValue = tmAscendingSortedSensorValueBuffer.firstKey(); // get oldest data point
				
					if( lGenerationTimeOfOldestSensorValue <= lGenerationTimeOfNewSensorValue ) {
						// remove oldest data point
						if( mSensorValueBuffer.remove(lGenerationTimeOfOldestSensorValue) == null){
							logger.warn("Removing data point from buffer failed. Key >" + lGenerationTimeOfOldestSensorValue + "< not found." );
						}
						else{
							logger.debug("Remove oldest data point with gentime: " + lGenerationTimeOfOldestSensorValue);
						}
						
						// add new data point
						mSensorValueBuffer.put(lGenerationTimeOfNewSensorValue, dNewSensorValue);
					}
					else{
						logger.warn("Control algorithm buffer too small to store all data within the window size");
					}
				}
				
				SortedMap<Long, Double> smSensorValuesWithinHoldTime; // this map contains sensor values which are within hold time window
				
				// sort sensor value according generation time in ascending order (smallest (oldest) values first)
				tmAscendingSortedSensorValueBuffer = new TreeMap<Long, Double>(mSensorValueBuffer);
				// create sub map with sensor data within hold time window (with respect to the current time)
				smSensorValuesWithinHoldTime = tmAscendingSortedSensorValueBuffer.subMap(lCurrentTime - iStartConditionHoldTimeInMin * 60 * 1000, lCurrentTime);

				// check whether enough values within hold time window
				if(smSensorValuesWithinHoldTime.size() < iMinNbOfDataPointsWithinHoldTime) {
					// not enough values within hold time window
					logger.info("Not enough values within hold time. " + smSensorValuesWithinHoldTime.size() + " instead of " + iMinNbOfDataPointsWithinHoldTime);
					eSensorValueEvaluationResult = SensorValueEvaluationResult.NOT_ENOUGH_DATA_POINTS_WITHIN_HOLD_TIME;
				}
				else{
					// enough values within hold time window
					logger.debug("Enough values within window: " + smSensorValuesWithinHoldTime.size() );
					// verify if min/max delta is above threshold for all sensor values
					// within hold time window (starting with the oldest sensor value)
					// the min, max and delta values of the most current sensor value
					// are used for storing in the database.
					
					SortedMap<Long, Double> smSensorDataWithinWindow;
					Collection<Double> col;
					eSensorValueEvaluationResult = SensorValueEvaluationResult.START_CONDITION_FULFILLED;
					for( Long lGenTimeToVerify : smSensorValuesWithinHoldTime.keySet() ){
						// get data within window for sensor value to verify (in ascending order)
						smSensorDataWithinWindow = tmAscendingSortedSensorValueBuffer.subMap(lGenTimeToVerify - iWindowSizeInMin * 60 * 1000, lGenTimeToVerify + 1); // '+1' to include the sensor value with lGenTimeToVerify
						
						// determine min and max value
						col = smSensorDataWithinWindow.values();
						dMaxValue = Collections.max(col); // calculate max value
						dMinValue = Collections.min(col); // calculate min value
						dMinMaxDelta = dMaxValue - dMinValue;
						logger.debug("Gen time: " + lGenTimeToVerify + ", min: " + dMinValue + ", max: " + dMaxValue + ", delta: " + dMinMaxDelta);
						
						if( dMinMaxDelta <= dStartConditionThreshold ){
							eSensorValueEvaluationResult = SensorValueEvaluationResult.START_CONDITION_NOT_FULFILLED;
						}
					}
				}
			}
						
			logger.debug("Sensor evaluation result: " + eSensorValueEvaluationResult.toString());
			
			// determine if a new schedule has to be transmitted
			if( bControlAlgorithmEnableState == true &
				eSensorValueEvaluationResult == SensorValueEvaluationResult.START_CONDITION_FULFILLED ){
				event = Event.GENERATE_SCHEDULE;
				sm.putEvent(event);
			}
			
			// generate output stream with sensor data information
			data = new StreamElement(dataField, new Serializable[] {lCurrentTime, lCurrentTime, iDestinationDeviceId, BT_SENSOR_DATA_TYPE, dStartConditionThreshold, iStartConditionHoldTimeInMin, iWindowSizeInMin, iMinNbOfDataPointsWithinHoldTime, eSensorValueEvaluationResult.getCode(), Double.isNaN(dMinValue)? null:dMinValue, Double.isNaN(dMaxValue)? null:dMaxValue, Double.isNaN(dMinMaxDelta)? null:dMinMaxDelta, null, null, null, null} );
			super.dataAvailable(inputStreamName, data);
		}
		else if ( inputStreamName.toLowerCase().contentEquals("schedule") ){
			// new schedule data received
			String sReceivedSchedule = "";

			//logger.info("file item type: " + data.getType("schedule"));
			//logger.info("file data: " + new String((byte[])data.getData("schedule")));
			
			// check if received schedule corresponds to last sent schedule
			sReceivedSchedule = new String((byte[])data.getData("schedule"));
			if( sReceivedSchedule.hashCode() == iHashCodeOfLastSentScheduleString ){
				// schedule string are identical
				Serializable sTransmissionTime = data.getData("transmission_time");
				// determine whether schedule was received by vs or if 
				// schedule was sent to core station (transmission_time != null)
				
				if( sTransmissionTime == null){
					// schedule has not been transmitted yet
					event = Event.SCHEDULE_ACK;
				}
				else{
					// schedule has been transmitted
					event = Event.SCHEDULE_TRANSMITTED;
				}
			}
			else{
				logger.info("Schedule received which was not sent by the control algorithm");
			}
				
			// treat event
			sm.putEvent(event);
		}
		else if ( inputStreamName.toLowerCase().contentEquals("dozer_command") ){
			// new schedule data received
			// not used
		}
		else if ( inputStreamName.toLowerCase().contentEquals("sampler6712_sampling") ){
			// not used
		}
		else if ( inputStreamName.toLowerCase().contentEquals("sampler6712_status") ){
			// not used
		}
		else {
			logger.warn("Unknown input stream element received: '"+ inputStreamName + "'. Check stream names in VS description file (*.xml)");
		}
	}
	
	@Override
	public boolean dataFromWeb(String command, String[] paramNames, Serializable[] paramValues) {
		Event event = Event.NONE;

		logger.debug("action: " + command + ", compare result: " +command.compareToIgnoreCase("configuration"));
		
		// determine action
		if( command.compareToIgnoreCase("configuration") == 0 ) {
			String sControlState = "", sSpecialAction = "";
			// read fields
			for (int i = 0 ; i < paramNames.length ; i++) {
				if( paramNames[i].compareToIgnoreCase("control_state") == 0 ) {
					sControlState = (String)paramValues[i];
				}
				
				if( paramNames[i].compareToIgnoreCase("special_action") == 0 ) {
					sSpecialAction = (String)paramValues[i];
				}
			}
			
			// convert fields
			try{
				if( sControlState.equalsIgnoreCase("on") ) {
					bControlAlgorithmEnableState = true;
				}
				else if (sControlState.equalsIgnoreCase("off")){
					bControlAlgorithmEnableState = false;
				}
				else
				{
					// don't modify control algorithm state
				}
				
				if( sSpecialAction.equalsIgnoreCase("none")){
					event = Event.NONE;
				}
				else if( sSpecialAction.equalsIgnoreCase("reset_schedule_generation")){
					event = Event.SCHEDULE_RESET;
				}
				else if( sSpecialAction.equalsIgnoreCase("manual_start_schedule_generation")){
					event = Event.MANUAL_SCHEDULE_GENERATION;
				}
				else{
					logger.warn("Unknown special action");
					return false;
				}
				
				// treat event
				sm.putEvent(event);
				
				return true;
			}
			catch(Exception e){
				logger.warn("Invalid input values, input not treated: " +e);
				return false;
			}
		}
		else{
			logger.info("action not supported (" + command +")" );
			return false;
		}
	}
	
	
	
	@Override
	public synchronized void dispose() {

	}
	
	/**
	 * This class represents the schedule state machine.
	 * It performs state changes based on events.
	 * 
	 * 
	 */
	class ScheduleStateMachine implements ActionListener{
		private State state;
		private Timer timeoutTimerScheduleAckReceived;
		private Timer timeoutTimerScheduleTransmitted;
		private int iTimeoutCntScheduleTransmitted;
		private int iTimeoutCntScheduleAckReceived;
		private int iMaxNbOfRepetition;
		private int iWakeupTimeoutInMin;
		private InputStream inputStream;
		private String sScheduleHostName;
		private String sScheduleVsName;
		private String sDozerCommandHostName;
		private String sDozerCommandVsName;		
		
		// constructor
		ScheduleStateMachine(int iMaxNbOfRepetition, 
							 int iWakeupTimeoutInMin,
							 String sScheduleHostName,
							 String sScheduleVsName,
							 String sDozerCommandHostName,
							 String sDozerCommandVsName){
			this.state = State.START;
			this.iTimeoutCntScheduleTransmitted = 0;
			this.iTimeoutCntScheduleAckReceived = 0;
			this.iMaxNbOfRepetition = iMaxNbOfRepetition;
			this.iWakeupTimeoutInMin = iWakeupTimeoutInMin;
			
			this.sScheduleVsName = sScheduleVsName;
			this.sDozerCommandVsName = sDozerCommandVsName;
			
			// ensure host names end with backslash
			this.sScheduleHostName = sScheduleHostName.trim();
			if( this.sScheduleHostName.endsWith("/") == false ){
				this.sScheduleHostName += "/";
			}
			
			this.sDozerCommandHostName = sDozerCommandHostName.trim();
			if( this.sDozerCommandHostName.endsWith("/") == false ){
				this.sDozerCommandHostName += "/";
			}
			
			this.setState(State.IDLE); // set state
		}
		
		/**
		 * This function is called by the timeout timer when it has expired.
		 * 
		 * @param actionEvent: event to send to the state machine
		 * 
		 */
		public void actionPerformed ( ActionEvent actionEvent ) {
			// convert actionEvent into event (enum)
			Event event = Event.valueOf(actionEvent.getActionCommand());
			logger.debug("actionPerformed event: " + event);
			putEvent(event); // send event
		}
		
		/**
		 * This function changes the state of the state machine and also
		 * performs the (re-)entering procedure of the specific state.
		 * 
		 * @param state: new state
		 * 
		 */
		private void setState(State state, Object...objects ){
			logger.debug("State machine: State " + state + " (re-) entered");
			
			// state exiting procedure
			switch(this.state){
				case SCHEDULE_TRANSMITTED:
					// emtpy
					break;
			}
			
			// state entering procedure
			this.state = state; // set new state and ... 
			
			// ... perform appropriate entering procedure
			switch(this.state){
				case IDLE:
					
					break;
					
				case SCHEDULE_GENERATION:
					Schedule schedule = null;
					if( objects.length == 1){
						
						Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")); // get current UTC time
						// delay start time of sampling, minimum value is 5 minutes
						calendar.add(Calendar.MINUTE, (iSamplingStartDelayInMin<5)? 5:iSamplingStartDelayInMin); 
						
						// generate schedule depending on parameter
						switch((ScheduleType)objects[0]){
							case SINGLE_SAMPLING_DEBUG_SCHEDULE:
								// generate schedule
								schedule = new Schedule();
								schedule.replaceReportStatusJob("0", "9", "*", "*");
								schedule.addSamplingJobSeries((byte)1, 	// bottle start nb
															  (byte)1, // bottle stop nb
															  (byte)1, 	// nb of samplings per bottle
															  (short)1, // sampling volume [ml]
															  calendar, // start time
															  (short)30); // sampling interval [min]
								break;
								
							case STATIC_SCHEDULE:
								// generate schedule
								schedule = new Schedule();
								schedule.replaceReportStatusJob("0", "9", "*", "*");
								schedule.addSamplingJobList(samplingScheme.getSamplingList(), calendar);
								break;
						
							case NONE:
							default:
								logger.warn("No schedule will be generated! Check parameter for schedule generation state");
						}
					}
					else{
						logger.warn("Invalid schedule generation parameter length: " + objects.length);
					}
					
					//logger.info("schedule: \r\n" + schedule.getScheduleAsString());
					
					// send schedule
					try {
						
						if(schedule != null){
						// create file
							File fScheduleFile = new File("schedule");
							FileWriter fScheduleFileWriter = new FileWriter(fScheduleFile);
							fScheduleFileWriter.write(schedule.getScheduleAsString());
							iHashCodeOfLastSentScheduleString = schedule.getScheduleAsString().hashCode();
							fScheduleFileWriter.close();
							
							// create post
							ClientHttpRequest clientHttpRequest = new ClientHttpRequest( this.sScheduleHostName + "upload");
							clientHttpRequest.setParameter("vsname", this.sScheduleVsName);
							clientHttpRequest.setParameter("cmd", "schedule_command");
							clientHttpRequest.setParameter("schedule_command;core_station", String.valueOf(iDestinationDeviceId));
							clientHttpRequest.setParameter("schedule_command;schedule", fScheduleFile);
							
							// post request
							inputStream = clientHttpRequest.post();
							logger.debug("Schedule post result: " + inputStream);
						}
					} catch (IOException e) {
						
						logger.warn("Schedule generation and sending via http post failed: " + e);
					} 
					
					// start timeout (schedule ack timeout)
					this.timeoutTimerScheduleAckReceived = new Timer( I_SCHEDULE_ACK_TIMEOUT_IN_SEC * 1000 , this );
					this.timeoutTimerScheduleAckReceived.setActionCommand("SCHEDULE_ACK_TIMEOUT_EXPIRED"); // action command is event enum
					this.timeoutTimerScheduleAckReceived.setRepeats(false); // use timer as one shot timer
					this.timeoutTimerScheduleAckReceived.start( );
					
					break;
					
				case SCHEDULE_TRANSMITTED:
					// disable control algorithm
					bControlAlgorithmEnableState = false;
					break;
					
				case WAIT_FOR_TRANSMISSION:
					
					// wake-up core station
					try {
						// create post
						ClientHttpRequest clientHttpRequest = new ClientHttpRequest( this.sDozerCommandHostName + "upload");
						clientHttpRequest.setParameter("vsname", this.sDozerCommandVsName);
						clientHttpRequest.setParameter("cmd", "tosmsg");
						clientHttpRequest.setParameter("tosmsg;destination", String.valueOf(iDestinationDeviceId));
						clientHttpRequest.setParameter("tosmsg;cmd", "14"); // GUMSTIX_CTRL_CMD
						clientHttpRequest.setParameter("tosmsg;arg", "2"); // Start Gumstix (BackLog will start normally and thus shutdown immediately if their is nothing to do) 
						clientHttpRequest.setParameter("tosmsg;repetitioncnt", "3"); // repeat command 3 times
						
						// post request
						inputStream = clientHttpRequest.post();
						logger.debug("Dozer command post result: " + inputStream);
					}catch (IOException e) {
						logger.warn("Sending of dozer command via http post failed: " + e);
					}
					
					// start schedule transmitted timeout
					this.timeoutTimerScheduleTransmitted = new Timer( iWakeupTimeoutInMin * 60 * 1000 , this );
					this.timeoutTimerScheduleTransmitted.setActionCommand("SCHEDULE_TRANSMITTED_TIMEOUT_EXPIRED"); // action command is event enum
					this.timeoutTimerScheduleTransmitted.setRepeats(false); // use timer as one shot timer
					this.timeoutTimerScheduleTransmitted.start( );

					break;
					
				default:
					logger.warn("State " + state + " is not handled in setState() function");
			}
			
		}
		
		/**
		 * This function changes the state machine state according the new
		 * event. Condition on state changes are verified in this function.
		 * Furthermore, the action on transitions are also executed in this 
		 * function. The entering process of the new state is done in the 
		 * newState() function. 
		 * 
		 * @param event: new event
		 * @param objects: optional parameters
		 */
		protected void putEvent(Event event, Object...objects ){
			
			State newState = null;
			Object aoSetStateParameter = null;
			
			// check if event is non event
			if(event != Event.NONE){
				logger.debug("State machine: new event: " + event);
				
				// determine new state depending on current state and event
				switch(this.state) {
					case IDLE:
						
						switch(event) {
							case MANUAL_SCHEDULE_GENERATION:	
							case GENERATE_SCHEDULE:
								this.iTimeoutCntScheduleAckReceived = 0;  // clear timeout counter
								newState = State.SCHEDULE_GENERATION;
								aoSetStateParameter = ScheduleType.STATIC_SCHEDULE;
								break;
							case INIT_STATE_MACHINE:
								newState = State.IDLE;
								break;
						}
						break;
						
					case SCHEDULE_GENERATION:
						switch(event){
							case SCHEDULE_ACK_TIMEOUT_EXPIRED:
								
								//logger.info("sm received timeout expired");
								this.iTimeoutCntScheduleAckReceived++; // increment timeout counter
								
								if( this.iTimeoutCntScheduleAckReceived > I_SCHEDULE_ACK_MAX_NB_OF_TIMEOUT) {
									// timer does not have to be stopped because it
									// is stopped after first action
									logger.info("Schedule could not been set because of missing ack!");
									newState = State.IDLE;
								}
								else{
									// timer is restarted in state entering procedure
									newState = State.SCHEDULE_GENERATION;
								}
								break;
								
							case SCHEDULE_ACK:
								this.timeoutTimerScheduleAckReceived.stop(); // stop timer
								this.iTimeoutCntScheduleTransmitted = 0;  // clear timeout counter
								newState = State.WAIT_FOR_TRANSMISSION;
								break;
								
							case SCHEDULE_TRANSMITTED:
								this.timeoutTimerScheduleAckReceived.stop(); // stop timer
								newState = State.SCHEDULE_TRANSMITTED;
								break;
							
							case INIT_STATE_MACHINE:
								newState = State.IDLE;
								break;
								
						}
						break;
						
					case WAIT_FOR_TRANSMISSION:
						switch(event){
							case SCHEDULE_TRANSMITTED_TIMEOUT_EXPIRED:
								//logger.info("sm received timeout expired");
								this.iTimeoutCntScheduleTransmitted++; // increment timeout counter
								
								if( this.iTimeoutCntScheduleTransmitted > this.iMaxNbOfRepetition) {
									// timer does not have to be stopped because it
									// is stopped after first action
									logger.info("Schedule could not been set because of missing transmission ack!");
									newState = State.IDLE;
								}
								else{
									// timer is restarted in state entering procedure
									newState = State.WAIT_FOR_TRANSMISSION;
								}
								break;
								
							case SCHEDULE_TRANSMITTED:
								this.timeoutTimerScheduleTransmitted.stop(); // stop timer
								newState = State.SCHEDULE_TRANSMITTED;
								break;
							
							case INIT_STATE_MACHINE:
								newState = State.IDLE;
								break;
						}
							
						break;
						
					case SCHEDULE_TRANSMITTED:
						switch(event){
							case SCHEDULE_RESET:
								newState = State.IDLE;
								break;
								
							case INIT_STATE_MACHINE:
								newState = State.IDLE;
								break;
						}
						break;
				}
				
				// check if state has to be changed/reentered or not
				if(newState == null){
					logger.debug("State machine: Event " + event + " has no effect on state");
				}
				else{
					// change state and perform state entering procedure
					if(aoSetStateParameter == null){
						this.setState(newState);
					}
					else{
						this.setState(newState, aoSetStateParameter);
					}
				}
			}
			
			long lCurrentTime = System.currentTimeMillis();

			// generate output stream
			StreamElement data = new StreamElement(dataField, new Serializable[] {lCurrentTime, lCurrentTime, iDestinationDeviceId, BT_CONTROL_TYPE, null, null, null, null, null, null, null, null, bControlAlgorithmEnableState? (byte)1:(byte)0, event.toString(), sm.getState().toString(), null} );
			dataProduced( data );
		}
		
		/**
		 * This function returns the current state of the state machine. 
		 * 
		 * @return current state
		 */
		protected State getState(){
			return this.state;
		}
	}
	
	/*
	 * Format of sampling scheme:
	 * startBottle, stopBottle, samplingIntervalInMin, samplingVolumeInMl ; startBottle, stopBottle, ...
	 * e.g.: "1,2,10,10;2,3,20,100;5,20,30,50"
	 */
	
	class SamplingScheme{
		List<SingleSampling> lSamplingList;
		
		SamplingScheme(String sSamplingScheme){
			byte btStartBottleNumber;
			byte btStopBottleNumber;
			short shSamplingIntervalInMin;
			short shSamplingVolumeInMl;
			
			lSamplingList = new ArrayList<SingleSampling>();

			logger.info("Sampling scheme: "+ sSamplingScheme);
			
			// split into sub sampling schemas
			String saSamplingScheme[] = sSamplingScheme.split(";");
			
			logger.debug("nb of sub sampling scheme: "+ saSamplingScheme.length);
			
			// for each sub sampling scheme add single samplings to list
			for(int i=0; i < saSamplingScheme.length; i++){
				
				logger.debug("sub sampling scheme: "+ saSamplingScheme[i]);
				
				// read data of sub sampling scheme
				String saSubSamplingScheme[] = saSamplingScheme[i].split(",");
				
				btStartBottleNumber = (byte) Integer.parseInt(saSubSamplingScheme[0]);
				btStopBottleNumber = (byte) Integer.parseInt(saSubSamplingScheme[1]);
				shSamplingIntervalInMin = (short) Integer.parseInt(saSubSamplingScheme[2]);
				shSamplingVolumeInMl = (short) Integer.parseInt(saSubSamplingScheme[3]);
				
				// generate and stor sub sampling scheme (in list)
				for(byte bBottleCntr = btStartBottleNumber; bBottleCntr <= btStopBottleNumber; bBottleCntr++){
					lSamplingList.add(new SingleSampling(bBottleCntr, shSamplingIntervalInMin, shSamplingVolumeInMl));
				}
			}
		}
		
		/*	
		 * This function returns the sampling scheme as string
		 * 
		 * 
		 */
		protected String getSamplingSchemeAsString(){
			StringBuilder sbSamplingScheme = new StringBuilder();
			
			for (SingleSampling singleSampling : this.lSamplingList) {
				sbSamplingScheme.append("Bottle number: " + singleSampling.btBottleNumber + ", interval: " + singleSampling.shSamplingIntervalInMin + "min., volume: " + singleSampling.shSamplingVolumeInMl + "ml\n");
			}
			
			return sbSamplingScheme.toString();
		}
		
		protected List<SingleSampling> getSamplingList(){
			return lSamplingList;
		}
	}
	
	/*
	 * class to store a single sampling consisting of:
	 * - bottle number (where to store the sample)
	 * - sampling volume (water volume to draw in ml)
	 * - sampling interval (time periode between this and previous sampling)
	 */
	class SingleSampling{
		byte btBottleNumber;
		short shSamplingVolumeInMl;
		short shSamplingIntervalInMin;
		
		SingleSampling(	byte btBottleNumber,
						short shSamplingIntervalInMin,
						short shSamplingVolumeInMl){
			this.btBottleNumber = btBottleNumber;
			this.shSamplingVolumeInMl = shSamplingVolumeInMl;
			this.shSamplingIntervalInMin = shSamplingIntervalInMin;
		}
		
		
	}
	
	class Schedule{
		private ReportStatus reportStatus = null;
		private Map<Date, SamplingJob> mSamplingJob;
		private final byte BT_BACKWARD_TOLERANCE_IN_MIN = 5;
		
		// constructor
		Schedule(){
			this.mSamplingJob = new HashMap<Date, SamplingJob>(); // hashmap as buffer containing <generation_time, raw value> pairs
		}
		/**
		 * This function appends a list of sampling jobs.
		 * @param lSamplingList: containing sampling information
		 * @param startCalendar: start time of first sampling (in UTC)
		 */
		protected void addSamplingJobList(List<SingleSampling> lSamplingList, Calendar startCalendar){
			
			// Subtract first sampling interval because first sampling is not 
			// started the sampling interval but immediately, i.e. at the 
			// startCalendar time
			startCalendar.add(Calendar.MINUTE, -lSamplingList.get(0).shSamplingIntervalInMin);
			
			// add sampling jobs
			for (SingleSampling singleSampling : lSamplingList) {
				startCalendar.add(Calendar.MINUTE, singleSampling.shSamplingIntervalInMin); // increment time
				this.addSamplingJob(startCalendar.getTime(), singleSampling.btBottleNumber, singleSampling.shSamplingVolumeInMl);
			}
		}
		
		/**
		 * This function appends a series of sampling jobs. 
		 * 
		 * @param btStartBottleNr: bottle nb of starting bottle
		 * @param btStopBottleNr: bottle nb of stopping bottle (inclusive)
		 * @param btNbOfSamplesPerBottle: number of samples per bottle
		 * @param shSamplingVolumeInMlPerSample: sampling volume per sample [ml]
		 * @param startCalendar: start time of sampling (in UTC)
		 * @param shSamplingIntervalInMin: time between two samplings [min]
		 * @return schedule as string
		 */
		protected void addSamplingJobSeries(	byte btStartBottleNr,
											byte btStopBottleNr,
											byte btNbOfSamplesPerBottle,
											short shSamplingVolumeInMlPerSample,
											Calendar startCalendar,
											short shSamplingIntervalInMin){
			//
			for(byte btBottleCounter = btStartBottleNr; btBottleCounter <= btStopBottleNr; btBottleCounter++ ){
				for( byte btCounter = 0; btCounter < btNbOfSamplesPerBottle; btCounter++){
					this.addSamplingJob(startCalendar.getTime(), btBottleCounter, shSamplingVolumeInMlPerSample);
					startCalendar.add(Calendar.MINUTE, shSamplingIntervalInMin); // increment time
				}
			}
		}
		
		/**
		 * This function returns the schedule as string.
		 * 
		 * @return schedule as string
		 */
		protected String getScheduleAsString(){
			StringBuilder sbSchedule = new StringBuilder();
			
			// sort sampling jobs according data (ascending)
			TreeMap<Date, SamplingJob> ascendingSortedMap = new TreeMap<Date, SamplingJob>(this.mSamplingJob);
			
			// append report status job if defined
			if( this.reportStatus != null ){
				sbSchedule.append(this.reportStatus.toCronJobString()).append("\r\n");
			}
			
			// append sampling jobs in sorted map
			for( Map.Entry<Date, SamplingJob> entry: ascendingSortedMap.entrySet()){
				sbSchedule.append(entry.getValue().toCronJobString()).append("\r\n");
			}
			
			return sbSchedule.toString();
		}
		
		/**
		 * This function adds a new sampling job. The backward tolerance is
		 * set to a constant value (see above). The backward tolerance allows
		 * the plugin in the core station to initiate the sampling even if it 
		 * has been expired by less than the specified constant.
		 * 
		 * @param date: time of sampling
		 * @param btBottleNb: bottle number
		 * @param shVolumeInMl: sampling volume
		 */
		protected void addSamplingJob(	Date date,
									byte btBottleNb,
									short shVolumeInMl){
			this.mSamplingJob.put(date, new SamplingJob(date, btBottleNb, shVolumeInMl, BT_BACKWARD_TOLERANCE_IN_MIN));
		}
		
		/**
		 * This function clears all sampling jobs
		 */
		protected void clearAllSamplingJobs(){
			this.mSamplingJob.clear();
		}
		
		/**
		 * This function replaces the existing report status. The reports status
		 * reports the current state of the 6712 water sampler.
		 * 
		 * @param sMin: minute of crontab
		 * @param sHour: hour of crontab
		 * @param sDayOfMonth: day of month of crontab
		 * @param sMonth: month of crontab
		 */
		protected void replaceReportStatusJob(	String sMin,
											String sHour,
											String sDayOfMonth,
											String sMonth){
			this.reportStatus = new ReportStatus(sMin, sHour, sDayOfMonth, sMonth);
		}
		
		class ReportStatus{
			private String sMin;
			private String sHour;
			private String sDayOfMonth;
			private String sMonth;
			
			/**
			 * This function replaces the existing report status time. 
			 * Use '*' for every minute/hour/day/month
			 * 
			 * @param sMin: minutes (0-59)
			 * @param sHour: hour (0-23)
			 * @param sDayOfMonth: day of month
			 * @param sMonth: month (1-12)
			 */
			protected ReportStatus(String sMin,
					String sHour,
					String sDayOfMonth,
					String sMonth) {
				this.sMin = sMin;
				this.sHour = sHour;
				this.sDayOfMonth = sDayOfMonth;
				this.sMonth = sMonth;
			}
			
			/**
			 * This function returns the report status cron job as string.
			 * 
			 * @return report status cron job as string
			 */
			protected String toCronJobString(){
				StringBuilder sbReportStatus = new StringBuilder();
				
				sbReportStatus.append(this.sMin).append("	");
				sbReportStatus.append(this.sHour).append("	");
				sbReportStatus.append(this.sDayOfMonth).append("	");
				sbReportStatus.append(this.sMonth).append("	");
				sbReportStatus.append("*").append("	"); // day of week
				sbReportStatus.append("plugin Sampler6712Plugin report_status");
				
				return sbReportStatus.toString();
			}
		}
		
		class SamplingJob{
			private Date date;
			private byte btBottleNb;
			private short shVolumeInMl;
			private byte btBackwardToleranceInMinutes;
			
			/**
			 * This function adds a new sampling cron job.
			 * 
			 * @param date: date of sampling
			 * @param btBottleNb: bottle for storing sample
			 * @param shVolumeInMl: sample volume [ml]
			 * @param btBackwardToleranceInMinutes: backward tolerance
			 */
			protected SamplingJob(Date date, byte btBottleNb, short shVolumeInMl, byte btBackwardToleranceInMinutes) {
				this.date = date;
				this.btBottleNb = btBottleNb;
				this.shVolumeInMl = shVolumeInMl;
				this.btBackwardToleranceInMinutes = btBackwardToleranceInMinutes;
			}
			
			/**
			 * This function returns the sampling cron job as string.
			 * 
			 * @return schedule cron job as string
			 */
			protected String toCronJobString(){
				Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")); // use UTC time
				calendar.setTime(this.date);
				
				StringBuilder sbSamplingCronJob = new StringBuilder();
				sbSamplingCronJob.append(String.valueOf(calendar.get(Calendar.MINUTE))).append("	"); //
				sbSamplingCronJob.append(String.valueOf(calendar.get(Calendar.HOUR_OF_DAY))).append("	");
				sbSamplingCronJob.append(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH))).append("	");
				sbSamplingCronJob.append(String.valueOf(calendar.get(Calendar.MONTH)+1)).append("	"); // month starts with 0 (January)
				sbSamplingCronJob.append("*").append("	");
				sbSamplingCronJob.append("plugin Sampler6712Plugin bottle(");
				sbSamplingCronJob.append(String.valueOf(this.btBottleNb)).append(") volume(");
				sbSamplingCronJob.append(String.valueOf(this.shVolumeInMl)).append(")");
				
				if(this.btBackwardToleranceInMinutes > 0){
					sbSamplingCronJob.append(" backward_tolerance_minutes=").append(String.valueOf(this.btBackwardToleranceInMinutes));
				}
				
				return sbSamplingCronJob.toString();
			}
		}
	}
}


