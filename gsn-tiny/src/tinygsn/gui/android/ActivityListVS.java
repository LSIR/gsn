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
* File: gsn-tiny/src/tinygsn/gui/android/ActivityListVSNew.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.gui.android;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import tinygsn.beans.StreamElement;
import tinygsn.controller.AndroidControllerListVS;
import tinygsn.gui.android.utils.VSListAdapter;
import tinygsn.gui.android.utils.VSRow;
import tinygsn.model.vsensor.AbstractVirtualSensor;
import tinygsn.model.wrappers.AbstractWrapper;
import tinygsn.storage.db.SqliteStorageManager;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;

@SuppressLint("NewApi")
public class ActivityListVS extends SherlockActivity implements Serializable  {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8598546037770495346L;
	private ListView listViewVS;
	private Context context;
	Handler handlerVS;
	AndroidControllerListVS controller;
	List<VSRow> vsRowList;
	ArrayList<AbstractVirtualSensor> vsList = new ArrayList<AbstractVirtualSensor>();
	TextView numVS = null;

	private final Handler handler = new Handler();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.vs_list);
		
		String VSName  = null;
		VSName = (String) getIntent().getSerializableExtra("VSName");
		if(VSName != null)
		{
			SqliteStorageManager storage = new SqliteStorageManager(this);
			AbstractVirtualSensor vs = storage.getVSByName(VSName);
			AndroidControllerListVS controllerListVSNew  = new AndroidControllerListVS(this);
			vs.getConfig().setController(controllerListVSNew);
			vs.start(this);
		}
		
		context = this;

		AbstractWrapper.getWrapperList(this);
		setUpController();
	}

	// ~~~~~~~~~~~~~~~~Handle the result from Controller~~~~~~~~~~~~~~~~
	public void setUpController() {
		handlerVS = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				vsList = (ArrayList<AbstractVirtualSensor>) msg.obj;
				renderLayout(vsList);
			};
		};

		controller = new AndroidControllerListVS(this);
		controller.setHandlerVS(handlerVS);
		controller.loadListVS();
	}

	private void renderLayout(ArrayList<AbstractVirtualSensor> vsList) {
		vsRowList = new ArrayList<VSRow>();
		for (AbstractVirtualSensor vs : vsList) {
			DecimalFormat df = new DecimalFormat("#.##");

			String latest = "";
			StreamElement se = controller.loadLatestData(vs.getConfig().getName());
			if (se != null)
				for (String field : se.getFieldNames()) {
					latest += field + ": " + df.format(se.getData(field)) + "\n";
				}

			vsRowList.add(new VSRow(vs.getConfig().getName(), vs.getConfig()
					.getRunning(), latest));
		}

		listViewVS = (ListView) findViewById(R.id.vs_list);
		VSListAdapter vSListAdapter = new VSListAdapter(context,
				R.layout.vs_row_item, vsRowList, controller, this);
		listViewVS.setAdapter(vSListAdapter);
		vSListAdapter.notifyDataSetChanged();
		
		ActionBar actionBar = getSupportActionBar();
		actionBar.setCustomView(R.layout.actionbar_top); // load your layout
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME
				| ActionBar.DISPLAY_SHOW_CUSTOM); // show it

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		numVS = (TextView) actionBar.getCustomView().findViewById(R.id.num_vs);

		if (numVS == null) {
			Toast.makeText(context, "numVS is null", Toast.LENGTH_SHORT).show();
		}
		else {
			numVS.setText(vsRowList.size() + "");
		}

		TextView lastUpdate = (TextView) actionBar.getCustomView().findViewById(
				R.id.lastUpdate);
		lastUpdate.setText("Last update:\n" + (new Date()).toString());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		final MenuItem add = menu.add("Add");
		add.setIcon(R.drawable.add).setShowAsAction(
				MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		add.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			// on selecting show add2 for 0.01s
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

		final MenuItem refresh = menu.add("Refresh");
		refresh.setIcon(R.drawable.ic_menu_refresh_holo_light).setShowAsAction(
				MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		refresh.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			// on selecting show progress spinner for 1s
			public boolean onMenuItemClick(MenuItem item) {
				item.setActionView(R.layout.indeterminate_progress_action);
				handler.postDelayed(new Runnable() {
					public void run() {
						refresh.setActionView(null);
						setUpController();
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

	private void startVSActivity() {
		Intent myIntent = new Intent(this, ActivityVSConfig.class);
		this.startActivity(myIntent);
	}
}