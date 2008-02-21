package gsn.acquisition;

import java.util.HashMap;

public class AcquisitionDirectory {
  
  private  HashMap < String , Class < ? >> wrappers ;

  public void setWrappers(HashMap<String, Class<?>> wrappers) {
    this.wrappers = wrappers;
  }

  public HashMap<String, Class<?>> getWrappers() {
    return wrappers;
  }
  
  
  
  
}
