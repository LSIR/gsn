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
import android.app.ProgressDialog;
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

import org.epfl.locationprivacy.map.activities.SemanticActivity;
import org.epfl.locationprivacy.map.databases.GridDBDataSource;
import org.epfl.locationprivacy.map.databases.VenuesCondensedDBDataSource;
import org.epfl.locationprivacy.privacyestimation.databases.LinkabilityGraphDataSource;
import org.epfl.locationprivacy.privacyprofile.activities.PrivacyProfileActivity;
import org.epfl.locationprivacy.privacyprofile.databases.SemanticLocationsDataSource;
import org.epfl.locationprivacy.userhistory.databases.LocationTableDataSource;
import org.epfl.locationprivacy.userhistory.databases.TransitionTableDataSource;

import tinygsn.model.publishers.AbstractDataPublisher;
import tinygsn.model.subscribers.AbstractSubscriber;
import tinygsn.model.wrappers.LocalWrapper;
//import com.readystatesoftware.viewbadger.BadgeView;

public class ActivityHome extends AbstractActivity {

	Handler handlerVS;
	TextView numVS = null;
	private ProgressDialog progressDialog;

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
				AbstractDataPublisher.startService();
				AbstractSubscriber.startService();
				return null;
			}
		}.execute(this);

		// Load databases for LocPrivLib
		new AsyncTask<Void, Integer, Void>() {
			@Override
			protected void onPreExecute() {
				//Create a new progress dialog
				progressDialog = new ProgressDialog(ActivityHome.this);
				//Set the progress dialog to display a horizontal progress bar
				progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				//Set the dialog title to 'Loading...'
				progressDialog.setTitle("Loading...");
				//Set the dialog message to 'Loading application View, please wait...'
				progressDialog.setMessage("Loading databases, please wait...");
				//This dialog can't be canceled by pressing the back key
				progressDialog.setCancelable(false);
				//This dialog isn't indeterminate
				progressDialog.setIndeterminate(false);
				//The maximum number of items is 100
				progressDialog.setMax(100);
				//Set the current progress to zero
				progressDialog.setProgress(0);
				//Display the progress dialog
				progressDialog.show();
			}

			//The code to be executed in a background thread.
			@Override
			protected Void doInBackground(Void... params) {
				//Get the current thread's token
				synchronized (this) {
					// open dbs
					GridDBDataSource.getInstance(ActivityHome.this);
					publishProgress((int) (1.0 * 100.0 / 6.0));
					VenuesCondensedDBDataSource.getInstance(ActivityHome.this);
					publishProgress((int) (2.0 * 100.0 / 6.0));
					LocationTableDataSource.getInstance(ActivityHome.this);
					publishProgress((int) (3.0 * 100.0 / 6.0));
					TransitionTableDataSource.getInstance(ActivityHome.this);
					publishProgress((int) (4.0 * 100.0 / 6.0));
					SemanticLocationsDataSource.getInstance(ActivityHome.this);
					// FIXME : ONLY FOR TESTING PURPOSE
					SemanticLocationsDataSource.getInstance(ActivityHome.this).updateSemanticLocation("university", 50);
					SemanticLocationsDataSource.getInstance(ActivityHome.this).updateSemanticLocation("bar", 40);
					SemanticLocationsDataSource.getInstance(ActivityHome.this).updateSemanticLocation("hospital", 100);
					SemanticLocationsDataSource.getInstance(ActivityHome.this).updateSemanticLocation("bank", 80);
					SemanticLocationsDataSource.getInstance(ActivityHome.this).updateSemanticLocation("restaurant", 30);
					SemanticLocationsDataSource.getInstance(ActivityHome.this).updateSemanticLocation("park", 0);
					SemanticLocationsDataSource.getInstance(ActivityHome.this).updateSemanticLocation("fast_food", 40);
					SemanticLocationsDataSource.getInstance(ActivityHome.this).updateSemanticLocation("hotel", 50);
					SemanticLocationsDataSource.getInstance(ActivityHome.this).updateSemanticLocation("clinic", 100);
					SemanticLocationsDataSource.getInstance(ActivityHome.this).updateSemanticLocation("nightclub", 80);
					SemanticLocationsDataSource.getInstance(ActivityHome.this).updateSemanticLocation("post_office", 10);
					SemanticLocationsDataSource.getInstance(ActivityHome.this).updateSemanticLocation("prison", 100);
					SemanticLocationsDataSource.getInstance(ActivityHome.this).updateSemanticLocation("museum", 5);
					SemanticLocationsDataSource.getInstance(ActivityHome.this).updateSemanticLocation("stadium", 25);
					SemanticLocationsDataSource.getInstance(ActivityHome.this).updateSemanticLocation("train_station", 15);
					SemanticLocationsDataSource.getInstance(ActivityHome.this).updateSemanticLocation("zoo", 30);
					SemanticLocationsDataSource.getInstance(ActivityHome.this).updateSemanticLocation("police", 70);
					SemanticLocationsDataSource.getInstance(ActivityHome.this).updateSemanticLocation("ferry_terminal", 15);
					SemanticLocationsDataSource.getInstance(ActivityHome.this).updateSemanticLocation("embassy", 90);
					SemanticLocationsDataSource.getInstance(ActivityHome.this).updateSemanticLocation("aerodrome", 70);
					SemanticLocationsDataSource.getInstance(ActivityHome.this).updateSemanticLocation("cemetery", 30);
					SemanticLocationsDataSource.getInstance(ActivityHome.this).updateSemanticLocation("commercial", 75);
					publishProgress((int) (5.0 * 100.0 / 6.0));
					LinkabilityGraphDataSource.getInstance(ActivityHome.this);
					publishProgress((int) (6.0 * 100.0 / 6.0));
				}
				return null;
			}

			//Update the progress
			protected void onProgressUpdate(Integer... values) {
				//set the current progress of the progress dialog
				progressDialog.setProgress(values[0]);
			}

			//after executing the code in the thread
			@Override
			protected void onPostExecute(Void result) {
				//close the progress dialog
				progressDialog.dismiss();
			}
		}.execute();


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

	public void open_privacySettings(View view) {
		Intent myIntent = new Intent(this, PrivacyProfileActivity.class);
		this.startActivity(myIntent);
	}

	public void update_semantic_locations(View view) {
		Intent myIntent = new Intent(this, SemanticActivity.class);
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