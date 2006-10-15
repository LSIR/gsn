package gsn.vsensor;

import gsn.Mappings;
import gsn.beans.VSensorConfig;
import gsn.control.VSensorInstance;
import gsn.storage.PoolIsFullException;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class VirtualSensorPool {
   
   public static final String             VSENSORCONFIG   = "VSENSORCONFIG";
   
   public static final String             CONTAINER       = "CONTAINER";
   
   private static final transient Logger  logger          = Logger.getLogger( VirtualSensorPool.class );
   
   private static final transient boolean isDebugEnabled  = logger.isDebugEnabled( );
   
   private int                            maxPoolSize;
   
   private int                            currentPoolSize = 0;
   
   private String                         processingClassName;
   
   private ArrayList < VirtualSensor >    allInstances    = new ArrayList < VirtualSensor >( );
   
   private ArrayList < VirtualSensor >    idleInstances   = new ArrayList < VirtualSensor >( );
   
   private VSensorConfig                  config;
   
   public VirtualSensorPool ( VSensorInstance instance ) {
      if ( logger.isInfoEnabled( ) ) logger.info( ( new StringBuilder( "Preparing the pool for: " ) ).append( instance.getConfig( ).getVirtualSensorName( ) ).append( " with the max size of:" )
            .append( maxPoolSize ).toString( ) );
      this.config = instance.getConfig( );
      this.processingClassName = instance.getConfig( ).getProcessingClass( );
      this.maxPoolSize = instance.getConfig( ).getLifeCyclePoolSize( );
   }
   
   public synchronized VirtualSensor borrowObject ( ) throws PoolIsFullException , VirtualSensorInitializationFailedException {
      if ( currentPoolSize == maxPoolSize ) throw new PoolIsFullException( config.getVirtualSensorName( ) );
      currentPoolSize++;
      VirtualSensor newInstance = null;
      if ( idleInstances.size( ) > 0 ) newInstance = idleInstances.remove( 0 );
      else
         try {
            newInstance = ( VirtualSensor ) Class.forName( processingClassName ).newInstance( );
            allInstances.add( newInstance );
            HashMap hashMap = new HashMap( );
            hashMap.put( CONTAINER , Mappings.getContainer( ) );
            hashMap.put( VSENSORCONFIG , config );
            if ( newInstance.initialize( hashMap ) == false ) {
               returnInstance( newInstance );
               throw new VirtualSensorInitializationFailedException( );
            }
         } catch ( InstantiationException e ) {
            logger.error( e.getMessage( ) , e );
         } catch ( IllegalAccessException e ) {
            logger.error( e.getMessage( ) , e );
         } catch ( ClassNotFoundException e ) {
            logger.error( e.getMessage( ) , e );
         }
      if ( isDebugEnabled ) logger.debug( new StringBuilder( ).append( "VSPool Of " ).append( config.getVirtualSensorName( ) ).append( " current busy instances : " ).append( currentPoolSize )
            .toString( ) );
      return newInstance;
   }
   
   public synchronized void returnInstance ( VirtualSensor o ) {
      if ( o == null ) return;
      idleInstances.add( o );
      currentPoolSize--;
      if ( isDebugEnabled ) logger.debug( new StringBuilder( ).append( "VSPool Of " ).append( config.getVirtualSensorName( ) ).append( " current busy instances : " ).append( currentPoolSize )
            .toString( ) );
   }
   
   public synchronized void closePool ( ) {
      HashMap map = new HashMap( ); // The default context for closing a
      // pool.
      closePool( map );
   }
   
   public synchronized void closePool ( HashMap map ) {
      for ( VirtualSensor o : allInstances )
         o.finalize( map );
      if ( isDebugEnabled ) logger.debug( new StringBuilder( ).append( "The VSPool Of " ).append( config.getVirtualSensorName( ) ).append( " is now closed." ).toString( ) );
   }
}
