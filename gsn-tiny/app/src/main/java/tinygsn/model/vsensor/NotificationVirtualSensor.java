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
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.beans.VSensorConfig;
import tinygsn.gui.android.ActivityViewData;
import tinygsn.model.vsensor.utils.ParameterType;
import tinygsn.model.vsensor.utils.VSParameter;
import tinygsn.model.wrappers.AbstractWrapper;
import tinygsn.storage.db.SqliteStorageManager;

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
		String prefix = "vsensor:" + getVirtualSensorConfiguration().getName();
		HashMap settings = storage.getSetting(prefix);
		String field = (String) settings.get(prefix + ":" + "field");
		String condition = (String) settings.get(prefix + ":" +  "condition");
		Double value = Double.parseDouble((String) settings.get(prefix + ":" +  "value"));
		action = (String) settings.get(prefix + ":" +  "action");
		String contact = (String) settings.get(prefix + ":" +  "Contact");
		boolean saveToDB;
		if (((String) settings.get(prefix + ":" +  "Save to Database")).equals("true")) {
			saveToDB = true;
		} else {
			saveToDB = false;
		}

		notify_contentText = field + " " + condition + " " + value;
		Date time = new Date();

		if (condition.equals("==")) {
			if ((Double) streamElement.getData(field) == value) {
				takeAction();
			}
		} else if (condition.equals("is >=")) {
			if ((Double) streamElement.getData(field) >= value) {
				takeAction();
			}
		} else if (condition.equals("is <=")) {
			if ((Double) streamElement.getData(field) <= value) {
				takeAction();
			}
		} else if (condition.equals("is <")) {
			if ((Double) streamElement.getData(field) < value) {
				takeAction();
			}
		} else if (condition.equals("is >")) {
			if ((Double) streamElement.getData(field) > value) {
				takeAction();
			}
		} else if (condition.equals("changes")) {
			notify_contentText = field + " is changed";
			takeAction();
		} else if (condition.equals("frozen")) {
			//TODO: to do
		} else if (condition.equals("back after frozen")) {
			//TODO: to do
		}

		if (saveToDB) {
			dataProduced(streamElement);
		}
	}

	private void takeAction() {
		if (action.equals("Notification")) {
			sendBasicNotification();
		} else if (action.equals("SMS")) {
			//TODO: to do
		} else {
			//TODO: to do
		}
	}

	public void sendBasicNotification() {

		NotificationManager nm = (NotificationManager) StaticData.globalContext.getSystemService(Context.NOTIFICATION_SERVICE);

		int icon = R.drawable.alert_dark_frame;
		CharSequence tickerText = "TinyGSN notification";
		long when = System.currentTimeMillis();

		//FIXME : deprecated
		Notification notification = new Notification(icon, tickerText, when);

		CharSequence contentTitle = "TinyGSN notification";
		Intent notificationIntent = new Intent(StaticData.globalContext, ActivityViewData.class);
		PendingIntent contentIntent = PendingIntent.getActivity(StaticData.globalContext, 0,
				                                                       notificationIntent, 0);
		//FIXME : deprecated
		notification.setLatestEventInfo(StaticData.globalContext, contentTitle, notify_contentText,
				                               contentIntent);
		notification.flags = Notification.FLAG_AUTO_CANCEL;

		nm.notify(notify_id++, notification);
	}


	@Override
	public ArrayList<VSParameter> getParameters() {
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
		ArrayList<VSParameter> list = new ArrayList<>();
		list.add(new VSParameter("condition", condition, ParameterType.SPINNER));
		list.add(new VSParameter("value", "10", ParameterType.EDITBOX_NUMBER));
		list.add(new VSParameter("action", action, ParameterType.SPINNER));
		list.add(new VSParameter("Contact", "+41798765432", ParameterType.EDITBOX_PHONE));
		list.add(new VSParameter("Save to Database", ParameterType.CHECKBOX));

		return list;
	}

	@Override
	public ArrayList<VSParameter> getParameters(ArrayList<String> params) {
		ArrayList<VSParameter> list = getParameters();
		Set<String> set = new HashSet();
		for (String field : params) {
			set.add(field);
		}
		ArrayList<String> paramsWithoutDuplicates = new ArrayList<>();
		for (String field : set) {
			paramsWithoutDuplicates.add(field);
		}
		list.add(0, new VSParameter("field", paramsWithoutDuplicates, ParameterType.SPINNER));

		return list;
	}
}