package gsn.notifications;

import gsn.Main;
import gsn.storage.DataEnumerator;

/**
 * Dummy notification for Testing Purposes.
 */
public class DummyNotification extends NotificationRequest {

    public DummyNotification() {
        this.notificationCode = notificationCode;
    }

    public StringBuilder getQuery() {
        throw new UnsupportedOperationException("This method shouldn't be called by others.");
    }

    public boolean send(DataEnumerator data) {
        throw new UnsupportedOperationException("This method shouldn't be called by others.");
    }

    private int notificationCode = Main.tableNameGenerator();
    private CharSequence notificationCodeS = Main.tableNameGeneratorInString(notificationCode);

    public int getNotificationCode() {
        return notificationCode;
    }

    public CharSequence getNotificationCodeInString() {
        return notificationCodeS;
    }

    public String getOriginalQuery() {
        throw new UnsupportedOperationException("This method shouldn't be called by others.");
    }

}
