/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2016, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: src/ch/epfl/gsn/wrappers/AbstractWrapper.java
*
* @author gsn_devs
* @author Ali Salehi
* @author Mehdi Riahi
* @author Timotee Maret
* @author Julien Eberle
*
*/

package ch.epfl.gsn.wrappers;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import javax.naming.OperationNotSupportedException;

import org.slf4j.LoggerFactory;

import ch.epfl.gsn.Main;
import ch.epfl.gsn.beans.AddressBean;
import ch.epfl.gsn.beans.DataField;
import ch.epfl.gsn.beans.StreamElement;
import ch.epfl.gsn.beans.StreamSource;
import ch.epfl.gsn.beans.windowing.LocalTimeBasedSlidingHandler;
import ch.epfl.gsn.beans.windowing.RemoteTimeBasedSlidingHandler;
import ch.epfl.gsn.beans.windowing.SlidingHandler;
import ch.epfl.gsn.beans.windowing.TupleBasedSlidingHandler;
import ch.epfl.gsn.beans.windowing.WindowType;
import ch.epfl.gsn.monitoring.Monitorable;
import ch.epfl.gsn.utils.GSNRuntimeException;
import ch.epfl.gsn.wrappers.AbstractWrapper;

import org.slf4j.Logger;

public abstract class AbstractWrapper extends Thread implements Monitorable {

	private final static transient Logger logger = LoggerFactory.getLogger(AbstractWrapper.class);

	protected final List<StreamSource> listeners = Collections
			.synchronizedList(new ArrayList<StreamSource>());

	private AddressBean activeAddressBean;

	private boolean isActive = true;

	private SlidingHandler tupleBasedSlidingHandler;

	private SlidingHandler timeBasedSlidingHandler;

	private HashMap<Class<? extends SlidingHandler>, SlidingHandler> slidingHandlers = new HashMap<Class<? extends SlidingHandler>, SlidingHandler>();

	private boolean usingRemoteTimestamp = false;

	private Hashtable<Object,Long> lastInOrderTimestamp = new Hashtable<Object, Long>();

	public static final int GARBAGE_COLLECT_AFTER_SPECIFIED_NO_OF_ELEMENTS = 2;
	
	private Long oooCount = 0L;
	
	private Long elementCount = 0L;

	/**
	 * Returns the view name created for this listener. Note that, GSN creates
	 * one view per listener.
	 * 
	 * @throws SQLException
	 */
	public void addListener(StreamSource ss) throws SQLException {
		if (WindowType.isTimeBased(ss.getWindowingType())) {
			if (timeBasedSlidingHandler == null) {
				timeBasedSlidingHandler = isUsingRemoteTimestamp() == false ? new LocalTimeBasedSlidingHandler(
						this)
						: new RemoteTimeBasedSlidingHandler(this);
				addSlidingHandler(timeBasedSlidingHandler);
			}
		} else {
			if (tupleBasedSlidingHandler == null)
				tupleBasedSlidingHandler = new TupleBasedSlidingHandler(this);
			addSlidingHandler(tupleBasedSlidingHandler);
		}

		for (SlidingHandler slidingHandler : slidingHandlers.values()) {
			if (slidingHandler.isInterestedIn(ss))
				slidingHandler.addStreamSource(ss);
		}

		listeners.add(ss);
		logger.debug("Adding listeners: " + ss.toString());
	}

	public void addSlidingHandler(SlidingHandler slidingHandler) {
		slidingHandlers.put(slidingHandler.getClass(), slidingHandler);
	}

	/**
	 * Removes the listener with it's associated view.
	 * 
	 * @throws SQLException
	 */
	public void removeListener(StreamSource ss) throws SQLException {
		listeners.remove(ss);
		// getStorageManager( ).executeDropView( ss.getUIDStr() );
		for (SlidingHandler slidingHandler : slidingHandlers.values()) {
			if (slidingHandler.isInterestedIn(ss))
				slidingHandler.removeStreamSource(ss);
		}
		if (listeners.size() == 0) {
			releaseResources();
		}
		
	}

	/**
	 * @return the listeners
	 */
	public List<StreamSource> getListeners() {
		return listeners;
	}

