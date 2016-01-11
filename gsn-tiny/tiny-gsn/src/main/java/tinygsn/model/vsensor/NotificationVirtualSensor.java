/**
 * Global Sensor Networks (GSN) Source Code
 * Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
 * <p/>
 * This file is part of GSN.
 * <p/>
 * GSN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * GSN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with GSN. If not, see <http://www.gnu.org/licenses/>.
 * <p/>
 * File: gsn-tiny/src/tinygsn/model/vsensor/NotificationVirtualSensor.java
 *
 * @author Do Ngoc Hoan
 */


package tinygsn.model.vsensor;

import android.R;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import org.epfl.locationprivacy.util.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.gui.android.ActivityViewData;
import tinygsn.model.utils.ParameterType;
import tinygsn.model.utils.Parameter;
import tinygsn.model.wrappers.AbstractWrapper;
import tinygsn.storage.db.SqliteStorageManager;

import static android.os.Debug.startMethodTracing;
import static android.os.Debug.stopMethodTracing;

public class NotificationVirtualSensor extends AbstractVirtualSensor {
	public static String[] ACTIONS = {"Notification", "SMS", "Email"};
	public static String[] CONDITIONS = {"==", "is >=", "is <=", "is <", "is >",
			                                    "changes", "frozen", "back after frozen"};

	private CharSequence notify_contentText = "Value changed";
	private int notify_id = 0;
	private String action;
	private long lastTimeHasData = 0;
	private Properties wrapperList;
	private SqliteStorageManager storage = null;
	private String LOGTAG = "NotificationVirtualSensor";


