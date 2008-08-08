package gsn.http.datarequest;

import gsn.Main;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import javax.servlet.ServletException;

public class StandardCriterion extends AbstractCriterion {

	private static Hashtable<String, String> allowedOp 		= null;
	private static Hashtable<String, String> allowedJoin 	= null;
	private static Hashtable<String, String> allowedNeg 	= null;

	private String critJoin 	= null;
	private String critNeg  	= null;
	private String critField	= null;
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
	 * <code><critJoin>:<negation>:<field>:<operator>:<value></code>.
	 * </p>
	 * @param inlinecrits
	 * @return
	 */
	public StandardCriterion (String inlinecrits) throws ServletException {

		String[] crits = inlinecrits.split(":");

		if (crits.length != 5) throw new ServletException (GENERAL_ERROR_MSG + " >" + inlinecrits + "<.") ;

		critJoin 		= getCriterion(crits[0], allowedJoin);
		critNeg			= getCriterion(crits[1], allowedNeg);
		critOperator	= getCriterion(crits[3], allowedOp);
		critField 		= crits[2];
		critValue		= crits[4];
	}
	
	public String toString () {
		String hrtf = critField.compareToIgnoreCase("timed") == 0 ? sdf.format(new Date (Long.parseLong(critValue))) : critValue;
		return critJoin + " " + critNeg + " " + critField + " " + critOperator + " " + hrtf;
	}

	public String getCritJoin() { return this.critJoin; }
	public String getNegation() { return this.critNeg; }
	public String getField()    { return this.critField; }
	public String getValue()    { return this.critValue; }
	public String getOperator()	{ return this.critOperator; }
}
