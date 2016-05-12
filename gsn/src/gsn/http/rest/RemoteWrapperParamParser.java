package gsn.http.rest;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import gsn.DataDistributer;
import gsn.Main;
import gsn.beans.AddressBean;
import gsn.beans.ContainerConfig;
import gsn.utils.Helpers;

import org.apache.log4j.Logger;
import org.joda.time.format.ISODateTimeFormat;

public class RemoteWrapperParamParser {

	private final transient Logger     logger                 = Logger.getLogger ( RemoteWrapperParamParser.class );

	private long startTime;
	private boolean isPushBased;
	private boolean continuous = false;
	private String query,deliveryContactPoint,remoteContactPoint;
	private String username,password;
    private boolean isSSLRequired;
    // The default timeout is set to 3 times the rate of the periodical Keep alive messages.
    // The timeout can be overriden in the virtual sensor description files.
    private int timeout =  3 * DataDistributer.getKeepAlivePeriod();

	private  final String CURRENT_TIME = ISODateTimeFormat.dateTime().print(System.currentTimeMillis());

	public RemoteWrapperParamParser(AddressBean addressBean, boolean isPushBased) {
		this.isPushBased = isPushBased;

		query = addressBean.getPredicateValueWithException("query" );
		
		logger.debug("Remote wrapper parameter [keep-alive: "+isPushBased+"], Query=> "+query);

		if (isPushBased ) 
			deliveryContactPoint = addressBean.getPredicateValueWithException(PushDelivery.LOCAL_CONTACT_POINT);

		username = addressBean.getPredicateValue( "username" );
		password = addressBean.getPredicateValue( "password" );

        timeout = addressBean.getPredicateValueAsInt("timeout", timeout);
        
        /**
		 * First looks for URL parameter, if it is there it will be used otherwise
		 * looks for host and port parameters.
		 */
		if ( (remoteContactPoint =addressBean.getPredicateValue ( "remote-contact-point" ))==null) {
			String host = addressBean.getPredicateValue ( "host" );
			if ( host == null || host.trim ( ).length ( ) == 0 ) 
				throw new RuntimeException( "The >host< parameter is missing from the RemoteWrapper wrapper." );
			int port = addressBean.getPredicateValueAsInt("port" ,ContainerConfig.DEFAULT_GSN_PORT);
			if ( port > 65000 || port <= 0 ) 
				throw new RuntimeException("Remote wrapper initialization failed, bad port number:"+port);

			remoteContactPoint ="http://" + host +":"+port+"/streaming/";
		}
		remoteContactPoint= remoteContactPoint.trim();
		if (!remoteContactPoint.trim().endsWith("/"))
			remoteContactPoint+="/";
        //
        isSSLRequired = remoteContactPoint.toLowerCase().startsWith("https");
        
		String str = addressBean.getPredicateValueWithDefault("start-time",CURRENT_TIME);
		
        String strStartTime = addressBean.getPredicateValue("start-time");
		if (strStartTime != null && strStartTime.equals("continue")) {
			Connection conn = null;
			ResultSet rs = null;
			try {
				conn = Main.getStorage(addressBean.getVirtualSensorConfig()).getConnection();

				// check if table already exists
				rs = conn.getMetaData().getTables(null, null, addressBean.getVirtualSensorName(), new String[] {"TABLE"});
				continuous = true;
				if (rs.next()) {
					StringBuilder query = new StringBuilder();
					query.append("select max(timed) from ").append(addressBean.getVirtualSensorName());
					Main.getStorage(addressBean.getVirtualSensorConfig()).close(rs);
					rs = Main.getStorage(addressBean.getVirtualSensorConfig()).executeQueryWithResultSet(query, conn);
					if (rs.next()) {
						startTime = rs.getLong(1);
					}
				}
				else
					logger.info("Table '" + addressBean.getVirtualSensorName() + "' doesn't exist => using all data from the remote database");
			} catch (SQLException e) {
				logger.error(e.getMessage(), e);
				throw new RuntimeException(e);
			} finally {
				Main.getStorage(addressBean.getVirtualSensorConfig()).close(rs);
				Main.getStorage(addressBean.getVirtualSensorConfig()).close(conn);
			}
		} else if (strStartTime != null && strStartTime.startsWith("-")) {
			try {
				startTime = System.currentTimeMillis() - Long.parseLong(strStartTime.substring(1));
			} catch (NumberFormatException e) {
				logger.error("Problem in parsing the start-time parameter, the provided value is: " + strStartTime);
				throw new RuntimeException(e);
			}
		} else {
			try {
				startTime = Helpers.convertTimeFromIsoToLong(str);
			} catch (Exception e) {
				logger.error("Failed to parse the start-time parameter of the remote wrapper, a sample time could be:"+(CURRENT_TIME));
				throw new RuntimeException(e);
			}
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

    public int getTimeout() {
        return timeout;
    }

    public boolean isSSLRequired() {
        return isSSLRequired;
    }

    public String getRemoteContactPointEncoded(long lastModifiedTime) {
		String toSend;
		try {
			if (continuous)
				toSend = getRemoteContactPoint()+URLEncoder.encode(query, "UTF-8")+"/"+URLEncoder.encode(getStartTimeInString(lastModifiedTime), "UTF-8")+"/c";
			else
				toSend = getRemoteContactPoint()+URLEncoder.encode(query, "UTF-8")+"/"+URLEncoder.encode(getStartTimeInString(lastModifiedTime), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		logger.debug ( new StringBuilder ("Wants to connect to ").append(getRemoteContactPoint()+query+"/"+getStartTimeInString(lastModifiedTime)).append( "==Encoded==> "+toSend));
		return toSend;
	}

}
