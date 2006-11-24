package gsn;

import gsn.beans.AddressBean;
import gsn.beans.InputStream;
import gsn.beans.Modifications;
import gsn.beans.StreamSource;
import gsn.beans.VSensorConfig;
import gsn.shared.Registry;
import gsn.shared.VirtualSensorIdentityBean;
import gsn.storage.PoolIsFullException;
import gsn.storage.StorageManager;
import gsn.utils.CaseInsensitiveComparator;
import gsn.utils.TCPConnPool;
import gsn.wrappers.Wrapper;
import gsn.wrappers.DataListener;
import gsn.wrappers.TableSizeEnforce;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.commons.collections.KeyValue;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class VSensorLoader extends Thread {
   
   public static final String                                     VSENSOR_POOL                        = "VSENSOR-POOL";
   
   public static final String                                     STORAGE_MANAGER                     = "STORAGE-MANAGER";
   
   public static final String                                     STREAM_SOURCE                       = "STREAM-SOURCE";
   
   public static final String                                     INPUT_STREAM                        = "INPUT-STREAM";
   
   private static transient Logger                                logger                              = Logger.getLogger( VSensorLoader.class );
   
   /**
    * Mapping between the AddressBean and DataSources
    */
   private static final HashMap < AddressBean , Wrapper > activeDataSources                   = new HashMap < AddressBean ,  Wrapper >( );
   
   private StorageManager                                         storageManager                      = StorageManager.getInstance( );
   
   private String                                                 pluginsDir;
   
   private boolean                                                isActive                              = true;
   
   private static int                                             VSENSOR_LOADER_THREAD_COUNTER       = 0;
   
   private static int                                             DIRECTORY_REFRESHING_THREAD_COUNTER = 0;
   
   private Thread                                                 directoryRefreshingThread;
   
   public VSensorLoader ( String pluginsDir ) {
      this.pluginsDir = pluginsDir;
      Thread thread = new Thread( this );
      thread.setName( "VSensorLoader-Thread" + VSENSOR_LOADER_THREAD_COUNTER++ );
      thread.start( );
      this.directoryRefreshingThread = new TheDirectoryRefreshing( );
      this.directoryRefreshingThread.start( );
   }
   
   public void run ( ) {
      if ( storageManager == null ) {
         logger.fatal( "The Storage Manager shouldn't be null, possible a BUG." );
         return;
      }
      
      while ( isActive ) {
         try {
            loadPlugin( );
         } catch ( Exception e ) {
            logger.error( e.getMessage( ) , e );
         }
      }
   }
   
   public void loadPlugin ( ) throws SQLException , JiBXException {
      Modifications modifications = getUpdateStatus( pluginsDir );
      Collection < String > removeIt = modifications.getRemove( );
      Collection < String > addIt = modifications.getAdd( );
      IBindingFactory bfact;
      IUnmarshallingContext uctx;
      VSensorConfig configuration;
      for ( String configFile : removeIt ) {
         String name = Mappings.getConfigurationObject( configFile ).getVirtualSensorName( );
         logger.warn( new StringBuilder( ).append( "removing : " ).append( name ).toString( ) );
         VirtualSensorPool sensorInstance = Mappings.getVSensorInstanceByFileName( configFile );
         this.toDirectoryService( sensorInstance.getConfig( ) , Registry.DEREGISTER );
         Mappings.removeFilename( configFile );
         this.removeAllResources( sensorInstance );
      }
      try {
         Thread.sleep( 3000 );
      } catch ( InterruptedException e ) {
         logger.error( e.getMessage( ) , e );
         if ( this.isActive == false ) return;
      }
      configFilesLoop : for ( String configFile : addIt ) {
         if ( this.isActive == false ) return;
         bfact = BindingDirectory.getFactory( VSensorConfig.class );
         uctx = bfact.createUnmarshallingContext( );
         try {
            configuration = ( VSensorConfig ) uctx.unmarshalDocument( new FileInputStream( configFile ) , null );
         } catch ( JiBXException e ) {
            logger.error( e.getMessage( ) , e );
            logger.error( new StringBuilder( ).append( "Adding the virtual sensor specified in " ).append( configFile ).append(
               " failed because there is syntax error in the configuration file. Please check the configuration file and try again." ).toString( ) );
            continue configFilesLoop;
         } catch ( FileNotFoundException e ) {
            logger.error( e.getMessage( ) , e );
            logger.error( new StringBuilder( ).append( "Adding the virtual sensor specified in " ).append( configFile ).append( " failed because the configuratio of I/O problems." ).toString( ) );
            continue configFilesLoop;
         }
         configuration.setFileName( configFile );
         if ( !configuration.validate( ) ) {
            logger.error( new StringBuilder( ).append( "Adding the virtual sensor specified in " ).append( configFile ).append( " failed because of one or more problems in configuration file." )
                  .toString( ) );
            logger.error( new StringBuilder( ).append( "Please check the file and try again" ).toString( ) );
            continue configFilesLoop;
         }
         for ( InputStream is : configuration.getInputStreams( ) ) {
            if ( !is.validate( ) ) {
               logger.error( new StringBuilder( ).append( "Adding the virtual sensor specified in " ).append( configFile ).append( " failed because of one or more problems in configuration file." )
                     .toString( ) );
               logger.error( new StringBuilder( ).append( "Please check the file and try again" ).toString( ) );
               continue configFilesLoop;
            }
         }
         String vsName = configuration.getVirtualSensorName( );
         if ( Mappings.getVSensorConfig( vsName ) != null ) {
            logger.error( new StringBuilder( ).append( "Adding the virtual sensor specified in " ).append( configFile ).append( " failed because the virtual sensor name used by " )
                  .append( configFile ).append( " is already used by : " ).append( Mappings.getVSensorConfig( vsName ).getFileName( ) ).toString( ) );
            logger.error( "Note that the virtual sensor name is case insensitive and all the spaces in it's name will be removed automatically." );
            continue;
         }
         if ( !StringUtils.isAlphanumericSpace( vsName ) ) {
            logger.error( new StringBuilder( ).append( "Adding the virtual sensor specified in " ).append( configFile ).append(
               " failed because the virtual sensor name is not following the requirements : " ).toString( ) );
            logger.error( "The virtual sensor name is case insensitive and all the spaces in it's name will be removed automatically." );
            logger.error( "That the name of the virutal sensor should starting by alphabetical character and they can contain numerical characters afterwards." );
            continue;
         }
         VirtualSensorPool pool = new VirtualSensorPool( configuration );
         if ( this.createInputStreams( configuration , pool ) == false ) {
            logger.error( "loading the >" + vsName + "< virtual sensor is stoped due to error(s) in preparing the input streams." );
            continue;
         }
         try {
            storageManager.createTable( configuration.getVirtualSensorName( ) , configuration.getOutputStructure( ) );
         } catch ( SQLException e ) {
            if ( e.getMessage( ).toLowerCase( ).contains( "table already exists" ) ) {
               logger.error( e.getMessage( ) );
               if ( logger.isInfoEnabled( ) ) logger.info( e.getMessage( ) , e );
               logger.error( new StringBuilder( ).append( "Loading the virtual sensor specified in the file : " ).append( configuration.getFileName( ) ).append( " failed" ).toString( ) );
               logger.error( new StringBuilder( ).append( "The table : " ).append( configuration.getVirtualSensorName( ) ).append( " is exists in the database specified in :" ).append(
                  Main.getContainerConfig( ).getContainerFileName( ) ).append( "." ).toString( ) );
               logger.error( "Solutions : " );
               logger.error( new StringBuilder( ).append( "1. Change the virtual sensor name, in the : " ).append( configuration.getFileName( ) ).toString( ) );
               logger.error( new StringBuilder( ).append( "2. Change the URL of the database in " ).append( Main.getContainerConfig( ).getContainerFileName( ) ).append(
                  " and choose another database." ).toString( ) );
               logger.error( new StringBuilder( ).append( "3. Rename/Move the table with the name : " ).append( Main.getContainerConfig( ).getContainerFileName( ) ).append( " in the database." )
                     .toString( ) );
               logger.error( new StringBuilder( ).append( "4. Change the overwrite-tables=\"true\" (be careful, this will overwrite all the data previously saved in " ).append(
                  configuration.getVirtualSensorName( ) ).append( " table )" ).toString( ) );
            } else {
               logger.error( e.getMessage( ) , e );
            }
            continue;
         }
         logger.warn( new StringBuilder( "adding : " ).append( vsName ).append( " virtual sensor[" ).append( configFile ).append( "]" ).toString( ) );
         Mappings.addVSensorInstance( pool );
         try {
            pool.start( );
         } catch ( PoolIsFullException e1 ) {
            logger.error( "Creating the virtual sensor >" + configuration.getVirtualSensorName( ) + "< failed." , e1 );
            continue;
         } catch ( VirtualSensorInitializationFailedException e1 ) {
            logger.error( "Creating the virtual sensor >" + configuration.getVirtualSensorName( ) + "< failed." , e1 );
            continue;
         }
         this.toDirectoryService( configuration , Registry.REGISTER );
      }
   }
   
   /**
    * This is used for register/deregistering a virtual sensor to the container.
    */
   private transient HashMap < VSensorConfig , PostMethod > directoryCommunicationCache         = new HashMap < VSensorConfig , PostMethod >( );
   
   private static int                                       TABLE_SIZE_ENFORCING_THREAD_COUNTER = 0;
   
   private static final ArrayList < VSensorConfig >         EMPTY_ARRAYLIST                     = new ArrayList < VSensorConfig >( );
   
   private synchronized void toDirectoryService ( VSensorConfig configuration , int action ) {
      PostMethod postMethod = this.directoryCommunicationCache.get( configuration );
      if ( Main.getContainerConfig( ).getRegistryBootstrapAddr( ) == null ) {
         logger.warn( "Can't contact the directory service, please fix the GSN Container Configuration File." );
         return;
      }
      if ( postMethod == null ) {
         String dirHost = Main.getContainerConfig( ).getRegistryBootstrapAddr( );
         if ( dirHost.indexOf( "http://" ) < 0 ) dirHost = "http://" + dirHost;
         postMethod = new PostMethod( dirHost + "/registry" );
         this.fillParameters( postMethod , configuration , action );
         this.directoryCommunicationCache.put( configuration , postMethod );
      }
      if ( action == Registry.DEREGISTER ) {
         postMethod.setRequestHeader( Registry.REQUEST , Integer.toString( Registry.DEREGISTER ) );
         this.directoryCommunicationCache.remove( configuration );
      }
      
      int statusCode = -1;
      statusCode = TCPConnPool.executeMethod( postMethod , false );
      if ( statusCode == -1 ) {
         
         logger.warn( new StringBuilder( ).append( "Can't connect to the directory serivce at *" ).append( Main.getContainerConfig( ).getRegistryBootstrapAddr( ) ).append(
            "* for register/deregistering requests." ).toString( ) );
         logger.warn( "Make sure that the directory service is running." );
         logger.warn( "For running the directory service, in a separate terminal run" );
         logger.warn( "ant dir" );
         logger.warn( new StringBuilder( ).append( "Make sure in the : " ).append( Main.getContainerConfig( ).getContainerFileName( ) ).append(
            " file, contents of the <directory-address> element reflects the address of the machine in which the above command is executed." ).toString( ) );
      }
      postMethod.releaseConnection( );
   }
   
   class TheDirectoryRefreshing extends Thread {
      
      private boolean toStop   = false;
      
      final int       INTERVAL = 5 * 60 * 1000; // each 5 mins
                                                
      public TheDirectoryRefreshing ( ) {
         this.setName( "Directory-RefereshingThread" + DIRECTORY_REFRESHING_THREAD_COUNTER++ );
      }
      
      public void run ( ) {
         while ( !this.toStop ) {
            try {
               Iterator < VSensorConfig > keys = Mappings.getAllVSensorConfigs( );
               while ( keys.hasNext( ) && !this.toStop )
                  VSensorLoader.this.toDirectoryService( keys.next( ) , Registry.REGISTER );
            } catch ( ConcurrentModificationException e ) {
               continue;
            } finally {
               try {
                  Thread.sleep( this.INTERVAL );
               } catch ( InterruptedException e ) {
                  if ( this.toStop == true ) break;
                  logger.error( e.getMessage( ) , e );
               }
            }
         }
      }
      
      public void stopPlease ( ) {
         this.toStop = true;
         this.interrupt( );
      }
   }
   
   private ArrayList < VirtualSensorIdentityBean > resolveByDirecotryService ( ArrayList < ? extends KeyValue > predicates ) {
      ArrayList < VirtualSensorIdentityBean > result = new ArrayList < VirtualSensorIdentityBean >( );
      
      if ( Main.getContainerConfig( ).getRegistryBootstrapAddr( ) == null ) {
         logger.warn( "Can't contact the directory service, please fix the GSN Container Configuration File." );
         return result;
      }
      String dirHost = Main.getContainerConfig( ).getRegistryBootstrapAddr( );
      if ( dirHost.indexOf( "http://" ) < 0 ) dirHost = "http://" + dirHost;
      PostMethod postMethod = new PostMethod( dirHost + "/registry" );
      postMethod.setRequestHeader( Registry.REQUEST , Integer.toString( Registry.QUERY ) );
      for ( KeyValue predicate : predicates ) {
         postMethod.addRequestHeader( Registry.VS_PREDICATES_KEYS , ( String ) predicate.getKey( ) );
         postMethod.addRequestHeader( Registry.VS_PREDICATES_VALUES , ( String ) predicate.getValue( ) );
      }
      postMethod.setFollowRedirects( false );
      int statusCode = TCPConnPool.executeMethod( postMethod , false );
      if ( statusCode == -1 ) {
         logger.error( "Can't connect to the directory serivce !!!" );
      }
      Header [ ] hosts = postMethod.getResponseHeaders( Registry.VS_HOST );
      Header [ ] ports = postMethod.getResponseHeaders( Registry.VS_PORT );
      Header [ ] names = postMethod.getResponseHeaders( Registry.VS_NAME );
      
      for ( int i = 0 ; i < hosts.length ; i++ ) {
         result.add( new VirtualSensorIdentityBean( names[ i ].getValue( ) , hosts[ i ].getValue( ) , Integer.parseInt( ports[ i ].getValue( ) ) ) );
         if ( logger.isInfoEnabled( ) ) logger.info( "RESOLVE RESULT : " + names[ i ].getValue( ) );
      }
      postMethod.releaseConnection( );
      return result;
   }
   
   private void fillParameters ( PostMethod postMethod , VSensorConfig configuration , int action ) {
      postMethod.addRequestHeader( Registry.REQUEST , Integer.toString( action ) );
      postMethod.addRequestHeader( Registry.VS_NAME , configuration.getVirtualSensorName( ) );
      postMethod.addRequestHeader( Registry.VS_PORT , Integer.toString( Main.getContainerConfig( ).getContainerPort( ) ) );
      for ( KeyValue predicate : configuration.getAddressing( ) ) {
         postMethod.addRequestHeader( Registry.VS_PREDICATES_KEYS , ( String ) predicate.getKey( ) );
         postMethod.addRequestHeader( Registry.VS_PREDICATES_VALUES , ( String ) predicate.getValue( ) );
      }
   }
   
   private void removeAllResources ( VirtualSensorPool pool ) {
      VSensorConfig config = pool.getConfig( );
      pool.closePool( );
      final String vsensorName = config.getVirtualSensorName( );
      if ( logger.isInfoEnabled( ) ) logger.info( new StringBuilder( ).append( "Releasing previously used resources used by [" ).append( vsensorName ).append( "]." ).toString( ) );
      for ( InputStream inputStream : config.getInputStreams( ) ) {
         for ( StreamSource streamSource : inputStream.getSources( ) ) {
            final DataListener activeDataListener = streamSource.getActiveDataListener( );
            final Wrapper activeDataSource = streamSource.getActiveSourceProducer( );
            activeDataSource.removeListener( activeDataListener );
            if ( activeDataSource.getListenersSize( ) == 0 ) {
               final AddressBean activeDataSourceAddressBean = activeDataSource.getActiveAddressBean( );
               activeDataSources.remove( activeDataSourceAddressBean );
               Mappings.getContainer( ).removeRemoteStreamSource( activeDataSource.getDBAlias( ) );
               activeDataSource.finalize(  );
               activeDataSource.releaseResources();
            }
         }
         inputStream.finalize( );
      }
      // storageManager.renameTable(vsensorName,vsensorName+"Before"+System.currentTimeMillis());
      Mappings.getContainer( ).removeAllResourcesAssociatedWithVSName( vsensorName );
      this.storageManager.dropTable( config.getVirtualSensorName( ) );
   }
   
   public static Modifications getUpdateStatus ( String virtualSensorsPath ) {
      TreeSet < String > remove = new TreeSet < String >( new Comparator( ) {
         
         public int compare ( Object o1 , Object o2 ) {
            String input1 = o1.toString( ).trim( );
            String input2 = o2.toString( ).trim( );
            return input1.compareTo( input2 );
         }
      } );
      TreeSet < String > add = new TreeSet < String >( new Comparator( ) {
         
         public int compare ( Object o1 , Object o2 ) {
            String input1 = o1.toString( ).trim( );
            String input2 = o2.toString( ).trim( );
            return input1.compareTo( input2 );
         }
      } );
      String [ ] previous = Mappings.getAllKnownFileName( );
      FileFilter filter = new FileFilter( ) {
         
         public boolean accept ( File file ) {
            if ( !file.isDirectory( ) && file.getName( ).endsWith( ".xml" ) && !file.getName( ).startsWith( "." ) ) return true;
            return false;
         }
      };
      File files[] = new File( virtualSensorsPath ).listFiles( filter );
      // --- preparing the remove list
      // Removing those in the previous which are not existing the new files
      // or modified.
      main : for ( String pre : previous ) {
         for ( File curr : files )
            if ( pre.equals( curr.getAbsolutePath( ) ) && ( Mappings.getLastModifiedTime( pre ) == curr.lastModified( ) ) ) continue main;
         remove.add( pre );
      }
      // ---adding the new files to the Add List a new file should added if
      //
      // 1. it's just deployed.
      // 2. it's modification time changed.
      
      main : for ( File cur : files ) {
         for ( String pre : previous )
            if ( cur.getAbsolutePath( ).equals( pre ) && ( cur.lastModified( ) == Mappings.getLastModifiedTime( pre ) ) ) continue main;
         add.add( cur.getAbsolutePath( ) );
      }
      
      Modifications result = new Modifications( add , remove );
      return result;
   }
   
   /**
    * The properties file contains information on wrappers for stream sources.
    * FIXME : The body of CreateInputStreams is incomplete b/c in the case of an
    * error it should remove the resources.
    */
   public boolean createInputStreams ( VSensorConfig vsensor , VirtualSensorPool pool ) {
      if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( ).append( "Preparing input streams for: " ).append( vsensor.getVirtualSensorName( ) ).toString( ) );
      if ( vsensor.getInputStreams( ).size( ) == 0 ) logger.warn( new StringBuilder( "There is no input streams defined for *" ).append( vsensor.getVirtualSensorName( ) ).append( "*" ).toString( ) );
      for ( Iterator < InputStream > inputStreamIterator = vsensor.getInputStreams( ).iterator( ) ; inputStreamIterator.hasNext( ) ; ) {
         InputStream inputStream = inputStreamIterator.next( );
         HashMap inputStreamContext = new HashMap( );
         inputStreamContext.put( STORAGE_MANAGER , storageManager );
         for ( Iterator < StreamSource > dataSouce = inputStream.getSources( ).iterator( ) ; dataSouce.hasNext( ) ; ) {
            if ( prepareStreamSource( inputStream , dataSouce.next( ) , vsensor ) == false ) return false;
         }
         if ( inputStream.initialize( inputStreamContext ) == false ) return false;
         inputStream.setPool( pool );
      }
      return true;
   }
   
   private boolean prepareStreamSource ( InputStream inputStream , StreamSource streamSource , VSensorConfig vsensor ) {
      HashMap < String , String > rewritingMapping = new HashMap < String , String >( );
      TreeMap < String , Object > context = new TreeMap < String , Object >( new CaseInsensitiveComparator( ) );
      for ( AddressBean addressBean : streamSource.getAddressing( ) ) {
         context.put( Container.STREAM_SOURCE_ACTIVE_ADDRESS_BEAN , addressBean );
         Wrapper ds = activeDataSources.get( addressBean );
         if ( ds == null ) {
            if ( !addressBean.isAbsoluteAddressSpecified( ) ) {// Dynamic-address
               ArrayList < VirtualSensorIdentityBean > resolved = this.resolveByDirecotryService( addressBean.getPredicates( ) );
               if ( resolved.size( ) == 0 ) {
                  logger.warn( new StringBuilder( ).append( "Resolving Dynamic Address for Stream-Source:" ).append( streamSource.getAlias( ) ).append( " with addressing " ).append( addressBean )
                        .append( " FAILED." ).toString( ) );
                  continue;
               }
               if ( logger.isInfoEnabled( ) )
                  logger.info( new StringBuilder( ).append( "Resolving Address for Stream-Source:" ).append( streamSource.getAlias( ) ).append( " with addressing " ).append( addressBean ).append(
                     " SUCCEED with " ).append( resolved.size( ) ).append( " candidate(s)." ).toString( ) );
               /**
                * TODO : Currently In here I'm using just first result returned
                * from directory service and ignoring the rest.
                */
               context.put( Registry.VS_HOST , resolved.get( 0 ).getRemoteAddress( ) );
               context.put( Registry.VS_PORT , Integer.toString( resolved.get( 0 ).getRemotePort( ) ) );
               context.put( Registry.VS_NAME , resolved.get( 0 ).getVSName( ) );
               context.put( Container.QUERY_VS_NAME , resolved.get( 0 ).getVSName( ) );
               
            } else if ( addressBean.isAbsoluteAddressSpecified( ) ) { // Absolute-address
               context.put( Registry.VS_HOST , addressBean.getPredicateValue( Registry.VS_HOST ) );
               context.put( Registry.VS_PORT , addressBean.getPredicateValue( Registry.VS_PORT ) );
               context.put( Registry.VS_NAME , addressBean.getPredicateValue( Registry.VS_NAME ) );
            }
            if ( Main.getWrapperClass( addressBean.getWrapper( ) ) == null ) {
               logger.error( "The wrapper >" + addressBean.getWrapper( ) + "< is not defined in the >" + Main.DEFAULT_WRAPPER_PROPERTIES_FILE + "< file." );
               continue;
            }
            try {
               ds = ( Wrapper ) Main.getWrapperClass( addressBean.getWrapper( ) ).newInstance( );
               ds.setActiveAddressBean( addressBean );
               boolean initializationResult = ds.initialize(  );
               if ( initializationResult == false )
                  continue;// This address
               // is not working, goto the next address.
               else {
                  if ( ds.getOutputFormat( ) == null ) {
                     logger.warn( "The output format of the " + ds.getClass( ).getName( ) + " is null !!!" );
                     logger.warn( "The initialization of the wrapper is failed." );
                     continue;
                  }
                  try {
                     storageManager.createTable( ds.getDBAlias( ) , ds.getOutputFormat( ) );
                     TableSizeEnforce tsf = new TableSizeEnforce( ds );
                     ds.setTableSizeEnforce( tsf );
                     Thread tableSizeEnforcingThread = new Thread( tsf );
                     tableSizeEnforcingThread.setName( "TableSizeEnforceing-WRAPPER-Thread" + TABLE_SIZE_ENFORCING_THREAD_COUNTER++ );
                     tableSizeEnforcingThread.start( );
                  } catch ( SQLException e ) {
                     logger.error( e.getMessage( ) , e );
                     continue;
                  }
                  ds.start( );
               }
            } catch ( InstantiationException e ) {
               logger.error( e.getMessage( ) , e );
            } catch ( IllegalAccessException e ) {
               logger.error( e.getMessage( ) , e );
            }
         }
         DataListener dbDataListener = new DataListener(inputStream,streamSource );
         String viewName = ds.addListener( dbDataListener );
         rewritingMapping.put( streamSource.getAlias( ) , viewName );
         streamSource.setUsedDataSource( ds , dbDataListener );
         activeDataSources.put( addressBean , ds );
         break;
      }
      if ( rewritingMapping.isEmpty( ) ) {
         logger.error( new StringBuilder( ).append( "Can't prepate the data source: \"" ).append( streamSource.getAlias( ) ).append( "\" for inputStream: \"" ).append(
            inputStream.getInputStreamName( ) ).append( "\" for Virtual Sensor: \"" ).append( vsensor.getVirtualSensorName( ) ).append( "\"" ).toString( ) );
         return false;
      }
      return true;
   }
   
   public void stopLoading ( ) {
      this.isActive = false;
      this.interrupt( );
      for ( String configFile : Mappings.getAllKnownFileName( ) ) {
         VirtualSensorPool sensorInstance = Mappings.getVSensorInstanceByFileName( configFile );
         removeAllResources( sensorInstance );
         logger.warn( "Removing the resources associated with : " + sensorInstance.getConfig( ).getFileName( ) + " [done]." );
      }
      try {
         this.storageManager.shutdown( );
      } catch ( SQLException e ) {
         e.printStackTrace( );
      }
   }
}
