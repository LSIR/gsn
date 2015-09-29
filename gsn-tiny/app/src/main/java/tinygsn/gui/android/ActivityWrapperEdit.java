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
 * File: gsn-tiny/src/tinygsn/gui/android/ActivityVSConfig.java
 *
 * @author Do Ngoc Hoan
 */


package tinygsn.gui.android;


import tinygsn.beans.StaticData;
import tinygsn.model.wrappers.AbstractWrapper;
import tinygsn.storage.db.SqliteStorageManager;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import android.view.Menu;
import android.view.MenuItem;

public class ActivityWrapperEdit extends AbstractActivity {

	private EditText editText_dcDuration, editText_dcInterval;
	private TextView textView_title;
	private SqliteStorageManager storage = null;
	private String editingW = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sensors_edit);

		getActionBar().setDisplayHomeAsUpEnabled(true);

		textView_title = (TextView) findViewById(R.id.textView_title);

		editText_dcDuration = (EditText) findViewById(R.id.editText_dcduration);
		editText_dcInterval = (EditText) findViewById(R.id.editText_dcinterval);
		editText_dcInterval.setText("" + AbstractWrapper.DEFAULT_DUTY_CYCLE_INTERVAL);
		editText_dcDuration.setText("" + AbstractWrapper.DEFAULT_DUTY_CYCLE_DURATION);

		storage = new SqliteStorageManager();
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			editingW = extras.getString("name");
			int[] info = storage.getWrapperInfo(editingW);
			if (info != null) {
				editText_dcInterval.setText("" + info[0]);
				editText_dcDuration.setText("" + info[1]);
				String[] name = editingW.split("\\.");
				textView_title.setText("Editing:" + name[name.length - 1]);
			}
		} else {
			finish();
		}
	}

	public void saveVS() {
		try {
			int interval = Integer.parseInt(editText_dcInterval.getText().toString());
			int duration = Integer.parseInt(editText_dcDuration.getText().toString());
			storage.setWrapperInfo(editingW, interval, duration);
			StaticData.getWrapperByName(editingW).updateWrapperInfo();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add("Save")
//				.setIcon(R.drawable.ic_menu_save)
				.setShowAsAction(
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
				new AsyncTask<Activity, Void, Void>() {
					@Override
					protected Void doInBackground(Activity... params) {
						saveVS();
						return null;
					}

					@Override
					protected void onPostExecute(Void result) {
						finish();
					}
				}.execute(this);
				break;
		}

		return true;
	}
}
