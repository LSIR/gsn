package gsn.wrappers.backlog.sf;

import gsn.wrappers.backlog.BackLogMessageMultiplexer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import org.apache.log4j.Logger;


/**
 * This class listens for serial forwarder (v1) connections.
 * <p>
 * It opens a server port and listens for incoming serial forwarder
 * connections. For each connection a new {@link SFClient} will
 * be started.
 *
 * @author	Tonio Gsell
 */
public class SFv1Listen extends Thread {
	BackLogMessageMultiplexer source;
    private ServerSocket serverSocket;
    private Vector<SFv1Client> clients  = new Vector<SFv1Client>();
    private static int sfListenThreadCounter = 1;
    private String platform = null;
	
	private final transient Logger logger = Logger.getLogger( SFv1Listen.class );
	
	private int serverPort = -1;
	
	public SFv1Listen(int localPort, BackLogMessageMultiplexer bc, String platform) throws IOException {
		this.platform = platform;
		serverPort = localPort;
		source = bc;
		
		if( serverPort < 0 | serverPort > 65536 )
			throw new IOException("localPort must be a positive integer smaller than 65536");
		
		setName("SFv1Listen-Thread:" + sfListenThreadCounter++);
	}

    public void run() {
    	logger.debug("start thread");
	
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
				    SFv1Client newServicer = new SFv1Client(currentSocket, this, platform);
				    clients.add(newServicer);
				    newServicer.start();
				}
		    }
		    catch (IOException e) { }
		}
	        finally {
		    cleanup();
        }

	    logger.debug("stop thread");
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
        SFv1Client crrntServicer;
        while (clients.size() != 0) {
		    crrntServicer = (SFv1Client)clients.firstElement();
		    crrntServicer.shutdown();
		    try {
		    	crrntServicer.join(1000);
		    }
		    catch (InterruptedException e) {
		    	logger.error(e.getMessage(), e);
		    }
		}
    }

    public void removeSFClient(SFv1Client clientS) {
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
