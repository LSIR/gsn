package gsn.http.datarequest;

import gsn.Main;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;

public class StandardCriterion extends AbstractCriterion {

	private static Hashtable<String, String> allowedOp 		= null;
	private static Hashtable<String, String> allowedJoin 	= null;
	private static Hashtable<String, String> allowedNeg 	= null;

	private String critJoin 	= null;
	private String critNeg  	= null;
	private String critField	= null;
	private String critVsname	= null;
	private String critValue	= null;
	private String critOperator	= null;
	
	private static SimpleDateFormat sdf = new SimpleDateFormat (Main.getContainerConfig().getTimeFormat());

	static {
		allowedOp = new Hashtable<String, String> () ;
		allowedOp.put("le", "<");
		allowedOp.put("leq", "<=");
		allowedOp.put("ge", ">");
		allowedOp.put("geq", ">=");
		allowedOp.put("eq", " equal");
		allowedOp.put("like", "like");
		//
		allowedJoin = new Hashtable<String, String> () ;
		allowedJoin.put("or", "or");
		allowedJoin.put("and", "and");
		//
		allowedNeg = new Hashtable<String, String> () ;
		allowedNeg.put("", "");
		allowedNeg.put("not", "not");
	}

	/**
	 * <p>
	 * Create a new Custom Criteria from a serialized Criteria description.
	 * The description must follow the syntax:<br />
	 * <code><critJoin>:<negation>:<vsname>:<field>:<operator>:<value></code>.
	 * 
	 * Note that if <vsname> is blank then the criteria applies to the field of all virtual sensors.
	 * </p>
	 * @param inlinecrits
	 * @return
	 */
	public StandardCriterion (String inlinecrits) throws DataRequestException {

		String[] crits = inlinecrits.split(":");

		if (crits.length != 6) throw new DataRequestException (GENERAL_ERROR_MSG + " >" + inlinecrits + "<.") ;

		critJoin 		= getCriterion(crits[0], allowedJoin);
		critNeg			= getCriterion(crits[1], allowedNeg);
		critVsname		= crits[2];
		critField 		= crits[3];
		critOperator	= getCriterion(crits[4], allowedOp);
		critValue		= crits[5];
	}
	
	public String toString () {
		String hrtf = critField.compareToIgnoreCase("timed") == 0 ? sdf.format(new Date (Long.parseLong(critValue))) : critValue;
		return critJoin + " " + critNeg + " " + critVsname + " " + critField + " " + critOperator + " " + hrtf;
	}

	public String getCritJoin() { return this.critJoin; }
	public String getNegation() { return this.critNeg; }
	public String getVsname()	{ return this.critVsname; }
	public String getField()    { return this.critField; }
	public String getValue()    { return this.critValue; }
	public String getOperator()	{ return this.critOperator; }
}
