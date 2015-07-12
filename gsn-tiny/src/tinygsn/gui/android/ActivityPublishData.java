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
* File: gsn-tiny/src/tinygsn/gui/android/ActivityPublishData.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.gui.android;

import gsn.http.rest.PushDelivery;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import tinygsn.beans.StreamElement;
import tinygsn.controller.AndroidControllerPublishData;
import tinygsn.model.vsensor.AbstractVirtualSensor;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class ActivityPublishData extends SherlockFragmentActivity {
	//public static String[] STRATEGY = { "On demand", "Periodically (time)", "Periodically (values)" };
	static int TEXT_SIZE = 10;
	public static String DEFAULT_SERVER = "";
	public static Hashtable<String,Double> SENSORS_IDS = new Hashtable<String, Double>();

	private Context context = null;
	private AndroidControllerPublishData controller;
	//private Spinner spinner_vsName;
	//private Spinner spinner_strategy;
	private EditText serverEditText = null;
    private EditText fromdateEditText = null;
    private EditText fromtimeEditText = null;
    private EditText totimeEditText = null;
    private EditText todateEditText = null;
    private Handler handlerVS;
	private ArrayList<AbstractVirtualSensor> vsList = new ArrayList<AbstractVirtualSensor>();


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		SENSORS_IDS.put("accelometer", 100001.0);
		SENSORS_IDS.put("gps", 100002.0);
		SENSORS_IDS.put("activity", 100003.0);
		SENSORS_IDS.put("gyroscope", 100004.0);
		SENSORS_IDS.put("light", 100005.0);
		SENSORS_IDS.put("wifi", 100006.0);
		
		
		setContentView(R.layout.activity_publish_data);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		registerForContextMenu(findViewById(R.id.select_server_btn));

		serverEditText = (EditText) findViewById(R.id.editText_server);
		fromdateEditText = (EditText) findViewById(R.id.fromdate);
		todateEditText = (EditText) findViewById(R.id.todate);
		totimeEditText = (EditText) findViewById(R.id.totime);
		fromtimeEditText = (EditText) findViewById(R.id.fromtime);
		
		Date date = new Date();
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, -15);
		totimeEditText.setText(new SimpleDateFormat("HH:mm").format(date));
		todateEditText.setText(new SimpleDateFormat("dd.MM.yyyy").format(date));
		fromtimeEditText.setText(new SimpleDateFormat("HH:mm").format(cal.getTime()));
		fromdateEditText.setText(new SimpleDateFormat("dd.MM.yyyy").format(cal.getTime()));
		serverEditText.setText(DEFAULT_SERVER);
		context = this;
		setUpController();
	}

	public void setUpController() {
		handlerVS = new Handler() {
			@SuppressWarnings("unchecked")
			@Override
			public void handleMessage(Message msg) {
				vsList = (ArrayList<AbstractVirtualSensor>) msg.obj;
				//renderVSList();
			};
		};

		controller = new AndroidControllerPublishData(this);
		controller.setHandlerVS(handlerVS);
		controller.loadListVS();
	}

	/*
	public void renderVSList() {
	
		List<String> list = new ArrayList<String>();

		for (VirtualSensor vs : vsList) {
			list.add(vs.getConfig().getName());
		}

		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
				R.layout.spinner_item, list);
		dataAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_vsName.setAdapter(dataAdapter);

		spinner_vsName.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos,
					long id) {
				Toast.makeText(
						parent.getContext(),
						"The VS \"" + parent.getItemAtPosition(pos).toString()
								+ "\" is selected.", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
	}
*/
	
/*	
	public void renderStrategyList() {
	
		List<String> list = new ArrayList<String>();

		for (String s : STRATEGY) {
			list.add(s);
		}

		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
				R.layout.spinner_item, list);
		dataAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_strategy.setAdapter(dataAdapter);

		spinner_strategy.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos,
					long id) {

				if (pos != 0) {
					Toast.makeText(
							parent.getContext(),
							"The strategy \"" + parent.getItemAtPosition(pos).toString()
									+ "\" has not been implemented!", Toast.LENGTH_SHORT).show();

				}
				else
					Toast.makeText(
							parent.getContext(),
							"The strategy \"" + parent.getItemAtPosition(pos).toString()
									+ "\" is selected.", Toast.LENGTH_SHORT).show();

			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
	}
*/
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add("Publish").setShowAsAction(
				MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		return super.onCreateOptionsMenu(menu);
	}

	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
//			Intent myIntent = new Intent(this, ActivityHome.class);
//			this.startActivity(myIntent);
			finish();
			break;
		case 0:
			publish(null);
			break;
		}
		return true;
	}
	
	public void publish(View view) {
		for (String vs_name : SENSORS_IDS.keySet()){
			StreamElement[] se = controller.loadRangeData(vs_name,fromdateEditText.getText().toString(),fromtimeEditText.getText().toString(),todateEditText.getText().toString(),totimeEditText.getText().toString());
	
			if (se.length > 0) {
				new PublishDataTask(new PushDelivery(serverEditText.getText() + "/streaming/", SENSORS_IDS.get(vs_name))).execute(se);
			}
			else {
				Toast.makeText(this, "No data to publish", Toast.LENGTH_SHORT)
						.show();
			}
		}
	}

	private class PublishDataTask extends AsyncTask<StreamElement[], Void, Boolean> {
		
		PushDelivery push;
		
		public PublishDataTask (PushDelivery p){
			push = p;
		}

		private StreamElement[] se = null;

		@Override
		protected Boolean doInBackground(StreamElement[]... params) {
			se = params[0];
			return push.writeStreamElements(se);
		}

		protected void onPostExecute(Boolean results) {
			if (results == true)
				Toast.makeText(context, "Published: " + se.length,
						Toast.LENGTH_SHORT).show();
			else
				Toast.makeText(context, "Publish fail: " + se.length,
						Toast.LENGTH_SHORT).show();
		}
	}

	// @Override
	// public boolean onOptionsItemSelected(MenuItem item) {
	// //This uses the imported MenuItem from ActionBarSherlock
	// Toast.makeText(this, "Got click: " + item.toString(),
	// Toast.LENGTH_SHORT).show();
	// return true;
	// }

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		//menu.add("http://10.0.2.2:22001");
		//menu.add("http://gsn.ijs.si");
		//menu.add("http://montblanc.slf.ch:22001");
		//menu.add("http://data.permasense.ch");
		menu.add("http://lsir-cloud.epfl.ch/gsn");
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		// Note how this callback is using the fully-qualified class name
		//Toast.makeText(this, "Got click: " + item.toString(), Toast.LENGTH_SHORT)
		//		.show();
		serverEditText.setText(item.toString());
		return true;
	}

	public void select_server(View view) {
		view.showContextMenu();
	}
}
