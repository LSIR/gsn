package gsn.storage;

import gsn.Main;
import gsn.beans.ContainerConfig;

import java.util.ArrayList;

import org.apache.log4j.PropertyConfigurator;
/**
 * Removes the temporary tables, tables starting with underscore.
 */
public class CleanDB {
  
  public static void main(String[] args) throws Exception {
    PropertyConfigurator.configure ( Main.DEFAULT_GSN_LOG4J_PROPERTIES );
    ContainerConfig cc =Main.getContainerConfig();
    Class.forName(cc.getJdbcDriver());
    StorageManager sm = StorageManagerFactory.getInstance(cc.getJdbcDriver ( ) , cc.getJdbcUsername ( ) , cc.getJdbcPassword ( ) , cc.getJdbcURL ( ), Main.DEFAULT_MAX_DB_CONNECTIONS);
    ArrayList<String> tables = sm.getInternalTables();
    for (String t : tables) 
      sm.executeDropTable(t);
    tables = sm.getInternalTables();
    for (String t : tables) 
      sm.executeDropView(new StringBuilder(t));
      
  }
}
