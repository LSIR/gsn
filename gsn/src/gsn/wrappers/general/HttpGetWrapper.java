package gsn.wrappers.general;

import gsn.Main;
import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.wrappers.AbstractWrapper;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;

public class HttpGetWrapper extends AbstractWrapper {
   
	private long						DEFAULT_RATE			= 2000;
	private String						DEFAULT_FILE_EXTENSION	= ".jpg";

	private final transient Logger	logger				= Logger.getLogger( HttpGetWrapper.class );

	private URL						url;
	private long					rate;
	private String					file_extension;
	private File					directory			= null;
	private String					subdirectory		= null;
	private Integer					deviceId			= null;

	private static int				threadCounter		= 0;
	final static private SimpleDateFormat format		= new SimpleDateFormat("yyyy-MM-dd_HHmmss");
	private SimpleDateFormat folderdatetimefm			= new SimpleDateFormat("yyyy-MM-dd");

	private DataField []			outputStructure;


	/**
	* From XML file it needs the followings :
	* <ul>
	* <li>url</li> The full url for retriving the binary data.
	* <li>rate</li> The interval in msec for updating/asking for new information.
	* <li>username</li> The username to be used for http authentication (optional).
	* <li>password</li> The password to be used for http authentication (optional).
	* <li>subdirectory-name</li> The subdirectory name the data should be stored in filesystem (optional).
	* <li>folder-datetime-format</li>datetime format (see SimpleDateFormat) the data should be subdivided in (default=yyyy-MM-dd) (optional).
	* <li>device-id</li> The device id of the data producer (optional).
	* </ul>
	*/
	public boolean initialize (  ) {
		AddressBean addressBean =getActiveAddressBean( );
		String urlPath = addressBean.getPredicateValue( "url" );
		format.setTimeZone(Main.getContainerConfig().getTimeZone());

		final String username = getActiveAddressBean().getPredicateValue("username");
		final String password;
		if (username != null) {
			password = getActiveAddressBean().getPredicateValue("password");
			if (password != null)
				Authenticator.setDefault(new Authenticator() { protected PasswordAuthentication getPasswordAuthentication() { return new PasswordAuthentication(username, password.toCharArray()); } });
			else
				logger.warn("password is not set for username '" + username + "' -> trying to connect without authentication");
		}
		
		try {
			url = new URL(urlPath);
		} catch (MalformedURLException e) {
			logger.error("Loading the http wrapper failed : "+e.getMessage(),e);
			return false;
		}
		
		String id = getActiveAddressBean().getPredicateValue("device-id");
		if (id != null) {
			try {
				deviceId = Integer.parseInt(id);
			} catch (NumberFormatException e) {
				logger.error("device id >" + id + "< is not an integer");
				return false;
			}
			
			if (deviceId < 0 || deviceId > 65535) {
				logger.error("device id >" + id + "< has to be between 0 and 65535");
				return false;
			}
		}

		
		subdirectory = getActiveAddressBean().getPredicateValue("subdirectory-name");
		if (subdirectory != null) {
			String rootBinaryDir;
			
			try {
				rootBinaryDir = getActiveAddressBean().getVirtualSensorConfig().getStorage().getStorageDirectory();
			} catch (NullPointerException e){
				logger.error("if subdirectory-name is specified the storage-directory has to be specified as well");
				return false;
			}
			
			File f = new File(rootBinaryDir);
			if (!f.isDirectory()) {
				logger.error(rootBinaryDir + " is not a directory");
				return false;
			}
			
			if (!f.canWrite()) {
				logger.error(rootBinaryDir + " is not writable");
				return false;
			}
	
			if (logger.isDebugEnabled()) {
				logger.debug("binary root directory: " + rootBinaryDir);
			}

			if (deviceId != null)
				directory = new File(new File(new File(rootBinaryDir, getActiveAddressBean().getVirtualSensorName().split("_")[0].toLowerCase()), id), subdirectory);
			else
				directory = new File(new File(rootBinaryDir, getActiveAddressBean().getVirtualSensorName().split("_")[0].toLowerCase()), subdirectory);
			
			if (!directory.exists()) {
		    	if (!directory.mkdirs()) {
		    		logger.error("could not mkdir >" + directory + "<");
		    		return false;
				}
		    	else
		    		logger.info("created new storage directory >" + directory + "<");
			}
			
			if (deviceId == null)
				outputStructure = new DataField [] {
					new DataField("GENERATION_TIME", "BIGINT"),
					new DataField("SIZE", "INTEGER"),
					new DataField("RELATIVE_FILE", "VARCHAR(255)")};
			else
				outputStructure = new DataField [] {
					new DataField("DEVICE_ID", "INTEGER"),
					new DataField("GENERATION_TIME", "BIGINT"),
					new DataField("SIZE", "INTEGER"),
					new DataField("RELATIVE_FILE", "VARCHAR(255)")};
			
			logger.info("binaries will be stored in >" + directory + "<");
		}
		else {
			if (deviceId == null)
				outputStructure = new DataField [] {
					new DataField("GENERATION_TIME", "BIGINT"),
					new DataField("SIZE", "INTEGER"),
					new DataField("DATA", "binary:image/jpeg")};
			else
				outputStructure = new DataField [] {
					new DataField("DEVICE_ID", "INTEGER"),
					new DataField("GENERATION_TIME", "BIGINT"),
					new DataField("SIZE", "INTEGER"),
					new DataField("DATA", "binary:image/jpeg")};
			
			logger.info("binaries will be stored in database");
		}
		
		String inputRate = addressBean.getPredicateValue( "rate" );
		if ( inputRate == null || inputRate.trim( ).length( ) == 0 )
			rate = DEFAULT_RATE;
		else
			rate = Long.parseLong( inputRate );
		
		String fileExtension = addressBean.getPredicateValue( "file-extension" );
		if ( fileExtension == null || fileExtension.trim( ).length( ) == 0 )
			file_extension = DEFAULT_FILE_EXTENSION;
		else
			file_extension =  fileExtension;
		
		String format = addressBean.getPredicateValue("folder-datetime-format");
		if (format != null) {
			try {
				folderdatetimefm = new SimpleDateFormat(format);
			} catch (Exception e) {
				logger.error(e.getMessage() + ": using default format 'yyyy-MM-dd'");
			}
		}
		folderdatetimefm.setTimeZone(Main.getContainerConfig().getTimeZone());
 
		setName( "HttpReceiver-Thread" + ( ++threadCounter ) );
		if ( logger.isDebugEnabled( ) ) logger.debug( "AXISWirelessCameraWrapper is now running @" + rate + " Rate." );
		return true;
	}

