package gsn.acquisition2.client;

import gsn.*;
import gsn.beans.*;
import gsn.utils.*;
import java.net.*;
import org.apache.log4j.*;
import org.apache.mina.common.*;
import org.apache.mina.filter.codec.*;
import org.apache.mina.filter.codec.serialization.*;
import org.apache.mina.transport.socket.nio.*;

public class SafeStorageClient {
  
  private static final int CONNECT_TIMEOUT = 30; // seconds 
  
  private static transient Logger                                logger                              = Logger.getLogger ( SafeStorageClient.class );
  
  
  public SafeStorageClient(String host,int port,AddressBean wrapperDetails) {
    SocketConnector connector = new SocketConnector();
    // Change the worker timeout to 1 second to make the I/O thread quit soon
    // when there's no connection to manage.
    connector.setWorkerTimeout(1);
    // Configure the service.
    SocketConnectorConfig cfg = new SocketConnectorConfig();
    cfg.setConnectTimeout(CONNECT_TIMEOUT);
    cfg.getFilterChain().addLast("codec",   new ProtocolCodecFilter( new ObjectSerializationCodecFactory()));
    IoSession session = null;
    try {
      ConnectFuture future = connector.connect(new InetSocketAddress(host, port), new SafeStorageClientSessionHandler(wrapperDetails ), cfg);
      future.join();
      session = future.getSession();
    } catch (RuntimeIOException e) {
      logger.error("Failed to connect to "+host+":"+port); 
      logger.error( e.getMessage(),e);
    }finally {
      if (session!=null)
        session.getCloseFuture().join();
    }
  }
  
  public static void main(String[] args) {
    PropertyConfigurator.configure ( Main.DEFAULT_GSN_LOG4J_PROPERTIES );
    AddressBean wrapperDetails = new AddressBean("mem2",new KeyValueImp("MyKey","MyValue"));
    new SafeStorageClient("localhost",12345,wrapperDetails);
  }
}
