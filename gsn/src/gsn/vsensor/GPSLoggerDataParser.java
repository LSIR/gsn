package gsn.vsensor;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.utils.Helpers;

import java.io.BufferedInputStream;
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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

public class GPSLoggerDataParser extends BridgeVirtualSensorPermasense {
	
	private static final byte RAW_DATA_TYPE = 0;
	private static final byte STATUS_TYPE = 1;

	private static final String RAW_STATUS_STREAM_TYPE = "raw-status";
	private static final String CONFIG_STREAM_TYPE = "configuration";
	private static final String EVENT_STREAM_TYPE = "events";
	private static final String PARSING_STATUS_STREAM_TYPE = "parsing-status";
	
	private static final Hashtable<Short, String> streamTypeNamingTable = new Hashtable<Short, String>();
	static
	{
		streamTypeNamingTable.put((short) 1, RAW_STATUS_STREAM_TYPE);
		streamTypeNamingTable.put((short) 2, CONFIG_STREAM_TYPE);
		streamTypeNamingTable.put((short) 3, EVENT_STREAM_TYPE);
	}
	
	private static final byte[] rawHeader = {(byte) 0xB5, 0x62, 0x02, 0x10};
	private static final byte[] statusHeader = {0x6D, 0x74, 0x01, 0x01};

	private static final transient Logger logger = Logger.getLogger(GPSLoggerDataParser.class);

	private String storage_directory = null;
	private short stream_type;
	private GPSFileParserThread gpsFileParserThread;
	
	private static DataField[] rawStatusField = {
			new DataField("POSITION", "INTEGER"),
			new DataField("GENERATION_TIME", "BIGINT"),
			new DataField("TIMESTAMP", "BIGINT"),
			new DataField("GSN_TIMESTAMP", "BIGINT"),
			new DataField("DEVICE_ID", "INTEGER"),

			new DataField("DEVICE_TYPE", "VARCHAR(16)"),
			new DataField("DATA_TYPE", "TINYINT"),
			new DataField("SAMPLE_COUNT", "INTEGER"),
			new DataField("GPS_RAW_DATA_VERSION", "SMALLINT"),
			new DataField("GPS_SATS", "INTEGER"),
			new DataField("STATUS_V12DC_IN", "INTEGER"),
			new DataField("STATUS_HUMIDITY", "INTEGER"),
			new DataField("STATUS_TEMPERATURE", "SMALLINT"),
			new DataField("STATUS_INCLINOMETER_X", "SMALLINT"),
			new DataField("STATUS_INCLINOMETER_Y", "SMALLINT"),
			new DataField("RAW_DATA", "BINARY")};
	
	private static DataField[] eventField = {
			new DataField("POSITION", "INTEGER"),
			new DataField("GENERATION_TIME", "BIGINT"),
			new DataField("TIMESTAMP", "BIGINT"),
			new DataField("GSN_TIMESTAMP", "BIGINT"),
			new DataField("DEVICE_ID", "INTEGER"),

			new DataField("DEVICE_TYPE", "VARCHAR(16)"),
			new DataField("EVENT_COUNT", "INTEGER"),
			new DataField("EVENT", "VARCHAR(256)")};
	