	//protected StorageManager getStorageManager() {
	//	return StorageManager.getInstance();
    //
	//}

	/**
	 * This method is called whenever the wrapper wants to send a data item back
	 * to the source where the data is coming from. For example, If the data is
	 * coming from a wireless sensor network (WSN), This method sends a data
	 * item to the sink node of the virtual sensor. So this method is the
	 * communication between the System and actual source of data. The data sent
	 * back to the WSN could be a command message or a configuration message.
	 * 
	 * @param dataItem
	 *            : The data which is going to be send to the source of the data
	 *            for this wrapper.
	 * @return True if the send operation is successful.
	 * @throws OperationNotSupportedException
	 *             If the wrapper doesn't support sending the data back to the
	 *             source. Note that by default this method throws this
	 *             exception unless the wrapper overrides it.
	 */

	public boolean sendToWrapper(String action, String[] paramNames,
			Object[] paramValues) throws OperationNotSupportedException {
		throw new OperationNotSupportedException(
				"This wrapper doesn't support sending data back to the source.");
	}

	public final AddressBean getActiveAddressBean() {
		if (this.activeAddressBean == null) {
			throw new RuntimeException(
					"There is no active address bean associated with the wrapper.");
		}
		return activeAddressBean;
	}

	/**
	 * Only sets if there is no other activeAddressBean configured.
	 * 
	 * @param newVal
	 *            the activeAddressBean to set
	 */
	public void setActiveAddressBean(AddressBean newVal) {
		if (this.activeAddressBean != null) {
			throw new RuntimeException(
					"There is already an active address bean associated with the wrapper.");
		}
		this.activeAddressBean = newVal;
	}

	private long noOfCallsToPostSE = 0;

	private final transient int aliasCode = Main.getWindowStorage().tableNameGenerator();
	private final CharSequence aliasCodeS = Main.getWindowStorage().tableNameGeneratorInString(aliasCode);

	public int getDBAlias() {
		return aliasCode;
	}

	public CharSequence getDBAliasInStr() {
		return aliasCodeS;
	}

	public abstract DataField[] getOutputFormat();

	public boolean isActive() {
		return isActive;
	}

	protected void postStreamElement(Serializable... values) {
		StreamElement se = new StreamElement(getOutputFormat(), values, System
				.currentTimeMillis());
		postStreamElement(se);
	}

	protected void postStreamElement(long timestamp, Serializable[] values) {
		StreamElement se = new StreamElement(getOutputFormat(), values,
				timestamp);
		postStreamElement(se);
	}

	/**
	 * This method gets the generated stream element and notifies the input
	 * streams if needed. The return value specifies if the newly provided
	 * stream element generated at least one input stream notification or not.
	 * 
	 * @param streamElement
	 * @return If the method returns false, it means the insertion doesn't
	 *         effected any input stream.
	 */

	protected Boolean postStreamElement(StreamElement streamElement) {
		if (streamElement == null) {
			logger.info("postStreamElement is called with null ! Wrapper "
					+ getWrapperName() + " might have a problem !");
			return false;
		}
		try {
			if (!isActive() || listeners.size() == 0)
				return false;
			if (!insertIntoWrapperTable(streamElement))
				return false;
			boolean toReturn = false;

			logger.debug("Size of the listeners to be evaluated - "+ listeners.size());

			for (SlidingHandler slidingHandler : slidingHandlers.values()) {
				toReturn = slidingHandler.dataAvailable(streamElement)
						|| toReturn;
			}
			if (++noOfCallsToPostSE
					% GARBAGE_COLLECT_AFTER_SPECIFIED_NO_OF_ELEMENTS == 0) {
				removeUselessValues();
			}
			return toReturn;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			logger.error("Produced data item from the wrapper couldn't be propagated inside the system.");
			return false;
		}
	}

