/**
 * 
 */
package gsn.utils.protocols.EPuck;

import java.util.HashMap;
import java.util.Vector;

import gsn.utils.protocols.AbstractHCIProtocol;
import gsn.utils.protocols.AbstractHCIQuery;


/**
 * @author alisalehi
 *
 */
public class SerComProtocol extends AbstractHCIProtocol {
   
   public static final String EPUCK_PROTOCOL="EPUCK_PROTOCOL";
   
   // add here an easy-to-use-and-remember name for each query class of the protocol
   public static final String SET_SPEED="SET_SPEED", RESET="RESET";
   
   private HashMap<String, HCIQuery> queries;
   
   public SerComProtocol() {

      queries = new HashMap<String, HCIQuery>();

      // Create and add here a query of each type
      // 1. Add RESET command
      queries.put(RESET, new Reset());
      //2. Add SET_SPEED command
      queries.put( SET_SPEED , new SetSpeed());
   
   }
   
// wait time in ms for an answer to a query
   public static final int EPUCK_DEFAULT_WAIT_TIME = 250;
   
   public String getName ( ) {
      return EPUCK_PROTOCOL;
   }
   
   /* (non-Javadoc)
    * @see gsn.utils.protocols.AbstractHCIProtocol#getQueries()
    */
   public Vector < AbstractHCIQuery > getQueries ( ) {
      // TODO Auto-generated method stub
      return new Vector<AbstractHCIQuery>(queries.values());
   }
   
   public AbstractHCIQuery getQuery(String queryName) {
      return queries.get(queryName);
   }
 

   public byte[] buildRawQuery(String queryName, Vector<Object> params) {
      HCIQuery query = queries.get(queryName);
      if (query == null)
         return null;
      else {
         return query.buildRawQuery( params );
      }
   }
 
   
}
