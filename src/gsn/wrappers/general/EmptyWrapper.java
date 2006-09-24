package gsn.wrappers.general ;

import gsn.beans.DataField ;
import gsn.wrappers.AbstractStreamProducer ;

import java.sql.SQLException ;
import java.util.ArrayList ;
import java.util.Collection ;
import java.util.HashMap ;
import java.util.TreeMap ;

import org.apache.log4j.Logger ;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class EmptyWrapper extends AbstractStreamProducer {

   private final transient Logger logger = Logger.getLogger ( EmptyWrapper.class ) ;

   private int threadCounter = 0 ;

   public boolean initialize ( TreeMap context ) {
      super.initialize ( context ) ;
      setName ( "EmptyWrapper-Thread" + ( ++ threadCounter ) ) ;
      try {
         getStorageManager ( ).createTable ( getDBAlias ( ) , getProducedStreamStructure ( ) ) ;
      } catch ( SQLException e ) {
         logger.error ( e.getMessage ( ) , e ) ;
         return false ;
      }
      // TASK : TRYING TO CONNECT USING THE ADDRESS
      this.start ( ) ;
      return true ;
   }

   public void run ( ) {
      while ( isActive ( ) ) {
         if ( listeners.isEmpty ( ) )
            continue ;
         // StreamElement streamElement = new StreamElement(new String[] {
         // "DATA", "DATA_BIN" }, new Integer[] { Types.INTEGER, Types.BINARY
         // });
         // streamElement.addRowToRelation(new Serializable[] { counter,
         // testBinaryData
         // });
         // streamElement.setTimeStamp(System.currentTimeMillis());
         // // SimulationResult.addJustProducedFromDummyDataSource () ;
         // publishData(streamElement);
      }
   }

   public Collection < DataField > getProducedStreamStructure ( ) {
      ArrayList < DataField > dataField = new ArrayList < DataField > ( ) ;
      dataField.add ( new DataField ( "DATA" , "int" , "incremental int" ) ) ;
      return dataField ;
   }

   public void finalize ( HashMap context ) {
      super.finalize ( context ) ;
      threadCounter -- ;
   }

}
