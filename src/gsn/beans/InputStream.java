package gsn.beans;

import gsn.VirtualSensorInitializationFailedException;
import gsn.VirtualSensorPool;
import gsn.storage.PoolIsFullException;
import gsn.storage.SQLUtils;
import gsn.storage.StorageManager;
import gsn.utils.CaseInsensitiveComparator;
import gsn.vsensor.AbstractVirtualSensor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.TreeMap;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class InputStream {

	public static final int                             INITIAL_DELAY_5000MSC = 5000;

	private transient static final Logger               logger                = Logger.getLogger( InputStream.class );

	private transient StorageManager                    storageMan = StorageManager.getInstance();

	private String                                      inputStreamName;

	private long                                        count                 = Long.MAX_VALUE;

	private transient long                              currentCount          = 1;

	private int                                         rate;

	private String                                      query;

	private  StreamSource[]                 sources;

	private HashMap < CharSequence , StreamSource >           streamSourceAliasNameToStreamSourceName =  new HashMap < CharSequence , StreamSource >( );

	private transient VirtualSensorPool                 pool;

	private final transient TreeMap< CharSequence , CharSequence > rewritingData         = new TreeMap < CharSequence , CharSequence >( new CaseInsensitiveComparator());

	private transient long                              lastVisited           = 0;

	private CharSequence                               rewrittenSQL;

	/**
	 * For making one initial delay.
	 */

	 public String getQuery ( ) {
		 return this.query;
	 }

	 public void setQuery ( final String sql ) {
		 this.query = sql;
	 }

	 public String getInputStreamName ( ) {
		 return this.inputStreamName;
	 }

	 public void setInputStreamName ( final String inputStreamName ) {
		 this.inputStreamName = inputStreamName;
	 }

	 public long getCount ( ) {
		 if ( this.count == 0 ) this.count = Long.MAX_VALUE;
		 return this.count;
	 }

	 public void setCount ( final long count ) {
		 this.count = count;
	 }

	 public int getRate ( ) {
		 return this.rate;
	 }

	 public Collection < StreamSource > getSources ( ) {
		 return this.streamSourceAliasNameToStreamSourceName.values( );
	 }

	 public StreamSource getSource ( final String streamSourceName ) {
		 return this.streamSourceAliasNameToStreamSourceName.get( streamSourceName );
	 }


	 public void setSources(StreamSource... ss) {
		 this.sources = ss;
	 }

	 /**
	  * This method is called by the Stream Source timed Stream-Source has new
	  * results.
	  * 
	  * @param alias The alias of the StreamSource which has new data.
	  */
	 public boolean dataAvailable ( final CharSequence alias ) {
		 if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( ).append( "Notified by StreamSource on the alias: " ).append( alias ).toString( ) );
		 if ( this.pool == null ) {
			 logger.debug( "The input is dropped b/c the VSensorInstance is not set yet." );
			 return false;
		 }

		 if ( this.currentCount > this.getCount( ) ) {
			 if ( logger.isInfoEnabled( ) ) logger.info( "Maximum count reached, the value *discarded*" );
			 return false;
		 }

		 final long currentTimeMillis = System.currentTimeMillis( );
		 if ( this.rate > 0 && ( currentTimeMillis - this.lastVisited ) < this.rate ) {
			 if ( logger.isInfoEnabled( ) ) logger.info( "Called by *discarded* b/c of the rate limit reached." );
			 return false;
		 }
		 this.lastVisited = currentTimeMillis;

		 if ( this.rewrittenSQL == null ) {
			 this.rewrittenSQL = new StringBuilder( SQLUtils.newRewrite( getQuery( ).trim( ).toLowerCase( ), this.rewritingData ));
			 if ( logger.isDebugEnabled( ) )
				 logger.debug( new StringBuilder( ).append( "Rewritten SQL: " ).append( this.rewrittenSQL ).append( "(" ).append( this.storageMan.isThereAnyResult( this.rewrittenSQL ) ).append( ")" )
						 .toString( ) );
		 }
		 if ( StorageManager.getInstance( ).isThereAnyResult( this.rewrittenSQL ) ) {
			 this.currentCount++;
			 AbstractVirtualSensor sensor = null;
			 if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( ).append( "Executing the main query for InputStream : " ).append( this.getInputStreamName( ) ).toString( ) );
			 int elementCounterForDebugging = -1;
			 final Enumeration < StreamElement > resultOfTheQuery = StorageManager.getInstance( ).executeQuery( this.rewrittenSQL , false );
			 try {
				 sensor = pool.borrowVS( );
				 while ( resultOfTheQuery.hasMoreElements( ) ) {
					 elementCounterForDebugging++;
					 sensor.dataAvailable( this.getInputStreamName( ) , resultOfTheQuery.nextElement( ) );
				 }
			 } catch ( final PoolIsFullException e ) {
				 logger.warn( "The stream element produced by the virtual sensor is dropped because of the following error : " );
				 logger.warn( e.getMessage( ) , e );
			 } catch ( final UnsupportedOperationException e ) {
				 logger.warn( "The stream element produced by the virtual sensor is dropped because of the following error : " );
				 logger.warn( e.getMessage( ) , e );
			 } catch ( final VirtualSensorInitializationFailedException e ) {
				 logger.error( "The stream element can't deliver its data to the virtual sensor " + sensor.getVirtualSensorConfiguration( ).getVirtualSensorName( )
						 + " because initialization of that virtual sensor failed" );
				 logger.error(e.getMessage(),e);
			 } finally {
				 this.pool.returnVS( sensor );
			 }
			 if ( logger.isDebugEnabled( ) ) {
				 logger.debug( new StringBuilder( ).append( "Input Stream's result has *" ).append( elementCounterForDebugging ).append( "* stream elements" ).toString( ) );
			 }
		 }
		 return true;
	 }

	 public void addToRenamingMapping ( final CharSequence aliasName , final CharSequence viewName ) {
		 this.rewritingData.put( aliasName , viewName );
	 }

	 public void refreshAlias ( final String alias ) {
		 if ( logger.isInfoEnabled( ) ) logger.info( "REFERES ALIAS CALEED" );
	 }

	 public boolean equals ( final Object o ) {
		 if ( this == o ) { return true; }
		 if ( !( o instanceof InputStream ) ) { return false; }

		 final InputStream inputStream = ( InputStream ) o;

		 if ( this.inputStreamName != null ? !this.inputStreamName.equals( inputStream.inputStreamName ) : inputStream.inputStreamName != null ) { return false; }
		 return true;
	 }

	 public int hashCode ( ) {
		 return ( this.inputStreamName != null ? this.inputStreamName.hashCode( ) : 0 );
	 }

	 public void finalize ( ) {
		 final HashMap map = new HashMap( );
		 this.finalize( map );
	 }

	 public void finalize ( final HashMap context ) {}

	 private transient boolean hasValidated             = false;

	 boolean                   cachedValidationResult = false;

	 public boolean validate ( ) {
		 if ( this.hasValidated ) return this.cachedValidationResult;
		 hasValidated=true;
		 if (sources==null || sources.length==0) {
			 logger.error("Input Stream "+getInputStreamName()+ " is not valid (No stream sources are specified), deployment failed !");
			 return false;
		 }
		 for ( final StreamSource ss : this.sources ) {
			 if ( !ss.validate( ) ) {
				 logger.error( new StringBuilder( ).append( "The Stream Source : " ).append( ss.getAlias( ) ).append( " specified in the Input Stream : " ).append( this.getInputStreamName( ) ).append(
				 " is not valid." ).toString( ) );
				 return (cachedValidationResult=false);
			 }
			 streamSourceAliasNameToStreamSourceName.put( ss.getAlias( ) , ss );
		 }
		 return (cachedValidationResult=true);   
	 }

	 /**
	  * @return the pool
	  */
	 public VirtualSensorPool getPool ( ) {
		 return pool;
	 }

	 /**
	  * @param pool the pool to set
	  */
	 public void setPool ( VirtualSensorPool pool ) {
		 this.pool = pool;
	 }
}
