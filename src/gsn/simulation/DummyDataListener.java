package gsn.simulation;

import gsn.beans.InputStream;
import gsn.beans.StreamSource;
import gsn.control.VSensorLoader;
import gsn.shared.Registry;
import gsn.wrappers.DataListener;

import java.util.TreeMap;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class DummyDataListener extends DataListener {
   
   public static final String CONTAINER_PORT = "CONTAINER_PORT";
   
   private int                port;
   
   public void initialize ( TreeMap map ) {
      this.streamSource = ( StreamSource ) map.get( VSensorLoader.STREAM_SOURCE );
      this.inputStream = ( InputStream ) map.get( VSensorLoader.INPUT_STREAM );
      this.inputStream.addToRenamingMapping( streamSource.getAlias( ) , getViewName( ) );
      this.port = Integer.parseInt( ( String ) map.get( Registry.VS_PORT ) );
   }
   
   public int getContainerPort ( ) {
      return port;
   }
   
}
