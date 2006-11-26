package gsn.vsensor;

import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.gui.JHCIProtocolControl;
import gsn.utils.protocols.AbstractHCIProtocol;
import gsn.utils.protocols.ProtocolManager;
import gsn.wrappers.AbstractWrapper;
import java.util.TreeMap;
import org.apache.log4j.Logger;

/**
 * @author Jerome Rousselot <jerome.rousselot@csem.ch>
 */
public class HCIProtocolGUIVS extends AbstractVirtualSensor {
   
   private static final transient Logger logger        = Logger.getLogger( HCIProtocolGUIVS.class );
   
   private TreeMap < String , String >   params;
   
   private ProtocolManager               protocolManager;
   
   private AbstractHCIProtocol           protocol;
   
   private AbstractWrapper                       outputWrapper = null;
   
   private JHCIProtocolControl           gui;
   
   private VSensorConfig                 vsensor;
   
   public boolean initialize ( ) {
      params = getVirtualSensorConfiguration( ).getMainClassInitialParams( );
      ClassLoader loader = getClass( ).getClassLoader( );
      try {
         Class protocolClass = loader.loadClass( params.get( "HCIProtocolClass" ) );
         protocol = ( AbstractHCIProtocol ) protocolClass.newInstance( );
      } catch ( InstantiationException e ) {
         logger.error( e );
         return false;
      } catch ( IllegalAccessException e ) {
         logger.error( e );
         return false;
      } catch ( ClassNotFoundException e ) {
         logger.error( e );
         return false;
      }
      outputWrapper = vsensor.getInputStream( "input1" ).getSource( "source1" ).getActiveSourceProducer( );
      protocolManager = new ProtocolManager( protocol , outputWrapper );
      if ( logger.isDebugEnabled( ) && protocol != null && protocolManager != null ) logger.debug( "Successfully loaded protocol class " + params.get( "HCIProtocolClass" ) );
      // Then, create GUI
      gui = new JHCIProtocolControl( protocolManager );
      return true;
   }
   
   public void dataAvailable ( String inputStreamName , StreamElement streamElement ) {
      
      gui.displayData( inputStreamName , streamElement );
   }
   
   public void finalize ( ) {

   }
}
