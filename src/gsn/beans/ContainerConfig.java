package gsn.beans;

import gsn.utils.ValidityTools;
import java.beans.PropertyChangeListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.StringTokenizer;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.OptionConverter;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;
import com.jgoodies.binding.beans.ExtendedPropertyChangeSupport;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class ContainerConfig {
   
   private ExtendedPropertyChangeSupport changeSupport = new ExtendedPropertyChangeSupport( this );
   
   public void addPropertyChangeListener ( PropertyChangeListener changeListener ) {
      changeSupport.addPropertyChangeListener( changeListener );
   }
   
   public void removePropertyChangeListener ( PropertyChangeListener changeListener ) {
      changeSupport.removePropertyChangeListener( changeListener );
   }
   
   private static final transient Logger logger                           = Logger.getLogger( ContainerConfig.class );
   
   public static final String            NOT_PROVIDED                     = "Not Provided";
   
   public static final int               DEFAULT_GSN_PORT                 = 22001;
   
   public static final int               DEFAULT_STORAGE_POOL_SIZE        = 100;
   
   protected String                      webName;
   
   public static final String            FIELD_NAME_webName               = "webName";
   
   protected String                      webAuthor;
   
   public static final String            FIELD_NAME_webAuthor             = "webAuthor";
   
   protected String                      webDescription;
   
   public static final String            FIELD_NAME_webDescription        = "webDescription";
   
   protected String                      webEmail;
   
   public static final String            FIELD_NAME_webEmail              = "webEmail";
   
   protected String                      mailServer;
   
   protected String                      smsServer;
   
   protected String                      smsPassword;
   
   protected int                         containerPort                    = DEFAULT_GSN_PORT;
   
   protected String                      registryBootstrapAddr;
   
   public static final String            FIELD_NAME_registryBootstrapAddr = "registryBootstrapAddr";
   
   protected String                      containerFileName;
   
   protected String                      jdbcDriver;
   
   protected String                      jdbcUsername;
   
   public static final String            FIELD_NAME_jdbcUsername          = "jdbcUsername";
   
   protected String                      jdbcPassword;
   
   public static final String            FIELD_NAME_jdbcPassword          = "jdbcPassword";
   
   protected String                      jdbcURL;
   
   public static final String            FIELD_NAME_jdbcURL               = "jdbcURL";
   
   protected int                         storagePoolSize                  = DEFAULT_STORAGE_POOL_SIZE;
   
   public String getContainerFileName ( ) {
      return this.containerFileName;
   }
   
   public void setContainerConfigurationFileName ( final String containerFileName ) {
      this.containerFileName = containerFileName;
   }
   
   /**
    * @return Returns the author.
    */
   public String getWebAuthor ( ) {
      if ( this.webAuthor == null || this.webAuthor.trim( ).equals( "" ) )
         this.webAuthor = NOT_PROVIDED;
      else
         this.webAuthor = this.webAuthor.trim( );
      return this.webAuthor;
      
   }
   
   public String getJdbcDriver ( ) {
      return this.jdbcDriver;
   }
   
   public String getJdbcUsername ( ) {
      return this.jdbcUsername;
   }
   
   public String getJdbcPassword ( ) {
      return this.jdbcPassword;
   }
   
   public static final String FIELD_NAME_jdbcDriver = "jdbcDriver";
   
   public void setJdbcDriver ( String newValue ) {
      String oldValue = this.jdbcDriver;
      this.jdbcDriver = newValue;
      changeSupport.firePropertyChange( FIELD_NAME_jdbcDriver , oldValue , newValue );
   }
   
   public void setJdbcPassword ( String newValue ) {
      String oldValue = this.jdbcPassword;
      this.jdbcPassword = newValue;
      changeSupport.firePropertyChange( FIELD_NAME_jdbcPassword , oldValue , newValue );
   }
   
   public void setJdbcUsername ( String newValue ) {
      String oldValue = this.jdbcUsername;
      this.jdbcUsername = newValue;
      changeSupport.firePropertyChange( FIELD_NAME_jdbcUsername , oldValue , newValue );
   }
   
   public void setJdbcURL ( String newValue ) {
      String oldValue = this.jdbcURL;
      this.jdbcURL = newValue;
      changeSupport.firePropertyChange( FIELD_NAME_jdbcURL , oldValue , newValue );
   }
   
   public String getJdbcURL ( ) {
      return this.jdbcURL;
   }
   
   /**
    * @return Returns the containerPort.
    */
   public int getContainerPort ( ) {
      return this.containerPort;
   }
   
   public static final String FIELD_NAME_gsnPortNo = "containerPort";
   
   public void setContainerPort ( int newValue ) {
      int oldValue = this.containerPort;
      this.containerPort = newValue;
      changeSupport.firePropertyChange( FIELD_NAME_gsnPortNo , oldValue , newValue );
   }
   
   /**
    * @return Returns the webDescription.
    */
   public String getWebDescription ( ) {
      if ( this.webDescription == null || this.webDescription.trim( ).equals( "" ) ) this.webDescription = NOT_PROVIDED;
      return this.webDescription.trim( );
   }
   
   /**
    * @return Returns the webEmail.
    */
   public String getWebEmail ( ) {
      if ( this.webEmail == null ) this.webEmail = NOT_PROVIDED;
      return this.webEmail;
   }
   
   /**
    * @return Returns the name.
    */
   public String getWebName ( ) {
      if ( this.webName == null || this.webName.trim( ).equals( "" ) ) this.webName = NOT_PROVIDED;
      this.webName = this.webName.trim( );
      return this.webName;
   }
   
   public void setWebEmail ( String newValue ) {
      String oldValue = this.webEmail;
      this.webEmail = newValue;
      changeSupport.firePropertyChange( FIELD_NAME_webEmail , oldValue , newValue );
   }
   
   public void setWebAuthor ( String newValue ) {
      String oldValue = this.webAuthor;
      this.webAuthor = newValue;
      changeSupport.firePropertyChange( FIELD_NAME_webAuthor , oldValue , newValue );
   }
   
   public void setWebName ( String newValue ) {
      String oldValue = this.webName;
      this.webName = newValue;
      changeSupport.firePropertyChange( FIELD_NAME_webName , oldValue , newValue );
   }
   
   /**
    * @return Returns the registryBootstrapAddr.
    */
   private boolean isRegistryBootStrapAddrInitialized = false;
   
   /**
    * Returns null if the Registery bootstrap is not valid (e.g., null, empty,
    * ...)
    * 
    * @return
    */
   public String getRegistryBootstrapAddr ( ) {
      if ( !this.isRegistryBootStrapAddrInitialized ) {
         if ( this.registryBootstrapAddr != null ) this.registryBootstrapAddr = this.registryBootstrapAddr.trim( );
         this.isRegistryBootStrapAddrInitialized = true;
      }
      return this.registryBootstrapAddr;
   }
   
   /**
    * @return Returns the storagePoolSize.
    */
   public int getStoragePoolSize ( ) {
      if ( this.storagePoolSize <= 0 ) this.storagePoolSize = DEFAULT_STORAGE_POOL_SIZE;
      return this.storagePoolSize;
   }
   
   public String toString ( ) {
      final StringBuilder builder = new StringBuilder( );
      builder.append( this.getClass( ).getName( ) ).append( " class [" ).append( "name=" ).append( this.webName ).append( "," );
      return builder.append( "]" ).toString( );
   }
   
   public void setRegistryBootstrapAddr ( String newValue ) {
      String oldValue = this.registryBootstrapAddr;
      this.registryBootstrapAddr = newValue;
      changeSupport.firePropertyChange( FIELD_NAME_registryBootstrapAddr , oldValue , newValue );
   }
   
   /****************************************************************************
    * UTILITY METHODS, Used by the GUI mainly.
    ***************************************************************************/
   
   private String                 directoryLoggingLevel;
   
   public static final String     FIELD_NAME_directoryLoggingLevel   = "directoryLoggingLevel";
   
   private long                   maxDirectoryLogSizeInMB;
   
   public static final String     FIELD_NAME_maxDirectoryLogSizeInMB = "maxDirectoryLogSizeInMB";
   
   private String                 gsnLoggingLevel;
   
   public static final String     FIELD_NAME_gsnLoggingLevel         = "gsnLoggingLevel";
   
   private long                   maxGSNLogSizeInMB;
   
   public static final String     FIELD_NAME_maxGSNLogSizeInMB       = "maxGSNLogSizeInMB";
   
   public static final String     FIELD_NAME_directoryPortNo         = "directoryPortNo";
   
   public static final int        DEFAULT_DIRECTORY_PORT             = 1882;
   
   private String                 directoryLogFileName;
   
   public static final String     FIELD_NAME_directoryLogFileName    = "directoryLogFileName";
   
   private String                 gsnLogFileName;
   
   public static final String     FIELD_NAME_gsnLogFileName          = "gsnLogFileName";
   
   private String                 dirLog4jFile;
   
   private String                 gsnLog4jFile;
   
   private String                 gsnConfigurationFileName;
   
   private Properties             dirLog4JProperties;
   
   private Properties             gsnLog4JProperties;
   
   public static final String     FIELD_NAME_directoryServiceHost    = "directoryServiceHost";
   
   public static final String [ ] LOGGING_LEVELS                     = { "DEBUG" , "INFO" , "WARN" , "ERROR" };
   
   public static String [ ]       NETWORK_ADDRESSES;
   
   public static final String [ ] JDBC_SYSTEMS                       = { "HSqlDB in Memory" , "HSqlDB in File" , "MySql" };
   
   public static final String [ ] JDBC_URLS                          = new String [ ] { "jdbc:hsqldb:mem:." , "jdbc:hsqldb:file:/path/to/file" , "jdbc:mysql://host:3306/dbName" };
   
   public static final String [ ] JDBC_DRIVERS                       = new String [ ] { "org.hsqldb.jdbcDriver" , "org.hsqldb.jdbcDriver" , "com.mysql.jdbc.Driver" };
   
   public static final String [ ] JDBC_URLS_PREFIX                   = new String [ ] { "jdbc:hsqldb:mem:" , "jdbc:hsqldb:file:" , "jdbc:mysql:" };
   
   public static final String     DEFAULT_LOGGING_LEVEL              = ContainerConfig.LOGGING_LEVELS[ 3 ];
   
   private String                 databaseSystem;
   
   public static final String     FIELD_NAME_databaseSystem          = "databaseSystem";
   
   /**
    * One Megabyte;
    */
   public static final long       DEFAULT_GSN_LOG_SIZE               = 1 * 1024 * 1024;
   
   private boolean                isdatabaseSystemInitialzied        = false;
   
   public void setDirectoryLoggingLevel ( String newValue ) {
      String oldValue = this.directoryLoggingLevel;
      this.directoryLoggingLevel = newValue;
      changeSupport.firePropertyChange( FIELD_NAME_directoryLoggingLevel , oldValue , newValue );
   }
   
   public String getDirectoryLoggingLevel ( ) {
      return this.directoryLoggingLevel;
   }
   
   public void setMaxDirectoryLogSizeInMB ( long newValue ) {
      long oldValue = this.maxDirectoryLogSizeInMB;
      this.maxDirectoryLogSizeInMB = newValue;
      changeSupport.firePropertyChange( FIELD_NAME_maxDirectoryLogSizeInMB , oldValue , newValue );
   }
   
   public long getMaxDirectoryLogSizeInMB ( ) {
      return this.maxDirectoryLogSizeInMB;
   }
   
   public void setGsnLoggingLevel ( String newValue ) {
      String oldValue = this.gsnLoggingLevel;
      this.gsnLoggingLevel = newValue;
      changeSupport.firePropertyChange( FIELD_NAME_gsnLoggingLevel , oldValue , newValue );
   }
   
   public String getGsnLoggingLevel ( ) {
      return this.gsnLoggingLevel;
   }
   
   public void setMaxGSNLogSizeInMB ( long newValue ) {
      long oldValue = this.maxGSNLogSizeInMB;
      this.maxGSNLogSizeInMB = newValue;
      changeSupport.firePropertyChange( FIELD_NAME_maxGSNLogSizeInMB , oldValue , newValue );
   }
   
   public long getMaxGSNLogSizeInMB ( ) {
      return this.maxGSNLogSizeInMB;
   }
   
   public void setDirectoryLogFileName ( String newValue ) {
      String oldValue = this.directoryLogFileName;
      this.directoryLogFileName = newValue;
      changeSupport.firePropertyChange( FIELD_NAME_directoryLogFileName , oldValue , newValue );
   }
   
   public String getDirectoryLogFileName ( ) {
      return this.directoryLogFileName;
   }
   
   public void setGsnLogFileName ( String newValue ) {
      String oldValue = this.gsnLogFileName;
      this.gsnLogFileName = newValue;
      changeSupport.firePropertyChange( FIELD_NAME_gsnLogFileName , oldValue , newValue );
   }
   
   public String getGsnLogFileName ( ) {
      return this.gsnLogFileName;
   }
   
   static {
      int i = 0;
      NETWORK_ADDRESSES = new String [ ValidityTools.NETWORK_LOCAL_ADDRESS.size( ) ];
      for ( String address : ValidityTools.NETWORK_LOCAL_ADDRESS )
         NETWORK_ADDRESSES[ i++ ] = address + ":" + DEFAULT_DIRECTORY_PORT;
   }
   
   private static String extractLoggingLevel ( String property , String [ ] setOfPossibleValues , String defaultValue ) {
      String toReturn = defaultValue;
      if ( property == null ) return toReturn;
      StringTokenizer st = new StringTokenizer( property , "," );
      if ( st == null || st.countTokens( ) == 0 ) return toReturn;
      String inputLogLevel = st.nextToken( );
      if ( inputLogLevel == null )
         return toReturn;
      else
         inputLogLevel = inputLogLevel.toUpperCase( ).trim( );
      for ( String level : setOfPossibleValues )
         if ( level.equals( inputLogLevel ) ) {
            toReturn = level;
            break;
         }
      return toReturn;
   }
   
   public static ContainerConfig getConfigurationFromFile ( String containerConfigurationFileName , String gsnLog4jFile , String dirLog4jFile ) throws JiBXException , FileNotFoundException {
      IBindingFactory bfact = BindingDirectory.getFactory( ContainerConfig.class );
      IUnmarshallingContext uctx = bfact.createUnmarshallingContext( );
      ContainerConfig toReturn = ( ContainerConfig ) uctx.unmarshalDocument( new FileInputStream( containerConfigurationFileName ) , null );
      
      Properties gsnLog4j = new Properties( );
      Properties dirLog4j = new Properties( );
      try {
         gsnLog4j.load( new FileInputStream( gsnLog4jFile ) );
         dirLog4j.load( new FileInputStream( dirLog4jFile ) );
      } catch ( IOException e ) {
         System.out.println( "Can't read the log4j files, please check the 2nd and 3rd parameters and try again." );
         e.printStackTrace( );
         System.exit( 1 );
      }
      toReturn.initLog4JProperties( gsnLog4j , dirLog4j );
      toReturn.setSourceFiles( containerConfigurationFileName , gsnLog4jFile , dirLog4jFile );
      return toReturn;
   }
   
   private void initLog4JProperties ( Properties gsnLog4j , Properties dirLog4j ) {
      this.gsnLog4JProperties = gsnLog4j;
      this.dirLog4JProperties = dirLog4j;
      setGsnLoggingLevel( extractLoggingLevel( gsnLog4j.getProperty( "log4j.rootLogger" ) , ContainerConfig.LOGGING_LEVELS , DEFAULT_LOGGING_LEVEL ) );
      setMaxGSNLogSizeInMB( OptionConverter.toFileSize( gsnLog4j.getProperty( "log4j.appender.file.MaxFileSize" ) , ContainerConfig.DEFAULT_GSN_LOG_SIZE ) / ( 1024 * 1024 ) );
      this.setDirectoryLoggingLevel( extractLoggingLevel( dirLog4j.getProperty( "log4j.rootLogger" ) , ContainerConfig.LOGGING_LEVELS , DEFAULT_LOGGING_LEVEL ) );
      this.setMaxDirectoryLogSizeInMB( OptionConverter.toFileSize( dirLog4j.getProperty( "log4j.appender.file.MaxFileSize" ) , ContainerConfig.DEFAULT_GSN_LOG_SIZE ) / ( 1024 * 1024 ) );
   }
   
   private void setSourceFiles ( String gsnConfigurationFileName , String gsnLog4jFile , String dirLog4jFile ) {
      this.gsnConfigurationFileName = gsnConfigurationFileName;
      this.gsnLog4jFile = gsnLog4jFile;
      this.dirLog4jFile = dirLog4jFile;
   }
   
   public void setdatabaseSystem ( String newValue ) {
      isdatabaseSystemInitialzied = true;
      String oldValue = this.databaseSystem;
      this.databaseSystem = newValue;
      setJdbcDriver( convertToDriver( newValue ) );
      if ( newValue == JDBC_SYSTEMS[ 0 ] ) {
         setJdbcPassword( "" );
         setJdbcUsername( "sa" );
         setJdbcURL( JDBC_URLS[ 0 ] );
      } else if ( newValue == JDBC_SYSTEMS[ 1 ] ) {
         setJdbcPassword( "" );
         setJdbcUsername( "sa" );
         setJdbcURL( JDBC_URLS[ 1 ] );
      } else if ( newValue == JDBC_SYSTEMS[ 2 ] ) {
         setJdbcURL( JDBC_URLS[ 2 ] );
      }
      changeSupport.firePropertyChange( FIELD_NAME_databaseSystem , oldValue , newValue );
   }
   
   public String getdatabaseSystem ( ) {
      if ( isdatabaseSystemInitialzied == false ) {
         isdatabaseSystemInitialzied = true;
         
         for ( int i = 0 ; i < JDBC_URLS_PREFIX.length ; i++ )
            if ( getJdbcURL( ).toLowerCase( ).trim( ).startsWith( JDBC_URLS_PREFIX[ i ] ) ) {
               setdatabaseSystem( JDBC_SYSTEMS[ i ] );
               break;
            }
      }
      return this.databaseSystem;
   }
   
   private String convertToDriver ( String dbSys ) {
      for ( int i = 0 ; i < JDBC_SYSTEMS.length ; i++ )
         if ( JDBC_SYSTEMS[ i ].equals( dbSys ) ) return JDBC_DRIVERS[ i ];
      return "";
   }
   
   public void writeConfigurations ( ) throws FileNotFoundException , IOException {
      gsnLog4JProperties.put( "log4j.rootLogger" , getGsnLoggingLevel( ) + ",file" );
      dirLog4JProperties.put( "log4j.rootLogger" , getDirectoryLoggingLevel( ) + ",file" );
      
      gsnLog4JProperties.put( "log4j.appender.file.MaxFileSize" , getMaxGSNLogSizeInMB( ) + "MB" );
      dirLog4JProperties.put( "log4j.appender.file.MaxFileSize" , getMaxDirectoryLogSizeInMB( ) + "MB" );
      
      StringTemplateGroup templateGroup = new StringTemplateGroup( "gsn" );
      StringTemplate st = templateGroup.getInstanceOf( "com/xoben/gsn/gui/templates/templateConf" );
      st.setAttribute( "name" , getWebName( ) );
      st.setAttribute( "author" , getWebAuthor( ) );
      st.setAttribute( "description" , getWebDescription( ) );
      st.setAttribute( "email" , getWebEmail( ) );
      st.setAttribute( "db_user" , getJdbcUsername( ) );
      st.setAttribute( "db_password" , getJdbcPassword( ) );
      st.setAttribute( "db_driver" , getJdbcDriver( ) );
      st.setAttribute( "db_url" , getJdbcURL( ) );
      st.setAttribute( "gsn_port" , getContainerPort( ) );
      st.setAttribute( "dir_socket" , getRegistryBootstrapAddr( ) );
      
      gsnLog4JProperties.store( new FileOutputStream( gsnLog4jFile ) , "" );
      dirLog4JProperties.store( new FileOutputStream( dirLog4jFile ) , "" );
      FileWriter writer = new FileWriter( gsnConfigurationFileName );
      writer.write( st.toString( ) );
      writer.close( );
      
   }
   
   public int extractDirectoryServicePort ( ) {
      String rawValue = getRegistryBootstrapAddr( );
      if ( rawValue == null || rawValue.trim( ).length( ) == 0 ) return -1;
      return ValidityTools.getPortNumber( rawValue );
   }
   
   public String extractDirectoryServiceHost ( ) {
      return extractDirectoryServiceHost( getRegistryBootstrapAddr( ) );
   }
   
   public static String extractDirectoryServiceHost ( String rawValue ) {
      return ValidityTools.getHostName( rawValue );
   }
   
   public static ContainerConfig getDefaultConfiguration ( ) {
      ContainerConfig bean = new ContainerConfig( );
      bean.setContainerPort( ContainerConfig.DEFAULT_GSN_PORT );
      bean.setRegistryBootstrapAddr( "localhost:1882" );
      bean.setJdbcDriver( ContainerConfig.JDBC_SYSTEMS[ 0 ] );
      bean.setJdbcPassword( "" );
      bean.setJdbcURL( "sa" );
      bean.setJdbcURL( ContainerConfig.JDBC_URLS[ 0 ] );
      bean.setWebName( "NoName." );
      bean.setWebAuthor( "Author not specified." );
      bean.setWebEmail( "Email not specified." );
      bean.setDirectoryLogFileName( "gsn-dir.log" );
      bean.setDirectoryLoggingLevel( LOGGING_LEVELS[ 3 ] );
      bean.setGsnLogFileName( "gsn.log" );
      bean.setGsnLoggingLevel( LOGGING_LEVELS[ 3 ] );
      bean.setMaxDirectoryLogSizeInMB( 1 );
      bean.setMaxGSNLogSizeInMB( 10 );
      return bean;
   }
   
}
