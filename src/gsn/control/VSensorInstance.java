package gsn.control ;

import gsn.beans.VSensorConfig ;
import gsn.storage.StorageManager ;
import gsn.vsensor.VirtualSensorPool ;

import org.apache.log4j.Logger ;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */

public class VSensorInstance {

    private long lastModified;

    private VSensorConfig config;

    private String filename;

    private VirtualSensorPool pool;

    private TableSizeEnforce sizeEnforce;
    private Thread sizeEnforementThread;
    private static int TABLE_SIZE_ENFORCING_THREAD_COUNTER = 0;

    /**
     * @param fileName
     * @param modified
     * @param config
     */
    public VSensorInstance(String fileName, long modified, VSensorConfig config) {
        this.filename = fileName;
        this.lastModified = modified;
        this.config = config;
        sizeEnforce = new TableSizeEnforce(config);
        sizeEnforementThread= new Thread(sizeEnforce);
        sizeEnforementThread.setName("TableSizeEnforceing-VSensor-Thread" + TABLE_SIZE_ENFORCING_THREAD_COUNTER++);
        
    }

    /**
     * @return Returns the pool.
     */
    public VirtualSensorPool getPool() {
        return this.pool;
    }

    /**
     * @param pool The pool to set.
     */
    public void setPool(VirtualSensorPool pool) {
        this.pool = pool;
    }

    /**
     * @param config The config to set.
     */
    public void setConfig(VSensorConfig config) {
        this.config = config;
    }

    /**
     * @param filename The filename to set.
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * @param lastModified The lastModified to set.
     */
    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * @return Returns the config.
     */
    public VSensorConfig getConfig() {
        return this.config;
    }

    /**
     * @return Returns the lastModified.
     */
    public long getLastModified() {
        return this.lastModified;
    }

    /**
     * @return Returns the name.
     */
    public String getFilename() {
        return this.filename;
    }

    public void shutdown() {
        sizeEnforce.teminate();
        getPool().closePool();

    }
    public void start() {
    	sizeEnforementThread.start();
    }

    class TableSizeEnforce implements Runnable {

        private final transient Logger logger = Logger.getLogger(TableSizeEnforce.class);

        private final int RUNNING_INTERVALS = 10 * 1000;

        private VSensorConfig virtualSensorConfiguration;

        private boolean canRun = true;

        public TableSizeEnforce(VSensorConfig virtualSensor) {
            this.virtualSensorConfiguration = virtualSensor;
        }

        public void run() {
            if (virtualSensorConfiguration.getParsedStorageSize() == VSensorConfig.STORAGE_SIZE_NOT_SET)
                return;
            String virtualSensorName = virtualSensorConfiguration.getVirtualSensorName();
            StringBuilder query = null;
            if (StorageManager.isHsql()) {
                if (virtualSensorConfiguration.isStorageCountBased())
                    query = new StringBuilder()
                            .append("delete from ").append(virtualSensorName).append(" where ").append(virtualSensorName)
                            .append(".TIMED not in ( select ").append(virtualSensorName).append(".TIMED from ").append(virtualSensorName)
                            .append(" order by ").append(virtualSensorName).append(".TIMED DESC  LIMIT  ").append(virtualSensorConfiguration
                            .getParsedStorageSize()).append(" offset 0 )");
                else
                    query = new StringBuilder()
                            .append("delete from ").append(virtualSensorName).append(" where ").append(virtualSensorName)
                            .append(".TIMED < (NOW_MILLIS() -").append(virtualSensorConfiguration.getParsedStorageSize()).append(")");
            } else if (StorageManager.isMysqlDB()) {
                if (virtualSensorConfiguration.isStorageCountBased())
                    query = new StringBuilder()
                            .append("delete from ").append(virtualSensorName).append(" where ").append(virtualSensorName)
                            .append(".TIMED <= ( SELECT * FROM ( SELECT TIMED FROM ").append(virtualSensorName).append(" group by ")
                            .append(virtualSensorName).append(".TIMED ORDER BY ").append(virtualSensorName).append(".TIMED DESC LIMIT 1 offset ")
                            .append(virtualSensorConfiguration.getParsedStorageSize()).append("  ) AS TMP_").append((int) (Math.random() * 100000000))
                            .append(" )");
                else
                    query = new StringBuilder()
                            .append("delete from ").append(virtualSensorName).append(" where ").append(virtualSensorName)
                            .append(".TIMED < (UNIX_TIMESTAMP() -").append(virtualSensorConfiguration.getParsedStorageSize()).append(")");
            }
            if (query == null)
                return;
            try {
                /**
                 * Initial delay. Very important, dont remove it. The VSensorLoader
                 * when reloads a sensor (touching the configuration file), it
                 * creates the data strcture of the table in the last step thus this
                 * method should be executed after some initial delay (therefore
                 * making sure the structure is created by the loader).
                 */
                Thread.sleep(RUNNING_INTERVALS);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }

            if (logger.isDebugEnabled())
                logger.debug(new StringBuilder().append("Enforcing the limit size on the table by : ").append(query).toString());
            while (canRun) {
                int effected = StorageManager.getInstance().executeUpdate(query);
                if (logger.isInfoEnabled())
                    logger.info(new StringBuilder().append(effected).append(" old rows dropped from ").append(virtualSensorConfiguration
                            .getVirtualSensorName()).toString());
                try {
                    Thread.sleep(RUNNING_INTERVALS);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        public void teminate() {
            this.canRun = false;
        }
    }
}
