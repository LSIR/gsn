package gsn.vsensor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.Map.Entry;

import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;

import org.apache.log4j.Logger;

public class StreamMergingVirtualSensor extends BridgeVirtualSensorPermasense {
	
	private static final String BUFFER_SIZE_IN_DAYS = "maximum_buffered_stream_age_in_days";
	private static final String NUMBER_OF_STREAMS_PER_MERGE_BUCKET = "streams_needed_to_merge";
	private static final String TIMELINE = "timeline";
	private static final String MERGE_BUCKET_SIZE_IN_MINUTES = "merge_bucket_size_in_minutes";
	private static final String MERGE_BUCKET_EDGE_TYPE = "merge_bucket_edge_type";
	private static final String MATCHING_FIELD1 = "matching_field1";
	private static final String MATCHING_FIELD2 = "matching_field2";
	private static final String DEFAULT_MERGE_OPERATOR = "default_merge_operator";
	private static final String FILTER_DUPLICATES = "filter_duplicates";
	private static final String DUPLICATES_IGNORE_FIELDS = "duplicates_ignore_field";

	private static final Long CLEANUP_TIMER_PERIOD = 86400000L;
	
	private static enum BucketEdgeType {
		STATIC, DYNAMIC
	}
	
	private static Map<String,BucketEdgeType> BUCKET_EdgeType = new HashMap<String,BucketEdgeType>();
    static
    {
    	BUCKET_EdgeType.put("static", BucketEdgeType.STATIC);
    	BUCKET_EdgeType.put("dynamic", BucketEdgeType.DYNAMIC);
    }
	
	private static enum Operator {
		NEW, OLD, ADD, AVG, MIN, MAX
	}
	
	private static Map<String,Operator> MERGE_Operator = new HashMap<String,Operator>();
    static
    {
    	MERGE_Operator.put("new", Operator.NEW);
    	MERGE_Operator.put("old", Operator.OLD);
    	MERGE_Operator.put("add", Operator.ADD);
    	MERGE_Operator.put("avg", Operator.AVG);
    	MERGE_Operator.put("min", Operator.MIN);
    	MERGE_Operator.put("max", Operator.MAX);
    }
	
	private static final transient Logger logger = Logger.getLogger(StreamMergingVirtualSensor.class);

	private Map<Serializable,Map<Serializable,ArrayList<StreamElementContainer>>> streamElementBuffer = new HashMap<Serializable,Map<Serializable,ArrayList<StreamElementContainer>>>();
	private Map<String,Operator> FieldNameToOperatorMap = new HashMap<String,Operator>();
	private Map<String,Integer> validatedStreams = new HashMap<String,Integer>();
	private Long bufferSizeInMs;
	private Integer bucketSpace;
	private String timeline;
	private Long bucketSizeInMs = null;
	private BucketEdgeType bucketEdgeType = null;
	private String matchingFieldName1 = null;
	private String matchingFieldName2 = null;
	private Operator defaultMergeOperator = null;
	private boolean filterDuplicates = false;
	private String[] duplicatesIgnoreFields = null;
	private DataField[] mergedDataFields;
	Timer cleanupTimer = new Timer();

	private static final DataField[] statisticsDataFields = {
		new DataField("MERGED_STREAMS", "INTEGER"),
		new DataField("BUFFERED_STREAMS_FOR_MATCHING_FIELD1", "BIGINT"),
		new DataField("TOTAL_BUFFERED_STREAMS", "BIGINT")
	};
	
