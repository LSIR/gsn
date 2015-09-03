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
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: src/gsn/http/rest/AndroidPushDelivery.java
*
* @author Do Ngoc Hoan
*
*/


package gsn.http.rest;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.http.rest.gcm.Datastore;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.thoughtworks.xstream.XStream;

/**
 * 
 * @author Do Ngoc Hoan (ngochoan1011@gmail.com)
 * 
 */
public class AndroidPushDelivery implements DeliverySystem {

	// private int DELAY = 60 * 15;
	// private int notificationClass = 3;
	// private String regId = "";
	// private String lastmessage = "";
	// private long lastTimeSent = 0;

	private XStream xstream = StreamElement4Rest.getXstream();

	private boolean isClosed = false;

	private static transient Logger logger = LoggerFactory.getLogger(AndroidPushDelivery.class);

	private HttpPost httpPost;

	private DefaultHttpClient httpclient = new DefaultHttpClient();

	private Writer writer;

	private double notificationId;

	private Sender sender;

	// API key obtained from Google
	//AIzaSyAgXOfaoNVeWxH1MxsigA884gzSGEBKdg4
	//AIzaSyA2LXgP6RX1Wu2bernBGdHanaT57v2YkEo
	private static final String API_KEY = "AIzaSyA2LXgP6RX1Wu2bernBGdHanaT57v2YkEo";

	/**
	 * Builds a new Android Phone Push Delivery class
	 * 
	 * @param deliveryContactPoint
	 *          : the unique url for Windows phone push service
	 * @param notificaitonId
	 *          : the id for identifying the phone on GSN
	 * @param writer
	 *          : where to write back the output structure
	 * @param notificationClass
	 *          : 1: tile notification, 2: toast notification, 3:raw notification
	 * @param regId
	 *          :
	 */
	public AndroidPushDelivery(String deliveryContactPoint,
			double notificaitonId, Writer writer, int notificationClass, String regId) {
		httpPost = new HttpPost(deliveryContactPoint);
		this.notificationId = notificaitonId;
		this.writer = writer;
		// this.notificationClass = notificationClass;
		// this.regId = regId;

		Datastore.register(regId);

		sender = new Sender(API_KEY);

		logger.warn("AndroidPushDelivery: regId=" + regId + " is registered!");
	}

	/**
	 * Write the structure of the data as xml in the writer given by the
	 * constructor. Can be useful in case of raw notification only.
	 * 
	 * @param fields
	 *          the field structure to write.
	 * @throws IOException
	 *           in case the writer fails.
	 */
	public void writeStructure(DataField[] fields) throws IOException {
		String xml = xstream.toXML(fields);
		if (writer == null)
			throw new RuntimeException("The writer structure is null.");
		writer.write(xml);
		writer = null;

		logger.warn("AndroidPushDelivery: writeStructure");
	}

	/**
	 * Generate the content of the notification according to the notification
	 * class and sends it to the phone. The content of the streamElement can be
	 * used in different ways and this method should be adapted to the specific
	 * application.
	 * 
	 * @param se
	 *          the streamElement
	 */
	public boolean writeStreamElement(StreamElement se) {
		String xml = xstream.toXML(new StreamElement4Rest(se));

		logger.warn("AndroidPushDelivery->writeStreamElement: sending... \nxml="
				+ xml);

		List<String> devices = Datastore.getDevices();
		String registrationId = devices.get(0);
		Message message = new Message.Builder().collapseKey("key")
				.timeToLive(1000).delayWhileIdle(true)
				.addData("source", "gsn-server")
				.addData("data", xml).build();
		
		try {
			Result result = sender.send(message, registrationId, 5);

			logger.warn("AndroidPushDelivery: sent message=" + message.toString()
					+ " to the device with registrationId=" + registrationId
					+ " via GCM, result=" + result);
		}
		catch (IOException e) {
			logger.error("AndroidPushDelivery:Error: result=" + e.toString());
			e.printStackTrace();
		}

		return true;
	}

	/**
	 * No need to keep the connection alive
	 */
	public boolean writeKeepAliveStreamElement() {
		logger.warn("AndroidPushDelivery: writeKeepAliveStreamElement");
		return true;
	}

	/**
	 * closing all connections
	 */
	public void close() {
		logger.warn("AndroidPushDelivery: close");
		httpclient.getConnectionManager().shutdown();
		isClosed = true;
	}

	public boolean isClosed() {
		return isClosed;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		AndroidPushDelivery that = (AndroidPushDelivery) o;
		if (Double.compare(that.notificationId, notificationId) != 0)
			return false;
		if (httpPost != null ? !httpPost.getURI().equals(that.httpPost.getURI())
				: that.httpPost != null)
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		result = httpPost != null ? httpPost.getURI().hashCode() : 0;
		temp = notificationId != +0.0d ? Double.doubleToLongBits(notificationId)
				: 0L;
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
}
