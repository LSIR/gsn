package gsn.wrappers.general;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;
import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.vsensor.Container;
import gsn.wrappers.AbstractStreamProducer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.TooManyListenersException;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import de.avetana.bluetooth.connection.BadURLFormat;
import de.avetana.bluetooth.connection.JSR82URL;
import de.avetana.bluetooth.rfcomm.RFCommConnectionImpl;

import javax.bluetooth.*;

/**
 * This class connects to a bluetooth URL and tries to open a serial port
 * connection on top of it. The url is given in the configuration file with
 * the "url" property.
 * 
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 * @author Jerome Rousselot CSEM<br>
 */
public class BTWrapper extends AbstractStreamProducer {
   
   private static final String    RAW_PACKET    = "RAW_PACKET";
   
   private final transient Logger logger        = Logger.getLogger( BTWrapper.class );
   public static final int MAX_SIZE = 65535;
   byte[] bytes = new byte[MAX_SIZE];   
   private String btUrl;   
   private int                    threadCounter = 0;
   private static int             MAXBUFFERSIZE = 1024;
   
   // private StringBuffer inputBuffer = new StringBuffer();
   private byte[]               inputBuffer;
   
   private AddressBean            addressBean;
   private String                 serialPort;
   private RFCommConnectionImpl connection;
   private InputStream             is;
   private OutputStream os;
   
   /*
    * Needs the following information from XML file : url: the bluetooth
    * url to get access to the device.
    */
   public boolean initialize ( TreeMap context ) {
      if ( !super.initialize( context ) ) return false;
      setName( "WNetSerialWrapper-Thread" + ( ++threadCounter ) );
      addressBean = ( AddressBean ) context.get( Container.STREAM_SOURCE_ACTIVE_ADDRESS_BEAN );
      btUrl = addressBean.getPredicateValue( "url" );
      try {
		connection = RFCommConnectionImpl.createRFCommConnection(new JSR82URL(btUrl));
		is = connection.openInputStream();
		os = connection.openOutputStream();
		
	} catch (BadURLFormat e1) {
		logger.error(e1);
		return false;
	} catch (Exception e1) {
		logger.error(e1);
		return false;
	}
	
      return true;
   }
   
   public void run ( ) {
	   int nbBytes;
      while ( isActive( ) ) {
    	  try {
    		  nbBytes = is.read(bytes);
    		  if (nbBytes > 0) {
    			  inputBuffer = new byte[nbBytes];
    			  for(int i=0; i < nbBytes; i++) {
    				  inputBuffer[i]= bytes[i];
    			  }
    			  StreamElement streamElement = new StreamElement( 
    					  new String [ ] { RAW_PACKET } , 
    					  new Integer [ ] { DataTypes.BINARY } , 
    					  new Serializable [ ] { inputBuffer } , 
    					  System.currentTimeMillis( ) );
    			  postStreamElement( streamElement );
    		  }
    	  }catch(IOException e) {
    		  logger.error(e);
    	  }
    	  
      }
   }
   
   public Collection < DataField > getOutputFormat ( ) {
      ArrayList < DataField > dataField = new ArrayList < DataField >( );
      dataField.add( new DataField( RAW_PACKET , "BINARY" , "The packet contains raw data from a sensor network." ) );
      return dataField;
   }
   
   public void finalize ( HashMap context ) {
      super.finalize( context );
      threadCounter--;
   }
}