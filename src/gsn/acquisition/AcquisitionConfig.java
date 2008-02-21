package gsn.acquisition;

import gsn.Main;

import org.apache.log4j.Logger;

public class AcquisitionConfig {
  
  public static transient Logger logger= Logger.getLogger ( AcquisitionConfig.class );
  
  private int port;

  public int getPort() {
    return port;
  }
  
}
