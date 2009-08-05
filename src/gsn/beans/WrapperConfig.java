package gsn.beans;

import gsn.ConfigurationVisitor;
import gsn2.conf.Parameters;

import java.io.Serializable;

public final class WrapperConfig implements Serializable{

	private String                 wrapperName;
	
	private Parameters parameters;

	public WrapperConfig (  String wrapperName , Parameters parameters) {
		this.parameters = parameters;
		this.wrapperName = wrapperName;
	}

	public WrapperConfig ( final String wrapper  ) {
		this.wrapperName = wrapper;
		parameters = Parameters.EMPTY_PARAMETERS;
	}
	
	/***
	 * Start of auto-generated code
	 */

	public Parameters getParameters() {
		return parameters;
	}

	public String getWrapperName ( ) {
		return this.wrapperName;
	}
	
	public void accept(ConfigurationVisitor v) {
		v.visit(this);
	}

	public String toString ( ) {
		final StringBuffer result = new StringBuffer( "[" ).append( this.getWrapperName( ) );
		result.append(parameters.toString());
		result.append( "]" );
		return result.toString( );
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((parameters == null) ? 0 : parameters.hashCode());
		result = prime * result
				+ ((wrapperName == null) ? 0 : wrapperName.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WrapperConfig other = (WrapperConfig) obj;
		if (parameters == null) {
			if (other.parameters != null)
				return false;
		} else if (!parameters.equals(other.parameters))
			return false;
		if (wrapperName == null) {
			if (other.wrapperName != null)
				return false;
		} else if (!wrapperName.equals(other.wrapperName))
			return false;
		return true;
	}
	

}
