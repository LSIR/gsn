package gsn.http.datarequest;

import gsn.Main;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.log4j.Logger;

/**
 * <p>
 * This class provides a generic and fine grained way to select data for a set of Virtual Sensors.
 * For each of the specified Virtual Sensors it creates a SQL query that can be directly executed to
 * access the data.
 * </p>
 * <p>
 * For each Virtual Sensor, the Fields can be selected. Moreover, The three following types of filters 
 * can be added to the queries. Notice that these filters are the same for all the generated queries.
 * </p>
 * 
 * <ul>
 * <li><strong>MAX NUMBER OF RESULTS</strong> This option limits the number of returned values to a maximal value.</li>
 * <li><strong>STANDARD CRITERIA</strong> Almost the SQL tests can be added to the SQL queries.</li>
 * <li><strong>AGGREGATION CRITERION</strong> A SQL grouping function can be added to the queries.</li>
 * </ul>
 * 
 * <h3>Examples</h3>
 * <ul>
 * <li><strong>Minimal Parameters:</strong> <code>?vsname=ss_mem_vs:heap_memory_usage</code><br /> This request return a SQL query that select all the <code>heap_memory_usage</code> values from the <code>ss_me_vs</code> Virtual Sensor</li>
 * <li>
 *      <strong>Typical Parameters:</strong> <code>?vsname=tramm_meadows_vs:toppvwc_1:toppvwc_3&vsname=ss_mem_vs:heap_memory_usage&nb=0:5&critfield=and:::timed:ge:1201600800000&critfield=and:::timed:le:1211678800000&groupby=10000000:min</code><br />
 *      This request returns two SQL queries, one for <code>tramm_meadows_vs</code> and one for <code>ss_mem_vs</code> Virtual Sensor. The number of elements returned is limited to 5, are associated to a timestamp between <code>1201600800000</code> and <code>1211678800000</code>.
 *      The elements returned are the minimals values grouped by the timed field divided by <code>10000000</code>.
 * </li>
 * </ul>
 * 
 * <ul>
 * <li>Notice that by the <code>timed</code> Field is associated to all the Virtual Sensors elements returned.</li>
 * <li>Notice that this class doesn't check if the Virtual Sensors and Fields names are corrects.</li>
 * </ul>
 */
public abstract class AbstractDataRequest {

	private static transient Logger 	logger 						= Logger.getLogger(AbstractDataRequest.class);

	/* Mandatory Parameters */
	public static final String 			PARAM_VSNAMES_AND_FIELDS 	= "vsname";

	/* Optional Parameters */
	public static final String 			PARAM_AGGREGATE_CRITERIA	= "groupby";
	public static final String 			PARAM_STANDARD_CRITERIA		= "critfield";
	public static final String 			PARAM_MAX_NB				= "nb";

	/* Parsed Parameters */
	private HashMap<String, FieldsCollection> 	vsnamesAndStreams 			= null;
	private AggregationCriterion 				aggregationCriterion 		= null;
	private ArrayList<StandardCriterion> 		standardCriteria 			= null;
	private LimitCriterion						limitCriterion				= null;

	private Hashtable<String, AbstractQuery> sqlQueries ;

	protected Map<String, String[]> requestParameters;

	protected static SimpleDateFormat sdf = new SimpleDateFormat (Main.getContainerConfig().getTimeFormat());

	public AbstractDataRequest (Map<String, String[]> requestParameters) throws DataRequestException {
		this.requestParameters = requestParameters ; 
		parseParameters();
		buildSQLQueries () ;
	}

	private void parseParameters () throws DataRequestException {

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
				streams = vsnamesParameters[i].substring(firstColumnIndex + 1).split(":");
			}

