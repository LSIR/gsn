package ch.slf;

import gsn.beans.AddressBean;
import gsn.beans.StreamElement;
import gsn.vsensor.AbstractVirtualSensor;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public abstract class WindowAwareVS extends AbstractVirtualSensor {

  private static transient Logger                                logger                              = Logger.getLogger ( WindowAwareVS.class );
  
  public void dataAvailable(String inputStreamName, StreamElement streamElement) {
    
  }

  public void finalize() {
    
  }

  public boolean initialize() {
    return false;
  }
  
  public abstract FFTOutputPacket process(double [] values , long[] timestamps);
  public abstract boolean initialize(AddressBean params);
}
