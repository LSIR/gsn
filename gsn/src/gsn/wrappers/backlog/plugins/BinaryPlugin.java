package gsn.wrappers.backlog.plugins;

import org.apache.log4j.Logger;
import org.h2.tools.Server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.wrappers.BackLogWrapper;



/**
 * This plugin offers the functionality to download binaries from a deployment
 * in the size of up to 4GB. The binaries will be sent in chunks. Thus, no significant
 * interrupts of other plugin traffic is guaranteed. In case of a connection loss,
 * the download of the actual binary will be resumed as soon
 * as GSN reconnects to the deployment. The downloaded binaries
 * can be stored on disk or in the database. This will be configured on side of the
 * deployment.
 * <p>
 * The 'storage-directory' predicate (defined in the virtual sensor's XML file) has
 * to be used to specify the storage location. If the binaries are stored in the
 * database the directory is only used to store the partly downloaded binary. If
 * the binaries are stored on disk, it defines the root directory in which the
 * binaries will be stored.
 * <p>
 * If the binaries should be stored on disk the same folder structure as on side of
 * the deployment is used. In addition to that the binaries are separated into subfolders
 * named and sorted after the binaries modification time. The needed resolution of separation
 * can be specified on side of the deployment.
 * 
 * @author Tonio Gsell
 * <p>
 * TODO: remove CRC functionality after long time testing. It is not necessary over TCP.
 */
public class BinaryPlugin extends AbstractPlugin {
	
	protected static final int RESEND_INTERVAL_SEC = 30;
	
	private static final String STORAGE_DIRECTORY = "storage-directory";
	
	private static final String PROPERTY_REMOTE_BINARY = "remote_binary";
	private static final String PROPERTY_DOWNLOADED_SIZE = "downloaded_size";
	private static final String PROPERTY_TRANS_START = "transmission_start";
	private static final String PROPERTY_TRANS_TIME = "transmission_time";
	private static final String PROPERTY_BINARY_TIMESTAMP = "timestamp";
	private static final String PROPERTY_BINARY_SIZE = "file_size";
	protected static final String PROPERTY_CHUNK_NUMBER = "chunk_number";
	protected static final String PROPERTY_CHUNK_RESEND = "chunk_resend";
	protected static final String PROPERTY_STORAGE_TYPE = "storage_type";
	protected static final String PROPERTY_TIME_DATE_FORMAT = "time_date_format";
	
	private static final String TEMP_BINARY_NAME = "binaryplugin_download.part";
	private static final String PROPERTY_FILE_NAME = ".gsnBinaryStat";

	protected static final byte ACK_PACKET = 0;
	protected static final byte INIT_PACKET = 1;
	protected static final byte RESEND_PACKET = 2;
	protected static final byte CHUNK_PACKET = 3;
	protected static final byte CRC_PACKET = 4;

	private Timer connectionTestTimer = null;
	private SimpleDateFormat folderdatetimefm;

	private String rootBinaryDir;

	protected final transient Logger logger = Logger.getLogger( BinaryPlugin.class );
	
	private DataField[] dataField = new DataField[] {
			new DataField("GENERATION_TIME", "BIGINT"),
			new DataField("DEVICE_ID", "INTEGER"),

			new DataField("FILE_SIZE", "BIGINT"),
			new DataField("TRANSMITTED", "BIGINT"),
			new DataField("TRANSMISSION_TIME_EFF", "INTEGER"),
			new DataField("TRANSMISSION_TIME_TOT", "INTEGER"),
			new DataField("CHUNK_RETRANSMISSIONS", "INTEGER"),
			new DataField("FILE_QUEUE_SIZE", "INTEGER"),
			new DataField("RELATIVE_FILE", "VARCHAR(255)"),
			new DataField("STORAGE_DIRECTORY", "VARCHAR(255)"),
			new DataField("DATA", "binary")};
	
	private LinkedBlockingQueue<Message> msgQueue = new LinkedBlockingQueue<Message>();
	private String propertyFileName = null;
	private boolean storeInDatabase;
	protected Properties configFile = new Properties();
	private long binaryTimestamp = -1;
	private long binaryLength = -1;
	private long lastTransmissionTimestamp;
	private int binaryTransmissionTime;
	private long binaryTransmissionStartTime;
	private short percentDownloaded;
	protected CRC32 calculatedCRC = new CRC32();
	protected String remoteBinaryName = null;
	protected String localBinaryName = null;
	protected long downloadedSize = -1;
	protected long lastChunkNumber = -1;
	protected int lastChunkResend;
	private static Set<String> coreStationsList = new HashSet<String>();