			vsnamesAndStreams.put(name, new FieldsCollection (streams));
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
		StringBuilder partFields;
		StringBuilder partVS;
		StringBuilder sqlQuery;
		while (iter.hasNext()) {

			next = iter.next();
			fields = next.getValue().getFields();
			vsname = next.getKey();

			// Standard Criteria
			StringBuilder partStandardCriteria = new StringBuilder () ;
			if (standardCriteria != null) {
				StandardCriterion lastStandardCriterionLinkedToVs = null;
				StandardCriterion cc ;
				for (int i = 0 ; i < standardCriteria.size() ; i++) {
					cc = standardCriteria.get(i);
					if (cc.getVsname().compareTo("") == 0 || cc.getVsname().compareToIgnoreCase(vsname) == 0) {

						if (lastStandardCriterionLinkedToVs != null) {
							partStandardCriteria.append(lastStandardCriterionLinkedToVs.getCritJoin() + " " + cc.getNegation() + " " + cc.getField() + " " + cc.getOperator() + " ");
						}
						else {
							partStandardCriteria.append(cc.getNegation() + " " + cc.getField() + " " + cc.getOperator() + " ");
						}

						lastStandardCriterionLinkedToVs = cc;

						if (cc.getOperator().compareToIgnoreCase("like") == 0) partStandardCriteria.append("'%");

						partStandardCriteria.append(cc.getValue());

						if (cc.getOperator().compareToIgnoreCase("like") == 0) partStandardCriteria.append("%'");
						partStandardCriteria.append(" ");
					}
				}
				if (lastStandardCriterionLinkedToVs != null) partStandardCriteria.insert(0, "where ");				
			}

			partFields = new StringBuilder () ;
			for (int i = 0 ; i < fields.length ; i++) {
				if (fields[i].equalsIgnoreCase("timed") && aggregationCriterion!=null)
					continue;
				if (partFields.length()>0)
					partFields.append(", ");
				if (aggregationCriterion != null) 	partFields.append(aggregationCriterion.getGroupOperator() + "(");
				partFields.append(fields[i]);
				if (aggregationCriterion != null)	partFields.append(") as " + fields[i]);

			}										

			if (aggregationCriterion != null) {
				if (partFields.length() > 0)
					partFields.append(", ");
				partFields.append("floor(timed/" + aggregationCriterion.getTimeRange() + ") as timed ");
			}
			else 								partFields.append(" ");

		
			// Build a final query
			sqlQuery = new StringBuilder();
			sqlQuery.append("select ");
			sqlQuery.append(partFields);
			sqlQuery.append("from ").append(vsname).append(" ");
			sqlQuery.append(partStandardCriteria);
			if (aggregationCriterion == null)	sqlQuery.append("order by timed desc ");
			else 								sqlQuery.append("group by timed desc ");

			logger.debug("SQL Query built >" + sqlQuery.toString() + "<");

			this.sqlQueries.put(vsname, new AbstractQuery(sqlQuery, limitCriterion));
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
	 * This class stores a list of Fields for a Virtual Sensor. It adds by default the 
	 * <code>timed</code> field if missing and keep track if the <code>timed</code> was
	 * needed or not.
	 */
	public class FieldsCollection {

		private boolean 	wantTimed;
		private String[] 	fields;

		public FieldsCollection (String[] _fields) {

			wantTimed = false;
			for (int j = 0 ; j < _fields.length ; j++) {
				if (_fields[j].compareToIgnoreCase("timed") == 0) wantTimed = true;
			}
			String[] tmp = _fields;
			if (! wantTimed ) {
				tmp = new String[_fields.length + 1] ;
				System.arraycopy(_fields, 0, tmp, 0, _fields.length);
				tmp[tmp.length - 1] = "timed";
			}
			this.fields = tmp;
		}

		public boolean isWantTimed() {
			return wantTimed;
		}

		public String[] getFields() {
			return fields;
		}
	}

	/**
	 * Returns the value of a request parameter as a <code>String</code>, or <code>null</code> if the parameter does not exist.
	 */
	protected static String getParameter (Map<String, String[]> parameters, String requestedParameter) {
		String[] rpv = parameters.get(requestedParameter);
		return (rpv == null || rpv.length == 0) ? null : rpv[0];
	}

	public abstract void process() throws DataRequestException ;

	public abstract void outputResult (OutputStream os) ;

}