	@Override
	public boolean initialize() {
		boolean ret = super.initialize();
		VSensorConfig vsensor = getVirtualSensorConfiguration();
		TreeMap<String,String> params = vsensor.getMainClassInitialParams();
		ArrayList<DataField> mergedFieldList = new ArrayList<DataField>();
		String bucketEdgeTypeStr = null;
		ArrayList<String> duplIgnoreFields = new ArrayList<String>();
		
		for (Entry<String,String> entry: params.entrySet()) {
			String paramName = entry.getKey().trim().toLowerCase();
			String value = entry.getValue().trim().toLowerCase();
			if (paramName.compareToIgnoreCase(BUFFER_SIZE_IN_DAYS) == 0) {
				try {
					bufferSizeInMs = Long.decode(value) * 86400000L;
				}
				catch (NumberFormatException e) {
					logger.error(BUFFER_SIZE_IN_DAYS + " has to be an integer");
					return false;
				}
			}
			else if (paramName.compareToIgnoreCase(MERGE_BUCKET_SIZE_IN_MINUTES) == 0) {
				try {
					bucketSizeInMs = Long.decode(value) * 60000L;
				}
				catch (NumberFormatException e) {
					logger.error(MERGE_BUCKET_SIZE_IN_MINUTES + " has to be an integer");
					return false;
				}
			}
			else if (paramName.compareToIgnoreCase(NUMBER_OF_STREAMS_PER_MERGE_BUCKET) == 0) {
				try {
					this.bucketSpace = Integer.decode(value);
				}
				catch (NumberFormatException e) {
					logger.error(NUMBER_OF_STREAMS_PER_MERGE_BUCKET + " has to be an integer");
					return false;
				}
				
			}
			else if (paramName.compareToIgnoreCase(MERGE_BUCKET_EDGE_TYPE) == 0) {
				if (BUCKET_EdgeType.get(value) == null) {
					logger.error("merge bucket edge type (" + paramName + "=" + value + ") unknown");
					return false;
				}
				bucketEdgeTypeStr = value;
				bucketEdgeType =  BUCKET_EdgeType.get(value);
			}
			else if (paramName.compareToIgnoreCase(TIMELINE) == 0) {
				if (!isInOutputStructure(value)) {
					logger.error(TIMELINE + " (" + value + ") can not be found in output structure");
					return false;
				}
				timeline = value;
			}
			else if (paramName.compareToIgnoreCase(MATCHING_FIELD1) == 0) {
				if (!isInOutputStructure(value)) {
					logger.error(MATCHING_FIELD1 + " (" + value + ") can not be found in output structure");
					return false;
				}
				matchingFieldName1 = value;
			}
			else if (paramName.compareToIgnoreCase(MATCHING_FIELD2) == 0) {
				if (!isInOutputStructure(value)) {
					logger.error(MATCHING_FIELD2 + " (" + value + ") can not be found in output structure");
					return false;
				}
				matchingFieldName2 = value;
			}
			else if (paramName.compareToIgnoreCase(FILTER_DUPLICATES) == 0) {
				filterDuplicates = Boolean.parseBoolean(value);
			}
			else if (paramName.toLowerCase().startsWith(DUPLICATES_IGNORE_FIELDS)) {
				String postfix = paramName.replaceFirst(DUPLICATES_IGNORE_FIELDS, "");
				if (!isInOutputStructure(value)) {
					logger.error(DUPLICATES_IGNORE_FIELDS + postfix + " (" + value + ") can not be found in output structure");
					return false;
				}
				duplIgnoreFields.add(value);
			}
			else if (paramName.compareToIgnoreCase(DEFAULT_MERGE_OPERATOR) == 0) {
				if (MERGE_Operator.get(value) == null) {
					logger.error("default merge operator (" + paramName + "=" + value + ") unknown");
					return false;
				}
				defaultMergeOperator =  MERGE_Operator.get(value);
			}
			else {
				if (!isInOutputStructure(paramName)) {
					logger.error(paramName + " can not be found in output structure");
					return false;
				}
				if (MERGE_Operator.get(value) == null) {
					logger.error("merge operator (" + paramName + "=" + value + ") unknown");
					return false;
				}
				FieldNameToOperatorMap.put(paramName, MERGE_Operator.get(value));
			}
		}
		
		if (bufferSizeInMs == null) {
			logger.error(BUFFER_SIZE_IN_DAYS + " parameter has to be spezified");
			return false;
		}
		if (bucketSpace == null) {
			logger.error(NUMBER_OF_STREAMS_PER_MERGE_BUCKET + " parameter has to be spezified");
			return false;
		}
		if (timeline == null) {
			logger.error(TIMELINE + " parameter has to be spezified");
			return false;
		}
		
		if (!(bucketSizeInMs == null && bucketEdgeType == null) && (bucketSizeInMs == null || bucketEdgeType == null)) {
			logger.error(MERGE_BUCKET_EDGE_TYPE + " and " + MERGE_BUCKET_SIZE_IN_MINUTES + " can not be used seperately");
			return false;
		}

		// check if Operator is available and matches field type
		for (DataField d: getVirtualSensorConfiguration().getOutputStructure()) {
			boolean cont = false;
			for (DataField df: statisticsDataFields) {
				if (d.getName().compareToIgnoreCase(df.getName()) == 0) {
					cont = true;
					break;
				}
			}
			if (cont)
				continue;
			
			if (d.getName().compareToIgnoreCase(timeline) == 0) {
				if (d.getDataTypeID() != DataTypes.BIGINT) {
					logger.error(TIMELINE + " (" + timeline + ") field type (" + d.getType() + ") must be BIGINT");
					return false;
				}
			}
			else if (matchingFieldName1 != null && d.getName().compareToIgnoreCase(matchingFieldName1) == 0) {
				if (d.getDataTypeID() == DataTypes.BINARY) {
					logger.error("binary matching is not supported (" + MATCHING_FIELD1 + ")");
					return false;
				}
			}
			else if (matchingFieldName2 != null && d.getName().compareToIgnoreCase(matchingFieldName2) == 0) {
				if (d.getDataTypeID() == DataTypes.BINARY) {
					logger.error("binary matching is not supported (" + MATCHING_FIELD2 + ")");
					return false;
				}
			}
			
			Operator op = getOperator(d.getName());
			if (op == null) {
				logger.error("No operator specified for output field " + d.getName() + " (you may use " + DEFAULT_MERGE_OPERATOR + " parameter)");
				return false;
			}
			
			boolean match = false;
			switch (op) {
			case ADD:
				if (d.getDataTypeID() == DataTypes.VARCHAR ||
				    d.getDataTypeID() == DataTypes.CHAR ||
				    d.getDataTypeID() == DataTypes.BINARY)
					match = true;
			case AVG:
			case MIN:
			case MAX:
				switch (d.getDataTypeID()) {
				case DataTypes.DOUBLE:
				case DataTypes.BIGINT:
				case DataTypes.INTEGER:
				case DataTypes.SMALLINT:
				case DataTypes.TINYINT:
					match = true;
					break;
				default:
					break;
				}
				break;
			case NEW:
			case OLD:
				match = true;
				break;
			default:
				break;
			}
			
			if (!match) {
				String opName = null;
				for (Entry<String,Operator> entry: MERGE_Operator.entrySet()) {
					if (entry.getValue() == op) {
						opName = entry.getKey();
						break;
					}
				}
				logger.error("merge operator (" + opName + ") can not be used with field " + d.getName() + " (field type: " + d.getType() + ")");
				return false;
			}
			
			mergedFieldList.add(d);
		}
		mergedDataFields = new DataField[mergedFieldList.size()];
		mergedFieldList.toArray(mergedDataFields);
		
		logger.info("Maximum buffered stream age: " + bufferSizeInMs + "ms");
		logger.info("Number of streams needed to merge: " + bucketSpace);
		logger.info("Timeline: " + timeline);
		if (bucketSizeInMs != null) {
			logger.info("Bucket size: " + bucketSizeInMs + "ms");
			logger.info("Bucket edge type: " + bucketEdgeTypeStr);
		}
		else
			logger.info("Timed bucket sorting not used");
		if (defaultMergeOperator != null)
			logger.info("Default merge operator: " + defaultMergeOperator);
		else
			logger.info("No default merge operator specified");
		if (matchingFieldName1 != null)
			logger.info("Matching field 1: " + matchingFieldName1);
		else
			logger.info("No matching field 1 specified");
		if (matchingFieldName2 != null)
			logger.info("Matching field 2: " + matchingFieldName2);
		else
			logger.info("No matching field 2 specified");
		if (filterDuplicates)
			logger.info("Duplicated streams will be filtered");
		else
			logger.info("Duplicated streams will not be filtered");
		if (!duplIgnoreFields.isEmpty()) {
			if (filterDuplicates) {
				duplicatesIgnoreFields = new String[duplIgnoreFields.size()];
				duplIgnoreFields.toArray(duplicatesIgnoreFields);
				for (String s: duplIgnoreFields)
					logger.info("field " + s + " will be ignorded during duplicate filtering");
			}
			else
				logger.warn(DUPLICATES_IGNORE_FIELDS + " has no effect without enabling " + FILTER_DUPLICATES);
		}
		
		logger.info("Merging list:");
		for (DataField df: mergedFieldList) {
			logger.info("	" + df.getName() + ": " + getOperator(df.getName()));
		}
		
		// start clean-up timer
		CleanupTimerTask timerTask = new CleanupTimerTask();
    	cleanupTimer.scheduleAtFixedRate(timerTask, CLEANUP_TIMER_PERIOD, CLEANUP_TIMER_PERIOD);
		
		return ret;
	}
	
