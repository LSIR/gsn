package gsn.operators;

import gsn.beans.*;
import gsn.channels.DataChannel;
import gsn.utils.protocols.ProtocolManager;
import gsn.utils.protocols.EPuck.SerComProtocol;
import gsn.wrappers.Wrapper;
import gsn.wrappers.general.SerialWrapper;
import gsn2.conf.OperatorConfig;

import java.io.Serializable;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;


/**
 * Virtual sensor to support GPS coord given by NMEA specification over serial
 * Only the $GPRMC values are required.
 * (works as well on bluetooth GPS mapped to serial)
 */
public class GPSNMEAVS  implements Operator {
	
	public void process ( String inputStreamName , List<StreamElement> data ) {
		for (StreamElement se: data)
			process(inputStreamName, se);
	}

  public DataField[] getStructure() {
    return new DataField[0];  //To change body of implemented methods use File | Settings | File Templates.
  }

  public void start() {}
	public void stop() {}

	private static final transient Logger logger = Logger.getLogger( GPSNMEAVS.class );

	private TreeMap < String , String >   params;

	private ProtocolManager               protocolManager;

	private Wrapper                       wrapper;

	private VSFile                 vsensor;

	private static final String [ ] fieldNames = new String [ ] { "latitude" , "longitude" };

	private static final Byte [ ] fieldTypes = new Byte [ ] { DataTypes.DOUBLE, DataTypes.DOUBLE};

	private Serializable [ ] outputData = new Serializable [ fieldNames.length ];

	private DataChannel outputChannel;

	public GPSNMEAVS (OperatorConfig config ,DataChannel outputChannel ) {
		this.outputChannel = outputChannel;
//		wrapper = vsensor.getInputStream( "input1" ).getSource( "source1" ).getWrapper( );
		protocolManager = new ProtocolManager( new SerComProtocol( ) , wrapper );
		if ( logger.isDebugEnabled( ) ) logger.debug( "Created protocolManager" );
//		try {
//			wrapper.sendToWrapper( "h\n" ,null,null);
//		} catch ( OperationNotSupportedException e ) {
//			e.printStackTrace( );
//		}      

		// protocolManager.sendQuery( SerComProtocol.RESET , null );
		if ( logger.isDebugEnabled( ) ) logger.debug( "Initialization complete." );

	}

	public void process ( String inputStreamName , StreamElement data) {
		if ( logger.isDebugEnabled( ) ) logger.debug( "SERIAL RAW DATA :"+new String((byte[])data.getValue(SerialWrapper.RAW_PACKET)));

		//needed? ######
//		Wrapper wrapper = vsensor.getInputStream( "input1" ).getSource( "source1" ).getWrapper( );

		//raw data from serial
		String s = new String( ( byte [ ] ) data.getValue( SerialWrapper.RAW_PACKET ) );
		String [ ] line = s.split( "\n" );
		//iterate on every line
		for ( int i = 0 ; i < line.length ; i++ ) {
			String [ ] part = line[ i ].split( "," );
			//Only the $GPRMC line are analyed for GPS coord
			//Using $GPGGA might be better but wouldn't give any result when no sat are tracked
			if ( part[ 0 ].equals( "$GPRMC" ) ) {
				//converting latitude from DDMM.MMMM to decimal notation
				Double d = Double.valueOf( part[ 3 ] );
				Double lat = d / 100.0;
				lat = Math.floor( lat );
				lat += Double.valueOf( d % 100.0 ) / 60.0;
				if ( part[ 4 ].equals( "S" ) )
					lat = -lat; // south coord
				else if ( !part[ 4 ].equals( "N" ) ) 
					continue; // neither south or north: invalid format -> skip 

				//converting longitude
				d = Double.valueOf( part[ 5 ] );
				Double lon = Math.floor( d / 100.0 );
				lon += Double.valueOf( d % 100.0 ) / 60.0;
				if ( part[ 6 ].equals( "W" ) ) 
					lon = -lon; // west coord

				logger.debug( "latitude:" + lat + " longitude:" + lon );

				//send back the data
				outputData[ 0 ] = lat;
				outputData[ 1 ] = lon;
        StreamElement output = StreamElement.from(this).setTime(System.currentTimeMillis());
        for (int j=0;j<fieldNames.length;j++)
          output.set(fieldNames[j],outputData[j]);
				outputChannel.write( output );
				break;//one $GPRMC line is enough
			}
		}

	}

	public void dispose ( ) {
	}

}
