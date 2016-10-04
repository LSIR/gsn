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
* File: src/ch/epfl/gsn/Main.java
*
* @author parobert
* @author cl3m
* @author Jerome Rousselot
* @author gsn_devs
* @author Mehdi Riahi
* @author Ali Salehi
* @author Behnaz Bostanipour
* @author Timotee Maret
* @author Julien Eberle
*
*/

package ch.epfl.gsn;

import ch.epfl.gsn.config.GsnConf;
import ch.epfl.gsn.config.VsConf;
import ch.epfl.gsn.data.DataStore;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.SplashScreen;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import org.zeromq.ZContext;

import ch.epfl.gsn.ContainerImpl;
import ch.epfl.gsn.DataDistributer;
import ch.epfl.gsn.Main;
import ch.epfl.gsn.Mappings;
import ch.epfl.gsn.VSensorLoader;
import ch.epfl.gsn.beans.BeansInitializer;
import ch.epfl.gsn.beans.ContainerConfig;
import ch.epfl.gsn.beans.StorageConfig;
import ch.epfl.gsn.beans.VSensorConfig;
import ch.epfl.gsn.delivery.LocalDeliveryWrapper;
import ch.epfl.gsn.monitoring.MemoryMonitor;
import ch.epfl.gsn.monitoring.Monitorable;
import ch.epfl.gsn.monitoring.MonitoringServer;
import ch.epfl.gsn.networking.zeromq.ZeroMQDeliveryAsync;
import ch.epfl.gsn.networking.zeromq.ZeroMQDeliverySync;
import ch.epfl.gsn.networking.zeromq.ZeroMQProxy;
import ch.epfl.gsn.storage.SQLValidator;
import ch.epfl.gsn.storage.StorageManager;
import ch.epfl.gsn.storage.StorageManagerFactory;
import ch.epfl.gsn.storage.hibernate.DBConnectionInfo;
import ch.epfl.gsn.utils.ValidityTools;
import ch.epfl.gsn.vsensor.SQLValidatorIntegration;
import ch.epfl.gsn.wrappers.WrappersUtil;



public final class Main {
	
    public static final int        DEFAULT_MAX_DB_CONNECTIONS       = 8;
	public static final String     DEFAULT_GSN_CONF_FOLDER            = "../conf";
	public static final String     DEFAULT_VIRTUAL_SENSOR_FOLDER = "../virtual-sensors";
	public static transient Logger logger                           = LoggerFactory.getLogger ( Main.class );

	/**
	 * Mapping between the wrapper name (used in addressing of stream source)
	 * into the class implementing DataSource.
	 */
	private static  Properties                            wrappers ;
	private static Main                                   singleton ;
	public static String                                  gsnConfFolder          = DEFAULT_GSN_CONF_FOLDER;
	public static String                                  virtualSensorDirectory = DEFAULT_VIRTUAL_SENSOR_FOLDER;
	private static ZeroMQProxy                            zmqproxy;
	private static StorageManager                         mainStorage;
    private static StorageManager                         windowStorage;
    private static StorageManager                         validationStorage;
    private static ZContext                               zmqContext              = new ZContext();
    private static HashMap<Integer, StorageManager>       storages                = new HashMap<Integer, StorageManager>();
    private static HashMap<VSensorConfig, StorageManager> storagesConfigs         = new HashMap<VSensorConfig, StorageManager>();
    private ContainerConfig                               containerConfig;
    private MonitoringServer                              monitoringServer;
    private static VSensorLoader vsLoader; 
    private static GsnConf gsnConf;
    private static Map <String,VsConf> vsConf = new HashMap<String,VsConf>();
    private static ArrayList<Monitorable> toMonitor = new ArrayList<Monitorable>();
    
    
    /*
     *  Retrieving ThreadMXBean instance of JVM
     *  It would be used for monitoring CPU time of each virtual sensor
     */

    private static ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

