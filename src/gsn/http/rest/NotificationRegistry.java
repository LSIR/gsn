package gsn.http.rest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NotificationRegistry {
	
	private static NotificationRegistry singleton = new NotificationRegistry();
	
	private Map<Double, IPushWrapper> clients =Collections.synchronizedMap( new HashMap<Double, IPushWrapper>());
	
	public static NotificationRegistry getInstance() {
		return singleton;
	}
	
	public void addNotification(Double notificationId,IPushWrapper wrapper ) {
		clients.put(notificationId, wrapper);
	}
	
	public void removeNotification(Double notificationId) {
		clients.remove(notificationId);
	}
	
	public IPushWrapper getNotification(Double notificationId) {
		return clients.get(notificationId);
	}
	
	
}
