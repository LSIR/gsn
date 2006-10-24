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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.TooManyListenersException;
import java.util.TreeMap;

import javax.naming.OperationNotSupportedException;

import org.apache.log4j.Logger;

/**
 * Modified to used RXTX (http://users.frii.com/jarvi/rxtx/) which a LGPL
 * replacement for javacomm. The Easiest way to install RXTX is from the binary
 * distribution which is available at
 * http://users.frii.com/jarvi/rxtx/download.html Links GSN to a sensor network
 * through serial port. <p/> The only needed parameter is the serial port
 * address, provided through xml. Default connection settings are 9600 8 N 1 (I
 * had some problems with javax.comm Linux when trying to use non-default
 * settings) TODO parametrize connection settings through xml.
 * 
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 * @author Jerome Rousselot CSEM<br>
 */
public class SerialWrapper extends AbstractStreamProducer implements SerialPortEventListener {
   
   public  static final String     RAW_PACKET    = "RAW_PACKET";
   
   private final transient Logger  logger        = Logger.getLogger( SerialWrapper.class );
   
   private SerialConnection        wnetPort;
   
   private int                     threadCounter = 0;
   
   public InputStream              is;
   
   private AddressBean             addressBean;
   
   private String                  serialPort;
   
   private ArrayList < DataField > dataField     = new ArrayList < DataField >( );
   
   private boolean                 onDemand      = false;
   
   /*
    * Needs the following information from XML file : serialport : the name of
    * the serial port (/dev/ttyS0...)
    */
   public boolean initialize ( TreeMap context ) {
      if ( !super.initialize( context ) ) return false;
      setName( "SerialWrapper-Thread" + ( ++threadCounter ) );
      addressBean = ( AddressBean ) context.get( Container.STREAM_SOURCE_ACTIVE_ADDRESS_BEAN );
      serialPort = addressBean.getPredicateValue( "serialport" );
      if ( serialPort == null || serialPort.trim( ).length( ) == 0 ) {
         logger.warn( "The >serialport< parameter is missing from the SerialWrapper, wrapper initialization failed." );
         return false;
      }
      // TASK : TRYING TO CONNECT USING THE ADDRESS
      wnetPort = new SerialConnection( serialPort );
      if ( wnetPort.openConnection( ) == false ) return false;
      wnetPort.addEventListener( this );
      is = wnetPort.getInputStream( );
      if ( logger.isDebugEnabled( ) ) logger.debug( "Serial port wrapper successfully opened port and registered itself as listener." );
      inputBuffer = new byte [ MAXBUFFERSIZE ];
      dataField.add( new DataField( RAW_PACKET , "BINARY" , "The packet contains raw data from a sensor network." ) );
      return true;
   }
   
   /**
    * A class that handles the details of the serial connection.
    */
   
   public class SerialConnection {
      
      // private SerialParameters parameters;
      
      protected OutputStream     os;
      
      protected InputStream      is;
      
      private CommPortIdentifier portId;
      
      public SerialPort          sPort;
      
      private String             serialPort;
      
      private boolean            open;
      
      /**
       * Creates a SerialConnection object and initialiazes variables passed in
       * as params.
       * 
       * @param serialPort A SerialParameters object.
       */
      public SerialConnection ( String serialPort ) {
         open = false;
         this.serialPort = serialPort;
      }
      
      /**
       * Attempts to open a serial connection (9600 8N1). If it is unsuccesfull
       * at any step it returns the port to a closed state, throws a
       * <code>SerialConnectionException</code>, and returns. <p/> Gives a
       * timeout of 30 seconds on the portOpen to allow other applications to
       * reliquish the port if have it open and no longer need it.
       */
      public boolean openConnection ( ) {
         // parameters = new SerialParameters("/dev/ttyS0", 9600, 0, 0,
         // 8, 1,
         // 1);
         // Obtain a CommPortIdentifier object for the port you want to
         // open.
         try {
            portId = CommPortIdentifier.getPortIdentifier( serialPort );
         } catch ( NoSuchPortException e ) {
            logger.error( e.getMessage( ) , e );
            return false;
         }
         
         // Open the port represented by the CommPortIdentifier object.
         // Give
         // the open call a relatively long timeout of 30 seconds to
         // allow
         // a different application to reliquish the port if the user
         // wants to.
         if ( portId.isCurrentlyOwned( ) ) {
            logger.error( "port owned by someone else" );
            return false;
         }
         try {
            sPort = ( SerialPort ) portId.open( "GSNSerialConnection" , 30 * 1000 );
         } catch ( PortInUseException e ) {
            logger.error( e.getMessage( ) , e );
            return false;
         }
         
         // Open the input and output streams for the connection. If they
         // won't
         // open, close the port before throwing an exception.
         try {
            os = sPort.getOutputStream( );
            is = sPort.getInputStream( );
         } catch ( IOException e ) {
            sPort.close( );
            logger.error( e.getMessage( ) , e );
            return false;
         }
         sPort.notifyOnDataAvailable( true );
         sPort.notifyOnBreakInterrupt( false );
         
         // Set receive timeout to allow breaking out of polling loop
         // during
         // input handling.
         try {
            sPort.enableReceiveTimeout( 30 );
         } catch ( UnsupportedCommOperationException e ) {

         }
         open = true;
         return true;
      }
      
