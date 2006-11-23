package gsn.vsensor;

import gsn.Mappings;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;

import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public abstract class AbstractProcessingClass {
   
   private static final transient Logger logger           = Logger.getLogger( AbstractProcessingClass.class );
   
   private VSensorConfig                 virtualSensorConfiguration;
   
   private ArrayList < StreamElement >   producedData     = new ArrayList < StreamElement >( );
   
   private long                          lastVisitiedTime = 0;
   
   /**
    * Called once while initializing an instance of the virtual sensor
    * 
    * @return True if the initialization is done successfully.
    */
   public abstract boolean initialize ( );
   
   private void validateStreamElement ( StreamElement streamElement ) {
      if ( !compatibleStructure( streamElement.getFieldTypes( ) , getVirtualSensorConfiguration( ).getOutputStructure( ) ) ) {
         StringBuilder exceptionMessage = new StringBuilder( ).append( "The streamElement produced by :" ).append( getVirtualSensorConfiguration( ).getVirtualSensorName( ) ).append(
            " Virtual Sensor is not compatible with the defined streamElement.\n" );
         exceptionMessage.append( "The expected stream element structure (specified in " ).append( getVirtualSensorConfiguration( ).getFileName( ) ).append( " is [" );
         for ( DataField df : getVirtualSensorConfiguration( ).getOutputStructure( ) ) {
            exceptionMessage.append( df.getFieldName( ) ).append( " (" ).append( DataTypes.TYPE_NAMES[ df.getDataTypeID( ) ] ).append( ") , " );
         }
         exceptionMessage.append( "] but the actual stream element received from the " + getVirtualSensorConfiguration( ).getVirtualSensorName( ) ).append( " has the [" );
         for ( int i = 0 ; i < streamElement.getFieldNames( ).length ; i++ )
            exceptionMessage.append( streamElement.getFieldNames( )[ i ] ).append( "(" ).append( DataTypes.TYPE_NAMES[ streamElement.getFieldTypes( )[ i ] ] ).append( ")," ).append(
               " ] thus the stream element dropped !!!" );
         throw new RuntimeException( exceptionMessage.toString( ) );
      }
   }
   
   protected synchronized void dataProduced ( StreamElement streamElement ) {
      try {
         validateStreamElement( streamElement );
      } catch ( Exception e ) {
         logger.error( e.getMessage( ) , e );
         return;
      }
      
      if ( !streamElement.isTimestampSet( ) ) streamElement.setTimeStamp( System.currentTimeMillis( ) );
      
      final int outputStreamRate = getVirtualSensorConfiguration( ).getOutputStreamRate( );
      final long currentTime = System.currentTimeMillis( );
      if ( ( currentTime - lastVisitiedTime ) < outputStreamRate ) {
         if ( logger.isInfoEnabled( ) ) logger.info( "Called by *discarded* b/c of the rate limit reached." );
         return;
      }
      lastVisitiedTime = currentTime;
      synchronized ( producedData ) {
         producedData.add( streamElement );
      }
      Mappings.getContainer( ).publishData( this );
   }
   
   private static boolean compatibleStructure ( Integer [ ] fieldTypes , ArrayList < DataField > outputStructure ) {
      if ( outputStructure.size( ) != fieldTypes.length ) {
         logger.warn( "Validation problem, the number of field doesn't match the number of output data strcture of the virtual sensor" );
         return false;
      }
      for ( int i = 0 ; i < outputStructure.size( ) ; i++ ) {
         if ( fieldTypes[ i ] != outputStructure.get( i ).getDataTypeID( ) ) {
            logger.warn( "Validation problem for output field >" + outputStructure.get( i ).getFieldName( ) + ", The field type declared as >" + DataTypes.TYPE_NAMES[ fieldTypes[ i ] ]
               + "< within the stream element but it is defined as in the VSD as : " + DataTypes.TYPE_NAMES[ outputStructure.get( i ).getDataTypeID( ) ] );
            return false;
         }
      }
      return true;
   }
   
   public synchronized StreamElement getData ( ) {
      StreamElement toReturn;
      synchronized ( producedData ) {
         toReturn = producedData.remove( 0 );
      }
      return toReturn;
   }
   
   /**
    * Called when the container want to stop the pool and remove it's resources.
    * The container will call this method once on each install of the virtual
    * sensor in the pool. The progrmmer should release all the resouce used by
    * this virtual sensor instance in this method specially those resouces
    * aquired during the <code>initialize</code> call. <p/> Called once while
    * finalizing an instance of the virtual sensor
    */
   public abstract void finalize ( );
   
   public void dataFromWeb ( String data ) {
      return;
   }
   
   /**
    * @return the virtualSensorConfiguration
    */
   public VSensorConfig getVirtualSensorConfiguration ( ) {
      if ( virtualSensorConfiguration == null ) { throw new RuntimeException( "The VirtualSensorParameter is not set !!!" ); }
      return virtualSensorConfiguration;
   }
   
   /**
    * @param virtualSensorConfiguration the virtualSensorConfiguration to set
    */
   public void setVirtualSensorConfiguration ( VSensorConfig virtualSensorConfiguration ) {
      this.virtualSensorConfiguration = virtualSensorConfiguration;
   }
   
   /*
    * This method is going to be called by the container when one of the input
    * streams has a data to be delivered to this virtual sensor. After receiving
    * the data, the virutal sensor can do the processing on it and this
    * processing could possibly result in producing a new stream element in this
    * virtual sensor in which case the virutal sensor will notify the container
    * by simply adding itself to the list of the virtual sensors which have
    * produced data. (calling <code>container.publishData(this)</code>. For
    * more information please check the <code>AbstractVirtalSensor</code>
    * @param inputStreamName is the name of the input stream as specified in the
    * configuration file of the virtual sensor. @param inputDataFromInputStream
    * is actually the real data which is produced by the input stream and should
    * be delivered to the virtual sensor for possible processing.
    */
   public abstract void dataAvailable ( String inputStreamName , StreamElement streamElement );
}
