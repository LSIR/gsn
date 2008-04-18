package gsn.acquisition2;

import gsn.acquisition2.messages.HelloMsg;
import gsn.acquisition2.wrappers.AbstractWrapper2;
import gsn.beans.AddressBean;
import gsn.wrappers.WrappersUtil;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.apache.mina.common.IoSession;

public class SafeStorage {
  
  private static transient Logger                                logger                              = Logger.getLogger ( SafeStorage.class );
  
  private HashMap<String, Class<?>> wrappers;
  
  private SafeStorageDB storage ;
  
  public SafeStorage() throws ClassNotFoundException, SQLException {
    wrappers = WrappersUtil.loadWrappers(new HashMap<String, Class<?>>());
    storage = new SafeStorageDB();
    storage.executeSQL("create table if not exists SETUP (pk INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY, table_name varchar not null unique, requester varchar not null unique,created_at TIMESTAMP default CURRENT_TIMESTAMP() not null )");
    
  }
  
  
  public AbstractWrapper2 prepareWrapper(HelloMsg helloMsg,IoSession network) throws InstantiationException, IllegalAccessException {
    AddressBean addressBean = helloMsg.getWrapperDetails();
    if ( wrappers.get  ( addressBean.getWrapper ( ) ) == null ) {
      logger.error ( "The wrapper >" + addressBean.getWrapper ( ) + "< is not defined in the >" + WrappersUtil.DEFAULT_WRAPPER_PROPERTIES_FILE + "< file." );
      return null;
    }
    AbstractWrapper2 wrapper = ( AbstractWrapper2 ) wrappers.get ( addressBean.getWrapper ( ) ).newInstance ( );
    wrapper.setActiveAddressBean ( addressBean );
    boolean initializationResult = wrapper.initialize (  );
    if ( initializationResult == false ) {
       network.close();
       return null; 
    }
    try {
      String table_name = storage.prepareTableIfNeeded(helloMsg.getRequster());
      PreparedStatement ps = storage.createPreparedStatement("insert into "+table_name+" (stream_element) values (?)");
      wrapper.setTableName(table_name);
      wrapper.setNetwork(network);
      wrapper.setPreparedStatement(ps);
    } catch ( SQLException e ) {
      logger.error ( e.getMessage ( ) , e );
      return null;
    }
    wrapper.start ( );
    return wrapper;
  }
  
  public static void main(String[] args) {
    
  }


  public SafeStorageDB getStorage() {
    return storage;
  }
  
  
}
