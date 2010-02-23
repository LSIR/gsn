package gsn.wrappers.backlog.plugins;

import org.apache.log4j.Logger;
import org.h2.tools.Server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.storage.StorageManager;
import gsn.wrappers.BackLogWrapper;



/**
 * This plugin offers the functionality to download binaries from a deployment
 * in the size of up to 4GB. The binaries will be sent in chunks. Thus, no significant
 * interrupts of other plugin traffic is guaranteed. In case of a connection loss,
 * the download of the actual binary will be resumed as soon
 * as GSN reconnects to the deployment. The downloaded binaries
 * can be stored on disk or in the database. The first happens if 'storage=filesystem'
 * (defined in the virtual sensor's XML file) is set. The second, if
 * 'storage=database' is set (only use the second option if the binaries are not too big,
 * or install a lot of RAM).
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
 * can be specified in the virtual sensor's XML file using the 'folder-date-time-fm'
 * predicate. It defines the format (and with it the separation of the binaries based
 * on their modification time) of the folder naming. For formatting issues please
 * refer to {@link SimpleDateFormat}.
 * 
 * @author Tonio Gsell
 * <p>
 * TODO: remove CRC functionality after long time testing. It is not necessary over TCP.
 */
public class BigBinaryPlugin extends AbstractPlugin {
	private static final String STORAGE = "storage";
	private static final String STORAGE_DIRECTORY = "storage-directory";
	private static final String FOLDER_DATE_TIME_FM = "folder-date-time-fm";
	
	private static final String PROPERTY_REMOTE_BINARY = "remote_binary";
	private static final String PROPERTY_DOWNLOADED_SIZE = "downloaded_size";
	private static final String PROPERTY_BINARY_TIMESTAMP = "timestamp";
	private static final String PROPERTY_BINARY_SIZE = "file_size";
	protected static final String PROPERTY_CHUNK_NUMBER = "chunk_number";
	
	private static final String TEMP_BINARY_NAME = "binaryplugin_download.part";
	private static final String DEFAULT_FOLDER_DATE_TIME_FM = "yyyy-MM-dd";
	private static final String PROPERTY_FILE_NAME = ".gsnBinaryStat";

	private static final byte INIT_PACKET = 0;
	private static final byte RESEND_PACKET = 1;
	private static final byte CHUNK_PACKET = 2;
	private static final byte CRC_PACKET = 3;
	
	private SimpleDateFormat folderdatetimefm;

	private String localBinaryDir;

	protected final transient Logger logger = Logger.getLogger( BigBinaryPlugin.class );
	
	private DataField[] dataField = new DataField[] {new DataField("MODIFICATIONTIME", "BIGINT"),
			   							new DataField("RELATIVEFILE", "VARCHAR(255)"),
			   							new DataField("STORAGEDIRECTORY", "VARCHAR(255)"),
			   							new DataField("DATA", "binary")};
	
	private LinkedBlockingQueue<Message> msgQueue = new LinkedBlockingQueue<Message>();
	private String propertyFileName = null;
	private boolean storeInDatabase;
	protected Properties configFile = new Properties();
	private long binaryTimestamp = -1;
	private long binaryLength = -1;
	protected CRC32 calculatedCRC = new CRC32();
	protected String remoteBinaryName = null;
	protected String localBinaryName = null;
	protected long downloadedSize = -1;
	private long lastChunkNumber = -1;
	private static Set<String> deploymentList = new HashSet<String>();
    private static int threadCounter = 0;

	private Server web;
	
	private CalculateChecksum calcChecksumThread;

