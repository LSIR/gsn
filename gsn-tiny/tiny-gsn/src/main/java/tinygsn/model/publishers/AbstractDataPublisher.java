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
 * File: gsn-tiny/src/tinygsn/model/publishers/AbstractDataPublisher.java
 *
 * @author Schaer Marc
 */
package tinygsn.model.publishers;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.epfl.locationprivacy.util.Utils;

import tinygsn.beans.DeliveryRequest;
import tinygsn.beans.StaticData;
import tinygsn.controller.AndroidControllerPublish;
import tinygsn.services.PublisherService;
import tinygsn.storage.db.SqliteStorageManager;
import tinygsn.utils.Logging;
import tinygsn.utils.ToastUtils;


public abstract class AbstractDataPublisher {

	private final static String LOGTAG = "AbstractDataPublisher";
	protected AndroidControllerPublish controller;
	protected SqliteStorageManager storage;
	protected DeliveryRequest dr;

	public abstract Class<? extends PublisherService> getSERVICE();

	public AbstractDataPublisher(DeliveryRequest dr) {
		controller = new AndroidControllerPublish();
		controller.loadListVS();
		storage = new SqliteStorageManager();
		this.dr = dr;
	}

	synchronized public boolean start() {
		try {
			Integer id = dr.getId();
			Intent serviceIntent = StaticData.getRunningIntentByName("" + id);
			if (serviceIntent == null) {
				serviceIntent = new Intent(StaticData.globalContext, getSERVICE());
				dr.setActive(true);
				storage.setPublishInfo(dr.getId(), dr.getUrl(), dr.getVsname(), dr.getKey(), dr.getMode(), dr.getLastTime(), dr.getIterationTime(), dr.isActive());
				Bundle bundle = new Bundle();
				bundle.putParcelable("tinygsn.beans.dr", dr);
				serviceIntent.putExtra("tinygsn.beans.dr", bundle);
				StaticData.addRunningService("" + id, serviceIntent);
				StaticData.globalContext.startService(serviceIntent);
				return true;
			}
		} catch (Exception e) {
			// release anything?
		}
		return false;
	}

	synchronized public boolean stop() {
		try {
			Integer id = dr.getId();
			Intent serviceIntent = StaticData.getRunningIntentByName("" + id);
			if (serviceIntent != null) {
				serviceIntent.removeExtra("tinygsn.beans.dr");
				dr.setActive(false);
				storage.setPublishInfo(dr.getId(), dr.getUrl(), dr.getVsname(), dr.getKey(), dr.getMode(), dr.getLastTime(), dr.getIterationTime(), dr.isActive());
				Bundle bundle = new Bundle();
				bundle.putParcelable("tinygsn.beans.dr", dr);
				serviceIntent.putExtra("tinygsn.beans.dr", bundle);
				StaticData.globalContext.startService(serviceIntent);
				StaticData.IntentStopped("" + id);
				return true;
			}
		} catch (Exception e) {
			// release anything?
		}
		return false;
	}

	public abstract void runOnce();

	public abstract void publish(DeliveryRequest dr);

	public void save(String server, String vsname, String key, Integer mode, Long iterationTime, DeliveryRequest dr) {
		int id = -1;
		long lastTime = 0;
		boolean active = false;
		if (dr != null) {
			id = dr.getId();
			lastTime = dr.getLastTime();
			active = dr.isActive();
		}
		if (server == null | vsname == null || mode == null || iterationTime == null) {
			ToastUtils.showToastInUiThread(StaticData.globalContext, "Server, Virtual Sensor and Publish Mode must be filled in", Toast.LENGTH_SHORT);
		} else {
			storage.setPublishInfo(id, server, vsname, key, mode, lastTime, iterationTime, active);
		}
	}

	public DeliveryRequest getDr() {
		return dr;
	}

	protected static void log(Context context, String s) {
		if ((boolean) Utils.getBuildConfigValue(context, "LOGGING")) {
			Log.d(LOGTAG, s);
			Logging.createNewLoggingFolder(context, "Publish");
			Logging.appendLog("Publish", LOGTAG + ".txt", s, context);
		}
	}
}
