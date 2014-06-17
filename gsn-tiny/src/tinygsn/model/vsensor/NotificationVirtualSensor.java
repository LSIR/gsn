package tinygsn.model.vsensor;

import java.util.Date;
import tinygsn.beans.StreamElement;
import tinygsn.gui.android.ActivityViewData;
import android.R;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class NotificationVirtualSensor extends AbstractVirtualSensor {
	public static String[] ACTIONS = { "Notification", "SMS", "Email" };
	public static String[] CONDITIONS = { "==", "is >=", "is <=", "is <", "is >",
			"changes", "frozen", "back after frozen" };

	private CharSequence notify_contentText = "Value changed";
	private int notify_id = 0;
	private String action;
	private long lastTimeHasData = 0;
	
	@Override
	public boolean initialize() {
		return true;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void dataAvailable(String inputStreamName, StreamElement streamElement) {
		String field = getVirtualSensorConfiguration().getNotify_field();
		String condition = getVirtualSensorConfiguration().getNotify_condition();
		Double value = getVirtualSensorConfiguration().getNotify_value();
		action = getVirtualSensorConfiguration().getNotify_action();
		String contact = getVirtualSensorConfiguration().getNotify_contact();

		notify_contentText = field + " " + condition + " " + value;
		Date time = new Date();
		
		if (condition.equals("==")) {
			if ((Double) streamElement.getData(field) == value) {
				takeAction();
			}
		}
		else if (condition.equals("is >=")) {
			if ((Double) streamElement.getData(field) >= value) {
				takeAction();
			}
		}
		else if (condition.equals("is <=")) {
			if ((Double) streamElement.getData(field) <= value) {
				takeAction();
			}
		}
		else if (condition.equals("is <")) {
			if ((Double) streamElement.getData(field) < value) {
				takeAction();
			}
		}
		else if (condition.equals("is >")) {
			if ((Double) streamElement.getData(field) > value) {
				takeAction();
			}
		}
		else if (condition.equals("changes")) {
			notify_contentText = field + " is changed";
			takeAction();
		}
		else if (condition.equals("frozen")) {
			
		}
		else if (condition.equals("back after frozen")) {
			
		}

		if (getVirtualSensorConfiguration().isSave_to_db()) {
			dataProduced(streamElement);
		}
	}

	private void takeAction() {
		if (action.equals("Notification")) {
			sendBasicNotification();
		}
		else if (action.equals("SMS")) {
			
		}
		else{
			
		}
	}

	public void sendBasicNotification() {
		Activity activity = getVirtualSensorConfiguration().getController()
				.getActivity();

		NotificationManager nm = (NotificationManager) activity
				.getSystemService(Context.NOTIFICATION_SERVICE);

		int icon = R.drawable.alert_dark_frame;
		CharSequence tickerText = "TinyGSN notification";
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);

		Context context = activity.getApplicationContext();
		CharSequence contentTitle = "TinyGSN notification";
		Intent notificationIntent = new Intent(activity, ActivityViewData.class);
		PendingIntent contentIntent = PendingIntent.getActivity(activity, 0,
				notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, notify_contentText,
				contentIntent);
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		
		nm.notify(notify_id++, notification);
	}

}