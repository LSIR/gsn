package gsn;

import gsn.acquisition2.server.SafeStorageController;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class SafeStorageStop {

	public static void main (String[] args) {
		stopSafeStorageServers();
	}
	
	public static void stopSafeStorageServers () {
	    try {
	      Socket socket = new Socket(InetAddress.getByName("localhost"), SafeStorageController.SAFE_STORAGE_CONTROL_PORT);
	      PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
	      writer.println(SafeStorageController.SAFE_STORAGE_SHUTDOWN);
	      writer.flush();
	      System.out.println("[Done]");
	    }catch (Exception e) {
	      System.out.println("[Failed: "+e.getMessage()+ "]");
	    }
	}
}
