package gsn;

import gsn.beans.DataField;
import gsn.beans.VSFile;

import java.util.Iterator;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

public final class Mappings {
   
   private static final ConcurrentHashMap<String,VSFile>              vsNameTOVSConfig               = new ConcurrentHashMap<String,VSFile>( );
   
   private static final ConcurrentHashMap < String , VirtualSensorPool >             fileNameToVSInstance           = new ConcurrentHashMap < String , VirtualSensorPool >(  );
   
   private static final ConcurrentHashMap < String , TreeMap < String , Boolean >> vsNamesToOutputStructureFields = new ConcurrentHashMap < String , TreeMap < String , Boolean >>( );
   
   private static final transient Logger                                 logger                         = Logger.getLogger( Mappings.class );
   
   public static void addVSensorInstance ( VirtualSensorPool sensorPool ) {
      TreeMap < String , Boolean > vsNameToOutputStructureFields = new TreeMap < String , Boolean >( );
      vsNamesToOutputStructureFields.put( sensorPool.getConfig( ).getName( ) , vsNameToOutputStructureFields );
      for ( DataField fields : sensorPool.getConfig( ).getProcessingClassConfig().getOutputFormat() )
         vsNameToOutputStructureFields.put( fields.getName( ) , Boolean.TRUE );
      vsNameToOutputStructureFields.put( "timed" , Boolean.TRUE );
      vsNameTOVSConfig.put( sensorPool.getConfig( ).getName( ) , sensorPool.getConfig( ) );
      fileNameToVSInstance.put( sensorPool.getConfig( ).getFileName( ) , sensorPool );
   }
   
   public static VirtualSensorPool getVSensorInstanceByFileName ( String fileName ) {
      return fileNameToVSInstance.get( fileName );
   }
   
   public static final TreeMap < String , Boolean > getVsNamesToOutputStructureFieldsMapping ( String vsName ) {
      return vsNamesToOutputStructureFields.get( vsName );
   }
   
   public static VSFile getVSensorConfig ( String vSensorName ) {
      if ( vSensorName == null ) return null;
      return vsNameTOVSConfig.get( vSensorName );
   }
   
   public static void removeFilename ( String fileName ) {
	   if(fileNameToVSInstance.containsKey(fileName)){
		   VSFile config = ( fileNameToVSInstance.get( fileName ) ).getConfig( );
		   vsNameTOVSConfig.remove( config.getName( ) );
		   fileNameToVSInstance.remove( fileName );
	   }
   }
   
   public static Long getLastModifiedTime ( String configFileName ) {
      return Long.valueOf( ( fileNameToVSInstance.get( configFileName ) ).getLastModified( ) );
   }
   
   public static String [ ] getAllKnownFileName ( ) {
      return fileNameToVSInstance.keySet( ).toArray( new String [ 0 ] );
   }
   
   public static VSFile getConfigurationObject ( String fileName ) {
      if ( fileName == null ) return null;
      return ( fileNameToVSInstance.get( fileName ) ).getConfig( );
   }
   
   public static Iterator < VSFile > getAllVSensorConfigs ( ) {
      return vsNameTOVSConfig.values( ).iterator( );
   }
   
   public static VirtualSensorPool getVSensorInstanceByVSName ( String vsensorName ) {
      if ( vsensorName == null ) return null;
      VSFile vSensorConfig = vsNameTOVSConfig.get( vsensorName );
      if ( vSensorConfig == null ) return null;
      return getVSensorInstanceByFileName( vSensorConfig.getFileName( ) );
   }
   /**
    * Case insensitive matching.
    * @param vsName
    * @return
    */
   public static VSFile getConfig(String vsName) {
		Iterator<VSFile> configs = Mappings.getAllVSensorConfigs();
		while(configs.hasNext()) {
			VSFile config = configs.next();
			if (config.getName().equalsIgnoreCase(vsName))
				return config;
		}
		return null;
	}  
}
