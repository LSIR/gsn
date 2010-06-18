package gsn.wrappers;

import java.io.Serializable;
import java.sql.SQLException;

import javax.naming.OperationNotSupportedException;

import org.apache.log4j.Logger;

import gsn.storage.StorageManager;
import gsn.wrappers.backlog.BackLogMessageMultiplexer;
import gsn.wrappers.backlog.plugins.AbstractPlugin;
import gsn.beans.AddressBean;
import gsn.beans.DataField;


/**
 * Offers the main backlog functionality needed on the GSN side.
 * <p>
 * First the BackLogWrapper starts a plugin (specified in the
 * virtual sensor's XML file under 'plugin-classname') which
 * offers the functionality to process a specific backlog
 * message type, e.g. MIG messages.
 * <p>
 * Second, the BackLogWrapper starts or reuses a
 * {@link DeploymentClient} which opens a connection to the
 * deployment address specified in the virtual sensor's XML file
 * under 'deployment'. It then registers itself to listen to the
 * DeploymentClient for the backlog message type specified in the
 * used plugin.
 * <p>
 * If the optional parameter 'local-sf-port' is specified in the
 * virtual sensor's XML file, the BackLogWrapper starts a local
 * serial forwarder ({@link SFListen}) and registers it to listen
 * for the MIG {@link BackLogMessage} type at the
 * DeploymentClient. Thus, all incoming MIG packets will
 * be forwarded to any SF listener. This guarantees any SF
 * listeners to receive all MIG packets produced at the deployment,
 * as it passively benefits from the backlog functionality.
 *
 * @author	Tonio Gsell
 * 
 * @see BackLogMessage
 * @see AbstractPlugin
 * @see BackLogMessageMultiplexer
 * @see SFListen
 */
public class BackLogWrapper extends AbstractWrapper {

	// Mandatory Parameter
	/**
	 * The field name for the deployment as used in the virtual sensor's XML file.
	 */
	private static final String BACKLOG_PLUGIN = "plugin-classname";
	
	private BackLogMessageMultiplexer blMsgMultiplexer = null;
	private Object sync = new Object();
	private String plugin = null;
	private AbstractPlugin pluginObject = null;
	private AddressBean addressBean = null;
	private static StorageManager storage = null;
    private static int threadCounter = 0;
	
	private final transient Logger logger = Logger.getLogger( BackLogWrapper.class );



	/**
	 * Initializes the BackLogWrapper. This function is called by GSN.
	 * <p>
	 * Checks the virtual sensor's XML file for the availability of
	 * 'deployment' and 'plugin-classname' fields.
	 * 
	 * Instantiates and initializes the specified plugin.
	 * 
	 * Starts or reuses a DeploymentClient and registers itself to
	 * it as listener for the BackLogMessage type specified by the
	 * used plugin.
	 * 
	 * If the optional parameter 'local-sf-port' is specified in the
	 * virtual sensor's XML file, the BackLogWrapper starts a local
	 * serial forwarder (SFListen) and registers it to listen for the
	 * MIG BackLogMessage type at the DeploymentClient. Thus, all
	 * incoming MIG packets will be forwarded to any SF listener.
	 * 
	 * @return true if the initialization was successful otherwise
	 * 			 false.
	 */
	@Override
	public boolean initialize() {
		if (logger.isDebugEnabled())
			logger.debug("BackLog wrapper initialize started...");
		
	    addressBean = getActiveAddressBean();

		String deployment = addressBean.getVirtualSensorName().split("_")[0].toLowerCase();
		String coreStationAddress = null;
		try {
			coreStationAddress = addressBean.getPredicateValueWithException("remote-connection");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return false;
		}
		
		synchronized (sync) {
			if (storage == null) {
				try {
					StorageManager sm = getStorageManager();
					if (StorageManager.getDatabaseForConnection(sm.getConnection()) == StorageManager.DATABASE.MYSQL) {
						sm.executeUpdate(new StringBuilder("DROP FUNCTION IF EXISTS to_tinyint"));
						sm.executeUpdate(new StringBuilder("CREATE FUNCTION to_tinyint(number bigint) RETURNS tinyint BEGIN return number; END"));
						sm.executeUpdate(new StringBuilder("DROP FUNCTION IF EXISTS to_smallint"));
						sm.executeUpdate(new StringBuilder("CREATE FUNCTION to_smallint(number bigint) RETURNS smallint BEGIN return number; END"));
						sm.executeUpdate(new StringBuilder("DROP FUNCTION IF EXISTS to_integer"));
						sm.executeUpdate(new StringBuilder("CREATE FUNCTION to_integer(number bigint) RETURNS integer BEGIN return number; END"));
						storage = sm;
					}
					else {
						logger.error("Currently only MySQL supported for function creation -> this may lead to missbehaviour!");
					}
				} catch (SQLException e) {
					logger.error("Could not add SQL cast functions", e);
					return false;
				}
			}
		}
		
		try {
			blMsgMultiplexer = BackLogMessageMultiplexer.getInstance(deployment, coreStationAddress);
		} catch (Exception e) {
			logger.error(e.getMessage());
			return false;
		}
		
		plugin = addressBean.getPredicateValue(BACKLOG_PLUGIN);
		
	    // check the XML file for the plugin entry
		if (plugin == null) {
			logger.error("Loading the PSBackLog wrapper failed due to missing >" + BACKLOG_PLUGIN + "< predicate.");
			return false;
	    }
		
		// instantiating the plugin class specified in the XML file
		if (logger.isDebugEnabled())
			logger.debug("Loading BackLog plugin: >" + plugin + "<");
		try {
			Class<?> cl = Class.forName(plugin);
			pluginObject = (AbstractPlugin) cl.getConstructor().newInstance();
		} catch (ClassNotFoundException e) {
			logger.error("The plugin class >" + plugin + "< could not be found");
			return false;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return false;
		}
		
		
		// initializing the plugin
        if( !pluginObject.initialize(this, blMsgMultiplexer.getCoreStationName(), deployment) ) {
    		logger.error("Could not load BackLog plugin: >" + plugin + "<");
        	return false;
        }
        
        setName("DiskSpaceWrapper-Thread" + (++threadCounter));
		
		return true;
	}
	
	
	
