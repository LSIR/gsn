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
import java.util.List;
import java.util.Properties;

import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.gui.android.ActivityViewData;
import tinygsn.model.vsensor.utils.ParameterType;
import tinygsn.model.vsensor.utils.VSParameter;
import tinygsn.model.wrappers.AbstractWrapper;

public class NotificationVirtualSensor extends AbstractVirtualSensor {
	public static String[] ACTIONS = {"Notification", "SMS", "Email"};
	public static String[] CONDITIONS = {"==", "is >=", "is <=", "is <", "is >",
			                                    "changes", "frozen", "back after frozen"};

	private CharSequence notify_contentText = "Value changed";
	private int notify_id = 0;
	private String action;
	private long lastTimeHasData = 0;
	private Properties wrapperList;

	@Override
	public boolean initialize() {
		wrapperList = AbstractWrapper.getWrapperList(StaticData.globalContext);
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

		if (getVirtualSensorConfiguration().isSave_to_db()) {
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
	public VSParameter[] getParameters() {
		ArrayList<String> field = new ArrayList();
		String[] fields = new String[]{"temp", "value"};
		for (int i = 0; i < fields.length; i++) {
			field.add(fields[i]);
		}
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

		return new VSParameter[]{
				                        //TODO : find a way to have right fields.
				                        new VSParameter("field", field, ParameterType.SPINNER),
				                        new VSParameter("condition",
						                                       condition,
						                                       ParameterType.SPINNER),
				                        new VSParameter("value", "10", ParameterType.EDITBOX),
				                        new VSParameter("action",
						                                       action,
						                                       ParameterType.SPINNER),
				                        new VSParameter("Contact", "+41798765432", ParameterType.EDITBOX),
				                        new VSParameter("Save to Database", ParameterType.CHECKBOX)
		};
	}

	/***********************************************************************************************
	//FIXME : KEPT only for the moment, but will be removed
	public void getRowParameters(TableLayout table_notify_config, Context context) {
		int TEXT_SIZE = 10;
		Spinner condition, field, action;
		EditText editText_value, editText_contact;
		CheckBox saveToDB;
		final Context ctx = context;

		initialize();

		table_notify_config.removeAllViews();

		// Row Field
		TableRow row = new TableRow(context);

		TextView txt = new TextView(context);
		txt.setText("Field");
		txt.setTextColor(Color.parseColor("#000000"));
		row.addView(txt);

		field = new Spinner(context);
		List<String> list = new ArrayList<String>();
		// FIXME : have every wrapper and not only gps
		String wrapperName = wrapperList.getProperty("android.hardware.location.gps");

		try {
			AbstractWrapper w = (AbstractWrapper) StaticData.getWrapperByName(wrapperName);
			for (String s : w.getFieldList()) {
				list.add(s);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(context,
				                                                           tinygsn.gui.android.R.layout.spinner_item, list);
		dataAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		field.setAdapter(dataAdapter);

		row.addView(field);
		table_notify_config.addView(row);

		// Row condition
		row = new TableRow(context);

		txt = new TextView(context);
		txt.setText("Sampling Rate");
		txt.setText("Condition    ");
		txt.setTextColor(Color.parseColor("#000000"));
		row.addView(txt);

		condition = new Spinner(context);
		List<String> list_condition = new ArrayList<String>();

		for (String s : NotificationVirtualSensor.CONDITIONS) {
			list_condition.add(s);
		}
		ArrayAdapter<String> dataAdapter_condition = new ArrayAdapter<String>(context,
				                                                                     tinygsn.gui.android.R.layout.spinner_item, list_condition);
		dataAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		condition.setAdapter(dataAdapter_condition);
		row.addView(condition);

		table_notify_config.addView(row);

		// Row value
		row = new TableRow(context);

		txt = new TextView(context);
		txt.setText("Value");
		txt.setTextColor(Color.parseColor("#000000"));
		row.addView(txt);

		editText_value = new EditText(context);
		editText_value.setText("10");
		editText_value.setTextSize(TEXT_SIZE + 5);
		editText_value.setInputType(InputType.TYPE_CLASS_NUMBER);
		// editText_value.requestFocus();
		editText_value.setTextColor(Color.parseColor("#000000"));
		row.addView(editText_value);

		editText_value.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
			                              int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				try {
					// numLatest =
					// Integer.parseInt(editText_numLatest.getText().toString());
					// loadLatestData();
				} catch (NumberFormatException e) {
					AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
							                                                                ctx);
					alertDialogBuilder.setTitle("Please input a number!");
				}
			}
		});

		table_notify_config.addView(row);

		// Row action
		row = new TableRow(context);

		txt = new TextView(context);
		txt.setText("Action");
		txt.setTextColor(Color.parseColor("#000000"));
		row.addView(txt);

		action = new Spinner(context);
		List<String> list_action = new ArrayList<String>();

		for (String s : NotificationVirtualSensor.ACTIONS) {
			list_action.add(s);
		}
		ArrayAdapter<String> dataAdapter_action = new ArrayAdapter<String>(context,
				                                                                  tinygsn.gui.android.R.layout.spinner_item, list_action);
		dataAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		action.setAdapter(dataAdapter_action);
		row.addView(action);

		table_notify_config.addView(row);

		// Row contact
		row = new TableRow(context);

		txt = new TextView(context);
		txt.setText("Contact");
		txt.setTextColor(Color.parseColor("#000000"));
		row.addView(txt);

		editText_contact = new EditText(context);
		editText_contact.setText("+41798765432");
		editText_contact.setTextSize(TEXT_SIZE + 5);
		// editText_contact.setInputType(InputType.TYPE_CLASS_NUMBER);
		// editText_contact.requestFocus();
		editText_contact.setTextColor(Color.parseColor("#000000"));
		row.addView(editText_contact);

		editText_contact.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
			                              int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				try {
					// numLatest =
					// Integer.parseInt(editText_numLatest.getText().toString());
					// loadLatestData();
				} catch (NumberFormatException e) {
					AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
							                                                                ctx);
					alertDialogBuilder.setTitle("Please input a phone number!");
				}
			}
		});

		table_notify_config.addView(row);

		// Row Save to DB
		row = new TableRow(context);

		txt = new TextView(context);
		txt.setText("Save to Database?");
		txt.setTextColor(Color.parseColor("#000000"));
		row.addView(txt);

		saveToDB = new CheckBox(context);
		saveToDB.setTextColor(Color.parseColor("#000000"));
		row.addView(saveToDB);


		table_notify_config.addView(row);
		// TableRow.LayoutParams params = new TableRow.LayoutParams();
		// params.span = 2;
	}
	 **********************************************************************************************/
}