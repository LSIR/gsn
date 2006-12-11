package gsn.wrappers;

import gsn.Main;
import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.storage.SQLUtils;
import gsn.storage.StorageManager;
import gsn.utils.CaseInsensitiveComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

import javax.naming.OperationNotSupportedException;
import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 * Date: Aug 4, 2005 <br>
 */
public abstract class AbstractWrapper extends Thread  {
   
   /**
    * Used by the data source when it wants to insert the data into it's main
    * database. The AbstractDataSource should keep track of this data source so
    * that it can release it when <code>finialize</code> called.
    */
   private final static transient Logger      logger         = Logger.getLogger( AbstractWrapper.class );
   
   private static final transient boolean     isDebugEnabled = logger.isDebugEnabled( );
   
   private static final transient boolean     isInfoEnabled  = logger.isInfoEnabled( );
   
   protected final ArrayList < DataListener > listeners      = new ArrayList < DataListener >( );
   
   private AddressBean                        activeAddressBean;
   
   private boolean                            isActive       = true;
   
   private TableSizeEnforce                   tableSizeEnforce;
   
   /**
    * @return the tableSizeEnforce
    */
   public TableSizeEnforce getTableSizeEnforce ( ) {
      return tableSizeEnforce;
   }
   
   /**
    * @param tableSizeEnforce the tableSizeEnforce to set
    */
   public void setTableSizeEnforce ( TableSizeEnforce tableSizeEnforce ) {
      this.tableSizeEnforce = tableSizeEnforce;
   }
   
   /**
    * Returns the view name created for this listener.
    * Note that, GSN creates one view per listener.
    */
   public CharSequence addListener ( DataListener dataListener ) {
      TreeMap < CharSequence , CharSequence > mapping = new TreeMap< CharSequence , CharSequence >( new CaseInsensitiveComparator());
      mapping.put( "wrapper" , aliasCodeS);
      CharSequence resultQuery = SQLUtils.newRewrite( dataListener.getMergedQuery( ) , mapping );
      CharSequence viewName = dataListener.getViewNameInString( );
      if ( isDebugEnabled == true ) logger.debug( new StringBuilder( ).append( "The view name=" ).append( viewName ).append( " with the query=" ).append( resultQuery ).toString( ) );
      getStorageManager( ).createView( viewName , resultQuery );
      synchronized ( listeners ) {
         listeners.add( dataListener );
         tableSizeEnforce.updateInternalCaches( );
      }
      return viewName;
   }
   
   /**
    * Removes the listener with it's associated view.
    */
   public void removeListener ( DataListener dataListener ) {
      synchronized ( listeners ) {
         listeners.remove( dataListener );
         tableSizeEnforce.updateInternalCaches( );
      }
      getStorageManager( ).dropView( dataListener.getViewNameInString( ) );
   }
   
   /**
    * @return The number of active listeners which are interested in hearing
    * from this data source.
    */
   
   public int getListenersSize ( ) {
      return listeners.size( );
   }
   
   /**
    * @return the listeners
    */
   public ArrayList < DataListener > getListeners ( ) {
      return listeners;
   }
   
   protected StorageManager getStorageManager ( ) {
      return StorageManager.getInstance( );
      
   }
   
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
   
   public final AddressBean getActiveAddressBean ( ) {
      if (this.activeAddressBean==null) {
         throw new RuntimeException("There is no active address bean associated with the wrapper.");
      }
      return activeAddressBean;
   }
   
   /**
    * @param activeAddressBean the activeAddressBean to set
    */
   public void setActiveAddressBean ( AddressBean activeAddressBean ) {
      if (this.activeAddressBean!=null) {
         throw new RuntimeException("There is already an active address bean associated with the wrapper.");
      }
      this.activeAddressBean = activeAddressBean;
   }
   
   private final transient int aliasCode = Main.tableNameGenerator( );
   private final CharSequence aliasCodeS = Main.tableNameGeneratorInString( aliasCode );
   
   public int getDBAlias ( ) {
      return aliasCode;
   }
   public CharSequence getDBAliasInStr() {
      return aliasCodeS;
   }
   
   public abstract  DataField [] getOutputFormat ( );
   
   public boolean isActive ( ) {
      return isActive;
   }
   
   
   protected void postStreamElement ( StreamElement streamElement ) {
      boolean result = getStorageManager( ).insertData( aliasCodeS , streamElement );
      if ( result == false ) {
         logger.warn( "Inserting the following data item failed : " + streamElement );
      } else
         synchronized ( listeners ) {
            if ( listeners.size( ) == 0 ) logger.warn( "A wrapper without listener shouldn't exist. !!!" );
            for ( Iterator < DataListener > iterator = listeners.iterator( ) ; iterator.hasNext( ) ; ) {
               DataListener dataListener = iterator.next( );
               boolean results = getStorageManager( ).isThereAnyResult( dataListener.getViewQuery( ) );
               if ( results ) {
                  if ( isDebugEnabled == true ) logger.debug( "Output stream produced/received from a wrapper" );
                  dataListener.dataAvailable( );
               }
            }
         }
   }
   /**
    * This method is called whenever the wrapper wants to send a data item back
    * to the source where the data is coming from. For example, If the data is
    * coming from a wireless sensor network (WSN), This method sends a data item
    * to the sink node of the virtual sensor. So this method is the
    * communication between the System and actual source of data. The data sent
    * back to the WSN could be a command message or a configuration message.
    * 
    * @param dataItem : The data which is going to be send to the source of the
    * data for this wrapper.
    * @return True if the send operation is successful.
    * @throws OperationNotSupportedException If the wrapper doesn't support
    * sending the data back to the source. Note that by default this method
    * throws this exception unless the wrapper overrides it.
    */
  
   public boolean sendToWrapper ( Object dataItem ) throws OperationNotSupportedException {
      throw new OperationNotSupportedException( "This wrapper doesn't support sending data back to the source." );
   }

   public void releaseResources ( ) {
      isActive = false;
      if ( isInfoEnabled ) logger.info( "Finalized called" );
      getStorageManager( ).dropTable( aliasCodeS );
   }
 public static final String TIME_FIELD = "TIMED";
   
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
  
   public abstract boolean initialize ( ) ;
   public abstract void finalize();
   public abstract String getWrapperName();
}
