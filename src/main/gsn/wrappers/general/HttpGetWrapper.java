package gsn.wrappers.general;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.beans.WrapperConfig;
import gsn.channels.DataChannel;
import gsn.wrappers.Wrapper;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;

public class HttpGetWrapper implements Wrapper {
	private final WrapperConfig conf;

	private final DataChannel dataChannel;

	private int                      DEFAULT_RATE       = 2000;

	private final transient Logger   logger             = Logger.getLogger( HttpGetWrapper.class );

	private HttpURLConnection httpURLConnection;

	private URL url;

	private int                      rate;

	private transient final DataField [] outputStructure = new  DataField [] { new DataField( "data" , "binary:image/jpeg" ) };


	/**
	 * From XML file it needs the followings :
	 * <ul>
	 * <li>url</li> The full url for retriving the binary data.
	 * <li>rate</li> The interval in msec for updating/asking for new information.
	 * <li>mime</li> Type of the binary data.
	 * </ul>
	 * @throws MalformedURLException 
	 */
	public HttpGetWrapper(WrapperConfig conf, DataChannel channel) throws MalformedURLException {
		this.conf = conf;
		this.dataChannel= channel;
		url = new URL( conf.getParameters().getValue( "url" ));

		rate = conf.getParameters().getValueAsInt( "rate",DEFAULT_RATE );
		logger.debug( "AXISWirelessCameraWrapper is now running @" + rate + " Rate." );
	}

	public void start(){
		ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream(1024*20);
		byte[] buffer = new byte[16*1024];
		BufferedInputStream content;
		while ( isActive ) {
			try {
				Thread.sleep( rate );
				httpURLConnection = (HttpURLConnection) url.openConnection();
				httpURLConnection.connect();
				if ( httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_ACCEPTED ) continue;
				content = new BufferedInputStream(httpURLConnection.getInputStream(),4096);
				arrayOutputStream.reset();
				int readIndex = -1;
				while ( (readIndex= content.read(buffer))!=-1)
					arrayOutputStream.write(buffer, 0, readIndex);
				dataChannel.write(StreamElement.from(this).set(getOutputFormat()[0].getName(), arrayOutputStream.toByteArray()).setTime(System.currentTimeMillis()));
			} catch ( InterruptedException e ) {
				logger.error( e.getMessage( ) , e );
			}catch (IOException e) {
				logger.error( e.getMessage( ) , e );
			}
		}
	}


	private boolean isActive=true;

	public void dispose ( ) {

	}
	public  DataField[] getOutputFormat ( ) {
		return outputStructure;
	}

	public void stop() {
		isActive = false;

	}

}
