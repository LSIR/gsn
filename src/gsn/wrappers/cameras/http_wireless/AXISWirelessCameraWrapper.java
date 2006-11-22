package gsn.wrappers.cameras.http_wireless;

import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.utils.TCPConnPool;
import gsn.vsensor.Container;
import gsn.wrappers.AbstractStreamProducer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS)<br>
 */
public class AXISWirelessCameraWrapper extends AbstractStreamProducer {
   
   /**
    * 
    */
   private static final Integer [ ] OUTPUT_FIELD_TYPES = new Integer [ ] { DataTypes.BINARY };
   
   /**
    * 
    */
   private static final String [ ]  OUTPUT_FIELD_NAMES = new String [ ] { "PICTURE" };
   
   private int                      DEFAULT_RATE       = 2000;
   
   private static int               threadCounter      = 0;
   
   private final transient Logger   logger             = Logger.getLogger( AXISWirelessCameraWrapper.class );
   
   private String                   host;
   
   private PostMethod                       postMethod;
   
   private AddressBean              addressBean;
   
   private String                   inputRate;
   
   private int                      rate;
   
   private transient final ArrayList < DataField > dataField = new ArrayList < DataField >( );
   
   /**
    * From XML file it needs the followings :
    * <ul>
    * <li>host</li>
    * <br>
    * <li>rate</li>
    * <br>
    * </ul>
    */
   public boolean initialize ( TreeMap context ) {
      this.addressBean = ( AddressBean ) context.get( Container.STREAM_SOURCE_ACTIVE_ADDRESS_BEAN );
      host = this.addressBean.getPredicateValue( "host" );
      inputRate = this.addressBean.getPredicateValue( "rate" );
      int remotePort = Integer.parseInt( this.addressBean.getPredicateValue( "port" ) );
      if ( inputRate == null || inputRate.trim( ).length( ) == 0 ) rate = DEFAULT_RATE;
      else
         rate = Integer.parseInt( inputRate );
      setName( "WirelessCameraWrapper-Thread" + ( ++threadCounter ) );
      postMethod = new PostMethod( "HTTP://" + host + "/axis-cgi/jpg/image.cgi" );
      postMethod.addParameter( "resolution" , "320x240" );
      // postMethod.addParameter ( "resolution" , "320x240" ) ;
      postMethod.addParameter( "compression" , "50" );
      postMethod.addParameter( "clock" , "1" );
      postMethod.addParameter( "date" , "1" );
      if ( logger.isDebugEnabled( ) ) logger.debug( "AXISWirelessCameraWrapper is now running @" + rate + " Rate." );
      dataField.add( new DataField( "picture" , "binary:image/jpeg" , "JPEG image from the remote networked camera." ) );
      return true;
   }
   
   public void run ( ) {
      byte [ ] received_image = null;
      while ( isActive( ) ) {
         int code = TCPConnPool.executeMethod( postMethod , false );
         try {
            if ( rate > 0 ) Thread.sleep( rate );
         } catch ( InterruptedException e ) {
            logger.error( e.getMessage( ) , e );
         }
         if ( code != 200 ) continue;
         try {
            received_image = postMethod.getResponseBody( );
         } catch ( IOException e1 ) {
            e1.printStackTrace( );
         }
         StreamElement streamElement = new StreamElement( OUTPUT_FIELD_NAMES , OUTPUT_FIELD_TYPES , new Serializable [ ] { received_image } , System.currentTimeMillis( ) );
         postStreamElement( streamElement );
      }
   }
   
   public void finalize ( HashMap context ) {
      super.finalize( context );
      threadCounter--;
   }
   
   public Collection < DataField > getOutputFormat ( ) {
      return dataField;
   }
   
}
