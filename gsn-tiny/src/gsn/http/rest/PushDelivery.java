package gsn.http.rest;

import java.util.ArrayList;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import tinygsn.beans.DataField;
import tinygsn.beans.StreamElement;
import android.util.Log;
import com.thoughtworks.xstream.XStream;

/**
 * This class takes care of publishing data to GSN server.
 * 
 */
public class PushDelivery {

	public static final String NOTIFICATION_ID_KEY = "notification-id";

	public static final String LOCAL_CONTACT_POINT = "local-contact-point";

	public static final String DATA = "data";

	private XStream xstream = StreamElement4Rest.getXstream();

	private boolean isClosed = false;

	private HttpPut httpPut;
	private HttpPost httpPost;

	private DefaultHttpClient httpclient = new DefaultHttpClient();

	private double notificationId;

	public PushDelivery(String deliveryContactPoint, double notificaitonId) {
		httpPut = new HttpPut(deliveryContactPoint);
		httpPost = new HttpPost(deliveryContactPoint);
		this.notificationId = notificaitonId;
	}

	public boolean writeStructure(DataField[] fields) {
		String xml = xstream.toXML(fields);
		boolean success = sendStructure(xml);
		return success;
	}

	/**
	 * Send a StreamElement to the server using deliveryContactPoint and
	 * notificaitonId
	 * 
	 * @param se
	 *          the StreamElement to be sent
	 * @return a boolean value indicating if successful
	 */
	public boolean writeStreamElement(StreamElement se) {
		String xml = xstream.toXML(new StreamElement4Rest(se));
		int success = sendData(xml);
		isClosed = success == 200;
		return isClosed;
	}

	public boolean writeKeepAliveStreamElement() {
		return true;
	}

	public void close() {
		httpclient.getConnectionManager().shutdown();
		isClosed = true;
	}

	public boolean isClosed() {
		return isClosed;
	}

	private boolean sendStructure(String xml) {
		try {
			ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
			postParameters.add(new BasicNameValuePair(
					PushDelivery.NOTIFICATION_ID_KEY, Double.toString(notificationId)));
			postParameters.add(new BasicNameValuePair(PushDelivery.DATA, xml));

			httpPost.setEntity(new UrlEncodedFormEntity(postParameters, HTTP.UTF_8));

			HttpResponse response = httpclient.execute(httpPost);

			int statusCode = response.getStatusLine().getStatusCode();
			response.getEntity().getContent().close(); // releasing the connection to
																									// the http client's pool
			if (statusCode != 200) {
				return false;
			}
			return true;
		}
		catch (Exception e) {
			Log.d("Opensense", "exception: " + e);
			return false;
		}
	}

	private int sendData(String xml) {
		try {
			ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
			postParameters.add(new BasicNameValuePair(
					PushDelivery.NOTIFICATION_ID_KEY, Double.toString(notificationId)));
			postParameters.add(new BasicNameValuePair(PushDelivery.DATA, xml));

			httpPut.setEntity(new UrlEncodedFormEntity(postParameters, HTTP.UTF_8));

			HttpResponse response = httpclient.execute(httpPut);

			int statusCode = response.getStatusLine().getStatusCode();
			response.getEntity().getContent().close(); // releasing the connection to
																									// the http client's pool

			Log.v("PushDelivery", "sendData: xml=" + xml);

			return statusCode;
		}
		catch (Exception e) {
			Log.e("PushDelivery", e.toString());
			return 0;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		PushDelivery that = (PushDelivery) o;
		if (Double.compare(that.notificationId, notificationId) != 0)
			return false;
		if (httpPut != null ? !httpPut.getURI().equals(that.httpPut.getURI())
				: that.httpPut != null)
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		result = httpPut != null ? httpPut.getURI().hashCode() : 0;
		temp = notificationId != +0.0d ? Double.doubleToLongBits(notificationId)
				: 0L;
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
}
