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


import tinygsn.controller.AndroidControllerPublish;
import tinygsn.gui.android.ActivityPublishData;
import tinygsn.gui.android.R;
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



public class PublishListAdapter extends ArrayAdapter<PublishRow> {

	public static final String EXTRA_SENSOR_NAME = "name";
	private int resource;
	private LayoutInflater inflater;
	private Context context;
	static int TEXT_SIZE = 8;

	public PublishListAdapter(Context ctx, int resourceId, AndroidControllerPublish controller) {
		super(ctx, resourceId);
		resource = resourceId;
		inflater = LayoutInflater.from(ctx);
		context = ctx;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		if(convertView == null){
			convertView = (LinearLayout) inflater.inflate(resource, null);
	
			final PublishRow vs = getItem(position);
	
			TextView sensorTxt = (TextView) convertView.findViewById(R.id.publish_name);
			sensorTxt.setText(vs.getVsname()+" -> "+vs.getServerurl());
	
			final Switch activeStch = (Switch) convertView.findViewById(R.id.enablePSwitch);
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
									//start it
									return true;
								}else{
									vs.setActive(false);
									//stop it
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
	
			TextView dataTxt = (TextView) convertView.findViewById(R.id.publish_info);
			dataTxt.setText(vs.getInfo());
	
			ImageView view = (ImageView) convertView.findViewById(R.id.delete);
			view.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					
				}
			});
	
			ImageView edit = (ImageView) convertView
					.findViewById(R.id.config_publish);
			edit.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent myIntent = new Intent(context, ActivityPublishData.class);
					myIntent.putExtra("tynigsn.beans.id", ""+vs.getId());
					context.startActivity(myIntent);
				}
			});
		}
		return convertView;
	}
}
