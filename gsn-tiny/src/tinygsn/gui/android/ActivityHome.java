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
* File: gsn-tiny/src/tinygsn/gui/android/ActivityHome.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.gui.android;


import java.util.ArrayList;

import tinygsn.model.vsensor.AbstractVirtualSensor;
import tinygsn.services.LocationScheduler;
import tinygsn.storage.db.SqliteStorageManager;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.readystatesoftware.viewbadger.BadgeView;

public class ActivityHome extends SherlockActivity {

	Handler handlerVS;
	TextView numVS = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home);
		getSupportActionBar().hide();
		TextView subscribe = (TextView) findViewById(R.id.tvSubscribe);
		BadgeView badge = new BadgeView(this, subscribe);
		badge.setBadgePosition(BadgeView.POSITION_BOTTOM_RIGHT); 
		badge.setText("2");
		badge.show();

		new AsyncTask<Activity, Void, Void>(){
			@Override
			protected Void doInBackground(Activity... params) {
				//start all defined virtual sensors
				SqliteStorageManager storage = new SqliteStorageManager();
				ArrayList<AbstractVirtualSensor> vsList = storage.getListofVS();
				for (AbstractVirtualSensor vs : vsList) {
					if (vs.getConfig().getRunning() == true) {
						vs.start();
					}
				}
				
				//start the scheduler
				/*Intent serviceIntent = null;
				serviceIntent = new Intent(this, schedular.class);
				this.startService(serviceIntent);*/
				return null;
			}
		}.execute(this);
		
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		final MenuItem add = menu.add("Quit TinyGSN");

		add.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(final MenuItem item) {
				finish();
				return true;
			}
		});

		return super.onCreateOptionsMenu(menu);
	}
	
	public void open_listVSActivity(View view) {
		Intent myIntent = new Intent(this, ActivityListVS.class);
		this.startActivity(myIntent);
	}

	public void open_publishActivity(View view) {
		Intent myIntent = new Intent(this, ActivityListPublish.class);
		this.startActivity(myIntent);
	}

	public void open_listWrapperActivity(View view) {
		Intent myIntent = new Intent(this, ActivityListSensor.class);
		this.startActivity(myIntent);
	}

	public void open_browseActivity(View view) {
		Intent myIntent = new Intent(this, ActivityListSubscription.class);
		this.startActivity(myIntent);
	}
	
	public void open_helpActivity(View view) {
		Intent myIntent = new Intent(this, ActivityHelp.class);
		this.startActivity(myIntent);
	}

	public void open_aboutActivity(View view) {
		Intent myIntent = new Intent(this, ActivityAboutUs.class);
		this.startActivity(myIntent);
	}
}