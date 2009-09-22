package gsn;

import gsn.beans.InputStream;
import gsn.beans.StreamSource;
import gsn.beans.VSensorConfig;
import gsn.storage.PoolIsFullException;
import gsn.storage.StorageManager;
import gsn.vsensor.AbstractVirtualSensor;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class VirtualSensorPool {

	private static final transient Logger         logger          = Logger.getLogger( VirtualSensorPool.class );

	private static final int GARBAGE_COLLECTOR_INTERVAL = 2;

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
			logger.info( ( new StringBuilder( "Preparing the pool for: " ) ).append( config.getName( ) ).append( " with the max size of:" ).append( maxPoolSize ).toString( ) );
		this.config = config;
		this.processingClassName = config.getProcessingClass( );
		this.maxPoolSize = config.getLifeCyclePoolSize( );
		this.lastModified = new File( config.getFileName( ) ).lastModified( );
	}

	public synchronized AbstractVirtualSensor borrowVS ( ) throws PoolIsFullException , VirtualSensorInitializationFailedException {
		if ( currentPoolSize == maxPoolSize ) throw new PoolIsFullException( config.getName( ) );
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
				logger.debug( new StringBuilder( ).append( "VSPool Of " ).append( config.getName( ) ).append( " current busy instances : " ).append( currentPoolSize ).toString( ) );
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
			logger.debug( new StringBuilder( ).append( "VSPool Of " ).append( config.getName( ) ).append( " current busy instances : " ).append( currentPoolSize ).toString( ) );
		if (++noOfCallsToReturnVS%GARBAGE_COLLECTOR_INTERVAL==0)
			DoUselessDataRemoval();

	}

	public synchronized void closePool ( ) {
		for ( AbstractVirtualSensor o : allInstances )
			o.dispose( );
		if ( logger.isDebugEnabled() ) logger.debug( new StringBuilder( ).append( "The VSPool Of " ).append( config.getName( ) ).append( " is now closed." ).toString( ) );
	}

	public void start ( ) throws PoolIsFullException , VirtualSensorInitializationFailedException {
		for(InputStream inputStream : config.getInputStreams()){
			for(StreamSource streamSource : inputStream.getSources()){
				streamSource.getWrapper().start();
			}
		}
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

	public void dispose ( ) {

	}
	public void DoUselessDataRemoval ( ) {
		if ( config.getParsedStorageSize( ) == VSensorConfig.STORAGE_SIZE_NOT_SET ) return;
		StringBuilder query = uselessDataRemovalQuery();
		int effected = 0;
		try {
			effected = StorageManager.getInstance( ).executeUpdate( query );
		} catch (SQLException e) {
			logger.error("Error in executing: "+query);
			logger.error(e.getMessage(),e);
		}
		if (logger.isDebugEnabled( ) )
			logger.debug( new StringBuilder( ).append( effected ).append( " old rows dropped from " ).append( config.getName( ) ).toString( ) );
	}

	/**
	 * @return
	 */
	public StringBuilder uselessDataRemovalQuery() {
		String virtualSensorName = config.getName( );
		StringBuilder query = null;
		if ( config.isStorageCountBased( ) ){
			if ( StorageManager.isH2( ) ) {
				query = new StringBuilder( ).append( "delete from " ).append( virtualSensorName ).append( " where " ).append( virtualSensorName ).append( ".timed not in ( select " ).append(
						virtualSensorName ).append( ".timed from " ).append( virtualSensorName ).append( " order by " ).append( virtualSensorName ).append( ".timed DESC  LIMIT  " ).append(
								config.getParsedStorageSize( ) ).append( " offset 0 )" );
			} else if ( StorageManager.isMysqlDB( ) ) {
				query = new StringBuilder( ).append( "delete from " ).append( virtualSensorName ).append( " where " ).append( virtualSensorName ).append( ".timed <= ( SELECT * FROM ( SELECT timed FROM " )
				.append( virtualSensorName ).append( " group by " ).append( virtualSensorName ).append( ".timed ORDER BY " ).append( virtualSensorName ).append( ".timed DESC LIMIT 1 offset " )
				.append( config.getParsedStorageSize( ) ).append( "  ) AS TMP)" );
			}else if (StorageManager.isSqlServer()) {
				query = new StringBuilder( ).append( "delete from " ).append( virtualSensorName ).append( " where " ).append( virtualSensorName ).append( ".timed < (select min(timed) from (select top " ).append(config.getParsedStorageSize()).append(
				" * ").append(" from ").append(virtualSensorName).append(" order by ").append(virtualSensorName).append(".timed DESC ) as x ) ");
			}else if (StorageManager.isOracle()) {
				query = new StringBuilder( ).append( "delete from " ).append( virtualSensorName ).append( " where timed <= ( SELECT * FROM ( SELECT timed FROM " )
				.append( virtualSensorName ).append( " group by timed ORDER BY timed DESC) where rownum = " )
				.append( config.getParsedStorageSize( )+1 ).append( " )" );
			}
		}else{
			long timedToRemove = -1;
			Connection conn = null; 
			try {
				ResultSet rs = StorageManager.getInstance().executeQueryWithResultSet(new StringBuilder("SELECT MAX(timed) FROM ").append(virtualSensorName),conn=StorageManager.getInstance().getConnection());
				if(rs.next())
					timedToRemove = rs.getLong(1);
			} catch (SQLException e) {
				logger.error(e.getMessage(), e);
			}finally {
				StorageManager.close(conn);
				
			}
			query = new StringBuilder( ).append( "delete from " ).append( virtualSensorName ).append( " where " ).append( virtualSensorName ).append( ".timed < " ).append(timedToRemove);
			query.append(" - ").append(config.getParsedStorageSize( ) );
		}
		if ( logger.isDebugEnabled( ) ) this.logger.debug( new StringBuilder( ).append( "Enforcing the limit size on the VS table by : " ).append( query ).toString( ) );
		return query;
	}
}

