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
   // wait time in ms for an answer to a query
   public static final int EPUCK_DEFAULT_WAIT_TIME = 250;
      
   // add here an easy-to-use-and-remember name for each query class of the protocol
   public static final String SET_SPEED="SET_SPEED", RESET="RESET";
      
   public SerComProtocol() {
	   super(EPUCK_PROTOCOL);
      // Create and add here a query of each type
      // 1. Add RESET command
      addQuery(new Reset(RESET));
      //2. Add SET_SPEED command
      addQuery(new SetSpeed(SET_SPEED));
   }

}
