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
 * File: gsn-tiny/src/tinygsn/gui/android/ActivityListVSNew.java
 *
 * @author Do Ngoc Hoan
 */


package tinygsn.gui.android;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

import tinygsn.beans.StreamElement;
import tinygsn.controller.AndroidControllerVS;
import tinygsn.gui.android.utils.VSListAdapter;
import tinygsn.gui.android.utils.VSRow;
import tinygsn.model.vsensor.AbstractVirtualSensor;
import tinygsn.model.wrappers.AbstractWrapper;

@SuppressLint("NewApi")
public class ActivityListVS extends AbstractActivity implements Serializable {

	private static final long serialVersionUID = 8598546037770495346L;
	private ListView listViewVS;
	private final Handler handler = new Handler();
	VSListAdapter vSListAdapter;
	AndroidControllerVS controller = new AndroidControllerVS();
	TextView numVS = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.vs_list);
		AbstractWrapper.getWrapperList(this);
		listViewVS = (ListView) findViewById(R.id.vs_list);
		vSListAdapter = new VSListAdapter(this, R.layout.vs_row_item, controller, this);
		listViewVS.setAdapter(vSListAdapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		final MenuItem add = menu.add("Add");
		add.setIcon(R.drawable.ic_action_new).setShowAsAction(
			MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		add.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(final MenuItem item) {
				startVSActivity();
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

	public void initialize() {
		new AsyncTask<AndroidControllerVS, Void, ArrayList<AbstractVirtualSensor>>() {
			@Override
			protected ArrayList<AbstractVirtualSensor> doInBackground(AndroidControllerVS... params) {
				return params[0].loadListVS();
			}

			@Override
			protected void onPostExecute(ArrayList<AbstractVirtualSensor> result) {
				renderLayout(result);
			}
		}.execute(controller);
	}

	@Override
	protected void onResume() {
		super.onResume();
		initialize();
	}

	private void renderLayout(ArrayList<AbstractVirtualSensor> vsList) {

		ActionBar actionBar = getActionBar();
		actionBar.setCustomView(R.layout.actionbar_top); // load your layout
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME
			| ActionBar.DISPLAY_SHOW_CUSTOM); // show it

		actionBar.setDisplayHomeAsUpEnabled(true);

		numVS = (TextView) actionBar.getCustomView().findViewById(R.id.num_vs);
		numVS.setText("0");
		vSListAdapter.clear();
		vSListAdapter.notifyDataSetChanged();

		for (AbstractVirtualSensor vs : vsList) {

			new AsyncTask<AbstractVirtualSensor, Void, VSRow>() {
				@Override
				protected VSRow doInBackground(AbstractVirtualSensor... params) {
					StreamElement se = controller.loadLatestData(params[0].getConfig().getName());
					DecimalFormat df = new DecimalFormat("#.##");
					String latest = "";
					if (se != null)
						for (String field : se.getFieldNames()) {
							latest += field + ": " + df.format(se.getData(field)) + "\n";
						}
					return new VSRow(params[0].getConfig().getName(), params[0].getConfig().getRunning(), latest);
				}

				@Override
				protected void onPostExecute(VSRow result) {
					vSListAdapter.add(result);
					vSListAdapter.notifyDataSetChanged();
					numVS.setText(vSListAdapter.getCount() + "");
				}
			}.execute(vs);

		}
		TextView lastUpdate = (TextView) actionBar.getCustomView().findViewById(
			R.id.lastUpdate);
		lastUpdate.setText("Last update:\n" + (new Date()).toString());
	}


	private void startVSActivity() {
		Intent myIntent = new Intent(this, ActivityVSConfig.class);
		this.startActivity(myIntent);
	}
}