package gsn;

import gsn.beans.Modifications;
import gsn.beans.VSFile;
import gsn.storage.StorageManager;

import java.io.File;
import java.io.FileFilter;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.jibx.runtime.JiBXException;

public class VSensorLoader extends Thread {

	private static transient Logger                                logger                              = Logger.getLogger ( VSensorLoader.class );

	private String                                                 pluginsDir;

	
	private ArrayList<VSensorStateChangeListener> changeListeners = new ArrayList<VSensorStateChangeListener>();

	public void addVSensorStateChangeListener(VSensorStateChangeListener listener) {
		if (!changeListeners.contains(listener))
			changeListeners.add(listener);
	}

	public void removeVSensorStateChangeListener(VSensorStateChangeListener listener) {
		changeListeners.remove(listener);
	}

	public boolean fireVSensorLoading(VSFile config) {
		for (VSensorStateChangeListener listener : changeListeners)
			if (!listener.vsLoading(config))
				return false;
		return true;
	}

	public boolean fireVSensorUnLoading(VSFile config) {
		for (VSensorStateChangeListener listener : changeListeners)
			if (!listener.vsUnLoading(config)) {
				logger.error("Unloading failed !",new RuntimeException("Unloading : "+config.getName()+" is failed."));
				return false;
			}
		return true;
	}
	
	public VSensorLoader ( String pluginsPath ) {
		this.pluginsDir = pluginsPath;
	}

	public void loadPlugin ( ) throws SQLException , JiBXException {
		Modifications modifications = getUpdateStatus ( pluginsDir );
		ArrayList < VSFile > removeIt = modifications.getRemove ( );
		ArrayList<VSFile> addIt = modifications.getAdd();
		for ( VSFile configFile : removeIt ) {
			removeVirtualSensor(configFile);
		}
		try {
			Thread.sleep ( 3000 );
		} catch ( InterruptedException e ) {
			logger.error ( e.getMessage ( ) , e );
		}
		
		for ( VSFile vs : addIt ) {
//			if (!isVirtualSensorValid(vs))
//				continue ;

			VirtualSensorPool pool = new VirtualSensorPool ( vs );
			
			try {
//				if (!StorageManager.getInstance().tableExists( vs.getName ( ) , vs.getProcessingClassConfig().getOutputFormat() ))
//					StorageManager.getInstance().executeCreateTable ( vs.getName ( ) , vs.getProcessingClassConfig().getOutputFormat(),pool.getConfig().getIsTimeStampUnique() );
//				else
//					logger.info("Reusing the existing "+vs.getName()+" table.");
			} catch ( Exception e ) {
				if ( e.getMessage ( ).toLowerCase ( ).contains ( "table already exists" ) ) {
					logger.error ( e.getMessage ( ) );
					if ( logger.isInfoEnabled ( ) ) logger.info ( e.getMessage ( ) , e );
					logger.error ( new StringBuilder ( ).append ( "Loading the virtual sensor specified in the file : " ).append ( vs.getFileName ( ) ).append ( " failed" ).toString ( ) );
					logger.error ( new StringBuilder ( ).append ( "The table : " ).append ( vs.getName ( ) ).append ( " is exists in the database specified in :" ).append (
							Main.getContainerConfig ( ).getContainerFileName ( ) ).append ( "." ).toString ( ) );
					logger.error ( "Solutions : " );
					logger.error ( new StringBuilder ( ).append ( "1. Change the virtual sensor name, in the : " ).append ( vs.getFileName ( ) ).toString ( ) );
					logger.error ( new StringBuilder ( ).append ( "2. Change the URL of the database in " ).append ( Main.getContainerConfig ( ).getContainerFileName ( ) ).append (
					" and choose another database." ).toString ( ) );
					logger.error ( new StringBuilder ( ).append ( "3. Rename/Move the table with the name : " ).append ( Main.getContainerConfig ( ).getContainerFileName ( ) ).append ( " in the database." )
							.toString ( ) );
					logger.error ( new StringBuilder ( ).append ( "4. Change the overwrite-tables=\"true\" (be careful, this will overwrite all the data previously saved in " ).append (
							vs.getName ( ) ).append ( " table )" ).toString ( ) );
				} else {
					logger.error ( e.getMessage ( ) , e );
				}
				continue;
			}
			logger.warn ( new StringBuilder ( "adding : " ).append ( vs.getName() ).append ( " virtual sensor[" ).append ( vs.getFileName ( ) ).append ( "]" ).toString ( ) );
			Mappings.addVSensorInstance ( pool );
			fireVSensorLoading(pool.getConfig());
		}
	}

