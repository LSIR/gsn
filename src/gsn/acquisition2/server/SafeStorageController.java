package gsn.acquisition2.server;

import gsn.GSNController;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

public class SafeStorageController extends Thread {

	private SafeStorageServer safeStorageServer;
	
	private Socket socket;
	
	public static transient Logger logger = Logger.getLogger(SafeStorageController.class);
	
	public SafeStorageController(SafeStorageServer safeStorageServer) {
		super();
		this.safeStorageServer = safeStorageServer;
	}

	@Override
	public void run() {
		try {
			socket = new Socket(InetAddress.getByName("localhost"), gsn.GSNController.GSN_CONTROL_PORT);
			PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			// Register
			writer.println(GSNController.GSN_CONTROL_REGISTER_SHUTDOWN);
		    writer.flush();
		    boolean shutdown_ss = false;
		    String message = null;
		    while (! shutdown_ss) {
		    	message = reader.readLine();
		    	if (message == null || message.compareToIgnoreCase(GSNController.GSN_CONTROL_SHUTDOWN) == 0) { 
		    		// Shutdown
		    		shutdown_ss = true;
		    		safeStorageServer.shutdown();
		    	}
		    }
		} catch (UnknownHostException e) {
			logger.warn(e.getMessage());
		} catch (IOException e) {
			logger.warn(e.getMessage());
		}
	}
}
