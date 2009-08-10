package gsn2.wrappers;

import gsn.ConfigurationVisitor;
import gsn.utils.Parameter;
import gsn2.conf.Parameters;

import java.io.Serializable;

public final class WrapperConfig implements Serializable{

	private String className;
	
	private Parameters parameters;

    public WrapperConfig () {/* Required by Jibx */ }
	
	public WrapperConfig (  String className , Parameters parameters) {
		this.parameters = parameters;
		this.className = className;
	}


	public WrapperConfig (  String className , Parameter... parameters) {
		this.parameters = new Parameters(parameters);
		this.className = className;
	}

	public WrapperConfig ( final String wrapper  ) {
		this.className = wrapper;
		parameters = Parameters.EMPTY_PARAMETERS;
	}
	
	/***
	 * Start of auto-generated code
	 */

	public Parameters getParameters() {
		return parameters;
	}

	public String getClassName( ) {
		return this.className;
	}
	
	public void accept(ConfigurationVisitor v) {
		v.visit(this);
	}

	public String toString ( ) {
		final StringBuffer result = new StringBuffer( "[" ).append( this.getClassName( ) );
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
				+ ((className == null) ? 0 : className.hashCode());
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
		if (className == null) {
			if (other.className != null)
				return false;
		} else if (!className.equals(other.className))
			return false;
		return true;
	}


}
