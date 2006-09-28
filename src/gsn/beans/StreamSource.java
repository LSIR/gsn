package gsn.beans;

import gsn.wrappers.DataListener;
import gsn.wrappers.StreamProducer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public final class StreamSource {

    private static final transient Logger logger = Logger
	    .getLogger(StreamSource.class);

    public static final int INFINITY = Integer.MAX_VALUE;

    private String alias;

    private final float samplingRate = 1;

    private String startTime;

    private String endTime;

    private String storageHistorySize = null;

    private int disconnectedBufferSize;

    private String sqlQuery;

    /**
         * Checks the timing to see whether the time is ok for starting.
         */
    private static final String[] dateFormats = new String[] {
	    "yyyy/MM/dd 'at' HH:mm:ss z", "h:mm:ss a", "h:mm a" };

    private transient Date startDate;

    private transient Date endDate;

    private ArrayList<AddressBean> addressing = new ArrayList<AddressBean>();

    private transient DataListener activeDataListener;

    private transient StreamProducer activeSourceProducer;

    /**
         * FIXME: Big verification test, because the query rewriter heavily
         * relies on the name of the tables being surrounded by ".
         * 
         * @param alias
         * @param query
         * @param size
         * @param addressing
         */
    public StreamSource(final String alias, final String query,
	    final String size, final ArrayList<AddressBean> addressing) {
	this.alias = alias;
	this.sqlQuery = query;
	this.storageHistorySize = size;
	this.addressing = addressing;
    }

    public StreamSource() {
    }

    public ArrayList<AddressBean> getAddressing() {
	return this.addressing;
    }

    /**
         * @return Returns the alias.
         */
    public String getAlias() {
	return this.alias.toUpperCase();
    }

    /**
         * @return Returns the bufferSize.
         */
    public int getDisconnectedBufferSize() {
	return this.disconnectedBufferSize;
    }

    public float getSamplingRate() {
	return this.samplingRate;
    }

    /**
         * @return Returns the endTime.
         */
    public String getEndTime() {
	if (this.endTime == null)
	    this.endTime = new SimpleDateFormat(dateFormats[0])
		    .format(new Date(System.currentTimeMillis() * 2));
	return this.endTime;
    }

    /**
         * @param endTime
         *                The endTime to set.
         */
    public void setEndTime(final String endTime) {
	this.endTime = endTime;
	this.endDate = null;
    }

    /**
         * @return Returns the startTime.
         */
    public String getStartTime() {
	if (this.startTime == null)
	    this.startTime = new SimpleDateFormat(dateFormats[0])
		    .format(new Date(System.currentTimeMillis()));
	return this.startTime;
    }

    /**
         * Converts the result of the <code>getEndTime()</code> to a Date
         * object and caches the object through execution.
         * 
         * @return Returns the endDate. <p/> Note that, if there is any error in
         *         conversion, the return value will be a Date object
         *         representing Long.Min
         */
    public Date getEndDate() {
	if (this.endDate == null) {
	    try {
		this.endDate = DateUtils.parseDate(this.getEndTime(),
			dateFormats);
	    } catch (final ParseException e) {
		logger.warn(e.getMessage(), e);
		this.endDate = new Date(0);
	    }
	}
	return this.endDate;
    }

    /**
         * @return Returns the startDate.
         */
    public Date getStartDate() {
	if (this.startDate == null) {
	    try {
		this.startDate = DateUtils.parseDate(this.getStartTime(),
			dateFormats);
	    } catch (final ParseException e) {
		logger.warn(e.getMessage(), e);
		this.startDate = new Date(Long.MAX_VALUE);
	    }
	}
	return this.startDate;
    }

    /**
         * @param startTime
         *                The startTime to set.
         */
    public void setStartTime(final String startTime) {
	this.startTime = startTime;
	this.startDate = null;
    }

    // public boolean canStart () {
    // boolean result = false ;
    // Date startDate = getStartDate () ;
    // Date endDate = getEndDate () ;
    // Date now = new Date ( System.currentTimeMillis () ) ;
    // result = startDate.before ( now ) ;
    // result &= endDate.after ( now ) ;
    // return result ;
    // }

    /**
         * Tries to find the first index of time the source is going to be
         * accessed. If it can't parse the start time, it'll return
         * Long.MAX_VALUE
         * 
         * @return Returns the current system time in the long form.
         */
    public long startTimeInLong() {
	long result = Long.MAX_VALUE;
	final Date startDate = this.getStartDate();
	result = startDate.getTime();
	return result;

    }

    /**
         * @return Returns the storageSize.
         */
    public String getStorageSize() {
	return this.storageHistorySize;
    }

    /**
         * @return Returns the sqlQuery.
         */
    public String getSqlQuery() {
	if (this.sqlQuery.trim().length() == 0 || this.sqlQuery == null)
	    this.sqlQuery = "select * from \"WRAPPER\"";
	return this.sqlQuery;
    }

    public void setUsedDataSource(final StreamProducer ds,
	    final DataListener dbDataListener) {
	this.activeSourceProducer = ds;
	this.activeDataListener = dbDataListener;

    }

    /**
         * @return Returns the activeDataListener.
         */
    public DataListener getActiveDataListener() {
	return this.activeDataListener;
    }

    /**
         * @return Returns the activeSourceProducer.
         */
    public StreamProducer getActiveSourceProducer() {
	return this.activeSourceProducer;
    }

    private transient boolean isStorageCountBased = false;

    public static final int STORAGE_SIZE_NOT_SET = -1;

    private transient int parsedStorageSize = STORAGE_SIZE_NOT_SET;

    public boolean validate() {
	if (this.storageHistorySize != null) {
	    this.storageHistorySize = this.storageHistorySize.replace(" ", "")
		    .trim().toLowerCase();
	    if (this.storageHistorySize.equalsIgnoreCase(""))
		return true;
	    final int second = 1000;
	    final int minute = second * 60;
	    final int hour = minute * 60;
	    final int mIndex = this.storageHistorySize.indexOf("m");
	    final int hIndex = this.storageHistorySize.indexOf("h");
	    final int sIndex = this.storageHistorySize.indexOf("s");
	    if (mIndex < 0 && hIndex < 0 && sIndex < 0) {
		try {
		    this.parsedStorageSize = Integer
			    .parseInt(this.storageHistorySize);
		    this.isStorageCountBased = true;
		} catch (final NumberFormatException e) {
		    logger.error(new StringBuilder().append(
			    "The storage size, ").append(
			    this.storageHistorySize).append(
			    ", specified for the Stream Source : ").append(
			    this.getAlias()).append(" is not valid.")
			    .toString(), e);
		    return false;
		}
		return true;
	    } else
		try {
		    final StringBuilder shs = new StringBuilder(
			    this.storageHistorySize);
		    if (mIndex > 0)
			this.parsedStorageSize = Integer.parseInt(shs
				.deleteCharAt(mIndex).toString())
				* minute;
		    else if (hIndex > 0)
			this.parsedStorageSize = Integer.parseInt(shs
				.deleteCharAt(hIndex).toString())
				* hour;
		    else if (sIndex > 0)
			this.parsedStorageSize = Integer.parseInt(shs
				.deleteCharAt(sIndex).toString())
				* second;
		    this.isStorageCountBased = false;
		} catch (final NumberFormatException e) {
		    logger.debug(e.getMessage(), e);
		    logger.error(new StringBuilder().append(
			    "The storage size, ").append(
			    this.storageHistorySize).append(
			    ", specified for the Stream Source : ").append(
			    this.getAlias()).append(" is not valid.")
			    .toString());
		    return false;
		}
	}
	return true;
    }

    public boolean isStorageCountBased() {
	return this.isStorageCountBased;
    }

    public int getParsedStorageSize() {
	return this.parsedStorageSize;
    }
}
