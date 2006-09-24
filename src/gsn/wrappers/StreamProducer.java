package gsn.wrappers ;

import gsn.beans.AddressBean ;
import gsn.beans.DataField ;

import java.util.Collection ;
import java.util.HashMap ;
import java.util.TreeMap ;

import javax.naming.OperationNotSupportedException ;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public interface StreamProducer {

   public static final String TIME_FIELD = "TIMED" ;

   /**
    * The addressing is provided in the ("ADDRESS",Collection<KeyValue>). If
    * the DataSource can't initialize itself because of either internal error or
    * inaccessibility of the host specified in the address the method returns
    * false. The dbAliasName of the DataSource is also specified with the
    * "DBALIAS" in the context. The "STORAGEMAN" points to the StorageManager
    * which should be used for querying.
    * 
    * @return True if the initialization do successfully otherwise false;
    */
   public abstract boolean initialize ( TreeMap initialContext ) ;

   public abstract AddressBean getActiveAddressBean ( ) ;

   /**
    * @return The number of active listeners which are interested in hearing
    *         from this data source.
    */
   public abstract int getListenersSize ( ) ;

   public abstract String addListener ( DataListener dataListener ) ;

   public abstract void removeListener ( DataListener dataListener ) ;

   public abstract String getDBAlias ( ) ;

   public abstract Collection < DataField > getProducedStreamStructure ( ) ;

   public abstract void finalize ( HashMap context ) ;

   /**
    * This method is called whenever the wrapper wants to send a data item back
    * to the source where the data is coming from. For example, If the data is
    * coming from a wireless sensor network (WSN), This method sends a data item
    * to the sink node of the virtual sensor. So this method is the
    * communication between the System and actual source of data. The data sent
    * back to the WSN could be a command message or a configuration message.
    * 
    * @param dataItem :
    *           The data which is going to be send to the source of the data for
    *           this wrapper.
    * 
    * @return True if the send operation is successful.
    * 
    * @throws OperationNotSupportedException
    *            If the wrapper doesn't support sending the data back to the
    *            source. Note that by default this method throws this exception
    *            unless the wrapper overrides it.
    */
   public boolean sendToWrapper ( Object dataItem ) throws OperationNotSupportedException ;

}