	@Override
	public void run() {
		synchronized (blMsgMultiplexer) {
			if (!blMsgMultiplexer.isAlive())
				blMsgMultiplexer.start();
		}

		if (logger.isDebugEnabled())
			logger.debug("Starting BackLog plugin: >" + plugin + "<");
		pluginObject.start();
	}




	/**
	 * This function can be called by the plugin, if it has processed
	 * the data. The data will be forwarded to the corresponding
	 * virtual sensor by GSN and will be put into the database.
	 * <p>
	 * The data format must correspond to the one specified by
	 * the plugin's getOutputFormat() function.
	 * 
	 * @param timestamp
	 * 			The timestamp in milliseconds this data has been
	 * 			generated.
	 * @param data 
	 * 			The data to be processed. Its format must correspond
	 * 			to the one specified by the plugin's getOutputFormat()
	 * 			function.
	 * @return false if storing the new item fails otherwise true
	 */
	public boolean dataProcessed(long timestamp, Serializable... data) {
		if (logger.isDebugEnabled())
			logger.debug("dataProcessed timestamp: " + timestamp);
		return postStreamElement(timestamp, data);
	}
	
	
	public BackLogMessageMultiplexer getBLMessageMultiplexer() {
		return blMsgMultiplexer;
	}





	/**
	 * Returns the output format specified by the used plugin.
	 * 
	 * This function is needed by GSN.
	 * 
	 * @return the output format for the used plugin.
	 */
	@Override
	public DataField[] getOutputFormat() {
		return pluginObject.getOutputFormat();
	}





	/**
	 * Returns the name of this wrapper.
	 * 
	 * This function is needed by GSN.
	 * 
	 * @return the name of this wrapper.
	 */
	@Override
	public String getWrapperName() {
		return "BackLogWrapper";
	}




	/**
	 * This function can be only called by a virtual sensor to send
	 * a command to the plugin.
	 * <p>
	 * The code in the virtual sensor's class would be something like:
	 * <p>
	 * <ul>
	 *  {@code vsensor.getInputStream( INPUT_STREAM_NAME ).getSource( STREAM_SOURCE_ALIAS_NAME ).getWrapper( ).sendToWrapper(command, paramNames, paramValues)}
	 * </ul>
	 * 
	 * @param action the action name
	 * @param paramNames the name of the different parameters
	 * @param paramValues the different parameter values
	 * 
	 * @return true if the plugin could successfully process the
	 * 			 data otherwise false
	 * @throws OperationNotSupportedException 
	 */
	@Override
	public boolean sendToWrapper ( String action , String [ ] paramNames , Object [ ] paramValues ) throws OperationNotSupportedException {
		Integer id = 65535;
		for (int i = 0 ; i < paramNames.length ; i++) {
			if ( paramNames[i].compareToIgnoreCase("core_station") == 0 ) {
				try {
					id = Integer.parseInt((String) paramValues[i]);
				} catch (NumberFormatException e) {
					logger.error("The device_id in the core station field has to be an integer.");
					return false;
				}
			}
		}
		
		if (id < 0 || id > 65535) {
			logger.error("device_id has to be a number between 0 and 65535 (inclusive)");
			return false;
		}
		
		if (blMsgMultiplexer.getDeviceID() == null) {
			logger.warn("no device id from core station (" + blMsgMultiplexer.getCoreStationName() + ") determined yet (no connection since last GSN start)");
			return pluginObject.sendToPlugin(action, paramNames, paramValues);
		}
		if ( id == blMsgMultiplexer.getDeviceID() || id == 65535) {
			if (logger.isDebugEnabled())
				logger.debug("Upload command received.");
			return pluginObject.sendToPlugin(action, paramNames, paramValues);
		}
		
		return true;
	}




	/**
	 * This function can be only called by a virtual sensor to send
	 * an object to the plugin.
	 * <p>
	 * The code in the virtual sensor's class would be something like:
	 * <p>
	 * <ul>
	 *  {@code vsensor.getInputStream( INPUT_STREAM_NAME ).getSource( STREAM_SOURCE_ALIAS_NAME ).getWrapper( ).sendToWrapper(dataItem)}
	 * </ul>
	 * 
	 * @param dataItem which is going to be sent to the plugin
	 * 
	 * @return true if the plugin could successfully process the
	 * 			 data otherwise false
	 * @throws OperationNotSupportedException 
	 */
	@Override
	public boolean sendToWrapper ( Object dataItem ) throws OperationNotSupportedException {
		if (logger.isDebugEnabled())
			logger.debug("Upload object received.");
		return pluginObject.sendToPlugin(dataItem);
	}

	

	/**
	 * Disposes this BackLogWrapper. This function is called by GSN.
	 * <p>
	 * Disposes the used plugin and deregisters itself from the 
	 * DeploymentClient.
	 *
	 * If this is the last BackLogWrapper for the used deployment,
	 * the DeploymentClient will be finalized, as well as an optional
	 * serial forwarder if needed.
	 */
	@Override
	public void dispose() {
		logger.info("dispose");
		
		// tell the plugin to stop
		pluginObject.dispose();
		try {
			pluginObject.join();
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
        threadCounter--;
	}
	
	
	@Override
   	public boolean isTimeStampUnique() {
   		return false;
   	}
}