	public void run ( ) {
		ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream(1024*20);
		byte[] buffer = new byte[16*1024];
		BufferedInputStream content;
		while ( isActive( ) ) {
			try {
				long timestamp = System.currentTimeMillis();
				HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
				httpURLConnection.connect();
				if ( httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_ACCEPTED ) continue;
				content = new BufferedInputStream(httpURLConnection.getInputStream(),4096);
				arrayOutputStream.reset();
				int readIndex = -1;
				while ( (readIndex= content.read(buffer))!=-1)
					arrayOutputStream.write(buffer, 0, readIndex);
				
				int size = arrayOutputStream.size();
				if (directory != null ) {
					String datedir = folderdatetimefm.format(new java.util.Date(timestamp));
				    File f = new File(directory+"/"+datedir);
				    if (!f.exists())
				    	f.mkdirs();
				    String name = datedir + "/" + format.format(new java.util.Date(timestamp))+file_extension;
				    
					arrayOutputStream.writeTo(new FileOutputStream (new File(directory, name)));
					if (deviceId == null)
						postStreamElement(new Serializable[]{timestamp, size, subdirectory+"/"+name});
					else
						postStreamElement(new Serializable[]{deviceId, timestamp, size, subdirectory+"/"+name});
				}
				else {
					if (deviceId == null)
						postStreamElement(new Serializable[]{timestamp, size, arrayOutputStream.toByteArray()});
					else
						postStreamElement(new Serializable[]{deviceId, timestamp, size, arrayOutputStream.toByteArray()});
				}
			} catch (IOException e) {
				logger.warn( e.getMessage( ) + " (host=" + url.getHost() + ")" );
			}
			try {
				Thread.sleep( rate );
			} catch ( InterruptedException e ) {
				logger.error( e.getMessage( ) , e );
			}
		}
	}
	
	public String getWrapperName() {
		return "Http Receiver";
	}

	public void dispose (  ) {
		threadCounter--;
	}

	public  DataField[] getOutputFormat ( ) {
		return outputStructure;
	}
}
