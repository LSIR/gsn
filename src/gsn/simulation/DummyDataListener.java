package gsn.simulation;

import gsn.beans.InputStream;
import gsn.beans.StreamSource;
import gsn.wrappers.DataListener;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class DummyDataListener extends DataListener {
   
   /**
    * @param is
    * @param ss
    */
   public DummyDataListener ( InputStream is , StreamSource ss ) {
      super( is , ss );
   }


   public static final String CONTAINER_PORT = "CONTAINER_PORT";
   
   private int                port;
   
   
   public int getContainerPort ( ) {
      return port;
   }
   
}
