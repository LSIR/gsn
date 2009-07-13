package gsn.http.rest;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.wrappers.AbstractWrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;

import com.thoughtworks.xstream.XStream;

public class PushRemoteWrapper extends AbstractWrapper {

	private static final int KEEP_ALIVE_PERIOD = 5000;

	private final transient Logger     logger                 = Logger.getLogger ( PushRemoteWrapper.class );

	private final XStream XSTREAM = StreamElement4Rest.getXstream();

	private double uid = -1; //only set for push based delivery(default)

	private RemoteWrapperParamParser initParams;

	private DefaultHttpClient httpclient = new DefaultHttpClient();

	private long lastReceivedTimestamp;

	private DataField[] structure;

	List <NameValuePair> postParameters;

	public void dispose() {
		NotificationRegistry.getInstance().removeNotification(uid);
	}

	public boolean initialize() {

		try {
			initParams = new RemoteWrapperParamParser(getActiveAddressBean(),true);
			if (initParams.isPushBased())
				uid  = Math.random();

			postParameters = new ArrayList <NameValuePair>();
			postParameters.add(new BasicNameValuePair(PushDelivery.NOTIFICATION_ID_KEY, Double.toString(uid)));
			postParameters.add(new BasicNameValuePair(PushDelivery.LOCAL_CONTACT_POINT, initParams.getLocalContactPoint()));
			
			lastReceivedTimestamp = initParams.getStartTime();
			structure = registerAndGetStructure();
		}catch (Exception e) {
			logger.error(e.getMessage(),e);
			NotificationRegistry.getInstance().removeNotification(uid);
			return false;
		}


		return true;
	}

	public DataField[] getOutputFormat() {
		return structure;
	}

	public String getWrapperName() {
		return "Push-Remote Wrapper";
	}

	public DataField[] registerAndGetStructure() throws ClientProtocolException, IOException, ClassNotFoundException {
		HttpPost httpPost = new HttpPost(initParams.getRemoteContactPointEncoded(lastReceivedTimestamp));
		httpPost.setEntity(new UrlEncodedFormEntity(postParameters, HTTP.UTF_8));
		
		HttpContext localContext = new BasicHttpContext();
		
		NotificationRegistry.getInstance().addNotification(uid, this);
		
		int tries = 0;
		while(tries < 2){
			tries++;
			HttpResponse response = httpclient.execute(httpPost, localContext);
			HttpEntity entity = response.getEntity();
			
			int sc = response.getStatusLine().getStatusCode();
			AuthState authState = null;
			if (sc == HttpStatus.SC_UNAUTHORIZED) {
				// Target host authentication required
				authState = (AuthState) localContext.getAttribute(ClientContext.TARGET_AUTH_STATE);
			}
			if (sc == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED) {
				// Proxy authentication required
				authState = (AuthState) localContext.getAttribute(ClientContext.PROXY_AUTH_STATE);
			}

			if (authState != null) {
				if(initParams.getUsername() == null || tries > 1){
					logger.error("A valid username/password required to connect to the remote host: " + initParams.getRemoteContactPoint());
				}else{
					AuthScope authScope = authState.getAuthScope();
					Credentials creds = new UsernamePasswordCredentials(initParams.getUsername(), initParams.getPassword());
					httpclient.getCredentialsProvider().setCredentials(authScope, creds);
				}
			} else {
				logger.debug ( new StringBuilder ( ).append ( "Wants to consume the strcture packet from " ).append(initParams.getRemoteContactPoint()));
				structure = (DataField[]) XSTREAM.fromXML(entity.getContent());
				logger.debug("Connection established for: "+ initParams.getRemoteContactPoint());
				break;	
			}
		}
		
		if(structure == null)
			throw new RuntimeException("Cannot connect to the remote host.");

		return structure;
	}

	public boolean manualDataInsertion(String Xstream4Rest) {
		logger.debug ( new StringBuilder ( ).append ( "Received Stream Element at the push wrapper."));
		StreamElement4Rest se = (StreamElement4Rest) XSTREAM.fromXML(Xstream4Rest);
		StreamElement streamElement = se.toStreamElement();
		lastReceivedTimestamp = streamElement.getTimeStamp();
		return postStreamElement(streamElement);
	}

	public void run() {
		HttpPost httpPost = new HttpPost(initParams.getRemoteContactPointEncoded(lastReceivedTimestamp));
		while(isActive()) {
			try {
				Thread.sleep(KEEP_ALIVE_PERIOD);
				httpPost.setEntity(new UrlEncodedFormEntity(postParameters, HTTP.UTF_8));
				HttpResponse response = httpclient.execute(httpPost); //This is acting as keep alive.
				int status = response.getStatusLine().getStatusCode();
				if (status != RestStreamHanlder.SUCCESS_200) {
					logger.error("Cant register to the remote client, retrying in:"+ (KEEP_ALIVE_PERIOD/1000)+" seconds.");
					structure = registerAndGetStructure();
				}
			} catch (InterruptedException e) {
				logger.warn(e.getMessage(),e);
			} catch (ClientProtocolException e) {
				logger.warn(e.getMessage(),e);
			} catch (IOException e) {
				logger.warn(e.getMessage(),e);		
			} catch (ClassNotFoundException e) {
				logger.warn(e.getMessage(),e);
			}
		}
	}
}
