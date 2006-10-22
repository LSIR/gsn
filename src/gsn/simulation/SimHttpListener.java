package gsn.simulation;

import gsn.vsensor.Container;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class SimHttpListener extends HttpServlet {
   
   private transient File         outputLog = null;
   
   private OutputStream           dos       = null;
   
   private final transient Logger logger    = Logger.getLogger( SimHttpListener.class );
   
   public void doPost ( HttpServletRequest req , HttpServletResponse res ) throws ServletException , IOException {
      int requestType = Integer.parseInt( ( String ) req.getHeader( Container.REQUEST ) );
      switch ( requestType ) {
         case Container.DATA_PACKET :
            res.setHeader( Container.RESPONSE_STATUS , Container.REQUEST_HANDLED_SUCCESSFULLY );
            if ( req.getLocalPort( ) == ( Simulation.START_PORT_INDEX + 1 ) ) {
               if ( outputLog == null ) {
                  outputLog = new File( "SuperLight-ReceivedTimes.log" );
                  try {
                     dos = ( new FileOutputStream( outputLog ) );
                  } catch ( FileNotFoundException e1 ) {
                     logger.error( "Logging the fail failed" , e1 );
                     return;
                  }
               }
               try {
                  if ( logger.isInfoEnabled( ) ) logger.info( "Data received for a typical client" );
                  dos.write( new StringBuffer( ).append( System.currentTimeMillis( ) ).append( '\n' ).toString( ).getBytes( ) );
                  dos.flush( );
               } catch ( IOException e ) {
                  logger.error( "Logging the fail failed" , e );
                  return;
               }
            }
            if ( logger.isDebugEnabled( ) ) logger.debug( "Data Received" );
            break;
         
      }
      
   }
   
}
