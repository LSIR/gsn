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
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.lang.String;
import java.io.File;
import javax.swing.Timer;

import gsn.Main;
import gsn.http.ClientHttpRequest;

import java.util.Date;

import gsn.beans.DataField;
import gsn.beans.StreamElement;


import org.apache.log4j.Logger;

/**
 * 
 * @author Daniel Burgener
 */
public class Sampler6712ControlAlgorithmVS extends BridgeVirtualSensorPermasense
{
	private static final transient Logger logger = Logger.getLogger(Sampler6712ControlAlgorithmVS.class);
	private static long lMinMaxDeltaThresholdOversteppingTime = 0; // 0 corresponds to min-max delta below threshold
	private static boolean bControlAlgorithmEnableState; // true=enabled
	private static ScheduleStateMachine sm;
	private static int iAutoScheduleGenerationResetTimeInMin; // time after transmitted schedule, state is automatically changed to idle
	private static int iHashCodeOfLastSentScheduleString;
	
	// from xml-file
	private static double dStartConditionThreshold;
	private static int iStartConditionHoldTimeInMin;
	private static int iDestinationDeviceId;
	private static int iCoreStationWakeupTimeoutInMin;
	private static int iCoreStationWakeupRepetitionCnt;
	private static String sScheduleHostName;
	private static String sScheduleVsName;
	private static String sDozerCommandHostName;
	private static String sDozerCommandVsName;
	
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
						DEBUG_GENERATE_SINGLE_SAMPLE_SCHEDULE,
						SCHEDULE_AUTOMATIC_RESET}
	
	private static DataField[] dataField = {	
		new DataField("GENERATION_TIME", "BIGINT"),
		new DataField("TIMESTAMP", "BIGINT"),
		new DataField("DEVICE_ID", "INTEGER"),
		new DataField("DATA_TYPE", "TINYINT"),
		
		// SENSOR DATA
		new DataField("START_CONDITION_THRESHOLD", "DOUBLE"),
		new DataField("START_CONDITION_HOLD_TIME_IN_MIN", "INTEGER"),
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
		
		// read configuration from xml file
		TreeMap <  String , String > params = getVirtualSensorConfiguration( ).getMainClassInitialParams( );
		
		dStartConditionThreshold = Double.parseDouble(params.get("threshold"));
		iStartConditionHoldTimeInMin = Integer.parseInt(params.get("hold_time_in_min"));
		
		iDestinationDeviceId = Integer.parseInt(params.get("destination_device_id"));
		iCoreStationWakeupTimeoutInMin = Integer.parseInt(params.get("core_station_wake_up_timeout_in_min"));
		iCoreStationWakeupRepetitionCnt = Integer.parseInt(params.get("core_station_wake_up_repetition_cnt"));
		
		sScheduleHostName = (String) params.get("schedule_host_name");
		sScheduleVsName = (String) params.get("schedule_vs_name");
		sDozerCommandHostName = (String) params.get("dozer_command_host_name");
		sDozerCommandVsName = (String) params.get("dozer_command_vs_name");
		
		iHashCodeOfLastSentScheduleString = 0;
		
		// init bControlAlgorithmEnableState and iAutoScheduleGenerationResetTimeInMin
		// based on DB
		try {
			logger.info("Get control algorithm enable state and suto schedule generation reset time from DB ");
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
			
				iAutoScheduleGenerationResetTimeInMin = rs.getInt("auto_schedule_generation_reset_time_in_min");
			}
			else{
				iAutoScheduleGenerationResetTimeInMin = 0; // 0 = disable
				bControlAlgorithmEnableState = true;
			}
		} catch (Exception e) {
			logger.error("Reading DB for init of control algorithm enable state and auto schedue generation reset time failed: " + e.getMessage());
			iAutoScheduleGenerationResetTimeInMin = 0; // 0 = disable
			bControlAlgorithmEnableState = true;
		}
		
		logger.info("Control algorithm enable state initialized to: " + bControlAlgorithmEnableState);
		logger.info("Auto schedule generation reset time initialized to: " + iAutoScheduleGenerationResetTimeInMin + "min.");
		
		// create and init state machine
		sm = new ScheduleStateMachine(iCoreStationWakeupRepetitionCnt, 
									  iCoreStationWakeupTimeoutInMin,
									  sScheduleHostName,
									  sScheduleVsName,
									  sDozerCommandHostName,
									  sDozerCommandVsName);
		return ret;
	}

	/**
	 * This function is called when new streaming data is available. This 
	 * virtual sensor consumes streaming data of different type. Therefore
	 * the names of the streams (<stream name="xxx"> in *.xml) has to be
	 * set appropriate.
	 * 
	 */
	@Override
	public void dataAvailable(String inputStreamName, StreamElement data) {
		double dMinValue;
		double dMaxValue;
		double dMinMaxDelta;
		boolean bStartConditionFulfilled = false;
		long lCurrentTime;
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
			// The start condition is fullfilled if the min-max delta is larger than
			// the threshold for a given hold time. The trigger output is cleared
			// a.s.a the min-max delta falls below the threshold.
			
			if(data.getData("minValue") == null |
			   data.getData("maxValue") == null){
				logger.debug("New streaming data from " + inputStreamName + " discarded. minValue: " + data.getData("minValue") + " , maxValue: " + data.getData("maxValue"));
				return;
			}
			
			// read min and max value and calculate delta
			dMinValue = (Double) data.getData("minValue");
			dMaxValue = (Double) data.getData("maxValue");
			dMinMaxDelta = dMaxValue - dMinValue;
			logger.debug("min: " + dMinValue + ", max: " + dMaxValue + ", delta: " + dMinMaxDelta);
			
			// determine if start condition is fulfilled
			if( dMinMaxDelta > dStartConditionThreshold ){
				if( lMinMaxDeltaThresholdOversteppingTime == 0 ){
					// overstepping of threshold
					lMinMaxDeltaThresholdOversteppingTime  = lCurrentTime;
				}
				
				if( lCurrentTime - lMinMaxDeltaThresholdOversteppingTime >= iStartConditionHoldTimeInMin * 60 * 1000 ){
					// min-max delta above threshold for more than hold time
					// --> set start condition fulfilled
					bStartConditionFulfilled = true;
				}
				else {
					bStartConditionFulfilled = false;
				}
			}
			else {
				// reset min max delta threshold overstepping time and clear 
				// start condition fulfilled
				lMinMaxDeltaThresholdOversteppingTime = 0;
				bStartConditionFulfilled = false;
			}
			
			logger.debug("Start condition fulfilled: " + bStartConditionFulfilled + ", overstepping time: " + lMinMaxDeltaThresholdOversteppingTime);
			
			// determine if a new schedule has to be transmitted
			if(bControlAlgorithmEnableState == true & bStartConditionFulfilled == true){
				event = Event.GENERATE_SCHEDULE;
				sm.putEvent(event);
				
				// generate output stream
				data = new StreamElement(dataField, new Serializable[] {lCurrentTime, lCurrentTime, iDestinationDeviceId, BT_CONTROL_TYPE, null, null, null, null, null, null, bControlAlgorithmEnableState? (byte)1:(byte)0, event.toString(), sm.getState().toString(), iAutoScheduleGenerationResetTimeInMin} );
				super.dataAvailable(inputStreamName, data);
			}
			
			// generate output stream
			data = new StreamElement(dataField, new Serializable[] {lCurrentTime, lCurrentTime, iDestinationDeviceId, BT_SENSOR_DATA_TYPE, dStartConditionThreshold, iStartConditionHoldTimeInMin, bStartConditionFulfilled? (byte)1:(byte)0, dMinValue, dMaxValue, dMinMaxDelta, null, null, null, null} );
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
			
			// generate output stream
			data = new StreamElement(dataField, new Serializable[] {lCurrentTime, lCurrentTime, iDestinationDeviceId, BT_CONTROL_TYPE, null, null, null, null, null, null, bControlAlgorithmEnableState? (byte)1:(byte)0, event.toString(), sm.getState().toString(), iAutoScheduleGenerationResetTimeInMin} );
			super.dataAvailable(inputStreamName, data);
		}
		else if ( inputStreamName.toLowerCase().contentEquals("dozer_command") ){
			// new schedule data received
			logger.debug("Dozer command send to core station");
		}
		else if ( inputStreamName.toLowerCase().contentEquals("sampler6712_sampling") ){
			// 
		}
		else if ( inputStreamName.toLowerCase().contentEquals("sampler6712_status") ){
			// 
		}
		else {
			logger.warn("Unknown input stream element received: '"+ inputStreamName + "'. Check stream names in VS description file (*.xml)");
		}
	}
	
	@Override
	public boolean dataFromWeb(String command, String[] paramNames, Serializable[] paramValues) {
		Event event = Event.NONE;
		
		long lCurrentTime = System.currentTimeMillis();
		
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
				else{
					bControlAlgorithmEnableState = false;
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
				
				// generate output stream
				StreamElement data = new StreamElement(dataField, new Serializable[] {lCurrentTime, lCurrentTime, iDestinationDeviceId, BT_CONTROL_TYPE, null, null, null, null, null, null, bControlAlgorithmEnableState? (byte)1:(byte)0, event.toString(), sm.getState().toString(), iAutoScheduleGenerationResetTimeInMin} );
				dataProduced( data );
				
				return true;
			}
			catch(Exception e){
				logger.warn("Invalid input values, input not treated: " +e);
				return false;
			}
		}
		else if( command.compareToIgnoreCase("debug") == 0 ) {
			String sAutoScheduleGenerationResetTime = "", sSpecialAction = "";
			// read fields
			for (int i = 0 ; i < paramNames.length ; i++) {
				if( paramNames[i].compareToIgnoreCase("auto_schedule_generation_reset_time") == 0 ) {
					sAutoScheduleGenerationResetTime = (String)paramValues[i];
				}
				
				if( paramNames[i].compareToIgnoreCase("special_action") == 0 ) {
					sSpecialAction = (String)paramValues[i];
				}
			}
			
			// convert fields
			try{
				// convert auto schedule generation reset time
				iAutoScheduleGenerationResetTimeInMin = Integer.parseInt(sAutoScheduleGenerationResetTime);
				
				if( sSpecialAction.equalsIgnoreCase("none")){
					event = Event.NONE;
				}
				else if( sSpecialAction.equalsIgnoreCase("start_single_sampling_schedule_generation")){
					event = Event.DEBUG_GENERATE_SINGLE_SAMPLE_SCHEDULE;
				}
				else{
					logger.warn("Unknown special action");
					return false;
				}
				
				// treat event
				sm.putEvent(event);
				
				// generate output stream
				StreamElement data = new StreamElement(dataField, new Serializable[] {lCurrentTime, lCurrentTime, iDestinationDeviceId, BT_CONTROL_TYPE, null, null, null, null, null, null, bControlAlgorithmEnableState? (byte)1:(byte)0, event.toString(), sm.getState().toString(), iAutoScheduleGenerationResetTimeInMin} );
				dataProduced( data );
				
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
		private Timer timeoutTimerAutoScheduleReset;
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
			long lCurrentTime = System.currentTimeMillis();
			
			// convert actionEvent into event (enum)
			Event event = Event.valueOf(actionEvent.getActionCommand());
			logger.debug("actionPerformed event: " + event);
			putEvent(event); // send event
			
			// generate output stream
			StreamElement data = new StreamElement(dataField, new Serializable[] {lCurrentTime, lCurrentTime, iDestinationDeviceId, BT_CONTROL_TYPE, null, null, null, null, null, null, bControlAlgorithmEnableState? (byte)1:(byte)0, event.toString(), sm.getState().toString(), iAutoScheduleGenerationResetTimeInMin} );
			dataProduced( data );
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
			if( this.state != state ){
				switch(this.state){
					case SCHEDULE_TRANSMITTED:
						if( this.timeoutTimerAutoScheduleReset != null ){
							if( this.timeoutTimerAutoScheduleReset.isRunning()){
								this.timeoutTimerAutoScheduleReset.stop();
								logger.debug("Stop schedule auto reset timer");
							}
						}
				}
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
						calendar.add(Calendar.MINUTE, 15); // start in 15min with sampling
						
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
															  (short)30); // sampling intervall [min]
								break;
								
							case STATIC_SCHEDULE:
								// generate schedule
								schedule = new Schedule();
								schedule.replaceReportStatusJob("0", "9", "*", "*");
								schedule.addSamplingJobSeries((byte)1, 	// bottle start nb
															  (byte)24, // bottle stop nb
															  (byte)1, 	// nb of samplings per bottle
															  (short)10, // sampling volume [ml]
															  calendar, // start time
															  (short)30); // sampling intervall [min]
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
					if( iAutoScheduleGenerationResetTimeInMin != 0 ){
						// start schedule transmitted timeout
						this.timeoutTimerAutoScheduleReset= new Timer( iAutoScheduleGenerationResetTimeInMin * 60 * 1000 , this );
						this.timeoutTimerAutoScheduleReset.setActionCommand(Event.SCHEDULE_AUTOMATIC_RESET.toString()); // action command is event enum
						this.timeoutTimerAutoScheduleReset.setRepeats(false); // use timer as one shot timer
						this.timeoutTimerAutoScheduleReset.start( );
					}
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
			
			// return if event is none
			if(event == Event.NONE){
				return; 
			}
			
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
							
						// only for debugging
						case DEBUG_GENERATE_SINGLE_SAMPLE_SCHEDULE:
							this.iTimeoutCntScheduleAckReceived = 0;  // clear timeout counter
							newState = State.SCHEDULE_GENERATION;
							aoSetStateParameter = ScheduleType.SINGLE_SAMPLING_DEBUG_SCHEDULE;
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
					}
						
					break;
					
				case SCHEDULE_TRANSMITTED:
					switch(event){
						case SCHEDULE_RESET:
						case SCHEDULE_AUTOMATIC_RESET:
							newState = State.IDLE;
							break;
					}
					break;
			}
			
			// check if state has to be changed or not
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
		
		/**
		 * This function returns the current state of the state machine. 
		 * 
		 * @return current state
		 */
		protected State getState(){
			return this.state;
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


