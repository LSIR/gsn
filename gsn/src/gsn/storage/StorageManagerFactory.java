package gsn.storage;

import gsn.storage.db.H2StorageManager;
import gsn.storage.db.MySQLStorageManager;
import gsn.storage.db.OracleStorageManager;
import gsn.storage.db.SQLServerStorageManager;
import org.apache.log4j.Logger;

public class StorageManagerFactory {

    private static final transient Logger logger = Logger.getLogger(StorageManagerFactory.class);

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
        logger.info("Creating a " + driver + " StorageManager");
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
		else {
			logger.error(new StringBuilder().append("The GSN doesn't support the database driver : ").append(driver).toString());
			logger.error(new StringBuilder().append("Please check the storage element in the file gsn.xml configuration file."));
		}
        // Initialise the storage manager
        if (storageManager != null) {
            storageManager.init(driver, username, password, databaseURL, maxDBConnections);    
        }
        //
        return storageManager;
    }
    
}
