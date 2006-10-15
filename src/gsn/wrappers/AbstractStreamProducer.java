package gsn.wrappers;

import gsn.Main;
import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.beans.StreamSource;
import gsn.control.VSensorLoader;
import gsn.shared.Registry;
import gsn.storage.SQLUtils;
import gsn.storage.StorageManager;
import gsn.vsensor.Container;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

import javax.naming.OperationNotSupportedException;

import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 *         Date: Aug 4, 2005 <br>
 */
public abstract class AbstractStreamProducer extends Thread implements
	StreamProducer {

    /**
         * Used by the data source when it wants to insert the data into it's
         * main database. The AbstractDataSource should keep track of this data
         * source so that it can release it when <code>finialize</code>
         * called.
         */
    private final static transient Logger logger = Logger
	    .getLogger(AbstractStreamProducer.class);

    private static final transient boolean isDebugEnabled = logger
	    .isDebugEnabled();

    private static final transient boolean isInfoEnabled = logger
	    .isInfoEnabled();

    protected final ArrayList<DataListener> listeners = new ArrayList<DataListener>();

    private String host;

    private int port;

    private AddressBean activeAddressBean;

    private boolean isAlive;

       private TableSizeEnforce tableSizeEnforce;

    private static int TABLE_SIZE_ENFORCING_THREAD_COUNTER = 0;

    public boolean initialize(TreeMap initialContext) {
	host = (String) initialContext.get(Registry.VS_HOST);
	if (host == null) {
	    logger.error("The host for stream source can't be Null.");
	    return false;
	}
	port = Integer.parseInt((String) initialContext.get(Registry.VS_PORT));
	if (port <= 0) {
	    logger.error(new StringBuilder().append("The port ").append(port)
		    .append(" is invalid for a stream source.").toString());
	    return false;
	}
	if (isDebugEnabled == true)
	    logger.debug(new StringBuilder().append(
		    "Initializing connection to ").append(host).append(":")
		    .append(port).toString());
	activeAddressBean = (AddressBean) initialContext
		.get(Container.STREAM_SOURCE_ACTIVE_ADDRESS_BEAN);

	if (activeAddressBean == null) {
	    logger
		    .fatal("The address bean supplied to initializer shouldn't be null, possible a BUG.");
	    return false;
	}

	isAlive = true;
	tableSizeEnforce = new TableSizeEnforce(getDBAlias(), listeners);
	Thread tableSizeEnforcingThread = new Thread(tableSizeEnforce);
	tableSizeEnforcingThread.setName("TableSizeEnforceing-WRAPPER-Thread"
		+ TABLE_SIZE_ENFORCING_THREAD_COUNTER++);
	tableSizeEnforcingThread.start();
	return true;
    }

    public String addListener(DataListener dataListener) {
	HashMap<String, String> mapping = new HashMap<String, String>();
	mapping.put("WRAPPER", getDBAlias());
	String resultQuery = SQLUtils.rewriteQuery(dataListener
		.getMergedQuery(), mapping);
	String viewName = dataListener.getViewName();
	if (isDebugEnabled == true)
	    logger.debug(new StringBuilder().append("The view name=").append(
		    viewName).append(" with the query=").append(resultQuery)
		    .toString());
	getStorageManager().createView(viewName, resultQuery);
	synchronized (listeners) {
	    listeners.add(dataListener);
	    tableSizeEnforce.updateInternalCaches();
	}
	return viewName;
    }

    /**
         * Removes the listener with it's associated view.
         */
    public void removeListener(DataListener dataListener) {
	synchronized (listeners) {
	    listeners.remove(dataListener);
	    tableSizeEnforce.updateInternalCaches();
	}
	getStorageManager().dropView(dataListener.getViewName());
    }

    /**
         * @return The number of active listeners which are interested in
         *         hearing from this data source.
         */
    public int getListenersSize() {
	return listeners.size();
    }

    protected StorageManager getStorageManager() {
	return StorageManager.getInstance();

    }
    /**
         * The addressing is provided in the ("ADDRESS",Collection<KeyValue>).
         * If the DataSource can't initialize itself because of either internal
         * error or inaccessibility of the host specified in the address the
         * method returns false. The dbAliasName of the DataSource is also
         * specified with the "DBALIAS" in the context. The "STORAGEMAN" points
         * to the StorageManager which should be used for querying.
         * 
         * @return True if the initialization do successfully otherwise false;
         */

    public final AddressBean getActiveAddressBean() {
	return activeAddressBean;
    }

    protected final int getAddressBeanActivePort() {
	return port;
    }

    protected final String getAddressBeanActiveHostName() {
	return host;
    }

    private String cachedDBAliasName = null;

    public String getDBAlias() {
	if (cachedDBAliasName == null)
	    cachedDBAliasName = Main.tableNameGenerator();
	return cachedDBAliasName;
    }

    public abstract Collection<DataField> getProducedStreamStructure();

    protected boolean isActive() {
	return isAlive;
    }

    public void finalize(HashMap context) {
	isAlive = false;
	if (isInfoEnabled)
	    logger.info("Finalized called");
	// TODO : RELEASING THE RESOURCE AUTOMATICALLY USING TIMEOUT.
	getStorageManager().dropTable(getDBAlias(), true);
    }

    class TableSizeEnforce implements Runnable {
	/**
         * This thread executes a query which in turn droppes the stream
         * elements which are not interested by any of the existing listeners.
         * The query which is used for dropping the unused stream elements is
         * going to be generated whever there is a change in the set of
         * registered DataListeners. If there is a change the
         * <code>isCacheNeedsUpdate</code> will be true which triggers the
         * thread to generate a new query for removing the unused stream
         * elements.
         */
	private boolean isCacheNeedsUpdate = true;

	private final transient Logger logger = Logger
		.getLogger(TableSizeEnforce.class);

	private final int RUNNING_INTERVALS = 1000 * 5;

	private String tableName;

	private ArrayList<DataListener> listeners;

	TableSizeEnforce(String tableName, ArrayList<DataListener> listeners) {
	    this.tableName = tableName;
	    this.listeners = listeners;

	}

	public void updateInternalCaches() {
	    isCacheNeedsUpdate = true;
	}

	public void run() {
	    StringBuilder deleteStatement = new StringBuilder();
	    while (isAlive) {
		try {
		    Thread.sleep(RUNNING_INTERVALS);
		} catch (InterruptedException e) {
		    logger.error(e.getMessage(), e);
		}
		/**
                 * Garbage collector's where clause. The garbage collector is in
                 * fact, the actual worker for size enforcement.
                 */

		synchronized (AbstractStreamProducer.this.listeners) {
		    if (listeners.size() == 0)
			continue;
		    if (isCacheNeedsUpdate) {
			deleteStatement = new StringBuilder();
			for (DataListener listener : listeners) {
			    if (listener.getStreamSource()
				    .getParsedStorageSize() != StreamSource.STORAGE_SIZE_NOT_SET)
				deleteStatement
					.append(
						extractConditions(
							listener.isCountBased(),
							listener
								.getHistorySize(),
							listener
								.getStreamSource()
								.getStartDate(),
							listener
								.getStreamSource()
								.getEndDate()))
					.append("  AND ");
			}
			if (deleteStatement.length() == 0) {
			    continue;
			}
			deleteStatement.replace(deleteStatement.length() - 4,
				deleteStatement.length(), "");
			deleteStatement.insert(0, " where ").insert(0,
				tableName).insert(0, "delete from ");
			if (isDebugEnabled == true)
			    logger.debug(new StringBuilder().append(
				    "RESULTING QUERY FOR Table Size Enforce ")
				    .append(deleteStatement.toString())
				    .toString());
			isCacheNeedsUpdate = false;
		    }
		}
		int resultOfUpdate = StorageManager.getInstance()
			.executeUpdate(deleteStatement);
		if (isDebugEnabled == true)
		    logger.debug(new StringBuilder().append(resultOfUpdate)
			    .append(" old rows dropped from ")
			    .append(tableName).toString());
	    }
	}

	private StringBuilder extractConditions(boolean countBased,
		int historySize, Date startDate, Date endDate) {
	    StringBuilder result = new StringBuilder();
	    if (StorageManager.isHsql()) {
		if (countBased)
		    // result.append ( "( ( " ).append ( tableName ).append
		    // ( ".TIMED
		    // not in ( select " ).append ( tableName ).append (
		    // ".TIMED from
		    // " ).append (
		    // tableName ).append ( " order by " ).append (
		    // tableName
		    // ).append ( ".TIMED DESC LIMIT " ).append (
		    // historySize
		    // ).append (
		    // " offset 0 )) AND (" + tableName + ".TIMED >=" +
		    // startDate.getTime () + ") AND (" + tableName +
		    // ".TIMED<=" +
		    // endDate.getTime () + ") )" );
		    result
			    .append("( ( ")
			    .append(tableName)
			    .append(".TIMED <= ( SELECT TIMED from ")
			    .append(tableName)
			    .append(
				    " group by TIMED order by TIMED desc limit 1 offset ")
			    .append(historySize).append(
				    "  ) ) AND (" + tableName + ".TIMED >="
					    + startDate.getTime() + ") AND ("
					    + tableName + ".TIMED<="
					    + endDate.getTime() + ") )");
		else
		    result.append("( ( ").append(tableName).append(
			    ".TIMED < (NOW_MILLIS() -").append(historySize)
			    .append(
				    ") ) AND (" + tableName + ".TIMED >="
					    + startDate.getTime() + ") AND ("
					    + tableName + ".TIMED<="
					    + endDate.getTime() + ") )");
	    } else if (StorageManager.isMysqlDB()) {
		if (countBased)
		    result
			    .append("( ( ")
			    .append(tableName)
			    .append(
				    ".TIMED <= ( SELECT * from ( SELECT TIMED from ")
			    .append(tableName)
			    .append(
				    " group by TIMED order by TIMED desc limit 1 offset ")
			    .append(historySize).append("  ) AS TMP_").append(
				    (int) (Math.random() * 10000000)).append(
				    " ) ) AND (" + tableName + ".TIMED >="
					    + startDate.getTime() + ") AND ("
					    + tableName + ".TIMED<="
					    + endDate.getTime() + ") )");
		else
		    result.append("( ( ").append(tableName).append(
			    ".TIMED < (UNIX_TIMESTAMP() -").append(historySize)
			    .append(
				    ") ) AND (" + tableName + ".TIMED >="
					    + startDate.getTime() + ") AND ("
					    + tableName + ".TIMED<="
					    + endDate.getTime() + ") )");
	    }

	    return result;
	}
    }

    protected void publishData(StreamElement streamElement) {
	boolean result = getStorageManager().insertData(getDBAlias(),
		streamElement);
	if (result == false) {
	    logger.warn("Inserting the following data item failed : "
		    + streamElement);
	} else
	    synchronized (listeners) {
		if (listeners.size() == 0)
		    logger
			    .warn("A wrapper without listener shouldn't exist. !!!");
		for (Iterator<DataListener> iterator = listeners.iterator(); iterator
			.hasNext();) {
		    DataListener dataListener = iterator.next();
		    boolean results = getStorageManager().isThereAnyResult(
			    dataListener.getViewQuery());
		    if (results) {
			if (isDebugEnabled == true)
			    logger
				    .debug("Output stream produced/received from a wrapper");
			dataListener.dataAvailable();
		    }
		}
	    }
    }

    public boolean sendToWrapper(Object dataItem)
	    throws OperationNotSupportedException {
	throw new OperationNotSupportedException(
		"This wrapper doesn't support sending data back to the source.");

    }

}
