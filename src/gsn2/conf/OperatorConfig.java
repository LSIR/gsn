package gsn2.conf;

import gsn.ConfigurationVisitor;
import gsn.Visitable;
import gsn.beans.DataField;
import gsn.beans.WebInput;

public class OperatorConfig implements Visitable{
	private String className="";
	private boolean uniqueTimestamp;
	private Parameters parameters=new Parameters();
	private DataField[] outputFormat = new DataField[0];
	private WebInput[] webInputs = new WebInput[0];
	
	public String getClassName() {
		if ( this.className == null ) 
			this.className = "gsn.vsensor.BridgeVirtualSensor";
		return this.className;
	}
	
	public void accept(ConfigurationVisitor v) {
		v.visit(this);
	}
	
	
	/***
	 * Auto-generated code follows:
	 */

	public void setClassName(String className) {
		this.className = className;
	}
	public boolean isUniqueTimestamp() {
		return uniqueTimestamp;
	}
	public void setUniqueTimestamp(boolean uniqueTimestamp) {
		this.uniqueTimestamp = uniqueTimestamp;
	}
	public Parameters getParameters() {
		return parameters;
	}
	public void setParameters(Parameters parameters) {
		this.parameters = parameters;
	}
	public DataField[] getOutputFormat() {
		return outputFormat;
	}
	public void setOutputFormat(DataField[] outputFormat) {
		this.outputFormat = outputFormat;
	}
	public WebInput[] getWebInputs() {
		return webInputs;
	}
	public void setWebInputs(WebInput[] webInputs) {
		this.webInputs = webInputs;
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
	    
	    retValue = "OperatorConfig ( "
	        + super.toString() + TAB
	        + "className = " + this.className + TAB
	        + "uniqueTimestamp = " + this.uniqueTimestamp + TAB
	        + "parameters = " + this.parameters + TAB
	        + "outputFormat = " + this.outputFormat + TAB
	        + "webInputs = " + this.webInputs + TAB
	        + " )";
	
	    return retValue;
	}


	
}
