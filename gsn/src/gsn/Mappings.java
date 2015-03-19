package gsn;

import gsn.beans.DataField;
import gsn.beans.VSensorConfig;

import java.util.Iterator;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

public final class Mappings {
   
   private static final ConcurrentHashMap<String,VSensorConfig>              vsNameTOVSConfig               = new ConcurrentHashMap<String,VSensorConfig>( );
   
   private static final ConcurrentHashMap < String , VirtualSensor>             fileNameToVSInstance           = new ConcurrentHashMap < String , VirtualSensor>(  );
   
   private static final ConcurrentHashMap < String , TreeMap < String , Boolean >> vsNamesToOutputStructureFields = new ConcurrentHashMap < String , TreeMap < String , Boolean >>( );
   
   private static final transient Logger                                 logger                         = Logger.getLogger( Mappings.class );
   
   public static boolean addVSensorInstance ( VirtualSensor sensorPool ) {
      try {
         if ( logger.isInfoEnabled( ) ) logger.info( ( new StringBuilder( "Testing the pool for :" ) ).append( sensorPool.getConfig( ).getName( ) ).toString( ) );
         vsNameTOVSConfig.put( sensorPool.getConfig( ).getName( ) , sensorPool.getConfig( ) );
         TreeMap < String , Boolean > vsNameToOutputStructureFields = new TreeMap < String , Boolean >( );
         vsNamesToOutputStructureFields.put( sensorPool.getConfig( ).getName( ) , vsNameToOutputStructureFields );
         for ( DataField fields : sensorPool.getConfig( ).getOutputStructure( ) )
            vsNameToOutputStructureFields.put( fields.getName( ) , Boolean.TRUE );
         vsNameToOutputStructureFields.put( "timed" , Boolean.TRUE );
         fileNameToVSInstance.put( sensorPool.getConfig( ).getFileName( ) , sensorPool );
         sensorPool.returnVS( sensorPool.borrowVS( ) );
      } catch ( Exception e ) {
         logger.error( e.getMessage( ) , e );
         vsNameTOVSConfig.remove( sensorPool.getConfig( ).getName( ) );
         vsNamesToOutputStructureFields.remove( sensorPool.getConfig( ).getName( ) );
         fileNameToVSInstance.remove( sensorPool.getConfig( ).getFileName( ) );
         sensorPool.closePool( );
         logger.error( "GSN can't load the virtual sensor specified at " + sensorPool.getConfig( ).getFileName( ) + " because the initialization of the virtual sensor failed (see above exception)." );
         logger.error( "Please fix the following error" );
         return false;
      }
      return true;
   }

   public static VirtualSensor getVSensorInstanceByFileName ( String fileName ) {
      return fileNameToVSInstance.get( fileName );
   }

   public static final TreeMap < String , Boolean > getVsNamesToOutputStructureFieldsMapping ( String vSensorName ) {
	   if ( vSensorName == null ) return null;
	   // case sensitive matching
	   TreeMap < String , Boolean > vsNameToOutputStructureFields = vsNamesToOutputStructureFields.get( vSensorName );
	   if ( vsNameToOutputStructureFields == null ) {
		   // try case insensitive matching
		   for(String vsName: vsNamesToOutputStructureFields.keySet()) {
			   if (vsName.equalsIgnoreCase(vSensorName))
				   return vsNamesToOutputStructureFields.get(vsName);
		   }
		   return null;
	   }
	   else
		   return vsNameToOutputStructureFields;
   }

   public static VSensorConfig getVSensorConfig ( String vSensorName ) {
	   if ( vSensorName == null ) return null;
	   // case sensitive matching
	   VSensorConfig config = vsNameTOVSConfig.get( vSensorName );
	   if ( config == null ) {
		   // try case insensitive matching
		   Iterator<VSensorConfig> configs = Mappings.getAllVSensorConfigs();
		   while(configs.hasNext()) {
			   config = configs.next();
			   if (config.getName().equalsIgnoreCase(vSensorName))
				   return config;
		   }
		   return null;
	   }
	   else
		   return config;
   }

   public static void removeFilename ( String fileName ) {
	   if(fileNameToVSInstance.containsKey(fileName)){
		   VSensorConfig config = ( fileNameToVSInstance.get( fileName ) ).getConfig( );
		   vsNameTOVSConfig.remove( config.getName( ) );
		   fileNameToVSInstance.remove( fileName );
	   }
   }

   public static Long getLastModifiedTime ( String fileName ) {
      return Long.valueOf( ( fileNameToVSInstance.get( fileName ) ).getLastModified( ) );
   }
   
   public static String [ ] getAllKnownFileName ( ) {
      return fileNameToVSInstance.keySet( ).toArray( new String [ 0 ] );
   }

   public static VSensorConfig getConfigurationObject ( String fileName ) {
      if ( fileName == null ) return null;
      return ( fileNameToVSInstance.get( fileName ) ).getConfig( );
   }

   public static Iterator < VSensorConfig > getAllVSensorConfigs ( ) {
      return vsNameTOVSConfig.values( ).iterator( );
   }

   public static VirtualSensor getVSensorInstanceByVSName ( String vsensorName ) {
      VSensorConfig vSensorConfig = getVSensorConfig( vsensorName );
      if ( vSensorConfig == null ) return null;
      return getVSensorInstanceByFileName( vSensorConfig.getFileName( ) );
   }
}

 	  	 
