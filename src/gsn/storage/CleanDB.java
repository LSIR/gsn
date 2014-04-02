/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
* 
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: src/gsn/storage/CleanDB.java
*
* @author Ali Salehi
* @author Timotee Maret
*
*/

package gsn.storage;

import gsn.Main;
import gsn.beans.ContainerConfig;

import java.util.ArrayList;

import gsn.beans.StorageConfig;
import org.apache.log4j.PropertyConfigurator;
/**
 * Removes the temporary tables, tables starting with underscore.
 */
public class CleanDB {
  
  public static void main(String[] args) throws Exception {
    PropertyConfigurator.configure ( Main.DEFAULT_GSN_LOG4J_PROPERTIES );
    ContainerConfig cc =Main.getContainerConfig();
    StorageConfig sc = cc.getSliding() != null ? cc.getSliding().getStorage() : cc.getStorage() ;
    Class.forName(sc.getJdbcDriver());
    StorageManager sm = StorageManagerFactory.getInstance(sc.getJdbcDriver ( ) , sc.getJdbcUsername ( ) , sc.getJdbcPassword ( ) , sc.getJdbcURL ( ), Main.DEFAULT_MAX_DB_CONNECTIONS);
    ArrayList<String> tables = sm.getInternalTables();
    for (String t : tables) 
      sm.executeDropTable(t);
    tables = sm.getInternalTables();
    for (String t : tables) 
      sm.executeDropView(new StringBuilder(t));
      
  }
}
