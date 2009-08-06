package gsn2.conf;

import gsn.utils.Parameter;

import java.io.Serializable;
import java.util.Vector;
import java.util.Arrays;

public class Parameters implements Serializable {

	public static final Parameters EMPTY_PARAMETERS = new Parameters(null);

	private Parameter[] parameters;

	public Parameters() {this(null);}
	public Parameters(Parameter...newPredicates) {
		if (newPredicates==null)
			this.parameters=new  Parameter[0];
		else
			this.parameters = newPredicates;
	}

	public String getPredicateValueWithException ( String key ) {
		key = key.trim( );
		for (  Parameter predicate : this.parameters ) {
			if ( predicate.getName( ).toString( ).trim( ).equalsIgnoreCase( key ) ) {
				final String value = ( String ) predicate.getValue( );
				if (value.trim().length()>0)
					return ( value);
			}
		}
		throw new RuntimeException("The required parameter: >"+key+"<+ is missing.from the virtual sensor configuration file.");
	}


	/**
	 * Note that the key for the value is case insensitive.
	 * 
	 * @param key
	 * @return
	 */

	public String getPredicateValue ( String key ) {
		key = key.trim( );
		for (  Parameter predicate : this.parameters ) {
			//    logger.fatal(predicate.getName()+" --- " +predicate.getValue());
			if ( predicate.getName( ).toString( ).trim( ).equalsIgnoreCase( key ) ) return ( ( String ) predicate.getValue( ));
		}
		return null;
	}

	/**
	 * Gets a parameter name. If the parameter value exists and is not an empty string, returns the value otherwise returns the
	 * default value
	 * @param key The key to look for in the map.
	 * @param defaultValue Will be return if the key is not present or its an empty string.
	 * @return
	 */
	public String getPredicateValueWithDefault(String key, String defaultValue) {
		String value = getPredicateValue(key);
		if (value==null|| value.trim().length()==0)
			return defaultValue;
		else
			return value;
	}

	/**
	 * Gets a parameter name. If the parameter value exists and is a valid integer, returns the value otherwise returns the
	 * default value
	 * @param key The key to look for in the map.
	 * @param defaultValue Will be return if the key is not present or its value is not a valid integer.
	 * @return
	 */
	public int getPredicateValueAsInt(String key, int defaultValue) {
		String value = getPredicateValue(key);
		if (value==null|| value.trim().length()==0)
			return defaultValue;
		try { 
			return Integer.parseInt(value);
		}catch (Exception e) {
			return defaultValue;
		}
	}

	public int getPredicateValueAsIntWithException ( String key ) {
		String value = getPredicateValue(key);
		if (value==null|| value.trim().length()==0)
			throw new RuntimeException("The required parameter: >"+key+"<+ is missing.");
		try { 
			return Integer.parseInt(value);
		}catch (Exception e) {
			throw new RuntimeException("The required parameter: >"+key+"<+ is bad formatted.",e);
		}
	}

	public double getPredicateValueAsDoubleWithException(String key) {
		String value = getPredicateValue(key);
		if (value==null|| value.trim().length()==0)
			throw new RuntimeException("The required parameter: >"+key+"<+ is missing.");
		try { 
			return Double.parseDouble(value);
		}catch (Exception e) {
			throw new RuntimeException("The required parameter: >"+key+"<+ is bad formatted.",e);
		}
	}

	public Parameter[] getPredicates() {
		return parameters;
	}

	public String[] getPredicateValues(String key) {
		Vector<String> toReturn = new Vector<String>();
		for (  Parameter predicate : this.parameters ) {
			if ( predicate.getName( ).toString( ).trim( ).equalsIgnoreCase( key ) )
				toReturn.add((String) predicate.getValue( ));
		}
		return toReturn.toArray(new String[] {});

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

		retValue = "Parameters ( "
			+ super.toString() + TAB
			+ "predicates = " ;

		for (  Parameter predicate : this.parameters ) 
			retValue+= predicate.getName().toString()+"="+predicate.getValue()+" , ";

		retValue= TAB + " )";

		return retValue;
	}

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Parameters that = (Parameters) o;

        if (!Arrays.equals(parameters, that.parameters)) return false;

        return true;
    }

    public int hashCode() {
        return parameters != null ? Arrays.hashCode(parameters) : 0;
    }

}
