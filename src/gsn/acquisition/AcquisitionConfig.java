package gsn.acquisition;

import org.apache.log4j.Logger;

public class AcquisitionConfig {
  
  public static transient Logger logger= Logger.getLogger ( AcquisitionConfig.class );
  
  private int port;

  public int getPort() {
    return port;
  }
  
}
