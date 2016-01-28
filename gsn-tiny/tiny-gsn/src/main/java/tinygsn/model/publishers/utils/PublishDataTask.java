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
 * File: gsn-tiny/src/tinygsn/model/publishers/utils/PublishDataTask.java
 *
 * @author Schaer Marc
 */
package tinygsn.model.publishers.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.epfl.locationprivacy.util.Utils;

import gsn.http.rest.PushDelivery;
import tinygsn.beans.DeliveryRequest;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.model.publishers.AbstractDataPublisher;
import tinygsn.storage.db.SqliteStorageManager;
import tinygsn.utils.Logging;

public class PublishDataTask extends AsyncTask<StreamElement[], Void, Boolean> {

	private static final String LOGTAG = "PublichDataTask";
	private PushDelivery push;
	private AbstractDataPublisher publisher;
	private DeliveryRequest dr;

	public PublishDataTask(PushDelivery p, AbstractDataPublisher publisher, DeliveryRequest dr) {
		this.push = p;
		this.publisher = publisher;
		this.dr = dr;
	}

	private StreamElement[] se = null;

	@Override
	protected Boolean doInBackground(StreamElement[]... params) {
		se = params[0];
		return push.writeStreamElements(se);
	}

	protected void onPostExecute(Boolean results) {
		if (results == true) {
			log(StaticData.globalContext, "Published: " + se.length);
			dr.setLastTime(System.currentTimeMillis());
            SqliteStorageManager storage = new SqliteStorageManager();
			storage.setPublishInfo(dr.getId(), dr.getUrl(), dr.getVsname(), dr.getKey(), dr.getMode(), dr.getLastTime(), dr.getIterationTime(), dr.isActive());
		} else {
			log(StaticData.globalContext, "Publish fail: " + se.length);
		}
	}

	protected static void log(Context context, String s) {
		if ((boolean) Utils.getBuildConfigValue(context, "LOGGING")) {
			Log.d(LOGTAG, s);
			Logging.createNewLoggingFolder(context, "Publish");
			Logging.appendLog("Publish", LOGTAG + ".txt", s, context);
		}
	}
}
