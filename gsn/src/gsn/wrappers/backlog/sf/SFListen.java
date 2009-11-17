package gsn.wrappers.backlog.sf;

import gsn.wrappers.backlog.DeploymentClient;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import org.apache.log4j.Logger;


/**
 * This class listens for serial forwarder (v2) connections.
 * <p>
 * It opens a server port and listens for incoming serial forwarder
 * connections. For each connection a new {@link SFClient} will
 * be started.
 *
 * @author	Tonio Gsell
 */
public class SFListen extends Thread {
    DeploymentClient source;
    private ServerSocket serverSocket;
    private Vector<SFClient> clients  = new Vector<SFClient>();
    private static int sfListenThreadCounter = 1;
	
	private final transient Logger logger = Logger.getLogger( SFListen.class );
	
	private int serverPort = -1;
	
	public SFListen(int localPort, DeploymentClient bc) throws IOException {
		serverPort = localPort;
		source = bc;
		
		if( serverPort < 0 | serverPort > 65536 )
			throw new IOException("localPort must be a positive integer smaller than 65536");
		
		setName("SFListen-Thread:" + sfListenThreadCounter++);
	}

    public void run() {
	
	    // open up our server socket
	    try {
	    	serverSocket = new ServerSocket(serverPort);
	    }
	    catch (Exception e) {
			logger.error("Could not listen on port: " + serverPort);
			return;
	    }
	    
		try {
		    logger.info("Listening for client connections on port " + serverPort);
		    try {
				for (;;) {
				    Socket currentSocket = serverSocket.accept();
				    SFClient newServicer = new SFClient(currentSocket, this);
				    clients.add(newServicer);
				    newServicer.start();
				}
		    }
		    catch (IOException e) { }
		}
	        finally {
		    cleanup();
        }
    }

    private void cleanup() {
		shutdownAllSFClients();
		logger.debug("Closing socket");
		if (serverSocket != null) {
		    try {
		    	serverSocket.close();
		    }
		    catch (IOException e) { }
		}
    }

    private void shutdownAllSFClients() {
        logger.info("Shutting down all client connections");
        SFClient crrntServicer;
        while (clients.size() != 0) {
		    crrntServicer = (SFClient)clients.firstElement();
		    crrntServicer.shutdown();
		    try {
		    	crrntServicer.join(1000);
		    }
		    catch (InterruptedException e) {
		    	logger.error(e.getMessage(), e);
		    }
		}
    }

    public void removeSFClient(SFClient clientS) {
        clients.remove(clientS);
    }

    public void interrupt() {
    	try {
    	    if (serverSocket != null) {
    	    	serverSocket.close();
    	    }
    	}
    	catch (IOException e) {
    	    logger.error("shutdown error " + e);
    	}
    	sfListenThreadCounter--;
    }
    
    public int getLocalPort() {
    	return serverPort;
    }
}