	/**
	 * Updates the table representing the data items produced by the stream
	 * element. Returns false if the update fails or doesn't change the state of
	 * the table.
	 * 
	 * @param se
	 *            Stream element to be inserted to the table if needed.
	 * @return true if the stream element is successfully inserted into the
	 *         table.
	 * @throws SQLException
	 */
	public boolean insertIntoWrapperTable(StreamElement se) throws SQLException {
		if (listeners.size() == 0)
			return false;

		Connection conn = null;
		try {
            if (isOutOfOrder(se)) {
            	oooCount = oooCount == Long.MAX_VALUE ? 0 : oooCount + 1;
				logger.debug("Out of order data item detected, it is not propagated into the system : [" + se.toString() + "]");
				return false;
			}
			conn = Main.getWindowStorage().getConnection();
			Main.getWindowStorage().executeInsert(aliasCodeS, getOutputFormat(), se, conn);
			if (getPartialOrdersKey() != null){
                lastInOrderTimestamp.put(se.getData(getPartialOrdersKey()), se.getTimeStamp());
			}else{
				lastInOrderTimestamp.put(0,se.getTimeStamp()); 
			}
            elementCount = elementCount == Long.MAX_VALUE ? 0 : elementCount + 1;
            return true;
		} finally {
			Main.getWindowStorage().close(conn);
		}
	}

    public boolean isOutOfOrder(StreamElement se) throws SQLException {
        if (listeners.size() == 0)
			return false;
        Connection conn = null;
        Object key = 0;
        if (getPartialOrdersKey() != null){
        	key = se.getData(getPartialOrdersKey());
        }
		try {
			// Checks if the stream element is out of order
            if (lastInOrderTimestamp.get(key) == null) {
                conn = Main.getWindowStorage().getConnection();
                StringBuilder query = new StringBuilder();
				query.append("select max(timed) from ").append(aliasCodeS);
				StringBuilder query2 = new StringBuilder();
				query2.append("select count(*) from ").append(aliasCodeS);
				if (getPartialOrdersKey() != null){
					query.append(" where "+getPartialOrdersKey()+"="+key); // code injection !!!
					query2.append(" where "+getPartialOrdersKey()+"="+key); 
				}
				ResultSet rs = Main.getWindowStorage().executeQueryWithResultSet(query,conn);
				ResultSet rs2 = Main.getWindowStorage().executeQueryWithResultSet(query2,conn);
				int n=rs2.next()?rs2.getInt(1):0;

				if (rs.next() && n>0) {
					lastInOrderTimestamp.put(key,rs.getLong(1));
				} else {
					lastInOrderTimestamp.put(key,Long.MIN_VALUE); // Table is empty
				}
			}
            if (isTimeStampUnique())
                return (se.getTimeStamp() <= lastInOrderTimestamp.get(key));
            else
            	return (se.getTimeStamp() < lastInOrderTimestamp.get(key));
		} finally {
			Main.getWindowStorage().close(conn);
		}
    }

	/**
	 * This method is called whenever the wrapper wants to send a data item back
	 * to the source where the data is coming from. For example, If the data is
	 * coming from a wireless sensor network (WSN), This method sends a data
	 * item to the sink node of the virtual sensor. So this method is the
	 * communication between the System and actual source of data. The data sent
	 * back to the WSN could be a command message or a configuration message.
	 * 
	 * @param dataItem
	 *            : The data which is going to be send to the source of the data
	 *            for this wrapper.
	 * @return True if the send operation is successful.
	 * @throws OperationNotSupportedException
	 *             If the wrapper doesn't support sending the data back to the
	 *             source. Note that by default this method throws this
	 *             exception unless the wrapper overrides it.
	 */

	public boolean sendToWrapper(Object dataItem)
			throws OperationNotSupportedException {
		if (isActive == false)
			throw new GSNRuntimeException(
					"Sending to an inactive/disabled wrapper is not allowed !");
		throw new OperationNotSupportedException(
				"This wrapper doesn't support sending data back to the source.");
	}

	/**
	 * Removes all the listeners, drops the views representing them, drops the
	 * sensor table, stops the TableSizeEnforce thread.
	 * 
	 */
	public StringBuilder getUselessWindow() {
		StringBuilder condition = new StringBuilder("");
		synchronized (slidingHandlers) {
			for (SlidingHandler slidingHandler : slidingHandlers.values()) {
				if (condition.length()>0){condition.append(" and ");}
				condition.append(slidingHandler.getCuttingCondition());
			}
		}
		logger.debug("Cutting condition : " + condition);
		if (condition.length() == 0)
			return null;
		StringBuilder sb = new StringBuilder("delete from ").append(
				getDBAliasInStr()).append(" where ");
		sb.append(condition);
		return sb;
	}

