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


import tinygsn.beans.Subscription;
import tinygsn.controller.AndroidControllerSubscribe;
import tinygsn.gui.android.ActivitySubscribeData;
import tinygsn.gui.android.R;
import tinygsn.storage.db.SqliteStorageManager;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
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



public class SubscribeListAdapter extends ArrayAdapter<SubscribeRow> {

	public static final String EXTRA_SENSOR_NAME = "name";
	private int resource;
	private LayoutInflater inflater;
	private Context context;
	static int TEXT_SIZE = 8;

	public SubscribeListAdapter(Context ctx, int resourceId, AndroidControllerSubscribe controller) {
		super(ctx, resourceId);
		resource = resourceId;
		inflater = LayoutInflater.from(ctx);
		context = ctx;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		if(convertView == null){
			convertView = (LinearLayout) inflater.inflate(resource, null);
	
			final SubscribeRow vs = getItem(position);
	
			TextView sensorTxt = (TextView) convertView.findViewById(R.id.subscribe_name);
			sensorTxt.setText(vs.getServerurl()+" -> "+vs.getVsname());
	
			final Switch activeStch = (Switch) convertView.findViewById(R.id.enableSubSwitch);
			activeStch.setChecked(vs.isActive());
			activeStch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					new AsyncTask<Boolean, Void, Boolean>(){
						@Override
						protected Boolean doInBackground(Boolean... params) {
							try{
								SqliteStorageManager storage = new SqliteStorageManager();
								if (params[0]){
									vs.setActive(true);
									Subscription su = storage.getSubscribeInfo(vs.getId());
									storage.setSubscribeInfo(su.getId(), su.getUrl(), su.getVsname(), su.getMode(), su.getLastTime(), su.getIterationTime(), true, su.getUsername(), su.getPassword());
									return true;
								}else{
									vs.setActive(false);
									Subscription su = storage.getSubscribeInfo(vs.getId());
									storage.setSubscribeInfo(su.getId(), su.getUrl(), su.getVsname(), su.getMode(), su.getLastTime(), su.getIterationTime(), false, su.getUsername(), su.getPassword());
									return true;
								}
							}
							catch(Exception e){
								return false;
							}
						}
						@Override
						protected void onPostExecute(Boolean result) {
							
							//activeStch.setChecked(result ^ activeStch.isChecked()); //infinite loop!
						}
					}.execute(isChecked);
					
				}
			});
	
			TextView dataTxt = (TextView) convertView.findViewById(R.id.subscribe_info);
			dataTxt.setText(vs.getInfo());
	
			ImageView view = (ImageView) convertView.findViewById(R.id.delete_subscribe);
			view.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					
				}
			});
	
			ImageView edit = (ImageView) convertView
					.findViewById(R.id.config_subscribe);
			edit.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent myIntent = new Intent(context, ActivitySubscribeData.class);
					myIntent.putExtra("tynigsn.beans.id", ""+vs.getId());
					context.startActivity(myIntent);
				}
			});
		}
		return convertView;
	}
}
