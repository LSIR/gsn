package gsn.wrappers.backlog.sf;

import gsn.wrappers.backlog.BackLogMessageMultiplexer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
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
    private ServerSocket serverSocket;
    private Vector<SFv1Client> clients  = new Vector<SFv1Client>();
    private static int sfListenThreadCounter = 1;
    private String platform = null;
	
	private final transient Logger logger = Logger.getLogger( SFv1Listen.class );
	private static Map<String,SFv1Listen> sfv1ListenMap = new HashMap<String,SFv1Listen>();
	private static Map<String, Map<String, BackLogMessageMultiplexer>> deplToSourcesMap = new HashMap<String, Map<String, BackLogMessageMultiplexer>>();
	
	private int serverPort = -1;
	private String deploymentName = null;
	
	public SFv1Listen(int localPort, BackLogMessageMultiplexer bc, String platform, String deploymentName) throws IOException {
		this.platform = platform;
		serverPort = localPort;
		this.deploymentName = deploymentName;
		
		if( serverPort < 0 | serverPort > 65536 )
			throw new IOException("localPort must be a positive integer smaller than 65536");
		
		setName("SFv1Listen-Thread:" + sfListenThreadCounter++);
	}
	
	public synchronized static SFv1Listen getInstance(int localPort, BackLogMessageMultiplexer bc, String platform, String coreStationName, String deploymentName) throws Exception {
		if(sfv1ListenMap.containsKey(deploymentName)) {
			if(!deplToSourcesMap.get(deploymentName).containsKey(coreStationName))
				deplToSourcesMap.get(deploymentName).put(coreStationName, bc);
			return sfv1ListenMap.get(deploymentName);
		}
		else {
			deplToSourcesMap.put(deploymentName, new HashMap<String, BackLogMessageMultiplexer>());
			deplToSourcesMap.get(deploymentName).put(coreStationName, bc);
			SFv1Listen sfListen = new SFv1Listen(localPort, bc, platform, deploymentName);
			sfv1ListenMap.put(deploymentName, sfListen);
			return sfListen;
		}
	}

    public void run() {
    	logger.info("start thread");
	
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

	    logger.info("stop thread");
    }
    
    public Collection<BackLogMessageMultiplexer> getSources() {
    	return deplToSourcesMap.get(deploymentName).values();
    }

    private void cleanup() {
		shutdownAllSFClients();
		if (logger.isDebugEnabled())
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

    public void dispose(String deploymentName) {
    	try {
    	    if (serverSocket != null) {
    	    	serverSocket.close();
    	    }
    	}
    	catch (IOException e) {
    	    logger.error("shutdown error " + e);
    	}
    	sfv1ListenMap.remove(deploymentName);
    	sfListenThreadCounter--;
    }
    
    public int getLocalPort() {
    	return serverPort;
    }
}
