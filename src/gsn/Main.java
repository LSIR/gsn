package gsn;

import gsn.beans.ContainerConfig;
import gsn.beans.VSensorConfig;
import gsn.control.VSensorLoader;
import gsn.pid.PIDUtils;
import gsn.storage.StorageManager;
import gsn.utils.ValidityTools;
import gsn.vsensor.Container;
import gsn.vsensor.ContainerImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public final class Main {

    public static final String DEFAULT_GSN_LOG4J_PROPERTIES = "conf/log4j.properties";

    public static transient Logger logger;

    public static final String DEFAULT_WRAPPER_PROPERTIES_FILE = "conf/wrappers.properties";

    public static final String DEFAULT_GSN_CONF_FILE = "conf/gsn.xml";

    public static final String DEFAULT_VIRTUAL_SENSOR_DIRECTORY = "virtual-sensors";

    public static final String DEFAULT_WEB_APP_PATH = "webapp";

    private static File pidFile;

    public static void main(String[] args) throws IOException, RuntimeException {
	ValidityTools.checkAccessibilityOfFiles(DEFAULT_GSN_LOG4J_PROPERTIES,
		DEFAULT_WRAPPER_PROPERTIES_FILE, DEFAULT_GSN_CONF_FILE);
	ValidityTools.checkAccessibilityOfDirs(
		DEFAULT_VIRTUAL_SENSOR_DIRECTORY, DEFAULT_WEB_APP_PATH);
	PropertyConfigurator.configure(DEFAULT_GSN_LOG4J_PROPERTIES);
	logger = Logger.getLogger(Main.class);
	if (PIDUtils.isPIDExist(PIDUtils.GSN_PID)) {
	    System.out.println("Error : Another GSN Server is running.");
	    System.exit(1);
	} else
	    pidFile = PIDUtils.createPID(PIDUtils.GSN_PID);

	try {
	    initialize("conf/gsn.xml");
	} catch (JiBXException e) {
	    logger.error(e.getMessage());
	    logger.error(new StringBuilder().append(
		    "Can't parse the GSN configuration file : ")
		    .append(args[0]).toString());
	    logger
		    .error("Please check the syntax of the file to be sure it is compatible with the requirements.");
	    logger
		    .error("You can find a sample configuration file from the GSN release.");
	    if (logger.isDebugEnabled())
		logger.debug(e.getMessage(), e);
	    System.exit(1);
	} catch (FileNotFoundException e) {
	    logger.error(new StringBuilder().append(
		    "The the configuration file : ").append(args[0]).append(
		    " doesn't exist.").toString());
	    logger.error(e.getMessage());
	    logger
		    .error("Check the path of the configuration file and try again.");
	    if (logger.isDebugEnabled())
		logger.debug(e.getMessage(), e);
	    System.exit(1);
	}

	StorageManager.getInstance()
		.initialize(containerConfig.getJdbcDriver(),
			containerConfig.getJdbcUsername(),
			containerConfig.getJdbcPassword(),
			containerConfig.getJdbcURL());
	if (logger.isInfoEnabled())
	    logger
		    .info("The Container Configuration file loaded successfully.");
	Container container = new ContainerImpl();
	Mappings.setContainer(container);

	final Server server = new Server();
	Connector connector = new SelectChannelConnector();
	connector.setPort(containerConfig.getContainerPort());
	server.setConnectors(new Connector[] { connector });
	ServletHandler servletHandler = new ServletHandler();
	servletHandler.addServletWithMapping("gsn.vsensor.ContainerImpl",
		"/gsn");
	WebAppContext webAppContext = new WebAppContext();
	webAppContext.setContextPath("/");
	webAppContext.setResourceBase(DEFAULT_WEB_APP_PATH);
	webAppContext.setServletHandler(servletHandler);
	server.setHandler(webAppContext);
	server.setStopAtShutdown(true);
	server.setSendServerVersion(false);
	try {
	    server.start();
	} catch (Exception e) {
	    logger
		    .error("Start of the HTTP server failed. The HTTP protocol is used in most of the communications.");
	    logger.error(e.getMessage(), e);
	    System.exit(1);
	}
	final VSensorLoader vsloader = new VSensorLoader(
		DEFAULT_VIRTUAL_SENSOR_DIRECTORY);
	Thread shutdown = new Thread(new Runnable() {
	    public void run() {
		try {
		    while (true) {
			int value = PIDUtils.getFirstIntFrom(pidFile);
			if (value != '0')
			    Thread.sleep(2500);
			else
			    break;
		    }
		    logger.warn("Shutdown request received.");
		    vsloader.stopPlease();
		    server.stop();
		 } catch (Exception e) {
		    logger.warn("Shutdowning the webserver failed.", e);
		    System.exit(1);
		}
		logger.warn("GSN server is stopped.");
		System.exit(0);
	    }
	});
	shutdown.start();
    }

    /**
         * Mapping between the wrapper name (used in addressing of stream
         * source) into the class implementing DataSource.
         */
    private static final HashMap<String, Class<?>> wrappers = new HashMap<String, Class<?>>();

    private static ContainerConfig containerConfig;

    private static HashMap<String, VSensorConfig> virtualSensors;

    private static void initialize(String containerConfigurationFileName)
	    throws JiBXException, FileNotFoundException {
	containerConfig = loadConfiguration(containerConfigurationFileName);
	containerConfig
		.setContainerConfigurationFileName(containerConfigurationFileName);
	if (logger.isInfoEnabled())
	    logger.info(new StringBuilder().append(
		    "Loading wrappers.properties at : ").append(
		    DEFAULT_WRAPPER_PROPERTIES_FILE).toString());
	Configuration config = null;

	try {// Trying to load the wrapper specified in the configuration
	    // file of
	    // the container.
	    config = new PropertiesConfiguration(
		    DEFAULT_WRAPPER_PROPERTIES_FILE);
	} catch (ConfigurationException e) {
	    logger
		    .error("The wrappers configuration file's syntax is not compatible.");
	    logger.error(new StringBuilder().append("Check the :").append(
		    DEFAULT_WRAPPER_PROPERTIES_FILE).append(
		    " file and make sure it's syntactically correct.")
		    .toString());
	    logger
		    .error("Sample wrappers extention properties file is provided in GSN distribution.");
	    logger.error(e.getMessage(), e);
	    System.exit(1);
	}
	// Adding the wrappers to the GSN data structures.
	String wrapperNames[] = config.getStringArray("wrapper.name");
	String wrapperClasses[] = config.getStringArray("wrapper.class");
	if (logger.isInfoEnabled())
	    logger.info("Wrappers initialization ...");
	for (int i = 0; i < wrapperNames.length; i++) {
	    String name = wrapperNames[i];
	    String className = wrapperClasses[i];
	    try {
		if (wrappers.get(name) != null) {
		    logger
			    .error("The wrapper name : "
				    + name
				    + " is used more than once in the properties file.");
		    logger.error(new StringBuilder()
			    .append("Please check the ").append(
				    DEFAULT_WRAPPER_PROPERTIES_FILE).append(
				    " file and try again.").toString());
		    System.exit(1);
		}
		wrappers.put(name, Class.forName(className));
	    } catch (ClassNotFoundException e) {
		logger.error(new StringBuilder().append(
			"Can't find the class associated with the wrapper : ")
			.append(name).toString());
		logger.error(new StringBuilder().append("Check the ").append(
			DEFAULT_WRAPPER_PROPERTIES_FILE).append(
			" file and try again.").toString());
		logger.error(e.getMessage(), e);
		System.exit(1);
	    }
	    if (logger.isInfoEnabled())
		logger.info(new StringBuilder().append("Wrapper [")
			.append(name).append("] added successfully.")
			.toString());
	}
    }

    public static ContainerConfig loadConfiguration(
	    String containerConfigurationFileName) throws JiBXException,
	    FileNotFoundException {
	return loadConfiguration(new File(containerConfigurationFileName));
    }

    public static ContainerConfig loadConfiguration(
	    File containerConfigurationFileName) throws JiBXException,
	    FileNotFoundException {
	IBindingFactory bfact = BindingDirectory
		.getFactory(ContainerConfig.class);
	IUnmarshallingContext uctx = bfact.createUnmarshallingContext();
	return (ContainerConfig) uctx.unmarshalDocument(new FileInputStream(
		containerConfigurationFileName), null);
    }

    public static Class<?> getWrapperClass(String id) {
	return wrappers.get(id);
    }

    public final HashMap<String, VSensorConfig> getVirtualSensors() {
	return virtualSensors;
    }

    public static boolean justConsumes() {
	Iterator<VSensorConfig> vsconfigs = virtualSensors.values().iterator();
	while (vsconfigs.hasNext())
	    if (!vsconfigs.next().needsStorage())
		return false;
	return true;
    }

    public static ContainerConfig getContainerConfig() {
	return containerConfig;
    }

    public static String randomTableNameGenerator(int length) {
	byte oneCharacter;
	StringBuffer result = new StringBuffer(length);
	for (int i = 0; i < length; i++) {
	    oneCharacter = (byte) ((Math.random() * ('z' - 'a' + 1)) + 'a');
	    result.append((char) oneCharacter);
	}
	return result.toString();
    }

    public static String tableNameGenerator() {
	return randomTableNameGenerator(15);
    }

    /**
         * @param containerConfig
         *                The containerConfig to set.
         */
    public static void setContainerConfig(ContainerConfig containerConfig) {
	if (Main.containerConfig == null) {
	    Main.containerConfig = containerConfig;
	} else {
	    throw new RuntimeException(
		    "Trying to replace the container config object in main class.");
	}
    }

}
