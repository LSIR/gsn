package gsn2.conf;

import gsn.ConfigurationVisitor;
import gsn.Visitable;

public class SQLOperatorConfig implements Visitable{

	public void accept(ConfigurationVisitor v) {
		v.visit(this);
	}

	private String                                      query;

	/**
	 * Auto-generated methods follow 
	 */
	public String getQuery() {
		return query;
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
	    
	    retValue = "SQLOperatorConfig ( "
	        + super.toString() + TAB
	        + "query = " + this.query + TAB
	        + " )";
	
	    return retValue;
	}

	


}
