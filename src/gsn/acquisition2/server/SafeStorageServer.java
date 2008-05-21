package gsn.acquisition2.server;

import gsn.*;
import gsn.acquisition2.SafeStorage;

import java.io.*;
import java.net.*;
import java.sql.SQLException;

import org.apache.log4j.*;
import org.apache.mina.common.*;
import org.apache.mina.filter.codec.*;
import org.apache.mina.filter.codec.serialization.*;
import org.apache.mina.transport.socket.nio.*;

public class SafeStorageServer {
  
	public static transient Logger logger = Logger.getLogger(SafeStorageServer.class);
	
	private IoAcceptor acceptor;
	
  public SafeStorageServer(int portNo) throws IOException, ClassNotFoundException, SQLException {
    SafeStorage ss  = new SafeStorage(portNo);
    acceptor = new SocketAcceptor();
    acceptor.getDefaultConfig().setThreadModel(ThreadModel.MANUAL);
    // Prepare the service configuration.
    SocketAcceptorConfig cfg = new SocketAcceptorConfig();
    cfg.setReuseAddress(true);
    cfg.getFilterChain().addLast("codec",   new ProtocolCodecFilter( new ObjectSerializationCodecFactory()));
    acceptor.bind(new InetSocketAddress(portNo),   new SafeStorageServerSessionHandler(ss), cfg);
    logger.info("Safe Storage Server is listening on port: " + portNo);
  }
  
  public void shutdown () {
	  acceptor.unbindAll();
  }
  
  public static void main(String[] args) throws Exception {
	  int safeStorageServerPort = Integer.parseInt(args[0]);
	int safeStorageControllerPort = Integer.parseInt(args[1]);
    PropertyConfigurator.configure ( Main.DEFAULT_GSN_LOG4J_PROPERTIES );
    SafeStorageServer sss = new SafeStorageServer(safeStorageServerPort);
    new SafeStorageController(sss, safeStorageControllerPort);
  }
}
