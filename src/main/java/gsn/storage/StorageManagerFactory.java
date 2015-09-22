/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
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
* File: src/gsn/storage/StorageManagerFactory.java
*
* @author Timotee Maret
* @author Sofiane Sarni
*
*/

package gsn.storage;

import gsn.storage.db.*;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class StorageManagerFactory {

    private static final transient Logger logger = LoggerFactory.getLogger(StorageManagerFactory.class);

    /**
     * @param driver
     * @param username
     * @param password
     * @param databaseURL
     * @param maxDBConnections
     * @return A new instance of {@link gsn.storage.StorageManager} that is described by its parameters, or null
     *         if the driver can't be found.
     */
    public static StorageManager getInstance(String driver, String username, String password, String databaseURL, int maxDBConnections) {
        //
        StorageManager storageManager = null;
        // Select the correct implementation
        if ("net.sourceforge.jtds.jdbc.Driver".equalsIgnoreCase(driver)) {
			storageManager = new SQLServerStorageManager();
        }
		else if ("com.mysql.jdbc.Driver".equalsIgnoreCase(driver)) {
            storageManager = new MySQLStorageManager();
        }
        else if ("oracle.jdbc.driver.OracleDriver".equalsIgnoreCase(driver)) {
            storageManager = new OracleStorageManager();
        }
        else if ("org.h2.Driver".equalsIgnoreCase(driver)) {
            storageManager = new H2StorageManager();
        }         
        else if ("org.postgresql.Driver".equalsIgnoreCase(driver)) {
            storageManager = new PostgresStorageManager();
        }
		else {
			logger.error(new StringBuilder().append("The GSN doesn't support the database driver : ").append(driver).toString());
			logger.error("Please check the storage elements in the configuration files.");
		}
        // Initialise the storage manager
        if (storageManager != null) {
            storageManager.init(driver, username, password, databaseURL, maxDBConnections);    
        }
        //
        return storageManager;
    }
    
}
