package gsn.wrappers.backlog.plugins;

import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.wrappers.BackLogWrapper;


/**
 * 
 * @author Tonio Gsell
 * 
 * TODO: remove CRC functionality after long time testing. It is not necessary over TCP.
 */
public class BigBinaryPlugin extends AbstractPlugin {

	private static final String PROPERTY_FILE = "property-file";
	private static final String STORAGE = "storage";
	private static final String STORAGE_DIRECTORY = "storage-directory";
	
	private static final String PROPERTY_REMOTE_FILE = "remote_file";
	private static final String PROPERTY_DOWNLOADED_SIZE = "downloaded_size";
	private static final String PROPERTY_FILE_TIMESTAMP = "timestamp";
	private static final String PROPERTY_FILE_SIZE = "file_size";
	protected static final String PROPERTY_CHUNK_NUMBER = "chunck_number";

	private static final byte INIT_PACKET = 0;
	private static final byte RESEND_PACKET = 1;
	private static final byte CHUNK_PACKET = 2;
	private static final byte CRC_PACKET = 3;

	private String localFileDir;

	protected final transient Logger logger = Logger.getLogger( BigBinaryPlugin.class );
	
	private DataField[] dataField = null;
	
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
	
	private CalculateChecksum calcChecksumThread;

	private boolean dispose = false;
	
	public boolean initialize ( BackLogWrapper backLogWrapper ) {
		super.initialize(backLogWrapper);
		calcChecksumThread = new CalculateChecksum(this);

		AddressBean addressBean = getActiveAddressBean();
		
		String storage;
		try {
			propertyFileName = addressBean.getPredicateValueWithException(PROPERTY_FILE);
			storage = addressBean.getPredicateValueWithException(STORAGE);
			localFileDir = addressBean.getPredicateValueWithException(STORAGE_DIRECTORY);
		} catch (Exception e) {
			logger.error(e.getMessage());
			return false;
		}
		
		logger.debug("property file name: " + propertyFileName);
		logger.debug("storage type: " + storage);
		logger.debug("local file directory: " + localFileDir);
		
		if (storage.equalsIgnoreCase("database")) {
			storeInDatabase = true;
			dataField = new DataField[] {new DataField("TIMESTAMP", "BIGINT"),
	  				   new DataField("REMOTEFILE", "VARCHAR(255)"),
	  				   new DataField("DATA", "binary")};
		}
		else if (storage.equalsIgnoreCase("filesystem")) {
			storeInDatabase = false;
			dataField = new DataField[] {new DataField("TIMESTAMP", "BIGINT"),
					   new DataField("REMOTEFILE", "VARCHAR(255)"),
					   new DataField("LOCALFILE", "VARCHAR(255)")};
		}
		else {
			logger.error("the 'storage' predicate in the virtual sensor's configuration file has to be 'database' or 'filesystem'");
			return false;
		}
			
		if(!localFileDir.endsWith("/"))
			localFileDir += "/";
		
		File f = new File(localFileDir);
		if (!f.isDirectory()) {
			logger.error(localFileDir + " is not a directory");
			return false;
		}
		
		return true;
	}


	@Override
	public String getPluginName() {
		return "BinaryPlugin";
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
		dispose = true;
		msgQueue.add(new Message());
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
    	
    				logger.debug("new incoming binary file:");
    				logger.debug("   remote binary name: " + remoteBinaryName);
    				logger.debug("   timestamp of the binary: " + binaryTimestamp);
    				logger.debug("   binary length: " + binaryLength);
    	
    			    File f = new File(remoteBinaryName);
    				localBinaryName = localFileDir + f.getName();
    	
    				filelen = 0;
    				
    				// delete the file if it already exists
    				f = new File(localBinaryName);
    			    if (f.exists())
    			    	f.delete();
    	
    			    // write the new file info to the property file
    				configFile.setProperty(PROPERTY_REMOTE_FILE, remoteBinaryName);
    				configFile.setProperty(PROPERTY_DOWNLOADED_SIZE, Long.toString(0));
    				configFile.setProperty(PROPERTY_FILE_TIMESTAMP, Long.toString(binaryTimestamp));
    				configFile.setProperty(PROPERTY_FILE_SIZE, Long.toString(binaryLength));
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
    					// write the actual file length and chunk number to the property file
    					// to be able to recover in case of a GSN failure
    					configFile.setProperty(PROPERTY_DOWNLOADED_SIZE, Long.toString(filelen));
    					configFile.setProperty(PROPERTY_CHUNK_NUMBER, Long.toString(chunknum));
    					configFile.store(new FileOutputStream(propertyFileName), null);
    				}
    				else {
    					// we should never reach this point...
    					logger.error("received chunk number (received nr=" + chunknum + "/last nr=" + lastChunkNumber + ") out of order -> drop this file (should never happen!)");
    					getNewFile();
    					continue;
    				}
    				
