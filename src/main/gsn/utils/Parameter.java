package gsn.utils;

import java.io.Serializable;

import org.apache.commons.collections.KeyValue;
import org.apache.log4j.Logger;

/**
 * The <I> Predicate </I> class represents mapping between a key and a value.
 * Two predicates are the same if they have the same name and value. This class
 * is used for Abstract Addressing and Options.
 */
public class Parameter implements Serializable {
   
	private static final long serialVersionUID = 5739537343169906104L;

	private transient final Logger logger = Logger.getLogger( Parameter.class );
   
   private String                 name;
   
   private String                 value;
   
   public Parameter ( ) {

   }
   
   public Parameter ( String key , String value ) {
      this.name = key;
      this.value = value;
   }
   
  
   public void setValue ( String value ) {
      this.value = value;
   }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Parameter parameter = (Parameter) o;

        if (name != null ? !name.equals(parameter.name) : parameter.name != null) return false;
        if (value != null ? !value.equals(parameter.value) : parameter.value != null) return false;

        return true;
    }

    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    public String getName() {
	return name;
}

public void setName(String name) {
	this.name = name;
}

/**
    * Converts the value inside the predicate object to boolean or returns
    * false.
    * 
    * @return The boolean representation of the value or false if the conversion
    * fails.
    */
   public boolean valueInBoolean ( ) {
      boolean result = false;
      try {
         result = Boolean.parseBoolean( getValue( ) );
      } catch ( Exception e ) {
         logger.error( e.getMessage( ) , e );
      }
      return result;
   }
   
   /**
    * Converts the value inside the predicate object to integer or returns 0.
    * 
    * @return The integer representation of the value or 0 plus contents of
    * stack trace if the conversion fails.
    */
   public int valueInInteger ( ) {
      int result = 0;
      try {
         result = Integer.parseInt( getValue( ) );
      } catch ( Exception e ) {
         logger.error( e.getMessage( ) , e );
      }
      return result;
   }
   
   public String toString ( ) {
      StringBuffer result = new StringBuffer( );
      result.append( "Predicate ( Key = " ).append( name ).append( ", Value = " ).append( value ).append( " )\n" );
      return result.toString( );
   }
   
   public String getValue ( ) {
      return this.value;
   }
   
}
