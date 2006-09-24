package gsn.beans ;

import gsn.utils.CaseInsensitiveComparator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;

import org.apache.commons.collections.KeyValue;
import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class VSensorConfig implements Serializable {
	

   public static final int DEFAULT_PRIORITY = 100 ;

   public static final int NO_FIXED_RATE = 0 ;

   private String virtualSensorName ;

   private int priority = DEFAULT_PRIORITY ;

   private String mainClass ;

   private String author ;

   private String email ;

   private String description ;

   private int lifeCyclePoolSize = 10 ;

   private int outputStreamRate ;

   private ArrayList < KeyValue > addressing = new ArrayList < KeyValue > ( ) ;

   private ArrayList < DataField > outputStructure = new ArrayList < DataField > ( ) ;

   private String storageHistorySize ;

   private TreeMap < String , InputStream > inputStreamNameToInputStreamObjectMapping = new TreeMap < String , InputStream > ( new CaseInsensitiveComparator ( ) ) ;

   private ArrayList < InputStream > inputStreams = new ArrayList < InputStream > ( ) ;

   private ArrayList < KeyValue > mainClassInitialParams = new ArrayList < KeyValue > ( ) ;

   private transient Long lastModified ;

   private boolean permanentStorage ;

   private transient String fileName ;

   private transient final Logger logger = Logger.getLogger ( VSensorConfig.class ) ;

   /**
    * @return Returns the addressing.
    */
   public ArrayList < KeyValue > getAddressing ( ) {
      return addressing ;
   }

   /**
    * @return Returns the author.
    */
   public String getAuthor ( ) {
      if ( author == null )
         author = "Not specified" ;
      return author ;
   }

   /**
    * @return Returns the description.
    */
   public String getDescription ( ) {
      if ( description == null )
         description = "Not specified" ;
      return description ;
   }

   /**
    * @return Returns the webEmail.
    */
   public String getEmail ( ) {
      if ( email == null )
         email = "Not specified" ;
      return email ;
   }

   /**
    * @return Returns the inputStreams.
    */
   public Collection < InputStream > getInputStreams ( ) {
      return inputStreamNameToInputStreamObjectMapping.values ( ) ;
   }

   public InputStream getInputStream ( String inputStreamName ) {
      return inputStreamNameToInputStreamObjectMapping.get ( inputStreamName ) ;
   }

   /**
    * @return Returns the lifeCyclePoolSize.
    */
   public int getLifeCyclePoolSize ( ) {
      return lifeCyclePoolSize ;
   }

   /**
    * @return Returns the mainClass.
    */
   public String getMainClass ( ) {
      if ( mainClass == null )
         mainClass = "gsn.vsensor.BridgeVirtualSensor" ;
      return mainClass ;
   }

   /**
    * The <code>nameInitialized</code> is used to cache the virtual sensor's
    * name for preformance.
    */
   private boolean nameInitialized = false ;

   public String getVirtualSensorName ( ) {
      if ( nameInitialized == false ) {
         virtualSensorName = virtualSensorName.replace ( " " , "" ).trim ( ) ;
         nameInitialized = true ;
      }
      return virtualSensorName ;
   }

   /**
    * @return Returns the outputStreamRate.
    */
   public int getOutputStreamRate ( ) {
      return outputStreamRate ;
   }

   /**
    * @return Returns the outputStructure.
    */
   public ArrayList < DataField > getOutputStructure ( ) {
      return outputStructure ;
   }

   /**
    * @return Returns the priority.
    */
   public int getPriority ( ) {
      return priority ;
   }

   public Long getLastModified ( ) {
      return lastModified ;
   }

   /**
    * @param addressing
    *           The addressing to set.
    */
   public void setAddressing ( ArrayList < KeyValue > addressing ) {
      this.addressing = addressing ;
   }

   /**
    * @param author
    *           The author to set.
    */
   public void setAuthor ( String author ) {
      this.author = author ;
   }

   /**
    * @param description
    *           The description to set.
    */
   public void setDescription ( String description ) {
      this.description = description ;
   }

   /**
    * @param webEmail
    *           The webEmail to set.
    */
   public void setEmail ( String email ) {
      this.email = email ;
   }

   /**
    * @param lastModified
    *           The lastModified to set.
    */
   public void setLastModified ( Long lastModified ) {
      this.lastModified = lastModified ;
   }

   /**
    * @param lifeCyclePoolSize
    *           The lifeCyclePoolSize to set.
    */
   public void setLifeCyclePoolSize ( int lifeCyclePoolSize ) {
      this.lifeCyclePoolSize = lifeCyclePoolSize ;
   }

   /**
    * @param mainClass
    *           The mainClass to set.
    */
   public void setMainClass ( String mainClass ) {
      this.mainClass = mainClass ;
   }

   /**
    * @param virtualSensorName
    *           The virtualSensorName to set.
    */
   public void setVirtualSensorName ( String virtualSensorName ) {
      this.virtualSensorName = virtualSensorName ;
   }

   /**
    * @param outputStreamRate
    *           The outputStreamRate to set.
    */
   public void setOutputStreamRate ( int outputStreamRate ) {
      this.outputStreamRate = outputStreamRate ;
   }

   /**
    * @param outputStructure
    *           The outputStructure to set.
    */
   public void setOutputStructure ( ArrayList < DataField > outputStructure ) {
      this.outputStructure = outputStructure ;
   }

   /**
    * @param priority
    *           The priority to set.
    */
   public void setPriority ( int priority ) {
      this.priority = priority ;
   }

   public String [ ] getAddressingKeys ( ) {
      String result[] = new String [ getAddressing ( ).size ( ) ] ;
      int counter = 0 ;
      for ( KeyValue predicate : getAddressing ( ) )
         result [ counter ++ ] = ( String ) predicate.getKey ( ) ;
      return result ;
   }

   public String [ ] getAddressingValues ( ) {
      String result[] = new String [ getAddressing ( ).size ( ) ] ;
      int counter = 0 ;
      for ( KeyValue predicate : getAddressing ( ) )
         result [ counter ++ ] = ( String ) predicate.getValue ( ) ;
      return result ;
   }

   public boolean isPermanentStorage ( ) {
      return permanentStorage ;
   }

   private boolean isGetMainClassInitParamsInitialized = false ;

   private TreeMap < String , String > mainClassInitParams = new TreeMap < String , String > ( new CaseInsensitiveComparator ( ) ) ;

   /**
    * Note that the key and value both are trimmed before being inserted into
    * the data strcture.
    * 
    * @return
    */
   public TreeMap < String , String > getMainClassInitialParams ( ) {
      if ( ! isGetMainClassInitParamsInitialized ) {
         isGetMainClassInitParamsInitialized = true ;
         for ( KeyValue param : mainClassInitialParams ) {
            mainClassInitParams.put ( param.getKey ( ).toString ( ) , param.getValue ( ).toString ( ) ) ;
         }
      }
      return mainClassInitParams ;
   }

   public void setMainClassInitialParams ( ArrayList < KeyValue > mainClassInitialParams ) {
      this.mainClassInitialParams = mainClassInitialParams ;
   }

   public String getFileName ( ) {
      return fileName ;
   }

   public void setFileName ( String fileName ) {
      this.fileName = fileName ;
   }

   private boolean isStorageCountBased = true ;

   public static final int STORAGE_SIZE_NOT_SET = - 1 ;

   private long parsedStorageSize = STORAGE_SIZE_NOT_SET ;

   /**
    * @return Returns the storageHistorySize.
    */
   public String getStorageHistorySize ( ) {
      if ( storageHistorySize == null || storageHistorySize.trim ( ).equals ( "" ) )
         storageHistorySize = "0" ;
      return storageHistorySize ;
   }

   /**
    * Checks whether the virtual sensor needs storage or not (checks the
    * variable <code>storageHistorySize</code>
    */
   public boolean needsStorage ( ) {
      if ( getStorageHistorySize ( ).equals ( "0" ) )
         return false ;
      return true ;
   }

   public boolean validate ( ) {
      String storageHistorySize = getStorageHistorySize ( ) ;
      storageHistorySize = storageHistorySize.replace ( " " , "" ).trim ( ).toLowerCase ( ) ;
      for ( InputStream inputStream : inputStreams )
         inputStreamNameToInputStreamObjectMapping.put ( inputStream.getInputStreamName ( ) , inputStream ) ;

      if ( storageHistorySize.equalsIgnoreCase ( "0" ) )
         return true ;
      final int second = 1000 ;
      final int minute = second * 60 ;
      final int hour = minute * 60 ;

      int mIndex = storageHistorySize.indexOf ( "m" ) ;
      int hIndex = storageHistorySize.indexOf ( "h" ) ;
      int sIndex = storageHistorySize.indexOf ( "s" ) ;
      if ( mIndex < 0 && hIndex < 0 && sIndex < 0 ) {
         try {
            this.parsedStorageSize = Integer.parseInt ( storageHistorySize ) ;
            isStorageCountBased = true ;
         } catch ( NumberFormatException e ) {
            logger.error ( new StringBuilder ( )
                  .append ( "The storage size, " ).append ( storageHistorySize ).append ( ", specified for the virtual sensor : " ).append ( virtualSensorName )
                  .append ( " is not valid." ).toString ( ) , e ) ;
            return false ;
         }
      } else {
         try {
            StringBuilder shs = new StringBuilder ( storageHistorySize ) ;
            if ( mIndex > 0 )
               parsedStorageSize = Integer.parseInt ( shs.deleteCharAt ( mIndex ).toString ( ) ) * minute ;
            else if ( hIndex > 0 )
               parsedStorageSize = Integer.parseInt ( shs.deleteCharAt ( hIndex ).toString ( ) ) * hour ;
            else if ( sIndex > 0 )
               parsedStorageSize = Integer.parseInt ( shs.deleteCharAt ( sIndex ).toString ( ) ) * second ;
            isStorageCountBased = false ;
         } catch ( NumberFormatException e ) {
            logger.error ( new StringBuilder ( )
                  .append ( "The storage size, " ).append ( storageHistorySize ).append ( ", specified for the virtual sensor : " ).append ( virtualSensorName )
                  .append ( " is not valid." ).toString ( ) , e ) ;
            return false ;
         }
      }
      return true ;
   }

   public boolean isStorageCountBased ( ) {
      return isStorageCountBased ;
   }

   public long getParsedStorageSize ( ) {
      return parsedStorageSize ;
   }

   public String toString ( ) {
      StringBuilder builder = new StringBuilder ( "Input Stream [" ) ;
      for ( InputStream inputStream : getInputStreams ( ) ) {
         builder.append ( "Input-Stream-Name" ).append ( inputStream.getInputStreamName ( ) ) ;
         builder.append ( "Input-Stream-Query" ).append ( inputStream.getQuery ( ) ) ;
         builder.append ( " Stream-Sources ( " ) ;
         if ( inputStream.getSources ( ) == null )
            builder.append ( "null" ) ;
         else
            for ( StreamSource ss : inputStream.getSources ( ) ) {
               builder.append ( "Stream-Source Alias : " ).append ( ss.getAlias ( ) ) ;
               for ( AddressBean addressing : ss.getAddressing ( ) ) {
                  builder.append ( "Stream-Source-wrapper >" ).append ( addressing.getWrapper ( ) ).append ( "< with addressign predicates : " ) ;
                  for ( KeyValue keyValue : addressing.getPredicates ( ) )
                     builder.append ( "Key=" ).append ( keyValue.getKey ( ) ).append ( "Value=" ).append ( keyValue.getValue ( ) ) ;
               }
               builder.append ( " , " ) ;
            }
         builder.append ( ")" ) ;
      }
      builder.append ( "]" ) ;
      return "VSensorConfig{" + "virtualSensorName='" + virtualSensorName + '\'' + ", priority=" + priority + ", mainClass='" + mainClass + '\'' + ", author='"
            + author + '\'' + ", webEmail='" + email + '\'' + ", description='" + description + '\'' + ", lifeCyclePoolSize=" + lifeCyclePoolSize
            + ", outputStreamRate=" + outputStreamRate + ", addressing=" + addressing + ", outputStructure=" + outputStructure + ", storageHistorySize='"
            + storageHistorySize + '\'' + builder.toString ( ) + ", mainClassInitialParams=" + mainClassInitialParams + ", lastModified=" + lastModified
            + ", permanentStorage=" + permanentStorage + ", fileName='" + fileName + '\'' + ", logger=" + logger + ", nameInitialized=" + nameInitialized
            + ", isStorageCountBased=" + isStorageCountBased + ", parsedStorageSize=" + parsedStorageSize + '}' ;
   }
}