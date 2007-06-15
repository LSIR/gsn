/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 * Creation time : Dec 18, 2006@4:18:05 PM<br> *  
 */
package gsn.notifications;

import org.apache.log4j.Logger;
import gsn.Main;
import gsn.beans.StreamElement;
import gsn.storage.DataEnumerator;
import gsn.storage.StorageManager;
import gsn.wrappers.InVMPipeWrapper;


public class InGSNNotification extends NotificationRequest {

	private InVMPipeWrapper inVMPipeWrapper;
	
	private String remoteVSName ;
	
	private final transient Logger logger = Logger.getLogger(InGSNNotification.class);

	private StringBuilder query;

	public InGSNNotification(InVMPipeWrapper listener, String remoteVSName) {
		this.inVMPipeWrapper = listener;
		this.remoteVSName=remoteVSName;
		if (StorageManager.isHsql() || StorageManager.isMysqlDB()) {
			query = new StringBuilder("select * from ").append(remoteVSName).append(" order by timed desc limit 1 offset 0");
		}
		if (StorageManager.isSqlServer()) {
			query = new StringBuilder ("select top 1 * from ").append(remoteVSName);
		}
	}

	private int notificationCode = Main.tableNameGenerator();

	public int getNotificationCode() {
		return notificationCode;
	}

	/**
	 * Returning null means select * This is used for optimization, see
	 * ContainerImpl for more information.
	 */
	public StringBuilder getQuery() {
		return query;
	}

	public boolean send(DataEnumerator data) {
		  if (logger.isDebugEnabled())
				logger.debug("InVMPipe received the stream elements.");
		try {
			if (!inVMPipeWrapper.isActive())
				return false;
			while (data.hasMoreElements()) {
				StreamElement nextElement = data.nextElement();
				  if (logger.isDebugEnabled())
						logger.debug("InVMPipe submit a stream element.");
				inVMPipeWrapper.remoteDataReceived(nextElement);
			}
			return true;
		} catch (Exception e) {
			logger.warn("Notification Failed !, Error : " + e.getMessage(), e);
			return false;
		}
	}

		@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((remoteVSName == null) ? 0 : remoteVSName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final InGSNNotification other = (InGSNNotification) obj;
		if (remoteVSName == null) {
			if (other.remoteVSName != null)
				return false;
		} else if (!remoteVSName.equals(other.remoteVSName))
			return false;
		return true;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("InGSNNotification, Listener : ").append(inVMPipeWrapper)
				.append(" with Query ").append(query);
		return sb.toString();
	}
}