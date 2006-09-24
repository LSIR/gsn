package gsn.vsensor ;

import gsn.Mappings;
import gsn.beans.VSensorConfig;
import gsn.control.VSensorInstance;
import gsn.storage.PoolIsFullException;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class VirtualSensorPool {

    public static final String VSENSORCONFIG = "VSENSORCONFIG";

    public static final String CONTAINER = "CONTAINER";

    private static final transient Logger logger = Logger.getLogger(VirtualSensorPool.class);

    private static final transient boolean isDebugEnabled = logger.isDebugEnabled();

    private int maxSize;

    private int currentSize = 0;

    private String className;

    private ArrayList<VirtualSensor> allInstances = new ArrayList<VirtualSensor>();

    private ArrayList<VirtualSensor> idleInstances = new ArrayList<VirtualSensor>();

    private VSensorConfig config;

    private static final Class<VirtualSensorPool> lock = VirtualSensorPool.class;

    public VirtualSensorPool(VSensorInstance instance) {
        this.config = instance.getConfig();
        this.className = instance.getConfig().getMainClass();
        this.maxSize = instance.getConfig().getLifeCyclePoolSize();
    }

    public String getClassName() {
        return this.className;
    }

    public int getMaxSize() {
        return this.maxSize;
    }

    public VirtualSensor borrowObject() throws PoolIsFullException, VirtualSensorInitializationFailedException {
        if (currentSize == maxSize)
            throw new PoolIsFullException(config.getVirtualSensorName());
        currentSize ++;
        VirtualSensor newInstance = null;
        if (idleInstances.size() > 0)
            synchronized (lock) {
                newInstance = idleInstances.remove(0);
            }
        else
            try {
                newInstance = (VirtualSensor) Class.forName(className).newInstance();
                synchronized (lock) {
                    allInstances.add(newInstance);
                }
                HashMap hashMap = new HashMap();
                hashMap.put(CONTAINER, Mappings.getContainer());
                hashMap.put(VSENSORCONFIG, config);
                if (newInstance.initialize(hashMap) == false) {
                    throw new VirtualSensorInitializationFailedException();
                }
            } catch (InstantiationException e) {
                logger.error(e.getMessage(), e);
            } catch (IllegalAccessException e) {
                logger.error(e.getMessage(), e);
            } catch (ClassNotFoundException e) {
                logger.error(e.getMessage(), e);
            }
        if (isDebugEnabled)
            logger.debug(new StringBuilder()
                    .append("VSPool Of ").append(config.getVirtualSensorName()).append(" current busy instances : ").append(currentSize).toString());
        return newInstance;
    }

    public void returnInstance(VirtualSensor o) {
        if (o == null)
            return;
        synchronized (lock) {
            idleInstances.add(o);
            currentSize --;
        }
        if (isDebugEnabled)
            logger.debug(new StringBuilder()
                    .append("VSPool Of ").append(config.getVirtualSensorName()).append(" current busy instances : ").append(currentSize).toString());
    }

    public void closePool() {
        HashMap map = new HashMap(); // The default context for closing a
        // pool.
        closePool(map);
    }

    public void closePool(HashMap map) {
        for (VirtualSensor o : allInstances)
            o.finalize(map);
        if (isDebugEnabled)
            logger.debug(new StringBuilder().append("The VSPool Of ").append(config.getVirtualSensorName()).append(" is now closed.").toString());
        className = null;
        allInstances = null;
        idleInstances = null;
        config = null;
    }

}
