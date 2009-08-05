package gsn;

import static org.picocontainer.behaviors.Behaviors.caching;
import static org.picocontainer.behaviors.Behaviors.synchronizing;
import gsn.beans.Operator;
import gsn.beans.VSFile;
import gsn.storage.StorageManager;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoBuilder;

public class VirtualSensorPool {

	private static final transient Logger         logger          = Logger.getLogger( VirtualSensorPool.class );

	private static final int GARBAGE_COLLECTOR_INTERVAL = 2;

	private String                                processingClassName;

	private long                                  lastModified    = -1;

	private int noOfCallsToReturnVS= 0;

	private MutablePicoContainer picoVS;

	private VSFile config;

	public VirtualSensorPool ( VSFile config ) {
		if ( logger.isInfoEnabled( ) )
			logger.info( ( new StringBuilder( "Preparing the pool for: " ) ).append( config.getName( ) ).toString( ) );

		picoVS = new PicoBuilder().withBehaviors(synchronizing(), caching()).withLifecycle().build();
		picoVS.addComponent(Operator.class, config.getProcessingClassConfig().getClassName());
		picoVS.addComponent(config.getProcessingClassConfig());
		picoVS.setName("VS-Pico:"+config.getName());
		this.config= config;
		this.lastModified = new File( config.getFileName( ) ).lastModified( );
	}

	public synchronized Operator borrowVS ( ) throws VirtualSensorInitializationFailedException {
		return picoVS.getComponent(Operator.class);
	}


	public synchronized void closePool ( ) {
		picoVS.dispose();
		if ( logger.isDebugEnabled() ) logger.debug( new StringBuilder( ).append( "The VSPool Of: " ).append(picoVS.toString()).toString( ) );
	}

	public VSFile getConfig ( ) {
		return config;
	}

	public long getLastModified ( ) {
		return lastModified;
	}

	public void DoUselessDataRemoval ( ) {
		if ( config.getParsedStorageSize( ) == VSFile.STORAGE_SIZE_NOT_SET ) return;
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

