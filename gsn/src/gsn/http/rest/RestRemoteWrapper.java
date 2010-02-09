package gsn.http.rest;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.storage.StorageManager;
import gsn.wrappers.AbstractWrapper;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;

import com.thoughtworks.xstream.XStream;

public class RestRemoteWrapper extends AbstractWrapper {

	private final XStream XSTREAM = StreamElement4Rest.getXstream();

	private final transient Logger     logger                 = Logger.getLogger ( RestRemoteWrapper.class );

	private  DataField[]    structure               = null;

	private DefaultHttpClient httpclient = new DefaultHttpClient();

	private ObjectInputStream inputStream;
	
	private long lastReceivedTimestamp = -1;
	
	
	private HttpResponse response;

	public DataField[] getOutputFormat() {
		return structure;
	}

	private RemoteWrapperParamParser params;
	
	public String getWrapperName() {
		return "Rest Remote Wrapper";
	}

	public boolean initialize() {
			params = new RemoteWrapperParamParser(getActiveAddressBean(),false);
			String startTime = getActiveAddressBean().getPredicateValue("start-time");
			if (startTime != null && startTime.equals("continue")) {
				Connection conn = null;
				try {
					conn = StorageManager.getInstance().getConnection();

					// check if table already exists
					ResultSet rs = conn.getMetaData().getTables(null, null, getActiveAddressBean().getVirtualSensorName(), new String[] {"TABLE"});
					if (rs.next()) {
						StringBuilder query = new StringBuilder();
						query.append("select max(timed) from ").append(getActiveAddressBean().getVirtualSensorName());
						rs = StorageManager.executeQueryWithResultSet(query, conn);
						if (rs.next()) {
							lastReceivedTimestamp = rs.getLong(1);
						}
					}
					else
						logger.info("Table '" + getActiveAddressBean().getVirtualSensorName() + "' doesn't exist => using all data from the remote database");
				} catch (SQLException e) {
					logger.error(e.getMessage(), e);
					return false;
				} finally {
					StorageManager.close(conn);
				}
			} else {
				lastReceivedTimestamp = params.getStartTime();
			}
			logger.info("lastReceivedTimestamp=" + String.valueOf(lastReceivedTimestamp));
			
			try {
				structure = connectToRemote();
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				return false;
			}
		return true;
	}

	
	private DataField[] connectToRemote() throws ClientProtocolException, IOException, ClassNotFoundException {
		HttpGet httpget = new HttpGet(params.getRemoteContactPointEncoded(lastReceivedTimestamp));
		HttpContext localContext = new BasicHttpContext();
		
		structure = null;
		
		
		int tries = 0;
		while(tries < 2){
			tries++;
			
			response = httpclient.execute(httpget, localContext);
			
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
				if(params.getUsername() == null || tries > 1){
					logger.error("A valid username/password required to connect to the remote host: " + params.getRemoteContactPoint());
				}else{
					AuthScope authScope = authState.getAuthScope();
					Credentials creds = new UsernamePasswordCredentials(params.getUsername(), params.getPassword());
					httpclient.getCredentialsProvider().setCredentials(authScope, creds);
				}
			} else {
				logger.debug ( new StringBuilder ( ).append ( "Wants to consume the strcture packet from " ).append(params.getRemoteContactPoint()));
				inputStream = XSTREAM.createObjectInputStream( response.getEntity().getContent());
				structure = (DataField[]) inputStream.readObject();
				logger.debug("Connection established for: "+ params.getRemoteContactPoint());
				break;	
			}
		}
		
		if(structure == null)
			throw new RuntimeException("Cannot connect to the remote host.");

		return structure;
		
	}
	
	private void reconnectToRemote() {
		logger.info("trying to reconnect every 3 seconds... ");
		while(isActive()) {
			try {
				Thread.sleep(3000);
				if(isActive())
					connectToRemote();
				return;
			} catch (Exception err) {
				logger.warn(err.getMessage());
			}
		}
	}

	public void dispose() {
		try {
			httpclient.getConnectionManager().shutdown(); //This closes the connection already in use by the response
		} catch (Exception e) {
			logger.debug(e.getMessage(),e);
		}
	}

	public void run() {
		StreamElement4Rest se = null;
		while(isActive()) {
			try {
				while(isActive() && (se = (StreamElement4Rest)inputStream.readObject())!=null) { 
					StreamElement streamElement = se.toStreamElement();
					postStreamElement(streamElement);
					lastReceivedTimestamp = streamElement.getTimeStamp();
				}
			} catch (Exception e) {
				if(isActive()) {
					logger.warn("Connection to: "+params.getRemoteContactPoint()+" is lost (Exception: " + e.getMessage() + ")");
					reconnectToRemote();
				}
			}
		}
	}

	public boolean manualDataInsertion(StreamElement se) {
		lastReceivedTimestamp = se.getTimeStamp();
		return postStreamElement(se);
	}
}
