package gsn.vsensor ;

import gsn.beans.StreamElement ;

import java.util.HashMap ;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */

public interface VirtualSensor {

   /**
    * Called once while initializing an instance of the virtual sensor. The map
    * has atleast the following keys :
    * <ul>
    * <li><code>VirtualSensorPool.CONTAINER</code> which references to an
    * instance of <code>Container</code> object. </li>
    * <li><code>VirtualSensorPool.VSENSORCONFIG</code> which references to
    * the actual configuration of this virtual sensor, an instance of
    * <code>VSensorConfig</code></li>
    * </ul>
    * 
    * @param map
    * @return True if the initialization is done successfully.
    */
   public abstract boolean initialize ( HashMap map ) ;

   /**
    * returns the virtual sensor's name which is specified in the configuration
    * file. This value can be obtained from the initialization method which is
    * going to be called first.
    */

   public abstract String getName ( ) ;

   /*
    * This method is going to be called by the container when one of the input
    * streams has a data to be delivered to this virtual sensor. After receiving
    * the data, the virutal sensor can do the processing on it and this
    * processing could possibly result in producing a new stream element in this
    * virtual sensor in which case the virutal sensor will notify the container
    * by simply adding itself to the list of the virtual sensors which have
    * produced data. (calling <code>container.publishData(this)</code>. For
    * more information please check the <code>AbstractVirtalSensor</code>
    * 
    * @param inputStreamName is the name of the input stream as specified in the
    * configuration file of the virtual sensor. @param inputDataFromInputStream
    * is actually the real data which is produced by the input stream and should
    * be delivered to the virtual sensor for possible processing.
    */
   public abstract void dataAvailable ( String inputStreamName , StreamElement streamElement ) ;

   public abstract StreamElement getData ( ) ;

   /**
    * Called when the container want to stop the pool and remove it's resources.
    * The container will call this method once on each install of the virtual
    * sensor in the pool. The progrmmer should release all the resouce used by
    * this virtual sensor instance in this method specially those resouces
    * aquired during the <code>initialize</code> call. <p/> Called once while
    * finalizing an instance of the virtual sensor
    * 
    * @param map
    *           The finalizing context which can be used for finilization
    *           process of a virtual sensor instance.
    */
   public abstract void finalize ( HashMap map ) ;

   public void dataFromWeb ( String data ) ;
}
