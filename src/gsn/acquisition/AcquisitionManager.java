package gsn.acquisition;

import gsn.wrappers.WrappersUtil;
import java.net.ServerSocket;
import java.util.HashMap;
import org.apache.log4j.Logger;

public class AcquisitionManager {
  
  AcquisitionConfig conf = new AcquisitionConfig();
  ServerSocket server;
  AcquisitionDirectory directory = new AcquisitionDirectory();
  
  public static transient Logger logger= Logger.getLogger ( AcquisitionManager.class );
  
  public void startServer() throws Exception {
    server = new ServerSocket(conf.getPort());
    while(true){
      AcquisitionWorker worker = new AcquisitionWorker(server.accept(), directory);
      Thread thread = new Thread(worker);
      thread.start();
    }
  }
  
  public AcquisitionDirectory getDirectory() {
    return directory;
  }
  
   public static void main(String[] args) throws Exception {
    AcquisitionManager acquisitionMan = new AcquisitionManager();
//    acquisitionMan.startServer();
    acquisitionMan.getDirectory().setWrappers(WrappersUtil.loadWrappers(new HashMap<String, Class<?>>()));
    AcquisitionGUI gui = new AcquisitionGUI(acquisitionMan.getDirectory());
  }
  
  
  
}
