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
* File: gsn-tiny/src/tinygsn/gui/android/utils/SubscriptionListAdapter.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.gui.android.utils;

import java.util.List;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import tinygsn.beans.StaticData;
import tinygsn.controller.AndroidControllerWrapper;
import tinygsn.gui.android.ActivityListSensor;
import tinygsn.gui.android.ActivityWrapperEdit;
import tinygsn.gui.android.R;
import tinygsn.storage.db.SqliteStorageManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;


public class SensorListAdapter extends ArrayAdapter<SensorRow> {

	public static final String EXTRA_SENSOR_NAME = "name";
	private int resource;
	private LayoutInflater inflater;
	private Context context;
	static int TEXT_SIZE = 8;

	public SensorListAdapter(Context ctx, int resourceId, List<SensorRow> objects, AndroidControllerWrapper controller) {
		super(ctx, resourceId, objects);
		resource = resourceId;
		inflater = LayoutInflater.from(ctx);
		context = ctx;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		convertView = (LinearLayout) inflater.inflate(resource, parent);

		final SensorRow vs = getItem(position);

		TextView sensorTxt = (TextView) convertView.findViewById(R.id.sensor_name);
		String[] name = vs.getName().split("\\.");
		sensorTxt.setText(name[name.length-1]);

		final Switch activeStch = (Switch) convertView.findViewById(R.id.enableWSwitch);
		activeStch.setChecked(vs.isActive());
		activeStch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				new AsyncTask<Boolean, Void, Boolean>(){
					@Override
					protected Boolean doInBackground(Boolean... params) {
						try{
							if (params[0]){
								vs.setActive(true);
								return StaticData.getWrapperByName(vs.getName()).start();
							}else{
								vs.setActive(false);
								return StaticData.getWrapperByName(vs.getName()).stop();
							}
						}
						catch(Exception e){
							return false;
						}
					}
					@Override
					protected void onPostExecute(Boolean result) {
						((ActivityListSensor)context).disableManaged();
						//activeStch.setChecked(result ^ activeStch.isChecked()); //infinite loop!
					}
				}.execute(isChecked);
				
			}
		});

		TextView dataTxt = (TextView) convertView.findViewById(R.id.info_values);
		dataTxt.setText(vs.getInfo());

		ImageButton view = (ImageButton) convertView.findViewById(R.id.view_flows);
		view.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialogDetail(vs.getName());
			}
		});

		ImageButton edit = (ImageButton) convertView
				.findViewById(R.id.edit_wrapper);
		edit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent myIntent = new Intent(context, ActivityWrapperEdit.class);
				myIntent.putExtra(EXTRA_SENSOR_NAME, vs.getName());
				context.startActivity(myIntent);
			}
		});
		
		if(vs.isManaged()){
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
		for (String s :storage.getVSfromSource(name)){
			sb.append(" - ").append(s).append("\n");
		}
		DialogFragment newFragment = DetailedDataFragment.newInstance(sb.toString());
		newFragment.show(((SherlockFragmentActivity) context).getSupportFragmentManager(), "dialog");
	}

	public static class DetailedDataFragment extends SherlockDialogFragment {
		String text;

		public static DetailedDataFragment newInstance(String text) {
			return new DetailedDataFragment(text);
		}

		public DetailedDataFragment(String text) {
			this.text = text;
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
