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

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.epfl.locationprivacy.util.Utils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;

import gsn.http.rest.PushDelivery;
import tinygsn.beans.DeliveryRequest;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.beans.Subscription;
import tinygsn.model.publishers.AbstractDataPublisher;
import tinygsn.storage.db.SqliteStorageManager;
import tinygsn.utils.Logging;

public class RetrieveDataTask extends AsyncTask<String, Void, ArrayList<StreamElement>> {

	private static final String LOGTAG = "SubDataTask";
	private Subscription su;
    private DefaultHttpClient httpclient = new DefaultHttpClient();

	public RetrieveDataTask(Subscription su) {
		this.su = su;
	}

	private String apiUrl = "";

	@Override
	protected ArrayList<StreamElement> doInBackground(String ... params) {
		apiUrl = params[0];
        ArrayList<StreamElement> se = new ArrayList<>();
        try{
            HttpGet httpGet = new HttpGet(apiUrl);
            HttpResponse response = httpclient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            InputStreamReader is = new InputStreamReader(response.getEntity().getContent(),"UTF-8");
            if (statusCode == 200) {
                BufferedReader bufferedReader = new BufferedReader(is);
                String line = bufferedReader.readLine();
                if(line != null){
                    JSONObject obj = new JSONObject(line);
                    JSONArray f = obj.getJSONArray("features");
                    for (int i = 1;i<f.length();i++){
                        JSONObject v = f.getJSONObject(i).getJSONObject("properties");
                        se.add(new StreamElement(new String[]{},new Byte[]{},new Serializable[]{}));
                    }
                }
            }
            is.close();
        }catch(Exception e){
            e.printStackTrace();
        }
		return se;
	}

	protected void onPostExecute(ArrayList<StreamElement> results) {
			log(StaticData.globalContext, "Retrieved: " + results.size());
			su.setLastTime(System.currentTimeMillis());
            SqliteStorageManager storage = new SqliteStorageManager();
			storage.setSubscribeInfo(su.getId(), su.getUrl(), su.getVsname(), su.getMode(), su.getLastTime(), su.getIterationTime(), su.isActive());
	}

	protected static void log(Context context, String s) {
		if ((boolean) Utils.getBuildConfigValue(context, "LOGGING")) {
			Log.d(LOGTAG, s);
			Logging.createNewLoggingFolder(context, "Publish");
			Logging.appendLog("Publish", LOGTAG + ".txt", s, context);
		}
	}
}
