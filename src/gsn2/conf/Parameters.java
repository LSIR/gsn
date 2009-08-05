package gsn2.conf;

import java.util.Vector;

import org.apache.commons.collections.KeyValue;

public class Parameters {

	public static final Parameters EMPTY_PARAMETERS = new Parameters(null);

	private KeyValue[] predicates;

	public Parameters(KeyValue...newPredicates) {
		if (newPredicates==null)
			this.predicates=new  KeyValue[0];
		else
			this.predicates = newPredicates;
	}

	public String getPredicateValueWithException ( String key ) {
		key = key.trim( );
		for (  KeyValue predicate : this.predicates ) {
			if ( predicate.getKey( ).toString( ).trim( ).equalsIgnoreCase( key ) ) {
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
		for (  KeyValue predicate : this.predicates ) {
			//    logger.fatal(predicate.getKey()+" --- " +predicate.getValue());
			if ( predicate.getKey( ).toString( ).trim( ).equalsIgnoreCase( key ) ) return ( ( String ) predicate.getValue( ));
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

	public KeyValue[] getPredicates() {
		return predicates;
	}

	public String[] getPredicateValues(String key) {
		Vector<String> toReturn = new Vector<String>();
		for (  KeyValue predicate : this.predicates ) {
			if ( predicate.getKey( ).toString( ).trim( ).equalsIgnoreCase( key ) )
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

		for (  KeyValue predicate : this.predicates ) 
			retValue+= predicate.getKey().toString()+"="+predicate.getValue()+" , ";

		retValue= TAB + " )";

		return retValue;
	}




	// TODO: Implementing the equals and hashchode methods.
}
