/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
* 
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: src/gsn/acquisition2/client/SafeStorageClientSessionHandler.java
*
* @author Timotee Maret
* @author Ali Salehi
*
*/

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
  
  private MessageHandler handler;
  
  public SafeStorageClientSessionHandler(AddressBean wrapprDetails,MessageHandler handler,String requester) {
    this.handler=handler;
    helloMsg = new HelloMsg(wrapprDetails,requester);
  }
  
  public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
	  logger.error(cause.getMessage(), cause);
	  session.close();
  }
  public void messageReceived(IoSession session, Object message) throws Exception {
    logger.debug("Received data from the server");
    DataMsg dataMsg = (DataMsg) message;
    if (handler.messageToBeProcessed(dataMsg)) {
      session.write(new AcknowledgmentMsg(AcknowledgmentMsg.SUCCESS,dataMsg.getSequenceNumber()));
      logger.debug("Sending Success Ack");
    }else {
      session.write(new AcknowledgmentMsg(AcknowledgmentMsg.FAILURE,dataMsg.getSequenceNumber()));
      logger.debug("Sending Success Nack");
    }
    
  }
  public void messageSent(IoSession session, Object message) throws Exception {

  }
  public void sessionClosed(IoSession session) throws Exception {
	  logger.warn("Session >" + session + "< is closed");
	  handler.restartConnection();
  }
  public void sessionOpened(IoSession session) throws Exception {
    session.write(helloMsg);
    logger.warn("Session >" + session + "< is open");
  }
  
}
