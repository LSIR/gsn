package gsn.notifications;

import gsn.beans.StreamElement;
import gsn.storage.DataEnumerator;
import gsn.storage.SQLUtils;
import gsn.storage.StorageManager;
import gsn.utils.CaseInsensitiveComparator;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

public class GSNNotification extends NotificationRequest {

	private final transient Logger logger = Logger.getLogger(GSNNotification.class);

	private int remotePort;

	private String remoteAddress;

	/**
	 * The <code>query</code> contains all the data in
	 * <code>queryWithoutDoubleQuots</code><br>
	 * except without using any double quotation.
	 */
	private StringBuilder query;
	private String originalQuery;

	private String prespectiveVirtualSensor;

	private transient PreparedStatement preparedStatement;

	private transient long latestVisitTime;

	private int notificationCode;

	private XmlRpcClient client = new XmlRpcClient();

	private XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();

	private String url;
    private boolean wrapperRequiresLocalAuthentication;
    private String wrapper_username;
    private String wrapper_password;

	public GSNNotification(String url, String virtualSensorName, String query, int notificationCode,
     boolean wrapperRequiresLocalAuthentication, String wrapper_username, String wrapper_password ) {

        logger.warn("Calling GSNNotification with parameters: url="+url
                  +" virtualSensorName= "+virtualSensorName
                  +" query="+query
                  +" notificationCode="+notificationCode
                  +" wrapper requires authentication"+ wrapperRequiresLocalAuthentication
                  +" wrapper's username"+ wrapper_username
                  +" wrapper's password"+ wrapper_password);

        this.originalQuery = query;
		this.url = url; //"http://" + remoteHost + ":" + remotePort + "/gsn-handler";
		this.prespectiveVirtualSensor = virtualSensorName;

        this.wrapperRequiresLocalAuthentication = wrapperRequiresLocalAuthentication;
        this.wrapper_username = wrapper_username;
        this.wrapper_password = wrapper_password;

		TreeMap<CharSequence,CharSequence> rewritingInfo = new TreeMap<CharSequence,CharSequence>(new CaseInsensitiveComparator());
		rewritingInfo.put("wrapper", virtualSensorName);
		if (StorageManager.isH2() || StorageManager.isMysqlDB()) {
			query += " order by timed desc limit 1 offset 0";
		}
		if (StorageManager.isSqlServer()) {
			query = "select top 1 * from "+virtualSensorName;
		}
		this.query = SQLUtils.newRewrite(query, rewritingInfo);
		this.notificationCode = notificationCode;
		try {
			config.setServerURL(new URL(url));

            // if authentication is needed on wrapper's side
            if (this.wrapperRequiresLocalAuthentication)  {
                config.setBasicUserName(this.wrapper_username);
                config.setBasicPassword(this.wrapper_password);
            }

            client.setConfig(config);
		} catch (MalformedURLException e1) {
			logger.warn("GSNNotification initialization failed! : "
					+ e1.getMessage(), e1);
		}
    }

	/**
	 * @return Returns the query.
	 */

	public StringBuilder getQuery() {
		return query;
	}

	/**
	 * @return Returns the address.
	 */
	public String getRemoteAddress() {
		return remoteAddress;
	}

	/**
	 * @return Returns the prespectiveVirtualSensor.
	 */
	public String getPrespectiveVirtualSensor() {
		return prespectiveVirtualSensor;
	}

	/**
	 * @return Returns the port.
	 */
	public int getRemotePort() {
		return remotePort;
	}

	/**
	 * @return Returns the notificationCode.
	 */
	public int getNotificationCode() {
		return notificationCode;
	}

	/**
	 * @return Returns the preparedStatement.
	 */
	public PreparedStatement getPreparedStatement() {
		return preparedStatement;
	}

	/**
	 * @param preparedStatement
	 *            The preparedStatement to set.
	 */
	public void setPreparedStatement(PreparedStatement preparedStatement) {
		this.preparedStatement = preparedStatement;
	}

	/**
	 * @return Returns the lastRespondTime.
	 */
	public long getLatestVisitTime() {
		return latestVisitTime;
	}

	/**
	 * @param lastRespondTime
	 *            The lastRespondTime to set.
	 */
	public void setLatestVisitTime(long lastRespondTime) {
		this.latestVisitTime = lastRespondTime;
	}

	public int hashCode() {
		final int PRIME = 31;
		int result = super.hashCode();
		result = PRIME * result + ((prespectiveVirtualSensor == null) ? 0 : prespectiveVirtualSensor.hashCode());
		result = PRIME * result + ((remoteAddress == null) ? 0 : remoteAddress.hashCode());
		result = PRIME * result + remotePort;
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		final GSNNotification other = (GSNNotification) obj;
		if (prespectiveVirtualSensor == null) {
			if (other.prespectiveVirtualSensor != null)
				return false;
		} else if (!prespectiveVirtualSensor.equals(other.prespectiveVirtualSensor))
			return false;
		if (remoteAddress == null) {
			if (other.remoteAddress != null)
				return false;
		} else if (!remoteAddress.equals(other.remoteAddress))
			return false;
		if (remotePort != other.remotePort)
			return false;
		return true;
	}

	public String toString() {
		StringBuffer result = new StringBuffer("GSN Notification Request : [ ");
		result.append("Address = ").append(remoteAddress).append(", ");
		result.append("Port = ").append(remotePort).append(", ");
		// result.append ( "InputVariableName = " ).append (
		// remoteInputVariableName ).append ( ", " ) ;
		result.append("Query = ").append(query).append(", ");
		result.append("PrespectiveVS = ").append(prespectiveVirtualSensor)
				.append(", ");
		result.append("NotificationCode = ").append(notificationCode);
		result.append("]");
		return result.toString();
	}

    int query_removal_counter=0;
	int query_removal_threshold=3;
	private boolean notifyPeerAboutData(StreamElement data) {
		boolean result = false;
		if (logger.isDebugEnabled())
			logger.debug(new StringBuilder().append(
					"Wants to send message to : ").append(remoteAddress)
					.toString());
		Object[] params = new Object[] { notificationCode,
				data.getFieldNames(), data.getDataInRPCFriendly(),
				Long.toString(data.getTimeStamp()) };
		try {
			result = (Boolean) client.execute("gsn.deliverData", params);
			if (result)
				query_removal_counter=0;
		} catch (XmlRpcException e) {
			if (logger.isInfoEnabled())
				logger.info("Couldn't notify the remote host : "
						+ config.getServerURL() + e.getMessage(), e);
			result=false;
		}		
		if (result == false) {
			if (++query_removal_counter>query_removal_threshold) {
				logger.warn("The remote is not interested anymore, the notification should be removed (try: "+query_removal_counter+" out of "+query_removal_threshold+")");
				result= false;
			}else {
				/**
				 * I return true even when the notification failed. Retuning true forces the
				 * system to keep the registeration. After three failed trials I'll return false.
				 */
				result=true; 
			}
		}
		return result;
	}

	public boolean send(DataEnumerator data) {
		StreamElement se;
		while (data.hasMoreElements()) {
			se = data.nextElement();
			boolean result = notifyPeerAboutData(se);
			if (logger.isDebugEnabled())
				logger.debug("GSN Notification wants to send data to the network.");
			if (result == false) {
				logger.warn(new StringBuilder().append("The result of delivering data was false, the remote client is not interested anymore, query dropped.").toString());
				return false;
			}
		}
		return true;
	}

  
  public String getOriginalQuery() {
    return originalQuery;
  }
}
