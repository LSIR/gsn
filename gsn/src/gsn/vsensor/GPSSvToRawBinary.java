package gsn.vsensor;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import gsn.beans.DataField;
import gsn.beans.StreamElement;

import org.apache.log4j.Logger;

public class GPSSvToRawBinary extends BridgeVirtualSensorPermasense {
	
	private static String GPS_TIME_FIELD_NAME = "gps_unixtime";
	private static Short GPS_RAW_DATA_VERSION = 1;

	private static String GPS_ITOW_FIELD_NAME = "gps_time";
	private static String GPS_WEEK_FIELD_NAME = "gps_week";
	private static String GPS_NUMSV_FIELD_NAME = "num_sv";
	private static String GPS_CPMES_FIELD_NAME = "carrier_phase";
	private static String GPS_PRMES_FIELD_NAME = "pseudo_range";
	private static String GPS_DOMES_FIELD_NAME = "doppler";
	private static String GPS_SV_FIELD_NAME = "space_vehicle";
	private static String GPS_MESQI_FIELD_NAME = "measurement_quality";
	private static String GPS_CNO_FIELD_NAME = "signal_strength";
	private static String GPS_LLI_FIELD_NAME = "loss_of_lock";
	private static String GPS_SEQUENCE_NR_NAME = "header_seqnr";
	
	private static final transient Logger logger = Logger.getLogger(GPSSvToRawBinary.class);

	private static final DataField[] dataField = {
		new DataField("POSITION", "INTEGER"),
		new DataField("GENERATION_TIME", "BIGINT"),
		new DataField("TIMESTAMP", "BIGINT"),
		new DataField("DEVICE_ID", "INTEGER"),
		new DataField("GPS_UNIXTIME", "BIGINT"),

		new DataField("DEVICE_TYPE", "VARCHAR(16)"),
		new DataField("GPS_RAW_DATA_VERSION", "SMALLINT"),
		new DataField("GPS_SATS", "INTEGER"),
		new DataField("GPS_MISSING_SV", "INTEGER"),
		new DataField("GPS_RAW_DATA", "BINARY"),
		new DataField("CURRENT_DATA_BUFFER_SIZE", "INTEGER"),
		new DataField("OLD_DATA_BUFFER_SIZE", "INTEGER"),
		new DataField("TOTAL_NUMBER_OF_BUFFERS", "INTEGER")};

	private Map<Integer,StreamElement> duplicatesBuffer = Collections.synchronizedMap(new HashMap<Integer,StreamElement>());
	private Map<Integer,Map<Long,SvContainer>> newSvBuffer = new HashMap<Integer,Map<Long,SvContainer>>();
	private Map<Integer,Map<Long,SvContainer>> oldSvBuffer = new HashMap<Integer,Map<Long,SvContainer>>();
	private long bufferSizeInMs;
	
	private enum Buf {
		NEW_BUF,
		OLD_BUF
	}

	private Map<Integer,Timer> emptyBufferTimers = new HashMap<Integer,Timer>();
	
	@Override
	public boolean initialize() {
		boolean ret = super.initialize();
		
		String bufferSizeInDays = getVirtualSensorConfiguration().getMainClassInitialParams().get("buffer_size_in_days");
		try {
			bufferSizeInMs = Long.decode(bufferSizeInDays) * 86400000L;
		}
		catch (NumberFormatException e) {
			logger.error("buffer_size_in_days has to be an integer");
			return false;
		}
		
		return ret;
	}
	
	@Override
	public void dataAvailable(String inputStreamName, StreamElement data) {
		Integer deviceId = (Integer)data.getData(dataField[3].getName());
		if (duplicatesBuffer.containsKey(deviceId)) {
			StreamElement lastData = duplicatesBuffer.get(deviceId);
			if (((Long)lastData.getData(GPS_TIME_FIELD_NAME)).longValue() == ((Long)data.getData(GPS_TIME_FIELD_NAME)).longValue() &&
				((Integer)lastData.getData(GPS_SEQUENCE_NR_NAME)).intValue() == ((Integer)data.getData(GPS_SEQUENCE_NR_NAME)).intValue()) {
				if (logger.isDebugEnabled())
					logger.debug("discard duplicate: [" + data.toString() + "]");
				return;
			}
			
		}
		duplicatesBuffer.put(deviceId, data);
		
		if (!newSvBuffer.containsKey(deviceId)) {
			newSvBuffer.put(deviceId, Collections.synchronizedMap(new HashMap<Long,SvContainer>()));
			oldSvBuffer.put(deviceId, Collections.synchronizedMap(new HashMap<Long,SvContainer>()));
			logger.info("New SV buffer created for device_id " + deviceId);
		}
		if ((Long)data.getData(GPS_TIME_FIELD_NAME) > System.currentTimeMillis()-bufferSizeInMs) {
			processData(inputStreamName, deviceId, data, System.currentTimeMillis(), newSvBuffer.get(deviceId), Buf.NEW_BUF);
		}
		else {
			updateTimer(deviceId);
			processData(inputStreamName, deviceId, data, (Long) data.getData(dataField[1].getName()), oldSvBuffer.get(deviceId), Buf.OLD_BUF);
		}
	}
	
