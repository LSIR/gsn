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
* File: src/gsn/http/rest/WPPushDelivery.java
*
* @author Julien Eberle
*
*/

package gsn.http.rest;

import gsn.beans.DataField;
import gsn.beans.StreamElement;

import java.io.IOException;
import java.io.Writer;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.thoughtworks.xstream.XStream;

public class WPPushDelivery implements DeliverySystem {

	public static final String NOTIFICATION_ID_KEY = "notification-id";

	public static final String LOCAL_CONTACT_POINT = "local-contact-point";

	public static final String DATA = "data";
	
	private int notificationClass = 3;
	
	private String notificationMessage = "";

	private XStream xstream = StreamElement4Rest.getXstream();

	private boolean isClosed = false;

	private static transient Logger       logger     = LoggerFactory.getLogger ( WPPushDelivery.class );

	private HttpPost httpPost;

	private DefaultHttpClient httpclient = new DefaultHttpClient();

	private Writer writer;
	
	private String lastmessage = "";
	
	private long lastTimeSent = 0;

	private double notificationId;

	public WPPushDelivery(String deliveryContactPoint,double notificaitonId, Writer writer,int notificationClass,String notificationMessage) {
		httpPost = new HttpPost(deliveryContactPoint);
		this.notificationClass = notificationClass;
		this.writer = writer;
		this.notificationId = notificaitonId;
		this.notificationMessage = notificationMessage;
	}


	public void writeStructure(DataField[] fields) throws IOException {
		String xml = xstream.toXML(fields);
		if (writer ==null)
			throw new RuntimeException("The writer structure is null.");
		writer.write(xml);
		writer=  null;
	}

	public boolean writeStreamElement(StreamElement se) {
		String xml = xstream.toXML(new StreamElement4Rest(se)); //raw notification
		
		
		if (notificationClass == 2) {     //toast notification
			httpPost.setHeader("X-WindowsPhone-Target", "toast");
			xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
	                "<wp:Notification xmlns:wp=\"WPNotification\">" +
	                   "<wp:Toast>" +
	                        "<wp:Text1>MobileObservatory</wp:Text1>" +
	                        "<wp:Text2>"+ notificationMessage +"</wp:Text2>" +
	                        "<wp:Param></wp:Param>" +
	                   "</wp:Toast> " +
	                "</wp:Notification>";
		}
		if (notificationClass == 1) {   //tile notification
			String compareText = "Air Quality is in the average for this city.";
			if((Integer)se.getData("MAX_REL")>3){
				compareText = "Air Quality is worst than the average for this city.";
			}else if((Integer)se.getData("MAX_REL")<3){
				compareText = "Air Quality is better than the average for this city.";
			}
			httpPost.setHeader("X-WindowsPhone-Target", "token"); 
			xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
	                  "<wp:Notification xmlns:wp=\"WPNotification\">" +
	                      "<wp:Tile ID=\"/ZurichSummary.xaml?Place=" + notificationMessage +"\">" +
	                        "<wp:BackgroundImage>/Images/0" + se.getData("MAX_ABS") + ".png</wp:BackgroundImage>" +
	                        "<wp:Count></wp:Count>" +
	                        "<wp:Title>" + notificationMessage+ "</wp:Title>" +
	                        "<wp:BackBackgroundImage>/Images/0" + se.getData("MAX_ABS") + "_visual.png</wp:BackBackgroundImage>" +
	                        "<wp:BackTitle>" + notificationMessage+ "</wp:BackTitle>" +
	                        "<wp:BackContent></wp:BackContent>" +
	                     "</wp:Tile> " +
	                  "</wp:Notification>";
		}
		
		if (xml.equalsIgnoreCase(lastmessage)){ // don't send twice the same message
			return true;
		}
		if (se.getTimeStamp() < lastTimeSent + 60*15){
			return true;
		}
		lastmessage = xml;
		lastTimeSent = se.getTimeStamp();
		boolean success = sendData(xml);
//		boolean success =true;
		isClosed = !success;
		return success;
	}

    public boolean writeKeepAliveStreamElement() {
        return true;
    }

    public void close() {
		httpclient.getConnectionManager().shutdown();
		isClosed = true;
	}

	public boolean isClosed() {
		return isClosed  ;
	}

	private boolean sendData(String xml) {
		try {
			httpPost.setHeader("X-NotificationClass",""+notificationClass);
			httpPost.setEntity(new StringEntity(xml, HTTP.UTF_8));
			HttpResponse response = httpclient.execute(httpPost);
			int statusCode = response.getStatusLine().getStatusCode();
			String nStatus = "NA";
			String dStatus = "NA";
			String sStatus = "NA";
			response.getEntity().getContent().close(); // releasing the connection to the http client's pool
			if (response.containsHeader("X-NotificationStatus")) // TODO: manage "QueueFull" status according to Microsoft's recommendations
				nStatus = response.getFirstHeader("X-NotificationStatus").getValue();
			if (response.containsHeader("X-DeviceConnectionStatus"))
				dStatus = response.getFirstHeader("X-DeviceConnectionStatus").getValue();
			if (response.containsHeader("X-SubscriptionStatus"))
				sStatus = response.getFirstHeader("X-SubscriptionStatus").getValue();
			logger.warn("Status for client "+notificationId+":(" +statusCode+")" + nStatus + ", " + dStatus + "," +sStatus);
			if (statusCode != RestStreamHanlder.SUCCESS_200) {
				return false;
			}
			if (nStatus.equalsIgnoreCase("QueueFull")){
				lastTimeSent = System.currentTimeMillis()/1000 + 60*60*2;
			}
			if (nStatus.equalsIgnoreCase("Suppressed")){
				return false;
			}
			return true;
		} catch (Exception e) {
			logger.warn(e.getMessage(),e);
			return false;
		}

	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WPPushDelivery that = (WPPushDelivery) o;
        if (Double.compare(that.notificationId, notificationId) != 0) return false;
        if (httpPost != null ? !httpPost.getURI().equals(that.httpPost.getURI()) : that.httpPost != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = httpPost != null ? httpPost.getURI().hashCode() : 0;
        temp = notificationId != +0.0d ? Double.doubleToLongBits(notificationId) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
