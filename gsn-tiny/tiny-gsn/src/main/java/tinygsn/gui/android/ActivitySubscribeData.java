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
import java.util.List;

import tinygsn.beans.Subscription;
import tinygsn.controller.AndroidControllerSubscribe;
import tinygsn.storage.db.SqliteStorageManager;
import tinygsn.utils.ToastUtils;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;



public class ActivitySubscribeData extends AbstractFragmentActivity {
	public static String[] STRATEGY = {"GSN API (Periodic Pull)", "GSN API (Opportunistic Pull)", "Google Cloud Messaging (Push)"};

	private AndroidControllerSubscribe controller;
	private EditText serverEditText = null;
	private EditText usernameEditText = null;
    private EditText passwordEditText = null;
	private Spinner vsnameSpinner = null;
	private Spinner modeSpinner = null;
	private Button connectButton = null;
	private Subscription su = null;
	private SqliteStorageManager storage;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_subscribe_data);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		serverEditText = (EditText) findViewById(R.id.editText_server_s);
        usernameEditText = (EditText) findViewById(R.id.editText_username_s);
        passwordEditText = (EditText) findViewById(R.id.editText_passwd_s);
		vsnameSpinner = (Spinner) findViewById(R.id.spinner_vsName_s);
		modeSpinner = (Spinner) findViewById(R.id.spinner_mode_s);
		connectButton = (Button) findViewById(R.id.button_connect);

		controller = new AndroidControllerSubscribe();
		storage = new SqliteStorageManager();

		String extra = getIntent().getStringExtra("tynigsn.beans.id");
		if (extra != null) {
			try {
				su = storage.getSubscribeInfo(Integer.parseInt(extra));
				serverEditText.setText(su.getUrl());
                usernameEditText.setText(su.getUsername());
                passwordEditText.setText(su.getPassword());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		connectButton.setOnClickListener(new View.OnClickListener() {
                                             @Override
                                             public void onClick(View view) {
                                                 renderVSList();
                                             }
                                         });
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
		List<String> list = new ArrayList<>();
		for (String s : STRATEGY) {
			list.add(s);
		}
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		modeSpinner.setAdapter(dataAdapter);
		if (su != null) {
			modeSpinner.setSelection(su.getMode());
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add("Save").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
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
        if (serverEditText.getText() == null || vsnameSpinner.getSelectedItem() == null || modeSpinner.getSelectedItem() == null) {
            ToastUtils.showToastInUiThread(this, "All arguments must be set", Toast.LENGTH_SHORT);
        } else {
            storage.setSubscribeInfo(id, serverEditText.getText().toString(), vsnameSpinner.getSelectedItem().toString(), modeSpinner.getSelectedItemPosition(), lastTime, 30000, active, usernameEditText.getText().toString(), passwordEditText.getText().toString());
        }
    }
}
