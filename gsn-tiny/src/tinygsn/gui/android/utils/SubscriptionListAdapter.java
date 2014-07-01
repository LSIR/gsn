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
import tinygsn.controller.AndroidControllerListSubscription;
import tinygsn.gui.android.ActivityListSubscription;
import tinygsn.gui.android.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author Do Ngoc Hoan (hoan.do@epfl.ch)
 * 
 */
public class SubscriptionListAdapter extends ArrayAdapter<SubscriptionRow> {

	public static final String EXTRA_VS_NAME = "vs_name";
	private int resource;
	private LayoutInflater inflater;
	private Context context;
	static int TEXT_SIZE = 8;
	AndroidControllerListSubscription controller;
	ActivityListSubscription activityListVSNew;

	public SubscriptionListAdapter(Context ctx, int resourceId,
			List<SubscriptionRow> objects,
			AndroidControllerListSubscription controller,
			ActivityListSubscription activityListVSNew) {

		super(ctx, resourceId, objects);
		resource = resourceId;
		inflater = LayoutInflater.from(ctx);
		context = ctx;
		this.controller = controller;
		this.activityListVSNew = activityListVSNew;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		convertView = (LinearLayout) inflater.inflate(resource, null);

		final SubscriptionRow vs = getItem(position);

		TextView serverTxt = (TextView) convertView.findViewById(R.id.server_name);
		serverTxt.setText(vs.getServer());

		TextView vsNameTxt = (TextView) convertView.findViewById(R.id.vs_name);
		vsNameTxt.setText(vs.getVsname());

		TextView dataTxt = (TextView) convertView.findViewById(R.id.latest_values);
		dataTxt.setText(vs.getData());

		ImageButton view = (ImageButton) convertView.findViewById(R.id.view);
		view.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(context, "View has not been implemented!",
						Toast.LENGTH_SHORT).show();
			}
		});

		ImageButton unsubscribe = (ImageButton) convertView
				.findViewById(R.id.unsubscribe);
		unsubscribe.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(context, "Unsubscribe has not been implemented!",
						Toast.LENGTH_SHORT).show();
			}
		});

		ImageButton delete = (ImageButton) convertView.findViewById(R.id.delete);
		delete.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case DialogInterface.BUTTON_POSITIVE:
							//TODO delete
							
							Toast.makeText(context, vs.getVsname() + " is deleted!",
									Toast.LENGTH_SHORT).show();

							break;
						case DialogInterface.BUTTON_NEGATIVE:
							break;
						}
					}
				};

				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder
						.setMessage("Are you sure to delete \'" + vs.getVsname() + "\'?")
						.setPositiveButton("Yes", dialogClickListener)
						.setNegativeButton("No", dialogClickListener).show();
			}
		});

		return convertView;
	}
}
