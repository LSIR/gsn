package gsn.http.datarequest;

import gsn.Main;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

public class QueriesBuilder {
	
	private static transient Logger 	logger 						= Logger.getLogger(QueriesBuilder.class);
	
	/* Mandatory Parameters */
	public static final String 			PARAM_VSNAMES_AND_FIELDS 	= "vsname";

	/* Optional Parameters */
	public static final String 			PARAM_AGGREGATE_CRITERIA	= "groupby";
	public static final String 			PARAM_STANDARD_CRITERIA		= "critfield";
	public static final String 			PARAM_MAX_NB				= "nb";
	public static final String			PARAM_TIME_FORMAT			= "timeformat";
	public static final String			PARAM_TIME_LINE				= "timeline";
	public static final String			PARAM_TEMP_ORDER			= "temporalorder";

	/* Parsed Parameters */
	private HashMap<String, FieldsCollection> 	vsnamesAndStreams 			= null;
	private AggregationCriterion 				aggregationCriterion 		= null;
	private ArrayList<StandardCriterion> 		standardCriteria 			= null;
	private LimitCriterion						limitCriterion				= null;
	private String timedfield;
	private String temporalorder;

	private Hashtable<String, AbstractQuery> sqlQueries ;

	protected Map<String, String[]> requestParameters;

	protected SimpleDateFormat sdf = new SimpleDateFormat (Main.getContainerConfig().getTimeFormat());
	
	private static Hashtable<String, String> allowedTimeFormats = null;
	
	static {
		allowedTimeFormats = new Hashtable<String, String> () ;
		allowedTimeFormats.put("iso","yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		allowedTimeFormats.put("unix", "unix");
	}
	
	public QueriesBuilder (Map<String, String[]> requestParameters) throws DataRequestException {
		this.requestParameters = requestParameters ;
		parseParameters();
		buildSQLQueries () ;
	}
	
	private void parseParameters () throws DataRequestException {

		String[] tf = requestParameters.get(PARAM_TIME_LINE);
		if (tf.length==1)
			timedfield = tf[0];
		else
			timedfield="timed";
		temporalorder = requestParameters.get(PARAM_TEMP_ORDER)[0];
		String[] vsnamesParameters = requestParameters.get(PARAM_VSNAMES_AND_FIELDS);

		if (vsnamesParameters == null) throw new DataRequestException ("You must specify at least one >" + PARAM_VSNAMES_AND_FIELDS + "< parameter.") ; 

		vsnamesAndStreams = new HashMap<String, FieldsCollection> () ;
		String name;
		String[] streams;
		for (int i = 0 ; i < vsnamesParameters.length ; i++) {
			int firstColumnIndex = vsnamesParameters[i].indexOf(':');
			if (firstColumnIndex == -1) {
				name = vsnamesParameters[i];
				streams = new String[0];
			}
			else {
				name = vsnamesParameters[i].substring(0, firstColumnIndex);
				streams = vsnamesParameters[i].substring(firstColumnIndex + 1).split(",");
			}

			vsnamesAndStreams.put(name, new FieldsCollection (streams, timedfield));
		}

		String ac = getParameter(requestParameters, PARAM_AGGREGATE_CRITERIA);
		if (ac != null) { aggregationCriterion = new AggregationCriterion (ac) ; }

		String[] cc = requestParameters.get(PARAM_STANDARD_CRITERIA);
		if (cc != null) {
			standardCriteria = new ArrayList<StandardCriterion> ();
			for (int i = 0 ; i < cc.length ; i++) {
				standardCriteria.add(new StandardCriterion (cc[i]));
			}
		}

		String lm = getParameter(requestParameters, PARAM_MAX_NB);
		if (lm != null) limitCriterion = new LimitCriterion (lm);
		
		String timeformat = getParameter(requestParameters, PARAM_TIME_FORMAT);
		if (timeformat != null && allowedTimeFormats.containsKey(timeformat)) {
			String format = allowedTimeFormats.get(timeformat);
			sdf = format.compareToIgnoreCase("unix") == 0 ? null : new SimpleDateFormat(format) ;
		}
	}

	public Hashtable<String, AbstractQuery> getSqlQueries() {
		return sqlQueries;
	}

	private void buildSQLQueries () {

		this.sqlQueries = new Hashtable<String, AbstractQuery> () ;

		// Fields and Virtual Sensors
		Iterator<Entry<String, FieldsCollection>> iter 	= vsnamesAndStreams.entrySet().iterator();
		Entry<String, FieldsCollection> next;
		String[] fields;
		String vsname;
		while (iter.hasNext()) {
			next = iter.next();
			fields = next.getValue().getFields();
			vsname = next.getKey();
			this.sqlQueries.put(vsname, new AbstractQuery(limitCriterion, aggregationCriterion, vsname, fields, standardCriteria, timedfield, temporalorder));
		}
	}

	public AggregationCriterion getAggregationCriterion() {
		return aggregationCriterion;
	}

	public ArrayList<StandardCriterion> getStandardCriteria() {
		return standardCriteria;
	}

	public HashMap<String, FieldsCollection> getVsnamesAndStreams() {
		return vsnamesAndStreams;
	}

	public LimitCriterion getLimitCriterion() {
		return limitCriterion;
	}



	/**
	 * Returns the value of a request parameter as a <code>String</code>, or <code>null</code> if the parameter does not exist.
	 */
	protected static String getParameter (Map<String, String[]> parameters, String requestedParameter) {
		String[] rpv = parameters.get(requestedParameter);
		return (rpv == null || rpv.length == 0) ? null : rpv[0];
	}

	public SimpleDateFormat getSdf() {
		return sdf;
	}


}