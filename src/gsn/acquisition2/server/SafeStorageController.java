package gsn.acquisition2.server;

import gsn.networking.ActionPort;
import gsn.networking.NetworkAction;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import org.apache.log4j.Logger;

public class SafeStorageController {

	public static final int SAFE_STORAGE_CONTROL_PORT = 25001;

	public static final String SAFE_STORAGE_SHUTDOWN = "SS SHUTDOWN";

	private SafeStorageServer safeStorageServer;

	public static transient Logger logger = Logger.getLogger(SafeStorageController.class);

	public SafeStorageController(final SafeStorageServer safeStorageServer) {
		super();
		this.safeStorageServer = safeStorageServer;
		ActionPort.listen(SAFE_STORAGE_CONTROL_PORT, new NetworkAction(){
			@Override
			public boolean actionPerformed(Socket socket) {
				try {
					BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					String incomingMsg = reader.readLine();
					
					if (incomingMsg != null && incomingMsg.equalsIgnoreCase(SAFE_STORAGE_SHUTDOWN)) {
						safeStorageServer.shutdown();
						return false;
					}
					else return true;
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
					return false;
				}
			}});
	}
}