      /**
       * Close the port and clean up associated elements.
       */
      public void closeConnection ( ) {
         // If port is alread closed just return.
         if ( !open ) { return; }
         // Check to make sure sPort has reference to avoid a NPE.
         if ( sPort != null ) {
            try {
               os.close( );
               is.close( );
            } catch ( IOException e ) {
               System.err.println( e );
            }
            sPort.close( );
         }
         open = false;
      }
      
      /**
       * Send a one second break signal.
       */
      public void sendBreak ( ) {
         sPort.sendBreak( 1000 );
      }
      
      /**
       * Reports the open status of the port.
       * 
       * @return true if port is open, false if port is closed.
       */
      public boolean isOpen ( ) {
         return open;
      }
      
      public void addEventListener ( SerialPortEventListener listener ) {
         try {
            sPort.addEventListener( listener );
         } catch ( TooManyListenersException e ) {
            sPort.close( );
            logger.warn( e.getMessage( ) , e );
         }
      }
      
      /**
       * Send a byte.
       */
      public void sendByte ( int i ) {
         try {
            os.write( i );
         } catch ( IOException e ) {
            System.err.println( "OutputStream write error: " + e );
         }
      }
      
      public InputStream getInputStream ( ) {
         return is;
      }
      
      public OutputStream getOutputStream ( ) {
         return os;
      }
      
   }
   
   public synchronized boolean sendToWrapper ( Object dataItem ) throws OperationNotSupportedException {
      if (!wnetPort.isOpen( ))
         throw new OperationNotSupportedException("The connection is closed.");
      try {
         if ( dataItem instanceof byte [ ] ) wnetPort.getOutputStream( ).write( ( byte [ ] ) dataItem );
         else { // general case, writes using the printwriter.
            PrintWriter pw = new PrintWriter( wnetPort.getOutputStream( ) );
            pw.write( dataItem.toString( ) );
            pw.flush( );
            pw.close( );
         }
         return true;
      } catch ( IOException e ) {
         logger.warn( "OutputStream write error. " , e );
         return false;
      }
   }
   
   public Collection < DataField > getOutputFormat ( ) {
      return dataField;
   }
   
   public void finalize ( HashMap context ) {
      super.finalize( context );
      wnetPort.closeConnection( );
      threadCounter--;
   }
   
   private static final int MAXBUFFERSIZE = 1024;
   
   private byte [ ]         inputBuffer;
   
   public synchronized void serialEvent ( SerialPortEvent e ) {
      if ( logger.isDebugEnabled( ) ) logger.debug( "Serial wrapper received a serial port event, reading..." );
      if ( !isActive( ) || listeners.isEmpty( ) ) {
         if ( logger.isDebugEnabled( ) ) logger.debug( "Serial wrapper dropped the input b/c there is no listener there or the wrapper is inactive." );
         return;
      }
      // Determine type of event.
      switch ( e.getEventType( ) ) {
         // Read data until -1 is returned.
         case SerialPortEvent.DATA_AVAILABLE :
            /*
             * int index = 0; while (newData != -1) { try { if (is == null) { if
             * (logger.isDebugEnabled ()) logger.debug("SerialWrapper: Warning,
             * is == null !"); is = wnetPort.getInputStream(); } else newData =
             * is.read(); if (newData > -1 && newData < 256) {
             * inputBuffer[index++] = (byte) newData; } } catch (IOException ex) {
             * System.err.println(ex); return; } }
             */
            try {
               is.read( inputBuffer );
            } catch ( IOException ex ) {
               logger.warn( "Serial port wrapper couldn't read data : " + ex );
               return;
            }
            break;
         // If break event append BREAK RECEIVED message.
         case SerialPortEvent.BI :
            // messageAreaIn.append("\n--- BREAK RECEIVED ---\n");
      }
      if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( "Serial port wrapper processed a serial port event, stringbuffer is now : " ).append( inputBuffer ).toString( ) );
      StreamElement streamElement = new StreamElement( new String [ ] { RAW_PACKET } , new Integer [ ] { DataTypes.BINARY } , new Serializable [ ] { inputBuffer } , System.currentTimeMillis( ) );
      postStreamElement( streamElement );
   }
}
