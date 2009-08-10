package gsn.beans;

import gsn.channels.DataChannel;
import gsn.core.OperatorConfig;

import java.io.Serializable;
import java.util.List;

import org.apache.log4j.Logger;

public class SQLOperator implements Serializable,Operator{
	
	private transient static final Logger               logger                = Logger.getLogger( SQLOperator.class );
	
	private OperatorConfig config;

	private DataChannel channel;

	public SQLOperator(OperatorConfig config, DataChannel channel) {
		this.config = config;
		this.channel = channel;
		// parsing the query and getting the table names.
		// creating the h2 engine with no database inside yet.
		// creating an empty mapping between the table names and their structures as extracted from the future stream elements.
	}
	
	public void process(String name, List<StreamElement> window) {
		// update the mapping.
		// if there is any item in the mapping that we don't have the appropriate stream element for it, return. (null join)
		// remove the table named name if exist.
		// create a table called name, dump the stream elements into it.
		// execute the query.
		// put the results of the query into a list.
		// write the list into the channel.
		// perform manual sliding.
	}

  public DataField[] getStructure() {
    return new DataField[0];  
  }

  public OperatorConfig getConfig() {
		return config;
	}

	public void start() {
		System.out.println("InputStream started:"+config.toString());
	}

	public void stop() {
		
	}

	public void dispose() {
		
	}

	/**
	 * Constructs a <code>String</code> with all attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	public String toString()
	{
	    final String TAB = "    ";
	    
	    String retValue = "";
	    
	    retValue = "SQLOperator ( "
	        + super.toString() + TAB
	        + "config = " + this.config + TAB
	        + "channel = " + this.channel + TAB
	        + " )";
	
	    return retValue;
	}

	
//	public boolean executeQuery( final CharSequence alias ) throws SQLException{
//		if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( ).append( "Notified by StreamSource on the alias: " ).append( alias ).toString( ) );
//
//		if (  StorageManager.getInstance( ).isThereAnyResult( this.rewrittenSQL ) ) {
//			if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( ).append( "Executing the main query for InputStream : " ).append( this.getInputStreamName( ) ).toString( ) );
//
//			final Enumeration < StreamElement > resultOfTheQuery = StorageManager.getInstance( ).executeQuery( this.rewrittenSQL , false );
//			try {
//				while ( resultOfTheQuery.hasMoreElements( ) ) {
//					elementCounterForDebugging++;
//					StreamElement element= resultOfTheQuery.nextElement( );
//					sensor.dataAvailable( this.getInputStreamName( ) , element );
//				}
//			}
//		}
//		if ( logger.isDebugEnabled( ) ) {
//			logger.debug( new StringBuilder( ).append( "Input Stream's result has *" ).append( elementCounterForDebugging ).append( "* stream elements" ).toString( ) );
//		}
//	}
	
}
