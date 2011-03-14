package gsn.wrappers.backlog.plugins;

import org.apache.log4j.Logger;

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
import java.util.TimeZone;
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
 * The 'storage-directory' attribute (defined in the storage section of the virtual sensor's XML file) has
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
		
	private static final String PROPERTY_DEVICE_ID = "device_id";
	private static final String PROPERTY_REMOTE_BINARY = "remote_binary";
	private static final String PROPERTY_DOWNLOADED_SIZE = "downloaded_size";
	private static final String PROPERTY_TRANS_START = "transmission_start";
	private static final String PROPERTY_TRANS_TIME = "transmission_time";
	private static final String PROPERTY_BINARY_TIMESTAMP = "timestamp";
	private static final String PROPERTY_BINARY_SIZE = "file_size";
	private static final String PROPERTY_CHUNK_RESEND = "chunk_resend";
	private static final String PROPERTY_STORAGE_TYPE = "storage_type";
	private static final String PROPERTY_TIME_DATE_FORMAT = "time_date_format";
	
	private static final String TEMP_BINARY_NAME = "binaryplugin_download.part";
	private static final String PROPERTY_FILE_NAME = "gsnBinaryStat";

	private static final byte ACK_PACKET = 0;
	private static final byte START_PACKET = 1;
	private static final byte INIT_PACKET = 2;
	private static final byte RESEND_PACKET = 3;
	private static final byte CHUNK_PACKET = 4;
	private static final byte CRC_PACKET = 5;

	private SimpleDateFormat folderdatetimefm;

	private String rootBinaryDir;

	private final transient Logger logger = Logger.getLogger( BinaryPlugin.class );
	
	private DataField[] dataField = new DataField[] {
			new DataField("GENERATION_TIME", "BIGINT"),
			new DataField("DEVICE_ID", "INTEGER"),

			new DataField("FILE_SIZE", "BIGINT"),
			new DataField("TRANSMITTED", "BIGINT"),
			new DataField("TRANSMISSION_TIME_EFF", "INTEGER"),
			new DataField("TRANSMISSION_TIME_TOT", "INTEGER"),
			new DataField("CHUNK_RETRANSMISSIONS", "INTEGER"),
			new DataField("FILE_QUEUE_LENGTH", "INTEGER"),
			new DataField("FILE_QUEUE_SIZE", "BIGINT"),
			new DataField("RELATIVE_FILE", "VARCHAR(255)"),
			new DataField("FILE_COMPLETE", "SMALLINT"),
			new DataField("DATA", "binary")};
	
	private LinkedBlockingQueue<Message> msgQueue = new LinkedBlockingQueue<Message>();
	private Integer deviceID = null;
	private Integer binaryDeviceID = null;
	private boolean storeInDatabase;
	private Properties configFile = new Properties();
	private long binaryTimestamp = -1;
	private long binaryLength = -1;
	private long lastTransmissionTimestamp;
	private int binaryTransmissionTime;
	private long binaryTransmissionStartTime;
	private short percentDownloaded;
	private CRC32 calculatedCRC = new CRC32();
	private String remoteBinaryName = null;
	private String localBinaryName = null;
	private long downloadedSize = -1;
	private int lastChunkResend;
	private static Set<String> coreStationsList = new HashSet<String>();
	
	long lastReceivedPacketNr = -1;

	private String coreStationName = null;
	private String deploymentName = null;

	private boolean dispose = false;
	
	
	@Override
	public boolean initialize ( BackLogWrapper backlogwrapper, String coreStationName, String deploymentName) {
		activeBackLogWrapper = backlogwrapper;
		this.coreStationName = coreStationName;
		this.deploymentName = deploymentName;

		AddressBean addressBean = getActiveAddressBean();
		String p = addressBean.getPredicateValue("priority");
		if (p == null)
			priority = null;
		else
			priority = Integer.valueOf(p);
		
		try {
			rootBinaryDir = addressBean.getVirtualSensorConfig().getStorage().getStorageDirectory();
		} catch (NullPointerException e){
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

		if (logger.isDebugEnabled()) {
			logger.debug("binary root directory: " + rootBinaryDir);
		}
        
        registerListener();

        this.setName("BinaryPlugin-" + coreStationName + "-Thread");
		
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

    	Message msg;
    	Serializable[] sendPacket = null;
    	while (!dispose) {
			try {
				msg = msgQueue.take();
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
				break;
			}
			if (dispose)
				break;
        	
			int chunkresend;
    		// get packet type
    		int pktNr;
    		byte pktType;
    		try {
    			pktNr = toInteger(msg.getData()[0]);
				chunkresend = toInteger(msg.getData()[1]);
	    		pktType = (Byte) msg.getData()[2];
			} catch (Exception e) {
				logger.error(e.getMessage());
				break;
			}
    		
    		if (pktNr == lastReceivedPacketNr)
    			logger.debug("packet number " + pktNr + " already received");
    		else {
    			lastReceivedPacketNr = pktNr;
    			if (pktType == START_PACKET)
    				sendPacket = startPacketReceived();
    			else if (pktType == INIT_PACKET)
    				sendPacket = initPacketReceived(msg, pktNr, chunkresend);
				else if (pktType == CHUNK_PACKET)
					sendPacket = chunkPacketReceived(msg, pktNr, chunkresend);
				else if (pktType == CRC_PACKET)
					sendPacket = crcPacketReceived(msg, pktNr, chunkresend);
    		}

    		if (sendPacket != null) {
				// send answer for received packet
				try {
					sendRemote(System.currentTimeMillis(), sendPacket, priority);
					if (logger.isDebugEnabled())
						logger.debug("acknowledge sent for packet type " + pktType);
				} catch (IOException e) {
					logger.warn(e.getMessage());
				}
    		}
    		else
    			logger.warn("sendPacket is null");
    	}
        
        logger.info("thread stopped");
    }


	private Serializable[] startPacketReceived() {
		if (deviceID != null && deviceID.compareTo(getDeviceID()) != 0) {
			logger.warn("device id has changed from " + Integer.toString(deviceID) + " to " + getDeviceID());
			String propertyfile = rootBinaryDir + deploymentName + "/." + Integer.toString(deviceID) + "_" + PROPERTY_FILE_NAME;
			File sf = new File(propertyfile);
			if (sf.delete())
				logger.warn("property file >" + propertyfile + "< for old device id " + Integer.toString(deviceID) + " has been deleted");
		}
		
		deviceID = getDeviceID();
		
		String propertyfile = rootBinaryDir + deploymentName + "/." + Integer.toString(deviceID) + "_" + PROPERTY_FILE_NAME;
		File sf = new File(propertyfile);
		if (sf.exists()) {
			try {
				configFile.load(new FileInputStream(propertyfile));
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				dispose();
				return null;
			}
			
			try {
				String prop = configFile.getProperty(PROPERTY_DEVICE_ID);
				if (prop == null)
					throw new Exception("property >" + PROPERTY_DEVICE_ID + "< not found in " + propertyfile);
				binaryDeviceID = Integer.valueOf(prop).intValue();
				remoteBinaryName = configFile.getProperty(PROPERTY_REMOTE_BINARY);
				if (remoteBinaryName == null)
					throw new Exception("property >" + PROPERTY_REMOTE_BINARY + "< not found in " + propertyfile);
				prop = configFile.getProperty(PROPERTY_DOWNLOADED_SIZE);
				if (prop == null)
					throw new Exception("property >" + PROPERTY_DOWNLOADED_SIZE + "< not found in " + propertyfile);
				downloadedSize = Long.valueOf(prop).longValue();
				prop = configFile.getProperty(PROPERTY_BINARY_TIMESTAMP);
				if (prop == null)
					throw new Exception("property >" + PROPERTY_BINARY_TIMESTAMP + "< not found in " + propertyfile);
				binaryTimestamp = Long.valueOf(prop).longValue();
				prop = configFile.getProperty(PROPERTY_BINARY_SIZE);
				if (prop == null)
					throw new Exception("property >" + PROPERTY_BINARY_SIZE + "< not found in " + propertyfile);
				binaryLength = Long.valueOf(prop).longValue();
				prop = configFile.getProperty(PROPERTY_TRANS_START);
				if (prop == null)
					throw new Exception("property >" + PROPERTY_TRANS_START + "< not found in " + propertyfile);
				binaryTransmissionStartTime = Long.valueOf(prop).longValue();
				prop = configFile.getProperty(PROPERTY_TRANS_TIME);
				if (prop == null)
					throw new Exception("property >" + PROPERTY_TRANS_TIME + "< not found in " + propertyfile);
				binaryTransmissionTime = Integer.valueOf(prop).intValue();
				prop = configFile.getProperty(PROPERTY_CHUNK_RESEND);
				if (prop == null)
					throw new Exception("property >" + PROPERTY_CHUNK_RESEND + "< not found in " + propertyfile);
				lastChunkResend = Integer.valueOf(prop).intValue();
				prop = configFile.getProperty(PROPERTY_STORAGE_TYPE);
				if (prop == null)
					throw new Exception("property >" + PROPERTY_STORAGE_TYPE + "< not found in " + propertyfile);
				storeInDatabase = Boolean.valueOf(prop).booleanValue();
				prop = configFile.getProperty(PROPERTY_TIME_DATE_FORMAT);
				if (prop == null)
					throw new Exception("property >" + PROPERTY_TIME_DATE_FORMAT + "< not found in " + propertyfile);

				lastTransmissionTimestamp = System.currentTimeMillis();
				folderdatetimefm = new SimpleDateFormat(prop);
				folderdatetimefm.setTimeZone(TimeZone.getTimeZone("UTC"));
			    if (storeInDatabase) {
			    	localBinaryName = rootBinaryDir + deploymentName + "/" + Integer.toString(deviceID) + "_" + TEMP_BINARY_NAME;
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
			    	
				    String datedir = rootBinaryDir + deploymentName + "/" + Integer.toString(binaryDeviceID) + "/" + subpath + folderdatetimefm.format(new java.util.Date(binaryTimestamp)) + "/";
				    String filename = f.getName();
				    localBinaryName = datedir + filename;
			    }
			    
			    percentDownloaded = (short) Math.ceil(downloadedSize*100/binaryLength);
			    if ((new File(localBinaryName)).exists())
			    	return calcChecksum(localBinaryName);
			    else {
			    	logger.error("binary >" + localBinaryName + "< does not exist -> request retransmission");
			    	return binaryRetransmissionRequestPacket(remoteBinaryName);
			    }
			} catch (Exception e) {
		    	logger.error(e.getMessage() + " -> request new binary");
		    	return newBinaryRequestPacket();
			}
		} else {
			return newBinaryRequestPacket();
		}
	}
	
	
	private Serializable[] initPacketReceived(Message msg, int pktNr, int chunkresend) {
		if (logger.isDebugEnabled())
			logger.debug("init packet received");
		
		// get file info
		long filequeuesize;
		int filequeuelength;
		try {
			filequeuesize = toLong(msg.getData()[3]);
			filequeuelength = toInteger(msg.getData()[4]);
			binaryDeviceID = toInteger(msg.getData()[5]);
			binaryTimestamp = toLong(msg.getData()[6]);
			binaryLength = toLong(msg.getData()[7]);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			dispose();
			return null;
		}
		byte storage = (Byte) msg.getData()[8];
		remoteBinaryName = (String) msg.getData()[9];
		String datetimefm = (String) msg.getData()[10];
		
		try {
			folderdatetimefm = new SimpleDateFormat(datetimefm);
			folderdatetimefm.setTimeZone(TimeZone.getTimeZone("UTC"));
			
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

		    String binaryDir = null;
		    if (storeInDatabase) {
		    	localBinaryName = rootBinaryDir + deploymentName + "/" + Integer.toString(deviceID) + "_" + TEMP_BINARY_NAME;
				
				return new Serializable[]{pktNr, ACK_PACKET, INIT_PACKET};
		    }
		    else {
				binaryDir = rootBinaryDir + deploymentName + "/" + Integer.toString(binaryDeviceID) + "/";
				File f = new File(binaryDir);
				if (!f.isDirectory()) {
			    	if (!f.mkdirs()) {
			    		logger.error("could not mkdir >" + binaryDir + "< -> drop this binary");
			    		return newBinaryRequestPacket();
    				}
			    	else
			    		logger.info("created new storage directory >" + binaryDir + "<");
				}
				
				logger.debug("storage directory is >" + binaryDir + "<");
				
				f = new File(remoteBinaryName);
		    	String subpath = f.getParent();
		    	if (subpath == null	)
		    		subpath = "";
				if (logger.isDebugEnabled())
					logger.debug("subpath: " + subpath);
				
				if(!subpath.endsWith("/"))
					subpath += "/";
		    	
			    String datedir = binaryDir + subpath + folderdatetimefm.format(new java.util.Date(binaryTimestamp)) + "/";
			    String filename = f.getName();
			    f = new File(datedir);
			    if (!f.exists()) {
			    	if (!f.mkdirs()) {
			    		logger.error("could not mkdir >" + datedir + "<  -> drop remote binary " + remoteBinaryName);
			    		return newBinaryRequestPacket();
			    	}
			    }
				localBinaryName = datedir + filename;
		    }
			
			// delete the file if it already exists
			File f = new File(localBinaryName);
		    if (f.exists()) {
				if (logger.isDebugEnabled())
					logger.debug("overwrite already existing binary >" + localBinaryName + "<");
		    	f.delete();
		    }
		    
			lastChunkResend = 0;

			binaryTransmissionStartTime = msg.getTimestamp();
			lastTransmissionTimestamp = System.currentTimeMillis();
			binaryTransmissionTime = 0;
			
		    // write the new binary info to the property file
			configFile.setProperty(PROPERTY_DEVICE_ID, Integer.toString(binaryDeviceID));
			configFile.setProperty(PROPERTY_REMOTE_BINARY, remoteBinaryName);
			configFile.setProperty(PROPERTY_DOWNLOADED_SIZE, Long.toString(0));
			configFile.setProperty(PROPERTY_TRANS_START, Long.toString(binaryTransmissionStartTime));
			configFile.setProperty(PROPERTY_TRANS_TIME, Integer.toString(binaryTransmissionTime));
			configFile.setProperty(PROPERTY_BINARY_TIMESTAMP, Long.toString(binaryTimestamp));
			configFile.setProperty(PROPERTY_BINARY_SIZE, Long.toString(binaryLength));
			configFile.setProperty(PROPERTY_CHUNK_RESEND, Integer.toString(chunkresend));
			configFile.setProperty(PROPERTY_STORAGE_TYPE, Boolean.toString(storeInDatabase));
			configFile.setProperty(PROPERTY_TIME_DATE_FORMAT, datetimefm);

			try {
				configFile.store(new FileOutputStream(rootBinaryDir + deploymentName + "/." + Integer.toString(deviceID) + "_" + PROPERTY_FILE_NAME), null);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				dispose();
				return null;
			}
			
			calculatedCRC.reset();

			String relativeName;
		    if (storeInDatabase)
		    	relativeName = remoteBinaryName;
		    else
		    	relativeName = localBinaryName.replaceAll(binaryDir, "");
			Serializable[] data = {binaryTimestamp, binaryDeviceID, binaryLength, (long)0, binaryTransmissionTime, binaryTransmissionTime, chunkresend, filequeuelength, filequeuesize, relativeName, (short)0, null};
			percentDownloaded = 0;
			if(!dataProcessed(System.currentTimeMillis(), data)) {
				logger.warn("The binary data with >" + binaryTimestamp + "< could not be stored in the database.");
			}
			
			return new Serializable[]{pktNr, ACK_PACKET, INIT_PACKET};
		} catch (IllegalArgumentException e) {
			logger.error("the received init packet does contain a mallformed date time format >" + datetimefm + "<! Please check your backlog configuration on the deployment -> drop this binary");
			return newBinaryRequestPacket();
		}
	}


	private Serializable[] chunkPacketReceived(Message msg, int pktNr, int chunkresend) {
		// get number of this chunk
		long filequeuesize;
		int filequeuelength;
		try {
			filequeuesize = toLong(msg.getData()[3]);
			filequeuelength = toInteger(msg.getData()[4]);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			dispose();
			return null;
		}
		if (logger.isDebugEnabled())
			logger.debug("Chunk for " + remoteBinaryName + " with packet number " + pktNr + " received");
		
		try {
			// store the binary chunk to disk
			File file = new File(localBinaryName);
			FileOutputStream fos;
			try {
				fos = new FileOutputStream(file, true);
				byte [] chunk = (byte[]) msg.getData()[5];
				calculatedCRC.update(chunk);
				if (logger.isDebugEnabled())
					logger.debug("updated crc: " + calculatedCRC.getValue());
				fos.write(chunk);
				fos.close();
				long filelen = file.length();

				long timenow = System.currentTimeMillis();
				binaryTransmissionTime += (int) (timenow-lastTransmissionTimestamp);
				lastTransmissionTimestamp = timenow;
				
				// write the actual binary length and chunk number to the property file
				// to be able to recover in case of a GSN failure
				configFile.setProperty(PROPERTY_DOWNLOADED_SIZE, Long.toString(filelen));
				configFile.setProperty(PROPERTY_CHUNK_RESEND, Integer.toString(chunkresend));
				configFile.setProperty(PROPERTY_TRANS_TIME, Integer.toString(binaryTransmissionTime));
				configFile.store(new FileOutputStream(rootBinaryDir + deploymentName + "/." + Integer.toString(deviceID) + "_" + PROPERTY_FILE_NAME), null);

				if (logger.isDebugEnabled())
					logger.debug("actual length of concatenated binary is " + filelen + " bytes");

				if ((100*filelen)/binaryLength > percentDownloaded+5) {
    				String relativeName;
    			    if (storeInDatabase)
    			    	relativeName = remoteBinaryName;
    			    else
    			    	relativeName = localBinaryName.replaceAll(rootBinaryDir + deploymentName + "/" + Integer.toString(binaryDeviceID) + "/", "");
					Serializable[] data = {binaryTimestamp, binaryDeviceID, binaryLength, filelen, binaryTransmissionTime, (int)(timenow-binaryTransmissionStartTime), lastChunkResend+chunkresend, filequeuelength, filequeuesize, relativeName, (short)0, null};
					if(!dataProcessed(System.currentTimeMillis(), data)) {
						logger.warn("The binary data with >" + binaryTimestamp + "< could not be stored in the database.");
					}
					percentDownloaded += 5;
				}
				
				return new Serializable[]{pktNr, ACK_PACKET, CHUNK_PACKET};
			} catch (FileNotFoundException e) {
				logger.warn(e.getMessage());
				return binaryRetransmissionRequestPacket(remoteBinaryName);
			}
		}
		catch (IOException e) {
			logger.error(e.getMessage(), e);
			dispose();
			return null;
		}
	}


	private Serializable[] crcPacketReceived(Message msg, int pktNr, int chunkresend) {
		long crc;
		long filequeuesize;
		int filequeuelength;
		try {
			filequeuesize = toLong(msg.getData()[3]);
			filequeuelength = toInteger(msg.getData()[4]);
			crc = toLong(msg.getData()[5]);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			dispose();
			return null;
		}

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
						
						String relDir = remoteBinaryName;
						long timenow = System.currentTimeMillis();
						Serializable[] data = {binaryTimestamp, binaryDeviceID, binaryLength, binaryLength, (int)(binaryTransmissionTime+timenow-lastTransmissionTimestamp), (int)(timenow-binaryTransmissionStartTime), lastChunkResend+chunkresend, filequeuelength, filequeuesize, relDir, (short)1, tmp};
						if(!dataProcessed(System.currentTimeMillis(), data)) {
							logger.warn("The binary data  (timestamp=" + binaryTimestamp + "/length=" + binaryLength + "/name=" + remoteBinaryName + ") could not be stored in the database.");
						}
						else {
							if (logger.isDebugEnabled())
								logger.debug("binary data (timestamp=" + binaryTimestamp + "/length=" + binaryLength + "/name=" + remoteBinaryName + ") successfully stored in database");
						}
						
						file.delete();
						
						File stat = new File(rootBinaryDir + deploymentName + "/." + Integer.toString(deviceID) + "_" + PROPERTY_FILE_NAME);
						stat.delete();
						
						localBinaryName = null;
					} catch (FileNotFoundException e) {
						logger.warn(e.getMessage());
						return binaryRetransmissionRequestPacket(remoteBinaryName);
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
						dispose();
						return null;
					}
				}
				else {
					String relLocalName = localBinaryName.replaceAll(rootBinaryDir + deploymentName + "/" + Integer.toString(binaryDeviceID) + "/", "");
					long timenow = System.currentTimeMillis();
					Serializable[] data = {binaryTimestamp, binaryDeviceID, binaryLength, binaryLength, (int)(binaryTransmissionTime+timenow-lastTransmissionTimestamp), (int)(timenow-binaryTransmissionStartTime), lastChunkResend+chunkresend, filequeuelength, filequeuesize, relLocalName, (short)1, null};
					if(!dataProcessed(System.currentTimeMillis(), data)) {
						logger.warn("The binary data with >" + binaryTimestamp + "< could not be stored in the database.");
					}
					if (!(new File(localBinaryName)).setLastModified(binaryTimestamp))
						logger.warn("could not set modification time for " + localBinaryName);
					if (logger.isDebugEnabled())
						logger.debug("binary data (timestamp=" + binaryTimestamp + "/length=" + binaryLength + "/name=" + remoteBinaryName + ") successfully stored on disk");
					
					File stat = new File(rootBinaryDir + deploymentName + "/." + Integer.toString(deviceID) + "_" + PROPERTY_FILE_NAME);
					stat.delete();
					
					localBinaryName = null;
				}
				
				return new Serializable[]{pktNr, ACK_PACKET, CRC_PACKET};
			}
			else {
				logger.warn("crc does not match (received=" + crc + "/calculated=" + calculatedCRC.getValue() + ") -> request binary retransmission");
				return binaryRetransmissionRequestPacket(remoteBinaryName);
			}
		}
		else {
			// we should never reach this point as well...
			logger.error("binary length does not match (actual length=" + (new File(localBinaryName)).length() + "/should be=" + binaryLength + ") -> request binary retransmission (should never happen!)");
			return binaryRetransmissionRequestPacket(remoteBinaryName);
		}
	}
	
	
	private Serializable[] calcChecksum(String file) {
		CheckedInputStream cis;
		// if the property file exists we have already downloaded a part of a binary -> resume
		// calculate crc from already downloaded binary
		if (logger.isDebugEnabled())
			logger.debug("calculating cheksum for already downloaded part of binary >" + localBinaryName + "<");
        try {
            // Computer CRC32 checksum
            cis = new CheckedInputStream(
                    new FileInputStream(file), new CRC32());

            byte[] buf = new byte[4096];
	        while(cis.read(buf) >= 0 && !dispose) {
	        	yield();
	        }
	        if (dispose)
	        	return new Serializable[]{};
        } catch (Exception e) {
			// no good... -> ask for retransmission of the binary
			logger.error(e.getMessage(), e);
			return binaryRetransmissionRequestPacket(remoteBinaryName);
		}
        
        calculatedCRC = (CRC32) cis.getChecksum();

		if (logger.isDebugEnabled())
			logger.debug("recalculated crc (" + calculatedCRC.getValue() + ") from " + localBinaryName);
		
		return resumeBinaryRequestPacket(remoteBinaryName, downloadedSize, calculatedCRC.getValue());
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
	public void remoteConnLost() {
		if (logger.isDebugEnabled())
			logger.debug("Connection lost");

		msgQueue.clear();
		lastReceivedPacketNr = -1;
	}


	private Serializable[] newBinaryRequestPacket() {
		return new Serializable[]{lastReceivedPacketNr, INIT_PACKET};
	}


	private Serializable[] binaryRetransmissionRequestPacket(String remoteLocation) {	
		calculatedCRC.reset();
		// delete the file if it already exists
		File f = new File(localBinaryName);
	    if (f.exists()) {
			if (logger.isDebugEnabled())
				logger.debug("overwrite already existing binary >" + localBinaryName + "<");
	    	f.delete();
	    }
		return specificBinaryRequestPacket(remoteLocation, 0, 0);
	}


	private Serializable[] resumeBinaryRequestPacket(String remoteLocation, long sizeAlreadyDownloaded, long crc) {
		return specificBinaryRequestPacket(remoteLocation, sizeAlreadyDownloaded, crc);
	}
	
	
	private Serializable[] specificBinaryRequestPacket(String remoteLocation, long sizeAlreadyDownloaded, long crc) {
		return new Serializable[] {lastReceivedPacketNr, RESEND_PACKET, sizeAlreadyDownloaded, crc, remoteLocation};
	}
}





/**
 * A message to be put into the message queue.
 * 
 * @author Tonio Gsell
 */
class Message {
	private long timestamp;
	private Serializable[] packet;
	
	Message() {	}
	
	Message(long t, Serializable[] pkt) {
		timestamp = t;
		packet = pkt.clone();
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	
	public Serializable[] getData() {
		return packet;
	}
}
