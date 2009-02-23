package gsn.vsensor;

import gsn.beans.StreamElement;
import org.apache.log4j.Logger;
import gsn.storage.StorageManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;
import java.util.TreeMap;

public class ClockedBridgeVirtualSensor extends AbstractVirtualSensor implements ActionListener {
	
	private static final String RATE_PARAM       = "rate";
	private static final String TABLE_NAME_PARAM = "table_name";
	
	private Timer timer;
	private int clock_rate;
	private String table_name;
	private long last_updated;

	private static final transient Logger logger = Logger.getLogger( ClockedBridgeVirtualSensor.class );

	public boolean initialize ( ) {

		TreeMap<String, String> params = getVirtualSensorConfiguration().getMainClassInitialParams();

		String rate_value = params.get(RATE_PARAM);
		
		if (rate_value == null){
			logger.warn("Parameter \"rate\" not provider in Virtual Sensor file");
			return false;
		}
		
		clock_rate = Integer.parseInt(rate_value);
		
		String table_name_value = params.get(TABLE_NAME_PARAM);
		
		if (table_name_value == null){
			logger.warn("Parameter \"table_name\" not provider in Virtual Sensor file");
			return false;
		}
		
		table_name = table_name_value;
		
		timer = new Timer( clock_rate , this );
		
		timer.start( );
		
		// TODO read current time
		last_updated = -1 ;
		
		return true;
	}

	public void dataAvailable ( String inputStreamName , StreamElement data ) {
		dataProduced( data );
		if ( logger.isDebugEnabled( ) ) logger.debug( "Data received under the name: " + inputStreamName );
	}

	public void finalize ( ) {
		timer.stop( );

	}
	
	  public void actionPerformed ( ActionEvent actionEvent ) {
		  
		  /*check if new data is posted*/
		  /*call dataProduced(StreamElement se)*/
		  StorageManager.
//		    StreamElement streamElement = new StreamElement( EMPTY_FIELD_LIST , EMPTY_FIELD_TYPES , EMPTY_DATA_PART , actionEvent.getWhen( ) );
//		    if(delayPostingElements){
//		    	streamElementBuffer.add(streamElement);
//		    	synchronized(objectLock){
//		    		objectLock.notifyAll();
//		    	}
//		    }
//		    else
//		    	postStreamElement( streamElement );
		  }

}
