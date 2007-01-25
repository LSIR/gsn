package gsn.beans;

import gsn.Main;
import gsn.storage.SQLUtils;
import gsn.storage.StorageManager;
import gsn.utils.CaseInsensitiveComparator;
import gsn.utils.GSNRuntimeException;
import gsn.wrappers.AbstractWrapper;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeMap;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 * Date: Aug 4, 2005 <br>
 * Time: 10:15:42 PM <br>
 */
public  class StreamSource {

	public static final String DEFAULT_QUERY = "select * from wrapper";

	private static final transient Logger logger             = Logger.getLogger( StreamSource.class );

	private String                        alias;

	private float                        samplingRate       ;

	private String                        startTime;

	private String                        endTime;

	private String                        rawHistorySize = null;

	private int                           disconnectedBufferSize;

	private String                        sqlQuery;

	protected  int         uid        ;

	protected  CharSequence         uidS    ;
	/**
	 * Checks the timing to see whether the time is ok for starting.
	 */
	private static final String [ ]       dateFormats        = new String [ ] { "yyyy/MM/dd 'at' HH:mm:ss z" , "h:mm:ss a" , "h:mm a" };

	private transient Date                startDate;

	private transient Date                endDate;

	private ArrayList < AddressBean >     addressing         = new ArrayList < AddressBean >( );

	private transient AbstractWrapper      wrapper;

	private InputStream inputStream ;

	public StreamSource() {
	}


	public String getRawHistorySize() {
		return rawHistorySize;
	}

	public StreamSource setRawHistorySize(String rawHistorySize) {
		this.rawHistorySize = rawHistorySize;
		return this;
	}

	public StreamSource setAddressing(ArrayList<AddressBean> addressing) {
		this.addressing = addressing;
		return this;
	}

	public StreamSource setAlias(String alias) {
		this.alias = alias;
		return this;
	}

	public StreamSource setSqlQuery(String sqlQuery) {
		this.sqlQuery = sqlQuery;
		return this;
	}

	public static final ArrayList<AddressBean> EMPTY_ADDRESS_BEAN = new ArrayList<AddressBean>();
	public ArrayList < AddressBean > getAddressing ( ) {
		if (this.addressing==null)
			this.addressing=EMPTY_ADDRESS_BEAN;
		return this.addressing;
	}

	/**
	 * @return Returns the alias.
	 */
	public CharSequence getAlias ( ) {
		return alias.toLowerCase( );
	}


	public InputStream getInputStream() {
		return inputStream;
	}

	/**
	 * @return Returns the bufferSize.
	 */
	public int getDisconnectedBufferSize ( ) {
		return disconnectedBufferSize;
	}

	public float getSamplingRate ( ) {
		return samplingRate;
	}


	public void setSamplingRate (float newRate ) {
		if (cachedSqlQuery!=null) 
			throw new GSNRuntimeException("Sampling rate can't be changed anymore !");

		if (newRate>=0 && newRate<=1)
			this.samplingRate=newRate;
		else
			throw new GSNRuntimeException("Invalid sampling rate is provided. Sampling rate is between 0 and 1.");
	}

	/**
	 * @return Returns the endTime.
	 */
	public String getEndTime ( ) {
		if ( this.endTime == null ) this.endTime = new SimpleDateFormat( dateFormats[ 0 ] ).format( new Date( System.currentTimeMillis( ) * 2 ) );
		return endTime;
	}

	/**
	 * @param endTime The endTime to set.
	 */
	public StreamSource setEndTime ( final String endTime ) {
		this.endTime = endTime;
		this.endDate = null;
		return this;
	}

	/**
	 * @return Returns the startTime.
	 */
	public String getStartTime ( ) {
		if ( this.startTime == null ) this.startTime = new SimpleDateFormat( dateFormats[ 0 ] ).format( new Date( System.currentTimeMillis( ) ) );
		return this.startTime;
	}

	/**
	 * Converts the result of the <code>getEndTime()</code> to a Date object
	 * and caches the object through execution.
	 * 
	 * @return Returns the endDate. <p/> Note that, if there is any error in
	 * conversion, the return value will be a Date object representing Long.Min
	 */
	public Date getEndDate ( ) {
		if ( this.endDate == null ) {
			try {
				this.endDate = DateUtils.parseDate( this.getEndTime( ) , dateFormats );
			} catch ( final ParseException e ) {
				logger.warn( e.getMessage( ) , e );
				this.endDate = new Date( 0 );
			}
		}
		return this.endDate;
	}

