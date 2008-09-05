package gsn.msr.sensormap;

import gsn.GSNController;
import gsn.VSensorLoader;
import gsn.beans.ContainerConfig;
import gsn.beans.VSensorConfig;
import gsn.storage.StorageManager;
import gsn.utils.ValidityTools;
import gsn.wrappers.WrappersUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Random;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.servlet.HashSessionIdManager;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * Web Service URL : http://localhost:22001/services/Service?wsdl
 *
 */
public final class Main {

  private static Main singleton ;

  private static int gsnControllerPort;

  private Main() throws Exception{
    System.out.println("GSN Starting ...");
    ValidityTools.checkAccessibilityOfFiles ( DEFAULT_GSN_LOG4J_PROPERTIES , WrappersUtil.DEFAULT_WRAPPER_PROPERTIES_FILE , DEFAULT_GSN_CONF_FILE );
    ValidityTools.checkAccessibilityOfDirs ( DEFAULT_VIRTUAL_SENSOR_DIRECTORY );
    PropertyConfigurator.configure ( Main.DEFAULT_GSN_LOG4J_PROPERTIES );
//  initializeConfiguration();
    try {
      controlSocket = new GSNController(null, gsnControllerPort);
      containerConfig = loadContainerConfiguration();
    } catch ( FileNotFoundException e ) {
      logger.error ( new StringBuilder ( ).append ( "The the configuration file : conf/gsn.xml").append ( " doesn't exist." ).toString ( ) );
      logger.error ( e.getMessage ( ) );
      logger.error ( "Check the path of the configuration file and try again." );
      if ( logger.isDebugEnabled ( ) ) logger.debug ( e.getMessage ( ) , e );
      return;
    } catch (UnknownHostException e) {
      logger.error ( e.getMessage ( ),e );
      return;
    } catch (IOException e) {
      logger.error ( e.getMessage ( ),e );
      return;
    } catch (Exception e) {
      logger.error ( e.getMessage ( ),e );
      return;
    }
    StorageManager.getInstance ( ).init ( containerConfig.getJdbcDriver ( ) , containerConfig.getJdbcUsername ( ) , containerConfig.getJdbcPassword ( ) , containerConfig.getJdbcURL ( ) );
    if ( logger.isInfoEnabled ( ) ) logger.info ( "The Container Configuration file loaded successfully." );
    final Server server = new Server ( );
    //Connector connector = new SelectChannelConnector( ); //using basic connector for windows bug
    Connector httpConnector = new SocketConnector ();
    httpConnector.setPort ( containerConfig.getContainerPort ( ) );
    SslSocketConnector sslSocketConnector = null;
    if (getContainerConfig().getSSLPort()>10){
      sslSocketConnector = new SslSocketConnector();
      sslSocketConnector.setKeystore("conf/gsn.jks");
      sslSocketConnector.setKeyPassword(getContainerConfig().getSSLKeyPassword());
      sslSocketConnector.setPassword(getContainerConfig().getSSLKeyStorePassword());
      sslSocketConnector.setPort(getContainerConfig().getSSLPort());
    }

    if (sslSocketConnector==null)
      server.setConnectors ( new Connector [ ] { httpConnector } );
    else
      server.setConnectors ( new Connector [ ] { httpConnector,sslSocketConnector } );
    WebAppContext webAppContext = new WebAppContext ( );
    webAppContext.setContextPath ( "/" );
    webAppContext.setResourceBase ( DEFAULT_WEB_APP_PATH );
    server.addHandler( webAppContext );
    server.setStopAtShutdown ( true );
    server.setSendServerVersion ( false );
    server.setSessionIdManager(new HashSessionIdManager(new Random()));
    server.addUserRealm(new HashUserRealm("GSNRealm","conf/realm.properties"));

    try {
      logger.debug("Starting the http-server @ port: "+containerConfig.getContainerPort()+" ...");
      server.start ( );
      logger.debug("http-server running @ port: "+containerConfig.getContainerPort());
    } catch ( Exception e ) {
      logger.error ( "Start of the HTTP server failed. The HTTP protocol is used in most of the communications." );
      logger.error ( e.getMessage ( ) , e );
      return;
    }
    final VSensorLoader vsloader = new VSensorLoader ( DEFAULT_VIRTUAL_SENSOR_DIRECTORY );
    controlSocket.setLoader(vsloader);
  }

  public synchronized static Main getInstance() {
    if (singleton==null)
      try {
        singleton=new Main();
      } catch (Exception e) {
        logger.error(e.getMessage(),e);
        System.exit(1);
      }
      return singleton;
  }

  private GSNController controlSocket;

  public static final String     DEFAULT_GSN_LOG4J_PROPERTIES     = "conf/log4j.properties";

  public static transient Logger logger= Logger.getLogger ( Main.class );

