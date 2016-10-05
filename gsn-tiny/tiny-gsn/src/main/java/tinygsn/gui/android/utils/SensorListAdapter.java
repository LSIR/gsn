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
 * File: gsn-tiny/src/tinygsn/gui/android/utils/SubscriptionListAdapter.java
 *
 * @author Do Ngoc Hoan
 */


package tinygsn.gui.android.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

import tinygsn.beans.StaticData;
import tinygsn.controller.AndroidControllerWrapper;
import tinygsn.gui.android.ActivityListSensor;
import tinygsn.gui.android.ActivityWrapperEdit;
import tinygsn.gui.android.R;
import tinygsn.storage.db.SqliteStorageManager;


public class SensorListAdapter extends ArrayAdapter<SensorRow> {

	public static final String EXTRA_SENSOR_NAME = "name";
	private int resource;
	private LayoutInflater inflater;
	private Context context;

	public SensorListAdapter(Context ctx, int resourceId, List<SensorRow> objects, AndroidControllerWrapper controller) {
		super(ctx, resourceId, objects);
		resource = resourceId;
		inflater = LayoutInflater.from(ctx);
		context = ctx;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		convertView = inflater.inflate(resource, null);

		final SensorRow vs = getItem(position);

		TextView sensorTxt = (TextView) convertView.findViewById(R.id.sensor_name);
		String[] name = vs.getName().split("\\.");
		sensorTxt.setText(name[name.length - 1]);
		final Switch activeStch = (Switch) convertView.findViewById(R.id.enableWSwitch);
		activeStch.setChecked(vs.isActive());
		activeStch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				new AsyncTask<Boolean, Void, Boolean>() {
					@Override
					protected Boolean doInBackground(Boolean... params) {
						try {
							if (params[0]) {
								vs.setActive(true);
								return StaticData.getWrapperByName(vs.getName()).start();
							} else {
								vs.setActive(false);
								return StaticData.getWrapperByName(vs.getName()).stop();
							}
						} catch (Exception e) {
							return false;
						}
					}

					@Override
					protected void onPostExecute(Boolean result) {
						((ActivityListSensor) context).disableManaged();
						//activeStch.setChecked(result ^ activeStch.isChecked()); //infinite loop!
					}
				}.execute(isChecked);

			}
		});

		TextView dataTxt = (TextView) convertView.findViewById(R.id.info_values);
		dataTxt.setText(vs.getInfo());

		ImageView view = (ImageView) convertView.findViewById(R.id.view_flows);
		view.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialogDetail(vs.getName());
			}
		});

		ImageView edit = (ImageView) convertView.findViewById(R.id.edit_wrapper);
		edit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent myIntent = new Intent(context, ActivityWrapperEdit.class);
				myIntent.putExtra(EXTRA_SENSOR_NAME, vs.getName());
				context.startActivity(myIntent);
			}
		});

		if (vs.isManaged()) {
			activeStch.setEnabled(false);
			view.setEnabled(false);
			view.setVisibility(View.INVISIBLE);
			edit.setEnabled(false);
			edit.setVisibility(View.INVISIBLE);
			convertView.setBackgroundColor(Color.rgb(200, 200, 200));
		}

		return convertView;
	}

	private void showDialogDetail(String name) {
		SqliteStorageManager storage = new SqliteStorageManager();
		StringBuilder sb = new StringBuilder("This sensor is used by the following Virtual Sensors:\n");
		for (String s : storage.getVSfromSource(name)) {
			sb.append(" - ").append(s).append("\n");
		}
		DialogFragment newFragment = DetailedDataFragment.newInstance(sb.toString());
		newFragment.show(((FragmentActivity) context).getSupportFragmentManager(), "dialog");
	}

	public static class DetailedDataFragment extends DialogFragment {
		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		String text;

		public static DetailedDataFragment newInstance(String text) {
			DetailedDataFragment i = new DetailedDataFragment();
			i.setText(text);
			return i;
		}

		public DetailedDataFragment(){
		}


		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
		                         Bundle savedInstanceState) {
			View v = inflater.inflate(R.layout.text, container, false);
			View tv = v.findViewById(R.id.text);
			((TextView) tv).setText(text);
			return v;
		}
	}
}
