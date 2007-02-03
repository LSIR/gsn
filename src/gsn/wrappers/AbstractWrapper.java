package gsn.wrappers;

import gsn.Main;
import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.beans.StreamSource;
import gsn.beans.TestStreamSource;
import gsn.storage.SQLUtils;
import gsn.storage.StorageManager;
import gsn.utils.CaseInsensitiveComparator;
import gsn.utils.GSNRuntimeException;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;
import javax.naming.OperationNotSupportedException;
import org.apache.log4j.Logger;

import com.mysql.jdbc.PreparedStatement;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 * Date: Aug 4, 2005 <br>
 */
public abstract class AbstractWrapper extends Thread {


	/**
	 * Used by the data source when it wants to insert the data into it's main
	 * database. The AbstractDataSource should keep track of this data source so
	 * that it can release it when <code>finialize</code> called.
	 */
	private final static transient Logger      logger         = Logger.getLogger( AbstractWrapper.class );

	protected final ArrayList < StreamSource > listeners      = new ArrayList < StreamSource >( );

	private AddressBean                        activeAddressBean;

	private boolean                            isActive       = true;

	private static final int GARBAGE_COLLECT_AFTER_SPECIFIED_NO_OF_ELEMENTS = 100;
	/**
	 * Returns the view name created for this listener.
	 * Note that, GSN creates one view per listener.
	 */
	public void addListener ( StreamSource ss ) {
		getStorageManager( ).createView( ss.getUIDStr() , ss.toSql() );
		listeners.add(ss);
	}

	/**
	 * Removes the listener with it's associated view.
	 */
	public void removeListener ( StreamSource ss ) {
		listeners.remove( ss );
		getStorageManager( ).dropView( ss.getUIDStr() );
	}

	/**
	 * @return the listeners
	 */
	public ArrayList < StreamSource > getListeners ( ) {
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
	 * @param activeAddressBean the activeAddressBean to set
	 */
	public void setActiveAddressBean ( AddressBean activeAddressBean ) {
		if (this.activeAddressBean!=null) {
			throw new RuntimeException("There is already an active address bean associated with the wrapper.");
		}
		this.activeAddressBean = activeAddressBean;
	}

	private final transient int aliasCode = Main.tableNameGenerator( );
	private final CharSequence aliasCodeS = Main.tableNameGeneratorInString( aliasCode );

	private long noOfCallsToPostSE;

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
		if (!isActive() || listeners.size()==0)
			return false;
		if (!insertIntoWrapperTable(streamElement))
			return false;
		synchronized ( listeners ) {
			for (  StreamSource ss: listeners ) {
				if( getStorageManager( ).isThereAnyResult( new StringBuilder("select * from ").append(ss.getUIDStr()) )) {
					if ( logger.isDebugEnabled() == true ) logger.debug( "Output stream produced/received from a wrapper" );
					return ss.dataAvailable( );
				}
			}
		}
		if (++noOfCallsToPostSE%GARBAGE_COLLECT_AFTER_SPECIFIED_NO_OF_ELEMENTS==0)
			removeUselessValues();
		return false;		
	}
	/**
	 * Updates the table representing the data items produced by the stream element.
	 * Returns false if the update fails or doesn't change the state of the table.
	 * 
	 * @param se Stream element to be inserted to the table if needed.
	 * @return true if the stream element is successfully inserted into the table.
	 */
	public boolean insertIntoWrapperTable(StreamElement se) {
		if (listeners.size()==0)
			return false;
		boolean result = getStorageManager( ).insertData( aliasCodeS , se );
		if ( result == false ) {
			logger.warn( "Inserting the following data item failed : " + se );
			return false;
		}
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
	public CharSequence getUselessWindow() {
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
			sb.append(maxTimeSizeInMSec).append(" )");
		}
		if (maxCountSize>0 && maxTimeSizeInMSec>0)
			sb.append(" AND ");
		if (maxCountSize>0 ) {
			sb.append(" timed < ( select * from (select timed from ").append(getDBAliasInStr());
			sb.append(" order by timed desc limit 1 offset ").append(maxCountSize).append(" ) as X)");
		}
		return sb;
	}

	public int removeUselessValues() {
		CharSequence query = getUselessWindow();
		if (query==null)
			return 0;
		if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( ).append( "RESULTING QUERY FOR Table Size Enforce " ).append( query ).toString( ) );
		int deletedRows =StorageManager.getInstance().executeUpdate(query);
	    if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( ).append( deletedRows ).append( " old rows dropped from " ).append( getDBAliasInStr() ).toString( ) );
		return deletedRows;
	}
	public void releaseResources ( ) {
		isActive = false;
		if ( logger.isInfoEnabled() ) logger.info( "Finalized called" );
		synchronized ( listeners ) {
			Iterator<StreamSource> list =listeners.iterator();
			while (list.hasNext()) {
				StreamSource ss = list.next();
				getStorageManager( ).dropView( ss.getUIDStr() );
				list.remove();
			}
		}
		getStorageManager( ).dropTable( aliasCodeS );
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
