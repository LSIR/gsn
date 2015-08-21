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
* File: gsn-tiny/src/tinygsn/gui/android/ActivityListSubscription.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.gui.android;

import java.util.ArrayList;
import java.util.List;
import tinygsn.controller.AndroidControllerSubscribe;
import tinygsn.gui.android.gcm.CommonUtilities;
import tinygsn.gui.android.utils.SensorListAdapter;
import tinygsn.gui.android.utils.SensorRow;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.google.android.gcm.GCMRegistrar;

@SuppressLint("NewApi")
public class ActivityListSubscription extends SherlockActivity {

	private static final String TAG = "ActivityListSubscription";
	private ListView listViewSubscription;
	private Context context;
	Handler handlerData;
	AndroidControllerSubscribe controller;
	List<SensorRow> subscriptionRowList;
	ArrayList<SensorRow> dataList = new ArrayList<SensorRow>();
	TextView numVS = null;

	private final Handler handler = new Handler();
	

	private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String data = intent.getExtras().getString(CommonUtilities.EXTRA_MESSAGE);
			String serverName = intent.getExtras().getString(
					CommonUtilities.EXTRA_SERVER_NAME);

			Log.i(TAG, "BroadcastReceiver onReceive: " + data);
			
			if (serverName == null)
				Toast.makeText(context, data, Toast.LENGTH_SHORT).show();
			else{
			//	controller.saveNewSubscriptionData(serverName, data);
			//	controller.loadListSubsData();
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sensors_list);
		context = this;

		GCMRegistrar.checkDevice(this);
		GCMRegistrar.checkManifest(this);
		registerReceiver(mHandleMessageReceiver, new IntentFilter(
				CommonUtilities.DISPLAY_MESSAGE_ACTION));
		
//		final String regId = GCMRegistrar.getRegistrationId(this);
//		registerOnServer(regId);

		setUpController();
	}

	
	@SuppressWarnings("unchecked")
	public void setUpController() {

		handlerData = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				dataList = (ArrayList<SensorRow>) msg.obj;
				//TextView txt = (TextView) findViewById(R.id.txt);
				//txt.setText(dataList.size() + " data are loaded!");
				renderLayout(dataList);
			};
		};

		controller = new AndroidControllerSubscribe(this);
		controller.setHandlerData(handlerData);
		//controller.loadListSubsData();
	}

	private void renderLayout(ArrayList<SensorRow> subscriptionRowList) {

/*		listViewSubscription = (ListView) findViewById(R.id.subscription_list);
		SensorListAdapter dataListAdapter = new SubscriptionListAdapter(
				context, R.layout.sensor_row_item, subscriptionRowList,
				controller, this);
		listViewSubscription.setAdapter(dataListAdapter);
		dataListAdapter.notifyDataSetChanged();

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);*/
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		final MenuItem add = menu.add("Add");
		add.setIcon(R.drawable.plus_b).setShowAsAction(
				MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		add.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			public boolean onMenuItemClick(final MenuItem item) {
				item.setIcon(R.drawable.plus_b);
				handler.postDelayed(new Runnable() {
					public void run() {
						item.setIcon(R.drawable.plus_b);
					}
				}, 10);

				startVSActivity();

				return false;
			}
		});

		final MenuItem deleteAll = menu.add("Delete all");
		deleteAll.setIcon(R.drawable.full_trash).setShowAsAction(
				MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		deleteAll.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			public boolean onMenuItemClick(MenuItem item) {
			//	controller.deleteAll();
			//	controller.loadListSubsData();
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
		//	controller.markDataUnreadToRead();
			finish();
			break;
		}
		return true;
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(mHandleMessageReceiver);
//		GCMRegistrar.onDestroy(this);
		super.onDestroy();
	}

	private void startVSActivity() {
		Intent myIntent = new Intent(this, ActivitySubscribe.class);
		this.startActivity(myIntent);
	}

	public void load_more(View view) {
	//	controller.loadMore();
	}
}