package gsn.wrappers.backlog.sf;

import gsn.wrappers.backlog.BackLogMessage;
import gsn.wrappers.backlog.BackLogMessageListener;
import gsn.wrappers.backlog.BackLogMessageMultiplexer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Iterator;

import org.apache.log4j.Logger;

import net.tinyos1x.packet.Platform;
import net.tinyos1x.packet.SFProtocol;
import net.tinyos1x.util.Messenger;



/**
 * This class implements the serial forwarder (v1) client functionality.
 * <p>
 * It listens for incoming messages and forwards them to the
 * {@link gsn.wrappers.BackLogWrapper BackLogWrapper} which sends them to
 * the deployment.
 * Furthermore, it registers itself to the
 * {@link gsn.wrappers.backlog.DeploymentClient DeploymentClient} as listener
 * and forwards all incoming TOS packets from the deployment to the remote
 * sf client.
 *
 * @author	Tonio Gsell
 */
public class SFv1Client extends SFProtocol implements Runnable, BackLogMessageListener, Messenger {
	
	private static final int SF_MESSAGE_PRIORITY = 20;
	
    private Thread thread;
    private Socket socket = null;
    private SFv1Listen listenServer;
    
    private static int sfClientThreadCounter = 1;
	
	private final transient Logger logger = Logger.getLogger( SFv1Client.class );

    public SFv1Client(Socket socket, SFv1Listen listenSvr, String platform) throws IOException {
    	super("", Platform.decodePlatform(platform));
    	if( Platform.decodePlatform(platform) == Platform.defaultPlatform )
    		logger.warn("Using the default TinyOS1.x platform >" + Platform.getPlatformName(Platform.decodePlatform(platform)) + "< for SF communication");
		thread = new Thread(this);
        listenServer = listenSvr;
        this.socket = socket;
        InetAddress addr = socket.getInetAddress();
        name = "client at " + addr.getHostName() + " (" + addr.getHostAddress() + ")";
        logger.info("new " + name);
        thread.setName("SFv1Client-Thread:" + sfClientThreadCounter++);
    }

    protected void openSource() throws IOException {
        is = socket.getInputStream();
        os = socket.getOutputStream();
        super.openSource();
    }
 
    protected void closeSource() throws IOException {
        socket.close();
    }

    private void init() throws IOException {
    	open(this);
    	Iterator<BackLogMessageMultiplexer> sources = listenServer.getSources().iterator();
    	while(sources.hasNext())
    		sources.next().registerListener(BackLogMessage.TOS1x_MESSAGE_TYPE, this, false);
    }

    public void shutdown() {
		try {
		    close();
		}
		catch (IOException e) { }
		sfClientThreadCounter--;
    }

    public void start() {
    	thread.start();
    }

    public final void join(long millis) throws InterruptedException {
    	thread.join(millis);
    }

    public void run() {
		try {
		    init();
		    readPackets();
		}
		catch (IOException e) { }
		finally {
	    	Iterator<BackLogMessageMultiplexer> sources = listenServer.getSources().iterator();
	    	while(sources.hasNext())
	    		sources.next().deregisterListener(BackLogMessage.TOS1x_MESSAGE_TYPE, this, false);
		    listenServer.removeSFClient(this);
		    shutdown();
		}
    }
	
    private void readPackets() throws IOException {
		for (;;) {
			// TODO: register this listener to newly started MigMessageMultiplexer
			// (till now only registering in init, thus, no new MigMessageMultiplexers
			// are considered...)
			byte[] packet = readPacket();
		    BackLogMessage msg;
			try {
				msg = new BackLogMessage(BackLogMessage.TOS1x_MESSAGE_TYPE, 0, packet);
			    
				// TODO: to which DeviceId has the message to be sent to?
			    if (!((BackLogMessageMultiplexer) listenServer.getSources().toArray()[0]).sendMessage(msg, null, SF_MESSAGE_PRIORITY))
			    	logger.error("write failed");
			    else
					logger.debug("Message from SFv1 with address >" + socket.getInetAddress().getHostName() + "< received and forwarded");
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
        }
    }

	@Override
	public boolean messageReceived(int deviceId, long timestamp, byte[] payload) {
		try {
		    if(writePacket(payload))
		    	logger.debug("Message with timestamp " + timestamp + " successfully written to sfv1 client " + socket.getLocalAddress().getHostAddress());
		    else
		    	logger.error("Message with timestamp " + timestamp + " could not be written to sfv1 client " + socket.getLocalAddress().getHostAddress());
		}
		catch (IOException e) {
		    shutdown();
		}
		return false;
	}

	public void message(String s) {
	}

	@Override
	public void remoteConnEstablished() {
	}

	@Override
	public void remoteConnLost() {
	}
}
