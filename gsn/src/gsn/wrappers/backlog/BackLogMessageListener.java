package gsn.wrappers.backlog;


/**
 * An interface for listening to messages built from
 * {@link BackLogMessage}. Each listener which likes
 * to register itself at a {@link DeploymentClient}
 * needs to implement this interface.
 *
 * @author	Tonio Gsell
 */
public interface BackLogMessageListener extends java.util.EventListener {
    /**
     * This method is called to signal message reception. It must be
     * implemented by any listener which likes to register itself at
     * a {@link DeploymentClient}.
	 *
     * @param timestamp contained in the message {@link BackLogMessage}
     * @param payload of the message
     * 
     * @return true, if the listener did acknowledge the message
     */
    public boolean messageReceived(long timestamp, byte[] payload);

    /**
     * This method is called to signal remote connection lost. It must be
     * implemented by any listener which likes to register itself at
     * a {@link DeploymentClient}.
     */
    public void remoteConnLost();

    /**
     * This method is called to signal remote connection establishment. It must be
     * implemented by any listener which likes to register itself at
     * a {@link DeploymentClient}.
     */
    public void remoteConnEstablished();
}
