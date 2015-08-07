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

import java.util.ArrayList;
import java.util.List;

import tinygsn.beans.StaticData;
import tinygsn.controller.AndroidControllerListSubscription;
import tinygsn.controller.AndroidControllerVS;
import tinygsn.controller.AndroidControllerWrapper;
import tinygsn.gui.android.ActivityListSensor;
import tinygsn.gui.android.ActivityListSubscription;
import tinygsn.gui.android.R;
import tinygsn.gui.android.R.color;
import tinygsn.model.vsensor.AbstractVirtualSensor;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
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
import android.widget.Toast;


public class SensorListAdapter extends ArrayAdapter<SensorRow> {

	public static final String EXTRA_VS_NAME = "vs_name";
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

		convertView = (LinearLayout) inflater.inflate(resource, null);

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
				Toast.makeText(context, "View has not been implemented!",
						Toast.LENGTH_SHORT).show();
			}
		});

		ImageButton edit = (ImageButton) convertView
				.findViewById(R.id.edit_wrapper);
		edit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(context, "Configure has not been implemented!",
						Toast.LENGTH_SHORT).show();
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
}
