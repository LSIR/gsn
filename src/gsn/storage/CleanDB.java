package gsn.storage;

import java.util.ArrayList;

import org.apache.log4j.PropertyConfigurator;

import gsn.Main;
import gsn.beans.ContainerConfig;
/**
 * Removes the temporary tables, tables starting with underscore.
 */
public class CleanDB {
  
  public static void main(String[] args) throws Exception {
    PropertyConfigurator.configure ( Main.DEFAULT_GSN_LOG4J_PROPERTIES );
    ContainerConfig cc =Main.getContainerConfig();
    Class.forName(cc.getJdbcDriver());
    StorageManager.getInstance ( ).initialize ( cc.getJdbcDriver ( ) , cc.getJdbcUsername ( ) , cc.getJdbcPassword ( ) , cc.getJdbcURL ( ) );
    ArrayList<String> tables = StorageManager.getInstance().getInternalTables();
    for (String t : tables) 
      StorageManager.getInstance().executeDropTable(t);
    tables = StorageManager.getInstance().getInternalTables();
    for (String t : tables) 
      StorageManager.getInstance().executeDropView(new StringBuilder(t));
      
  }
}
