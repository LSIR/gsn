package gsn.http.rest;

import gsn.beans.DataField;
import gsn.beans.StreamElement;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.log4j.Logger;

import com.thoughtworks.xstream.XStream;

public class PushDelivery implements DeliverySystem {

	public static final String NOTIFICATION_ID_KEY = "notification-id";

	public static final String LOCAL_CONTACT_POINT = "local-contact-point";

	public static final String DATA = "data";

	private XStream xstream = StreamElement4Rest.getXstream();

	private boolean isClosed = false;

	private static transient Logger       logger     = Logger.getLogger ( PushDelivery.class );

	private HttpPut httpPut;

	private DefaultHttpClient httpclient = new DefaultHttpClient();

	private Writer writer;

	private double notificationId;

	public PushDelivery(String deliveryContactPoint,double notificaitonId, Writer writer) {
		httpPut = new HttpPut(deliveryContactPoint);
		
		this.writer = writer;
		this.notificationId = notificaitonId;
	}


	public void writeStructure(DataField[] fields) throws IOException {
		String xml = xstream.toXML(fields);
		if (writer ==null)
			throw new RuntimeException("The writer structue is null.");
		writer.write(xml);
		writer=  null;
	}

	public boolean writeStreamElement(StreamElement se) {
		String xml = xstream.toXML(new StreamElement4Rest(se));
		boolean success = sendData(xml);
//		boolean success =true;
		isClosed = !success;
		return success;
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
			ArrayList<NameValuePair> postParameters = new ArrayList <NameValuePair>();
			postParameters.add(new BasicNameValuePair(PushDelivery.NOTIFICATION_ID_KEY, Double.toString(notificationId)));
			postParameters.add(new BasicNameValuePair(PushDelivery.DATA, xml));
			
			httpPut.setEntity(new UrlEncodedFormEntity(postParameters, HTTP.UTF_8));
			
			HttpResponse response = httpclient.execute(httpPut);
			
			int statusCode = response.getStatusLine().getStatusCode();
			response.getEntity().getContent().close(); // releasing the connection to the http client's pool
			if (statusCode != RestStreamHanlder.SUCCESS_200) {
				return false;
			}
			return true;
		} catch (Exception e) {
			logger.warn(e.getMessage(),e);
			return false;
		}

	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(notificationId);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PushDelivery other = (PushDelivery) obj;
		if (Double.doubleToLongBits(notificationId) != Double
				.doubleToLongBits(other.notificationId))
			return false;
		return true;
	}

}
