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
import java.util.Locale;

import tinygsn.beans.DeliveryRequest;
import tinygsn.beans.StreamElement;
import tinygsn.controller.AndroidControllerPublish;
import tinygsn.storage.db.SqliteStorageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.ContextMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import android.view.Menu;
import android.view.MenuItem;

public class ActivityPublishData extends FragmentActivity {
	public static String[] STRATEGY = { "On demand", "Periodically", "Opportunistically" };
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
    private Spinner vsnameSpinner = null;
    private Spinner modeSpinner = null;
    private DeliveryRequest dr = null;
    private SqliteStorageManager storage;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
			
		setContentView(R.layout.activity_publish_data);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		registerForContextMenu(findViewById(R.id.select_server_btn));

		serverEditText = (EditText) findViewById(R.id.editText_server);
		keyEditText = (EditText) findViewById(R.id.editText_key);
		fromdateEditText = (EditText) findViewById(R.id.fromdate);
		todateEditText = (EditText) findViewById(R.id.todate);
		totimeEditText = (EditText) findViewById(R.id.totime);
		fromtimeEditText = (EditText) findViewById(R.id.fromtime);
		vsnameSpinner = (Spinner)  findViewById(R.id.spinner_vsName);
		modeSpinner = (Spinner) findViewById(R.id.spinner_mode);
		publishButton = (Button) findViewById(R.id.Button_publish);

		controller = new AndroidControllerPublish();
		storage = new SqliteStorageManager();
		
		Date date = new Date();
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, -15);
		
		String extra = getIntent().getStringExtra("tynigsn.beans.id");
		if (extra != null) {
			try{
			dr = storage.getPublishInfo(Integer.parseInt(extra));
			serverEditText.setText(dr.getUrl());
			keyEditText.setText(dr.getKey());
			cal.setTimeInMillis(dr.getLastTime());
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		totimeEditText.setText(new SimpleDateFormat("HH:mm",Locale.ENGLISH).format(date));
		todateEditText.setText(new SimpleDateFormat("dd.MM.yyyy",Locale.ENGLISH).format(date));
		fromtimeEditText.setText(new SimpleDateFormat("HH:mm",Locale.ENGLISH).format(cal.getTime()));
		fromdateEditText.setText(new SimpleDateFormat("dd.MM.yyyy",Locale.ENGLISH).format(cal.getTime()));
		renderVSList();
		renderModeList();	
	}



	public void renderVSList() {
	
		new AsyncTask<AndroidControllerPublish, Void, ArrayList<String>>(){
			@Override
			protected ArrayList<String> doInBackground(AndroidControllerPublish... params) {
				return params[0].loadListVS();
			}
			@Override
			protected void onPostExecute(ArrayList<String> result) {
                ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(ActivityPublishData.this,R.layout.spinner_item, result);
				dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				vsnameSpinner.setAdapter(dataAdapter);	
				if (dr != null){
					int p = dataAdapter.getPosition(dr.getVsname());
					if (p != -1){
						vsnameSpinner.setSelection(p);
					}
				}
			}
		}.execute(controller);
	}
	
	public void renderModeList() {
	
		modeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
				if (pos != 0) {
					fromdateEditText.setVisibility(View.GONE);
					todateEditText.setVisibility(View.GONE);
					totimeEditText.setVisibility(View.GONE);
					fromtimeEditText.setVisibility(View.GONE);
					publishButton.setVisibility(View.GONE);
					findViewById(R.id.textViewFrom).setVisibility(View.GONE);
					findViewById(R.id.textViewTo).setVisibility(View.GONE);
				}
				else{
					fromdateEditText.setVisibility(View.VISIBLE);
					todateEditText.setVisibility(View.VISIBLE);
					totimeEditText.setVisibility(View.VISIBLE);
					fromtimeEditText.setVisibility(View.VISIBLE);
					publishButton.setVisibility(View.VISIBLE);
					findViewById(R.id.textViewFrom).setVisibility(View.VISIBLE);
					findViewById(R.id.textViewTo).setVisibility(View.VISIBLE);
				}
					
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
		if (dr != null){
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
			save();
			finish();
			break;
		}
		return true;
	}
	
	public void save(){
		int id = -1;
		long lastTime = 0;
		boolean active = false;
		if(dr != null){
			id = dr.getId();
			lastTime = dr.getLastTime();
			active = dr.isActive();
		}
		storage.setPublishInfo(id, serverEditText.getText().toString(), vsnameSpinner.getSelectedItem().toString(),
				keyEditText.getText().toString(), modeSpinner.getSelectedItemPosition(), lastTime, active);
	}
	
	public void publish(View view) {
			StreamElement[] se = controller.loadRangeData(vsnameSpinner.getSelectedItem().toString(),fromdateEditText.getText().toString(),fromtimeEditText.getText().toString(),todateEditText.getText().toString(),totimeEditText.getText().toString());
			if (se.length > 0) {
				new PublishDataTask(new PushDelivery(serverEditText.getText() + "/streaming/", Double.parseDouble(keyEditText.getText().toString()))).execute(se);
			}
			else {
				Toast.makeText(this, "No data to publish", Toast.LENGTH_SHORT)
						.show();
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
			if (results == true){
				Toast.makeText(ActivityPublishData.this, "Published: " + se.length,
						Toast.LENGTH_SHORT).show();
			    dr.setLastTime(System.currentTimeMillis());
			    save();
			}else{
				Toast.makeText(ActivityPublishData.this, "Publish fail: " + se.length,
						Toast.LENGTH_SHORT).show();
			}
		}
	}


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
