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
* File: gsn-tiny/src/tinygsn/controller/AndroidControllerPullData.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import tinygsn.beans.StreamElement;
import tinygsn.gui.android.ActivityPullData;
import tinygsn.storage.StorageManager;
import tinygsn.storage.db.SqliteStorageManager;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class AndroidControllerPullData extends AbstractController {
	private static final String TAG = "AndroidControllerPullData";

	private ActivityPullData view = null;
	private Handler handlerVS = null;
	private Handler handlerData = null;

	// private ArrayList<VirtualSensor> vsList = new ArrayList<VirtualSensor>();
	private SqliteStorageManager storage = null;

	public AndroidControllerPullData(ActivityPullData androidViewer) {
		this.view = androidViewer;
		storage = new SqliteStorageManager(view);
	}

	public void loadListVS(String server) {
		new GetVSList().execute(server);
	}

	private class GetVSList extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			String server = params[0];

			HttpClient httpClient = new DefaultHttpClient();
			HttpContext localContext = new BasicHttpContext();
			HttpGet httpGet = new HttpGet(server + "/gsn");
			Log.i(TAG, server + "/gsn");

			String text = "";
			try {
				HttpResponse response = httpClient.execute(httpGet, localContext);
				HttpEntity entity = response.getEntity();
				text = getASCIIContentFromEntity(entity);
				Log.i(TAG, text);
			}
			catch (Exception e) {
				Log.e(TAG, e.toString());
			}

			Log.v(TAG, text);
			return text;
		}

		protected void onPostExecute(String results) {
			try {
				Document doc = stringToDocument(results);
				ArrayList<String> vsNameList = new ArrayList<String>();
				doc.getDocumentElement().normalize();
				NodeList nList = doc.getElementsByTagName("virtual-sensor");
				for (int i = 0; i < nList.getLength(); i++) {
					Node nNode = nList.item(i);
					if (nNode.getNodeType() == Node.ELEMENT_NODE) {
						Element eElement = (Element) nNode;
						vsNameList.add(eElement.getAttribute("name"));
					}
				}

				Message msg = new Message();
				msg.obj = vsNameList;
				handlerVS.sendMessage(msg);
			}
			catch (SAXException e) {
				e.printStackTrace();
			}
			catch (ParserConfigurationException e) {
				e.printStackTrace();
			}
			catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	public static String getASCIIContentFromEntity(HttpEntity entity)
			throws IllegalStateException, IOException {
		InputStream in = entity.getContent();
		StringBuffer out = new StringBuffer();
		int n = 1;
		while (n > 0) {
			byte[] b = new byte[4096];
			n = in.read(b);
			if (n > 0)
				out.append(new String(b, 0, n));
		}
		return out.toString();
	}

	public static Document stringToDocument(final String xmlSource)
			throws SAXException, ParserConfigurationException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();

		return builder.parse(new InputSource(new StringReader(xmlSource)));
	}

	public Handler getHandlerVS() {
		return handlerVS;
	}

	public void setHandlerVS(Handler handlerVS) {
		this.handlerVS = handlerVS;
	}

	public Handler getHandlerData() {
		return handlerData;
	}

	public void setHandlerData(Handler handlerData) {
		this.handlerData = handlerData;
	}


	public void consume(StreamElement streamElement) {
	}

	@Override
	public Activity getActivity() {
		return view;
	}

	@Override
	public StorageManager getStorageManager() {
		return null;
	}

	public void pullLatestData(String serverName, String vsName, String numLatest) {
		new GetVSDataLatest()
				.execute(new String[] { serverName, vsName, numLatest });
	}

	private class GetVSDataLatest extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... params) {
			String server = params[0];
			String vsName = params[1];
			String window = params[2];
			String query = server + "/gsn?REQUEST=114&name=" + vsName + "&window="
					+ window;

			HttpClient httpClient = new DefaultHttpClient();
			HttpContext localContext = new BasicHttpContext();
			HttpGet httpGet = new HttpGet(query);
			Log.i(TAG, query);

			String text = "";
			try {
				HttpResponse response = httpClient.execute(httpGet, localContext);
				HttpEntity entity = response.getEntity();
				text = getASCIIContentFromEntity(entity);
			}
			catch (Exception e) {
				Log.e(TAG, e.toString());
			}

			Log.v(TAG, text);
			return text;
		}

		protected void onPostExecute(String results) {
			Message msg = new Message();
			msg.obj = results;
			handlerData.sendMessage(msg);
		}
	}

}