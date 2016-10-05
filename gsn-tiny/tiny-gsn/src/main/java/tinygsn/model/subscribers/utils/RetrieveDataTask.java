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
package tinygsn.model.subscribers.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.epfl.locationprivacy.util.Utils;
import java.lang.Exception;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.beans.Subscription;
import tinygsn.model.utils.Oauth2Connection;
import tinygsn.storage.db.SqliteStorageManager;
import tinygsn.utils.Logging;

public class RetrieveDataTask extends AsyncTask<String, Void, List<StreamElement>> {

	private static final String LOGTAG = "SubDataTask";
	private Subscription su;
    private Oauth2Connection connection;

	public RetrieveDataTask(Subscription su) {
		this.su = su;
	}

	private String apiUrl = "";

	@Override
	protected List<StreamElement> doInBackground(String ... params) {
		apiUrl = params[0];
        List<StreamElement> se;
        try{
            connection = new Oauth2Connection(apiUrl, su.getUsername(), su.getPassword());
            connection.authenticate();
            StreamElement[] ses = StreamElement.fromJSON(connection.doJsonRequest("GET", "/api/sensors/"+su.getVsname()+"/" ,""));
            se = Arrays.asList(ses);
        }catch(Exception e){
            e.printStackTrace();
            se = null;
        }
		return se;
	}

    protected void onPostExecute(ArrayList<StreamElement> results) {
        if (results != null) {
            log(StaticData.globalContext, "Retrieved: " + results.size());
            try {
                for (StreamElement s : results) {
                    StaticData.getWrapperByName("tinygsn.model.wrappers.RemoteWrapper?" + su.getId()).postStreamElement(s);
                }
                su.setLastTime(System.currentTimeMillis());
                SqliteStorageManager storage = new SqliteStorageManager();
                storage.setSubscribeInfo(su.getId(), su.getUrl(), su.getVsname(), su.getMode(), su.getLastTime(), su.getIterationTime(), su.isActive(), su.getUsername(), su.getPassword());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            log(StaticData.globalContext, "Error retrieving data from remote GSN.");
        }
	}

	protected static void log(Context context, String s) {
		/*if ((boolean) Utils.getBuildConfigValue(context, "LOGGING")) {
			Log.d(LOGTAG, s);
			Logging.createNewLoggingFolder(context, "Publish");
			Logging.appendLog("Publish", LOGTAG + ".txt", s, context);
		}*/
	}
}