	private Server web;
	protected String coreStationName = null;
	
	private CalculateChecksum calcChecksumThread;
	protected BinarySender binarySender;

	private boolean dispose = false;
	
	
	@Override
	public boolean initialize ( BackLogWrapper backlogwrapper, String coreStationName, String deploymentName) {
		activeBackLogWrapper = backlogwrapper;
		this.coreStationName = coreStationName;

		AddressBean addressBean = getActiveAddressBean();
		String p = addressBean.getPredicateValue("priority");
		if (p == null)
			priority = null;
		else
			priority = Integer.valueOf(p);
		
		calcChecksumThread = new CalculateChecksum(this);
		binarySender = new BinarySender(this);
		
		try {
			rootBinaryDir = addressBean.getPredicateValueWithException(STORAGE_DIRECTORY);
		} catch (Exception e) {
			logger.error(e.getMessage());
			return false;
		}
		
		if(!rootBinaryDir.endsWith("/"))
			rootBinaryDir += "/";
		
		File f = new File(rootBinaryDir);
		if (!f.isDirectory()) {
			logger.error(rootBinaryDir + " is not a directory");
			return false;
		}
		
		if (!f.canWrite()) {
			logger.error(rootBinaryDir + " is not writable");
			return false;
		}
		
		// check if this plugin has already been used for this core station
		synchronized (coreStationsList) {
			if (!coreStationsList.add(coreStationName)) {
				logger.error("This plugin can only be used once per CoreStation (core station " + coreStationName + " already used!");
				return false;
			}
		}
		
		propertyFileName = rootBinaryDir + PROPERTY_FILE_NAME;

		if (logger.isDebugEnabled()) {
			logger.debug("property file name: " + propertyFileName);
			logger.debug("local binary directory: " + rootBinaryDir);
		}
        
        registerListener();

        setName("BinaryPlugin-" + coreStationName + "-Thread");
		
		return true;
	}


	@Override
	public String getPluginName() {
		return "BinaryPlugin";
	}
	

