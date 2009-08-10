package gsn.operators;

import gsn.beans.Operator;
import gsn.beans.StreamElement;
import gsn.beans.DataField;
import gsn.channels.DataChannel;
import gsn.core.OperatorConfig;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

public class SensorInternetVS  implements Operator {
	
	public void process ( String inputStreamName , List<StreamElement> data ) {
		for (StreamElement se: data)
			process(inputStreamName, se);
	}

  public DataField[] getStructure() {
    return new DataField[0];  //To change body of implemented methods use File | Settings | File Templates.
  }

  public void start() {}
	public void stop() {}

	private static final String SI_URL = "si-url";
	private URL siUrl = null;

	private static final String SI_USERNAME = "si-username";
	private String siUsername = null;

	private static final String SI_PASSWORD = "si-password";
	private String siPassword = null;

	private static final String SI_STREAM_MAPPING = "si-stream-mapping";
	private Integer[] siStreamMapping = null;
	private DataChannel outputChannel;

	private static final String REQUEST_AGENT = "GSN (Global Sensors Networks) Virtual Sensor" ;

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss") ;

	private static transient Logger logger  = Logger.getLogger ( SensorInternetVS.class );

	public  SensorInternetVS(OperatorConfig config,DataChannel outputChannel ) throws MalformedURLException {
		this.outputChannel = outputChannel;
		
		siUrl = new URL ( config.getParameters().getValueWithException(SI_URL)) ;
		siUsername =config.getParameters().getValueWithException(SI_USERNAME);
		siPassword =config.getParameters().getValueWithException(SI_PASSWORD);
		
		String param = config.getParameters().getValueWithException(SI_STREAM_MAPPING) ;
		siStreamMapping = initStreamMapping(param) ;
		if (siStreamMapping == null) 
			throw new RuntimeException("Failed to parse the required parameter: >" + SI_STREAM_MAPPING + "< (" + param + ")");

		// Enabling Basic authentication
		Authenticator.setDefault(new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication (siUsername, siPassword.toCharArray());
			}
		});
		
	}

	public void process(String inputStreamName, StreamElement streamElement) {
		try {
			
			// Init the HTTP Connection
			HttpURLConnection siConnection = (HttpURLConnection) siUrl.openConnection();
			siConnection.setRequestMethod("POST");
			siConnection.setDoOutput(true);
			siConnection.setRequestProperty( "User-Agent", REQUEST_AGENT );
			siConnection.connect();

			// Build and send the parameters
			PrintWriter out = new PrintWriter(siConnection.getOutputStream());
			String postParams = buildParameters(streamElement) ;
			logger.debug("POST parameters: " + postParams) ;
			out.print(postParams);
			out.flush();
			out.close();


			if (siConnection.getResponseCode() == 200) {
				logger.debug("data successfully sent");
			}
			else {
				logger.error("Unable to send the data. Check you configuration file. " + siConnection.getResponseMessage() + " Code (" + siConnection.getResponseCode() + ")");
			}
		} catch (IOException e) {
			logger.error(e.getMessage()) ;
		}
	}

	public void dispose() {

	}

	private String buildParameters (StreamElement se) {

		StringBuilder sb = new StringBuilder () ;
		//
		for (int i = 0 ; i < se.getFieldNames().length ; i++) {
			if (i < siStreamMapping.length) {
				if (i != 0) sb.append("&");
				sb.append(createPostParameter ("time[" + i + "]=", dateFormat.format(new Date (se.getTimeInMillis()))));
				sb.append("&");
				sb.append(createPostParameter ("data[" + i + "]=", se.getValue(se.getFieldNames()[i]).toString()));
				sb.append("&");
				sb.append(createPostParameter ("key[" + i + "]=", Integer.toString(siStreamMapping[i])));
			}
			else {
				logger.warn("The field >" + se.getFieldNames()[i] + "< is not mapped in your configuration file.");
			}
		}
		return sb.toString();
	}

	private String createPostParameter (String paramName, String paramValue) {
		try {
			return paramName + URLEncoder.encode(paramValue, "UTF-8") ;
		} catch (UnsupportedEncodingException e) {
			logger.debug(e.getMessage(), e);
		}
		return null;
	}

	private Integer[] initStreamMapping (String param) {
		String[] mps = param.split(",");
		Integer[] mapping = new Integer[mps.length] ;
		try {
			for (int i = 0 ; i < mps.length ; i++) {
				mapping[i] = Integer.parseInt(mps[i]);
			}
		}
		catch (java.lang.NumberFormatException e) {
			logger.error(e.getMessage());
			return null;
		}
		return mapping;
	}

}
