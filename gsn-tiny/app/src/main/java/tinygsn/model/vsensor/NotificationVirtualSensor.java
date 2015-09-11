/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
*
* This file is part of GSN.
*
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with GSN. If not, see <http://www.gnu.org/licenses/>.
*
* File: gsn-tiny/src/tinygsn/model/vsensor/NotificationVirtualSensor.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.model.vsensor;

import java.util.Date;

import tinygsn.beans.StaticData;
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
		
		NotificationManager nm = (NotificationManager) StaticData.globalContext.getSystemService(Context.NOTIFICATION_SERVICE);

		int icon = R.drawable.alert_dark_frame;
		CharSequence tickerText = "TinyGSN notification";
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);

		CharSequence contentTitle = "TinyGSN notification";
		Intent notificationIntent = new Intent(StaticData.globalContext, ActivityViewData.class);
		PendingIntent contentIntent = PendingIntent.getActivity(StaticData.globalContext, 0,
				notificationIntent, 0);

		notification.setLatestEventInfo(StaticData.globalContext, contentTitle, notify_contentText,
				contentIntent);
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		
		nm.notify(notify_id++, notification);
	}

}