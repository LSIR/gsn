package gsn.vsensor;

import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import javax.imageio.ImageIO;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class DemoVSensor extends AbstractVirtualSensor {
   
   private static final transient Logger logger                  = Logger.getLogger( DemoVSensor.class );
   
   private ArrayList < String >          fields                  = new ArrayList < String >( );
   
   private ByteArrayOutputStream         outputStream            = new ByteArrayOutputStream( 24 * 1024 );
   
   private JPEGImageEncoder              codec                   = JPEGCodec.createJPEGEncoder( outputStream );
   
   private ByteArrayInputStream          input;
   
   private static final String           IMAGE_OUTPUT_FIELD      = "image";
   
   private static final int              IMAGE_OUTPUT_FIELD_TYPE = DataTypes.BINARY;
   
   private static final String [ ]       OUTPUT_FIELDS           = new String [ ] { IMAGE_OUTPUT_FIELD };
   
   private static final Integer [ ]      OUTPUT_TYPES            = new Integer [ ] { IMAGE_OUTPUT_FIELD_TYPE };
   
   private static BufferedImage          cachedBufferedImage     = null;
   
   private static int                    counter                 = 0;
   
   public void dataAvailable ( String inputStreamName , StreamElement data ) {
      if ( inputStreamName.equalsIgnoreCase( "SSTREAM" ) ) {
         String action = ( String ) data.getData( "STATUS" );
         /**
          * 
          */
         String moteId = ( String ) data.getData( "ID" );
         if ( moteId.toLowerCase( ).indexOf( "mica" ) < 0 ) return;
         if ( action.toLowerCase( ).indexOf( "add" ) >= 0 ) counter++;
         if ( action.toLowerCase( ).indexOf( "remove" ) >= 0 ) counter--;
      }
      if ( inputStreamName.equalsIgnoreCase( "CSTREAM" ) ) {
         
         BufferedImage bufferedImage = null;
         outputStream.reset( );
         byte [ ] rawData = ( byte [ ] ) data.getData( "IMAGE" );
         input = new ByteArrayInputStream( rawData );
         try {
            bufferedImage = ImageIO.read( input );
         } catch ( IOException e ) {
            e.printStackTrace( );
         }
         Graphics2D graphics = ( Graphics2D ) bufferedImage.getGraphics( );
         int size = 30;
         int locX = 0;
         int locY = 0;
         if ( counter < 0 ) counter = 0;
         switch ( counter ) {
            case 0 :
               graphics.setColor( Color.RED );
               break;
            case 1 :
               graphics.setColor( Color.ORANGE );
               break;
            
            case 2 :
               graphics.setColor( Color.YELLOW );
               break;
            
            case 3 :
               graphics.setColor( Color.GREEN );
               break;
            default :
               logger.warn( new StringBuilder( ).append( "Shouldn't happen.>" ).append( counter ).append( "<" ).toString( ) );
         }
         graphics.fillOval( locX , locY , size , size );
         try {
            codec.encode( bufferedImage );
         } catch ( IOException e ) {
            e.printStackTrace( );
         }
         
         StreamElement outputSE = new StreamElement( OUTPUT_FIELDS , OUTPUT_TYPES , new Serializable [ ] { outputStream.toByteArray( ) } , data.getTimeStamp( ) );
         dataProduced( outputSE );
      }
      if ( logger.isInfoEnabled( ) ) logger.info( new StringBuilder( ).append( "Data received under the name: " ).append( inputStreamName ).toString( ) );
   }
   
   public boolean initialize ( HashMap map ) {
      VSensorConfig vsensor = ((VSensorConfig) map.get( VirtualSensorPool.VSENSORCONFIG ));
      super.initialize( map );
      for ( DataField field : vsensor.getOutputStructure( ) )
         fields.add( field.getFieldName( ) );
      return true;
   }
}