	public int removeUselessValues() throws SQLException {
		StringBuilder query = getUselessWindow();
		if (query == null)
			return 0;
		logger.debug(new StringBuilder().append(
					"RESULTING QUERY FOR Table Size Enforce ").append(query)
					.toString());
		int deletedRows = Main.getWindowStorage().executeUpdate(query);
		logger.debug(new StringBuilder().append(deletedRows).append(
					" old rows dropped from ").append(getDBAliasInStr())
					.toString());
		return deletedRows;
	}

	public void releaseResources() throws SQLException {
		isActive = false;
		Main.getInstance().getToMonitor().remove(this);
		dispose();
		logger.info("dispose called");
		listeners.clear();
		for (SlidingHandler slidingHandler : slidingHandlers.values()) {
			slidingHandler.dispose();
		}
		Main.getWindowStorage().executeDropTable(aliasCodeS);
	}

	public static final String TIME_FIELD = "timed";
	
	
	public final boolean initialize_wrapper(){
		boolean r = initialize();
		if (r){
			Main.getInstance().getToMonitor().add(this);
			setName(getWrapperName()+"::"+activeAddressBean.getVirtualSensorName());
		}
		return r;
	}

	/**
	 * The addressing is provided in the ("ADDRESS",Collection<KeyValue>). If
	 * the DataSource can't initialize itself because of either internal error
	 * or inaccessibility of the host specified in the address the method
	 * returns false. The dbAliasName of the DataSource is also specified with
	 * the "DBALIAS" in the context. The "STORAGEMAN" points to the
	 * StorageManager which should be used for querying.
	 * 
	 * @return True if the initialization do successfully otherwise false;
	 */

	public abstract boolean initialize();
	
	public abstract void dispose();

	public abstract String getWrapperName();

	/**
	 * Indicates whether we use GSN's time (local time) or the time already
	 * exists in the data (remote time) for the timestamp of generated stream
	 * elements.
	 * 
	 * @return <code>false</code> if we use local time <br>
	 *         <code>true</code> if we use remote time
	 */
	protected boolean isUsingRemoteTimestamp() {
		return usingRemoteTimestamp;
	}

	/**
	 * 
	 * @param usingRemoteTimestamp
	 */
	protected void setUsingRemoteTimestamp(boolean usingRemoteTimestamp) {
		this.usingRemoteTimestamp = usingRemoteTimestamp;
	}

	/**
	 * Returns false if the wrapper can produce multiple different data items
	 * [stream elements] with the same timestamp. If this is false, then all the
	 * stream elements with the same timestamp will be accepted. If this method
	 * returns true (default value), duplicates are discarded and only the first
	 *  one is kept.
	 */
	public boolean isTimeStampUnique() {
		return true;
	}
	
	/**
	 * Allows for having partial ordering by only checking the time stamp order of
	 * stream elements having the same key.
	 * null is total ordering should be applied
	 */
	public String getPartialOrdersKey(){
		return activeAddressBean.getPartialOrderKey();
	}

	public boolean manualDataInsertion(StreamElement se) {
		throw new RuntimeException(
				"Manual data insertion is not supported by this wrapper");
	}
	
	public Hashtable<String, Object> getStatistics(){
		Hashtable<String, Object> stat = new Hashtable<String, Object>();
		stat.put("vs."+activeAddressBean.getVirtualSensorName().replaceAll("\\.", "_")+".input."+ activeAddressBean.getInputStreamName().replaceAll("\\.", "_") +".outOfOrder.counter", oooCount);
		stat.put("vs."+activeAddressBean.getVirtualSensorName().replaceAll("\\.", "_")+".input."+ activeAddressBean.getInputStreamName().replaceAll("\\.", "_") +".produced.counter", elementCount);
		return stat;
	}
	
	
}
