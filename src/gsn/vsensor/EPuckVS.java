package gsn.vsensor;

import gsn.beans.StreamElement;
import gsn.utils.protocols.ProtocolManager;
import gsn.utils.protocols.EPuck.SerComProtocol;
import gsn.wrappers.StreamProducer;
import gsn.wrappers.general.SerialWrapper;

import java.util.HashMap;
import java.util.TreeMap;

import javax.naming.OperationNotSupportedException;

import org.apache.log4j.Logger;

public class EPuckVS extends AbstractVirtualSensor {
   
   private static final transient Logger logger = Logger.getLogger( EPuckVS.class );
   
   private TreeMap < String , String >   params;
   
   private ProtocolManager protocolManager;
   
   public boolean initialize ( HashMap map ) {
      boolean toReturn = super.initialize( map );
      if ( toReturn == false ) return false;
      params = virtualSensorConfiguration.getMainClassInitialParams( );
      protocolManager = new ProtocolManager(new SerComProtocol());
      if(logger.isDebugEnabled( ))
         logger.debug( "Created protocolManager" );
      // send an initial reset command to put the robot in a clean state
      //StreamProducer wrapper = virtualSensorConfiguration.getInputStream( "input1" ).getSource( "source1" ).getActiveSourceProducer( );
      //protocolManager.sendQuery( SerComProtocol.RESET , null ,  wrapper);
      //protocolManager.sendQuery( SerComProtocol.RESET , null ,  wrapper);
      if(logger.isDebugEnabled())
         logger.debug( "Initialization complete." );
      return true;
   }
   
   boolean actionA = false;
   
   public void dataAvailable ( String inputStreamName , StreamElement data ) {
      if(logger.isDebugEnabled( ))
         logger.debug( "I just received some data from the robot" );
      System.out.println(new String((byte[])data.getData( SerialWrapper.RAW_PACKET )));
      StreamProducer wrapper = virtualSensorConfiguration.getInputStream( "input1" ).getSource( "source1" ).getActiveSourceProducer( );
      if ( actionA == false ) {
         actionA = true;
         try {
            //wrapper.sendToWrapper( "h\n" );
            wrapper.sendToWrapper( "f,2\n" );
         } catch ( OperationNotSupportedException e ) {
            logger.error( e.getMessage( ) , e );
         }
      }
   }

   public void finalize ( HashMap map ) {
      try {
         virtualSensorConfiguration.getInputStream( "input1" ).getSource( "source1" ).getActiveSourceProducer( ).sendToWrapper( "R\n" );
      } catch ( OperationNotSupportedException e ) {
         logger.error( e.getMessage( ) , e );
      }
   }
   
   
}
