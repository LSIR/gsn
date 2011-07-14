package gsn.vsensor;

import gsn.beans.DataField;
import gsn.beans.StreamElement;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

import org.apache.log4j.Logger;

public class GPSLoggerDataParser extends BridgeVirtualSensorPermasense {
	
	private static final byte RAW_DATA_TYPE = 0;
	private static final byte STATUS_TYPE = 1;

	private static final String RAW_STATUS_FILE_TYPE = "raw-status";
	private static final String CONFIG_FILE_TYPE = "configuration";
	private static final String EVENT_FILE_TYPE = "events";
	
	private static final byte[] rawHeader = {(byte) 0xB5, 0x62, 0x02, 0x10};
	private static final byte[] statusHeader = {0x6D, 0x74, 0x01, 0x01};

	private static final transient Logger logger = Logger.getLogger(GPSLoggerDataParser.class);

	private String storage_directory = null;
	private int file_type;
	
	private static DataField[] rawStatusField = {
			new DataField("POSITION", "INTEGER"),
			new DataField("GENERATION_TIME", "BIGINT"),
			new DataField("TIMESTAMP", "BIGINT"),
			new DataField("DEVICE_ID", "INTEGER"),

			new DataField("DATA_TYPE", "TINYINT"),
			new DataField("SAMPLE_COUNT", "INTEGER"),
			new DataField("GPS_RAW_DATA_VERSION", "SMALLINT"),
			new DataField("GPS_SATS", "INTEGER"),
			new DataField("STATUS_SOLAR", "INTEGER"),
			new DataField("STATUS_HUMIDITY", "INTEGER"),
			new DataField("STATUS_TEMPERATURE", "SMALLINT"),
			new DataField("STATUS_INCL_X", "SMALLINT"),
			new DataField("STATUS_INCL_Y", "SMALLINT"),
			new DataField("RAW_DATA", "BINARY")};
	
	private static DataField[] eventField = {
			new DataField("POSITION", "INTEGER"),
			new DataField("GENERATION_TIME", "BIGINT"),
			new DataField("TIMESTAMP", "BIGINT"),
			new DataField("DEVICE_ID", "INTEGER"),

			new DataField("EVENT_COUNT", "INTEGER"),
			new DataField("EVENT", "VARCHAR(256)")};
	
	private static DataField[] configField = {
			new DataField("POSITION", "INTEGER"),
			new DataField("GENERATION_TIME", "BIGINT"),
			new DataField("TIMESTAMP", "BIGINT"),
			new DataField("DEVICE_ID", "INTEGER"),
			
			new DataField("START_DATE", "BIGINT"),
			new DataField("END_DATE", "BIGINT"),
			new DataField("UPLOADER", "VARCHAR(64)"),
			new DataField("PROTOCOL", "VARCHAR(8)"),
			new DataField("FIRMWARE", "SMALLINT"),
			new DataField("SERIAL", "INTEGER"),
			new DataField("LOW_POWER_CYCLE_TIME", "INTEGER"),
			new DataField("LOW_POWER_ACTIVE_TIME", "INTEGER"),
			new DataField("LOW_POWER_MEASUREMENT", "INTEGER"),
			new DataField("ENTRY_VOLTAGE", "INTEGER"),
			new DataField("EXIT_VOLTAGE", "INTEGER"),
			new DataField("LOGGING_RATE", "INTEGER"),
			new DataField("CONFIG_USED", "INTEGER"),
			new DataField("CONFIG_TOTAL", "INTEGER"),
			new DataField("CONFIG_STRING", "BINARY"),
			new DataField("ANGLE", "VARCHAR(16)"),
			new DataField("ANTENNA_SERIAL", "VARCHAR(32)")};
	
