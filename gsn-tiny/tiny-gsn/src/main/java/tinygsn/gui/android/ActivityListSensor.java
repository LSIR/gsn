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
 * File: gsn-tiny/src/tinygsn/gui/android/ActivityListSensor.java
 *
 * @author Do Ngoc Hoan, Julien Eberle
 */


package tinygsn.gui.android;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

import tinygsn.controller.AndroidControllerWrapper;
import tinygsn.gui.android.utils.SensorListAdapter;
import tinygsn.gui.android.utils.SensorRow;


public class ActivityListSensor extends AbstractFragmentActivity {


	private ListView listViewSensors, listViewScheduler;

	AndroidControllerWrapper controller = new AndroidControllerWrapper();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sensors_list);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		listViewSensors = (ListView) findViewById(R.id.wrapper_list);
		listViewScheduler = (ListView) findViewById(R.id.scheduler_list);
		initialize();
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
			case android.R.id.home:
				finish();
				break;
		}
		return true;
	}


	public void disableManaged() {
		new AsyncTask<AndroidControllerWrapper, Void, ArrayList<String>>() {
			@Override
			protected ArrayList<String> doInBackground(AndroidControllerWrapper... params) {
				return params[0].loadListManaged();
			}

			@Override
			protected void onPostExecute(ArrayList<String> result) {
				for (int i = listViewSensors.getAdapter().getCount() - 1; i >= 0; i--) {
					SensorRow row = (SensorRow) listViewSensors.getAdapter().getItem(i);
					if (result.contains(row.getName())) {
						row.setManaged(true);
					} else {
						row.setManaged(false);
					}
				}
				for (int i = listViewScheduler.getAdapter().getCount() - 1; i >= 0; i--) {
					SensorRow row = (SensorRow) listViewScheduler.getAdapter().getItem(i);
					if (result.contains(row.getName())) {
						row.setManaged(true);
					} else {
						row.setManaged(false);
					}
				}
				((ArrayAdapter<SensorRow>) (listViewSensors.getAdapter())).notifyDataSetChanged();
				((ArrayAdapter<SensorRow>) (listViewScheduler.getAdapter())).notifyDataSetChanged();
			}
		}.execute(controller);
	}

	private void initialize() {
		new AsyncTask<AndroidControllerWrapper, Void, ArrayList<SensorRow>>() {
			@Override
			protected ArrayList<SensorRow> doInBackground(AndroidControllerWrapper... params) {
				return params[0].loadListWrapper();
			}

			@Override
			protected void onPostExecute(ArrayList<SensorRow> result) {
				SensorListAdapter dataListAdapter = new SensorListAdapter(ActivityListSensor.this, R.layout.sensor_row_item, result, controller);
				listViewSensors.setAdapter(dataListAdapter);
				dataListAdapter.notifyDataSetChanged();
			}
		}.execute(controller);
		new AsyncTask<AndroidControllerWrapper, Void, ArrayList<SensorRow>>() {
			@Override
			protected ArrayList<SensorRow> doInBackground(AndroidControllerWrapper... params) {
				return params[0].loadListScheduler();
			}

			@Override
			protected void onPostExecute(ArrayList<SensorRow> result) {
				SensorListAdapter dataListAdapter = new SensorListAdapter(ActivityListSensor.this, R.layout.sensor_row_item, result, controller);
				listViewScheduler.setAdapter(dataListAdapter);
				dataListAdapter.notifyDataSetChanged();
				disableManaged();
			}
		}.execute(controller);

	}
}