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
* File: gsn-tiny/src/tinygsn/controller/AndroidControllerListSubscription.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import org.kroz.activerecord.ActiveRecordBase;
import org.kroz.activerecord.ActiveRecordException;
import org.kroz.activerecord.Database;
import org.kroz.activerecord.DatabaseBuilder;
import org.kroz.activerecord.utils.Logg;
import tinygsn.beans.StreamElement;
import tinygsn.gui.android.ActivityListSubscription;
import tinygsn.gui.android.utils.SubscriptionRow;
import tinygsn.storage.StorageManager;
import tinygsn.storage.db.SqliteStorageManager;
import tinygsn.utils.Const;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class AndroidControllerListSubscription extends AbstractController {

	private ActivityListSubscription view = null;

	private Handler handlerData = null;

	private ArrayList<SubscriptionRow> subsDataList = new ArrayList<SubscriptionRow>();

	private static final String TAG = "AndroidControllerListSubscription";

	private ActiveRecordBase _db;
	private int numLoaded = 0;

	public AndroidControllerListSubscription(
			ActivityListSubscription androidViewer) {
		this.view = androidViewer;

		initDb();
		createSampleData();
	}

	public void consume(StreamElement streamElement) {
	}

	public Handler getHandlerData() {
		return handlerData;
	}

	public void setHandlerData(Handler handlerData) {
		this.handlerData = handlerData;
	}

	public StorageManager getStorageManager() {
		SqliteStorageManager storage = new SqliteStorageManager(view);
		return storage;
	}

	@Override
	public Activity getActivity() {
		return view;
	}

	private void initDb() {

		try {
			// --------- Prepare mDatabase connection ----------
			DatabaseBuilder builder = new DatabaseBuilder(Const.DATABASE_NAME);
			builder.addClass(SubscriptionRow.class);
			Database.setBuilder(builder);

			// Open database
			_db = ActiveRecordBase.open(view, Const.DATABASE_NAME,
					Const.DATABASE_VERSION);
		}
		catch (ActiveRecordException e) {
			Logg.e(TAG, e, "(%t) %s.initDb(): Error=%s", TAG, e.getMessage());
		}
	}

	public void closeDB() {
		if (null != _db) {
			_db.close();
		}
	}

	public void createSampleData() {
		for (int i = 0; i < 2; i++) {
			try {
				SubscriptionRow r = _db.newEntity(SubscriptionRow.class);

				r.setServer("http://10.0.2.2:22001");
				r.setVsname("temperature");
				r.setData("timed: 10/11/2013\n\ttemp: " + (new Random().nextInt(100)));
				r.setRead("0");

				r.save();
				Log.v(TAG, "Create row ok");
			}
			catch (ActiveRecordException e) {
				e.printStackTrace();
			}
		}
	}

	public void saveNewSubscriptionData(String server, String data) {

		try {
			SubscriptionRow r = _db.newEntity(SubscriptionRow.class);

			r.setServer(server);
			r.setVsname("multiFormatSample");
			r.setData("timed: " + (new Date()).toString() + "\n\t " + (data));
			r.setRead("0");

			r.save();

			Log.v(TAG, "Insert new subscription ok!");
		}
		catch (ActiveRecordException e) {
			e.printStackTrace();
		}

	}

	public void loadListSubsData() {
		try {
			subsDataList = (ArrayList<SubscriptionRow>) _db.findByColumn(
					SubscriptionRow.class, "read", "0");

			for (SubscriptionRow r : subsDataList) {
				numLoaded++;
			}
		}
		catch (ActiveRecordException e) {
			e.printStackTrace();
		}

		Message msg = new Message();
		msg.obj = subsDataList;
		handlerData.sendMessage(msg);
	}

	public void markDataUnreadToRead() {
		try {
			subsDataList = (ArrayList<SubscriptionRow>) _db.findByColumn(
					SubscriptionRow.class, "read", "0");

			for (SubscriptionRow r : subsDataList) {
				r.setRead("1");
				r.save();
				numLoaded++;
			}
		}
		catch (ActiveRecordException e) {
			e.printStackTrace();
		}
	}

	public void deleteAll() {
		try {
			subsDataList = (ArrayList<SubscriptionRow>) _db
					.findAll(SubscriptionRow.class);

			for (SubscriptionRow r : subsDataList) {
				boolean delete = false;
				
				if (r.getVsname().equals("temperature"))
					delete = true;
				if (r.getData().contains("Trying"))
					delete = true;
				if (r.getData().contains("Demo"))
					delete = true;
				
				if (delete == true)
					r.delete();
			}
		}
		catch (ActiveRecordException e) {
			e.printStackTrace();
		}
	}

	public void loadMore() {
		try {
			numLoaded += 3;

			subsDataList = (ArrayList<SubscriptionRow>) _db
					.rawQuery(
							SubscriptionRow.class,
							"SELECT * FROM Subscription_Row WHERE _id > (Select max(_id) as maxid from Subscription_Row) - "+numLoaded);

		}
		catch (ActiveRecordException e) {
			e.printStackTrace();
		}

		Message msg = new Message();
		msg.obj = subsDataList;
		handlerData.sendMessage(msg);
	}
}