	@Override
	public boolean initialize() {
		wrapperList = AbstractWrapper.getWrapperList(StaticData.globalContext);
		storage = new SqliteStorageManager();
		return true;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void dataAvailable(String inputStreamName, StreamElement streamElement) {

		if ((boolean) Utils.getBuildConfigValue(StaticData.globalContext, "PERFORMANCE")) {
			startMethodTracing("Android/data/tinygsn.gui.android/" + LOGTAG + "_" + inputStreamName + "_" + System.currentTimeMillis());
		}
		log("dataAvailable_" + LOGTAG + "_" + inputStreamName, "===========================================");
		log("dataAvailable_" + LOGTAG + "_" + inputStreamName, "Starting to process data in dataAvailable");
		long startLogTime = System.currentTimeMillis();

		streamElement = super.anonymizeData(inputStreamName, streamElement);
		String prefix = "vsensor:" + getVirtualSensorConfiguration().getName();
		HashMap settings = storage.getSetting(prefix);
		String field = (String) settings.get(prefix + ":" + "field");
		String condition = (String) settings.get(prefix + ":" + "condition");
		Double value = Double.parseDouble((String) settings.get(prefix + ":" + "value"));
		action = (String) settings.get(prefix + ":" + "action");
		String contact = (String) settings.get(prefix + ":" + "Contact");
		boolean saveToDB;
		if (((String) settings.get(prefix + ":" + "Save to Database")).equals("true")) {
			saveToDB = true;
		} else {
			saveToDB = false;
		}

		notify_contentText = field + " " + condition + " " + value;
		Date time = new Date();

		if (condition.equals("==")) {
			if ((Double) streamElement.getData(field) == value) {
				takeAction(field + " " + condition + " " + value + " (" + (Double) streamElement.getData(field) + ")");
			}
		} else if (condition.equals("is >=")) {
			if ((Double) streamElement.getData(field) >= value) {
				takeAction(field + " " + condition + " " + value + " (" + (Double) streamElement.getData(field) + ")");
			}
		} else if (condition.equals("is <=")) {
			if ((Double) streamElement.getData(field) <= value) {
				takeAction(field + " " + condition + " " + value + " (" + (Double) streamElement.getData(field) + ")");
			}
		} else if (condition.equals("is <")) {
			if ((Double) streamElement.getData(field) < value) {
				takeAction(field + " " + condition + " " + value + " (" + (Double) streamElement.getData(field) + ")");
			}
		} else if (condition.equals("is >")) {
			if ((Double) streamElement.getData(field) > value) {
				takeAction(field + " " + condition + " " + value + " (" + (Double) streamElement.getData(field) + ")");
			}
		} else if (condition.equals("changes")) {
			notify_contentText = field + " is changed";
			takeAction(field + " " + condition + " " + value + " (" + (Double) streamElement.getData(field) + ")");
		} else if (condition.equals("frozen")) {
			//TODO: to do
		} else if (condition.equals("back after frozen")) {
			//TODO: to do
		}

		long endLogTime = System.currentTimeMillis();
		log("dataAvailable_" + LOGTAG + "_" + inputStreamName, "Total Time to process data in dataAvailable() (without dataProduced()) : " + (endLogTime - startLogTime) + " ms.");

		if (saveToDB) {
			dataProduced(streamElement);
		}

		if ((boolean) Utils.getBuildConfigValue(StaticData.globalContext, "PERFORMANCE") || (boolean) Utils.getBuildConfigValue(StaticData.globalContext, "GPSPERFORMANCE")) {
			stopMethodTracing();
		}
	}

	private void takeAction(String notif) {
		if (action.equals("Notification")) {
			sendBasicNotification(notif);
		} else if (action.equals("SMS")) {
			//TODO: to do
		} else {
			//TODO: to do
		}
	}

	public void sendBasicNotification(String notif) {

		NotificationManager nm = (NotificationManager) StaticData.globalContext.getSystemService(Context.NOTIFICATION_SERVICE);
		int icon = R.drawable.alert_dark_frame;
		CharSequence contentTitle = "TinyGSN notification";
		CharSequence tickerText = notif;
		long when = System.currentTimeMillis();

		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(StaticData.globalContext)
						.setSmallIcon(icon)
						.setContentTitle(contentTitle)
						.setContentText(tickerText);

		Intent notificationIntent = new Intent(StaticData.globalContext, ActivityViewData.class);

		TaskStackBuilder stackBuilder = TaskStackBuilder.create(StaticData.globalContext);
		stackBuilder.addParentStack(ActivityViewData.class);
		stackBuilder.addNextIntent(notificationIntent);

		PendingIntent contentIntent = PendingIntent.getActivity(StaticData.globalContext, 0,
				                                                       notificationIntent, 0);
		mBuilder.setContentIntent(contentIntent);
		mBuilder.setAutoCancel(true);
		nm.notify(notify_id++, mBuilder.build());
	}


	@Override
	public ArrayList<Parameter> getParameters() {
		ArrayList<String> condition = new ArrayList<>();
		String[] conditions = new String[]{"==", "is >=", "is <=", "is <", "is >", "changes", "frozen", "back after frozen"};
		for (int i = 0; i < conditions.length; i++) {
			condition.add(conditions[i]);
		}

		ArrayList<String> action = new ArrayList<>();
		String[] actions = new String[]{"Notification", "SMS", "Email"};
		for (int i = 0; i < actions.length; i++) {
			action.add(actions[i]);
		}
		ArrayList<Parameter> list = new ArrayList<>();
		list.add(new Parameter("condition", condition, ParameterType.SPINNER));
		list.add(new Parameter("value", "10", ParameterType.EDITBOX_NUMBER));
		list.add(new Parameter("action", action, ParameterType.SPINNER));
		list.add(new Parameter("Contact", "+41798765432", ParameterType.EDITBOX_PHONE));
		list.add(new Parameter("Save to Database", ParameterType.CHECKBOX));

		return list;
	}

	@Override
	public ArrayList<Parameter> getParameters(ArrayList<String> params) {
		ArrayList<Parameter> list = super.getParameters(params);
		Set<String> set = new HashSet();
		for (String field : params) {
			set.add(field);
		}
		ArrayList<String> paramsWithoutDuplicates = new ArrayList<>();
		for (String field : set) {
			paramsWithoutDuplicates.add(field);
		}
		list.add(1, new Parameter("field", paramsWithoutDuplicates, ParameterType.SPINNER));

		return list;
	}
}