  public static final String     DEFAULT_GSN_CONF_FILE            = "conf/gsn.xml";

  public static String     DEFAULT_VIRTUAL_SENSOR_DIRECTORY = "virtual-sensors";

  public static final String     DEFAULT_WEB_APP_PATH             = "webapp";



  public static void main ( String [ ]  args)  {
    Main.gsnControllerPort = Integer.parseInt(args[0]) ;
    Main.getInstance();
  }

  /**
   * Mapping between the wrapper name (used in addressing of stream source)
   * into the class implementing DataSource.
   */
  private static  Properties wrappers ;

  private  ContainerConfig                       containerConfig;

  private  HashMap < String , VSensorConfig >    virtualSensors;

  public static ContainerConfig loadContainerConfiguration() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, KeyStoreException, CertificateException, SecurityException, SignatureException, IOException{
    ValidityTools.checkAccessibilityOfFiles ( Main.DEFAULT_GSN_LOG4J_PROPERTIES , WrappersUtil.DEFAULT_WRAPPER_PROPERTIES_FILE , Main.DEFAULT_GSN_CONF_FILE );
    ValidityTools.checkAccessibilityOfDirs ( Main.DEFAULT_VIRTUAL_SENSOR_DIRECTORY );
    PropertyConfigurator.configure ( Main.DEFAULT_GSN_LOG4J_PROPERTIES );
    ContainerConfig toReturn = null;
    try {
      toReturn = loadContainerConfig (DEFAULT_GSN_CONF_FILE );
      wrappers = WrappersUtil.loadWrappers(new HashMap<String, Class<?>>());
      if ( logger.isInfoEnabled ( ) ) logger.info ( new StringBuilder ( ).append ( "Loading wrappers.properties at : " ).append ( WrappersUtil.DEFAULT_WRAPPER_PROPERTIES_FILE ).toString ( ) );
      if ( logger.isInfoEnabled ( ) ) logger.info ( "Wrappers initialization ..." );
    } catch ( JiBXException e ) {
      logger.error ( e.getMessage ( ) );
      logger.error ( new StringBuilder ( ).append ( "Can't parse the GSN configuration file : conf/gsn.xml" ).toString ( ) );
      logger.error ( "Please check the syntax of the file to be sure it is compatible with the requirements." );
      logger.error ( "You can find a sample configuration file from the GSN release." );
      if ( logger.isDebugEnabled ( ) ) logger.debug ( e.getMessage ( ) , e );
      System.exit ( 1 );
    } catch ( FileNotFoundException e ) {
      logger.error ( new StringBuilder ( ).append ( "The the configuration file : conf/gsn.xml").append ( " doesn't exist." ).toString ( ) );
      logger.error ( e.getMessage ( ) );
      logger.error ( "Check the path of the configuration file and try again." );
      if ( logger.isDebugEnabled ( ) ) logger.debug ( e.getMessage ( ) , e );
      System.exit ( 1 );
    } catch ( ClassNotFoundException e ) {
      logger.error ( "The file wrapper.properties refers to one or more classes which don't exist in the classpath");
      logger.error ( e.getMessage ( ),e );
      System.exit ( 1 );
    }finally {
      return toReturn;
    }
  }

  /**
   * This method is called by Rails's Application.rb file.
   */
  public static ContainerConfig loadContainerConfig (String gsnXMLpath) throws JiBXException, FileNotFoundException, NoSuchAlgorithmException, NoSuchProviderException, IOException, KeyStoreException, CertificateException, SecurityException, SignatureException, InvalidKeyException, ClassNotFoundException {
    IBindingFactory bfact = BindingDirectory.getFactory ( ContainerConfig.class );
    IUnmarshallingContext uctx = bfact.createUnmarshallingContext ( );
    ContainerConfig conf = ( ContainerConfig ) uctx.unmarshalDocument ( new FileInputStream ( new File ( gsnXMLpath ) ) , null );
    Class.forName(conf.getJdbcDriver());
    conf.setContainerConfigurationFileName (  gsnXMLpath );
    return conf;
  }

public final HashMap < String , VSensorConfig > getVirtualSensors ( ) {
    return virtualSensors;
  }

  public  boolean justConsumes ( ) {
    Iterator < VSensorConfig > vsconfigs = virtualSensors.values ( ).iterator ( );
    while ( vsconfigs.hasNext ( ) )
      if ( !vsconfigs.next ( ).needsStorage ( ) ) return false;
    return true;
  }

  /**
   * Get's the GSN configuration without starting GSN.
   * @return
   * @throws Exception
   */
  public static ContainerConfig getContainerConfig ( ) {
    if (singleton==null)
      try {
        return loadContainerConfig(DEFAULT_GSN_CONF_FILE);
      } catch (Exception e) {
        return null;
      }
      else
        return singleton.containerConfig;
  }

}
