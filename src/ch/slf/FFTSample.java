package ch.slf;

import org.apache.log4j.Logger;

import gsn.VSensorLoader;
import gsn.beans.AddressBean;

public class FFTSample extends WindowAwareVS {
  
  private static transient Logger logger  = Logger.getLogger ( FFTSample.class );
  
  double fft_size;
  double overlap ;
  
  public boolean initialize(AddressBean params) {
    try {
      fft_size= params.getPredicateValueAsIntWithException("window-size");
      overlap = (params.getPredicateValueAsIntWithException("step-size") / fft_size) * 100.0;
    }catch (Exception e) {
      return false;
    }
    return true;
  }
  
  
  /**
   * Output : One timestamp, set of double values for every input set, df .
   */
  public FFTOutputPacket process(double [] values , long[] timestampsInMSec) {
    double sampling_rate = 1/((timestampsInMSec[timestampsInMSec.length-1] - timestampsInMSec[0])/timestampsInMSec.length)*1000;
    // External call...
    double[] results = null ;
    
    FFTOutputPacket out = new FFTOutputPacket(timestampsInMSec[timestampsInMSec.length/2]);
    out.setdf(sampling_rate/values.length);
    out.addValues(results);
    return out;
  }
}