	private static DataField[] configField = {
			new DataField("POSITION", "INTEGER"),
			new DataField("GENERATION_TIME", "BIGINT"),
			new DataField("TIMESTAMP", "BIGINT"),
			new DataField("GSN_TIMESTAMP", "BIGINT"),
			new DataField("DEVICE_ID", "INTEGER"),
			
			new DataField("DEVICE_TYPE", "VARCHAR(16)"),
			new DataField("START_DATE", "BIGINT"),
			new DataField("END_DATE", "BIGINT"),
			new DataField("UPLOADER", "VARCHAR(64)"),
			new DataField("PROTOCOL", "VARCHAR(8)"),
			new DataField("SOFTWARE_VERSION", "INTEGER"),
			new DataField("FIRMWARE", "SMALLINT"),
			new DataField("MANUAL_POSITION", "INTEGER"),
			new DataField("LOW_POWER_CYCLE_TIME", "SMALLINT"),
			new DataField("LOW_POWER_ACTIVE_TIME", "SMALLINT"),
			new DataField("LOW_POWER_MEASUREMENT", "SMALLINT"),
			new DataField("ENTRY_VOLTAGE", "INTEGER"),
			new DataField("EXIT_VOLTAGE", "INTEGER"),
			new DataField("LOGGING_RATE", "INTEGER"),
			new DataField("CONFIG_USED", "SMALLINT"),
			new DataField("CONFIG_TOTAL", "SMALLINT"),
			new DataField("CONFIG_STRING", "BINARY"),
			new DataField("MAST_ORIENTATION_START", "SMALLINT"),
			new DataField("MAST_ORIENTATION_END", "SMALLINT"),
			new DataField("DATA_PAGES", "INTEGER"),
			new DataField("EVENT_PAGES", "INTEGER"),
			new DataField("HIGH_POWER_MEASUREMENT", "SMALLINT"),
			new DataField("ANTENNA_SERIAL", "VARCHAR(32)")};
	
	private static DataField[] parsingStatusField = {
			new DataField("DEVICE_ID", "INTEGER"),
			
			new DataField("FILE_TYPE", "VARCHAR(16)"),
			new DataField("FILE_NAME", "VARCHAR(64)"),
			new DataField("FILE_QUEUE_SIZE", "INTEGER"),
			new DataField("START_PARSING", "BIGINT"),
			new DataField("FINISHED_PARSING", "BIGINT"),
			new DataField("GENERATED_STREAMS", "INTEGER"),
			new DataField("UNPARSABLE_STREAMS", "INTEGER")};
	
	@Override
	public boolean initialize() {
		boolean ret = super.initialize();
		storage_directory = getVirtualSensorConfiguration().getStorage().getStorageDirectory();
		if (storage_directory != null) {
			storage_directory = new File(storage_directory, deployment).getPath();
		}
		
		String streamtype = getVirtualSensorConfiguration().getMainClassInitialParams().get("stream_type");
		if (streamtype != null) {
			if (streamtype.equalsIgnoreCase(RAW_STATUS_STREAM_TYPE))
				stream_type = 1;
			else if (streamtype.equalsIgnoreCase(CONFIG_STREAM_TYPE))
				stream_type = 2;
			else if (streamtype.equalsIgnoreCase(EVENT_STREAM_TYPE))
				stream_type = 3;
			else if (streamtype.equalsIgnoreCase(PARSING_STATUS_STREAM_TYPE))
				stream_type = 127;
			else {
				logger.error("stream_type " + streamtype + " not recognized");
				return false;
			}
		}
		else {
			logger.error("stream_type init parameter has to specified in virtual sensor xml file");
			return false;
		}
		

		gpsFileParserThread = GPSFileParserThread.getSingletonObject(deployment);
		gpsFileParserThread.setNewListener(stream_type, this);
		synchronized (gpsFileParserThread) {
			if (!gpsFileParserThread.isAlive())
				gpsFileParserThread.start();
		}
		
		return ret;
	}
	
	
	@Override
	public void dataAvailable(String inputStreamName, StreamElement data) {
		if (stream_type != 127) {
			String relativeFile = (String) data.getData("relative_file" + stream_type);
			if (relativeFile != null) {
				logger.info("adding new file to parsing queue: " + relativeFile);
				File file = new File(new File(storage_directory, Integer.toString((Integer)data.getData("device_id"))).getPath(), relativeFile);
				file = file.getAbsoluteFile();
				try {
					gpsFileParserThread.newFile(stream_type, file, inputStreamName, data);
				} catch (InterruptedException e) {
					logger.error(e.getMessage());
				}
			}
		}
	}

	@Override
	public void dispose() {
		if (gpsFileParserThread != null)
			gpsFileParserThread.shutdown();
	}
	
	
	private void newStreamElement(String inputStreamName, StreamElement data) {
		super.dataAvailable(inputStreamName, data);
	}
	

