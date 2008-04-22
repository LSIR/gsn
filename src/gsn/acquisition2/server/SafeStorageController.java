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

import org.apache.log4j.Logger;

public class SafeStorageController extends Thread {

	private SafeStorageServer safeStorageServer;

	public static transient Logger logger = Logger.getLogger(SafeStorageController.class);

	public SafeStorageController(SafeStorageServer safeStorageServer) {
		super();
		this.safeStorageServer = safeStorageServer;
	}

	@Override
	public void run() {

		boolean shutdown_ss = false;
		boolean connected_ss = false;

		Socket socket = null;
		PrintWriter writer = null;
		BufferedReader reader = null;
		String message = null;

		while (! shutdown_ss) {
			if (connected_ss){
				// Register to GSNController
				writer.println(GSNController.GSN_CONTROL_REGISTER_SHUTDOWN);
				writer.flush();
				// Look for shutdown message
				while (connected_ss) {
					try {
						message = reader.readLine();
						if (message == null) {
							connected_ss = false;
						}
						else if (message.compareToIgnoreCase(GSNController.GSN_CONTROL_SHUTDOWN) == 0) {
							// Shutdown
							shutdown_ss = true;
							connected_ss = false;
							safeStorageServer.shutdown();				
						}
						else {
							logger.warn("Received unknow message");
						}
					} catch (IOException e) {
						connected_ss = false;
						e.printStackTrace();
					}
				}
			}
			else {
				// Try to connect to GSNController
				try {
					socket = new Socket(InetAddress.getByName("localhost"), gsn.GSNController.GSN_CONTROL_PORT);
					writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
					reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					connected_ss = true;
				} catch (Exception e) {
					connected_ss = false;
					try {
						Thread.sleep(6000);
					} catch (InterruptedException e1) { e1.printStackTrace(); }
				}
			}
		}
	}
}