	private void processData(String inputStreamName, Integer deviceId, StreamElement data, Long refTime, Map<Long,SvContainer> svContainerMap, Buf buf) {
		Long gps_unixtime = (Long)data.getData(GPS_TIME_FIELD_NAME);

		SvContainer svContainer = svContainerMap.get(gps_unixtime);
		try {
			if (svContainer == null) {
				svContainer = new SvContainer(inputStreamName, (Byte)data.getData(GPS_NUMSV_FIELD_NAME));
			}
			
			if (svContainer.putSv(data)) {
				data = svContainer.getRawBinaryStream();
				svContainerMap.remove(gps_unixtime);
				data.setData(dataField[10].getName(), newSvBuffer.get(deviceId).size());
				data.setData(dataField[11].getName(), oldSvBuffer.get(deviceId).size());
				data.setData(dataField[12].getName(), newSvBuffer.size()+oldSvBuffer.size());
				super.dataAvailable(svContainer.getInputStreamName(), data);
			}
			else {
				svContainerMap.put(gps_unixtime, svContainer);
			}
		} catch (Exception e) {
			logger.error("Device Id: " + deviceId + ": " + e.getMessage());
		}
		
		if (refTime != null) {
			Iterator<Long> iter = svContainerMap.keySet().iterator();
			while (iter.hasNext()) {
				gps_unixtime = iter.next();
				if (gps_unixtime < refTime-bufferSizeInMs) {
					if (buf == Buf.NEW_BUF) {
						// put old streams into old buffer
						svContainer = svContainerMap.get(gps_unixtime);
						iter.remove();
						if (oldSvBuffer.isEmpty())
							updateTimer(deviceId);
						for (StreamElement se : svContainer.getStreamElements()) {
							processData(svContainer.getInputStreamName(), deviceId, se, null, oldSvBuffer.get(deviceId), Buf.OLD_BUF);
						}
					}
					else {
						// generate stream element out of really old streams
						SvContainer svc = svContainerMap.get(gps_unixtime);
						iter.remove();
						data = svc.getRawBinaryStream();
						data.setData(dataField[10].getName(), newSvBuffer.get(deviceId).size());
						data.setData(dataField[11].getName(), oldSvBuffer.get(deviceId).size());
						data.setData(dataField[12].getName(), newSvBuffer.size()+oldSvBuffer.size());
						super.dataAvailable(svc.getInputStreamName(), data);
					}
				}
			}
		}
	}
	
	@Override
	public synchronized void dispose() {
		emptyBuffers(newSvBuffer);
		emptyBuffers(oldSvBuffer);
		for (Timer t: emptyBufferTimers.values())
			t.cancel();
		super.dispose();
	}
	
	private void emptyBuffers(Map<Integer,Map<Long,SvContainer>> buffer) {
		Iterator<Map<Long,SvContainer>> iter = buffer.values().iterator();
		while (iter.hasNext()) {
			emptyBuffer(iter.next());
			iter.remove();
		}
	}
	
	private void emptyBuffer(Map<Long,SvContainer> buf) {
		Iterator<Long> it = buf.keySet().iterator();
		while (it.hasNext()) {
			SvContainer svc = buf.get(it.next());
			it.remove();
			StreamElement data = svc.getRawBinaryStream();
			data.setData(dataField[10].getName(), newSvBuffer.get(data.getData(dataField[3].getName())).size());
			data.setData(dataField[11].getName(), oldSvBuffer.get(data.getData(dataField[3].getName())).size());
			data.setData(dataField[12].getName(), newSvBuffer.size()+oldSvBuffer.size());
			super.dataAvailable(svc.getInputStreamName(), data);
		}
	}

    public void updateTimer(Integer deviceId) {
    	if (!emptyBufferTimers.containsKey(deviceId))
    		emptyBufferTimers.put(deviceId, new Timer());
    	
    	TimerTaskWithDeviceId timerTask = new TimerTaskWithDeviceId(deviceId);
    	emptyBufferTimers.get(deviceId).cancel();
    	emptyBufferTimers.put(deviceId, new Timer());
        emptyBufferTimers.get(deviceId).schedule(timerTask, bufferSizeInMs);
    }