	@Override
	public boolean initialize() {
		boolean ret = super.initialize();
		storage_directory = getVirtualSensorConfiguration().getStorage().getStorageDirectory();
		if (storage_directory != null) {
			storage_directory = new File(storage_directory, deployment).getPath();
		}
		
		String filetype = getVirtualSensorConfiguration().getMainClassInitialParams().get("file_type");
		if (filetype != null) {
			if (filetype.equalsIgnoreCase(RAW_STATUS_FILE_TYPE))
				file_type = 1;
			else if (filetype.equalsIgnoreCase(CONFIG_FILE_TYPE))
				file_type = 2;
			else if (filetype.equalsIgnoreCase(EVENT_FILE_TYPE))
				file_type = 3;
			else {
				logger.error("file_type " + filetype + " not recognized");
				return false;
			}
		}
		else {
			logger.error("file_type init parameter has to specified in virtual sensor xml file");
			return false;
		}
		
		return ret;
	}
	
	
	@Override
	public void dataAvailable(String inputStreamName, StreamElement data) {
		String relativeFile = (String) data.getData("relative_file" + file_type);
		if (relativeFile != null) {
			File file = new File(new File(storage_directory, Integer.toString((Integer)data.getData("device_id"))).getPath(), relativeFile);
			file = file.getAbsoluteFile();
			
			logger.info("new incoming file (" + file.getAbsolutePath() + ")");
	
			switch (file_type) {
				case 1:
					short GPS_RAW_DATA_VERSION = 1;
					int rawSampleCount = 1;
					int rawIncorrectChecksumCount = 0;
					int statusSampleCount = 1;
					int statusIncorrectChecksumCount = 0;
					try {
						DataInputStream dis = new DataInputStream(new FileInputStream(fixFile(file)));
						
						int b = dis.readByte();
						while (true) {
							boolean readOn = false;
							if (b == rawHeader[0]) {
								b = dis.readByte();
								if (b == rawHeader[1]) {
									b = dis.readByte();
									if (b == rawHeader[2]) {
										b = dis.readByte();
										if (b == rawHeader[3]) {
											// gps logger raw data
											byte [] rawPacket = getRawPacket(dis, rawHeader, file.getAbsolutePath());
											if (checkChecksum(rawPacket)) {
												long timestamp = getGPSRawTimestamp(rawPacket);
												data = new StreamElement(rawStatusField, new Serializable[]{
														data.getData(rawStatusField[0].getName()),
														timestamp,
														timestamp,
														data.getData(rawStatusField[3].getName()),
														RAW_DATA_TYPE,
														rawSampleCount,
														GPS_RAW_DATA_VERSION,
														(int)getUByte(rawPacket[12]),
														null,
														null,
														null,
														null,
														null,
														rawPacket});
	
												super.dataAvailable(inputStreamName, data);
											}
											else {
												rawIncorrectChecksumCount++;
												logger.warn("checksum for gps raw data sample " + rawSampleCount + " is not correct in " + file.getAbsolutePath());
												dis.reset();
											}
											
											rawSampleCount++;
											if (rawSampleCount==Integer.MAX_VALUE)
												rawSampleCount = 1;
											
											readOn = true;
										}
									}
								}
							}
							else if (b == statusHeader[0]) {
								b = dis.readByte();
								if (b == statusHeader[1]) {
									b = dis.readByte();
									if (b == statusHeader[2]) {
										b = dis.readByte();
										if (b == statusHeader[3]) {
											// gps logger status data
											byte [] rawPacket = getRawPacket(dis, statusHeader, file.getAbsolutePath());
											if (checkChecksum(rawPacket)) {
												ByteBuffer buf = ByteBuffer.wrap(rawPacket);
												buf.order(ByteOrder.LITTLE_ENDIAN);
												buf.position(6);
												long timestamp = getUInt(new byte[]{buf.get(),buf.get(),buf.get(),buf.get()})*1000L;
												data = new StreamElement(rawStatusField, new Serializable[]{
														data.getData(rawStatusField[0].getName()),
														timestamp,
														timestamp,
														data.getData(rawStatusField[3].getName()),
														STATUS_TYPE,
														statusSampleCount,
														null,
														null,
														getUShort(new byte[]{buf.get(),buf.get()}),
														getUShort(new byte[]{buf.get(),buf.get()})/10,
														(short)(buf.getShort()/10),
														buf.getShort(),
														buf.getShort(),
														rawPacket});
	
												super.dataAvailable(inputStreamName, data);
											}
											else {
												statusIncorrectChecksumCount++;
												logger.warn("checksum for gps status data sample " + statusSampleCount + " is not correct in " + file.getAbsolutePath());
												dis.reset();
											}
											
											statusSampleCount++;
											if (statusSampleCount==Integer.MAX_VALUE)
												statusSampleCount = 1;
											
											readOn = true;
										}
									}
								}
							}
							else
								readOn = true;
							
							if (readOn)
								b = dis.readByte();
						}
					} catch (EOFException e) {
						logger.debug("end of file reached");
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
	
					logger.info((rawSampleCount-1) + " raw samples and " + (statusSampleCount-1) + " status samples read");
					if (rawIncorrectChecksumCount > 0)
						logger.warn(rawIncorrectChecksumCount + " checksums did not match for raw data samples in " + file.getAbsolutePath());
					if (statusIncorrectChecksumCount > 0)
						logger.warn(statusIncorrectChecksumCount + " checksums did not match for status data samples in " + file.getAbsolutePath());
					break;
				case 2:
					try {
						BufferedReader bufr = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(file))));
						
						Serializable [] out = new Serializable[configField.length];
						out[0] = data.getData(rawStatusField[0].getName());
						out[3] = data.getData(rawStatusField[3].getName());
						
						String line;
						while ((line = bufr.readLine()) != null) {
							String [] spl = line.split("=", 2);
							if (spl.length == 2) {
								String param = spl[0].trim().toLowerCase();
								String value = spl[1].trim();
								if (param.equals("startdate")) {
									try {
										out[1] = out[2] = out[4] = (new SimpleDateFormat("dd/MM/yyyy").parse(value)).getTime();
									} catch (ParseException e) {
										logger.error(e.getMessage());
									}
								} else if (param.equals("enddate")) {
									try {
										out[5] = (new SimpleDateFormat("dd/MM/yyyy").parse(value)).getTime();
									} catch (ParseException e) {
										logger.error(e.getMessage());
									}
								} else if (param.equals("uploader")) {
									out[6] = value;
								} else if (param.equals("protocol")) {
									out[7] = value;
								} else if (param.equals("firmware")) {
									out[8] = Short.parseShort(value);
								} else if (param.equals("serial")) {
									out[9] = Integer.parseInt(value);
								} else if (param.equals("lowpowercycletime")) {
									out[10] = Integer.parseInt(value);
								} else if (param.equals("lowpoweractivetime")) {
									out[11] = Integer.parseInt(value);
								} else if (param.equals("lowpowermeasurement")) {
									out[12] = Integer.parseInt(value);
								} else if (param.equals("entryvoltage")) {
									out[13] = Integer.parseInt(value);
								} else if (param.equals("exitvoltage")) {
									out[14] = Integer.parseInt(value);
								} else if (param.equals("loggingrate")) {
									out[15] = Integer.parseInt(value);
								} else if (param.equals("configused")) {
									out[16] = Integer.parseInt(value);
								} else if (param.equals("configtotal")) {
									out[17] = Integer.parseInt(value);
								} else if (param.equals("confstring")) {
									out[18] = value.getBytes();
								} else if (param.equals("angle")) {
									out[19] = value;
								} else if (param.equals("antenna_serial")) {
									out[20] = value;
								}
							}
						}
						data = new StreamElement(configField, out);

						super.dataAvailable(inputStreamName, data);
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
					break;
				case 3:
					int eventCount = 0;
					int unknownEventCounter = 0;
					long lastTimestamp = 0;
					try {
						DataInputStream dis = new DataInputStream(new FileInputStream(fixFile(file)));
	
						byte[] tmp4b = new byte[4];
						byte[] tmp2b = new byte[2];
						Vector<String> noTimestampEvents = new Vector<String>();
						boolean timestampReady = false;
						while (true) {
							if(dis.read(tmp4b) != 4)
								break;
							long timestamp = getUInt(tmp4b)*1000L;

							if(dis.read(tmp2b) != 2)
								break;
							int eventNr = getUShort(tmp2b);

							if(dis.read(tmp2b) != 2)
								break;
							int eventData = getUShort(tmp2b);

							String event = null;
							if (eventNr == 0x0000) {
								if (!noTimestampEvents.isEmpty()) {
									int cnt = 1;
									for(Iterator<String> it=noTimestampEvents.iterator(); it.hasNext(); ) {
										String noTimestampEvent = it.next();
										eventCount++;
										data = new StreamElement(eventField, new Serializable[]{
												data.getData(eventField[0].getName()),
												lastTimestamp+cnt,
												lastTimestamp+cnt,
												data.getData(eventField[3].getName()),
												eventCount,
												noTimestampEvent});
										
										super.dataAvailable(inputStreamName, data);
										cnt++;
									}
									noTimestampEvents.clear();
								}
								continue;
							}
							else {
								switch (eventNr) {
									case 0x0001:
										timestampReady = false;
										event = "firmware version " + eventData + " startup";
										break;
									case 0x0002:
										event = "initialize persistent parameter with default values (parameter set version = " + eventData + ")";
										break;
									case 0x0003:
										event = "sd card full";
										break;
									case 0x0004:
										switch (eventData) {
										case 0x0001:
											event = "solar voltage measurement done";
											break;
										case 0x0002:
											event = "humidity and temperature measurement done";
											break;
										case 0x0004:
											event = "tilt x measurement done";
											break;
										case 0x0008:
											event = "tilt y measurement done";
											break;
										default:
											logger.warn("measurement done flag " + eventData + " unknown");
											break;
										}
										break;
									case 0x0005:
										event = "enter STOP mode";
										break;
									case 0x0006:
										event = "self-test started";
										break;
									case 0x0007:
										switch (eventData) {
										case 0x0000:
											event = "self-test finished: measurement failed (test aborted)";
											break;
										case 0x0001:
											event = "self-test finished: measurement ok, GPS failure";
											break;
										case 0x0002:
											event = "self-test finished: measurement, GPS ok, SD-Card failure";
											break;
										case 0x0003:
											event = "self-test finished: all tests ok so far, but measurement values out of expected range";
											break;
										case 0x0004:
											event = "self-test finished: passed successfully";
											break;
										default:
											logger.warn("self-test result data " + eventData + " unknown");
											break;
										}
										break;
									case 0x0008:
										event = "the custom GPS configuration (string index " + eventData + ") failed";
										break;
									case 0x0009:
										timestampReady = true;
										event = "first valid GPS week received from GPS receiver (GPS week number = " + eventData + ")";
										break;
									case 0x000A:
										event = "GPS message lost";
										break;
									case 0x000B:
										event = "fatal GPS failure";
										break;
									case 0x000C:
										event = "fatal measurement failure";
										break;
									case 0x1000:
										event = "repeated last event " + eventData;
										break;
									default:
										logger.warn("event number " + eventNr + " unknown");
										break;
								}
		
								if (event != null) {
									if (timestampReady) {
										eventCount++;
										if (!noTimestampEvents.isEmpty()) {
											int cnt = noTimestampEvents.size();
											for(Iterator<String> it=noTimestampEvents.iterator(); it.hasNext(); ) {
												String noTimeStampEvent = it.next();
												data = new StreamElement(eventField, new Serializable[]{
														data.getData(eventField[0].getName()),
														timestamp-cnt,
														timestamp-cnt,
														data.getData(eventField[3].getName()),
														eventCount,
														noTimeStampEvent});

												super.dataAvailable(inputStreamName, data);
												cnt--;
												eventCount++;
											}
											noTimestampEvents.clear();
										}
										
										data = new StreamElement(eventField, new Serializable[]{
												data.getData(eventField[0].getName()),
												timestamp,
												timestamp,
												data.getData(eventField[3].getName()),
												eventCount,
												event});

										super.dataAvailable(inputStreamName, data);
										lastTimestamp = timestamp;
										
									}
									else
										noTimestampEvents.add(event);
								}
								else {
									eventCount++;
									unknownEventCounter++;
								}
							}
						}
					} catch (EOFException e) {
						logger.debug("end of file reached");
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
	
					logger.info(eventCount + " events read");
					if (unknownEventCounter > 0)
						logger.warn(unknownEventCounter + " events have not been recognized");
					break;
				default:
					logger.error("file_type unknown");
					break;
			}
		}
	}
	
	
	private byte[] getRawPacket(DataInputStream dis, byte[] header, String filename) throws IOException, EOFException {
		dis.mark(65538);
		byte [] length = new byte [2];
		dis.read(length);
		byte [] rest = new byte [((length[0]&0xFF) | ((length[1]&0xFF) << 8)) + 2];
		int plength = dis.read(rest);
		if (plength != rest.length) {
			if (plength != -1)
				logger.warn("could not read a whole data packet at the end of file " + filename + " -> drop " + plength + " bytes");
			throw new EOFException();
		}
		
		return concatAll(header, new byte [] {length[0], length[1]}, rest);
	}
	
	
	private long getGPSRawTimestamp(byte[] rawPacket) {
		ByteBuffer buf = ByteBuffer.wrap(rawPacket);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		return (315964800L+(604800L*(long)buf.getShort(10)))*1000L+(long)buf.getInt(6);
	}


