package gsn.vsensor;

import gsn.beans.StreamElement;
import gsn.beans.DataTypes;
import gsn.beans.VSensorConfig;
import gsn.utils.protocols.ProtocolManager;
import gsn.utils.protocols.EPuck.SerComProtocol;
import gsn.wrappers.StreamProducer;
import gsn.wrappers.general.SerialWrapper;

import java.util.HashMap;
import java.util.TreeMap;
import java.io.Serializable;


import javax.naming.OperationNotSupportedException;

import org.apache.log4j.Logger;

public class GPSNMEAVS extends AbstractVirtualSensor {
   
   private static final transient Logger logger = Logger.getLogger( EPuckVS.class );
   
   private TreeMap < String , String >   params;
   
   private ProtocolManager protocolManager;
   
   private StreamProducer wrapper;
   
   private VSensorConfig vsensor;
   
   public boolean initialize ( HashMap map ) {
      boolean toReturn = super.initialize( map );
      if ( toReturn == false ) return false;
      vsensor = ((VSensorConfig) map.get( VirtualSensorPool.VSENSORCONFIG ));
      params = vsensor.getMainClassInitialParams( );
      wrapper = vsensor.getInputStream( "input1" ).getSource( "source1" ).getActiveSourceProducer( );
      protocolManager = new ProtocolManager(new SerComProtocol(), wrapper);
      if(logger.isDebugEnabled( ))
         logger.debug( "Created protocolManager" );
      try {
         wrapper.sendToWrapper( "h\n" );
      } catch ( OperationNotSupportedException e ) {
         e.printStackTrace();
      }
//      protocolManager.sendQuery( SerComProtocol.RESET , null );
      if(logger.isDebugEnabled())
         logger.debug( "Initialization complete." );
      return true;
   }
   
   boolean actionA = false;
   
   public void dataAvailable ( String inputStreamName , StreamElement data ) {
      if(logger.isDebugEnabled( ))
         logger.debug( "Got data!" );
      //System.out.println(new String((byte[])data.getData( SerialWrapper.RAW_PACKET )));
      StreamProducer wrapper = vsensor.getInputStream( "input1" ).getSource( "source1" ).getActiveSourceProducer( );
      if ( actionA == false ) {
         actionA = true;
         try {
            //wrapper.sendToWrapper( "h\n" );
            wrapper.sendToWrapper( "d,1000,-1000\n" );
         } catch ( OperationNotSupportedException e ) {
            logger.error( e.getMessage( ) , e );
         }
      }
      
      
      String [ ] fieldNames = new String [ ] { "latitude" , "longitude" };
      Integer [ ] fieldTypes = new Integer [ 2 ];
      fieldTypes[0] = DataTypes.DOUBLE;
      fieldTypes[1] = DataTypes.DOUBLE;
      Serializable [ ] outputData = new Serializable [ fieldNames.length ];
      
      String s = new String((byte[])data.getData( SerialWrapper.RAW_PACKET ));
      String[] line = s.split("\n");
      for (int i = 0 ; i < line.length ; i++) {
			String[] part = line[i].split(",");
	        if (part[0].equals("$GPRMC")) {
	        	Double d = Double.valueOf(part[3]);
	        	Double lat = d / 100.0;
	        	lat = Math.floor(lat);
	        	lat += Double.valueOf(d % 100.0) / 60.0;
	        	if (part[4].equals("S")) lat = -lat;
	        	else if (!part[4].equals("N")) continue; //invalid format
	        	
	        	d = Double.valueOf(part[5]);
	        	Double lon = Math.floor(d / 100.0);
	        	lon += Double.valueOf(d % 100.0) / 60.0;
	        	if (part[6].equals("W")) lon = -lon; 
	        	
	        	outputData[ 0 ] = lat;
	            outputData[ 1 ] = lon;
	            
	            logger.debug( "latitude:"+ lat+" longitude:"+lon);
	            StreamElement output = new StreamElement( fieldNames , fieldTypes , outputData , System.currentTimeMillis( ) );
	            dataProduced( output );
	            break;
	        }
	    }

   }

   public void finalize ( HashMap map ) {
      try {
         vsensor.getInputStream( "input1" ).getSource( "source1" ).getActiveSourceProducer( ).sendToWrapper( "R\n" );
      } catch ( OperationNotSupportedException e ) {
         logger.error( e.getMessage( ) , e );
      }
   }
   
   
}