	@Override
	public byte getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.BINARY_MESSAGE_TYPE;
	}

	@Override
	public DataField[] getOutputFormat() {
		return dataField;
	}
	
	@Override
	public void dispose() {
		if (logger.isDebugEnabled())
			logger.debug("dispose thread");
		calcChecksumThread.dispose();
		try {
			calcChecksumThread.join();
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
		binarySender.dispose();
		try {
			binarySender.join();
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
		if (web != null) {
			web.shutdown();
			web = null;
		}
		dispose = true;
		msgQueue.add(new Message());
		
		synchronized (coreStationsList) {
			coreStationsList.remove(coreStationName);
		}
        
        super.dispose();
	}
	
	@Override
	public void run() {
        logger.info("thread started");
        
		binarySender.start();
		calcChecksumThread.start();
		long lastRecvPacketType = -1;
		
    	// start connection check timer
		if (connectionTestTimer == null) {
			connectionTestTimer = new Timer("ConnectionCheck");
			connectionTestTimer.schedule( new ConnectionCheckTimer(this), 15000 );
		}

    	Message msg;
    	while (!dispose) {
			try {
				msg = msgQueue.take();
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
				break;
			}
			if (dispose)
				break;
			
			binarySender.stopSending();
        	
    		long filelen = -1;
    		// get packet type
    		byte pktType = (Byte) msg.getData()[0];
    		
			if (pktType == ACK_PACKET) {
				byte ackType = (Byte) msg.getData()[1];
				if (logger.isDebugEnabled())
					logger.debug("acknowledge packet type >" + ackType + "< received");
			}
			else if (pktType == INIT_PACKET) {
				if (lastRecvPacketType == INIT_PACKET) {
					if (logger.isDebugEnabled())
						logger.debug("init packet already received");
				}
				else {
					if (logger.isDebugEnabled())
						logger.debug("init packet received");
    				
    				// get file info
					int filequeuesize;
					int chunkresend;
    				try {
    					filequeuesize = toInteger(msg.getData()[1]);
    					chunkresend = toInteger(msg.getData()[2]);
						binaryTimestamp = toLong(msg.getData()[3]);
						binaryLength = toLong(msg.getData()[4]);
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
						dispose();
						continue;
					}
    				byte storage = (Byte) msg.getData()[5];
    				remoteBinaryName = (String) msg.getData()[6];
    				String datetimefm = (String) msg.getData()[7];
    				try {
    					folderdatetimefm = new SimpleDateFormat(datetimefm);
    				} catch (IllegalArgumentException e) {
    					logger.error("the received init packet does contain a mallformed date time format >" + datetimefm + "<! Please check your backlog configuration on the deployment -> drop this binary");
    					binarySender.requestNewBinary();
    					continue;
    				}
    				
    				if (storage == 1)
    					storeInDatabase = true;
    				else
    					storeInDatabase = false;

    				if (logger.isDebugEnabled()) {
						logger.debug("new incoming binary:");
						logger.debug("   remote binary name: " + remoteBinaryName);
						logger.debug("   timestamp of the binary: " + binaryTimestamp);
						logger.debug("   binary length: " + binaryLength);

	    				if (storeInDatabase)
	    					logger.debug("   store in database");
	    				else
	    					logger.debug("   store on disk");
	    				logger.debug("   folder date time format: " + datetimefm);
    				}
    	
    			    File f = new File(remoteBinaryName);
    			    

    			    if (storeInDatabase) {
    			    	localBinaryName = rootBinaryDir + TEMP_BINARY_NAME;
    			    }
    			    else {
    			    	String subpath = f.getParent();
    			    	if (subpath == null	)
    			    		subpath = "";
    					if (logger.isDebugEnabled())
    						logger.debug("subpath: " + subpath);
    					
    					if(!subpath.endsWith("/"))
    						subpath += "/";
    			    	
	    			    String datedir = rootBinaryDir + subpath + folderdatetimefm.format(new java.util.Date(binaryTimestamp)) + "/";
	    			    String filename = f.getName();
	    			    f = new File(datedir);
	    			    if (!f.exists()) {
	    			    	if (!f.mkdirs()) {
	    			    		logger.error("could not mkdir >" + datedir + "<  -> drop remote binary " + remoteBinaryName);
	    			    		binarySender.requestNewBinary();
	    			    		continue;
	    			    	}
	    			    }
	    			    localBinaryName = datedir + filename;
    			    }
    	
    				filelen = 0;
    				
    				// delete the file if it already exists
    				f = new File(localBinaryName);
    			    if (f.exists()) {
    					if (logger.isDebugEnabled())
    						logger.debug("overwrite already existing binary >" + localBinaryName + "<");
    			    	f.delete();
    			    }
    			    
					lastChunkNumber = -1;
					lastChunkResend = 0;
    	
					binaryTransmissionStartTime = lastTransmissionTimestamp = System.currentTimeMillis();
					binaryTransmissionTime = (int) (lastTransmissionTimestamp-msg.getTimestamp());
					
    			    // write the new binary info to the property file
    				configFile.setProperty(PROPERTY_REMOTE_BINARY, remoteBinaryName);
    				configFile.setProperty(PROPERTY_DOWNLOADED_SIZE, Long.toString(0));
    				configFile.setProperty(PROPERTY_TRANS_START, Long.toString(binaryTransmissionStartTime));
    				configFile.setProperty(PROPERTY_TRANS_TIME, Integer.toString(binaryTransmissionTime));
    				configFile.setProperty(PROPERTY_BINARY_TIMESTAMP, Long.toString(binaryTimestamp));
    				configFile.setProperty(PROPERTY_BINARY_SIZE, Long.toString(binaryLength));
    				configFile.setProperty(PROPERTY_CHUNK_NUMBER, Long.toString(lastChunkNumber));
    				configFile.setProperty(PROPERTY_CHUNK_RESEND, Integer.toString(chunkresend));
    				configFile.setProperty(PROPERTY_STORAGE_TYPE, Boolean.toString(storeInDatabase));
    				configFile.setProperty(PROPERTY_TIME_DATE_FORMAT, datetimefm);
    				
    				try {
						configFile.store(new FileOutputStream(propertyFileName), null);
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
						dispose();
					}
    				
    				calculatedCRC.reset();

    				String relativeName;
    			    if (storeInDatabase)
    			    	relativeName = remoteBinaryName;
    			    else
    			    	relativeName = localBinaryName.replaceAll(rootBinaryDir, "");
					Serializable[] data = {binaryTimestamp, getDeviceID(), binaryLength, (long)0, binaryTransmissionTime, binaryTransmissionTime, chunkresend, filequeuesize, relativeName, null, null};
					percentDownloaded = 0;
					if(!dataProcessed(System.currentTimeMillis(), data)) {
						logger.warn("The binary data with >" + binaryTimestamp + "< could not be stored in the database.");
					}
				}
				
	    		binarySender.sendInitAck();
			}
			else if (pktType == CHUNK_PACKET) {
				// get number of this chunk
				long chunknum;
				int chunkresend;
				int filequeuesize;
				try {
					filequeuesize = toInteger(msg.getData()[1]);
					chunkresend = toInteger(msg.getData()[2]);
					chunknum = toLong(msg.getData()[3]);
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					dispose();
					continue;
				}
				if (logger.isDebugEnabled())
					logger.debug("Chunk for " + remoteBinaryName + " with number " + chunknum + " received");
				
				if (chunknum == lastChunkNumber)
					logger.info("chunk already received");
				else if (lastChunkNumber+1 == chunknum) {
					try {
						// store the binary chunk to disk
						File file = new File(localBinaryName);
						FileOutputStream fos;
						try {
							fos = new FileOutputStream(file, true);
						} catch (FileNotFoundException e) {
							logger.warn(e.getMessage());
							binarySender.requestRetransmissionOfBinary(remoteBinaryName);
							continue;
						}
						byte [] chunk = (byte[]) msg.getData()[4];
						calculatedCRC.update(chunk);
						if (logger.isDebugEnabled())
							logger.debug("updated crc: " + calculatedCRC.getValue());
						fos.write(chunk);
						fos.close();
						filelen = file.length();

						long timenow = System.currentTimeMillis();
						binaryTransmissionTime = (int) (binaryTransmissionTime+timenow-lastTransmissionTimestamp);
						lastTransmissionTimestamp = timenow;
						
						// write the actual binary length and chunk number to the property file
						// to be able to recover in case of a GSN failure
						configFile.setProperty(PROPERTY_DOWNLOADED_SIZE, Long.toString(filelen));
						configFile.setProperty(PROPERTY_CHUNK_NUMBER, Long.toString(chunknum));
						configFile.setProperty(PROPERTY_CHUNK_RESEND, Integer.toString(chunkresend));
	    				configFile.setProperty(PROPERTY_TRANS_TIME, Integer.toString(binaryTransmissionTime));
						configFile.store(new FileOutputStream(propertyFileName), null);

						if (logger.isDebugEnabled())
							logger.debug("actual length of concatenated binary is " + filelen + " bytes");

						if ((100*filelen)/binaryLength > percentDownloaded+5) {
		    				String relativeName;
		    			    if (storeInDatabase)
		    			    	relativeName = remoteBinaryName;
		    			    else
		    			    	relativeName = localBinaryName.replaceAll(rootBinaryDir, "");
							Serializable[] data = {binaryTimestamp, getDeviceID(), binaryLength, filelen, binaryTransmissionTime, (int)(timenow-binaryTransmissionStartTime), lastChunkResend+chunkresend, filequeuesize, relativeName, null, null};
							if(!dataProcessed(System.currentTimeMillis(), data)) {
								logger.warn("The binary data with >" + binaryTimestamp + "< could not be stored in the database.");
							}
							percentDownloaded += 5;
						}
					}
					catch (IOException e) {
						logger.error(e.getMessage(), e);
						dispose();
					}
				}
				else {
					// we should never reach this point...
					logger.error("received chunk number (received nr=" + chunknum + "/last nr=" + lastChunkNumber + ") out of order -> request binary retransmission");
					binarySender.requestRetransmissionOfBinary(remoteBinaryName);
					continue;
				}

				lastChunkNumber = chunknum;

	    		binarySender.sendChunkAck(lastChunkNumber);
			}
			else if (pktType == CRC_PACKET) {
				long crc;
				int filequeuesize;
				int chunkresend;
				try {
					filequeuesize = toInteger(msg.getData()[1]);
					chunkresend = toInteger(msg.getData()[2]);
					crc = toLong(msg.getData()[3]);
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					dispose();
					continue;
				}
				
				if (lastRecvPacketType == CRC_PACKET) {
					if (logger.isDebugEnabled())
						logger.debug("crc packet already received -> drop it");
		    		binarySender.sendCRCAck();
				}
				else {
					if (logger.isDebugEnabled())
						logger.debug("crc packet with crc32 >" + crc + "< received");
					
    				// do we really have the whole binary?
    				if ((new File(localBinaryName)).length() == binaryLength) {
    					// check crc
    					if (calculatedCRC.getValue() == crc) {
    						if (logger.isDebugEnabled())
    							logger.debug("crc is correct");
    						if (storeInDatabase) {
    							byte[] tmp = null;
    							File file = new File(localBinaryName);
    							FileInputStream fin;
    							
    							try {
    								fin = new FileInputStream(file);
    								// find index of first null byte
    								tmp = new byte[(int)file.length()];
    								fin.read(tmp);
    								fin.close();
    							} catch (FileNotFoundException e) {
    								logger.warn(e.getMessage());
    								binarySender.requestRetransmissionOfBinary(remoteBinaryName);
    								continue;
    							} catch (IOException e) {
    								logger.error(e.getMessage(), e);
    								dispose();
    							}

    							String relDir = remoteBinaryName;
    							long timenow = System.currentTimeMillis();
    							Serializable[] data = {binaryTimestamp, getDeviceID(), binaryLength, binaryLength, (int)(binaryTransmissionTime+timenow-lastTransmissionTimestamp), (int)(timenow-binaryTransmissionStartTime), lastChunkResend+chunkresend, filequeuesize, relDir, null, tmp};
    							if(!dataProcessed(System.currentTimeMillis(), data)) {
    								logger.warn("The binary data  (timestamp=" + binaryTimestamp + "/length=" + binaryLength + "/name=" + remoteBinaryName + ") could not be stored in the database.");
    							}
    							else {
    								if (logger.isDebugEnabled())
    									logger.debug("binary data (timestamp=" + binaryTimestamp + "/length=" + binaryLength + "/name=" + remoteBinaryName + ") successfully stored in database");
    							}
    							
    							file.delete();
    						}
    						else {
    							String relLocalName = localBinaryName.replaceAll(rootBinaryDir, "");
    							long timenow = System.currentTimeMillis();
    							Serializable[] data = {binaryTimestamp, getDeviceID(), binaryLength, binaryLength, (int)(binaryTransmissionTime+timenow-lastTransmissionTimestamp), (int)(timenow-binaryTransmissionStartTime), lastChunkResend+chunkresend, filequeuesize, relLocalName, rootBinaryDir, null};
    							if(!dataProcessed(System.currentTimeMillis(), data)) {
    								logger.warn("The binary data with >" + binaryTimestamp + "< could not be stored in the database.");
    							}
    							if (!(new File(localBinaryName)).setLastModified(binaryTimestamp))
    								logger.warn("could not set modification time for " + localBinaryName);
    							if (logger.isDebugEnabled())
    								logger.debug("binary data (timestamp=" + binaryTimestamp + "/length=" + binaryLength + "/name=" + remoteBinaryName + ") successfully stored on disk");
    						}
    					
    						File stat = new File(propertyFileName);
    						stat.delete();
    						
    						localBinaryName = null;

    			    		binarySender.sendCRCAck();
    					}
    					else {
    						logger.warn("crc does not match (received=" + crc + "/calculated=" + calculatedCRC.getValue() + ") -> request binary retransmission");
    						binarySender.requestRetransmissionOfBinary(remoteBinaryName);
    					}
    				}
    				else {
    					// we should never reach this point as well...
    					logger.error("binary length does not match (actual length=" + (new File(localBinaryName)).length() + "/should be=" + binaryLength + ") -> request binary retransmission (should never happen!)");
						binarySender.requestRetransmissionOfBinary(remoteBinaryName);
    				}
    			}
			}
			lastRecvPacketType = pktType;
    	}
        
        logger.info("thread stopped");
    }
	

	@Override
	public boolean messageReceived(int deviceID, long timestamp, Serializable[] packet) {
		try {
			if (logger.isDebugEnabled())
				logger.debug("message received with timestamp " + timestamp);
			msgQueue.add(new Message(timestamp, packet));
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return false;
		}
		return true;
	}


	@Override
	public void remoteConnEstablished() {
		if (logger.isDebugEnabled())
			logger.debug("Connection established");
		if (connectionTestTimer != null)
			connectionTestTimer.cancel();
		else
			connectionTestTimer = new Timer("ConnectionCheck");
		
		File sf = new File(propertyFileName);
		if (sf.exists()) {
			try {
				configFile.load(new FileInputStream(propertyFileName));
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				dispose();
				return;
			}
			
			try {
				remoteBinaryName = configFile.getProperty(PROPERTY_REMOTE_BINARY);
				if (remoteBinaryName == null)
					throw new Exception("property >" + PROPERTY_REMOTE_BINARY + "< not found in " + propertyFileName);
				String prop = configFile.getProperty(PROPERTY_DOWNLOADED_SIZE);
				if (prop == null)
					throw new Exception("property >" + PROPERTY_DOWNLOADED_SIZE + "< not found in " + propertyFileName);
				downloadedSize = Long.valueOf(prop).longValue();
				prop = configFile.getProperty(PROPERTY_BINARY_TIMESTAMP);
				if (prop == null)
					throw new Exception("property >" + PROPERTY_BINARY_TIMESTAMP + "< not found in " + propertyFileName);
				binaryTimestamp = Long.valueOf(prop).longValue();
				prop = configFile.getProperty(PROPERTY_BINARY_SIZE);
				if (prop == null)
					throw new Exception("property >" + PROPERTY_BINARY_SIZE + "< not found in " + propertyFileName);
				binaryLength = Long.valueOf(prop).longValue();
				prop = configFile.getProperty(PROPERTY_TRANS_START);
				if (prop == null)
					throw new Exception("property >" + PROPERTY_TRANS_START + "< not found in " + propertyFileName);
				binaryTransmissionStartTime = Long.valueOf(prop).longValue();
				prop = configFile.getProperty(PROPERTY_TRANS_TIME);
				if (prop == null)
					throw new Exception("property >" + PROPERTY_TRANS_TIME + "< not found in " + propertyFileName);
				binaryTransmissionTime = Integer.valueOf(prop).intValue();
				prop = configFile.getProperty(PROPERTY_CHUNK_RESEND);
				if (prop == null)
					throw new Exception("property >" + PROPERTY_CHUNK_RESEND + "< not found in " + propertyFileName);
				lastChunkResend = Integer.valueOf(prop).intValue();
				prop = configFile.getProperty(PROPERTY_STORAGE_TYPE);
				if (prop == null)
					throw new Exception("property >" + PROPERTY_STORAGE_TYPE + "< not found in " + propertyFileName);
				storeInDatabase = Boolean.valueOf(prop).booleanValue();
				prop = configFile.getProperty(PROPERTY_TIME_DATE_FORMAT);
				if (prop == null)
					throw new Exception("property >" + PROPERTY_TIME_DATE_FORMAT + "< not found in " + propertyFileName);

				lastTransmissionTimestamp = System.currentTimeMillis();
				folderdatetimefm = new SimpleDateFormat(prop);
			    if (storeInDatabase) {
			    	localBinaryName = rootBinaryDir + TEMP_BINARY_NAME;
			    }
			    else {
				    File f = new File(remoteBinaryName);
			    	String subpath = f.getParent();
			    	if (subpath == null	)
			    		subpath = "";
			    	else if(!subpath.endsWith("/"))
						subpath += "/";

					if (logger.isDebugEnabled())
						logger.debug("subpath: " + subpath);
			    	
				    String datedir = rootBinaryDir + subpath + folderdatetimefm.format(new java.util.Date(binaryTimestamp)) + "/";
				    String filename = f.getName();
				    localBinaryName = datedir + filename;
			    }
			    
			    percentDownloaded = (short) Math.ceil(downloadedSize*100/binaryLength);
			    if ((new File(localBinaryName)).exists())
			    	calcChecksumThread.newChecksum(localBinaryName);
			    else {
			    	logger.error("binary >" + localBinaryName + "< does not exist -> request retransmission");
			    	binarySender.requestRetransmissionOfBinary(remoteBinaryName);
			    }
			} catch (Exception e) {
		    	logger.error(e.getMessage() + " -> request new binary");
				binarySender.requestNewBinary();
			}
		} else {
			binarySender.requestNewBinary();
		}
	}


	@Override
	public void remoteConnLost() {
		if (logger.isDebugEnabled())
			logger.debug("Connection lost");

		msgQueue.clear();
		binarySender.stopSending();
	}
}





/**
 * A message to be put into the message queue.
 * 
 * @author Tonio Gsell
 */
class Message {
	protected long timestamp;
	protected Serializable[] packet;
	
	Message() {	}
	
	Message(long t, Serializable[] pkt) {
		timestamp = t;
		packet = pkt.clone();
	}
	
	public long getTimestamp() {
		return this.timestamp;
	}
	
	public Serializable[] getData() {
		return this.packet;
	}
}


/**
 * Offers the functionality to calculate the checksum of a partly downloaded
 * binary. After the checksum has been calculated the deployment is asked to
 * resume the download.
 * 
 * @author Tonio Gsell
 */
class CalculateChecksum extends Thread {
	private boolean dispose = false;
    private CheckedInputStream cis = null;
    private BinaryPlugin parent = null;
	private LinkedBlockingQueue<String> fileQueue = new LinkedBlockingQueue<String>();
	
	public CalculateChecksum(BinaryPlugin plug) {
		setName("CalculateChecksum-" + plug.coreStationName + "-Thread");
		parent = plug;
	}
	
	public void run() {
		String file;
        
        parent.logger.info("thread started");
		
		while (!this.dispose) {
			try {
				file = fileQueue.take();
			} catch (InterruptedException e) {
				if (parent.logger.isDebugEnabled())
					parent.logger.debug(e.getMessage());
				break;
			}
			if (this.dispose)
				break;
			
			// if the property file exists we have already downloaded a part of a binary -> resume
			// calculate crc from already downloaded binary
			if (parent.logger.isDebugEnabled())
				parent.logger.debug("calculating cheksum for already downloaded part of binary >" + parent.localBinaryName + "<");
	        try {
	            // Computer CRC32 checksum
	            cis = new CheckedInputStream(
	                    new FileInputStream(file), new CRC32());

	            byte[] buf = new byte[4096];
		        while(cis.read(buf) >= 0 && !this.dispose) {
		        	yield();
		        }
		        if (this.dispose)
		        	break;
	        } catch (Exception e) {
				// no good... -> ask for retransmission of the binary
				parent.logger.error(e.getMessage(), e);
				parent.binarySender.requestRetransmissionOfBinary(parent.remoteBinaryName);
				continue;
			}
	        
	        parent.calculatedCRC = (CRC32) cis.getChecksum();

			if (parent.logger.isDebugEnabled())
				parent.logger.debug("recalculated crc (" + parent.calculatedCRC.getValue() + ") from " + parent.localBinaryName);
			
			parent.binarySender.resumeBinary(parent.remoteBinaryName, parent.downloadedSize, Long.valueOf(parent.configFile.getProperty(BinaryPlugin.PROPERTY_CHUNK_NUMBER)).longValue()+1, parent.calculatedCRC.getValue());
		}
        
        parent.logger.info("thread stopped");
	}
	
	



	/**
	 * Calculate the checksum of the partly downloaded binary.
	 * 
	 * @param binary the partly downloaded binary which should be resumed
	 */
	public void newChecksum(String binary) {
		fileQueue.add(binary);
	}
	
	public void dispose() {
		this.dispose = true;
		fileQueue.add("");
	}
}

class BinarySender extends Thread
{
    private BinaryPlugin parent = null;
	private boolean stopped = false;
	private boolean triggered = false;
	private Object event = new Object();
	private Serializable [] packet = null;
	
	BinarySender(BinaryPlugin plug) {
		setName("BinarySender-" + plug.coreStationName + "-Thread");
		parent = plug;
	}
	
	public void run() {
		parent.logger.info("thread started");
		while (!stopped) {
			try {
				synchronized (event) {
					if (!triggered)
						event.wait();
					else {
						event.wait(BinaryPlugin.RESEND_INTERVAL_SEC*1000);
						if (triggered)
							parent.logger.info("resend message");
						else
							continue;
					}
				}
			} catch (InterruptedException e) {
				break;
			}
			
			if(triggered) {
				try {
					if(!parent.sendRemote(System.currentTimeMillis(), packet, parent.priority))
						stopSending();
				} catch (IOException e) {
					parent.logger.warn(e.getMessage());
				}
			}
		}
		parent.logger.info("thread stopped");
	}
	
	
	private void trigger() {
		synchronized (event) {
			triggered = true;
			event.notify();
		}
	}
	
	public void stopSending() {
		if (parent.logger.isDebugEnabled())
			parent.logger.debug("stop sending");
		synchronized (event) {
			triggered = false;
			packet = null;
			event.notify();
		}
	}
	
	public void dispose() {
		stopped = true;
		super.interrupt();
	}
	
	
	public void sendChunkAck(long ackNr) {
		if (triggered)
			parent.logger.error("already sending a message");
		else {
			if (parent.logger.isDebugEnabled())
				parent.logger.debug("acknowledge for chunk number >" + ackNr + "< sent");
			try {
				Serializable [] ack = {BinaryPlugin.ACK_PACKET, BinaryPlugin.CHUNK_PACKET, ackNr};
				parent.sendRemote(System.currentTimeMillis(), ack, parent.priority);
			} catch (IOException e) {
				parent.logger.warn(e.getMessage());
			}
		}
	}
	
	
	public void sendInitAck() {
		if (triggered)
			parent.logger.error("already sending a message");
		else {
			if (parent.logger.isDebugEnabled())
				parent.logger.debug("init acknowledge sent");
			Serializable [] packet = {BinaryPlugin.ACK_PACKET, BinaryPlugin.INIT_PACKET};
			try {
				parent.sendRemote(System.currentTimeMillis(), packet, parent.priority);
			} catch (IOException e) {
				parent.logger.warn(e.getMessage());
			}
		}
	}
	
	
	public void sendCRCAck() {
		if (triggered)
			parent.logger.error("already sending a message");
		else {
			if (parent.logger.isDebugEnabled())
				parent.logger.debug("crc acknowledge sent");
			Serializable [] packet = {BinaryPlugin.ACK_PACKET, BinaryPlugin.CRC_PACKET};
			try {
				parent.sendRemote(System.currentTimeMillis(), packet, parent.priority);
			} catch (IOException e) {
				parent.logger.warn(e.getMessage());
			}
		}
	}



	/**
	 * Get a new binary from the deployment.
	 */
	public void requestNewBinary() {
		if (triggered)
			parent.logger.error("already sending a message");
		else {
			packet = new Serializable[] {BinaryPlugin.INIT_PACKET};
			trigger();
		}
	}



	/**
	 * Retransmit the specified binary from the deployment.
	 */
	public void requestRetransmissionOfBinary(String remoteLocation) {	
		parent.calculatedCRC.reset();
		// delete the file if it already exists
		File f = new File(parent.localBinaryName);
	    if (f.exists()) {
			if (parent.logger.isDebugEnabled())
				parent.logger.debug("overwrite already existing binary >" + parent.localBinaryName + "<");
	    	f.delete();
	    }
		requestSpecificBinary(remoteLocation, 0, 0, 0);
	}


	/**
	 * Resume the specified binary from the deployment
	 * 
	 * @param remoteLocation the relative location of the remote binary
	 * @param sizeAlreadyDownloaded the size of the binary which has already been downloaded
	 * @param chunkNr the number of the last chunk which has already been downloaded
	 * 
	 * @throws Exception if an I/O error occurs.
	 */
	public void resumeBinary(String remoteLocation, long sizeAlreadyDownloaded, long chunkNr, long crc) {
		requestSpecificBinary(remoteLocation, sizeAlreadyDownloaded, chunkNr, crc);
	}
	
	
	private void requestSpecificBinary(String remoteLocation, long sizeAlreadyDownloaded, long chunkNr, long crc) {
		if (triggered)
			parent.logger.error("already sending a message");
		else {
			packet = new Serializable[] {BinaryPlugin.RESEND_PACKET, sizeAlreadyDownloaded, chunkNr, crc, remoteLocation};
			
			parent.lastChunkNumber = chunkNr-1;
			
			trigger();
		}
	}
}



/**
 * Pretends a connection establishment on fire.
 */
class ConnectionCheckTimer extends TimerTask {
	private BinaryPlugin parent;
	
	public ConnectionCheckTimer(BinaryPlugin parent) {
		this.parent = parent;
	}
	
	public void run() {
		if (parent.logger.isDebugEnabled())
			parent.logger.debug("connection check timer fired");
		if (parent.isConnected())
			parent.remoteConnEstablished();
	}
}
