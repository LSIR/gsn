/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2016, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: src/ch/epfl/gsn/VSensorLoader.java
*
* @author Mehdi Riahi
* @author gsn_devs
* @author Sofiane Sarni
* @author Ali Salehi
* @author Mehdi Riahi
* @author Timotee Maret
* @author Julien Eberle
*
*/

package ch.epfl.gsn;

import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import org.slf4j.LoggerFactory;

import ch.epfl.gsn.Main;
import ch.epfl.gsn.Mappings;
import ch.epfl.gsn.VSensorLoader;
import ch.epfl.gsn.VSensorStateChangeListener;
import ch.epfl.gsn.VirtualSensor;
import ch.epfl.gsn.VirtualSensorInitializationFailedException;
import ch.epfl.gsn.beans.AddressBean;
import ch.epfl.gsn.beans.DataField;
import ch.epfl.gsn.beans.InputStream;
import ch.epfl.gsn.beans.Modifications;
import ch.epfl.gsn.beans.StreamSource;
import ch.epfl.gsn.beans.VSensorConfig;
import ch.epfl.gsn.wrappers.AbstractWrapper;
import ch.epfl.gsn.wrappers.WrappersUtil;

import org.slf4j.Logger;

public class VSensorLoader extends Thread {
 
	public static final String VSENSOR_POOL = "VSENSOR-POOL";
	public static final String STREAM_SOURCE = "STREAM-SOURCE";
	public static final String INPUT_STREAM = "INPUT-STREAM";
	
	private String pluginsDir;
	private boolean isActive = true;
	private ArrayList<VSensorStateChangeListener> changeListeners = new ArrayList<VSensorStateChangeListener>();
	
    private static int VSENSOR_LOADER_THREAD_COUNTER = 0;
    private static VSensorLoader singleton = null;
    private static transient Logger logger = LoggerFactory.getLogger ( VSensorLoader.class );
    

	public void addVSensorStateChangeListener(VSensorStateChangeListener listener) {
		if (!changeListeners.contains(listener))
			changeListeners.add(listener);
	}

	public void removeVSensorStateChangeListener(VSensorStateChangeListener listener) {
		changeListeners.remove(listener);
	}
	
	public boolean fireVSensorLoading(VSensorConfig config) {
		for (VSensorStateChangeListener listener : changeListeners)
			if (!listener.vsLoading(config))
				return false;
		return true;
	}
	
	public boolean fireVSensorUnLoading(VSensorConfig config) {
		for (VSensorStateChangeListener listener : changeListeners)
			if (!listener.vsUnLoading(config)) {
				logger.error("Unloading failed !",new RuntimeException("Unloading : "+config.getName()+" is failed."));
				return false;
			}
		return true;
	}

	public VSensorLoader() {}
	
	public VSensorLoader ( String pluginsPath ) {
		this.pluginsDir = pluginsPath;
	}

    public static VSensorLoader getInstance(String path) {
        if (singleton == null)
            singleton = new VSensorLoader(path);
        return singleton;
    }
	
	public void startLoading() {
		Thread thread = new Thread ( this );
		thread.setName ( "VSensorLoader-Thread" + VSENSOR_LOADER_THREAD_COUNTER++ );
		thread.start ( );
	}

	public void run ( ) {
		if ( Main.getStorage((VSensorConfig)null) == null || Main.getWindowStorage() == null ) { // Checks only if the default storage and the window storage are defined.
			logger.error ( "The Storage Manager shouldn't be null, possible a BUG." );
			return;
		}
		while ( isActive ) {
			try {
				loadPlugin ( );
			} catch ( Exception e ) {
				logger.error ( e.getMessage ( ) , e );
			}
			try {
				Thread.sleep ( 3000 );
			} catch ( InterruptedException e ) {
				logger.error ( e.getMessage ( ) , e );
			} 
		}
	}

    public synchronized void loadVirtualSensor(String vsConfigurationFileContent, String fileName) throws Exception {
        String filePath = getVSConfigurationFilePath(fileName);
        File file = new File(filePath);
        if (!file.exists()) {
            try {
                // Create the VS configuration file
                Writer fw = new BufferedWriter(new FileWriter(filePath, true));
                fw.write(vsConfigurationFileContent);
                fw.flush();
                fw.close();
                // Try to load it
                if ( ! loadPlugin(fileName)) {
                    throw new Exception("Failed to load the Virtual Sensor: " + fileName + " because there is syntax error in the configuration file. Please check the configuration file and try again.");
                }
            }
            catch (Exception e) {
                logger.warn(e.getMessage(), e);
                if (file.exists()) file.delete();
                throw e;
            }
        } else {
            logger.warn("The configuration file:" + filePath + " already exist.");
            throw new Exception("The configuration file:" + filePath + " already exist.");
        }
    }

