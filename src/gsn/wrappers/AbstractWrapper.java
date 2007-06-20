package gsn.wrappers;

import gsn.Main;
import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.beans.StreamSource;
import gsn.storage.StorageManager;
import gsn.utils.GSNRuntimeException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.naming.OperationNotSupportedException;
import org.apache.log4j.Logger;

public abstract class AbstractWrapper extends Thread {

	private final static transient Logger      logger         = Logger.getLogger( AbstractWrapper.class );

	protected final List < StreamSource > listeners      = Collections.synchronizedList(new ArrayList < StreamSource >( ));

	private AddressBean                        activeAddressBean;

	private boolean                            isActive       = true;

	public static final int GARBAGE_COLLECT_AFTER_SPECIFIED_NO_OF_ELEMENTS = 2;
	/**
	 * Returns the view name created for this listener.
	 * Note that, GSN creates one view per listener.
	 * @throws SQLException 
	 */
	public void addListener ( StreamSource ss ) throws SQLException {
		getStorageManager( ).executeCreateView( ss.getUIDStr() , ss.toSql() );
		listeners.add(ss);
		if (logger.isDebugEnabled())
			logger.debug("Adding listeners: "+ss.toString());
	}

	/**
	 * Removes the listener with it's associated view.
	 * @throws SQLException 
	 */
	public void removeListener ( StreamSource ss ) throws SQLException {
		listeners.remove( ss );
		getStorageManager( ).executeDropView( ss.getUIDStr() );
		if (listeners.size()==0) 
			releaseResources();
	}

	/**
	 * @return the listeners
	 */
	public List < StreamSource > getListeners ( ) {
		return listeners;
	}

	protected StorageManager getStorageManager ( ) {
		return StorageManager.getInstance( );

	}

	/**
	 * This method is called whenever the wrapper wants to send a data item back
	 * to the source where the data is coming from. For example, If the data is
	 * coming from a wireless sensor network (WSN), This method sends a data item
	 * to the sink node of the virtual sensor. So this method is the
	 * communication between the System and actual source of data. The data sent
	 * back to the WSN could be a command message or a configuration message.
	 * 
	 * @param dataItem : The data which is going to be send to the source of the
	 * data for this wrapper.
	 * @return True if the send operation is successful.
	 * @throws OperationNotSupportedException If the wrapper doesn't support
	 * sending the data back to the source. Note that by default this method
	 * throws this exception unless the wrapper overrides it.
	 */

	public boolean sendToWrapper ( String action , String [ ] paramNames , Object [ ] paramValues ) throws OperationNotSupportedException {
		throw new OperationNotSupportedException( "This wrapper doesn't support sending data back to the source." );
	}

	public final AddressBean getActiveAddressBean ( ) {
		if (this.activeAddressBean==null) {
			throw new RuntimeException("There is no active address bean associated with the wrapper.");
		}
		return activeAddressBean;
	}

	/**
	 * Only sets if there is no other activeAddressBean configured.
	 * @param newVal the activeAddressBean to set
	 */
	public void setActiveAddressBean ( AddressBean newVal ) {
		if (this.activeAddressBean!=null) {
			throw new RuntimeException("There is already an active address bean associated with the wrapper.");
		}
		this.activeAddressBean = newVal;
	}

	private final transient int aliasCode = Main.tableNameGenerator( );
	private final CharSequence aliasCodeS = Main.tableNameGeneratorInString( aliasCode );

	private long noOfCallsToPostSE = 0;

	public int getDBAlias ( ) {
		return aliasCode;
	}
	public CharSequence getDBAliasInStr() {
		return aliasCodeS;
	}

	public abstract  DataField [] getOutputFormat ( );

	public boolean isActive ( ) {
		return isActive;
	}

	protected void postStreamElement ( Serializable... values  ) {
		StreamElement se = new StreamElement(getOutputFormat(),values,System.currentTimeMillis());
		postStreamElement(se);
	}

	/**
	 * This method gets the generated stream element and notifies the input streams if needed.
	 * The return value specifies if the newly provided stream element generated
	 * at least one input stream notification or not.
	 * @param streamElement
	 * @return If the method returns false, it means the insertion doesn't effected any input stream.
	 */

