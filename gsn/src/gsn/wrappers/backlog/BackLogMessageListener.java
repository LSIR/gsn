package gsn.wrappers.backlog;

import java.io.Serializable;


/**
 * An interface for listening to messages built from
 * {@link BackLogMessage}. Each listener which likes
 * to register itself at a {@link AsyncCoreStationClient}
 * needs to implement this interface.
 *
 * @author	Tonio Gsell
 */
public interface BackLogMessageListener extends java.util.EventListener {
    /**
     * This method is called to signal message reception. It must be
     * implemented by any listener which likes to register itself at
     * a {@link AsyncCoreStationClient}.
	 *
     * @param deviceId the DeviceId the message has been received from
     * @param timestamp contained in the message {@link BackLogMessage}
     * @param data of the message
     * 
     * @return true, if the listener did acknowledge the message
     */
    public boolean messageReceived(int deviceId, long timestamp, Serializable[] data);

    /**
     * This method is called to signal remote connection lost. It must be
     * implemented by any listener which likes to register itself at
     * a {@link AsyncCoreStationClient}.
     */
    public void remoteConnLost();

    /**
     * This method is called to signal remote connection establishment. It must be
     * implemented by any listener which likes to register itself at
     * a {@link AsyncCoreStationClient}.
     */
    public void remoteConnEstablished();
}
