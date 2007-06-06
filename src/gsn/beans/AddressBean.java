package gsn.beans;

import java.io.Serializable;

import org.apache.commons.collections.KeyValue;

/**
 * Date: Aug 4, 2005 <br>
 * Time: 10:46:16 PM <br>
 */
public final class AddressBean implements Serializable{
  
  private static final   KeyValue[] EMPTY_PREDICATES = new  KeyValue[0];
  
  private String                 wrapper;
  
  private  KeyValue[] predicates  = EMPTY_PREDICATES;
  
  public AddressBean ( final String wrapper , KeyValue... newPredicates ) {
    this.wrapper = wrapper;
    if (newPredicates==null)
      this.predicates=EMPTY_PREDICATES;
    else
      this.predicates = newPredicates;
  }
  
  public AddressBean ( final String wrapper  ) {
    this.wrapper = wrapper;
    this.predicates=EMPTY_PREDICATES;
  }
  
  public String getWrapper ( ) {
    return this.wrapper;
  }
  
  public  KeyValue[] getPredicates ( ) {
    return this.predicates;
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
      if ( predicate.getKey( ).toString( ).trim( ).equalsIgnoreCase( key ) ) return ( ( String ) predicate.getValue( ) ).trim( );
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

  
  public boolean equals ( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || this.getClass( ) != o.getClass( ) ) return false;
    
    final AddressBean addressBean = ( AddressBean ) o;
    if ( !this.wrapper.equals( addressBean.wrapper ) ) return false;
    
    if(predicates.length != addressBean.predicates.length)
    	return false;
    for ( final KeyValue predicate : this.predicates ) {
    	boolean isEqual = false;
    	for ( final KeyValue predicate2 : addressBean.predicates ){
        	if(predicate.equals(predicate2)){
        		isEqual = true;
        		break;
        	}
        }
    	if(!isEqual)
    		return false;
    }
    
    return true;
  }
  
  public int hashCode ( ) {
    int result;
    result = this.wrapper.hashCode( );
    for ( final KeyValue predicate : this.predicates ) {
    	result = 29 * result + predicate.hashCode();
    }
    return result;
  }
  
  public String toString ( ) {
    final StringBuffer result = new StringBuffer( "[" ).append( this.getWrapper( ) );
    for ( final KeyValue predicate : this.predicates ) {
      result.append( predicate.getKey( ) + " = " + predicate.getValue( ) + "," );
    }
    result.append( "]" );
    return result.toString( );
  }
  
  
}
