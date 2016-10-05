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
 * File: gsn-tiny/src/tinygsn/gui/android/ActivityListVSNew.java
 *
 * @author Do Ngoc Hoan and Schaer Marc
 */


package tinygsn.gui.android;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import tinygsn.beans.DeliveryRequest;
import tinygsn.controller.AndroidControllerPublish;
import tinygsn.gui.android.utils.PublishListAdapter;
import tinygsn.gui.android.utils.PublishRow;
import tinygsn.gui.android.utils.SubscribeRow;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ListView;
import android.widget.TextView;

import android.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;


public class ActivityListPublish extends AbstractActivity implements Serializable {

	private static final long serialVersionUID = 8598546037770495346L;
	private ListView listViewPublish;
	PublishListAdapter listAdapter;
	AndroidControllerPublish controller = new AndroidControllerPublish();

	TextView numVS = null;

	private final Handler handler = new Handler();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_publish_list);
		listViewPublish = (ListView) findViewById(R.id.publish_list);
		listAdapter = new PublishListAdapter(this, R.layout.publish_row_item, this);
		listViewPublish.setAdapter(listAdapter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		initialize();
	}


	public void initialize() {
		new AsyncTask<AndroidControllerPublish, Void, ArrayList<DeliveryRequest>>() {
			@Override
			protected ArrayList<DeliveryRequest> doInBackground(AndroidControllerPublish... params) {
				return params[0].loadList();
			}

			@Override
			protected void onPostExecute(ArrayList<DeliveryRequest> result) {
				renderLayout(result);
			}
		}.execute(controller);
	}

	private void renderLayout(ArrayList<DeliveryRequest> list) {

		ActionBar actionBar = getActionBar();
		actionBar.setCustomView(R.layout.actionbar_top); // load your layout
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME
			| ActionBar.DISPLAY_SHOW_CUSTOM); // show it

		actionBar.setDisplayHomeAsUpEnabled(true);

		numVS = (TextView) actionBar.getCustomView().findViewById(R.id.num_vs);
		numVS.setText("0");
		listAdapter.clear();
		listAdapter.notifyDataSetChanged();

		for (DeliveryRequest dr : list) {

			new AsyncTask<DeliveryRequest, Void, PublishRow>() {
				@Override
				protected PublishRow doInBackground(DeliveryRequest... params) {
					Calendar cal = Calendar.getInstance();
					cal.setTimeInMillis(params[0].getLastTime());
					String info = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH).format(cal.getTime());
					return new PublishRow(params[0].getId(), params[0].getUrl(), params[0].getClientID(),params[0].getClientSecret(), params[0].isActive(), "Last connection: " + info, params[0].getVsname());
				}

				@Override
				protected void onPostExecute(PublishRow result) {
					listAdapter.add(result);
					listAdapter.notifyDataSetChanged();
					numVS.setText(listAdapter.getCount() + "");
				}
			}.execute(dr);

		}
		TextView lastUpdate = (TextView) actionBar.getCustomView().findViewById(
			R.id.lastUpdate);
		lastUpdate.setText("Last update:\n" + (new Date()).toString());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		final MenuItem add = menu.add("Add");
		add.setIcon(R.drawable.ic_action_new).setShowAsAction(
			MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		add.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			public boolean onMenuItemClick(final MenuItem item) {
				Intent myIntent = new Intent(ActivityListPublish.this, ActivityPublishData.class);
				ActivityListPublish.this.startActivity(myIntent);
				return false;
			}
		});

		final MenuItem refresh = menu.add("Refresh");
		refresh.setIcon(R.drawable.ic_action_refresh).setShowAsAction(
			MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		refresh.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			// on selecting show progress spinner for 1s
			public boolean onMenuItemClick(MenuItem item) {
				item.setActionView(R.layout.indeterminate_progress_action);
				handler.postDelayed(new Runnable() {
					public void run() {
						refresh.setActionView(null);
						initialize();
					}
				}, 50);
				return false;
			}
		});

		return super.onCreateOptionsMenu(menu);
	}

	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
			case android.R.id.home:
				finish();
				break;
		}
		return true;
	}
}