    private Main() throws Exception {

		ValidityTools.checkAccessibilityOfFiles ( WrappersUtil.DEFAULT_WRAPPER_PROPERTIES_FILE , gsnConfFolder + "/gsn.xml");
		ValidityTools.checkAccessibilityOfDirs ( virtualSensorDirectory );
		containerConfig = loadContainerConfiguration();
		updateSplashIfNeeded(new String[] {"GSN is starting...", "All GSN logs are available at: logs/ch.epfl.gsn.log"});
		System.out.println("Global Sensor Networks (GSN) is starting...");
	
        int maxDBConnections = System.getProperty("maxDBConnections") == null ? DEFAULT_MAX_DB_CONNECTIONS : Integer.parseInt(System.getProperty("maxDBConnections"));
        int maxSlidingDBConnections = System.getProperty("maxSlidingDBConnections") == null ? DEFAULT_MAX_DB_CONNECTIONS : Integer.parseInt(System.getProperty("maxSlidingDBConnections"));

    	DataStore ds = new DataStore(gsnConf);

        mainStorage = StorageManagerFactory.getInstance(containerConfig.getStorage().getJdbcDriver ( ) , containerConfig.getStorage().getJdbcUsername ( ) , containerConfig.getStorage().getJdbcPassword ( ) , containerConfig.getStorage().getJdbcURL ( ) , maxDBConnections);
        
        StorageConfig sc = containerConfig.getSliding() != null ? containerConfig.getSliding().getStorage() : containerConfig.getStorage() ;
        windowStorage = StorageManagerFactory.getInstance(sc.getJdbcDriver ( ) , sc.getJdbcUsername ( ) , sc.getJdbcPassword ( ) , sc.getJdbcURL ( ), maxSlidingDBConnections);
        
        validationStorage = StorageManagerFactory.getInstance("org.h2.Driver", "sa", "", "jdbc:h2:mem:validator", Main.DEFAULT_MAX_DB_CONNECTIONS);

        logger.trace ( "The Container Configuration file loaded successfully." );
        
        // starting the monitoring socket
        toMonitor.add(new MemoryMonitor());
        monitoringServer = new MonitoringServer(containerConfig.getMonitorPort());
        monitoringServer.start();
        
		if (containerConfig.isZMQEnabled()){
			//start the 0MQ proxy
			zmqproxy = new ZeroMQProxy(containerConfig.getZMQProxyPort(),containerConfig.getZMQMetaPort());
		}
		
		VSensorLoader vsloader = VSensorLoader.getInstance ( virtualSensorDirectory );
		File vsDir=new File(virtualSensorDirectory);
		for (File f:vsDir.listFiles()){
			if (f.getName().endsWith(".xml")){
				VsConf vs= VsConf.load(f.getPath());
				vsConf.put(vs.name(), vs);
			}
		}
		Main.vsLoader = vsloader;

		vsloader.addVSensorStateChangeListener(new SQLValidatorIntegration(SQLValidator.getInstance()));
		vsloader.addVSensorStateChangeListener(DataDistributer.getInstance(LocalDeliveryWrapper.class));
		if (containerConfig.isZMQEnabled())
			vsloader.addVSensorStateChangeListener(DataDistributer.getInstance(ZeroMQDeliverySync.class));
		    vsloader.addVSensorStateChangeListener(DataDistributer.getInstance(ZeroMQDeliveryAsync.class));

		ContainerImpl.getInstance().addVSensorDataListener(DataDistributer.getInstance(LocalDeliveryWrapper.class));
		ContainerImpl.getInstance().addVSensorDataListener(DataDistributer.getInstance(ZeroMQDeliverySync.class));
		ContainerImpl.getInstance().addVSensorDataListener(DataDistributer.getInstance(ZeroMQDeliveryAsync.class));
		vsloader.startLoading();

	}