	private void removeVirtualSensor(VSFile configFile) {
		logger.warn ( new StringBuilder ( ).append ( "removing : " ).append ( configFile.getName ( ) ).toString ( ) );
		VirtualSensorPool sensorInstance = Mappings.getVSensorInstanceByFileName ( configFile.getFileName ( ) );
		Mappings.removeFilename ( configFile.getFileName ( ) );
		removeAllVSResources ( sensorInstance );
	}

	static protected boolean isValidJavaIdentifier(final String name) {
		boolean valid = false;
		while (true) {
			if (false == Character.isJavaIdentifierStart(name.charAt(0))) 
				break;
			valid = true;
			final int count = name.length();
			for (int i = 1; i < count; i++) {
				if (false == Character.isJavaIdentifierPart(name.charAt(i))) {
					valid = false;
					break;
				}
			}
			break;
		}
		return valid;
	}

	public void removeAllVSResources ( VirtualSensorPool pool ) {
		VSFile config = pool.getConfig ( );
		pool.closePool ( );
		final String vsensorName = config.getName ( );
		if ( logger.isInfoEnabled ( ) ) logger.info ( new StringBuilder ( ).append ( "Releasing previously used resources used by [" ).append ( vsensorName ).append ( "]." ).toString ( ) );
		
		logger.debug("Total change Listeners:"+changeListeners.size());
		fireVSensorUnLoading(pool.getConfig());
	}

	public static Modifications getUpdateStatus ( String virtualSensorsPath ) {
		ArrayList< String > remove = new ArrayList<String> ();
		ArrayList< String > add = new ArrayList<String> ();

		String [ ] previous = Mappings.getAllKnownFileName ( );
		FileFilter filter = new FileFilter ( ) {

			public boolean accept ( File file ) {
				if ( !file.isDirectory ( ) && file.getName ( ).endsWith ( ".xml" ) && !file.getName ( ).startsWith ( "." ) ) return true;
				return false;
			}
		};
		File files[] = new File ( virtualSensorsPath ).listFiles ( filter );
		// --- preparing the remove list
		// Removing those in the previous which are not existing the new files
		// or modified.
		main : for ( String pre : previous ) {
			for ( File curr : files )
				if ( pre.equals ( curr.getAbsolutePath ( ) ) && ( Mappings.getLastModifiedTime ( pre ) == curr.lastModified ( ) ) ) continue main;
			remove.add ( pre );
		}
		// ---adding the new files to the Add List a new file should added if
		//
		// 1. it's just deployed.
		// 2. it's modification time changed.

		main : for ( File cur : files ) {
			for ( String pre : previous )
				if ( cur.getAbsolutePath ( ).equals ( pre ) && ( cur.lastModified ( ) == Mappings.getLastModifiedTime ( pre ) ) ) continue main;
			add.add ( cur.getAbsolutePath ( ) );
		}
		Modifications result = new Modifications ( add , remove );
		return result;
	}
	
	public void stopLoading ( ) {
		for ( String configFile : Mappings.getAllKnownFileName ( ) ) {
			VirtualSensorPool sensorInstance = Mappings.getVSensorInstanceByFileName ( configFile );
			removeAllVSResources ( sensorInstance );
			logger.warn ( "Removing the resources associated with : " + sensorInstance.getConfig ( ).getFileName ( ) + " [done]." );
		}
		try {
			StorageManager.getInstance().shutdown ( );
		} catch ( SQLException e ) {
			logger.error(e.getMessage(),e);
		}finally {
			System.exit(0);
		}
	}
}
