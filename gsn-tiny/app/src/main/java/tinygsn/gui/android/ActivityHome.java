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
 * File: gsn-tiny/src/tinygsn/gui/android/ActivityHome.java
 *
 * @author Do Ngoc Hoan
 */


package tinygsn.gui.android;


import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.TextView;

import tinygsn.model.wrappers.LocalWrapper;
//import com.readystatesoftware.viewbadger.BadgeView;

public class ActivityHome extends Activity {

	Handler handlerVS;
	TextView numVS = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home);
		getActionBar().hide();
		/*TextView subscribe = (TextView) findViewById(R.id.tvSubscribe);
		BadgeView badge = new BadgeView(this, subscribe);
		badge.setBadgePosition(BadgeView.POSITION_BOTTOM_RIGHT);
		badge.setText("2");
		badge.show();*/

		new AsyncTask<Activity, Void, Void>() {
			@Override
			protected Void doInBackground(Activity... params) {
				LocalWrapper.startLocal();
				return null;
			}
		}.execute(this);

		warnIfAPILessThan20();
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

	private void warnIfAPILessThan20() {
		int currentAPIVersion = android.os.Build.VERSION.SDK_INT;
		//KITKAT corresponds to API 19
		if (currentAPIVersion <= Build.VERSION_CODES.KITKAT) {
			TextView messageAPI = (TextView) findViewById(R.id.warnAPIVersion);
			messageAPI.setText(getString(R.string.warnAPIVersionText));
		}
	}
}