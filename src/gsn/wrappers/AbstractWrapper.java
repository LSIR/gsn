package gsn.wrappers;

import gsn.Main;
import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.storage.SQLUtils;
import gsn.storage.StorageManager;
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
public abstract class AbstractWrapper extends Thread implements Wrapper {
   
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
   
   private boolean                            isActive        = true;
   
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
   
   public abstract boolean initialize ( TreeMap initialContext );
   
   public String addListener ( DataListener dataListener ) {
      HashMap < String , String > mapping = new HashMap < String , String >( );
      mapping.put( "WRAPPER" , getDBAlias( ) );
      String resultQuery = SQLUtils.rewriteQuery( dataListener.getMergedQuery( ) , mapping );
      String viewName = dataListener.getViewName( );
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
      getStorageManager( ).dropView( dataListener.getViewName( ) );
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
      return activeAddressBean;
   }
   
   private String cachedDBAliasName = null;
   
   public String getDBAlias ( ) {
      if ( cachedDBAliasName == null ) cachedDBAliasName = Main.tableNameGenerator( );
      return cachedDBAliasName;
   }
   
   public abstract Collection < DataField > getOutputFormat ( );
   
   public boolean isActive ( ) {
      return isActive;
   }
   
   public void finalize ( HashMap context ) {
      isActive = false;
      if ( isInfoEnabled ) logger.info( "Finalized called" );
      getStorageManager( ).dropTable( getDBAlias( ) );
   }
   
   protected void postStreamElement ( StreamElement streamElement ) {
      boolean result = getStorageManager( ).insertData( getDBAlias( ) , streamElement );
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
   
   public boolean sendToWrapper ( Object dataItem ) throws OperationNotSupportedException {
      throw new OperationNotSupportedException( "This wrapper doesn't support sending data back to the source." );
      
   }
}
