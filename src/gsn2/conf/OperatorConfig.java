package gsn2.conf;

import java.io.Serializable;

import gsn.ConfigurationVisitor;
import gsn.Visitable;
import gsn.beans.DataField;
import gsn.beans.WebInput;
import gsn.beans.WrapperConfig;

public class OperatorConfig implements Visitable,Serializable{
	private String className="";
	private String identifier="";
	private boolean uniqueTimestamp;
	private Parameters parameters=new Parameters();
	private ChannelConfig[] channels ;
	
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

	public String getIdentifier() {
		return identifier;
	}

	public boolean isUniqueTimestamp() {
		return uniqueTimestamp;
	}

	public Parameters getParameters() {
		return parameters;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public void setUniqueTimestamp(boolean uniqueTimestamp) {
		this.uniqueTimestamp = uniqueTimestamp;
	}

	public void setParameters(Parameters parameters) {
		this.parameters = parameters;
	}
	

	
}
