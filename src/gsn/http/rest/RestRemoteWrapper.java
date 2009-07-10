package gsn.http.rest;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.wrappers.AbstractWrapper;

import java.io.IOException;
import java.io.ObjectInputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
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
		try {
			params = new RemoteWrapperParamParser(getActiveAddressBean(),false);
			lastReceivedTimestamp = params.getStartTime();
			structure = connectToRemote();
		}catch (Exception e) {
			logger.error(e.getMessage(),e);
			return false;
		}
		return true;
	}

	
	public DataField[] connectToRemote() throws ClientProtocolException, IOException, ClassNotFoundException {
		HttpGet httpget = new HttpGet(params.getRemoteContactPointEncoded(lastReceivedTimestamp));
		//TODO: set the authentication.
		response = httpclient.execute(httpget);
		logger.debug ( new StringBuilder ( ).append ( "Wants to consume the strcture packet from " ).append(params.getRemoteContactPoint()));
		inputStream = XSTREAM.createObjectInputStream( response.getEntity().getContent());
		DataField[] structure = (DataField[]) inputStream.readObject();
		logger.debug("Connection established for: "+ params.getRemoteContactPoint());
		return structure;
	}

	public void dispose() {
		try {
			response.getEntity().consumeContent(); //can't close without consuming
			response.getEntity().getContent().close();
		} catch (IOException e) {
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
				logger.debug(e.getMessage(),e);
				logger.warn("Connection to: "+params.getRemoteContactPoint()+" is lost, trying to reconnect in 3 seconds...");
				try {
					Thread.sleep(3000);
					connectToRemote();
				} catch (Exception err) {
					logger.error(err.getMessage(),err);
				}
			}
		}
	}

	public boolean manualDataInsertion(StreamElement se) {
		lastReceivedTimestamp = se.getTimeStamp();
		return postStreamElement(se);
	}
}
