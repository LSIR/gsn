/**
 * 
 */
package gsn.utils.protocols;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.OperationNotSupportedException;

import gsn.vsensor.EPuckVS;
import gsn.wrappers.StreamProducer;

import org.apache.log4j.Logger;


/**
 * @author alisalehi
 *
 */
public class ProtocolManager {
   private static final transient Logger logger = Logger.getLogger( ProtocolManager.class );
   private AbstractHCIProtocol protocol;
   private ProtocolStates currentState;
   private AbstractHCIQuery lastExecutedQuery = null;
   private Vector < Object > lastParams;
   
   private Timer timer;
   private TimerTask answerTimeout = new TimerTask() {
      
      public synchronized void run ( ) {
         lastExecutedQuery = null;
         currentState = ProtocolStates.READY;
      } 
   };
   
   public enum ProtocolStates {
      READY, WAITING
   }
   
   public ProtocolManager(AbstractHCIProtocol protocol) {
      this.protocol = protocol;
      currentState = ProtocolStates.READY;
   }
   
   public synchronized ProtocolStates getCurrentState() {
      return currentState;
   }
   
   /*
    * This method tries to execute a query named queryName with parameters params
    * on the wrapper wrapper.
    * If successful, it returns true.
    */
   public synchronized boolean sendQuery(String queryName, Vector<Object> params, StreamProducer wrapper) {
      boolean successful = false;
      if(currentState == ProtocolStates.READY) {
         AbstractHCIQuery query = protocol.getQuery( queryName );
         if(logger.isDebugEnabled())
            logger.debug( "Retrieved query " + queryName + ", trying to build raw query.");
         if(query != null) {
            byte[] queryBytes = query.buildRawQuery( params );
            if(queryBytes != null) {
               try {
                  if(logger.isDebugEnabled())
                     logger.debug("Built query, it looks like: " + new String(queryBytes));
                  wrapper.sendToWrapper(queryBytes);
                  lastExecutedQuery = query;
                  lastParams = params;
                  successful = true;
                  if(logger.isDebugEnabled())
                     logger.debug("Query succesfully sent!");
                  if(query.needsAnswer( params )) {
                     if(logger.isDebugEnabled())
                        logger.debug("Now entering wait mode for answer.");
                     timer = new Timer();
                     currentState = ProtocolStates.WAITING;
                     timer.schedule( answerTimeout , new Date());
                  }
               } catch( OperationNotSupportedException e ) {
                  if(logger.isDebugEnabled())
                     logger.debug("Query could not be sent ! See error message.");
                  logger.error( e.getMessage( ) , e );
                  currentState = ProtocolStates.READY;
               }            
            }
         }
      }
      return successful;
   }
   
   /*
    * This tries to match incoming data to the pattern
    * expected by the query. If the pattern describes
    * several groups then all the different String
    * matching these groups are returned.
    */
   public synchronized String[] getAnswer(byte[] rawData) {
      String[] answer = null;
      if(currentState == ProtocolStates.WAITING) {
         Pattern pattern = lastExecutedQuery.getAnswerPattern( lastParams );
         Matcher matcher = pattern.matcher(new String(rawData));
         if(matcher.matches()) {
            answer= new String[matcher.groupCount()+1];
            for(int i=0; i < matcher.groupCount()+1; i++)
               answer[i] = matcher.group(i); // group zero = the whole pattern match
         }
      }
      return answer;
   }
}