	private static class GPSFileParserThread extends Thread {
		
		private static Map<String,GPSFileParserThread> perDeploymentSingletons = new Hashtable<String,GPSFileParserThread>();
		
		private final BlockingQueue<FileItem> queue;
		private boolean stop;
		private Map<Short,GPSLoggerDataParser> streamtypeToListener;
		
		
		private GPSFileParserThread() {
			stop = false;
			queue = new LinkedBlockingQueue<FileItem>();
			streamtypeToListener = new Hashtable<Short,GPSLoggerDataParser>();
			setName("GPSFileParserThread-Thread");
		}
		
		
		public synchronized static GPSFileParserThread getSingletonObject(String deployment) {
			if (!perDeploymentSingletons.containsKey(deployment))
				perDeploymentSingletons.put(deployment, new GPSFileParserThread());
			
			return perDeploymentSingletons.get(deployment);
		}
		
		protected void setNewListener(short streamtype, GPSLoggerDataParser listener) {
			streamtypeToListener.put(streamtype, listener);
		}
		
		protected void newFile(short type, File file, String inputStreamName, StreamElement data) throws InterruptedException {
			queue.put(new FileItem(type, file, inputStreamName, data));
		}

		@Override
		public void run() {
			try {
				while (!stop) {
					FileItem fileItem = queue.take();
					long startParsingTime = System.currentTimeMillis();
					short streamType = fileItem.getType();
					File file = fileItem.getFile();
					String inputStreamName = fileItem.getInputStreamName();
					StreamElement data = fileItem.getData();
					GPSLoggerDataParser listener = streamtypeToListener.get(streamType);
					long gsn_timestamp = data.getTimeStamp();
					String relativeFile = (String) data.getData("relative_file" + streamType);
					
					if (listener == null) {
						logger.error("for stream type " + streamType + " there is no listener available -> skip file");
						continue;
					}
					
					processParsingStatusStreamElement(inputStreamName, data, streamTypeNamingTable.get(streamType),
							relativeFile, queue.size(), startParsingTime, null, null, null);

					boolean finished = false;
					switch (streamType) {
						case 1:
							logger.info("start parsing " + RAW_STATUS_STREAM_TYPE + " file (" + relativeFile + ")");
							
							short GPS_RAW_DATA_VERSION = 1;
							int rawSampleCount = 1;
							int rawIncorrectChecksumCount = 0;
							int statusSampleCount = 1;
							int statusIncorrectChecksumCount = 0;
							try {
								DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(fixFile(file))));
								
								int b = dis.readByte();
								while (!stop) {
									boolean readOn = false;
									if (b == rawHeader[0]) {
										b = dis.readByte();
										if (b == rawHeader[1]) {
											b = dis.readByte();
											if (b == rawHeader[2]) {
												b = dis.readByte();
												if (b == rawHeader[3]) {
													// gps logger raw data
													byte [] rawPacket = getRawPacket(dis, rawHeader, relativeFile);
													if (checkChecksum(rawPacket)) {
														long timestamp = getGPSRawTimestamp(rawPacket);
														data = new StreamElement(rawStatusField, new Serializable[]{
																data.getData(rawStatusField[0].getName()),
																timestamp,
																timestamp,
																gsn_timestamp++,
																data.getData(rawStatusField[4].getName()),
																data.getData(rawStatusField[5].getName()),
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
			
														listener.newStreamElement(inputStreamName, data);
													}
													else {
														rawIncorrectChecksumCount++;
														logger.warn("checksum for gps raw data sample " + rawSampleCount + " is not correct in " + relativeFile);
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
													byte [] rawPacket = getRawPacket(dis, statusHeader, relativeFile);
													if (checkChecksum(rawPacket)) {
														ByteBuffer buf = ByteBuffer.wrap(rawPacket);
														buf.order(ByteOrder.LITTLE_ENDIAN);
														buf.position(6);
														long timestamp = getUInt(new byte[]{buf.get(),buf.get(),buf.get(),buf.get()})*1000L;
														data = new StreamElement(rawStatusField, new Serializable[]{
																data.getData(rawStatusField[0].getName()),
																timestamp,
																timestamp,
																gsn_timestamp++,
																data.getData(rawStatusField[4].getName()),
																data.getData(rawStatusField[5].getName()),
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
			
														listener.newStreamElement(inputStreamName, data);
													}
													else {
														statusIncorrectChecksumCount++;
														logger.warn("checksum for gps status data sample " + statusSampleCount + " is not correct in " + relativeFile);
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
								finished = true;
								logger.debug("end of file reached");
							} catch (IOException e) {
								logger.error(e.getMessage(), e);
							}
			
							if (finished) {
								logger.info((rawSampleCount-1) + " raw samples and " + (statusSampleCount-1) + " status samples read");
								if (rawIncorrectChecksumCount > 0)
									logger.warn(rawIncorrectChecksumCount + " checksums did not match for raw data samples in " + relativeFile);
								if (statusIncorrectChecksumCount > 0)
									logger.warn(statusIncorrectChecksumCount + " checksums did not match for status data samples in " + relativeFile);
								processParsingStatusStreamElement(inputStreamName, data, streamTypeNamingTable.get(streamType),
										relativeFile, queue.size(), startParsingTime, System.currentTimeMillis(),
										rawSampleCount+statusSampleCount-2-rawIncorrectChecksumCount-statusIncorrectChecksumCount,
										rawIncorrectChecksumCount+statusIncorrectChecksumCount);
							}
							else
								logger.error("end of raw status file has not been reached -> not all stream elements could be generated");
							break;
						case 2:
							logger.info("start parsing  " + CONFIG_STREAM_TYPE + " file (" + relativeFile + ")");

							BufferedReader bufr = null;
							try {
								bufr = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(file))));
								
								Serializable [] out = new Serializable[configField.length];
								out[0] = data.getData(configField[0].getName());
								out[3] = gsn_timestamp;
								out[4] = data.getData(configField[4].getName());
								out[5] = data.getData(configField[5].getName());
								
								String line;
								int pos = -1;
								while (!stop) {
									if ((line = bufr.readLine()) != null) {
										String [] spl = line.split("=", 2);
										if (spl.length == 2) {
											String param = spl[0].trim().toLowerCase();
											String value = spl[1].trim();
											try {
												if (param.equals("start_date")) {
													try {
														out[6] = (new SimpleDateFormat("dd/MM/yy").parse(value)).getTime();
													} catch (ParseException e) {
														logger.error(e.getMessage());
													}
												} else if (param.equals("end_date")) {
													try {
														out[7] = (new SimpleDateFormat("dd/MM/yy").parse(value)).getTime();
													} catch (ParseException e) {
														logger.error(e.getMessage());
													}
												} else if (param.equals("uploader")) {
													out[8] = value;
												} else if (param.equals("protocol")) {
													out[9] = value;
												} else if (param.equals("software_version")) {
													pos = 10;
													out[pos] = Integer.parseInt(value);
												} else if (param.equals("firmware")) {
													pos = 11;
													out[pos] = Short.parseShort(value);
												} else if (param.equals("position")) {
													pos = 12;
													out[pos] = Integer.parseInt(value);
												} else if (param.equals("low_power_cycle_time")) {
													pos = 13;
													out[pos] = Short.parseShort(value);
												} else if (param.equals("low_power_active_time")) {
													pos = 14;
													out[pos] = Short.parseShort(value);
												} else if (param.equals("low_power_measurement")) {
													pos = 15;
													out[pos] = Short.parseShort(value);
												} else if (param.equals("entry_voltage")) {
													pos = 16;
													out[pos] = Integer.parseInt(value);
												} else if (param.equals("exit_voltage")) {
													pos = 17;
													out[pos] = Integer.parseInt(value);
												} else if (param.equals("logging_rate")) {
													pos = 18;
													out[pos] = Integer.parseInt(value);
												} else if (param.equals("config_used")) {
													pos = 19;
													out[pos] = Short.parseShort(value);
												} else if (param.equals("config_total")) {
													pos = 20;
													out[pos] = Short.parseShort(value);
												} else if (param.equals("config_string")) {
													pos = 21;
													out[pos] = value.getBytes();
												} else if (param.equals("mast_orientation_start")) {
													pos = 22;
													out[pos] = Short.parseShort(value);
												} else if (param.equals("mast_orientation_end")) {
													pos = 23;
													out[pos] = Short.parseShort(value);
												} else if (param.equals("data_pages")) {
													pos = 24;
													out[pos] = Integer.parseInt(value);
												} else if (param.equals("event_pages")) {
													pos = 25;
													out[pos] = Integer.parseInt(value);
												} else if (param.equals("high_power_measurement")) {
													pos = 26;
													out[pos] = Short.parseShort(value);
												} else if (param.equals("antenna_serial")) {
													out[27] = value;
												}
											} catch (NumberFormatException e) {
												logger.error(e.getMessage());
												out[pos] = null;
											}
										}	
									}
									else {
										finished = true;
										break;
									}
								}
								
								try {
									out[1] = out[2] = ((Long)out[7]+(Long)out[6])/2;
								}
								catch (Exception e) {
									logger.error("can not set generation_time because start_date and/or end_date is not set properly");
									out[1] = out[2] = null;
								}
								
								data = new StreamElement(configField, out);

								listener.newStreamElement(inputStreamName, data);
							} catch (IOException e) {
								logger.error(e.getMessage(), e);
							}
							finally {
								try {
									bufr.close();
								} catch (IOException e) {
									logger.error(e.getMessage(), e);
								}
							}
							
							if (finished){
								logger.info("successfully parsed config file (" + relativeFile + ")");
								processParsingStatusStreamElement(inputStreamName, data, streamTypeNamingTable.get(streamType),
										relativeFile, queue.size(), startParsingTime, System.currentTimeMillis(), 1, 0);
							}
							else
								logger.error("end of config file has not been reached -> stream element could be generated");
							break;
						case 3:
							logger.info("start parsing " + EVENT_STREAM_TYPE + " file (" + relativeFile + ")");
							
							int eventCount = 0;
							int unknownEventCounter = 0;
							DataInputStream dis = null;
							try {
								dis = new DataInputStream(new FileInputStream(fixFile(file)));
			
								byte[] tmp4b = new byte[4];
								byte[] tmp2b = new byte[2];
								Vector<String> noTimestampEvents = new Vector<String>();
								boolean timestampReady = false;
								while (!finished && !stop) {
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
									switch (eventNr) {
										case 0x0000:
											finished = true;
											continue;
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
										case 0x000D:
											switch (eventData) {
											case 0x0000:
												event = "SD card initialized and ready for use";
												break;
											case 0x0001:
												event = "SD card not present";
												break;
											case 0x0002:
												event = "SD card present, but currently not available";
												break;
											case 0x0003:
												event = "SD card size not supported (1GB to 2GB expected)";
												break;
											case 0x0004:
												event = "SD card operation not allowed (e.g. write to block in file system area)";
												break;
											case 0x0005:
												event = "SD card init error";
												break;
											case 0x0006:
												event = "SD card busy";
												break;
											case 0x0007:
												event = "SD card is full";
												break;
											default:
												logger.warn("self-test result data " + eventData + " unknown");
												break;
											}
											break;
										case 0x000E:
											event = "GPS checksum match failure";
											break;
										case 0x000F:
											event = "TinyNode status buffer overflow (" + eventData + " status packets dropped)";
											break;
										case 0x0010:
											event = "TinyNode space vehicle buffer overflow (" + eventData + " vehicle packets dropped)";
											break;
										case 0x0011:
											event = "TinyNode event buffer overflow (" + eventData + " events dropped)";
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
															gsn_timestamp++,
															data.getData(eventField[4].getName()),
															data.getData(eventField[5].getName()),
															eventCount,
															noTimeStampEvent});

													listener.newStreamElement(inputStreamName, data);
													cnt--;
													eventCount++;
												}
												noTimestampEvents.clear();
											}
											
											data = new StreamElement(eventField, new Serializable[]{
													data.getData(eventField[0].getName()),
													timestamp,
													timestamp,
													gsn_timestamp++,
													data.getData(eventField[4].getName()),
													data.getData(eventField[5].getName()),
													eventCount,
													event});

											listener.newStreamElement(inputStreamName, data);
											
										}
										else
											noTimestampEvents.add(event);
									}
									else {
										eventCount++;
										unknownEventCounter++;
									}
								}
							} catch (EOFException e) {
								finished = true;
								logger.debug("end of file reached");
							} catch (IOException e) {
								logger.error(e.getMessage(), e);
							}
							finally {
								try {
									dis.close();
								} catch (IOException e) {
									logger.error(e.getMessage(), e);
								}
							}
							if (finished) {
								processParsingStatusStreamElement(inputStreamName, data, streamTypeNamingTable.get(streamType),
										relativeFile, queue.size(), startParsingTime, System.currentTimeMillis(),
										eventCount-unknownEventCounter, unknownEventCounter);
								logger.info(eventCount + " events read");
								if (unknownEventCounter > 0)
									logger.warn(unknownEventCounter + " events have not been recognized");
							}
							else
								logger.error("end of event file has not been reached -> not all stream elements could be generated");
							break;
						default:
							logger.error("stream_type unknown");
							break;
					}
				}
			} catch (InterruptedException e) { }
		}
		
		synchronized public void shutdown() {
			if (!stop) {
				stop = true;
				synchronized(queue) {
					queue.notifyAll();
				}
			}
		}
		
		
		private void processParsingStatusStreamElement(String inputStreamName, StreamElement data, String type, String name,
				Integer queuesize, Long start, Long finished, Integer generated, Integer unparsable) {
			data = new StreamElement(parsingStatusField, new Serializable[]{
				data.getData(parsingStatusField[0].getName()),
				type,
				name,
				queuesize,
				start,
				finished,
				generated,
				unparsable});
	
			GPSLoggerDataParser listener = streamtypeToListener.get((short)127);
			if (listener != null)
				listener.newStreamElement(inputStreamName, data);
			else
				logger.warn("no listener for parsing status stream elements available");
		}
		
		
		private byte[] getRawPacket(DataInputStream dis, byte[] header, String filename) throws IOException, EOFException {
			dis.mark(66000);
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
		
		
		private long getGPSRawTimestamp(byte[] rawPacket) {
			ByteBuffer buf = ByteBuffer.wrap(rawPacket);
			buf.order(ByteOrder.LITTLE_ENDIAN);
			return (long) (Helpers.convertGPSTimeToUnixTime((double)(buf.getInt(6)/1000.0), buf.getShort(10))*1000.0);
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
		
		private short getUByte(byte b) {
			return (short) (b&0xFF);
		}
		
		private int getUShort(byte[] array) {
			return (array[1]&0xFF) << 8 | (array[0]&0xFF);
		}
		
		private long getUInt(byte[] array) {
			return (long) ((array[3]&0xFF) << 24 | (array[2]&0xFF) << 16 | (array[1]&0xFF) << 8 | (array[0]&0xFF));
		}
		
		private byte[] concatAll(byte[] first, byte[]... rest) {
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
		
		private class FileItem {
			private short type;
			private File file;
			private String inputStreamName;
			private StreamElement data;
			
			public FileItem (short type, File file, String inputStreamName, StreamElement data) {
				this.type = type;
				this.file = file;
				this.inputStreamName = inputStreamName;
				this.data = data;
			}
			
			public short getType() { return type; };
			
			public File getFile() { return file; };
			
			public String getInputStreamName() { return inputStreamName; };
			
			public StreamElement getData() { return data; };
		}
	}
}
