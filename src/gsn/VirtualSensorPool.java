package gsn;

import gsn.beans.VSensorConfig;
import gsn.storage.PoolIsFullException;
import gsn.storage.StorageManager;
import gsn.vsensor.AbstractVirtualSensor;

import java.io.File;
import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class VirtualSensorPool {

	private static final transient Logger         logger          = Logger.getLogger( VirtualSensorPool.class );

	private static final int GARBAGE_COLLECTOR_INTERVAL = 100;

	private int                                   maxPoolSize;

	private int                                   currentPoolSize = 0;

	private String                                processingClassName;

	private ArrayList < AbstractVirtualSensor > allInstances    = new ArrayList < AbstractVirtualSensor >( );

	private ArrayList < AbstractVirtualSensor > idleInstances   = new ArrayList < AbstractVirtualSensor >( );

	private VSensorConfig                         config;

	private long                                  lastModified    = -1;

	private int noOfCallsToReturnVS= 0;
	public VirtualSensorPool ( VSensorConfig config ) {
		if ( logger.isInfoEnabled( ) )
			logger.info( ( new StringBuilder( "Preparing the pool for: " ) ).append( config.getVirtualSensorName( ) ).append( " with the max size of:" ).append( maxPoolSize ).toString( ) );
		this.config = config;
		this.processingClassName = config.getProcessingClass( );
		this.maxPoolSize = config.getLifeCyclePoolSize( );
		this.lastModified = new File( config.getFileName( ) ).lastModified( );
	}

	public synchronized AbstractVirtualSensor borrowVS ( ) throws PoolIsFullException , VirtualSensorInitializationFailedException {
		if ( currentPoolSize == maxPoolSize ) throw new PoolIsFullException( config.getVirtualSensorName( ) );
		currentPoolSize++;
		AbstractVirtualSensor newInstance = null;
		if ( idleInstances.size( ) > 0 )
			newInstance = idleInstances.remove( 0 );
		else
			try {
				newInstance = ( AbstractVirtualSensor ) Class.forName( processingClassName ).newInstance( );
				newInstance.setVirtualSensorConfiguration( config );
				allInstances.add( newInstance );
				if ( newInstance.initialize( ) == false ) {
					returnVS( newInstance );
					throw new VirtualSensorInitializationFailedException( );
				}
			} catch ( InstantiationException e ) {
				logger.error( e.getMessage( ) , e );
			} catch ( IllegalAccessException e ) {
				logger.error( e.getMessage( ) , e );
			} catch ( ClassNotFoundException e ) {
				logger.error( e.getMessage( ) , e );
			}
			if ( logger.isDebugEnabled() )
				logger.debug( new StringBuilder( ).append( "VSPool Of " ).append( config.getVirtualSensorName( ) ).append( " current busy instances : " ).append( currentPoolSize ).toString( ) );
			return newInstance;
	}
	/**
	 * The method ignores the call if the input is null
	 * @param o
	 */
	public synchronized void returnVS ( AbstractVirtualSensor o ) {
		if ( o == null ) return;
		idleInstances.add( o );
		currentPoolSize--;
		if ( logger.isDebugEnabled() )
			logger.debug( new StringBuilder( ).append( "VSPool Of " ).append( config.getVirtualSensorName( ) ).append( " current busy instances : " ).append( currentPoolSize ).toString( ) );
		if (++noOfCallsToReturnVS%GARBAGE_COLLECTOR_INTERVAL==0)
			DoUselessDataRemoval();
			
	}

	public synchronized void closePool ( ) {
		for ( AbstractVirtualSensor o : allInstances )
			o.finalize( );
		if ( logger.isDebugEnabled() ) logger.debug( new StringBuilder( ).append( "The VSPool Of " ).append( config.getVirtualSensorName( ) ).append( " is now closed." ).toString( ) );
	}

	public void start ( ) throws PoolIsFullException , VirtualSensorInitializationFailedException {
		returnVS( borrowVS( ) ); // To initialize the first VS.
	}

	/**
	 * @return the config
	 */
	public VSensorConfig getConfig ( ) {
		return config;
	}

	/**
	 * @return the lastModified
	 */
	public long getLastModified ( ) {
		return lastModified;
	}

	public void finalize ( ) {

	}
	public void DoUselessDataRemoval ( ) {
		if ( config.getParsedStorageSize( ) == VSensorConfig.STORAGE_SIZE_NOT_SET ) return;
		StringBuilder query = uselessDataRemovalQuery();
		int effected = StorageManager.getInstance( ).executeUpdate( query );
		if (logger.isDebugEnabled( ) )
			logger.debug( new StringBuilder( ).append( effected ).append( " old rows dropped from " ).append( config.getVirtualSensorName( ) ).toString( ) );
	}

	/**
	 * @return
	 */
	public StringBuilder uselessDataRemovalQuery() {
		String virtualSensorName = config.getVirtualSensorName( );
		StringBuilder query = null;
		if ( StorageManager.isHsql( ) ) {
			if ( config.isStorageCountBased( ) )
				query = new StringBuilder( ).append( "delete from " ).append( virtualSensorName ).append( " where " ).append( virtualSensorName ).append( ".timed not in ( select " ).append(
						virtualSensorName ).append( ".timed from " ).append( virtualSensorName ).append( " order by " ).append( virtualSensorName ).append( ".timed DESC  LIMIT  " ).append(
								config.getParsedStorageSize( ) ).append( " offset 0 )" );
			else
				query = new StringBuilder( ).append( "delete from " ).append( virtualSensorName ).append( " where " ).append( virtualSensorName ).append( ".timed < (NOW_MILLIS() -" ).append(
						config.getParsedStorageSize( ) ).append( ")" );
		} else if ( StorageManager.isMysqlDB( ) ) {
			if ( config.isStorageCountBased( ) )
				query = new StringBuilder( ).append( "delete from " ).append( virtualSensorName ).append( " where " ).append( virtualSensorName ).append( ".timed <= ( SELECT * FROM ( SELECT timed FROM " )
				.append( virtualSensorName ).append( " group by " ).append( virtualSensorName ).append( ".timed ORDER BY " ).append( virtualSensorName ).append( ".timed DESC LIMIT 1 offset " )
				.append( config.getParsedStorageSize( ) ).append( "  ) AS TMP_" ).append( ( int ) ( Math.random( ) * 100000000 ) ).append( " )" );
			else
				query = new StringBuilder( ).append( "delete from " ).append( virtualSensorName ).append( " where " ).append( virtualSensorName ).append( ".timed < (UNIX_TIMESTAMP()*1000 -" ).append(
						config.getParsedStorageSize( ) ).append( ")" );
		}
		if ( logger.isDebugEnabled( ) ) this.logger.debug( new StringBuilder( ).append( "Enforcing the limit size on the VS table by : " ).append( query ).toString( ) );
		return query;
	}
}