	private boolean dispose = false;
	
	
	public boolean initialize ( BackLogWrapper backLogWrapper ) {
		super.initialize(backLogWrapper);

		AddressBean addressBean = getActiveAddressBean();

		String deployment = addressBean.getVirtualSensorName().split("_")[0].toLowerCase();
		
		// check if this plugin has already be used for this deployment
		synchronized (deploymentList) {
			if (!deploymentList.add(deployment)) {
				logger.error("This plugin can only be used once per deployment!");
				return false;
			}
		}
		
		calcChecksumThread = new CalculateChecksum(this);

		try {
			if (logger.isDebugEnabled() && StorageManager.getDatabaseForConnection(StorageManager.getInstance().getConnection()) == StorageManager.DATABASE.H2) {
				try {
					String [] args = {"-webPort", "8082", "-webAllowOthers", "false"};
					web = Server.createWebServer(args);
					web.start();
				} catch (SQLException e) {
					logger.warn(e.getMessage(), e);
				}
			}
		} catch (SQLException e) {
			logger.warn(e.getMessage(), e);
		}
		
		String storage;
		try {
			storage = addressBean.getPredicateValueWithException(STORAGE);
			localBinaryDir = addressBean.getPredicateValueWithException(STORAGE_DIRECTORY);
		} catch (Exception e) {
			logger.error(e.getMessage());
			return false;
		}
		
		folderdatetimefm = new SimpleDateFormat(addressBean.getPredicateValueWithDefault(FOLDER_DATE_TIME_FM, DEFAULT_FOLDER_DATE_TIME_FM));
		
		if (storage.equalsIgnoreCase("database"))
			storeInDatabase = true;
		else if (storage.equalsIgnoreCase("filesystem"))
			storeInDatabase = false;
		else {
			logger.error("the 'storage' predicate in the virtual sensor's configuration file has to be 'database' or 'filesystem'");
			return false;
		}
			
		if(!localBinaryDir.endsWith("/"))
			localBinaryDir += "/";
		
		File f = new File(localBinaryDir);
		if (!f.isDirectory()) {
			logger.error(localBinaryDir + " is not a directory");
			return false;
		}
		
		if (!f.canWrite()) {
			logger.error(localBinaryDir + " is not writable");
			return false;
		}
		
		propertyFileName = localBinaryDir + PROPERTY_FILE_NAME;
		
		logger.debug("property file name: " + propertyFileName);
		logger.debug("storage type: " + storage);
		logger.debug("local binary directory: " + localBinaryDir);

        setName(getPluginName() + "-Thread" + (++threadCounter));
		
		return true;
	}


	@Override
	public String getPluginName() {
		return "BigBinaryPlugin";
	}
	

