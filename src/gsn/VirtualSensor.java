package gsn;

import gsn.beans.InputStream;
import gsn.beans.StreamSource;
import gsn.beans.VSensorConfig;
import gsn.storage.StorageManager;
import gsn.vsensor.AbstractVirtualSensor;
import org.apache.log4j.Logger;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class VirtualSensor {

    private static final transient Logger logger = Logger.getLogger(VirtualSensor.class);

    private static final int GARBAGE_COLLECTOR_INTERVAL = 2;

    private String processingClassName;

    private AbstractVirtualSensor virtualSensor = null;

    private VSensorConfig config = null;

    private long lastModified = -1;

    private int noOfCallsToReturnVS = 0;

    public VirtualSensor(VSensorConfig config) {
        this.config = config;
        this.lastModified = new File(config.getFileName()).lastModified();
    }

    public synchronized AbstractVirtualSensor borrowVS() throws VirtualSensorInitializationFailedException {
        if (virtualSensor == null) {
            try {
                virtualSensor = (AbstractVirtualSensor) Class.forName(config.getProcessingClass()).newInstance();
                virtualSensor.setVirtualSensorConfiguration(config);
            } catch (Exception e) {
                throw new VirtualSensorInitializationFailedException(e.getMessage(), e);
            }
            if (virtualSensor.initialize() == false) {
                virtualSensor = null;
                throw new VirtualSensorInitializationFailedException();
            }
            if (logger.isDebugEnabled())
                logger.debug(new StringBuilder().append("Created a new instance for VS ").append(config.getName()));
        }
        return virtualSensor;
    }

    /**
     * The method ignores the call if the input is null
     *
     * @param o
     */
    public synchronized void returnVS(AbstractVirtualSensor o) {
        if (o == null) return;
        if (++noOfCallsToReturnVS % GARBAGE_COLLECTOR_INTERVAL == 0)
            DoUselessDataRemoval();
    }

    public synchronized void closePool() {
        if (virtualSensor != null) {
            virtualSensor.dispose();
            if (logger.isDebugEnabled())
                logger.debug(new StringBuilder().append("VS ").append(config.getName()).append(" is now released."));
        } else if (logger.isDebugEnabled())
            logger.debug(new StringBuilder().append("VS ").append(config.getName()).append(" was already released."));
    }

    public void start() throws VirtualSensorInitializationFailedException {
        for (InputStream inputStream : config.getInputStreams()) {
            for (StreamSource streamSource : inputStream.getSources()) {
                streamSource.getWrapper().start();
            }
        }
        borrowVS();
    }

    /**
     * @return the config
     */
    public VSensorConfig getConfig() {
        return config;
    }

    /**
     * @return the lastModified
     */
    public long getLastModified() {
        return lastModified;
    }

    public void dispose() {
    }

    public void DoUselessDataRemoval() {
        if (config.getParsedStorageSize() == VSensorConfig.STORAGE_SIZE_NOT_SET) return;
        StringBuilder query = uselessDataRemovalQuery();
        int effected = 0;
        try {
            effected = StorageManager.getInstance().executeUpdate(query);
        } catch (SQLException e) {
            logger.error("Error in executing: " + query);
            logger.error(e.getMessage(), e);
        }
        if (logger.isDebugEnabled())
            logger.debug(new StringBuilder().append(effected).append(" old rows dropped from ").append(config.getName()).toString());
    }

    /**
     * @return
     */
    public StringBuilder uselessDataRemovalQuery() {
        String virtualSensorName = config.getName();
        StringBuilder query = null;
        if (config.isStorageCountBased()) {
            if (StorageManager.isH2()) {
                query = new StringBuilder().append("delete from ").append(virtualSensorName).append(" where ").append(virtualSensorName).append(".timed not in ( select ").append(
                        virtualSensorName).append(".timed from ").append(virtualSensorName).append(" order by ").append(virtualSensorName).append(".timed DESC  LIMIT  ").append(
                        config.getParsedStorageSize()).append(" offset 0 )");
            } else if (StorageManager.isMysqlDB()) {
                query = new StringBuilder().append("delete from ").append(virtualSensorName).append(" where ").append(virtualSensorName).append(".timed <= ( SELECT * FROM ( SELECT timed FROM ")
                        .append(virtualSensorName).append(" group by ").append(virtualSensorName).append(".timed ORDER BY ").append(virtualSensorName).append(".timed DESC LIMIT 1 offset ")
                        .append(config.getParsedStorageSize()).append("  ) AS TMP)");
            } else if (StorageManager.isSqlServer()) {
                query = new StringBuilder().append("delete from ").append(virtualSensorName).append(" where ").append(virtualSensorName).append(".timed < (select min(timed) from (select top ").append(config.getParsedStorageSize()).append(
                        " * ").append(" from ").append(virtualSensorName).append(" order by ").append(virtualSensorName).append(".timed DESC ) as x ) ");
            } else if (StorageManager.isOracle()) {
                query = new StringBuilder().append("delete from ").append(virtualSensorName).append(" where timed <= ( SELECT * FROM ( SELECT timed FROM ")
                        .append(virtualSensorName).append(" group by timed ORDER BY timed DESC) where rownum = ")
                        .append(config.getParsedStorageSize() + 1).append(" )");
            }
        } else {
            long timedToRemove = -1;
            Connection conn = null;
            try {
                ResultSet rs = StorageManager.getInstance().executeQueryWithResultSet(new StringBuilder("SELECT MAX(timed) FROM ").append(virtualSensorName), conn = StorageManager.getInstance().getConnection());
                if (rs.next())
                    timedToRemove = rs.getLong(1);
            } catch (SQLException e) {
                logger.error(e.getMessage(), e);
            } finally {
                StorageManager.close(conn);

            }
            query = new StringBuilder().append("delete from ").append(virtualSensorName).append(" where ").append(virtualSensorName).append(".timed < ").append(timedToRemove);
            query.append(" - ").append(config.getParsedStorageSize());
        }
        if (logger.isDebugEnabled())
            this.logger.debug(new StringBuilder().append("Enforcing the limit size on the VS table by : ").append(query).toString());
        return query;
    }
}