	class SvContainer {
		private ArrayList<StreamElement> streamElements;
		private String inputStreamName;
		private Integer numSv;
		
		protected SvContainer(String inputStreamName, Byte numSv) throws Exception {
			if (numSv <= 0 || numSv > Byte.MAX_VALUE)
				throw new Exception("numSv out of range: " + numSv);
			this.inputStreamName = inputStreamName;
			this.numSv = (int)numSv;
			streamElements = new ArrayList<StreamElement>(numSv);
		}
		
		public ArrayList<StreamElement> getStreamElements() {
			return streamElements;
		}

		protected boolean putSv(StreamElement streamElement) throws Exception {
			if (streamElements.size() == numSv)
				throw new Exception("SvContainer already full!");

			// discard duplicates
			for (StreamElement se : streamElements) {
				if (((Integer)se.getData(GPS_SEQUENCE_NR_NAME)).intValue() == ((Integer)streamElement.getData(GPS_SEQUENCE_NR_NAME)).intValue()) {
					if (logger.isDebugEnabled())
						logger.debug("discard duplicate in SV container: [" + streamElement.toString() + "]");
					return false;
				}
			}
			
			streamElements.add(streamElement);
			if (streamElements.size() == numSv)
				return true;
			else
				return false;
		}
		
		protected String getInputStreamName() {
			return inputStreamName;
		}
		
		protected StreamElement getRawBinaryStream() {
			ByteBuffer rxmRaw = ByteBuffer.allocate(16+24*streamElements.size());
			rxmRaw.order(ByteOrder.LITTLE_ENDIAN);
			
			// RXM-RAW Header
			rxmRaw.put((byte) 0xB5);
			rxmRaw.put((byte) 0x62);
			
			// RXM-RAW ID
			rxmRaw.put((byte) 0x02);
			rxmRaw.put((byte) 0x10);
			
			// RXM-RAW Length
			rxmRaw.putShort((short) (8+24*streamElements.size()));
			
			// RXM-RAW Payload
			rxmRaw.putInt((Integer)streamElements.get(0).getData(GPS_ITOW_FIELD_NAME));
			rxmRaw.putShort((Short)streamElements.get(0).getData(GPS_WEEK_FIELD_NAME));
			rxmRaw.put((byte) (streamElements.size() & 0xFF));
			rxmRaw.put((byte) 0x00);
			for (StreamElement se : streamElements) {
				rxmRaw.putDouble((Double)se.getData(GPS_CPMES_FIELD_NAME));
				rxmRaw.putDouble((Double)se.getData(GPS_PRMES_FIELD_NAME));
				double d = (Double)se.getData(GPS_DOMES_FIELD_NAME);
				rxmRaw.putFloat((float)d);
				rxmRaw.put((Byte)se.getData(GPS_SV_FIELD_NAME));
				rxmRaw.put((byte)((Short)se.getData(GPS_MESQI_FIELD_NAME)&0xFF));
				rxmRaw.put((Byte)se.getData(GPS_CNO_FIELD_NAME));
				rxmRaw.put((Byte)se.getData(GPS_LLI_FIELD_NAME));
			}
			
			// RXM-RAW Checksum
			byte CK_A = 0;
			byte CK_B = 0;
			for (int i=2; i<14+24*streamElements.size(); i++) {
				CK_A += rxmRaw.get(i);
				CK_B += CK_A;
			}
			rxmRaw.put(CK_A);
			rxmRaw.put(CK_B);
			
			return new StreamElement(dataField, new Serializable[]{
					streamElements.get(0).getData(dataField[0].getName()),
					streamElements.get(0).getData(dataField[1].getName()),
					streamElements.get(0).getData(dataField[2].getName()),
					streamElements.get(0).getData(dataField[3].getName()),
					streamElements.get(0).getData(dataField[4].getName()),
					streamElements.get(0).getData(dataField[5].getName()),
					GPS_RAW_DATA_VERSION,
					(int)streamElements.size(),
					numSv-streamElements.size(),
					rxmRaw.array(),
					null,
					null,
					null});
		}
	}
	
	class TimerTaskWithDeviceId extends TimerTask {
		private Integer deviceId;
		
		public TimerTaskWithDeviceId(Integer id) {
			deviceId = id;
		}

		@Override
		public void run() {
			emptyBuffer(oldSvBuffer.get(deviceId));
			if (logger.isDebugEnabled())
				logger.debug("TimerTask for device_id " + deviceId + " executed");
			if (newSvBuffer.get(deviceId).isEmpty()) {
				logger.info("no more SV packages arrived from device_id " + deviceId + " since " + bufferSizeInMs + " ms -> remove SV buffer");
				newSvBuffer.remove(deviceId);
				oldSvBuffer.remove(deviceId);
			}
		}
		
	}
}
