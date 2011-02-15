package gsn.wrappers.wsn.simulator;

interface DataListener {
   public void newDataAvailable ( DataPacket dataPacket );
}