	/**
	 * @return Returns the startDate.
	 */
	public Date getStartDate ( ) {
		if ( this.startDate == null ) {
			try {
				this.startDate = DateUtils.parseDate( this.getStartTime( ) , dateFormats );
			} catch ( final ParseException e ) {
				logger.warn("Stream Source disabled, invalid start time !\n "+ e.getMessage( ) , e );
				this.startDate = new Date( Long.MAX_VALUE );
			}
		}
		return this.startDate;
	}


	public boolean canStart () {
		if (!validate())
			return false;
		Date startDate = getStartDate () ;
		Date endDate = getEndDate () ;
		Date now = new Date ( System.currentTimeMillis () ) ;
		boolean result = false;
		result = startDate.before ( now ) ;
		result &= endDate.after ( now ) ;
		return result ;
	}

	/**
	 * Tries to find the first index of time the source is going to be accessed.
	 * If it can't parse the start time, it'll return Long.MAX_VALUE
	 * 
	 * @return Returns the current system time in the long form.
	 */
	public long startTimeInLong ( ) {
		long result = Long.MAX_VALUE;
		final Date startDate = this.getStartDate( );
		result = startDate.getTime( );
		return result;

	}

	/**
	 * @return Returns the storageSize.
	 */
	public String getStorageSize ( ) {
		return this.rawHistorySize;
	}

	/**
	 * @return Returns the sqlQuery.
	 */
	public String getSqlQuery ( ) {
		if (sqlQuery==null || sqlQuery.trim( ).length( ) == 0 ) sqlQuery = DEFAULT_QUERY;
		return sqlQuery;
	}

	public void setWrapper ( AbstractWrapper wrapper  ) {
		if (validate()==false)
			throw new GSNRuntimeException("Can't set the wrapper when the stream source is invalid.");
		this.wrapper = wrapper;
		wrapper.addListener(this);
	}

	/**
	 * @return Returns the activeSourceProducer.
	 */
	public AbstractWrapper getWrapper ( ) {
		return this.wrapper;
	}

	private transient boolean isStorageCountBased  = false;

	public static final int   STORAGE_SIZE_NOT_SET = -1;

	private transient int     parsedStorageSize    = STORAGE_SIZE_NOT_SET;

	private boolean isValidated = false;
	private boolean validationResult = false;
	/**;
	 * Note that the validate method doesn't case if the wrapper variable or input stream variable are set or not.
	 * 
	 * @return
	 */
	public boolean validate ( ) {
		if (isValidated==true)
			return validationResult;
		isValidated=true;
		if (getSamplingRate()<=0) 
			logger.warn( new StringBuilder( ).append( "The sampling rate is set to zero (or negative) which means no results. StreamSource = " )
					.append( getAlias( ) ).toString( ) );
		if (getAddressing().size()==0) {
			logger.warn(new StringBuilder("Validation failed because there is no addressing predicates provided for the stream source (the addressing part of the stream source is empty)").append("stream source alias = ").append(getAlias()));
			return validationResult=false;
		}
		if ( this.rawHistorySize != null ) {
			this.rawHistorySize = this.rawHistorySize.replace( " " , "" ).trim( ).toLowerCase( );
			if ( this.rawHistorySize.equalsIgnoreCase( "" ) ) return true;
			final int second = 1000;
			final int minute = second * 60;
			final int hour = minute * 60;
			final int mIndex = this.rawHistorySize.indexOf( "m" );
			final int hIndex = this.rawHistorySize.indexOf( "h" );
			final int sIndex = this.rawHistorySize.indexOf( "s" );
			if ( mIndex < 0 && hIndex < 0 && sIndex < 0 ) {
				try {
					this.parsedStorageSize = Integer.parseInt( this.rawHistorySize );
					this.isStorageCountBased = true;
				} catch ( final NumberFormatException e ) {
					logger.error( new StringBuilder( ).append( "The storage size, " ).append( this.rawHistorySize ).append( ", specified for the Stream Source : " ).append( this.getAlias( ) ).append(
					" is not valid." ).toString( ) , e );
					return (validationResult= false);
				}
				return (validationResult=true);
			} else
				try {
					final StringBuilder shs = new StringBuilder( this.rawHistorySize );
					if ( mIndex > 0 ) this.parsedStorageSize = Integer.parseInt( shs.deleteCharAt( mIndex ).toString( ) ) * minute;
					else if ( hIndex > 0 ) this.parsedStorageSize = Integer.parseInt( shs.deleteCharAt( hIndex ).toString( ) ) * hour;
					else if ( sIndex > 0 ) this.parsedStorageSize = Integer.parseInt( shs.deleteCharAt( sIndex ).toString( ) ) * second;
					this.isStorageCountBased = false;
				} catch ( NumberFormatException e ) {
					logger.debug( e.getMessage( ) , e );
					logger.error( new StringBuilder( ).append( "The storage size, " ).append( this.rawHistorySize ).append( ", specified for the Stream Source : " ).append( this.getAlias( ) ).append(
					" is not valid." ).toString( ) );
					return (validationResult=false);
				}
		}
		return validationResult=true;
	}