	private static void closeSplashIfneeded() {
		if (isHeadless())
			return;
		SplashScreen splash = SplashScreen.getSplashScreen();
		//Check if we have specified any splash screen
		if (splash == null) {
			return;
		}
		if (splash.isVisible())
			splash.close();
	}


	private static void updateSplashIfNeeded(String message[]) {
		boolean headless_check = isHeadless();

		if (!headless_check) {
			SplashScreen splash = SplashScreen.getSplashScreen();
			if (splash == null)
				return;
			if (splash.isVisible()) {
				//Get a graphics overlay for the splash screen
				Graphics2D g = splash.createGraphics();
				//Do some drawing on the graphics object
				//Now update to the splash screen

				g.setComposite(AlphaComposite.Clear);
				g.fillRect(0,0,400,70);
				g.setPaintMode();
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setColor(Color.BLACK);
				g.setFont(new Font("Arial",Font.BOLD,11));
				for (int i=0;i<message.length;i++)
					g.drawString(message[i], 13, 16*i+10);
				splash.update();
			}
		}
	}

	private static boolean isHeadless() {
		return GraphicsEnvironment.isHeadless();
	}

	public synchronized static Main getInstance() {
		if (singleton==null)
			try {
				singleton=new Main();
			} catch (Exception e) {
				logger.error(e.getMessage(),e);
				throw new RuntimeException(e);
			}
			return singleton;
	}

	public static void main(String[] args)  {
		if (args.length > 0) {
			Main.gsnConfFolder = args[0];
		}
		if (args.length > 1) {
			Main.virtualSensorDirectory = args[1];
		}
		updateSplashIfNeeded(new String[] {"GSN is trying to start.","All GSN logs are available at: logs/gsn.log"});
		Runtime.getRuntime().addShutdownHook(new Thread()
	        {
	            @Override
	            public void run()
	            {
	                System.out.println("GSN is stopping...");
	                new Thread(new Runnable() {
	    				public void run() {
	    					try {
	    						Thread.sleep(10000);
	    					} catch (InterruptedException e) {
	    					}finally {
	    						logger.warn("Forced exit...");
	    						System.out.println("GSN is stopped (forced).");
	    						Runtime.getRuntime().halt(1);
	    					}
	    				}}).start();

	    			try {
	    				logger.info("Shutting down GSN...");
	    				if (vsLoader != null) {
	    					vsLoader.stopLoading();
	    					logger.info("All virtual sensors have been stopped, shutting down virtual machine.");
	    				} else {
	    					logger.warn("Could not shut down virtual sensors properly. We are probably exiting GSN before it has been completely initialized.");
	    				}
	    			} catch (Exception e) {
	    				logger.warn("Error while reading from or writing to control connection: " + e.getMessage(), e);
	    			}finally {
	    				System.out.println("GSN is stopped.");
	    			}
	            }
	        });
		
		try {
			Main.getInstance();
		}catch (Exception e) {
			logger.error(e.getMessage(), e);
			updateSplashIfNeeded(new String[] {"Starting GSN failed! Look at logs/gsn.log for more information."});
			try {
				Thread.sleep(4000);
			} catch (InterruptedException e1) {}
		}
		closeSplashIfneeded();
	}

	public static ContainerConfig loadContainerConfiguration() {
		ValidityTools.checkAccessibilityOfFiles (WrappersUtil.DEFAULT_WRAPPER_PROPERTIES_FILE , gsnConfFolder + "/gsn.xml");
		ValidityTools.checkAccessibilityOfDirs (virtualSensorDirectory);
		ContainerConfig toReturn = null;
		try {
			toReturn = loadContainerConfig (gsnConfFolder + "/gsn.xml");
			logger.info ( "Loading wrappers.properties at : " + WrappersUtil.DEFAULT_WRAPPER_PROPERTIES_FILE);
			wrappers = WrappersUtil.loadWrappers(new HashMap<String, Class<?>>());
			logger.info ( "Wrappers initialization ..." );
		} catch (ClassNotFoundException e) {
			logger.error ("The file wrapper.properties refers to one or more classes which don't exist in the classpath"+ e.getMessage());
			System.exit (1);
		}
		return toReturn;

	}

