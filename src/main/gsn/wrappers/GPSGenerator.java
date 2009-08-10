package gsn.wrappers;

import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn2.wrappers.WrapperConfig;
import gsn.channels.DataChannel;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import gsn2.wrappers.Wrapper;

public class GPSGenerator implements Wrapper {

  private final WrapperConfig conf;

  private final DataChannel dataChannel;

  private static final int         DEFAULT_SAMPLING_RATE = 1000;

  private static final String [ ]  FIELD_NAMES           = new String [ ] { "latitude" , "longitude" , "temperature" , "light" , "camera" };

  private static final Byte [ ] FIELD_TYPES           = new Byte [ ] { DataTypes.NUMERIC, DataTypes.NUMERIC, DataTypes.NUMERIC, DataTypes.NUMERIC , DataTypes.BINARY };

  private static final String [ ]  FIELD_DESCRIPTION     = new String [ ] { "Latitude Reading" , "Longitude Reading" , "Temperature Sensor" , "Light Sensor" , "Camera Picture" };

  private static final String [ ]  FIELD_TYPES_STRING    = new String [ ] { "double" , "double" , "double" , "int" , "binary:jpeg" };

  private int                      samplingRate          = DEFAULT_SAMPLING_RATE;

  private final transient Logger   logger                = Logger.getLogger( GPSGenerator.class );

  private byte [ ]                 picture;

  private DataField[]  outputStrcture     ;

  public DataField [] getOutputFormat ( ) {
    return outputStrcture;
  }

  public GPSGenerator(WrapperConfig conf, DataChannel channel) throws IOException {
    this.conf = conf;
    this.dataChannel= channel;
    samplingRate = conf.getParameters().getValueAsInt("rate", DEFAULT_SAMPLING_RATE);
    String picture = conf.getParameters().getValueWithException("picture");

    File pictureF = new File( picture );
    if ( !pictureF.isFile( ) || !pictureF.canRead( ) )
      throw new RuntimeException( "The GPSGenerator can't access the specified picture file. Initialization failed." );

    BufferedInputStream fis = new BufferedInputStream( new FileInputStream( pictureF ) );
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
    byte [ ] buffer = new byte [ 4 * 1024 ];
    while ( fis.available( ) > 0 )
      outputStream.write( buffer , 0 , fis.read( buffer )  );
    fis.close( );
    this.picture = outputStream.toByteArray( );
    outputStream.close( );

    ArrayList<DataField > output = new ArrayList < DataField >();
    for ( int i = 0 ; i < FIELD_NAMES.length ; i++ )
      output.add( new DataField( FIELD_NAMES[ i ] , FIELD_TYPES_STRING[ i ]  ) );
    outputStrcture = output.toArray( new DataField[] {} );
    isActive = true;
  }

  private static int step = 1;
  public void start() {
    while ( isActive ) {
      double latitude = 37.4419+.01* (step++);
      double longitude = -122.1419;
      try {
        Thread.sleep( samplingRate );
      } catch ( InterruptedException e ) {
        logger.error( e.getMessage( ) , e );
      }
      Serializable[] values = {latitude, longitude, 25.5, 650, picture};
      StreamElement se = StreamElement.from(this).setTime(System.currentTimeMillis());
      for (int i=0;i<values.length;i++)
        se.set(getOutputFormat()[i].getName(),values[i]);

      dataChannel.write( se );
    }
  }

  private boolean isActive;

  public void dispose ( ) {
    isActive = false;
  }

  public void stop() {

  }

}