	public boolean isStorageCountBased ( ) {
		validate();
		return this.isStorageCountBased;
	}

	public int getParsedStorageSize ( ) {
		validate();
		return this.parsedStorageSize;
	}



	public CharSequence getUIDStr() {
		if (validate()==false)
			return null;
		if (uidS==null) {
			uid    = Main.tableNameGenerator( );
			uidS   = Main.tableNameGeneratorInString( uid );
		}
		return uidS;

	}

	public int hashCode() {
		return getUIDStr().hashCode();
	}

	public Boolean dataAvailable ( ) {
		if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( ).append( "Data avialble in the stream *" ).append( getAlias( ) ).append( "*" ).toString( ) );
		return inputStream.dataAvailable( getUIDStr() );
	}

	CharSequence cachedSqlQuery = null;

	/**
	 * This method gets a stream source specification and generates the appropriate where clauses representing the stream source.
	 * The method takes into account the start time, end time, sampling rate, storage size (Both timed and count).
	 * This method combines the conditions generated by the stream source specification with the
	 * actually conditions listed in the stream source query. Afterwades, the method does the
	 * renaming of the whole query. The result will be cased in this object for later reuse.
	 *
	 * @return
	 */
	public CharSequence toSql() {
		if (cachedSqlQuery!=null)
			return cachedSqlQuery;
		if (getWrapper()==null)
			throw new GSNRuntimeException("Wrapper object is null, most probably a bug, please report it !");
		if (validate()==false)
			throw new GSNRuntimeException("Validation of this object the stream source failed, please check the logs.");
		CharSequence wrapperAlias = getWrapper().getDBAliasInStr();
		if (samplingRate==0 || (isStorageCountBased && getParsedStorageSize()==0))
			return cachedSqlQuery = "select * from "+wrapperAlias+ " where FALSE";
		TreeMap < CharSequence , CharSequence > rewritingMapping = new TreeMap < CharSequence , CharSequence >(new CaseInsensitiveComparator() );
		rewritingMapping.put("wrapper", wrapperAlias);
		StringBuilder toReturn = new StringBuilder(getSqlQuery());
		if (getSqlQuery().toLowerCase().indexOf(" where ")<0)
			toReturn.append(" where " );
		else
			toReturn.append(" and " );
//		Applying the ** START  AND END TIME ** for count based windows
		toReturn.append(" wrapper.timed >=").append(getStartDate().getTime()).append(" and timed <=").append(getEndDate().getTime()).append(" and ");
		
		if (isStorageCountBased()) {
			toReturn.append("timed >= (select distinct(timed) from ").append(wrapperAlias).append(" order by timed desc limit 1 offset " );
			toReturn.append(getParsedStorageSize()-1).append( " )" );
		}else {
			toReturn.append("(wrapper.timed >");
			if ( StorageManager.isHsql( ) ) 
				toReturn.append( " (NOW_MILLIS()");
			else if ( StorageManager.isMysqlDB( ) ) 
				toReturn.append(" (UNIX_TIMESTAMP()*1000");
			toReturn.append(" - ").append(getParsedStorageSize()).append(" ) ) ");
		}
		if ( samplingRate !=1  )
			toReturn.append( " and ( mod( timed , 100)< " ).append( samplingRate*100 ).append( ")" );
		toReturn = new StringBuilder(SQLUtils.newRewrite(toReturn, rewritingMapping));
		toReturn.append(" order by timed desc ");
		if ( logger.isDebugEnabled( ) ) {
			logger.debug( new StringBuilder( ).append( "The original Query : " ).append( getSqlQuery( ) ).toString( ) );
			logger.debug( new StringBuilder( ).append( "The merged query : " ).append( toReturn.toString( ) ).append( " of the StreamSource " ).append( getAlias( ) ).append(
			" of the InputStream: " ).append( inputStream.getInputStreamName() ).append( "" ).toString( ) );
		}
		return cachedSqlQuery=toReturn;
	}

	public StreamSource setInputStream(InputStream is) throws GSNRuntimeException{
		if (alias==null)
			throw new NullPointerException("Alias can't be null!");
		if (this.inputStream!=null && is!=this.inputStream)
			throw new GSNRuntimeException("Can't reset the input stream variable !.");
		this.inputStream=is;
		if (validate()==false)
			throw new GSNRuntimeException("You can't set the input stream on an invalid stream source. ");
		return this;
	}
	


		
}
