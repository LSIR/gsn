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
 * This class listens for serial forwarder (v2) connections.
 * <p>
 * It opens a server port and listens for incoming serial forwarder
 * connections. For each connection a new {@link SFClient} will
 * be started.
 *
 * @author	Tonio Gsell
 */
public class SFListen extends Thread {
    private ServerSocket serverSocket;
    private Vector<SFClient> clients  = new Vector<SFClient>();
    private static int sfListenThreadCounter = 1;
	
	private final transient Logger logger = Logger.getLogger( SFListen.class );
	private static Map<String,SFListen> sfListenMap = new HashMap<String,SFListen>();
	private static Map<String, Map<String, BackLogMessageMultiplexer>> deplToSourcesMap = new HashMap<String, Map<String, BackLogMessageMultiplexer>>();
	
	private int serverPort = -1;
	private String deploymentName = null;
	
	public SFListen(int localPort, String deploymentName) throws IOException {
		serverPort = localPort;
		this.deploymentName = deploymentName;
		
		if( serverPort < 0 | serverPort > 65536 )
			throw new IOException("localPort must be a positive integer smaller than 65536");
		
		setName("SFListen-Thread:" + sfListenThreadCounter++);
	}
	
	public synchronized static SFListen getInstance(int localPort, BackLogMessageMultiplexer bc, String coreStationName, String deploymentName) throws Exception {
		if(sfListenMap.containsKey(deploymentName)) {
			if(!deplToSourcesMap.get(deploymentName).containsKey(coreStationName))
				deplToSourcesMap.get(deploymentName).put(coreStationName, bc);
			return sfListenMap.get(deploymentName);
		}
		else {
			deplToSourcesMap.put(deploymentName, new HashMap<String, BackLogMessageMultiplexer>());
			deplToSourcesMap.get(deploymentName).put(coreStationName, bc);
			SFListen sfListen = new SFListen(localPort, deploymentName);
			sfListenMap.put(deploymentName, sfListen);
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

    public void removeSFClient(SFClient client) {
        clients.remove(client);
    }

    public void dispose(String coreStationName) {
    	deplToSourcesMap.get(deploymentName).remove(coreStationName);
    	if(deplToSourcesMap.get(deploymentName).isEmpty()) {
	    	try {
	    	    if (serverSocket != null) {
	    	    	serverSocket.close();
	    	    }
	    	}
	    	catch (IOException e) {
	    	    logger.error("shutdown error " + e);
	    	}
	    	sfListenMap.remove(deploymentName);
	    	deplToSourcesMap.remove(deploymentName);
	    	sfListenThreadCounter--;
    	}
    }
    
    public int getLocalPort() {
    	return serverPort;
    }
}
