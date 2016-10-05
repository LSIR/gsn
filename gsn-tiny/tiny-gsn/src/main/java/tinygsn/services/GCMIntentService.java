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
* File: gsn-tiny/src/tinygsn/gui/android/GCMIntentService.java
*
* @author Do Ngoc Hoan
*/


/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tinygsn.services;

import tinygsn.gui.android.ActivityListSubscription;
import tinygsn.model.subscribers.utils.CommonUtilities;
import tinygsn.model.subscribers.utils.ServerUtilities;

import android.app.Activity;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import android.widget.Toast;
import tinygsn.gui.android.R;

import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
 * IntentService responsible for handling GCM messages.
 */
public class GCMIntentService extends IntentService {

	private static final String TAG = "GCMIntentService";

	public GCMIntentService() {
		super("GCMIntentService");
	}

	public void register(Context context, String registrationId) {
		Log.i(TAG, "Device registered: regId = " + registrationId);
		ServerUtilities.register(context, registrationId);
	}

	public void unregister(Context context, String registrationId) {
		Log.i(TAG, "Device unregistered");
		ServerUtilities.unregister(context, registrationId);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle extras = intent.getExtras();
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);

		String messageType = gcm.getMessageType(intent);

		if (!extras.isEmpty()) {
			if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
				sendNotification("Send error: " + extras.toString());
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
				sendNotification("Deleted messages on server: "
						+ extras.toString());
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {

				sendNotification("Message Received from Google GCM Server: "  + extras.get(CommonUtilities.EXTRA_MESSAGE));
				Log.i(TAG, "Received: " + extras.toString());
			}
		}
		GCMBroadcastReceiver.completeWakefulIntent(intent);
	}


	/**
	 * Issues a notification to inform the user that server has sent a message.
	 */
	private void sendNotification(String message) {
		int icon = R.drawable.ic_stat_gcm;
		long when = System.currentTimeMillis();
		String title = getString(R.string.app_name);
		// Intent notificationIntent = new Intent(context, DemoActivity.class);
		Intent notificationIntent = new Intent(this, ActivityListSubscription.class);

		// set intent so it does not start a new activity
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent intent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification.Builder(this).setContentTitle(title).setSmallIcon(icon).setContentText(message).setWhen(when).setContentIntent(intent).build();

		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notificationManager.notify(0, notification);
	}

	public static class GCMBroadcastReceiver extends WakefulBroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String data = intent.getExtras().getString(CommonUtilities.EXTRA_MESSAGE);
			String serverName = intent.getExtras().getString(CommonUtilities.EXTRA_SERVER_NAME);
			Toast.makeText(context, data, Toast.LENGTH_SHORT).show();
			ComponentName comp = new ComponentName(context.getPackageName(),GCMIntentService.class.getName());
			startWakefulService(context, (intent.setComponent(comp)));
			setResultCode(Activity.RESULT_OK);
		}
	}

}
