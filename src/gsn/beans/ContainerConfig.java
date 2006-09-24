package gsn.beans ;

import gsn.utils.ValidityTools ;

import java.beans.PropertyChangeEvent ;
import java.beans.PropertyChangeListener ;
import java.io.File ;
import java.util.StringTokenizer ;

import org.apache.commons.validator.GenericValidator ;
import org.apache.log4j.Logger ;

import com.jgoodies.binding.beans.ExtendedPropertyChangeSupport ;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public final class ContainerConfig {

   private ExtendedPropertyChangeSupport propertyChangeSupport = new ExtendedPropertyChangeSupport ( this ) ;

   private static final transient Logger logger = Logger.getLogger ( ContainerConfig.class ) ;

   public static final String NOT_PROVIDED = "Not Provided" ;

   private static final String AUTHOR = "author" ;

   private static final String PORT = "port" ;

   private static final String DESCRIPTION = "description" ;

   private static final String EMAIL = "email" ;

   private static final String NAME = "name" ;

   private static final String NOTIFICATION_EXTENTION_FILE = "notification-extention-file" ;

   private static final String VIRTUAL_SENSOR_DIR = "vsdir" ;

   private static final String BOOTSTRAP_ADDRESS = "Bootstrap-address" ;

   private static final String STORAGE_POOL_SIZE = "storage-pool-size" ;

   private static final String MAIL_SERVER = "mail-server" ;

   private static final String STORAGE_URL = "storage-url" ;

   private static final String SMS_SERVER = "sms-server" ;

   private static final String SMS_PASSWORD = "sms-password" ;

   private static final String WEB_APP_PATH = "web-app-path" ;

   private static final String WRAPPER_EXTENTION_PERPERTIES_FILE = "wrapper-extention-properties-file" ;

   private static final int DEFAULT_GSN_PORT = 22001 ;

   private String name ;

   private String author ;

   private String description ;

   private String email ;

   private String mailServer ;

   private String smsServer ;

   private String smsPassword ;

   private int containerPort = DEFAULT_GSN_PORT ;

   private String virtualSensorsDir ;

   private String registryBootstrapAddr ;

   private String containerFileName ;

   private boolean overwriteTables = Boolean.FALSE ;

   private String databaseDriver ;

   private String databaseUserName ;

   private String databasePassword ;

   private String storageURL ;

   private int storagePoolSize = 100 ;

   private String webAppPath ;

   private String wrapperExtentionsPropertiesFile ;

   private String notificationExtentionsPropertiesFile ;

   public boolean isOverwriteTables ( ) {
      return overwriteTables ;
   }

   public String getContainerFileName ( ) {
      return containerFileName ;
   }

   public void setContainerConfigurationFileName ( String containerFileName ) {
      this.containerFileName = containerFileName ;
   }

   public String getStorageURL ( ) {
      return storageURL ;
   }

   /**
    * @return Returns the author.
    */
   public String getAuthor ( ) {
      if ( this.author == null || this.author.trim ( ).equals ( "" ) )
         this.author = NOT_PROVIDED ;
      else
         this.author = this.author.trim ( ) ;
      return this.author ;

   }

   public String getDatabaseDriver ( ) {
      return databaseDriver ;
   }

   public String getDatabaseUserName ( ) {
      return databaseUserName ;
   }

   public String getDatabasePassword ( ) {
      return databasePassword ;
   }

   /**
    * @return Returns the containerPort.
    */
   public int getContainerPort ( ) {
      return containerPort ;
   }

   /**
    * @return Returns the description.
    */
   public String getDescription ( ) {
      if ( this.description == null || this.description.trim ( ).equals ( "" ) )
         this.description = NOT_PROVIDED ;
      return description.trim ( ) ;
   }

   /**
    * @return Returns the email.
    */
   public String getEmail ( ) {
      if ( this.email == null )
         this.email = NOT_PROVIDED ;
      return email ;
   }

   /**
    * @return Returns the name.
    */
   public String getName ( ) {
      if ( name == null || this.name.trim ( ).equals ( "" ) )
         name = NOT_PROVIDED ;
      name = name.trim ( ) ;
      return name ;
   }

   /**
    * @return Returns the notificationExtentionsPropertiesFile.
    */
   public String getNotificationExtentionsPropertiesFile ( ) {
      return notificationExtentionsPropertiesFile.trim ( ) ;
   }

   /**
    * @return Returns the pluginsDir.
    */
   private boolean isGetVirtualSensorsDir_Initialized = false;
   public String getVirtualSensorsDir ( ) {
      if ( this.virtualSensorsDir == null ) {
         logger.warn ( "The VirtualSensorDirectory shouldn't be NULL." );
         return "" ;
      }
      if (!this.isGetVirtualSensorsDir_Initialized) {
         this.virtualSensorsDir=virtualSensorsDir.trim ( ) ;
         isGetVirtualSensorsDir_Initialized=true;
      }
      return this.virtualSensorsDir;
      
   }

   /**
    * @return Returns the registryBootstrapAddr.
    */
   public String getRegistryBootstrapAddr ( ) {
      if ( registryBootstrapAddr == null )
         return "" ;
      return registryBootstrapAddr.trim ( ) ;
   }

   /**
    * @return Returns the storagePoolSize.
    */
   public int getStoragePoolSize ( ) {
      return storagePoolSize ;
   }

   /**
    * @return Returns the webAppPath.
    */
   public String getWebAppPath ( ) {
      if ( this.webAppPath == null )
         return "" ;
      return webAppPath.trim ( ) ;
   }

   /**
    * @return Returns the wrapperExtentionsPropertiesFile.
    */
   public String getWrapperExtentionsPropertiesFile ( ) {
      if ( this.wrapperExtentionsPropertiesFile == null )
         return "" ;
      return wrapperExtentionsPropertiesFile.trim ( ) ;
   }

   /**
    * @param newAuthor
    *           The author to set.
    */
   public void setAuthor ( String newAuthor ) {
      String oldAuthor = this.author ;
      this.author = newAuthor ;
      propertyChangeSupport.firePropertyChange ( AUTHOR , oldAuthor , newAuthor ) ;
   }

   /**
    * @param newContainerPort
    *           The containerPort to set.
    */
   public void setContainerPort ( int newContainerPort ) {
      int oldPort = this.containerPort ;
      this.containerPort = newContainerPort ;
      propertyChangeSupport.firePropertyChange ( PORT , oldPort , newContainerPort ) ;
   }

   /**
    * @param newDescription
    *           The description to set.
    */
   public void setDescription ( String newDescription ) {
      String oldDescription = this.description ;
      this.description = newDescription ;
      propertyChangeSupport.firePropertyChange ( DESCRIPTION , oldDescription , newDescription ) ;
   }

   /**
    * @param newEmail
    *           The email to set.
    */
   public void setEmail ( String newEmail ) {
      String oldEmail = this.email ;
      this.email = newEmail ;
      propertyChangeSupport.firePropertyChange ( EMAIL , oldEmail , newEmail ) ;
   }

   /**
    * @param newName
    *           The name to set.
    */
   public void setName ( String newName ) {
      String oldName = this.name ;
      this.name = newName ;
      propertyChangeSupport.firePropertyChange ( NAME , oldName , newName ) ;
   }

   /**
    * @param newNotificationExtentionsPropertiesFile
    *           The notificationExtentionsPropertiesFile to set.
    */
   public void setNotificationExtentionsPropertiesFile ( String newNotificationExtentionsPropertiesFile ) {
      String oldValue = this.notificationExtentionsPropertiesFile ;
      this.notificationExtentionsPropertiesFile = newNotificationExtentionsPropertiesFile ;
      propertyChangeSupport.firePropertyChange ( NOTIFICATION_EXTENTION_FILE , oldValue , newNotificationExtentionsPropertiesFile ) ;
   }

   /**
    * @param newPluginsDir
    *           The pluginsDir to set.
    */
   public void setVirtualSensorsDir ( String newPluginsDir ) {
      String oldValue = this.virtualSensorsDir ;
      this.virtualSensorsDir = newPluginsDir ;
      propertyChangeSupport.firePropertyChange ( VIRTUAL_SENSOR_DIR , oldValue , newPluginsDir ) ;
   }

   /**
    * @param newRegistryBootstrapAddr
    *           The registryBootstrapAddr to set.
    */
   public void setRegistryBootstrapAddr ( String newRegistryBootstrapAddr ) {
      String oldValue = this.registryBootstrapAddr ;
      this.registryBootstrapAddr = newRegistryBootstrapAddr ;
      propertyChangeSupport.firePropertyChange ( BOOTSTRAP_ADDRESS , oldValue , newRegistryBootstrapAddr ) ;
   }

   /**
    * @param storagePoolSize
    *           The storagePoolSize to set.
    */
   public void setStoragePoolSize ( int storagePoolSize ) {
      int oldValue = this.storagePoolSize ;
      this.storagePoolSize = storagePoolSize ;
      propertyChangeSupport.firePropertyChange ( STORAGE_POOL_SIZE , oldValue , storagePoolSize ) ;

   }

   /**
    * @param webAppPath
    *           The webAppPath to set.
    */
   public void setWebAppPath ( String webAppPath ) {
      String oldWebAppPath = this.webAppPath ;
      this.webAppPath = webAppPath ;
      propertyChangeSupport.firePropertyChange ( WEB_APP_PATH , oldWebAppPath , webAppPath ) ;
   }

   /**
    * @param wrapperExtentionsPropertiesFile
    *           The wrapperExtentionsPropertiesFile to set.
    */
   public void setWrapperExtentionsPropertiesFile ( String wrapperExtentionsPropertiesFile ) {
      String oldValue = this.wrapperExtentionsPropertiesFile ;
      this.wrapperExtentionsPropertiesFile = wrapperExtentionsPropertiesFile ;
      propertyChangeSupport.firePropertyChange ( WRAPPER_EXTENTION_PERPERTIES_FILE , oldValue , wrapperExtentionsPropertiesFile ) ;
   }

   public boolean isValied ( ) {
      File file = new File ( getVirtualSensorsDir ( ) ) ;
      if ( ! file.exists ( ) || ! file.isDirectory ( ) ) {
         logger.error ( " The path in the <virtual-sensors-dir> at :" + getContainerFileName ( ) + " is not valid." ) ;
         return false ;
      }
      file = new File ( getWebAppPath ( ) ) ;
      if ( ! file.exists ( ) || ! file.isDirectory ( ) ) {
         logger.error ( " The path in the <webapp-location> at :" + getContainerFileName ( ) + " is not valid." ) ;
         return false ;
      }
      file = new File ( getWrapperExtentionsPropertiesFile ( ) ) ;
      if ( ! file.exists ( ) || ! file.isFile ( ) ) {
         logger.error ( " The path in the <wrapper-extentions> at :" + getContainerFileName ( ) + " is not valid." ) ;
         return false ;
      }
      if ( getMailServer ( ) != null && ! ValidityTools.isAccessibleSocket ( getMailServer ( ) , getPort ( getMailServer ( ) , ValidityTools.SMTP_PORT ) ) )
         return false ;
      if ( getSmsServer ( ) != null && ! ValidityTools.isAccessibleSocket ( getSmsServer ( ) , getPort ( getSmsServer ( ) , ValidityTools.SMTP_PORT ) ) )
         return false ;
      return true ;
   }

   private int getPort ( String emailServer , int default_port ) {
      if ( emailServer == null || emailServer.length ( ) < 3 ) {
         logger.warn ( "can't understand the value provided for the email server" ) ;
         return default_port ;
      }
      StringTokenizer stringTokenizer = new StringTokenizer ( emailServer , ":" ) ;

      stringTokenizer.nextToken ( ) ;// passing the hostname
      if ( stringTokenizer.hasMoreTokens ( ) )
         try {
            return Integer.parseInt ( stringTokenizer.nextToken ( ) ) ;
         } catch ( Exception e ) {
            logger.warn ( e.getMessage ( ) ) ;
            logger.debug ( "can't convert the port number to the integer." , e ) ;
         }
      return default_port ;

   }

   /**
    * @return Returns the smsPassword.
    */
   public String getSmsPassword ( ) {
      return smsPassword ;
   }

   /**
    * @param newSmsPassword
    *           The smsPassword to set.
    */
   public void setSmsPassword ( String newSmsPassword ) {
      String oldValue = this.smsPassword ;
      this.smsPassword = newSmsPassword ;
      propertyChangeSupport.firePropertyChange ( SMS_PASSWORD , oldValue , newSmsPassword ) ;
   }

   /**
    * @return Returns the smsServer.
    */
   public String getSmsServer ( ) {
      return smsServer ;
   }

   /**
    * @param newSmsServer
    *           The smsServer to set.
    */
   public void setSmsServer ( String newSmsServer ) {
      String oldValue = this.smsServer ;
      this.smsServer = newSmsServer ;
      propertyChangeSupport.firePropertyChange ( SMS_SERVER , oldValue , newSmsServer ) ;
   }

   /**
    * @param newStorageURL
    *           The storageURL to set.
    */
   public void setStorageURL ( String newStorageURL ) {
      String oldValue = this.storageURL ;
      this.storageURL = newStorageURL ;
      propertyChangeSupport.firePropertyChange ( STORAGE_URL , oldValue , this.storageURL ) ;
   }

   /**
    * @return Returns the mailServer.
    */
   public String getMailServer ( ) {
      return mailServer ;
   }

   /**
    * @param mailServer
    *           The mailServer to set.
    */
   public void setMailServer ( String mailServer ) {
      String oldValue = this.mailServer ;
      this.mailServer = mailServer ;
      propertyChangeSupport.firePropertyChange ( MAIL_SERVER , oldValue , mailServer ) ;
   }

   public void addPropertyChangeListener ( PropertyChangeListener changeListener ) {
      propertyChangeSupport.addPropertyChangeListener ( changeListener ) ;
   }

   public void removePropertyChangeListener ( PropertyChangeListener listener ) {
      propertyChangeSupport.removePropertyChangeListener ( listener ) ;
   }

   public String toString ( ) {
      StringBuilder builder = new StringBuilder ( ) ;
      builder.append ( this.getClass ( ).getName ( ) ).append ( " class [" ).append ( "name=" ).append ( name ).append ( "," ) ;
      return builder.append ( "]" ).toString ( ) ;
   }

}
