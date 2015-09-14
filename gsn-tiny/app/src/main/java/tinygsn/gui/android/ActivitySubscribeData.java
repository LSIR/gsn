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
 * File: gsn-tiny/src/tinygsn/gui/android/ActivityPublishData.java
 *
 * @author Do Ngoc Hoan
 */


package tinygsn.gui.android;

import java.util.ArrayList;

import tinygsn.beans.Subscription;
import tinygsn.controller.AndroidControllerSubscribe;
import tinygsn.storage.db.SqliteStorageManager;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import android.view.Menu;
import android.view.MenuItem;

public class ActivitySubscribeData extends FragmentActivity {
	public static String[] STRATEGY = {"Google Cloud Messaging (Push)", "GSN API (Pull)"};
	static int TEXT_SIZE = 10;
	public static String DEFAULT_SERVER = "";

	private AndroidControllerSubscribe controller;
	private EditText serverEditText = null;
	private Spinner vsnameSpinner = null;
	private Spinner modeSpinner = null;
	private Subscription su = null;
	private SqliteStorageManager storage;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_subscribe_data);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		serverEditText = (EditText) findViewById(R.id.editText_server_s);
		vsnameSpinner = (Spinner) findViewById(R.id.spinner_vsName_s);
		modeSpinner = (Spinner) findViewById(R.id.spinner_mode_s);

		controller = new AndroidControllerSubscribe();
		storage = new SqliteStorageManager();

		String extra = getIntent().getStringExtra("tynigsn.beans.id");
		if (extra != null) {
			try {
				su = storage.getSubscribeInfo(Integer.parseInt(extra));
				serverEditText.setText(su.getUrl());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		renderVSList();
		renderModeList();
	}


	public void renderVSList() {

		new AsyncTask<String, Void, ArrayList<String>>() {
			@Override
			protected ArrayList<String> doInBackground(String... params) {
				return controller.loadListVS(params[0]);
			}

			@Override
			protected void onPostExecute(ArrayList<String> result) {
				ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(ActivitySubscribeData.this, R.layout.spinner_item, result);
				dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				vsnameSpinner.setAdapter(dataAdapter);
				if (su != null) {
					int p = dataAdapter.getPosition(su.getVsname());
					if (p != -1) {
						vsnameSpinner.setSelection(p);
					}
				}
			}
		}.execute(serverEditText.getText().toString());
	}

	public void renderModeList() {
		if (su != null) {
			modeSpinner.setSelection(su.getMode());
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
				save();
				finish();
				break;
		}
		return true;
	}

	public void save() {
		int id = -1;
		long lastTime = 0;
		boolean active = false;
		if (su != null) {
			id = su.getId();
			lastTime = su.getLastTime();
			active = su.isActive();
		}
		storage.setSubscribeInfo(id, serverEditText.getText().toString(), vsnameSpinner.getSelectedItem().toString(), modeSpinner.getSelectedItemPosition(), lastTime, active);
	}

}
