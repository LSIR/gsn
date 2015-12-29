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
 * File: gsn-tiny/src/tinygsn/gui/android/utils/VSListAdapter.java
 *
 * @author Do Ngoc Hoan
 */


package tinygsn.gui.android.utils;

import tinygsn.beans.StaticData;
import tinygsn.controller.AndroidControllerVS;
import tinygsn.gui.android.ActivityListVS;
import tinygsn.gui.android.ActivityViewData;
import tinygsn.gui.android.R;
import tinygsn.storage.db.SqliteStorageManager;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This list adapter for the ListActivity of Virtual Sensors UI
 *
 * @author Do Ngoc Hoan (hoan.do@epfl.ch)
 */
public class VSListAdapter extends ArrayAdapter<VSRow> {

	public static final String EXTRA_VS_NAME = "vs_name";
	private int resource;
	private LayoutInflater inflater;
	private Context context = null;
	private SqliteStorageManager storage = null;
	static int TEXT_SIZE = 8;
	AndroidControllerVS controller;
	ActivityListVS activityListVSNew;


	public VSListAdapter(Context ctx, int resourceId,
	                     AndroidControllerVS controller, ActivityListVS activityListVSNew) {

		super(ctx, resourceId);
		this.context = ctx;
		resource = resourceId;
		inflater = LayoutInflater.from(ctx);
		context = ctx;
		this.controller = controller;
		this.activityListVSNew = activityListVSNew;
		storage = new SqliteStorageManager();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		convertView = (LinearLayout) inflater.inflate(resource, null);

		final VSRow vs = getItem(position);

		TextView txtName = (TextView) convertView.findViewById(R.id.vs_name);
		txtName.setText(vs.getName());

		final Switch runningSwitch = (Switch) convertView
				                                      .findViewById(R.id.enableSwitch);
		runningSwitch.setTextOn("Running");
		runningSwitch.setTextOff("Disabled");
		runningSwitch.setChecked(vs.isRunning());
		runningSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				controller.startStopVS(vs.getName(), runningSwitch.isChecked());

				String state = "enabled";
				if (runningSwitch.isChecked() == false)
					state = "disabled";
				Toast.makeText(context,
						              vs.getName() + " is " + state + " successfully!",
						              Toast.LENGTH_SHORT).show();
			}
		});

		TextView txtWiki = (TextView) convertView.findViewById(R.id.latest_values);
		txtWiki.setText(vs.getLatestValue());

		ImageView view = (ImageView) convertView.findViewById(R.id.view);
		view.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivityViewData(vs.getName());
			}
		});

		ImageView edit = (ImageView) convertView.findViewById(R.id.config);
		edit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(context, "Config has not been implemented!", Toast.LENGTH_SHORT)
						.show();
			}
		});

		ImageView delete = (ImageView) convertView.findViewById(R.id.delete);
		delete.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
							case DialogInterface.BUTTON_POSITIVE:
								controller.deleteVS(vs.getName());
								StaticData.deleteVS(vs.getName());
								// Remove all informations about vs in settings table
								storage.deleteSetting("vsensor:" + vs.getName() + ":");

								Toast.makeText(context, vs.getName() + " is deleted!",
										              Toast.LENGTH_SHORT).show();
								activityListVSNew.initialize();

								break;
							case DialogInterface.BUTTON_NEGATIVE:
								break;
						}
					}
				};

				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setMessage("Are you sure you want to delete \'" + vs.getName() + "\'?")
						.setPositiveButton("Yes", dialogClickListener)
						.setNegativeButton("No", dialogClickListener).show();
			}
		});
		return convertView;

	}

	protected void startActivityViewData(String vsName) {
		Intent myIntent = new Intent(context, ActivityViewData.class);
		myIntent.putExtra(EXTRA_VS_NAME, vsName);

		context.startActivity(myIntent);
	}
}
