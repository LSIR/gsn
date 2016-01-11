/**
 * Global Sensor Networks (GSN) Source Code
 * Copyright (c) 2006-2015, Ecole Polytechnique Federale de Lausanne (EPFL)
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
 * File: gsn-tiny/src/tinygsn/gui/android/ActivityPublishData.java
 *
 * @author Do Ngoc Hoan and Schaer Marc
 */


package tinygsn.gui.android;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import tinygsn.beans.DeliveryRequest;
import tinygsn.controller.AndroidControllerPublish;
import tinygsn.model.publishers.AbstractDataPublisher;
import tinygsn.model.publishers.OnDemandDataPublisher;
import tinygsn.model.publishers.OpportunisticDataPublisher;
import tinygsn.model.publishers.PeriodicDataPublisher;
import tinygsn.storage.db.SqliteStorageManager;
import tinygsn.utils.ToastUtils;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class ActivityPublishData extends AbstractFragmentActivity {
	public static String[] STRATEGY = {"On demand", "Periodically", "Opportunistically"};
	static int TEXT_SIZE = 10;
	public static String DEFAULT_SERVER = "";

	private AndroidControllerPublish controller;
	private EditText serverEditText = null;
	private Button publishButton = null;
	private EditText keyEditText = null;
	private EditText fromdateEditText = null;
	private EditText fromtimeEditText = null;
	private EditText totimeEditText = null;
	private EditText todateEditText = null;
	private EditText iterationTime = null;
	private Spinner vsnameSpinner = null;
	private Spinner modeSpinner = null;
	private DeliveryRequest dr = null;
	private SqliteStorageManager storage;
	private AbstractDataPublisher dataPublisher;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_publish_data);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		serverEditText = (EditText) findViewById(R.id.editText_server);
		keyEditText = (EditText) findViewById(R.id.editText_key);
		fromdateEditText = (EditText) findViewById(R.id.fromdate);
		todateEditText = (EditText) findViewById(R.id.todate);
		totimeEditText = (EditText) findViewById(R.id.totime);
		fromtimeEditText = (EditText) findViewById(R.id.fromtime);
		vsnameSpinner = (Spinner) findViewById(R.id.spinner_vsName);
		modeSpinner = (Spinner) findViewById(R.id.spinner_mode);
		publishButton = (Button) findViewById(R.id.Button_publish);
		iterationTime = (EditText) findViewById(R.id.iterationTime);

		controller = new AndroidControllerPublish();
		storage = new SqliteStorageManager();

		Date date = new Date();
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, -15);

		String extra = getIntent().getStringExtra("tynigsn.beans.id");
		if (extra != null) {
			try {
				dr = storage.getPublishInfo(Integer.parseInt(extra));
				serverEditText.setText(dr.getUrl());
				keyEditText.setText(dr.getKey());
				cal.setTimeInMillis(dr.getLastTime());
				iterationTime.setText("" + dr.getIterationTime());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		totimeEditText.setText(new SimpleDateFormat("HH:mm", Locale.ENGLISH).format(date));
		todateEditText.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH).format(date));
		fromtimeEditText.setText(new SimpleDateFormat("HH:mm", Locale.ENGLISH).format(cal.getTime()));
		fromdateEditText.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH).format(cal.getTime()));
		renderVSList();
		renderModeList();
	}


	public void renderVSList() {

		new AsyncTask<AndroidControllerPublish, Void, ArrayList<String>>() {
			@Override
			protected ArrayList<String> doInBackground(AndroidControllerPublish... params) {
				return params[0].loadListVS();
			}

			@Override
			protected void onPostExecute(ArrayList<String> result) {
				ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(ActivityPublishData.this, R.layout.spinner_item, result);
				dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				vsnameSpinner.setAdapter(dataAdapter);
				if (dr != null) {
					int p = dataAdapter.getPosition(dr.getVsname());
					if (p != -1) {
						vsnameSpinner.setSelection(p);
					}
				}
			}
		}.execute(controller);
	}

	public void renderModeList() {

		modeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				if (pos != 0) {
					fromdateEditText.setVisibility(View.GONE);
					todateEditText.setVisibility(View.GONE);
					totimeEditText.setVisibility(View.GONE);
					fromtimeEditText.setVisibility(View.GONE);
					publishButton.setVisibility(View.GONE);
					findViewById(R.id.textViewFrom).setVisibility(View.GONE);
					findViewById(R.id.textViewTo).setVisibility(View.GONE);
					iterationTime.setVisibility(View.VISIBLE);
					findViewById(R.id.textIterationTime).setVisibility(View.VISIBLE);
				} else {
					fromdateEditText.setVisibility(View.VISIBLE);
					todateEditText.setVisibility(View.VISIBLE);
					totimeEditText.setVisibility(View.VISIBLE);
					fromtimeEditText.setVisibility(View.VISIBLE);
					publishButton.setVisibility(View.VISIBLE);
					findViewById(R.id.textViewFrom).setVisibility(View.VISIBLE);
					findViewById(R.id.textViewTo).setVisibility(View.VISIBLE);
					iterationTime.setVisibility(View.GONE);
					findViewById(R.id.textIterationTime).setVisibility(View.GONE);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		if (dr != null) {
			modeSpinner.setSelection(dr.getMode());
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add("Save").setShowAsAction(
			MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		return super.onCreateOptionsMenu(menu);
	}

	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
			case android.R.id.home:
				finish();
				break;
			case 0:
				int mode = modeSpinner.getSelectedItemPosition();
				if (iterationTime.getText().toString().equals("") || serverEditText.getText().toString().equals("") || vsnameSpinner.getSelectedItem().toString().equals("") ||
					keyEditText.getText().toString().equals("")) {
					ToastUtils.showToastInUiThread(this, "One of the field is empty. It should not !", Toast.LENGTH_SHORT);
				} else {
					createDataPublisher(mode).save(serverEditText.getText().toString(), vsnameSpinner.getSelectedItem().toString(),
						keyEditText.getText().toString(), mode, Long.valueOf(iterationTime.getText().toString()), dr);
					finish();
				}
				break;
		}
		return true;
	}

	/**
	 * This method is called directly from the button on the view when it is an "On Demand" publisher
	 *
	 * @param view
	 */
	public void publish(View view) {
		dataPublisher = createDataPublisher(0);
		dataPublisher.publish(dr);
	}

	private AbstractDataPublisher createDataPublisher(int mode) {
		if (mode == 0) {
			dataPublisher = new OnDemandDataPublisher(fromdateEditText.getText().toString(), fromtimeEditText.getText().toString(), todateEditText.getText().toString(), totimeEditText.getText().toString(), dr);
		} else if (mode == 1) {
			dataPublisher = new PeriodicDataPublisher(dr);
		} else {
			dataPublisher = new OpportunisticDataPublisher(dr);
		}
		return dataPublisher;
	}

	public static AbstractDataPublisher createPublicDataPublisher(int mode, DeliveryRequest dr) {
		if (mode == 0) {
			return new OnDemandDataPublisher(dr);
		} else if (mode == 1) {
			return new PeriodicDataPublisher(dr);
		} else {
			return new OpportunisticDataPublisher(dr);
		}
	}
}
