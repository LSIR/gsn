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
* File: gsn-tiny/src/tinygsn/controller/AndroidControllerSubscribe.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import tinygsn.beans.Subscription;
import tinygsn.storage.db.SqliteStorageManager;
import tinygsn.utils.ToastUtils;


public class AndroidControllerSubscribe extends AbstractController {

	private SqliteStorageManager storage = null;
	private HttpGet httpGet;
	private DefaultHttpClient httpclient = new DefaultHttpClient();
	
	public AndroidControllerSubscribe() {
		storage = new SqliteStorageManager();
	}

	public ArrayList<String> loadListVS(String server) {
		ArrayList<String> output = new ArrayList<String>();
		try{
			httpGet = new HttpGet(server+"/rest/sensors");
			HttpResponse response = httpclient.execute(httpGet);
			int statusCode = response.getStatusLine().getStatusCode();
			InputStreamReader is = new InputStreamReader(response.getEntity().getContent(),"UTF-8");																				
			if (statusCode == 200) {
				BufferedReader bufferedReader = new BufferedReader(is);
		        String line = bufferedReader.readLine();
		        if(line != null){
		        	JSONObject obj = new JSONObject(line);
		        	JSONArray f = obj.getJSONArray("features");
		        	for (int i = 0;i<f.length();i++){
		        		JSONObject v = f.getJSONObject(i).getJSONObject("properties");
		        		output.add(v.getString("vs_name"));
		        	}
		        }
			}
			is.close();
		}catch(Exception e){
			e.printStackTrace();
			output.add("Unable to retrieve Virtual Sensors");
		}
		return output;
	}
	
	/*	
	public boolean registerGCM(String server) {

			final String regId = GCMRegistrar.getRegistrationId(this);

			if (regId.equals("")) {
				GCMRegistrar.register(this, CommonUtilities.SENDER_ID);

			}
			else
				ServerUtilities.registerWithQuery(context, serverURL, regId, query,
						"1.1111", "bcd");
	}
	
	public boolean registerGCM() {
			GCMRegistrar.unregister(this);
		return true;
	}
*/
	public ArrayList<Subscription> loadList() {
		return storage.getSubscribeList();
	}


}