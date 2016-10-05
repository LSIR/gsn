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
 * File: gsn-tiny/src/tinygsn/gui/android/utils/SubscriptionListAdapter.java
 *
 * @author Do Ngoc Hoan and Schaer Marc
 */


package tinygsn.gui.android.utils;


import tinygsn.beans.DeliveryRequest;
import tinygsn.gui.android.ActivityListPublish;
import tinygsn.gui.android.ActivityPublishData;
import tinygsn.gui.android.R;
import tinygsn.storage.db.SqliteStorageManager;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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

	private int resource;
	private LayoutInflater inflater;
	private Context context;
	private ActivityListPublish listPublish;

	public PublishListAdapter(Context ctx, int resourceId, ActivityListPublish listPublish) {
		super(ctx, resourceId);
		resource = resourceId;
		inflater = LayoutInflater.from(ctx);
		context = ctx;
		this.listPublish = listPublish;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		if (convertView == null) {
			convertView = (LinearLayout) inflater.inflate(resource, null);

			final PublishRow pr = getItem(position);

			TextView sensorTxt = (TextView) convertView.findViewById(R.id.publish_name);
			sensorTxt.setText(pr.getVsname() + " -> " + pr.getServerurl());

			final Switch activeStch = (Switch) convertView.findViewById(R.id.enablePSwitch);
			activeStch.setChecked(pr.isActive());
			activeStch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					new AsyncTask<Boolean, Void, Boolean>() {
						@Override
						protected Boolean doInBackground(Boolean... params) {
							try {
								SqliteStorageManager storage = new SqliteStorageManager();
								if (params[0]) {
                                    pr.setActive(true);
									DeliveryRequest dr = storage.getPublishInfo(pr.getId());
									storage.setPublishInfo(dr.getId(), dr.getUrl(), dr.getVsname(), dr.getClientID(), dr.getClientSecret(), dr.getMode(), dr.getLastTime(), dr.getIterationTime(), true);
									return true;
								} else {
									pr.setActive(false);
                                    DeliveryRequest dr = storage.getPublishInfo(pr.getId());
                                    storage.setPublishInfo(dr.getId(), dr.getUrl(), dr.getVsname(), dr.getClientID(), dr.getClientSecret(), dr.getMode(), dr.getLastTime(), dr.getIterationTime(), false);
									return true;
								}
							} catch (Exception e) {
								return false;
							}
						}

						@Override
						protected void onPostExecute(Boolean result) {

						}
					}.execute(isChecked);

				}
			});

			TextView dataTxt = (TextView) convertView.findViewById(R.id.publish_info);
			dataTxt.setText(pr.getInfo());

			ImageView view = (ImageView) convertView.findViewById(R.id.delete);
			view.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
								case DialogInterface.BUTTON_POSITIVE:
									new SqliteStorageManager().deletePublishInfo(pr.getId());
									listPublish.initialize();
									break;
								case DialogInterface.BUTTON_NEGATIVE:
									break;
							}
						}
					};

					AlertDialog.Builder builder = new AlertDialog.Builder(context);
					builder.setMessage("Are you sure you want to delete \'" + pr.getVsname() + "\' publisher?")
						.setPositiveButton("Yes", dialogClickListener)
						.setNegativeButton("No", dialogClickListener).show();
				}
			});

			ImageView edit = (ImageView) convertView
				.findViewById(R.id.config_publish);
			edit.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent myIntent = new Intent(context, ActivityPublishData.class);
					myIntent.putExtra("tynigsn.beans.id", "" + pr.getId());
					context.startActivity(myIntent);
				}
			});
		}
		return convertView;
	}
}
