package gsn;

import gsn.beans.ContainerConfig;
import gsn.beans.VSensorConfig;
import gsn.storage.StorageManager;
import gsn.utils.ValidityTools;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
//import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.webapp.WebAppContext;

public final class Main {
  
  private static GSNController controlSocket;
  
  public static final String     DEFAULT_GSN_LOG4J_PROPERTIES     = "conf/log4j.properties";
  
  public static transient Logger logger= Logger.getLogger ( Main.class );
  
  public static final String     DEFAULT_WRAPPER_PROPERTIES_FILE  = "conf/wrappers.properties";
  
  public static final String     DEFAULT_GSN_CONF_FILE            = "conf/gsn.xml";
  
  public static final String     DEFAULT_VIRTUAL_SENSOR_DIRECTORY = "virtual-sensors";
  
  public static final String     DEFAULT_WEB_APP_PATH             = "webapp";
  
  public static void main ( String [ ]  args)  {
    System.out.println("GSN Starting ...");
    ValidityTools.checkAccessibilityOfFiles ( DEFAULT_GSN_LOG4J_PROPERTIES , DEFAULT_WRAPPER_PROPERTIES_FILE , DEFAULT_GSN_CONF_FILE );
    ValidityTools.checkAccessibilityOfDirs ( DEFAULT_VIRTUAL_SENSOR_DIRECTORY );
    PropertyConfigurator.configure ( DEFAULT_GSN_LOG4J_PROPERTIES );
    try {
      controlSocket = new GSNController(null);
      initialize ( "conf/gsn.xml" );
    } catch ( JiBXException e ) {
      logger.error ( e.getMessage ( ) );
      logger.error ( new StringBuilder ( ).append ( "Can't parse the GSN configuration file : conf/gsn.xml" ).toString ( ) );
      logger.error ( "Please check the syntax of the file to be sure it is compatible with the requirements." );
      logger.error ( "You can find a sample configuration file from the GSN release." );
      if ( logger.isDebugEnabled ( ) ) logger.debug ( e.getMessage ( ) , e );
      return;
    } catch ( FileNotFoundException e ) {
      logger.error ( new StringBuilder ( ).append ( "The the configuration file : conf/gsn.xml").append ( " doesn't exist." ).toString ( ) );
      logger.error ( e.getMessage ( ) );
      logger.error ( "Check the path of the configuration file and try again." );
      if ( logger.isDebugEnabled ( ) ) logger.debug ( e.getMessage ( ) , e );
      return;
    } catch ( ClassNotFoundException e ) {
      logger.error ( "The file wrapper.properties refers to one or more classes which don't exist in the classpath");
      logger.error ( e.getMessage ( ),e );
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
    StorageManager.getInstance ( ).initialize ( containerConfig.getJdbcDriver ( ) , containerConfig.getJdbcUsername ( ) , containerConfig.getJdbcPassword ( ) , containerConfig.getJdbcURL ( ) );
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
    ServletHandler servletHandler = new ServletHandler ( );
    servletHandler.addServletWithMapping ( "gsn.GSNRPC" , "/gsn-handler" );
    servletHandler.addServletWithMapping ( "gsn.http.ControllerServlet" , "/gsn" );
    servletHandler.addServletWithMapping ( "gsn.http.DataDownload" , "/data" );
    servletHandler.addServletWithMapping ( "gsn.http.FieldDownloadServlet" , "/field" );
    servletHandler.addServletWithMapping ( "gsn.http.FieldUpload" , "/upload" );
    servletHandler.addServletWithMapping ( "gsn.msr.sensormap.MSRSenseRSSHandler" , "/rss" );
    webAppContext.setServletHandler ( servletHandler );
    /// webAppContext.setConnectorNames(new String[]{httpConnector.getName()});
    
    server.setHandler ( webAppContext );
    server.setStopAtShutdown ( true );
    server.setSendServerVersion ( false );
    try {
      server.start ( );
    } catch ( Exception e ) {
      logger.error ( "Start of the HTTP server failed. The HTTP protocol is used in most of the communications." );
      logger.error ( e.getMessage ( ) , e );
      return;
    }
    final VSensorLoader vsloader = new VSensorLoader ( DEFAULT_VIRTUAL_SENSOR_DIRECTORY );
    controlSocket.setLoader(vsloader);
    
  }
  
  /**
   * Mapping between the wrapper name (used in addressing of stream source)
   * into the class implementing DataSource.
   */
  private static final HashMap < String , Class < ? >> wrappers = new HashMap < String , Class < ? >>( );
  
  private static ContainerConfig                       containerConfig;
  
  private static HashMap < String , VSensorConfig >    virtualSensors;
  
  public static void initializeWrappers() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, KeyStoreException, CertificateException, SecurityException, SignatureException, IOException{
    ValidityTools.checkAccessibilityOfFiles ( DEFAULT_GSN_LOG4J_PROPERTIES , DEFAULT_WRAPPER_PROPERTIES_FILE , DEFAULT_GSN_CONF_FILE );
    ValidityTools.checkAccessibilityOfDirs ( DEFAULT_VIRTUAL_SENSOR_DIRECTORY );
    PropertyConfigurator.configure ( DEFAULT_GSN_LOG4J_PROPERTIES );
    try {
      initialize ( "conf/gsn.xml" );
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
    }
  }
  
  private static void initialize ( String containerConfigurationFileName ) throws JiBXException , FileNotFoundException, NoSuchAlgorithmException, NoSuchProviderException, IOException, KeyStoreException, CertificateException, SecurityException, SignatureException, InvalidKeyException, ClassNotFoundException {
    containerConfig = loadConfiguration ( containerConfigurationFileName );
    Class.forName(containerConfig.getJdbcDriver());
    containerConfig.setContainerConfigurationFileName ( containerConfigurationFileName );
    if ( logger.isInfoEnabled ( ) ) logger.info ( new StringBuilder ( ).append ( "Loading wrappers.properties at : " ).append ( DEFAULT_WRAPPER_PROPERTIES_FILE ).toString ( ) );
    Configuration config = null;
    
    try {// Trying to load the wrapper specified in the configuration
      // file of
      // the container.
      config = new PropertiesConfiguration ( DEFAULT_WRAPPER_PROPERTIES_FILE );
    } catch ( ConfigurationException e ) {
      logger.error ( "The wrappers configuration file's syntax is not compatible." );
      logger.error ( new StringBuilder ( ).append ( "Check the :" ).append ( DEFAULT_WRAPPER_PROPERTIES_FILE ).append ( " file and make sure it's syntactically correct." ).toString ( ) );
      logger.error ( "Sample wrappers extention properties file is provided in GSN distribution." );
      logger.error ( e.getMessage ( ) , e );
      System.exit ( 1 );
    }
    // Adding the wrappers to the GSN data structures.
    if ( logger.isInfoEnabled ( ) ) logger.info ( "Wrappers initialization ..." );
    loadWrapperList(config);
    //initPKI (PUBLIC_KEY_FILE, PUBLIC_KEY_FILE);
  }
  
  public static void loadWrapperList(Configuration config) throws ClassNotFoundException{
    String wrapperNames[] = config.getStringArray ( "wrapper.name" );
    String wrapperClasses[] = config.getStringArray ( "wrapper.class" );
    for ( int i = 0 ; i < wrapperNames.length ; i++ ) {
      String name = wrapperNames[ i ];
      String className = wrapperClasses[ i ];
      if ( wrappers.get ( name ) != null ) {
        logger.error ( "The wrapper name : " + name + " is used more than once in the properties file." );
        logger.error ( new StringBuilder ( ).append ( "Please check the " ).append ( DEFAULT_WRAPPER_PROPERTIES_FILE ).append ( " file and try again." ).toString ( ) );
        System.exit ( 1 );
      }
      Class wrapperClass = Class.forName ( className );
      wrappers.put ( name , wrapperClass );
      if ( logger.isInfoEnabled ( ) ) logger.info ( new StringBuilder ( ).append ( "Wrapper [" ).append ( name ).append ( "] added successfully." ).toString ( ) );
    }
    
  }
  public static void resetWrapperList() {
    wrappers.clear();
  }
  
  private static final String PUBLIC_KEY_FILE=".public_key";
  
  private static final String PRIVATE_KEY_FILE=".private_key";
  
  public static void initPKI ( String publicKeyFile,String privateKeyFile ) throws NoSuchAlgorithmException , NoSuchProviderException , FileNotFoundException , IOException, KeyStoreException, CertificateException, SecurityException, SignatureException, InvalidKeyException {
    // TODO  : Use the pri/pub keys if they exist. (needs verification first).
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance ( "DSA" , "SUN" );
    SecureRandom random = SecureRandom.getInstance ( "SHA1PRNG" , "SUN" );
    keyGen.initialize ( 512 , random );
    KeyPair pair = keyGen.generateKeyPair ( );
    PrivateKey priv = pair.getPrivate ( );
    PublicKey pub = pair.getPublic ( );
    CertificateFactory certificateFactory =  CertificateFactory.getInstance ("X.509");
    
    File privateF = new File (privateKeyFile);
    File publicF = new File (publicKeyFile);
    publicF.createNewFile ();
    privateF.createNewFile ();
    OutputStream output = new FileOutputStream (privateF );
    output.write ( priv.getEncoded ( ) );
    output.close ( );
    output = new FileOutputStream ( publicF );
    output.write ( pub.getEncoded ( ) );
    output.close ( );
    KeyStore ksca = KeyStore.getInstance ("JKS","SUN");
    ksca.load (null);
    logger.warn ("Public and Private keys are generated successfully.");
  }
  
  private static PrivateKey readPrivateKey () throws FileNotFoundException, IOException, NoSuchAlgorithmException, InvalidKeySpecException{
    FileInputStream keyfis = new FileInputStream (PRIVATE_KEY_FILE);
    byte[] encKey = new byte[keyfis.available ()];
    keyfis.read (encKey);
    keyfis.close ();
    PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec (encKey);
    KeyFactory keyFactory = KeyFactory.getInstance ("DSA");
    return keyFactory.generatePrivate (privKeySpec);
  }
  
  private static PublicKey readPublicKey () throws IOException, NoSuchAlgorithmException, InvalidKeySpecException{
    FileInputStream keyfis = new FileInputStream (PUBLIC_KEY_FILE);
    byte[] encKey = new byte[keyfis.available ()];
    keyfis.read (encKey);
    keyfis.close ();
    PKCS8EncodedKeySpec pubKeySpec = new PKCS8EncodedKeySpec (encKey);
    KeyFactory keyFactory = KeyFactory.getInstance ("DSA");
    return keyFactory.generatePublic (pubKeySpec);
  }
  
  public static ContainerConfig loadConfiguration ( String containerConfigurationFileName ) throws JiBXException , FileNotFoundException {
    return loadConfiguration ( new File ( containerConfigurationFileName ) );
  }
  
  public static ContainerConfig loadConfiguration ( File containerConfigurationFileName ) throws JiBXException , FileNotFoundException {
    IBindingFactory bfact = BindingDirectory.getFactory ( ContainerConfig.class );
    IUnmarshallingContext uctx = bfact.createUnmarshallingContext ( );
    return ( ContainerConfig ) uctx.unmarshalDocument ( new FileInputStream ( containerConfigurationFileName ) , null );
  }
  
  public static Class < ? > getWrapperClass ( String id ) {
    return wrappers.get ( id );
  }
  
  public final HashMap < String , VSensorConfig > getVirtualSensors ( ) {
    return virtualSensors;
  }
  
  public static boolean justConsumes ( ) {
    Iterator < VSensorConfig > vsconfigs = virtualSensors.values ( ).iterator ( );
    while ( vsconfigs.hasNext ( ) )
      if ( !vsconfigs.next ( ).needsStorage ( ) ) return false;
    return true;
  }
  
  public static ContainerConfig getContainerConfig ( ) {
    return containerConfig;
  }
  
  public static String randomTableNameGenerator ( int length ) {
    byte oneCharacter;
    StringBuffer result = new StringBuffer ( length );
    for ( int i = 0 ; i < length ; i++ ) {
      oneCharacter = ( byte ) ( ( Math.random ( ) * ( 'z' - 'a' + 1 ) ) + 'a' );
      result.append ( ( char ) oneCharacter );
    }
    return result.toString ( );
  }
  
  public static int tableNameGenerator ( ) {
    return randomTableNameGenerator ( 15 ).hashCode ( );
  }
  
  public static StringBuilder tableNameGeneratorInString (int code) {
    StringBuilder sb = new StringBuilder ("_");
    if (code<0)
      sb.append ( "_" );
    sb.append ( Math.abs (code) );
    return sb;
  }
  
  /**
   * @param containerConfig The containerConfig to set.
   */
  public static void setContainerConfig ( ContainerConfig containerConfig ) {
    if ( Main.containerConfig == null ) {
      Main.containerConfig = containerConfig;
    } else {
      throw new RuntimeException ( "Trying to replace the container config object in main class." );
    }
  }
}