	public static ContainerConfig loadContainerConfig (String gsnXMLpath) throws ClassNotFoundException {
		if (!new File(gsnXMLpath).isFile()) {
			logger.error("Couldn't find the gsn.xml file @: "+(new File(gsnXMLpath).getAbsolutePath()));
			System.exit(1);
		}		
		GsnConf gsn = GsnConf.load(gsnXMLpath);
		gsnConf = gsn;
		ContainerConfig conf=BeansInitializer.container(gsn);
		Class.forName(conf.getStorage().getJdbcDriver());
		conf.setContainerConfigurationFileName (  gsnXMLpath );
		return conf;
	}

	public static Properties getWrappers()  {
		if (singleton==null )
			return WrappersUtil.loadWrappers(new HashMap<String, Class<?>>());
		return Main.wrappers;
	}
    
	public  static Class < ? > getWrapperClass ( String id ) {
		try {
			String className =  getWrappers().getProperty(id);
			if (className ==null) {
				logger.error("The requested wrapper: "+id+" doesn't exist in the wrappers.properties file.");
				return null;
			}

			return Class.forName(className);
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(),e);
		}
		return null;
	}

	/**
	 * Get's the GSN configuration without starting GSN.
	 * @return
	 * @throws Exception
	 */
	public static ContainerConfig getContainerConfig() {
		if (singleton == null)
			try {
				return loadContainerConfig(Main.gsnConfFolder + "/gsn.xml");
			} catch (Exception e) {
				return null;
			}
			else
				return singleton.containerConfig;
	}

    public static StorageManager getValidationStorage() {
        return validationStorage;
    }

    public static StorageManager getStorage(VSensorConfig config) {
        StorageManager sm = storagesConfigs.get(config == null ? null : config);
        if  (sm != null)
            return sm;

        DBConnectionInfo dci = null;
        if (config == null || config.getStorage() == null || !config.getStorage().isDefined()) {
            sm = mainStorage;
        } else {
            if (config.getStorage().isIdentifierDefined()) {
                throw new IllegalArgumentException("Identifiers for storage is not supported yet.");
            } else {
                dci = new DBConnectionInfo(config.getStorage().getJdbcDriver(),
                        config.getStorage().getJdbcURL(),
                        config.getStorage().getJdbcUsername(),
                        config.getStorage().getJdbcPassword());
            }
            sm = storages.get(dci.hashCode());
            if (sm == null) {
                sm = StorageManagerFactory.getInstance(config.getStorage().getJdbcDriver(), config.getStorage().getJdbcUsername(), config.getStorage().getJdbcPassword(), config.getStorage().getJdbcURL(), DEFAULT_MAX_DB_CONNECTIONS);
                storages.put(dci.hashCode(), sm);
                storagesConfigs.put(config, sm);
            }
        }
        return sm;

    }

    public static StorageManager getStorage(String vsName) {
        return getStorage(Mappings.getVSensorConfig(vsName));
    }

    public static StorageManager getDefaultStorage() {
        return getStorage((VSensorConfig)null);
    }

    public static StorageManager getWindowStorage() {
        return windowStorage;
    }
    
    public static ZContext getZmqContext(){
    	return zmqContext;
    }
    
    public static ZeroMQProxy getZmqProxy(){
    	return zmqproxy;
    }
    public GsnConf getGsnConf(){
    	return gsnConf;
    }
    public Map<String,VsConf> getVsConf(){
    	return vsConf;
    }
    public ArrayList<Monitorable> getToMonitor(){
    	return toMonitor;
    }
    
    public static ThreadMXBean getThreadMXBean() {
        return threadBean;
    }

}