	protected Boolean postStreamElement ( StreamElement streamElement ) {
		try {
			if (!isActive() || listeners.size()==0)
				return false;
			if (!insertIntoWrapperTable(streamElement))
				return false;
			boolean toReturn = false;
			synchronized ( listeners ) {
				if (logger.isDebugEnabled()) logger.debug("Size of the listeners to be evaluated - "+listeners.size() );
				for (  StreamSource ss: listeners ) {
					if( getStorageManager( ).isThereAnyResult( new StringBuilder("select * from ").append(ss.getUIDStr()) )) {
						if ( logger.isDebugEnabled() ) logger.debug( getWrapperName()+ " - Output stream produced/received from a wrapper "+ss.toString() );
						try {
							ss.dataAvailable( );
						} catch (SQLException e) {
							logger.error(e.getMessage(),e);
							return false;
						}
						toReturn=true;
					}
				}
			}
			if (++noOfCallsToPostSE%GARBAGE_COLLECT_AFTER_SPECIFIED_NO_OF_ELEMENTS==0) {
				int removedRaws = removeUselessValues();
			}
			return toReturn;		
		}catch (Exception e) {
			logger.error(e.getMessage(),e);
			logger.error("Produced data item from the wrapper couldn't be propagated inside the system.");
			return false;
		}
	}
	/**
	 * Updates the table representing the data items produced by the stream element.
	 * Returns false if the update fails or doesn't change the state of the table.
	 * 
	 * @param se Stream element to be inserted to the table if needed.
	 * @return true if the stream element is successfully inserted into the table.
	 * @throws SQLException 
	 */
	public boolean insertIntoWrapperTable(StreamElement se) throws SQLException {
		if (listeners.size()==0)
			return false;
		getStorageManager( ).executeInsert( aliasCodeS , getOutputFormat(),se );
		return true;
	}
	/**
	 * This method is called whenever the wrapper wants to send a data item back
	 * to the source where the data is coming from. For example, If the data is
	 * coming from a wireless sensor network (WSN), This method sends a data item
	 * to the sink node of the virtual sensor. So this method is the
	 * communication between the System and actual source of data. The data sent
	 * back to the WSN could be a command message or a configuration message.
	 * 
	 * @param dataItem : The data which is going to be send to the source of the
	 * data for this wrapper.
	 * @return True if the send operation is successful.
	 * @throws OperationNotSupportedException If the wrapper doesn't support
	 * sending the data back to the source. Note that by default this method
	 * throws this exception unless the wrapper overrides it.
	 */

	public boolean sendToWrapper ( Object dataItem ) throws OperationNotSupportedException {
		if (isActive==false)
			throw new GSNRuntimeException("Sending to an inactive/disabled wrapper is not allowed !");
		throw new OperationNotSupportedException( "This wrapper doesn't support sending data back to the source." );
	}


	/**
	 * Removes all the listeners, drops the views representing them, drops the sensor table,
	 * stops the TableSizeEnforce thread.
	 *
	 */
	public StringBuilder getUselessWindow() {
		int maxCountSize = -1;
		int maxTimeSizeInMSec = -1;
		synchronized (listeners) {
			for (StreamSource ss:listeners)
				if (ss.isStorageCountBased())
					maxCountSize=Math.max(maxCountSize, ss.getParsedStorageSize());
				else
					maxTimeSizeInMSec=Math.max(maxTimeSizeInMSec,ss.getParsedStorageSize());
		}
		if (maxCountSize<=0 && maxTimeSizeInMSec<=0)
			return null;
		StringBuilder sb = new StringBuilder("delete from ").append(getDBAliasInStr()).append(" where " );
		if (maxTimeSizeInMSec>0) {
			sb.append(" timed < (");
			if (StorageManager.isHsql())
				sb.append("NOW_MILLIS() - ");
			else if (StorageManager.isMysqlDB())
				sb.append("UNIX_TIMESTAMP()*1000 - ");
			else if (StorageManager.isSqlServer())
				sb.append("(convert(bigint,datediff(second,'1/1/1970',current_timestamp))*1000 ) - ");
			sb.append(maxTimeSizeInMSec).append(" )");
		}
		if (maxCountSize>0 && maxTimeSizeInMSec>0)
			sb.append(" AND ");
		if (maxCountSize>0 ) {
			if (StorageManager.isHsql()|| StorageManager.isMysqlDB()) {
			sb.append(" timed < (select min(timed) from (select timed from ").append(getDBAliasInStr());
			sb.append(" order by timed desc limit 1 offset ").append(maxCountSize).append(" ) as X )");
			}else if (StorageManager.isSqlServer()) {
				sb.append(" timed < (select min(timed) from (select top " ).append(maxCountSize).append(" * ").append(" from ").append(getDBAliasInStr()).append(")as x ) ");
			}
		}
		return sb;
	}

	public int removeUselessValues() throws SQLException {
		StringBuilder query = getUselessWindow();
		if (query==null)
			return 0;
		if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( ).append( "RESULTING QUERY FOR Table Size Enforce " ).append( query ).toString( ) );
		int deletedRows =StorageManager.getInstance().executeUpdate(query);
		if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( ).append( deletedRows ).append( " old rows dropped from " ).append( getDBAliasInStr() ).toString( ) );
		return deletedRows;
	}

	public void releaseResources ( ) throws SQLException {
		isActive = false;
		finalize();
		if ( logger.isInfoEnabled() ) logger.info( "Finalized called" );
		synchronized ( listeners ) {
			Iterator<StreamSource> list =listeners.iterator();
			while (list.hasNext()) {
				StreamSource ss = list.next();
				getStorageManager( ).executeDropView( ss.getUIDStr() );
				list.remove();
			}
		}
		getStorageManager( ).executeDropTable( aliasCodeS );
	}

	public static final String TIME_FIELD = "timed";

	/**
	 * The addressing is provided in the ("ADDRESS",Collection<KeyValue>). If
	 * the DataSource can't initialize itself because of either internal error or
	 * inaccessibility of the host specified in the address the method returns
	 * false. The dbAliasName of the DataSource is also specified with the
	 * "DBALIAS" in the context. The "STORAGEMAN" points to the StorageManager
	 * which should be used for querying.
	 * 
	 * @return True if the initialization do successfully otherwise false;
	 */

	public abstract boolean initialize ( );

	public abstract void finalize ( );

	public abstract String getWrapperName ( );



}