    				logger.debug("actual length of concatenated binary is " + filelen + " bytes");

    				lastChunkNumber = chunknum;
    			}
    			else if (type == CRC_PACKET) {
    				long crc = arr2uint(msg.getPacket(), 1);
    				
    				logger.debug("crc packet with crc32 >" + crc + "< received");
    				
    				// do we really have the whole file?
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
    							
    							Serializable[] data = {binaryTimestamp, remoteBinaryName, tmp};
    							if(!dataProcessed(System.currentTimeMillis(), data)) {
    								logger.warn("The binary data  (timestamp=" + binaryTimestamp + "/length=" + binaryLength + "/name=" + remoteBinaryName + ") could not be stored in the database.");
    							}
    							else
    								logger.debug("binary data (timestamp=" + binaryTimestamp + "/length=" + binaryLength + "/name=" + remoteBinaryName + ") successfully stored in database");
    							
    							file.delete();
    						}
    						else {
    							Serializable[] data = {binaryTimestamp, remoteBinaryName, localBinaryName};
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
    						logger.warn("crc does not match (received=" + crc + "/calculated=" + calculatedCRC.getValue() + ") -> request file retransmission");
    						calculatedCRC.reset();
    						getSpecificFile(remoteBinaryName, 0, 0);
    						continue;
    					}
    				}
    				else {
    					// we should never reach this point as well...
    					logger.error("binary length does not match (actual length=" + (new File(localBinaryName)).length() + "/should be=" + binaryLength + ") -> drop this file (should never happen!)");
    					getNewFile();
    					continue;
    				}
    			}
    		} catch (Exception e) {
    			// something is very wrong -> get the next file
    			logger.error(e.getMessage(), e);
    			getNewFile();
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
				getNewFile();
			}
			remoteBinaryName = configFile.getProperty(PROPERTY_REMOTE_FILE);
		    File f = new File(remoteBinaryName);
			localBinaryName = localFileDir + f.getName();
			downloadedSize = Long.valueOf(configFile.getProperty(PROPERTY_DOWNLOADED_SIZE)).longValue();
			binaryTimestamp = Long.valueOf(configFile.getProperty(PROPERTY_FILE_TIMESTAMP)).longValue();
			binaryLength = Long.valueOf(configFile.getProperty(PROPERTY_FILE_SIZE)).longValue();
			
			calcChecksumThread.newChecksum(localBinaryName);
		}
		else
			getNewFile();
	}


	@Override
	public void remoteConnLost() {
		logger.debug("Connection lost");
	}
	
	
	protected void getNewFile() {
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
	
	protected void getSpecificFile(String remoteLocation, long sizeAlreadyDownloaded, long chunkNr) throws Exception {
		// ask the deployment to resend the specified file from the specified position
		ByteArrayOutputStream baos = new ByteArrayOutputStream(remoteLocation.length() + 5);
		baos.write(RESEND_PACKET);
		baos.write(uint2arr(sizeAlreadyDownloaded));
		baos.write(remoteLocation.getBytes());
		byte [] pkt = baos.toByteArray();
		lastChunkNumber = chunkNr;
		
		while(!sendRemote(pkt)) {
			logger.info("could not send the message -> retry in 3 seconds");
			Thread.sleep(3000);
		}
	}
}



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
			
			// if the property file exists we have already downloaded a part of a file -> resume
			try {
				// calculate crc from already downloaded file
				parent.logger.debug("calculating cheksum for already downloaded part of file >" + parent.localBinaryName + "<");
		        try {
		            // Computer CRC32 checksum
		            cis = new CheckedInputStream(
		                    new FileInputStream(file), new CRC32());
		        } catch (FileNotFoundException e) {
		            System.err.println("File not found.");
		            System.exit(1);
		        }

		        byte[] buf = new byte[4096];
		        while(cis.read(buf) >= 0 && !this.dispose) {
		        	yield();
		        }
		        if (this.dispose)
		        	break;
		        
		        parent.calculatedCRC = (CRC32) cis.getChecksum();
				
		        parent.logger.debug("recalculated crc (" + parent.calculatedCRC.getValue() + ") from " + parent.localBinaryName);
				
				parent.getSpecificFile(parent.remoteBinaryName, parent.downloadedSize, Long.valueOf(parent.configFile.getProperty(BigBinaryPlugin.PROPERTY_CHUNK_NUMBER)).longValue());
			} catch (Exception e) {
				// no good... -> ask for a new file
				parent.logger.error(e.getMessage(), e);
				parent.getNewFile();
			}
		}
        
        parent.logger.debug("thread stopped");
	}
	
	
	public void newChecksum(String file) {
		fileQueue.add(file);
	}
	
	public void dispose() {
		this.dispose = true;
		fileQueue.add("");
	}
}