     public static String getVSConfigurationFilePath(String fileName) {
        return Main.virtualSensorDirectory + File.separator + fileName + ".xml";
    }

    public synchronized void loadPlugin() throws SQLException {

        Modifications modifications = getUpdateStatus(pluginsDir);
        ArrayList<VSensorConfig> removeIt = modifications.getRemove();
        ArrayList<VSensorConfig> addIt = modifications.getAdd();
        for (VSensorConfig configFile : removeIt) {
            removeVirtualSensor(configFile);
        }
        for (VSensorConfig vs : addIt) {
        	try{
                loadPlugin(vs);
        	}catch(Exception e){
        		logger.error("Unable to load VSensor " + vs.getName() + ", retrying later... : "+ e.getMessage());
        	}
        }
    }

    public synchronized boolean loadPlugin(String fileFilterName) throws SQLException {
        Modifications modifications = getUpdateStatus(pluginsDir, fileFilterName);
        ArrayList<VSensorConfig> addIt = modifications.getAdd();

        boolean found = false;
        for (VSensorConfig config : addIt){
            if (config.getName().equals(fileFilterName)) {
                found = true;
                break;
            }
        }
        if (!found)
            return false;
        else
            return loadPlugin(addIt.get(0));
    }


    private synchronized boolean loadPlugin(VSensorConfig vs) throws SQLException {

        if (!isVirtualSensorValid(vs))
            return false;

        VirtualSensor pool = new VirtualSensor(vs);
        try {
            if (createInputStreams(pool) == false) {
                logger.error("loading the >" + vs.getName() + "< virtual sensor is stoped due to error(s) in preparing the input streams.");
                return false;
            }
        } catch (InstantiationException e2) {
            logger.error(e2.getMessage(), e2);
        } catch (IllegalAccessException e2) {
            logger.error(e2.getMessage(), e2);
        }
        try {
            if (!Main.getStorage(vs).tableExists(vs.getName(), vs.getOutputStructure()))
                Main.getStorage(vs).executeCreateTable(vs.getName(), vs.getOutputStructure(), pool.getConfig().getIsTimeStampUnique());
            else
                logger.info("Reusing the existing " + vs.getName() + " table.");
        } catch (Exception e) {
        	removeAllVSResources(pool);
            if (e.getMessage().toLowerCase().contains("table already exists")) {
                logger.error("Loading the virtual sensor from " + vs.getFileName() + " failed, because the table " + vs.getName() + " already exists in the database specified in " + Main.getContainerConfig().getContainerFileName() + ".");
                logger.info("Solutions : ");
                logger.info("1. Change the virtual sensor name, in the : " + vs.getFileName());
                logger.info("2. Change the URL of the database in " + Main.getContainerConfig().getContainerFileName() + " and choose another database.");
                logger.info("3. Rename/Move the table with the name : " + Main.getContainerConfig().getContainerFileName() + " in the database.");
                logger.info("4. Change the overwrite-tables=\"true\" (be careful, this will overwrite all the data previously saved in " + vs.getName() + " table )");
            } else {
                logger.error(e.getMessage(), e);
            }
            return false;
        }
        logger.info("adding : " + vs.getName() + " virtual sensor[" + vs.getFileName() + "]");
        if (Mappings.addVSensorInstance(pool)) {
            try {
                fireVSensorLoading(pool.getConfig());
                pool.start();
            } catch (VirtualSensorInitializationFailedException e1) {
                logger.error("Creating the virtual sensor >" + vs.getName() + "< failed.", e1);
                removeVirtualSensor(vs);
                return false;
            }
        } else {
        	removeAllVSResources(pool);
        }
        return true;

    }

    
	private void removeVirtualSensor(VSensorConfig configFile) {
		logger.info ("removing : " + configFile.getName ( ));
		VirtualSensor sensorInstance = Mappings.getVSensorInstanceByFileName ( configFile.getFileName ( ) );
		Mappings.removeFilename ( configFile.getFileName ( ) );
		removeAllVSResources ( sensorInstance );
	}

