/**
 * 
 */
package gsn.utils.protocols.EPuck;

import java.util.Vector;


/**
 * @author alisalehi
 *
 */
public class Reset extends HCIQuery {

   public Reset () {
      super( "RESET" );
   }
 

   /*
    * This query does not take any parameters.
    * If you provide any, these will be ignored.
    */
   public byte [ ] buildRawQuery ( Vector < Object > params ) {
      byte[] query = new byte[1];
      query[0] = 'r';
      return query;
   }
   
}
