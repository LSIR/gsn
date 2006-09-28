package gsn.wrappers.wsn.simulator;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */

public interface WirelessNodeInterface {
    public WirelessNode getParent();

    public void send(DataPacket e);
}
