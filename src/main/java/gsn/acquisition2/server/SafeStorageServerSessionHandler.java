/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
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
* File: src/gsn/acquisition2/server/SafeStorageServerSessionHandler.java
*
* @author Timotee Maret
* @author Ali Salehi
*
*/

package gsn.acquisition2.server;

import gsn.acquisition2.SafeStorage;
import gsn.acquisition2.messages.AcknowledgmentMsg;
import gsn.acquisition2.messages.DataMsg;
import gsn.acquisition2.messages.HelloMsg;
import gsn.acquisition2.wrappers.AbstractWrapper2;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;

public class SafeStorageServerSessionHandler extends IoHandlerAdapter{
	
	private static final String SESSION_STATE_KEY = "ssk";
	
	private SafeStorage ss;

	public SafeStorageServerSessionHandler(SafeStorage ss) throws ClassNotFoundException, SQLException {
		this.ss = ss;
	}

	private static transient Logger                                logger                              = LoggerFactory.getLogger ( SafeStorageServerSessionHandler.class );

	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		session.close();
		// Update the number of clients using this wrapper.
	}

	public void messageReceived(IoSession session, Object message) throws Exception {

		if (message instanceof HelloMsg) {
			HelloMsg hello = (HelloMsg) message;
			logger.debug("Hello received : "+hello.getWrapperDetails().toString());

			AbstractWrapper2 wrapper = ss.prepareWrapper(hello, session);
			if (wrapper == null) {
				//session.close();
				return;
			}
			SessionState sstate = new SessionState () ;
			sstate.setWrapper(wrapper);
			sstate.setReaderPS(ss.getStorage().createPreparedStatement("select pk,stream_element,created_at from "+wrapper.getTableName()+" where processed = false order by pk asc limit 1"));	
			logger.debug("isKeepProcessedSafeStorageEntries: " + wrapper.isKeepProcessedSafeStorageEntries());
			if (wrapper.isKeepProcessedSafeStorageEntries()) {
				sstate.setSuccessAckUpdatePS(ss.getStorage().createPreparedStatement("update "+wrapper.getTableName()+" set PROCESSED  = true where pk = ? "));
			}
			else {
				sstate.setSuccessAckUpdatePS(ss.getStorage().createPreparedStatement("delete from " + wrapper.getTableName() + " where pk = ? "));
			}
			session.setAttribute(SESSION_STATE_KEY, sstate);
		}
		if (message instanceof AcknowledgmentMsg) {
			AcknowledgmentMsg ack = (AcknowledgmentMsg)message;
			if (!ack.isAck()) {
				logger.error("Recieved Nack for Hello Message sent for "+((HelloMsg) message).getWrapperDetails().toString());
				logger.info("Closing the connection to the SafeStorageServer...");
				session.close();
				return;
			}else {
				SessionState sstate = (SessionState) session.getAttribute(SESSION_STATE_KEY);
				if (sstate != null) {
					sstate.getSuccessAckUpdatePS().clearParameters();
					sstate.getSuccessAckUpdatePS().setLong(1, ack.getSeqNumber());
					sstate.getSuccessAckUpdatePS().executeUpdate();
				}
				else {
					logger.error("No Session State found for session >" + session + "<");
				}
			}
		}
		//At this point we've got either HelloMsg or Positive AckMsg
		// keep sending new data 
		postData(session);
	}

	/**
	 * Send one (block until one available to be sent).
	 * @param session
	 * @throws InterruptedException 
	 */
	private void postData(IoSession session) throws SQLException, InterruptedException{
		
		SessionState sstate = (SessionState) session.getAttribute(SESSION_STATE_KEY);
		if (sstate == null) {
			logger.error("No Session State found for session >" + session + "<");
			return ;
		}
		ResultSet rs = sstate.getReaderPS().executeQuery();
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
			sstate.getWrapper().canReaderDB();
			postData(session);
		}
	}

	public void sessionClosed(IoSession session) throws Exception {
		
		SessionState sstate =  (SessionState) session.getAttribute(SESSION_STATE_KEY);
		if (sstate == null) {
			logger.error("No Session State found for session >" + session + "<");
			return ;
		}
		
		if (sstate.getReaderPS() != null) sstate.getReaderPS().close();
		if (sstate.getSuccessAckUpdatePS() != null) sstate.getSuccessAckUpdatePS().close();
		
		logger.warn("Session >" + session + "< is closed");
		
		// Update the number of clients using this wrapper.
	}

	public void sessionOpened(IoSession session) throws Exception {
		
		logger.warn("Session >" + session + "< is open");
		
		// Update the number of clients using this wrapper.
	}

	private class SessionState {

		private AbstractWrapper2 wrapper;
		
		private PreparedStatement readerPS = null;
		
		private PreparedStatement successAckUpdatePS;

		public SessionState () {}

		public AbstractWrapper2 getWrapper() {
			return wrapper;
		}

		public void setWrapper(AbstractWrapper2 wrapper) {
			this.wrapper = wrapper;
		}

		public PreparedStatement getReaderPS() {
			return readerPS;
		}

		public void setReaderPS(PreparedStatement readerPS) {
			this.readerPS = readerPS;
		}

		public PreparedStatement getSuccessAckUpdatePS() {
			return successAckUpdatePS;
		}

		public void setSuccessAckUpdatePS(PreparedStatement successAckUpdatePS) {
			this.successAckUpdatePS = successAckUpdatePS;
		}
	}
}