	@Override
	public byte getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.BIG_BINARY_MESSAGE_TYPE;
	}

	@Override
	public DataField[] getOutputFormat() {
		return dataField;
	}
	
	@Override
	public void dispose() {
		logger.debug("dispose thread");
		calcChecksumThread.dispose();
		try {
			calcChecksumThread.join();
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
		if (web != null) {
			web.shutdown();
			web = null;
		}
		dispose = true;
		msgQueue.add(new Message());
		
        threadCounter--;
	}
	
	@Override
	public void run() {
		calcChecksumThread.start();
    	while (!dispose) {
        	Message msg;
			try {
				msg = msgQueue.take();
			} catch (InterruptedException e) {
				logger.debug(e.getMessage());
				break;
			}
			if (dispose)
				break;
        	
    		long filelen = -1;
    		// get packet type
    		byte type = msg.getPacket()[0];
    		try {
    			if (type == INIT_PACKET) {
    				StringBuffer name = new StringBuffer();
    				
    				// get file info
    				binaryTimestamp = arr2long(msg.getPacket(), 1);
    				binaryLength = arr2uint(msg.getPacket(), 9);
    				for (int i = 13; i < msg.getPacket().length; i++) {
    					if (msg.getPacket()[i] == 0) break;
    					name.append((char) msg.getPacket()[i]);
    				}
    				remoteBinaryName = name.toString();
    	
    				logger.debug("new incoming binary:");
    				logger.debug("   remote binary name: " + remoteBinaryName);
    				logger.debug("   timestamp of the binary: " + binaryTimestamp);
    				logger.debug("   binary length: " + binaryLength);
    	
    			    File f = new File(remoteBinaryName);
    			    
    			    if (storeInDatabase) {
    			    	localBinaryName = localBinaryDir + TEMP_BINARY_NAME;
    			    }
    			    else {
    			    	String subpath = f.getParent();
    			    	if (subpath == null	)
    			    		subpath = "./";
    			    	logger.debug("subpath: " + subpath);
    					
    					if(!subpath.endsWith("/"))
    						subpath += "/";
    			    	
	    			    String datedir = localBinaryDir + subpath + folderdatetimefm.format(new java.util.Date(binaryTimestamp * 1000)) + "/";
	    			    String filename = f.getName();
	    			    f = new File(datedir);
	    			    if (!f.exists()) {
	    			    	if (!f.mkdirs()) {
	    			    		logger.error("could not mkdir >" + datedir + "<  -> drop remote binary " + remoteBinaryName);
	    			    		getNewBinary();
	    			    		continue;
	    			    	}
	    			    }
	    			    localBinaryName = datedir + filename;
    			    }
    	
    				filelen = 0;
    				
    				// delete the file if it already exists
    				f = new File(localBinaryName);
    			    if (f.exists()) {
    			    	logger.debug("overwrite already existing binary >" + localBinaryName + "<");
    			    	f.delete();
    			    }
    	
    			    // write the new binary info to the property file
    				configFile.setProperty(PROPERTY_REMOTE_BINARY, remoteBinaryName);
    				configFile.setProperty(PROPERTY_DOWNLOADED_SIZE, Long.toString(0));
    				configFile.setProperty(PROPERTY_BINARY_TIMESTAMP, Long.toString(binaryTimestamp));
    				configFile.setProperty(PROPERTY_BINARY_SIZE, Long.toString(binaryLength));
    				configFile.setProperty(PROPERTY_CHUNK_NUMBER, Long.toString(0));
    				
    				configFile.store(new FileOutputStream(propertyFileName), null);
    				
    				calculatedCRC.reset();
    				lastChunkNumber = 0;
    			}
    			else if (type == CHUNK_PACKET) {
    				// get number of this chunk
    				long chunknum = arr2uint(msg.getPacket(), 1);
    				logger.debug("Chunk for " + remoteBinaryName + " with number " + chunknum + " received");
    				
    				if (chunknum == lastChunkNumber) {
    					logger.info("chunk already received -> drop it");
    				}
    				else if (lastChunkNumber+1 == chunknum) {
    					// store the binary chunk to disk
    					File file = new File(localBinaryName);
    					FileOutputStream fos = new FileOutputStream(file, true);
    					byte [] chunk = java.util.Arrays.copyOfRange(msg.getPacket(), 5, msg.getPacket().length);
    					calculatedCRC.update(chunk);
    					logger.debug("updated crc: " + calculatedCRC.getValue());
    					fos.write(chunk);
    					fos.close();
    					filelen = file.length();
    					// write the actual binary length and chunk number to the property file
    					// to be able to recover in case of a GSN failure
    					configFile.setProperty(PROPERTY_DOWNLOADED_SIZE, Long.toString(filelen));
    					configFile.setProperty(PROPERTY_CHUNK_NUMBER, Long.toString(chunknum));
    					configFile.store(new FileOutputStream(propertyFileName), null);
    				}
    				else {
    					// we should never reach this point...
    					logger.error("received chunk number (received nr=" + chunknum + "/last nr=" + lastChunkNumber + ") out of order -> drop this binary (should never happen!)");
    					getNewBinary();
    					continue;
    				}
    				
    				logger.debug("actual length of concatenated binary is " + filelen + " bytes");

    				lastChunkNumber = chunknum;
    			}
    			else if (type == CRC_PACKET) {
    				long crc = arr2uint(msg.getPacket(), 1);
    				
    				logger.debug("crc packet with crc32 >" + crc + "< received");
    				
    				if (lastChunkNumber == -1) {
    					logger.debug("crc packet already received -> drop it");
    				}
    				else {
	    				// do we really have the whole binary?
	    				if ((new File(localBinaryName)).length() == binaryLength) {
	    					// check crc
	    					if (calculatedCRC.getValue() == crc) {
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
	    								logger.error(e.getMessage(), e);
	    							} catch (IOException e) {
	    								logger.error(e.getMessage(), e);
	    							}

	    							String relDir = remoteBinaryName;
	    							if (!relDir.startsWith("./"))
	    								relDir = "./" + relDir;
	    							Serializable[] data = {binaryTimestamp, relDir, null, tmp};
	    							if(!dataProcessed(System.currentTimeMillis(), data)) {
	    								logger.warn("The binary data  (timestamp=" + binaryTimestamp + "/length=" + binaryLength + "/name=" + remoteBinaryName + ") could not be stored in the database.");
	    							}
	    							else
	    								logger.debug("binary data (timestamp=" + binaryTimestamp + "/length=" + binaryLength + "/name=" + remoteBinaryName + ") successfully stored in database");
	    							
	    							file.delete();
	    						}
	    						else {
	    							String relLocalName = localBinaryName.replaceAll(localBinaryDir, "");
	    							if (!relLocalName.startsWith("./"))
	    								relLocalName = "./" + relLocalName;
	    							Serializable[] data = {binaryTimestamp, relLocalName, localBinaryDir, null};
	    							if(!dataProcessed(System.currentTimeMillis(), data)) {
	    								logger.warn("The binary data with >" + binaryTimestamp + "< could not be stored in the database.");
	    							}
	    							logger.debug("binary data (timestamp=" + binaryTimestamp + "/length=" + binaryLength + "/name=" + remoteBinaryName + ") successfully stored on disk");
	    						}
	    					
	    						File stat = new File(propertyFileName);
	    						stat.delete();
	    						
	    						localBinaryName = null;
	    						lastChunkNumber = -1;
	    					}
	    					else {
	    						logger.warn("crc does not match (received=" + crc + "/calculated=" + calculatedCRC.getValue() + ") -> request binary retransmission");
	    						calculatedCRC.reset();
	    						getSpecificBinary(remoteBinaryName, 0, 0);
	    						continue;
	    					}
	    				}
	    				else {
	    					// we should never reach this point as well...
	    					logger.error("binary length does not match (actual length=" + (new File(localBinaryName)).length() + "/should be=" + binaryLength + ") -> drop this binary (should never happen!)");
	    					getNewBinary();
	    					continue;
	    				}
	    			}
    			}
    		} catch (Exception e) {
    			// something is very wrong -> get the next binary
    			logger.error(e.getMessage(), e);
    			getNewBinary();
    			continue;
    		}
    		
    		ackMessage(msg.getTimestamp());
    	}
        
        logger.debug("thread stopped");
    }
	

	@Override
	public int packetReceived(long timestamp, byte[] packet) {
		try {
			msgQueue.add(new Message(timestamp, packet));
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return PACKET_SKIPPED;
		}
		return PACKET_PROCESSED;
	}


	@Override
	public void remoteConnEstablished() {
		logger.debug("Connection established");
		
		File sf = new File(propertyFileName);
		if (sf.exists()) {
			try {
				configFile.load(new FileInputStream(propertyFileName));
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				getNewBinary();
			}
			remoteBinaryName = configFile.getProperty(PROPERTY_REMOTE_BINARY);
			downloadedSize = Long.valueOf(configFile.getProperty(PROPERTY_DOWNLOADED_SIZE)).longValue();
			binaryTimestamp = Long.valueOf(configFile.getProperty(PROPERTY_BINARY_TIMESTAMP)).longValue();
			binaryLength = Long.valueOf(configFile.getProperty(PROPERTY_BINARY_SIZE)).longValue();

		    if (storeInDatabase) {
		    	localBinaryName = localBinaryDir + TEMP_BINARY_NAME;
		    }
		    else {
			    File f = new File(remoteBinaryName);
		    	String subpath = f.getParent();
		    	if (subpath == null	)
		    		subpath = "./";
		    	logger.debug("subpath: " + subpath);
				
				if(!subpath.endsWith("/"))
					subpath += "/";
		    	
			    String datedir = localBinaryDir + subpath + folderdatetimefm.format(new java.util.Date(binaryTimestamp * 1000)) + "/";
			    String filename = f.getName();
			    f = new File(datedir);
			    localBinaryName = datedir + filename;
		    }
		    
			calcChecksumThread.newChecksum(localBinaryName);
		}
		else
			getNewBinary();
	}


	@Override
	public void remoteConnLost() {
		logger.debug("Connection lost");
	}



	/**
	 * Get a new binary from the deployment.
	 */
	protected void getNewBinary() {
		byte [] pkt = new byte [1];
		pkt[0] = INIT_PACKET;
		
		while(!sendRemote(pkt)) {
			logger.info("could not send the message -> retry in 3 seconds");
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}



	/**
	 * Get a specific binary from the deployment (resume the download of a partly
	 * downloaded binary).
	 * 
	 * @param remoteLocation the relative location of the remote binary
	 * @param sizeAlreadyDownloaded the size of the binary which has already been downloaded
	 * @param chunkNr the number of the last chunk which has already been downloaded
	 * 
	 * @throws Exception if an I/O error occurs.
	 */
	protected void getSpecificBinary(String remoteLocation, long sizeAlreadyDownloaded, long chunkNr) throws Exception {
		// ask the deployment to resend the specified binary from the specified position
		ByteArrayOutputStream baos = new ByteArrayOutputStream(remoteLocation.length() + 5);
		baos.write(RESEND_PACKET);
		baos.write(uint2arr(sizeAlreadyDownloaded));
		baos.write(uint2arr(chunkNr));
		baos.write(remoteLocation.getBytes());
		byte [] pkt = baos.toByteArray();
		lastChunkNumber = chunkNr;
		
		while(!sendRemote(pkt)) {
			logger.info("could not send the message -> retry in 3 seconds");
			Thread.sleep(3000);
		}
	}
}





