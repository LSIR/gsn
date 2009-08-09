package gsn.http.rest;

import gsn.beans.ContainerConfig;
import gsn.beans.WrapperConfig;
import gsn.utils.Helpers;
import gsn2.conf.Parameters;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.log4j.Logger;
import org.joda.time.format.ISODateTimeFormat;

public class RemoteWrapperParamParser {

	private final transient Logger     logger                 = Logger.getLogger ( RemoteWrapperParamParser.class );

	private long startTime;
	private boolean isPushBased;
	private String query,deliveryContactPoint,remoteContactPoint;
	private String username,password;

	private  final String CURRENT_TIME = ISODateTimeFormat.dateTime().print(System.currentTimeMillis());

	public RemoteWrapperParamParser(WrapperConfig addressBean, boolean isPushBased) {

		Parameters params = addressBean.getParameters();
		query = params.getValueWithException("query" );
		
		logger.debug("Remote wrapper parameter [keep-alive: "+isPushBased+"], Query=> "+query);

		if (isPushBased ) 
			deliveryContactPoint = params.getValueWithException(PushDelivery.LOCAL_CONTACT_POINT);

		username = params.getValue( "username" );
		password = params.getValue( "password" );
		/**
		 * First looks for URL parameter, if it is there it will be used otherwise
		 * looks for host and port parameters.
		 */
		if ( (remoteContactPoint =params.getValue( "remote-contact-point" ))==null) {
			String host = params.getValue( "host" );
			if ( host == null || host.trim ( ).length ( ) == 0 ) 
				throw new RuntimeException( "The >host< parameter is missing from the RemoteWrapper wrapper." );
			int port = params.getValueAsInt("port" ,ContainerConfig.DEFAULT_GSN_PORT);
			if ( port > 65000 || port <= 0 ) 
				throw new RuntimeException("Remote wrapper initialization failed, bad port number:"+port);

			remoteContactPoint ="http://" + host +":"+port+"/streaming/";
		}
		remoteContactPoint= remoteContactPoint.trim();
		if (!remoteContactPoint.trim().endsWith("/"))
			remoteContactPoint+="/";

		try {
			startTime = Helpers.convertTimeFromIsoToLong(params.getValueWithDefault("start-time",CURRENT_TIME ));
		}catch (Exception e) {
			logger.error("Failed to parse the start-time parameter of the remote wrapper, a sample time could be:"+(CURRENT_TIME));
			throw new RuntimeException(e);
		}
	}

	public long getStartTime() {
		return startTime;
	}

	public String getStartTimeInString(long time) {
		return ISODateTimeFormat.dateTime().print(time);
	}

	public boolean isPushBased() {
		return isPushBased;
	}

	public String getQuery() {
		return query;
	}

	public String getLocalContactPoint() {
		return deliveryContactPoint;
	}

	public String getRemoteContactPoint() {
		return remoteContactPoint;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getRemoteContactPointEncoded(long lastModifiedTime) {
		String toSend;
		try {
			toSend = getRemoteContactPoint()+URLEncoder.encode(query, "UTF-8")+"/"+URLEncoder.encode(getStartTimeInString(lastModifiedTime), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		logger.debug ( new StringBuilder ("Wants to connect to ").append(getRemoteContactPoint()+query+"/"+getStartTimeInString(lastModifiedTime)).append( "==Encoded==> "+toSend));
		return toSend;
	}

}
