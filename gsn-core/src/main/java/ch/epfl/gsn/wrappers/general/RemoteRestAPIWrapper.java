/**
 * Global Sensor Networks (GSN) Source Code
 * Copyright (c) 2006-2016, Ecole Polytechnique Federale de Lausanne (EPFL)
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
 * File: src/ch/epfl/gsn/wrappers/general/RemoteRestAPIWrapper.java
 *
 * @author Julien Eberle
 *
 */

package ch.epfl.gsn.wrappers.general;

import play.libs.Json;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import org.apache.commons.codec.binary.Base64;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import ch.epfl.gsn.beans.AddressBean;
import ch.epfl.gsn.beans.DataField;
import ch.epfl.gsn.beans.StreamElement;
import ch.epfl.gsn.wrappers.AbstractWrapper;

import org.slf4j.Logger;

public class RemoteRestAPIWrapper extends AbstractWrapper {

	private final transient Logger logger = LoggerFactory.getLogger(RemoteRestAPIWrapper.class);

	private DataField[] structure = null;
	private String wsURL;
	String clientId;
	String clientSecret;
	private AddressBean addressBean;
	private long lastReceivedTimestamp = -1;
	private String token = "";
	private String vsName;
	private HttpClient client = HttpClients.createDefault();
	private HttpGet getData = null;
	private int samplingPeriodInMsc;


	public DataField[] getOutputFormat() {
		return structure;
	}


	public String getWrapperName() {
		return "Remote REST API Wrapper";
	}

	public boolean initialize() {
		try {
			addressBean = getActiveAddressBean( );
			wsURL = addressBean.getPredicateValue("url");
			if ( wsURL == null || wsURL.trim().length() == 0 ) {
				logger.warn( "The url parameter is missing from the Remote Rest API wrapper, initialization failed." );
				return false;
			}
			clientId = addressBean.getPredicateValue("client_id");
			if ( clientId == null || clientId.trim().length() == 0 ) {
				logger.warn( "The client_id parameter is missing from the Remote Rest API wrapper, initialization failed." );
				return false;
			}
			clientSecret = addressBean.getPredicateValue("client_secret");
			if ( clientSecret == null || clientSecret.trim().length() == 0 ) {
				logger.warn( "The client_secret parameter is missing from the Remote Rest API wrapper, initialization failed." );
				return false;
			}
			vsName = addressBean.getPredicateValue("vs_name");
			if ( vsName == null || vsName.trim().length() == 0 ) {
				logger.warn( "The vs_name parameter is missing from the Remote Rest API wrapper, initialization failed." );
				return false;
			}
			String starting = addressBean.getPredicateValueWithDefault("starting_time", ""+System.currentTimeMillis());
			lastReceivedTimestamp = Long.valueOf(starting);
			samplingPeriodInMsc = addressBean.getPredicateValueAsInt("sampling", 10000);
			token = getToken();
			structure = getRemoteStructure();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return false;
		}
		return structure != null;
	}
		
	public String getToken(){
		HttpPost post = new HttpPost(wsURL + "/oauth2/token");
		List<BasicNameValuePair> parametersBody = new ArrayList<BasicNameValuePair>();
		parametersBody.add(new BasicNameValuePair("grant_type", "client_credentials"));
		parametersBody.add(new BasicNameValuePair("client_id", clientId));
		parametersBody.add(new BasicNameValuePair("client_secret", clientSecret));
		HttpResponse response = null;
		try {
		    post.setEntity(new UrlEncodedFormEntity(parametersBody, StandardCharsets.UTF_8));
			response = client.execute(post);
			int code = response.getStatusLine().getStatusCode();
			if (code == 401) {
			    // Add Basic Authorization header
				post.addHeader("Authorization",Base64.encodeBase64String((clientId+":"+clientSecret).getBytes()));
				post.releaseConnection();
				response = client.execute(post);
				code = response.getStatusLine().getStatusCode();
			}
			if (code == 200){
			    String content = EntityUtils.toString(response.getEntity());
			    return Json.parse(content).get("access_token").asText();
			}
		} catch (Exception e){
			logger.error(e.getMessage(), e);	
		}	
		return null;
	}
	
	private String doRequest(HttpGet get){
		HttpResponse response = null;
		int code = 401;
		try {
			if (token != null){
				get.removeHeaders("Authorization");
				get.addHeader("Authorization", "Bearer " + token);
				get.releaseConnection();
			    response = client.execute(get);
			    code = response.getStatusLine().getStatusCode();
			}
			if (code == 401) {
				// Access token is invalid or expired. Regenerate the access token
				getToken();
				if (token != null) {
				    get.removeHeaders("Authorization");
					get.addHeader("Authorization", "Bearer " + token);
					get.releaseConnection();
					response = client.execute(get);
					code = response.getStatusLine().getStatusCode();
				}
			}
			if (code == 200){
				return EntityUtils.toString(response.getEntity());
			}
		} catch (Exception e){
			logger.error(e.getMessage(), e);
		}
		return "";
	}


	public DataField[] getRemoteStructure() throws IOException, ClassNotFoundException {
		HttpGet get = new HttpGet(wsURL + "/api/sensors/" + vsName);
		try {
			String content = doRequest(get);
			JsonNode jn = Json.parse(content).get("properties");
			DataField[] df = new DataField[jn.get("fields").size()-1];
			int i = 0;
			for(JsonNode f : jn.get("fields")){
				if (f.get("name").asText().equals("timestamp")) continue; 
				df[i] = new DataField(f.get("name").asText(),f.get("type").asText());
				i++;
			}
			return df;

		} catch (Exception e){
			logger.error(e.getMessage(), e);
		}
		return null;
	}
					
		
	public void dispose() {
		try {
			getData.releaseConnection();
		} catch (Exception e) {
			logger.debug(e.getMessage(), e);
		}
	}

	public void run() {
		getData = new HttpGet();
		while (isActive()) {
			String uri = wsURL + "/api/sensors/" + vsName + "/data?from=" + ISODateTimeFormat.dateTime().print(lastReceivedTimestamp);
			try {
				getData.setURI(new URI(uri));
				String content = doRequest(getData);
				for (StreamElement se : StreamElement.fromJSON(content)){
					manualDataInsertion(se);
				}
				Thread.sleep(samplingPeriodInMsc);
			}
			catch (Exception e) {
				logger.warn("Something went wrong when querying the REST API at " + uri + " trying again in 1 minute...");
				try {
					if (isActive()) {
						Thread.sleep(60000);
					}
				} catch (Exception err) {
					logger.debug(err.getMessage(), err);
				}
			}
		}
	}

	public boolean manualDataInsertion(StreamElement se) {
		try {
			// If the stream element is out of order, we accept the stream element and wait for the next (update the last received time and return true)
			if (isOutOfOrder(se)) {
				lastReceivedTimestamp = se.getTimeStamp();
				return true;
			}
			// Otherwise, we first try to insert the stream element.
			// If the stream element was inserted succesfully, we wait for the next,
			// otherwise, we return false.
			boolean status = postStreamElement(se);
			if (status)
				lastReceivedTimestamp = se.getTimeStamp();
			return status;
		}
		catch (SQLException e) {
			logger.warn(e.getMessage(), e);
			return false;
		}
	}
}
