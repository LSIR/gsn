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

import java.io.IOException;
import java.util.List;

import tinygsn.beans.DeliveryRequest;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.model.utils.Oauth2Connection;
import tinygsn.storage.db.SqliteStorageManager;
import tinygsn.utils.Logging;

public class PublishDataTask extends AsyncTask<List<StreamElement>, Void, Boolean> {

	private static final String LOGTAG = "PublishDataTask";
	private Oauth2Connection connection;
	private DeliveryRequest dr;

	public PublishDataTask(Oauth2Connection c, DeliveryRequest dr) {
		this.connection = c;
		this.dr = dr;
	}

	private List<StreamElement> se = null;

	@Override
	protected Boolean doInBackground(List<StreamElement>... params) {
		se = params[0];
		try {
			connection.authenticate();
			connection.doJsonRequest("POST", "/api/sensors/"+dr.getVsname().toLowerCase()+"/data", StreamElement.toJSON("tiny-gsn", se.toArray(new StreamElement[se.size()])));
		}catch (IOException e){
			log(StaticData.globalContext, "Error in publishing data: " + e.getMessage());
			return false;
		}
		return true;
	}

	protected void onPostExecute(Boolean results) {
		if (results) {
			log(StaticData.globalContext, "Published: " + se.size());
			dr.setLastTime(System.currentTimeMillis());
            SqliteStorageManager storage = new SqliteStorageManager();
			storage.setPublishInfo(dr.getId(), dr.getUrl(), dr.getVsname(), dr.getClientID(), dr.getClientSecret(), dr.getMode(), dr.getLastTime(), dr.getIterationTime(), dr.isActive());
		} else {
			log(StaticData.globalContext, "Publish fail: " + se.size());
		}
	}

	protected static void log(Context context, String s) {
/*		if ((boolean) Utils.getBuildConfigValue(context, "LOGGING")) {
			Log.d(LOGTAG, s);
			Logging.createNewLoggingFolder(context, "Publish");
			Logging.appendLog("Publish", LOGTAG + ".txt", s, context);
		}*/
	}
}