	private boolean isInOutputStructure(String value) {
		for (DataField d: getVirtualSensorConfiguration().getOutputStructure()) {
			if (d.getName().compareToIgnoreCase(value) == 0) {
				return true;
			}
		}
		return false;
	}
	
	private Operator getOperator(String fieldName) {
		Operator op = FieldNameToOperatorMap.get(fieldName);
		if (op == null)
			op = defaultMergeOperator;
		return op;
	}
	
	@Override
	public void dataAvailable(String inputStreamName, StreamElement data) {
		Serializable match1 = null;
		if (matchingFieldName1 != null)
			match1 = data.getData(matchingFieldName1);
		try {	
			Long startTime = 0L;
			if (logger.isDebugEnabled())
				startTime = System.currentTimeMillis();

			// check source for correctness
			if (!validatedStreams.containsKey(inputStreamName)) {
				if(!isStreamValid(data)) {
					logger.error("Input stream (" + inputStreamName + ") is not valide -> will not get processed");
					return;
				}
				else
					logger.info(inputStreamName + " successfully validated");
				validatedStreams.put(inputStreamName, 1);
			}
			
			if (!streamElementBuffer.containsKey(match1)) {
				streamElementBuffer.put(match1, Collections.synchronizedMap(new HashMap<Serializable,ArrayList<StreamElementContainer>>()));
				if (logger.isDebugEnabled() && match1 != null)
					logger.debug("New StreamElement buffer created for " + matchingFieldName1 + " " + match1);
			}

			if (((Long)data.getData(timeline)).compareTo(System.currentTimeMillis()-bufferSizeInMs) > 0) {
				processPerDeviceData(inputStreamName, data, streamElementBuffer.get(match1));
			}
			else {
				super.dataAvailable("mergedStream", new StreamElement(data, statisticsDataFields, generateStats(null, data)));
			}
			
			if (logger.isDebugEnabled())
				logger.debug("merge time: " + (System.currentTimeMillis()-startTime) + "ms");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	private boolean isStreamValid(StreamElement data) {
		for (DataField df: mergedDataFields) {
			if (data.getType(df.getName()) == null) {
				logger.error("there is no data field available in the stream element for " + df.getName());
				return false;
			}
		}
		return true;
	}

	private void processPerDeviceData(String inputStreamName, StreamElement data, Map<Serializable,ArrayList<StreamElementContainer>> streamElementContainerMap) throws Exception {
		Serializable match2 = null;
		if (matchingFieldName2 != null)
			match2 = data.getData(matchingFieldName2);

		ArrayList<StreamElementContainer> streamElementContainerList = streamElementContainerMap.get(match2);
		
		if (streamElementContainerList == null) {
			streamElementContainerList = new ArrayList<StreamElementContainer>();
			if (logger.isDebugEnabled() && matchingFieldName2 != null)
				logger.debug("New StreamElementContainer list created for " + matchingFieldName2 + " " + match2);
		}

		Iterator<StreamElementContainer> iter = streamElementContainerList.iterator();
		while (iter.hasNext()) {
			StreamElementContainer sec = iter.next();
			
			if (sec.checkTime((Long)data.getData(timeline))) {
				if (sec.putStreamElement(data)) {
					int mergedStreams = sec.getNumberOfStreams();
					StreamElement newSE = sec.getMergedStreamElement();
					iter.remove();
					if (streamElementContainerMap.isEmpty()) {
						streamElementContainerMap.remove(match2);
					}
					else {
						streamElementContainerMap.put(match2, streamElementContainerList);
					}

					super.dataAvailable("mergedStream", new StreamElement(newSE, statisticsDataFields, generateStats(mergedStreams, newSE)));
				}
				else {
					streamElementContainerMap.put(match2, streamElementContainerList);
				}
				return;
			}
		}
		
		StreamElementContainer sec = new StreamElementContainer(inputStreamName, data);
		streamElementContainerList.add(sec);
		streamElementContainerMap.put(match2, streamElementContainerList);
	}

	@Override
	public synchronized void dispose() {
		cleanupTimer.cancel();
		cleanupBuffers(true);
		super.dispose();
	}
	
	private void cleanupBuffers(boolean cleanupAll) {
		Iterator<Map<Serializable,ArrayList<StreamElementContainer>>> iter1 = streamElementBuffer.values().iterator();
		while (iter1.hasNext()) {
			Map<Serializable,ArrayList<StreamElementContainer>> buf = iter1.next();
			Iterator<ArrayList<StreamElementContainer>> iter2 = buf.values().iterator();
			while (iter2.hasNext()) {
				ArrayList<StreamElementContainer> secList = iter2.next();
				Iterator<StreamElementContainer> iter3 = secList.iterator();
				while (iter3.hasNext()) {
					StreamElementContainer sec = iter3.next();
					if (cleanupAll || (sec.getNewestTimestamp().compareTo((System.currentTimeMillis()-bufferSizeInMs))) < 0) {
						StreamElement mergedSE = sec.getMergedStreamElement();
						int numberOfStreams = sec.getNumberOfStreams();
						iter3.remove();
						super.dataAvailable("mergedStream", new StreamElement(mergedSE, statisticsDataFields, generateStats(numberOfStreams, mergedSE)));
					}
				}
				if (secList.isEmpty())
					iter2.remove();
			}
			if (buf.isEmpty())
				iter1.remove();
		}
	}

	private Serializable[] generateStats(Integer mergedStreams, StreamElement se) {
		Serializable [] stats = new Serializable [statisticsDataFields.length];
		// MERGED_STREAMS
		stats[0] = mergedStreams;
		
		stats[1] = stats[2] = 0;
		for (Entry<Serializable, Map<Serializable, ArrayList<StreamElementContainer>>> match1Buffer: streamElementBuffer.entrySet()) {
			for (ArrayList<StreamElementContainer> secList: match1Buffer.getValue().values()) {
				for (StreamElementContainer sec: secList) {
					if (matchingFieldName1 != null) {
						if (match1Buffer.getKey().equals(se.getData(matchingFieldName1))) {
							// BUFFERED_STREAMS_FOR_MATCHING_FIELD1
							stats[1] = ((Integer)stats[1]) + sec.getNumberOfStreams();
						}
					}
					else
						stats[1] = ((Integer)stats[1]) + sec.getNumberOfStreams();
					// TOTAL_BUFFERED_STREAMS
					stats[2] = ((Integer)stats[2]) + sec.getNumberOfStreams();
				}
			}
		}
		return stats;
	}
	
	class StreamElementContainer {
		private ArrayList<StreamElement> streamElements;
		private StreamElement newSE;
		private StreamElement oldSE;
		private Long timestampSE = null;
		private Long bucketStartTime = null;
		private Long bucketEndTime = null;
		
		protected StreamElementContainer(String inputStreamName, StreamElement streamElement) {
			streamElements = new ArrayList<StreamElement>(bucketSpace);
			streamElements.add(streamElement);
			newSE = streamElement;
			oldSE = streamElement;
			timestampSE = streamElement.getTimeStamp();
			
			if (bucketSizeInMs != null) {
				Long time = (Long) streamElement.getData(timeline);
				switch (bucketEdgeType) {
				case STATIC:
					bucketStartTime = time-(time%bucketSizeInMs);
					bucketEndTime = bucketStartTime+bucketSizeInMs;
					break;
				case DYNAMIC:
					bucketStartTime = time-(bucketSizeInMs/2);
					bucketEndTime = bucketStartTime+bucketSizeInMs;
					break;
				}
			}
		}

		protected boolean checkTime(Long incomingTime) {
			return (bucketStartTime == null) || ((incomingTime >= bucketStartTime) && (incomingTime < bucketEndTime));
		}

		protected boolean putStreamElement(StreamElement streamElement) throws Exception {
			if (streamElements.size() == bucketSpace)
				throw new Exception("StreamElementContainer already full!");

			if (filterDuplicates) {
				// discard duplicates
				for (StreamElement se : streamElements) {
					if (se.equalsIgnoreTimedAndFields(streamElement, duplicatesIgnoreFields)) {
						logger.debug("discard duplicate in StreamElement container: [" + streamElement.toString() + "]");
						return false;
					}
				}
			}
			
			streamElements.add(streamElement);

			if (bucketEdgeType != null && bucketEdgeType == BucketEdgeType.DYNAMIC) {
				long time = 0;
				for (StreamElement se: streamElements)
					time += (Long)se.getData(timeline);
				time = time/streamElements.size();
				bucketStartTime = time-(bucketSizeInMs/2);
				bucketEndTime = bucketStartTime+bucketSizeInMs;
			}
			
			if (streamElement.getTimeStamp() > timestampSE)
				timestampSE = streamElement.getTimeStamp();
			if (((Long)streamElement.getData(timeline)).compareTo((Long)newSE.getData(timeline)) > 0)
				newSE = streamElement;
			if (((Long)streamElement.getData(timeline)).compareTo((Long)oldSE.getData(timeline)) < 0)
				oldSE = streamElement;
			if (streamElements.size() == bucketSpace)
				return true;
			else
				return false;
		}
		
		protected int getNumberOfStreams() {
			return streamElements.size();
		}
		
		protected Long getNewestTimestamp() {
			return (Long)newSE.getData(timeline);
		}
		
		protected StreamElement getMergedStreamElement() {
			Serializable [] mergedData = new Serializable [mergedDataFields.length];

			for (int i=0; i<mergedDataFields.length; i++) {
				switch (getOperator(mergedDataFields[i].getName())) {
				case NEW:
					mergedData[i] = newFnc(mergedDataFields[i]);
					break;
				case OLD:
					mergedData[i] = old(mergedDataFields[i]);
					break;
				case ADD:
					mergedData[i] = add(mergedDataFields[i]);
					break;
				case AVG:
					mergedData[i] = avg(mergedDataFields[i]);
					break;
				case MIN:
					mergedData[i] = min(mergedDataFields[i]);
					break;
				case MAX:
					mergedData[i] = max(mergedDataFields[i]);
					break;
				}
			}
			
			return new StreamElement(mergedDataFields, mergedData, timestampSE);
		}

		private Serializable newFnc(DataField dataField) {
			return newSE.getData(dataField.getName());
		}
		
		private Serializable old(DataField dataField) {
			return oldSE.getData(dataField.getName());
		}

		private Serializable add(DataField d) {
			Serializable toReturn = null;
			switch (d.getDataTypeID()) {
			case DataTypes.DOUBLE:
				for (StreamElement se: streamElements) {
					Serializable srl = se.getData(d.getName());
					if (srl != null) {
						if (toReturn == null)
							toReturn = (Double)srl;
						else
							toReturn = (Double)toReturn + (Double)srl;
					}
				}
				break;
			case DataTypes.BIGINT:
				for (StreamElement se: streamElements) {
					Serializable srl = se.getData(d.getName());
					if (srl != null) {
						if (toReturn == null)
							toReturn = (Long)srl;
						else
							toReturn = (Long)toReturn + (Long)srl;
					}
				}
				break;
			case DataTypes.INTEGER:
				for (StreamElement se: streamElements) {
					Serializable srl = se.getData(d.getName());
					if (srl != null) {
						if (toReturn == null)
							toReturn = (Integer)srl;
						else
							toReturn = (Integer)toReturn + (Integer)srl;
					}
				}
				break;
			case DataTypes.SMALLINT:
				for (StreamElement se: streamElements) {
					Serializable srl = se.getData(d.getName());
					if (srl != null) {
						if (toReturn == null)
							toReturn = srl;
						else
							toReturn = (short)((Short)toReturn + (Short)srl);
					}
				}
				break;
			case DataTypes.TINYINT:
				for (StreamElement se: streamElements) {
					Serializable srl = se.getData(d.getName());
					if (srl != null) {
						if (toReturn == null)
							toReturn = (Byte)srl;
						else
							toReturn = (byte)((Byte)toReturn + (Byte)srl);
					}
				}
				break;
			case DataTypes.CHAR:
			case DataTypes.VARCHAR:
				for (StreamElement se: streamElements) {
					Serializable srl = se.getData(d.getName());
					if (srl != null) {
						if (toReturn == null)
							toReturn = (String)srl;
						else
							toReturn = ((String)toReturn).concat((String)srl);
					}
				}
				break;
			case DataTypes.BINARY:
				for (StreamElement se: streamElements) {
					Serializable srl = se.getData(d.getName());
					if (srl != null) {
						if (toReturn == null)
							toReturn = (byte[])srl;
						else {
							int mergedDataLength = ((byte[])toReturn).length;
							int srlLength = ((byte[])srl).length;
							byte[] concat = new byte[mergedDataLength + srlLength];
							System.arraycopy(toReturn, 0, concat, 0, mergedDataLength);
							System.arraycopy(srl, 0, concat, mergedDataLength, srlLength);
							toReturn = concat;
						}
					}
				}
				break;
			default:
				logger.error("type id " + d.getDataTypeID() + " not allowed with ADD operator");
				break;
			}
			return toReturn;
		}

		private Serializable avg(DataField d) {
			Serializable toReturn = null;
			int divider = 0;
			switch (d.getDataTypeID()) {
			case DataTypes.DOUBLE:
				for (StreamElement se: streamElements) {
					Serializable srl = se.getData(d.getName());
					if (srl != null) {
						divider++;
						if (toReturn == null)
							toReturn = (Double)srl;
						else
							toReturn = (Double)toReturn + (Double)srl;
					}
				}
				if (toReturn != null)
					toReturn = (Double)toReturn / new Double(divider);
				break;
			case DataTypes.BIGINT:
				for (StreamElement se: streamElements) {
					Serializable srl = se.getData(d.getName());
					if (srl != null) {
						divider++;
						if (toReturn == null)
							toReturn = (Long)srl;
						else
							toReturn = (Long)toReturn + (Long)srl;
					}
				}
				if (toReturn != null)
					toReturn = (Long)((Long)toReturn / new Long(divider));
				break;
			case DataTypes.INTEGER:
				for (StreamElement se: streamElements) {
					Serializable srl = se.getData(d.getName());
					if (srl != null) {
						divider++;
						if (toReturn == null)
							toReturn = new Long((Integer)srl);
						else
							toReturn = (Long)toReturn + new Long((Integer)srl);
					}
				}
				if (toReturn != null)
					toReturn = (Integer)((Long)((Long)toReturn / new Long(divider))).intValue();
				break;
			case DataTypes.SMALLINT:
				for (StreamElement se: streamElements) {
					Serializable srl = se.getData(d.getName());
					if (srl != null) {
						divider++;
						if (toReturn == null)
							toReturn = new Long((Short)srl);
						else
							toReturn = (Long)toReturn + new Long((Short)srl);
					}
				}
				if (toReturn != null)
					toReturn = (Short)((Long)((Long)toReturn / new Long(divider))).shortValue();
				break;
			case DataTypes.TINYINT:
				for (StreamElement se: streamElements) {
					Serializable srl = se.getData(d.getName());
					if (srl != null) {
						divider++;
						if (toReturn == null)
							toReturn = new Long((Byte)srl);
						else
							toReturn = (Long)toReturn + new Long((Byte)srl);
					}
				}
				if (toReturn != null)
					toReturn = (Byte)((Long)((Long)toReturn / new Long(divider))).byteValue();
				break;
			default:
				logger.error("type id " + d.getDataTypeID() + " not allowed with AVG operator");
				break;
			}
			return toReturn;
		}

		private Serializable min(DataField d) {
			Serializable toReturn = null;
			switch (d.getDataTypeID()) {
			case DataTypes.DOUBLE:
				for (StreamElement se: streamElements) {
					Serializable srl = se.getData(d.getName());
					if (srl != null && (toReturn == null || (Double)srl < (Double)toReturn))
							toReturn = (Double)srl;
				}
				break;
			case DataTypes.BIGINT:
				for (StreamElement se: streamElements) {
					Serializable srl = se.getData(d.getName());
					if (srl != null && (toReturn == null || (Long)srl < (Long)toReturn))
							toReturn = (Long)srl;
				}
				break;
			case DataTypes.INTEGER:
				for (StreamElement se: streamElements) {
					Serializable srl = se.getData(d.getName());
					if (srl != null && (toReturn == null || (Integer)srl < (Integer)toReturn))
							toReturn = (Integer)srl;
				}
				break;
			case DataTypes.SMALLINT:
				for (StreamElement se: streamElements) {
					Serializable srl = se.getData(d.getName());
					if (srl != null && (toReturn == null || (Short)srl < (Short)toReturn))
							toReturn = srl;
				}
				break;
			case DataTypes.TINYINT:
				for (StreamElement se: streamElements) {
					Serializable srl = se.getData(d.getName());
					if (srl != null && (toReturn == null || (Byte)srl < (Byte)toReturn))
							toReturn = (Byte)srl;
				}
				break;
			default:
				logger.error("type id " + d.getDataTypeID() + " not allowed with MIN operator");
				break;
			}
			return toReturn;
		}

		private Serializable max(DataField d) {
			Serializable toReturn = null;
			switch (d.getDataTypeID()) {
			case DataTypes.DOUBLE:
				for (StreamElement se: streamElements) {
					Serializable srl = se.getData(d.getName());
					if (srl != null && (toReturn == null || (Double)srl > (Double)toReturn))
							toReturn = (Double)srl;
				}
				break;
			case DataTypes.BIGINT:
				for (StreamElement se: streamElements) {
					Serializable srl = se.getData(d.getName());
					if (srl != null && (toReturn == null || (Long)srl > (Long)toReturn))
							toReturn = (Long)srl;
				}
				break;
			case DataTypes.INTEGER:
				for (StreamElement se: streamElements) {
					Serializable srl = se.getData(d.getName());
					if (srl != null && (toReturn == null || (Integer)srl > (Integer)toReturn))
							toReturn = (Integer)srl;
				}
				break;
			case DataTypes.SMALLINT:
				for (StreamElement se: streamElements) {
					Serializable srl = se.getData(d.getName());
					if (srl != null && (toReturn == null || (Short)srl > (Short)toReturn))
							toReturn = srl;
				}
				break;
			case DataTypes.TINYINT:
				for (StreamElement se: streamElements) {
					Serializable srl = se.getData(d.getName());
					if (srl != null && (toReturn == null || (Byte)srl > (Byte)toReturn))
							toReturn = (Byte)srl;
				}
				break;
			default:
				logger.error("type id " + d.getDataTypeID() + " not allowed with MAX operator");
				break;
			}
			return toReturn;
		}
	}
	
	class CleanupTimerTask extends TimerTask {
		@Override
		public void run() {
			cleanupBuffers(false);
		}
		
	}

}
