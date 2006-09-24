package gsn.wrappers.wsn.simulator ;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
interface DataListener {
   public void newDataAvailable ( DataPacket dataPacket ) ;
}