package gsn.wrappers;

import gsn.Mappings;
import gsn.VirtualSensorInitializationFailedException;
import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.beans.StreamSource;
import gsn.beans.VSensorConfig;
import gsn.notifications.InGSNNotification;
import gsn.notifications.NotificationRequest;
import gsn.storage.PoolIsFullException;
import gsn.vsensor.AbstractVirtualSensor;
import java.io.Serializable;
import java.sql.SQLException;
import javax.naming.OperationNotSupportedException;
import org.apache.log4j.Logger;

public class InVMPipeWrapper extends AbstractWrapper {
	
  private static transient Logger                                      logger                             = Logger.getLogger( InVMPipeWrapper.class );
  
  public void finalize ( ) {
    
  }
  
  public DataField [ ] getOutputFormat ( ) {
    return config.getOutputStructure( );
  }
  
  public String getWrapperName ( ) {
    return "InVM Pipe Wrapper";
  }
  
  private VSensorConfig          config;
  
  private NotificationRequest    notificationRequest;
  
  public boolean initialize ( ) {
    String remoteVSName = getActiveAddressBean( ).getPredicateValue( "name" );
    if ( remoteVSName == null ) {
      logger.warn( "The \"NAME\" paramter of the AddressBean which corresponds to the local Virtual Sensor is missing, Initialization failed." );
      return false;
    }
    this.config = Mappings.getVSensorConfig( remoteVSName.toLowerCase( ).trim( ) );
    if ( this.config == null ) {
      logger.warn( "The Requested virtual sensor is not available (or not loaded yet), initialization failed !" );
      return false;
    }
    setUsingRemoteTimestamp(true);
    return true;
  }
  
  public void addListener ( StreamSource ss ) throws SQLException {
    /**
     * First we create a view over the main source
     * (config.getVirtualSensorName). We encode all the conditions and
     * filtering in the view's definition. We perform select * from the view.
     */
    super.addListener( ss );
    notificationRequest = new InGSNNotification( this , config.getName( ) );
    Mappings.getContainer( ).addNotificationRequest( config.getName( ) , notificationRequest );
  }
  //TODO : Remove Listener is missing !
  
  public boolean sendToWrapper ( String action,String[] paramNames, Serializable[] paramValues ) throws OperationNotSupportedException {
    AbstractVirtualSensor vs;
    try {
      vs = Mappings.getVSensorInstanceByVSName( config.getName( ) ).borrowVS( );
    } catch ( PoolIsFullException e ) {
      logger.warn( "Sending data back to the source virtual sensor failed !: "+e.getMessage( ),e);
      return false;
    } catch ( VirtualSensorInitializationFailedException e ) {
      logger.warn("Sending data back to the source virtual sensor failed !: "+e.getMessage( ),e);
      return false;
    }
    boolean toReturn = vs.dataFromWeb( action , paramNames , paramValues );
    Mappings.getVSensorInstanceByVSName( config.getName( ) ).returnVS( vs );
    return toReturn;
  }
  
  public boolean remoteDataReceived ( StreamElement se) {
	return postStreamElement(se);
  }
  
  public String toString ( ) {
    StringBuilder sb = new StringBuilder( "InVMPipeWrapper, " );
    sb.append( " RemoteVS : " ).append( config.getName( ) );
    return sb.toString( );
  }
  
}
