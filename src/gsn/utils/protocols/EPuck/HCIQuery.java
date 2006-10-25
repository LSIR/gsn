/**
 * 
 */
package gsn.utils.protocols.EPuck;

import java.util.Vector;

import gsn.utils.protocols.AbstractHCIQuery;


/**
 * @author alisalehi
 *
 */
public class HCIQuery extends AbstractHCIQuery {
   
   private static String QUERY_NAME;
   
   public HCIQuery(String Name) {
      QUERY_NAME=Name;
   }

   public byte [ ] buildRawQuery ( Vector < Object > params ) {
      return null;
   }
   
   public String getName ( ) {
      return QUERY_NAME;
   }
   
   // we usually dont expect an answer
   public int getWaitTime ( Vector < Object > params ) {
      // TODO Auto-generated method stub
      return NO_WAIT_TIME;
   }
   
   /* 
    * By default we dont expect an answer. 
    */
   public boolean needsAnswer ( Vector < Object > params ) {
      return false;
   }
   
}
