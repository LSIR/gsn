package gsn.acquisition;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.zip.GZIPInputStream;

public class AcquisitionWorker implements Runnable {

  public AcquisitionWorker(Socket accept, AcquisitionDirectory directory) throws IOException {
    new ObjectInputStream(new GZIPInputStream(new BufferedInputStream(accept.getInputStream(),32*1024)));
  }

  public void run() {
    
  }
  
}
