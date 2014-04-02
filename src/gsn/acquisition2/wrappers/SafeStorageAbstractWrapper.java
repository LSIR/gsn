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
* File: src/gsn/acquisition2/wrappers/SafeStorageAbstractWrapper.java
*
* @author Timotee Maret
* @author Ali Salehi
* @author Mehdi Riahi
*
*/

package gsn.acquisition2.wrappers;

import gsn.acquisition2.client.MessageHandler;
import gsn.acquisition2.client.SafeStorageClientSessionHandler;
import gsn.beans.AddressBean;
import gsn.wrappers.AbstractWrapper;

import java.net.InetSocketAddress;

import org.apache.log4j.Logger;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;
/**
 * Required parameters: 
 * ss-port
 * ss-host
 * wrapper-name
 *
 */
public abstract class SafeStorageAbstractWrapper extends AbstractWrapper implements MessageHandler{
	
	private static final long CONNECTION_RETRY_TIME = 10000;
  
  private final transient Logger     logger                 = Logger.getLogger ( SafeStorageAbstractWrapper.class );

  public void dispose() {
    // TODO
  }

  public String getWrapperName() {
    return "Safe Storage Proxy - "+key;
  }
  
  String key,ss_host;
  AddressBean wrapperDetails;
  int ss_port;
  
  public boolean initialize() {
    String wrapper = getActiveAddressBean().getPredicateValue("wrapper-name");
    String vs = getActiveAddressBean().getVirtualSensorName();
    String inputStreamName = getActiveAddressBean().getInputStreamName();
    wrapperDetails = getActiveAddressBean();
    key = new StringBuilder(vs).append("/").append(inputStreamName).append("/").append(wrapper).toString();
    ss_host = getActiveAddressBean().getPredicateValue("ss-host");
    ss_port = getActiveAddressBean().getPredicateValueAsInt("ss-port",-1);
    return true;
  }
  public void run() {
	  boolean connected = false;
	  while (! connected) {
		  connected = connect(ss_host,ss_port,wrapperDetails,this,key);
		  if (! connected) {
			  try {
				Thread.sleep(CONNECTION_RETRY_TIME);
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
			}
		  }
	  } 
  }
  
 /**
  * HELPER METHOD FOR CONNECTING TO STORAGE SERVER
  */
  public boolean connect(String host,int port,AddressBean wrapperDetails,MessageHandler handler,String requester) {
    int CONNECT_TIMEOUT = 30; // seconds
    SocketConnector connector = new SocketConnector();
    // Change the worker timeout to 1 second to make the I/O thread quit soon
    // when there's no connection to manage.
    connector.setWorkerTimeout(1);
    // Configure the service.
    SocketConnectorConfig cfg = new SocketConnectorConfig();
    cfg.setConnectTimeout(CONNECT_TIMEOUT);
    ObjectSerializationCodecFactory oscf = new ObjectSerializationCodecFactory();
    oscf.setDecoderMaxObjectSize(oscf.getEncoderMaxObjectSize());
    //logger.debug("MINA Decoder MAX: " + oscf.getDecoderMaxObjectSize() + " MINA Encoder MAX: " + oscf.getEncoderMaxObjectSize());
    cfg.getFilterChain().addLast("codec",   new ProtocolCodecFilter(oscf));
    IoSession session = null;
    try {
      ConnectFuture future = connector.connect(new InetSocketAddress(host, port), new SafeStorageClientSessionHandler(wrapperDetails,handler,key ), cfg);
      future.join();
      session = future.getSession();
      return true;
    } catch (RuntimeIOException e) {
      logger.error("Failed to connect to SafeStorage on "+host+":"+port); 
      return false;
    }finally {
      if (session!=null) {
        session.getCloseFuture().join();
      }
    }
  }
  
  public void restartConnection () {
	  run();
  }
}