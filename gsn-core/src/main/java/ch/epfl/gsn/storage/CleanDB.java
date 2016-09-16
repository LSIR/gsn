/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2016, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
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
* File: src/ch/epfl/gsn/storage/CleanDB.java
*
* @author Ali Salehi
* @author Timotee Maret
*
*/

package ch.epfl.gsn.storage;

import java.util.ArrayList;

import ch.epfl.gsn.Main;
import ch.epfl.gsn.beans.ContainerConfig;
import ch.epfl.gsn.beans.StorageConfig;
import ch.epfl.gsn.storage.StorageManager;
import ch.epfl.gsn.storage.StorageManagerFactory;

/**
 * Removes the temporary tables, tables starting with underscore.
 */
public class CleanDB {
  
  public static void main(String[] args) throws Exception {
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
