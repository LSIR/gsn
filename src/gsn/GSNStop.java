package gsn;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class GSNStop {
  public static void main(String[] args)  {
    System.out.print("Stopping ... ");
    try {
    Socket socket = new Socket(InetAddress.getLocalHost(), gsn.GSNController.GSN_CONTROL_PORT);
    PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
    writer.println(gsn.GSNController.GSN_CONTROL_SHUTDOWN);
    writer.flush();
    System.out.println("[Done]");
    }catch (Exception e) {
      System.out.println("[Failed: "+e.getMessage()+ "]");
    }
    
    
  }
}
