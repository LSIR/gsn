package gsn.wrappers.general;

import gsn.beans.DataField;
import gsn.wrappers.AbstractWrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class EmptyWrapper extends AbstractWrapper {
   
   private final transient Logger               logger        = Logger.getLogger( EmptyWrapper.class );
   
   private int                                  threadCounter = 0;
   
   private static final ArrayList < DataField > dataField     = new ArrayList < DataField >( );
   
   public boolean initialize ( TreeMap context ) {
      setName( "EmptyWrapper-Thread" + ( ++threadCounter ) );
      
      dataField.add( new DataField( "DATA" , "int" , "incremental int" ) );
      return true;
   }
   
   public void run ( ) {
      while ( isActive( ) ) {
         if ( listeners.isEmpty( ) ) continue;
      }
   }
   
   public Collection < DataField > getOutputFormat ( ) {
      return dataField;
   }
   
   public void finalize ( HashMap context ) {
      super.finalize( context );
      threadCounter--;
   }
   
}
