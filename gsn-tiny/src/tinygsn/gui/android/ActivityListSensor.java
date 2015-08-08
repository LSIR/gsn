/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2015, Ecole Polytechnique Federale de Lausanne (EPFL)
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
* File: gsn-tiny/src/tinygsn/gui/android/ActivityListSensor.java
*
* @author Do Ngoc Hoan, Julien Eberle
*/


package tinygsn.gui.android;

import java.util.ArrayList;

import tinygsn.controller.AndroidControllerWrapper;
import tinygsn.gui.android.utils.SensorListAdapter;
import tinygsn.gui.android.utils.SensorRow;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.actionbarsherlock.app.SherlockFragmentActivity;



public class ActivityListSensor extends SherlockFragmentActivity {


	private ListView listViewSensors, listViewScheduler;

	AndroidControllerWrapper controller = new AndroidControllerWrapper();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sensors_list);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		listViewSensors = (ListView) findViewById(R.id.wrapper_list);
		listViewScheduler = (ListView) findViewById(R.id.scheduler_list);
		initialize();
	}

	
	private void initialize(){
		new AsyncTask<AndroidControllerWrapper, Void, ArrayList<SensorRow>>(){
			@Override
			protected ArrayList<SensorRow> doInBackground(AndroidControllerWrapper... params) {
				return params[0].loadListWrapper();
			}
			@Override
			protected void onPostExecute(ArrayList<SensorRow> result) {
				SensorListAdapter dataListAdapter = new SensorListAdapter(ActivityListSensor.this, R.layout.sensor_row_item, result,controller);
				listViewSensors.setAdapter(dataListAdapter);
				dataListAdapter.notifyDataSetChanged();
			}
		}.execute(controller);
		new AsyncTask<AndroidControllerWrapper, Void, ArrayList<SensorRow>>(){
			@Override
			protected ArrayList<SensorRow> doInBackground(AndroidControllerWrapper... params) {
				return params[0].loadListScheduler();
			}
			@Override
			protected void onPostExecute(ArrayList<SensorRow> result) {
				SensorListAdapter dataListAdapter = new SensorListAdapter(ActivityListSensor.this, R.layout.sensor_row_item, result,controller);
				listViewScheduler.setAdapter(dataListAdapter);
				dataListAdapter.notifyDataSetChanged();
				disableManaged();
			}
		}.execute(controller);
		
	}
	
	public void disableManaged(){
		new AsyncTask<AndroidControllerWrapper, Void, ArrayList<String>>(){
			@Override
			protected ArrayList<String> doInBackground(AndroidControllerWrapper... params) {
				return params[0].loadListManaged();
			}
			@Override
			protected void onPostExecute(ArrayList<String> result) {
				for (int i = listViewSensors.getAdapter().getCount()-1;i>=0;i--){
					SensorRow row = (SensorRow)listViewSensors.getAdapter().getItem(i);
					if(result.contains(row.getName())){
						row.setManaged(true);
					}else{
						row.setManaged(false);
					}
				}
				for (int i = listViewScheduler.getAdapter().getCount()-1;i>=0;i--){
					SensorRow row = (SensorRow)listViewScheduler.getAdapter().getItem(i);
					if(result.contains(row.getName())){
						row.setManaged(true);
					}else{
						row.setManaged(false);
					}
				}
				((ArrayAdapter<SensorRow>)(listViewSensors.getAdapter())).notifyDataSetChanged();
				((ArrayAdapter<SensorRow>)(listViewScheduler.getAdapter())).notifyDataSetChanged();
			}
		}.execute(controller);
	}
	
	
	/*
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		final MenuItem add = menu.add("Add");
		add.setIcon(R.drawable.add).setShowAsAction(
				MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		add.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			public boolean onMenuItemClick(final MenuItem item) {
				item.setIcon(R.drawable.add2);
				handler.postDelayed(new Runnable() {
					public void run() {
						item.setIcon(R.drawable.add);
					}
				}, 10);

				startVSActivity();

				return false;
			}
		});

		final MenuItem deleteAll = menu.add("Delete all");
		deleteAll.setIcon(R.drawable.clear).setShowAsAction(
				MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		deleteAll.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			public boolean onMenuItemClick(MenuItem item) {
				controller.deleteAll();
				controller.loadListSubsData();
				Toast.makeText(context, "Delete all subscribed data!",
						Toast.LENGTH_SHORT).show();

				return false;
			}
		});

		return super.onCreateOptionsMenu(menu);
	}

	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
			controller.markDataUnreadToRead();
			finish();
			break;
		}
		return true;
	}
*/
}