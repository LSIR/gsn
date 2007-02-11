package gsn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.*;

import org.apache.log4j.Logger;

/**
 * @author Jérôme Rousselot (jerome@jaimepaslaneige.com)
 *
 */
public class GSNController extends Thread {

	private ServerSocket mySocket;
	public static final int GSN_CONTROL_PORT = 22222;
	private static final int GSN_CONTROL_READ_TIMEOUT = 20000;
	public static final String GSN_CONTROL_SHUTDOWN = "GSN STOP";
	private boolean running = true;
	private Socket incoming;
	public static transient Logger logger= Logger.getLogger ( GSNController.class );
	private VSensorLoader vsLoader;
	
	public GSNController(VSensorLoader vsLoader) throws UnknownHostException, IOException {
		super();
		this.vsLoader = vsLoader;
		mySocket = new ServerSocket(GSN_CONTROL_PORT, 0, InetAddress.getLocalHost());
		this.start();
	}
	
	public void run() {
		logger.info("Started GSN Controller on port " + GSN_CONTROL_PORT); 
		while(running) {
			try {
			incoming = mySocket.accept();
			if(logger.isDebugEnabled())
				logger.debug("Opened connection on control socket.");
			incoming.setSoTimeout(GSN_CONTROL_READ_TIMEOUT);
			BufferedReader reader = new BufferedReader(new InputStreamReader(incoming.getInputStream()));
			PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(incoming.getOutputStream())), true);
			String message = reader.readLine().trim();
			if(logger.isDebugEnabled())
				logger.debug("Received control message: " + message);
			if(message.equalsIgnoreCase(GSN_CONTROL_SHUTDOWN)) { // We stop GSN here
				logger.info("Shutting down GSN...");
				running = false;
				vsLoader.stopLoading();
				logger.info("All virtual sensors have been stopped, shutting down virtual machine.");
				System.exit(0);
			}
			} catch(SocketTimeoutException e) {
				if(logger.isDebugEnabled())
					logger.debug("Connection timed out. Message was: " + e.getMessage());
			} catch(IOException e) {
				logger.warn("Error while accepting control connection: " + e.getMessage());
			}
		}
	}
	
}
