package gsn.beans;

import gsn.registry.Registry;
import gsn.utils.CaseInsensitiveComparator;
import gsn.wrappers.RemoteWrapper;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;
import org.apache.commons.collections.KeyValue;
import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class VSensorConfig implements Serializable {

	private static final long                      serialVersionUID                          = 1625382440863797197L;

	public static final int                        DEFAULT_PRIORITY                          = 100;

	public static final int                        NO_FIXED_RATE                             = 0;

	private String                                 virtualSensorName;

	private int                                    priority                                  = DEFAULT_PRIORITY;

	private String                                 mainClass;

	private String                                 description;

	private int                                    lifeCyclePoolSize                         = 10;

	private int                                    outputStreamRate;

	private KeyValue[]                 addressing                             ;

	private DataField []                outputStructure                        ;


	private String                                 webParameterPassword                             = null;

	private String                                 storageHistorySize;

	private final HashMap < String , InputStream > inputStreamNameToInputStreamObjectMapping = new HashMap < String , InputStream >();

	private final ArrayList < InputStream >        inputStreams                              = new ArrayList < InputStream >( );

	private ArrayList < KeyValue >                 mainClassInitialParams                    = new ArrayList < KeyValue >( );

	private transient Long                         lastModified;

	private transient String                       fileName;

	private transient final Logger                 logger                                    = Logger.getLogger( VSensorConfig.class );

	private String directoryQuery ;

	private String generalPassword = null;

	private WebInput[] webinput;
	/**
	 * @return Returns the addressing.
	 */
	 public  KeyValue[] getAddressing ( ) {
		return this.addressing;
	 }

	 public String[][] getRPCFriendlyAddressing() {
		 String[][] toReturn = new String[this.addressing.length][2] ;
		 for(int i=0;i<toReturn.length;i++)
			 for (KeyValue val : this.addressing) {
				 toReturn[i][0] = ( String ) val.getKey( );
				 toReturn[i][1] = ( String ) val.getValue( );
			 }
		 return toReturn;
	 }

	 public String[][] getRPCFriendlyOutputStructure() {
		 String[][] toReturn = new String[this.outputStructure.length][2] ;
		 for(int i=0;i<outputStructure.length;i++){
			 toReturn[i][0] = ( String ) outputStructure[i].getFieldName();
			 toReturn[i][1] = ( String ) outputStructure[i].getType();
		 }
		 return toReturn;
	 }


	 /**
	  * @return Returns the description.
	  */
	  public String getDescription ( ) {
		  return this.description;
	  }


	  /**
	   * @return Returns the inputStreams.
	   */
	  public Collection < InputStream > getInputStreams ( ) {
		  return this.inputStreamNameToInputStreamObjectMapping.values( );
	  }

	  public InputStream getInputStream ( final String inputStreamName ) {
		  return this.inputStreamNameToInputStreamObjectMapping.get( inputStreamName );
	  }

	  /**
	   * @return Returns the lifeCyclePoolSize.
	   */
	  public int getLifeCyclePoolSize ( ) {
		  return this.lifeCyclePoolSize;
	  }

	  /**
	   * @return Returns the mainClass.
	   */
	  public String getProcessingClass ( ) {
		  if ( this.mainClass == null ) this.mainClass = "gsn.vsensor.BridgeVirtualSensor";
		  return this.mainClass;
	  }

	  /**
	   * The <code>nameInitialized</code> is used to cache the virtual sensor's
	   * name for preformance.
	   */
	  private boolean nameInitialized = false;

	  public String getVirtualSensorName ( ) {
		  if ( this.nameInitialized == false ) {
			  this.virtualSensorName = this.virtualSensorName.replace( " " , "" ).trim( ).toLowerCase();
			  this.nameInitialized = true;
		  }
		  return this.virtualSensorName;
	  }

	  /**
	   * @return Returns the outputStreamRate.
	   */
	  public int getOutputStreamRate ( ) {
		  return this.outputStreamRate;
	  }

	  /**
	   * @return Returns the outputStructure.
	   */
	  public  DataField [] getOutputStructure ( ) {
		  return this.outputStructure;
	  }

	  /**
	   * @return Returns the priority.
	   */
	  public int getPriority ( ) {
		  return this.priority;
	  }

	  public Long getLastModified ( ) {
		  return this.lastModified;
	  }

	  /**
	   * @param addressing The addressing to set.
	   */
	  public void setAddressing ( KeyValue [] addressing ) {
		  this.addressing = addressing;
	  }

	  /**
	   * @param description The description to set.
	   */
	  public void setDescription ( final String description ) {
		  this.description = description;
	  }

	  /**
	   * @param lastModified The lastModified to set.
	   */
	  public void setLastModified ( final Long lastModified ) {
		  this.lastModified = lastModified;
	  }

	  /**
	   * @param lifeCyclePoolSize The lifeCyclePoolSize to set.
	   */
	  public void setLifeCyclePoolSize ( final int lifeCyclePoolSize ) {
		  this.lifeCyclePoolSize = lifeCyclePoolSize;
	  }

	  /**
	   * @param mainClass The mainClass to set.
	   */
	  public void setMainClass ( final String mainClass ) {
		  this.mainClass = mainClass;
	  }

	  /**
	   * @param virtualSensorName The virtualSensorName to set.
	   */
	  public void setVirtualSensorName ( final String virtualSensorName ) {
		  this.virtualSensorName = virtualSensorName;
	  }

	  /**
	   * @param outputStreamRate The outputStreamRate to set.
	   */
	  public void setOutputStreamRate ( final int outputStreamRate ) {
		  this.outputStreamRate = outputStreamRate;
	  }

	  /**
	   * @param outputStructure The outputStructure to set.
	   */
	  public void setOutputStructure ( DataField[] outputStructure) {
		  this.outputStructure = outputStructure;
	  }

	  /**
	   * @param priority The priority to set.
	   */
	  public void setPriority ( final int priority ) {
		  this.priority = priority;
	  }

	  public String [ ] getAddressingKeys ( ) {
		  final String result[] = new String [ this.getAddressing( ).length ];
		  int counter = 0;
		  for ( final KeyValue predicate : this.getAddressing( ) )
			  result[ counter++ ] = ( String ) predicate.getKey( );
		  return result;
	  }

	  public String [ ] getAddressingValues ( ) {
		  final String result[] = new String [ this.getAddressing( ).length ];
		  int counter = 0;
		  for ( final KeyValue predicate : this.getAddressing( ) )
			  result[ counter++ ] = ( String ) predicate.getValue( );
		  return result;
	  }

	  private boolean                           isGetMainClassInitParamsInitialized = false;

	  private final TreeMap < String , String > mainClassInitParams                 = new TreeMap  < String , String >( new CaseInsensitiveComparator());

	  /**
	   * Note that the key and value both are trimmed before being inserted into
	   * the data strcture.
	   * 
	   * @return
	   */
	  public TreeMap < String , String > getMainClassInitialParams ( ) {
		  if ( !this.isGetMainClassInitParamsInitialized ) {
			  this.isGetMainClassInitParamsInitialized = true;
			  for ( final KeyValue param : this.mainClassInitialParams ) {
				  this.mainClassInitParams.put( param.getKey( ).toString( ).toLowerCase( ) , param.getValue( ).toString( ) );
			  }
		  }
		  return this.mainClassInitParams;
	  }

	  public void setMainClassInitialParams ( final ArrayList < KeyValue > mainClassInitialParams ) {
		  this.mainClassInitialParams = mainClassInitialParams;
	  }

	  public String getFileName ( ) {
		  return this.fileName;
	  }

	  public void setFileName ( final String fileName ) {
		  this.fileName = fileName;
	  }

	  private boolean         isStorageCountBased  = true;

	  public static final int STORAGE_SIZE_NOT_SET = -1;

	  private long            parsedStorageSize    = STORAGE_SIZE_NOT_SET;

	  /**
	   * @return Returns the storageHistorySize.
	   */
	  public String getStorageHistorySize ( ) {
		  if ( this.storageHistorySize == null || this.storageHistorySize.trim( ).equals( "" ) ) this.storageHistorySize = "0";
		  return this.storageHistorySize;
	  }

	  /**
	   * Checks whether the virtual sensor needs storage or not (checks the
	   * variable <code>storageHistorySize</code>
	   */
	  public boolean needsStorage ( ) {
		  if ( this.getStorageHistorySize( ).equals( "0" ) ) return false;
		  return true;
	  }

	  public boolean validate ( ) {
		  String storageHistorySize = this.getStorageHistorySize( );
		  storageHistorySize = storageHistorySize.replace( " " , "" ).trim( ).toLowerCase( );
		  for ( final InputStream inputStream : this.inputStreams )
			  this.inputStreamNameToInputStreamObjectMapping.put( inputStream.getInputStreamName( ) , inputStream );

		  if ( storageHistorySize.equalsIgnoreCase( "0" ) ) return true;
		  final int second = 1000;
		  final int minute = second * 60;
		  final int hour = minute * 60;

		  final int mIndex = storageHistorySize.indexOf( "m" );
		  final int hIndex = storageHistorySize.indexOf( "h" );
		  final int sIndex = storageHistorySize.indexOf( "s" );
		  if ( mIndex < 0 && hIndex < 0 && sIndex < 0 ) {
			  try {
				  this.parsedStorageSize = Integer.parseInt( storageHistorySize );
				  this.isStorageCountBased = true;
			  } catch ( final NumberFormatException e ) {
				  this.logger.error( new StringBuilder( ).append( "The storage size, " ).append( storageHistorySize ).append( ", specified for the virtual sensor : " ).append( this.virtualSensorName )
						  .append( " is not valid." ).toString( ) , e );
				  return false;
			  }
		  } else {
			  try {
				  final StringBuilder shs = new StringBuilder( storageHistorySize );
				  if ( mIndex > 0 )
					  this.parsedStorageSize = Integer.parseInt( shs.deleteCharAt( mIndex ).toString( ) ) * minute;
				  else if ( hIndex > 0 )
					  this.parsedStorageSize = Integer.parseInt( shs.deleteCharAt( hIndex ).toString( ) ) * hour;
				  else if ( sIndex > 0 ) this.parsedStorageSize = Integer.parseInt( shs.deleteCharAt( sIndex ).toString( ) ) * second;
				  this.isStorageCountBased = false;
			  } catch ( final NumberFormatException e ) {
				  this.logger.error( new StringBuilder( ).append( "The storage size, " ).append( storageHistorySize ).append( ", specified for the virtual sensor : " ).append( this.virtualSensorName )
						  .append( " is not valid." ).toString( ) , e );
				  return false;
			  }
		  }
		  return true;
	  }

	  public boolean isStorageCountBased ( ) {
		  return this.isStorageCountBased;
	  }

	  public long getParsedStorageSize ( ) {
		  return this.parsedStorageSize;
	  }

	  public String getDirectoryQuery() {
		  return directoryQuery;
	  }

	  public String getUsedSources ( ) {
		  StringBuilder usedSources = new StringBuilder( );
		  for ( InputStream is : inputStreams )
			  for ( StreamSource ss : is.getSources( ) )

				  if ( ss.getWrapper( ) instanceof RemoteWrapper ) {
					  RemoteWrapper remote = ( RemoteWrapper ) ss.getWrapper( );
					  usedSources.append( remote.getRemoteHost( ) ).append( ":" ).append( remote.getRemotePort( ) ).append( "/" ).append( remote.getRemoveVSName( ) ).append( " " );
				  } else {
//					  usedSources.append( ss.getWrapper( ).getWrapperName( ) ).append( Registry.SPACE_CHARACTER );
				  }
		  return usedSources.toString( );
	  }

	  /**
	   * @return the securityCode
	   */
	  public String getWebParameterPassword ( ) {
		  return webParameterPassword;
	  }


	  public String toString ( ) {
		  final StringBuilder builder = new StringBuilder( "Input Stream [" );
		  for ( final InputStream inputStream : this.getInputStreams( ) ) {
			  builder.append( "Input-Stream-Name" ).append( inputStream.getInputStreamName( ) );
			  builder.append( "Input-Stream-Query" ).append( inputStream.getQuery( ) );
			  builder.append( " Stream-Sources ( " );
			  if ( inputStream.getSources( ) == null )
				  builder.append( "null" );
			  else
				  for ( final StreamSource ss : inputStream.getSources( ) ) {
					  builder.append( "Stream-Source Alias : " ).append( ss.getAlias( ) );
					  for ( final AddressBean addressing : ss.getAddressing( ) ) {
						  builder.append( "Stream-Source-wrapper >" ).append( addressing.getWrapper( ) ).append( "< with addressign predicates : " );
						  for ( final KeyValue keyValue : addressing.getPredicates( ) )
							  builder.append( "Key=" ).append( keyValue.getKey( ) ).append( "Value=" ).append( keyValue.getValue( ) );
					  }
					  builder.append( " , " );
				  }
			  builder.append( ")" );
		  }
		  builder.append( "]" );
		  return "VSensorConfig{" + "virtualSensorName='" + this.virtualSensorName + '\'' + ", priority=" + this.priority + ", mainClass='" + this.mainClass + '\'' 
		  + ", description='" + this.description + '\'' + ", lifeCyclePoolSize=" + this.lifeCyclePoolSize + ", outputStreamRate=" + this.outputStreamRate
		  + ", addressing=" + this.addressing + ", outputStructure=" + this.outputStructure + ", storageHistorySize='" + this.storageHistorySize + '\'' + builder.toString( )
		  + ", mainClassInitialParams=" + this.mainClassInitialParams + ", lastModified=" + this.lastModified + ", fileName='" + this.fileName + '\'' + ", logger=" + this.logger + ", nameInitialized="
		  + this.nameInitialized + ", isStorageCountBased=" + this.isStorageCountBased + ", parsedStorageSize=" + this.parsedStorageSize + '}';
	  }

	  public String getGeneralPassword(){
		  return generalPassword;
	  }

	  public void setGeneralPassword(String generalPassword){
		  this.generalPassword=generalPassword;
	  }


	  /**
	   * @return the webinput
	   */
	  public WebInput [ ] getWebinput ( ) {
		  return webinput;
	  }

}