package gsn.beans;

import gsn2.conf.OperatorConfig;
import gsn2.conf.SQLOperatorConfig;

import java.io.Serializable;

import org.apache.commons.collections.KeyValue;
import org.apache.log4j.Logger;

public class VSFile implements Serializable {

	private String                                 name;

	private String                                 description;

	
	private KeyValue[]                 addressing                             ;

	private String                                 storageHistorySize;

	private  SQLOperatorConfig        inputStreams  []                            ;

	private transient Long                         lastModified;

	private String                       fileName;

	private transient final Logger                 logger                                    = Logger.getLogger( VSFile.class );

	private String sensorMap = "false";

	private OperatorConfig processingClassConfig;
	/**
	 * @return Returns the addressing.
	 */
	public  KeyValue[] getAddressing ( ) {
		return this.addressing;
	}

	/**
	 * @return Returns the description.
	 */
	public String getDescription ( ) {
		return this.description;
	}

	
	public OperatorConfig getProcessingClassConfig() {
		return processingClassConfig;
	}

	public void setProcessingClassConfig(OperatorConfig processingClassConfig) {
		this.processingClassConfig = processingClassConfig;
	}

	/**
	 * The <code>nameInitialized</code> is used to cache the virtual sensor's
	 * name for preformance.
	 */
	private boolean nameInitialized = false;

	public String getName ( ) {
		if ( this.nameInitialized == false ) {
			this.name = this.name.replace( " " , "" ).trim( ).toLowerCase();
			this.nameInitialized = true;
		}
		return this.name;
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
	 * @param name The name to set.
	 */
	public void setName ( final String virtualSensorName ) {
		this.name = virtualSensorName;
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

	public String getFileName ( ) {
		return this.fileName;
	}

	public void setFileName ( final String fileName ) {
		this.fileName = fileName;
	}

	public static final int STORAGE_SIZE_NOT_SET = -1;

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
		
		if ( storageHistorySize.equalsIgnoreCase( "0" ) ) return true;
		
		return true;
	}

	
	public String toString ( ) {
		final StringBuilder builder = new StringBuilder( "Input Stream [" );
//		for ( final SQLOperator inputStream : getInputStreams( ) ) {
//			builder.append( "Input-Stream-Name" ).append( inputStream.getConfig().getName() );
//			builder.append( "Input-Stream-Query" ).append( inputStream.getConfig().getQuery() );
//			builder.append( " Stream-Sources ( " );
//		}
		builder.append( "]" );
		return "VSensorConfig{" + "name='" + this.name + '\'' + "procesisng-class="+getProcessingClassConfig().toString()  
		+ ", addressing=" + this.addressing +", storageHistorySize='" + this.storageHistorySize + '\'' + builder.toString( )
		+ ", mainClassInitialParams=" + getProcessingClassConfig().getClassName() + ", lastModified=" + this.lastModified + ", fileName='" + this.fileName + '\'' + ", logger=" + this.logger + ", nameInitialized="
		+ this.nameInitialized + '}';
	}

	public boolean equals(Object obj){
		if (obj instanceof VSFile) {
			VSFile vSensorConfig = (VSFile) obj;
			return name.equals(vSensorConfig.getName()); 			
		}
		return false;
	}

	public int hashCode(){
		if(name != null){
			return name.hashCode();
		}
		else{
			return super.hashCode();
		}
	}


	public void setStorageHistorySize(String storageHistorySize){
		this.storageHistorySize = storageHistorySize;
	}

	public boolean getPublishToSensorMap() {
		if (sensorMap==null)
			return false;
		return Boolean.parseBoolean(sensorMap.toString());
	}

	/**
	 * Addressing Helper methods.
	 */
	private transient Double cached_altitude = null ;
	private transient Double cached_longitude = null;
	private transient Double cached_latitude =  null;
	private boolean addressing_processed = false;

	private boolean isTimestampUnique = false;

	public void preprocess_addressing() {
		if (!addressing_processed) {
			for (KeyValue kv:getAddressing())
				if (kv.getKey().toString().equalsIgnoreCase("altitude"))
					cached_altitude=Double.parseDouble(kv.getValue().toString());
				else if (kv.getKey().toString().equalsIgnoreCase("longitude"))
					cached_longitude=Double.parseDouble(kv.getValue().toString());
				else if (kv.getKey().toString().equalsIgnoreCase("latitude"))
					cached_latitude=Double.parseDouble(kv.getValue().toString());
			addressing_processed=true;
		}
	}
	public Double getAltitude() {
		preprocess_addressing();
		return cached_altitude;
	}
	public Double getLatitude() {
		preprocess_addressing();
		return cached_latitude;
	}
	public Double getLongitude() {
		preprocess_addressing();
		return cached_longitude;
	}

	public boolean getIsTimeStampUnique() {
		return isTimestampUnique ;
	}



}