	public boolean isVirtualSensorValid(VSensorConfig configuration) {
		for ( InputStream is : configuration.getInputStreams ( ) ) {
			if ( !is.validate ( ) ) {
				logger.error("Adding the virtual sensor specified in " + configuration.getFileName ( ) + " failed because of one or more problems in configuration file." );
				logger.info("Please check the file and try again");
				return false;
			}
		}
		String vsName = configuration.getName ( );
		if ( Mappings.getVSensorConfig ( vsName ) != null ) {
			logger.error("Adding the virtual sensor specified in " + configuration.getFileName ( ) + " failed because the virtual sensor name used by " +
					       configuration.getFileName ( ) + " is already used by : " + Mappings.getVSensorConfig ( vsName ).getFileName ( ));
			logger.info( "Note that the virtual sensor name is case insensitive and all the spaces in it's name will be removed automatically." );
			return false;
		}

		if ( !isValidJavaIdentifier( vsName ) ) {
			logger.error("Adding the virtual sensor specified in "+configuration.getFileName()+" failed because the virtual sensor name is not following the requirements : ");
			logger.info ( "The virtual sensor name is case insensitive and all the spaces in it's name will be removed automatically." );
			logger.info ( "That the name of the virutal sensor should starting by alphabetical character and they can contain numerical characters afterwards." );
			return false;
		}
		return true;
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

	public void removeAllVSResources ( VirtualSensor pool ) {
		VSensorConfig config = pool.getConfig ( );
		pool.closePool ( );
		final String vsensorName = config.getName ( );
		logger.info ("Releasing previously used resources used by [" + vsensorName + "].");
		for ( InputStream inputStream : config.getInputStreams ( ) ) {
			for ( StreamSource streamSource : inputStream.getSources ( ) ) 
				releaseStreamSource(streamSource);
			inputStream.release();
		}
		logger.debug("Total change Listeners:"+changeListeners.size());
		fireVSensorUnLoading(pool.getConfig());
	}

	public void releaseStreamSource(StreamSource streamSource) {
		final AbstractWrapper wrapper = streamSource.getWrapper ( );
		streamSource.getInputStream().getRenamingMapping().remove(streamSource.getAlias());
		try {
			wrapper.removeListener(streamSource);
		} catch (SQLException e) {
			logger.error("Release the resources failed !" + e.getMessage());
		}
	}

    public static Modifications getUpdateStatus(String virtualSensorsPath) {
            return getUpdateStatus(virtualSensorsPath, null);
        }

        public static Modifications getUpdateStatus(String virtualSensorsPath, String filterFileName) {
            ArrayList<String> remove = new ArrayList<String>();
            ArrayList<String> add = new ArrayList<String>();

            String[] previous = Mappings.getAllKnownFileName();

            FileFilter filter = new FileFilter ( ) {
                public boolean accept ( File file ) {
                    if ( !file.isDirectory ( ) && file.getName ( ).endsWith ( ".xml" ) && !file.getName ( ).startsWith ( "." ) ) return true;
                    return false;
                }
            };

            File files[] = new File(virtualSensorsPath).listFiles(filter);

			Arrays.sort(files, new Comparator<File>(){
				@Override
				public int compare(File a, File b) {
					return a.getName().compareTo(b.getName());
				}});
			
            // --- preparing the remove list
            // Removing those in the previous which are not existing the new files
            // or modified.
            main:
            for (String pre : previous) {
                for (File curr : files)
                    if (pre.equals(curr.getAbsolutePath()) && (Mappings.getLastModifiedTime(pre) == curr.lastModified()))
                        continue main;
                remove.add(pre);
            }
            // ---adding the new files to the Add List a new file should added if
            //
            // 1. it's just deployed.
            // 2. it's modification time changed.

            main:
            for (File cur : files) {
                for (String pre : previous)
                    if (cur.getAbsolutePath().equals(pre) && (cur.lastModified() == Mappings.getLastModifiedTime(pre)))
                        continue main;
                add.add(cur.getAbsolutePath());
            }
            Modifications result = new Modifications(add, remove);
            return result;
        }
	

	/**
	 * The properties file contains information on wrappers for stream sources.
	 * FIXME : The body of CreateInputStreams is incomplete b/c in the case of an
	 * error it should remove the resources.
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public boolean createInputStreams ( VirtualSensor pool ) throws InstantiationException, IllegalAccessException {
		logger.debug ( new StringBuilder ( ).append ( "Preparing input streams for: " ).append ( pool.getConfig().getName ( ) ).toString ( ) );
		if ( pool.getConfig().getInputStreams ( ).size ( ) == 0 ) logger.warn ( new StringBuilder ( "There is no input streams defined for *" ).append ( pool.getConfig().getName ( ) ).append ( "*" ).toString ( ) );
		ArrayList<StreamSource> sources = new ArrayList<StreamSource>();
		ArrayList<InputStream> streams = new ArrayList<InputStream>();
		for ( Iterator < InputStream > inputStreamIterator = pool.getConfig().getInputStreams ( ).iterator ( ) ; inputStreamIterator.hasNext ( ) ; ) {
			InputStream inputStream = inputStreamIterator.next ( );
			for ( StreamSource  dataSouce : inputStream.getSources ( )) {
				if ( ! prepareStreamSource( pool.getConfig(),inputStream , dataSouce )){
					for (StreamSource ss : sources){
						releaseStreamSource(ss);
					}
					for (InputStream is : streams){
						is.release();
					}
					return false;
				}
				sources.add(dataSouce);
			}
			streams.add(inputStream);
			inputStream.setPool (pool );
		}
		return true;
	}
	/**
	 * Instantiate the wrapper from its addressBean.
	 * if it doesn't return null, the calling class is responsible for releasing the resources of the wrapper.
	 * @param addressBean
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * FIXME: COPIED_FOR_SAFE_STOAGE
	 */
	public AbstractWrapper createWrapper(AddressBean addressBean) throws InstantiationException, IllegalAccessException {

			if ( Main.getWrapperClass ( addressBean.getWrapper ( ) ) == null ) {
				logger.error ( "The wrapper >" + addressBean.getWrapper ( ) + "< is not defined in the >" + WrappersUtil.DEFAULT_WRAPPER_PROPERTIES_FILE + "< file." );
				return null;
			}
			AbstractWrapper wrapper = ( AbstractWrapper ) Main.getWrapperClass ( addressBean.getWrapper ( ) ).newInstance ( );
			wrapper.setActiveAddressBean ( addressBean );
			boolean initializationResult = wrapper.initialize_wrapper (  );
			if ( initializationResult == false )
				return null;
			try {
				logger.debug("Wrapper name: "+wrapper.getWrapperName()+ " -- view name "+ wrapper.getDBAliasInStr());
				if (!Main.getWindowStorage().tableExists(wrapper.getDBAliasInStr(),wrapper.getOutputFormat()))
					Main.getWindowStorage().executeCreateTable ( wrapper.getDBAliasInStr ( ) , wrapper.getOutputFormat ( ),wrapper.isTimeStampUnique() );
			} catch ( Exception e ) {
				try{
				wrapper.releaseResources();  //releasing resources
				}catch (SQLException sql){
					logger.error ( sql.getMessage ( ) , sql );
				}
				logger.error ( e.getMessage ( ) , e );
				return null;
			}
			return wrapper;
	}
	
	public boolean prepareStreamSource ( VSensorConfig vsensorConfig,InputStream inputStream , StreamSource streamSource  ) throws InstantiationException, IllegalAccessException {
		streamSource.setInputStream(inputStream);
		AbstractWrapper wrapper = null;
		for ( AddressBean addressBean : streamSource.getAddressing ( ) ) {
			addressBean.setInputStreamName(inputStream.getInputStreamName());
			addressBean.setVirtualSensorName(vsensorConfig.getName());
			wrapper = createWrapper(addressBean);
			try {
				if (wrapper!=null && prepareStreamSource( streamSource,wrapper.getOutputFormat(),wrapper)) 
					break;
				else
					if (wrapper!=null){
						wrapper.releaseResources();
					}
					wrapper=null;
			} catch (Exception e) {
				if (wrapper!=null){
					try{
					    wrapper.releaseResources();
					}catch(SQLException sql){
						logger.error(sql.getMessage(),sql);
					}
				}
				logger.error("Preparation of the stream source failed : "+streamSource.getAlias()+ " from the input stream : "+inputStream.getInputStreamName()+". " + e.getMessage(),e);
			}
		}
		return (wrapper!=null);
	}

	public boolean prepareStreamSource ( StreamSource streamSource ,DataField[] outputformat, AbstractWrapper wrapper ) throws InstantiationException, IllegalAccessException, SQLException {
		if (outputformat==null) {
			logger.error("Preparing the stream source failed because the wrapper : "+wrapper.getWrapperName()+" returns null for the >getOutputStructure< method!");
			return false;
		}
		streamSource.setWrapper ( wrapper );
		streamSource.getInputStream().addToRenamingMapping(streamSource.getAlias(), streamSource.getUIDStr());
		return true;
	}

	public void stopLoading ( ) {
		this.isActive = false;
		this.interrupt ( );
		for ( String configFile : Mappings.getAllKnownFileName ( ) ) {
			VirtualSensor sensorInstance = Mappings.getVSensorInstanceByFileName ( configFile );
			removeAllVSResources ( sensorInstance );
			logger.info( "Removing the resources associated with : " + sensorInstance.getConfig ( ).getFileName ( ) + " [done]." );
		}
		try {
			Main.getWindowStorage().shutdown( );
            Iterator<VSensorConfig> iter = Mappings.getAllVSensorConfigs();
            while (iter.hasNext()) {
                Main.getStorage(iter.next()).shutdown();
            }
		} catch ( SQLException e ) {
			logger.error(e.getMessage(),e);
		}
	}
}
