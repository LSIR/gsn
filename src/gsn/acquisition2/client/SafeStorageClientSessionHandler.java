package gsn.acquisition2.client;

import gsn.acquisition2.messages.AbstractMessage;
import gsn.acquisition2.messages.AcknowledgmentMsg;
import gsn.acquisition2.messages.DataMsg;
import gsn.acquisition2.messages.HelloMsg;
import gsn.beans.AddressBean;
import org.apache.log4j.Logger;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;

public class SafeStorageClientSessionHandler extends IoHandlerAdapter {
  
  private static transient Logger                                logger                              = Logger.getLogger ( SafeStorageClientSessionHandler.class );
  
  AbstractMessage helloMsg = null;
  
  public SafeStorageClientSessionHandler(AddressBean wrapprDetails) {
    helloMsg = new HelloMsg(wrapprDetails,"requestee-1");
    
  }
  public SafeStorageClientSessionHandler(AddressBean wrapprDetails,boolean continueOnErr) {
    helloMsg = new HelloMsg(wrapprDetails,"requestee-1",continueOnErr);
  }
  public void exceptionCaught(IoSession session, Throwable cause) throws Exception {

  }
  public void messageReceived(IoSession session, Object message) throws Exception {
    logger.debug("Received data from the server");
    DataMsg dataMsg = (DataMsg) message;
    
    // todo : PROCESS IT.
    session.write(new AcknowledgmentMsg(AcknowledgmentMsg.SUCCESS,dataMsg.getSequenceNumber()));
    logger.debug("Sending Success ACK");
  }
  public void messageSent(IoSession session, Object message) throws Exception {

  }
  public void sessionClosed(IoSession session) throws Exception {

  }
  public void sessionOpened(IoSession session) throws Exception {
    session.write(helloMsg);
  }
  
}