	private File fixFile(File f) throws IOException {
		FileInputStream fis = new FileInputStream(f);
		FileOutputStream fos = new FileOutputStream(f.getAbsolutePath() + ".fix");
		
		byte[] tmp = new byte [1016];
		while (fis.skip(8) == 8) {
			int i = fis.read(tmp);
			if (i == -1)
				break;
			else if (i != 1016)
				logger.warn("less than 1016 bytes have been read at the end of file " + f.getAbsolutePath() + " -> drop " + i + " bytes");
			
			fos.write(tmp);
		}
		fis.close();
		fos.close();
		
		return new File(f.getAbsolutePath() + ".fix");
	}
	
	
	private boolean checkChecksum(byte [] check) {
		long ck_a = 0x00;
		long ck_b = 0x00;
		
		for (int i=2; i<check.length-2; i++) {
			ck_a += (long) check[i] & 0xFF;
			ck_b += ck_a;
		}
		ck_a &= 0xFF;
		ck_b &= 0xFF;
		return ((check[check.length-2] & 0xFF) == ck_a) && ((check[check.length-1] & 0xFF) == ck_b);
	}
	
	private static short getUByte(byte b) {
		return (short) (b&0xFF);
	}
	
	private static int getUShort(byte[] array) {
		return (array[1]&0xFF) << 8 | (array[0]&0xFF);
	}
	
	private static long getUInt(byte[] array) {
		return (long) ((array[3]&0xFF) << 24 | (array[2]&0xFF) << 16 | (array[1]&0xFF) << 8 | (array[0]&0xFF));
	}
	
	private static byte[] concatAll(byte[] first, byte[]... rest) {
		int totalLength = first.length;
		for (byte[] array : rest) {
			totalLength += array.length;
		}
		byte[] result = Arrays.copyOf(first, totalLength);
		int offset = first.length;
		for (byte[] array : rest) {
			System.arraycopy(array, 0, result, offset, array.length);
			offset += array.length;
		}
		return result;
	}
}
