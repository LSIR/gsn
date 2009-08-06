package gsn.operators;

import gsn.beans.Operator;
import gsn.beans.StreamElement;
import gsn.beans.VSFile;
import gsn.beans.DataField;
import gsn.channels.DataChannel;
import gsn.utils.protocols.ProtocolManager;
import gsn.utils.protocols.EPuck.SerComProtocol;
import gsn.wrappers.Wrapper;
import gsn.wrappers.general.SerialWrapper;
import gsn2.conf.OperatorConfig;

import java.util.List;

import org.apache.log4j.Logger;

public class EPuckVS  implements Operator {
	
	public void process ( String inputStreamName , List<StreamElement> data ) {
		for (StreamElement se: data)
			process(inputStreamName, se);
	}

  public DataField[] getStructure() {
    return new DataField[0];  //To change body of implemented methods use File | Settings | File Templates.
  }

  public void start() {}
	public void stop() {}

   
   private static final transient Logger logger = Logger.getLogger( EPuckVS.class );
   
   private ProtocolManager               protocolManager;
   
   private Wrapper                       wrapper;
   
   private VSFile                 vsensor;

private DataChannel outputChannel;
   
   public EPuckVS(OperatorConfig config,DataChannel outputChannel ) {
		this.outputChannel = outputChannel;
      
//      TODO: wrapper = getVirtualSensorConfiguration( ).getInputStream( "input1" ).getSource( "source1" ).getWrapper( );
      protocolManager = new ProtocolManager( new SerComProtocol( ) , wrapper );
      if ( logger.isDebugEnabled( ) ) logger.debug( "Created protocolManager" );
      try {
//         wrapper.sendToWrapper( "h\n",null,null );
      } catch ( Exception e ) {//OperationNotSupportedException
         e.printStackTrace( );
      }
      // protocolManager.sendQuery( SerComProtocol.RESET , null );
      if ( logger.isDebugEnabled( ) ) logger.debug( "Initialization complete." );

   }
   
   boolean actionA = false;

   public void process ( String inputStreamName , StreamElement data ) {
      if ( logger.isDebugEnabled( ) ) logger.debug( "I just received some data from the robot" );
      System.out.println( new String( ( byte [ ] ) data.getValue( SerialWrapper.RAW_PACKET ) ) );
//      Wrapper wrapper = vsensor.getInputStream( "input1" ).getSource( "source1" ).getWrapper( );
      if ( actionA == false ) {
         actionA = true;
         try {
            // wrapper.sendToWrapper( "h\n" );
//        wrapper.sendToWrapper( "d,1000,-1000\n",null,null );
         } catch ( Exception e ) {//OperationNotSupportedException
            logger.error( e.getMessage( ) , e );
         }
      }
   }
   
   public void dispose ( ) {
      try {
//         vsensor.getInputStream( "input1" ).getSource( "source1" ).getWrapper().sendToWrapper( "R\n",null,null );
      } catch ( Exception e ) {//OperationNotSupportedException 
         logger.error( e.getMessage( ) , e );
      }
   }
   
}
