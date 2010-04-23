package gsn.wrappers.backlog.sf;

import gsn.wrappers.backlog.BackLogMessage;
import gsn.wrappers.backlog.BackLogMessageListener;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import org.apache.log4j.Logger;

import net.tinyos.packet.SFProtocol;
import net.tinyos.util.Messenger;


/**
 * This class implements the serial forwarder (v2) client functionality.
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
public class SFClient extends SFProtocol implements Runnable, BackLogMessageListener, Messenger {
    private Thread thread;
    private Socket socket = null;
    private SFListen listenServer;
    
    private static int sfClientThreadCounter = 1;
	
	private final transient Logger logger = Logger.getLogger( SFClient.class );

    public SFClient(Socket socket, SFListen listenSvr) throws IOException {
    	super("");
		thread = new Thread(this);
        listenServer = listenSvr;
        this.socket = socket;
        InetAddress addr = socket.getInetAddress();
        name = "client at " + addr.getHostName() + " (" + addr.getHostAddress() + ")";
        logger.info("new " + name);
        thread.setName("SFClient-Thread:" + sfClientThreadCounter++);
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
		listenServer.source.registerListener(BackLogMessage.TOS_MESSAGE_TYPE, this, false);
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
		    listenServer.source.deregisterListener(BackLogMessage.TOS_MESSAGE_TYPE, this, false);
		    listenServer.removeSFClient(this);
		    shutdown();
		}
    }
	
    private void readPackets() throws IOException {
		for (;;) {
			byte[] packet = readPacket();
		    BackLogMessage msg;
			try {
				msg = new BackLogMessage(BackLogMessage.TOS_MESSAGE_TYPE, 0, packet);

				// TODO: to which DeviceId has the message to be sent to?
			    if (!listenServer.source.sendMessage(msg, null))
			    	logger.error("write failed");
			    else
					logger.debug("Message from SF with address >" + socket.getInetAddress().getHostName() + "< received and forwarded");
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
        }
    }

	@Override
	public boolean messageReceived(int deviceId, long timestamp, byte[] payload) {
		try {
		    if(writeSourcePacket(payload))
		    	logger.debug("Message with timestamp " + timestamp + " successfully written to sf client " + socket.getLocalAddress().getHostAddress());
		    else
		    	logger.error("Message with timestamp " + timestamp + " could not be written to sf client " + socket.getLocalAddress().getHostAddress());
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
