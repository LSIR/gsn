package gsn.acquisition2.server;

import gsn.acquisition2.SafeStorage;
import gsn.acquisition2.messages.AcknowledgmentMsg;
import gsn.acquisition2.messages.DataMsg;
import gsn.acquisition2.messages.HelloMsg;
import gsn.acquisition2.wrappers.AbstractWrapper2;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.log4j.Logger;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;

public class SafeStorageServerSessionHandler extends IoHandlerAdapter{
  
  
  private AbstractWrapper2 wrapper;
  private SafeStorage ss;
  
  private PreparedStatement readerPS = null;
  private PreparedStatement successAckUpdatePS;
  
  public SafeStorageServerSessionHandler(SafeStorage ss) throws ClassNotFoundException, SQLException {
    this.ss = ss;
  }
  
  private static transient Logger                                logger                              = Logger.getLogger ( SafeStorageServerSessionHandler.class );
  
  public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
    logger.error(cause.getMessage(),cause);
    session.close();
    // Update the number of clients using this wrapper.
  }
  
  public void messageReceived(IoSession session, Object message) throws Exception {
    if (message instanceof HelloMsg) {
      HelloMsg hello = (HelloMsg) message;
      logger.debug("Hello received : "+hello.getWrapperDetails().toString());
      wrapper = ss.prepareWrapper(hello, session);
      if (wrapper == null) {
        session.close();
        return;
      }
        
      readerPS = ss.getStorage().createPreparedStatement("select pk,stream_element,created_at from "+wrapper.getTableName()+" where processed = false order by pk asc limit 1");
      successAckUpdatePS = ss.getStorage().createPreparedStatement("update "+wrapper.getTableName()+" set PROCESSED  = true where pk = ? ");
    }
    if (message instanceof AcknowledgmentMsg) {
      AcknowledgmentMsg ack = (AcknowledgmentMsg)message;
      if (!ack.isAck()) {
        logger.error("Recieved Nack for Hello Message sent for "+((HelloMsg) message).getWrapperDetails().toString());
        logger.error("Closing the connection to the SafeStorageServer...");
        session.close();
        return;
      }else {
        updatePositiveAck(ack.getSeqNumber());
      }
    }
    //At this point we've got either HelloMsg or Positive AckMsg
    // keep sending new data 
    postData(session);
  }
  
  private void updatePositiveAck(long seqNumber) throws SQLException {
    successAckUpdatePS.clearParameters();
    successAckUpdatePS.setLong(1, seqNumber);
    successAckUpdatePS.executeUpdate();
  }
  
  /**
   * Send one (block until one available to be sent).
   * @param session
   * @throws InterruptedException 
   */
  private void postData(IoSession session) throws SQLException, InterruptedException{
    ResultSet rs = readerPS.executeQuery();
    if (rs.next()) {
      long pk =rs.getLong(1);
      Object[]  se = (Object[]) rs.getArray(2).getArray();
      long ts = rs.getTimestamp(3).getTime();
      rs.close();
      session.write(new DataMsg(se,pk,ts));
      logger.debug("Sending data");
    }
    else { 
      logger.debug("Blocking for the wrapper's until a new data have generated.");
      wrapper.canReaderDB();
      postData(session);
    }
  }

public void sessionClosed(IoSession session) throws Exception {
  readerPS.close();
  successAckUpdatePS.close();
  // Update the number of clients using this wrapper.
}

public void sessionOpened(IoSession session) throws Exception {
  // Update the number of clients using this wrapper.
}

}
