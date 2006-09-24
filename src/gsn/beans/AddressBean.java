package gsn.beans ;

import gsn.utils.KeyValueImp ;

import java.util.ArrayList ;

import org.apache.commons.collections.KeyValue ;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 *         Date: Aug 4, 2005 <br>
 *         Time: 10:46:16 PM <br>
 */
public final class AddressBean {

   private String wrapper ;

   private ArrayList < KeyValue > predicates ;

   public AddressBean ( ) {
   }

   public AddressBean ( String wrapper , ArrayList < KeyValue > predicates ) {
      this.wrapper = wrapper ;
      this.predicates = predicates ;
   }

   /**
    * @param predicates
    *           The predicates to set.
    */
   public void setPredicates ( ArrayList < KeyValue > predicates ) {
      this.predicates = predicates ;
   }

   /**
    * @param wrapper
    *           The wrapper to set.
    */
   public void setWrapper ( String wrapper ) {
      this.wrapper = wrapper ;
   }

   public String getWrapper ( ) {
      return wrapper ;
   }

   public ArrayList < KeyValue > getPredicates ( ) {
      return predicates ;
   }

   public void addPredicate ( String key , String value ) {
      predicates.add ( new KeyValueImp ( key , value ) ) ;
   }

   /**
    * Note that the key for the value is case insensitive.
    * 
    * @param key
    * @return
    */
   public String getPredicateValue ( String key ) {
      key = key.trim ( ) ;
      for ( KeyValue predicate : predicates ) {
         if ( predicate.getKey ( ).toString ( ).trim ( ).equalsIgnoreCase ( key ) )
            return ( ( String ) predicate.getValue ( ) ).trim ( ) ;
      }
      return null ;
   }

   /**
    * Returns true TIMEDever the set of predicates contain "port" and "host"
    * keys.
    * 
    * @return
    */
   public boolean isAbsoluteAddressSpecified ( ) {
      boolean containsAddress = false ;
      boolean containsPort = false ;
      for ( KeyValue predicate : getPredicates ( ) ) {
         if ( "host".equalsIgnoreCase ( ( String ) predicate.getKey ( ) ) )
            containsAddress = true ;
      }
      for ( KeyValue predicate : getPredicates ( ) ) {
         if ( "port".equalsIgnoreCase ( ( String ) predicate.getKey ( ) ) )
            containsPort = true ;
      }
      return containsAddress && containsPort ;
   }

   public boolean equals ( Object o ) {
      if ( this == o )
         return true ;
      if ( o == null || getClass ( ) != o.getClass ( ) )
         return false ;

      final AddressBean addressBean = ( AddressBean ) o ;

      if ( ! predicates.equals ( addressBean.predicates ) )
         return false ;
      if ( ! wrapper.equals ( addressBean.wrapper ) )
         return false ;

      return true ;
   }

   public int hashCode ( ) {
      int result ;
      result = wrapper.hashCode ( ) ;
      result = 29 * result + predicates.hashCode ( ) ;
      return result ;
   }

   public String toString ( ) {
      StringBuffer result = new StringBuffer ( "[" ).append ( getWrapper ( ) ) ;
      for ( KeyValue predicate : predicates ) {
         result.append ( predicate.getKey ( ) + " = " + predicate.getValue ( ) + "," ) ;
      }
      result.append ( "]" ) ;
      return result.toString ( ) ;
   }
}
