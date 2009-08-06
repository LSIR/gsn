package gsn2.conf;

import gsn.ConfigurationVisitor;
import gsn.Visitable;

import org.apache.log4j.Logger;

public class StreamConfig implements Visitable {

	public static final String DEFAULT_QUERY = "select * from wrapper";

	private static final transient Logger logger             = Logger.getLogger( StreamConfig.class );

	private static final String DEFAULT_SLIDE_VALUE =  "1";

	private String                        name;

	private float                         samplingRate       ;

	private String                        windowSize = null;

	private String                        sliding = null;

	private String                        query;

	public void accept(ConfigurationVisitor v) {
		v.visit(this);
	}
	/**
	 * Auto-generated codes
	 */

	public String getName() {
		return name;
	}

	public float getSamplingRate() {
		return samplingRate;
	}

	public String getWindowSize() {
		return windowSize;
	}

	public String getSliding() {
		return sliding != null ? sliding: String.valueOf(DEFAULT_SLIDE_VALUE);
	}

	/**
	 * @return Returns the sqlQuery.
	 */
	public String getSqlQuery ( ) {
		if (query==null || query.trim( ).length( ) == 0 ) query = DEFAULT_QUERY;
		return query;
	}

	public String toString(){
		final String TAB = "    ";

		String retValue = "";

		retValue = "StreamConfig ( "
			+ super.toString() + TAB
			+ "name = " + this.name + TAB
			+ "samplingRate = " + this.samplingRate + TAB
			+ "windowSize = " + this.windowSize + TAB
			+ "sliding = " + this.sliding + TAB
			+ "query = " + this.query + TAB
			+ " )";

		return retValue;
	}

}