/**
 * A message to be put into the message queue.
 * 
 * @author Tonio Gsell
 */
class Message {
	protected long timestamp;
	protected byte[] packet;
	
	Message() {	}
	
	Message(long t, byte[] pkt) {
		timestamp = t;
		packet = pkt.clone();
	}
	
	public long getTimestamp() {
		return this.timestamp;
	}
	
	public byte[] getPacket() {
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
    private BigBinaryPlugin parent = null;
	private LinkedBlockingQueue<String> fileQueue = new LinkedBlockingQueue<String>();
	
	public CalculateChecksum(BigBinaryPlugin plug) {
		this.setName("CalculateChecksumThread");
		parent = plug;
	}
	
	public void run() {
		String file;
        
        parent.logger.debug("thread started");
		
		while (!this.dispose) {
			try {
				file = fileQueue.take();
			} catch (InterruptedException e) {
				parent.logger.debug(e.getMessage());
				break;
			}
			if (this.dispose)
				break;
			
			// if the property file exists we have already downloaded a part of a binary -> resume
			try {
				// calculate crc from already downloaded binary
				parent.logger.debug("calculating cheksum for already downloaded part of binary >" + parent.localBinaryName + "<");
		        try {
		            // Computer CRC32 checksum
		            cis = new CheckedInputStream(
		                    new FileInputStream(file), new CRC32());
		        } catch (FileNotFoundException e) {
		            parent.logger.error("binary >" + file + "< not found -> request a new binary from the deployment");
		            parent.getNewBinary();
		            continue;
		        }

		        byte[] buf = new byte[4096];
		        while(cis.read(buf) >= 0 && !this.dispose) {
		        	yield();
		        }
		        if (this.dispose)
		        	break;
		        
		        parent.calculatedCRC = (CRC32) cis.getChecksum();
				
		        parent.logger.debug("recalculated crc (" + parent.calculatedCRC.getValue() + ") from " + parent.localBinaryName);
				
				parent.getSpecificBinary(parent.remoteBinaryName, parent.downloadedSize, Long.valueOf(parent.configFile.getProperty(BigBinaryPlugin.PROPERTY_CHUNK_NUMBER)).longValue());
			} catch (Exception e) {
				// no good... -> ask for a new binary
				parent.logger.error(e.getMessage(), e);
				parent.getNewBinary();
			}
		}
        
        parent.logger.debug("thread stopped");
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
