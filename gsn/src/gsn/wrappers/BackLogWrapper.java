package gsn.wrappers;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.naming.OperationNotSupportedException;

import org.apache.log4j.Logger;

import gsn.wrappers.backlog.BackLogMessage;
import gsn.wrappers.backlog.DeploymentClient;
import gsn.wrappers.backlog.BackLogMessageListener;
import gsn.wrappers.backlog.plugins.AbstractPlugin;
import gsn.wrappers.backlog.sf.SFListen;
import gsn.wrappers.backlog.sf.SFv1Listen;
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
 * @see DeploymentClient
 * @see SFListen
 */
public class BackLogWrapper extends AbstractWrapper implements BackLogMessageListener {

	// Mandatory Parameter
	/**
	 * The field name for the deployment as used in the virtual sensor's XML file.
	 */
	public static final String BACKLOG_DEPLOYMENT = "deployment";
	private static final String BACKLOG_PLUGIN = "plugin-classname";
	private static final String SF_LOCAL_PORT = "local-sf-port";
	private static final String TINYOS1X_PLATFORM_NAME = "tinyos1x-platformName";
	
	
	
	private static int blWrapperthreadCounter = 0;
	private AbstractPlugin pluginObject = null;
	private AddressBean addressBean = null;
	private static Map<String,DeploymentClient> deploymentClientList = new HashMap<String,DeploymentClient> ();
	private DeploymentClient deploymentClient = null;
	private static Map<String,SFListen> sfListenList = new HashMap<String,SFListen> ();
	private static Map<String,SFv1Listen> sfv1ListenList = new HashMap<String,SFv1Listen> ();
	private SFListen sfListen = null;
	private SFv1Listen sfv1Listen = null;
	private static Map<String,Integer> activePluginsCounterList = new HashMap<String,Integer> ();
	private static Map<String,Semaphore> deploymentSemaphoreList = new HashMap<String,Semaphore> ();
	private Semaphore deploymentSemaphore = null;
	
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
		logger.info("BackLog wrapper initialize started...");

	    addressBean = getActiveAddressBean( );

	    // check the XML file for the basestation entry
		if ( addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ) == null ) {
			logger.error("Loading the PSBackLog wrapper failed due to missing *" + BACKLOG_DEPLOYMENT + "* parameter.");
			return false;
	    }
	    // check the XML file for the plugin entry
		if ( addressBean.getPredicateValue( BACKLOG_PLUGIN ) == null ) {
			logger.error("Loading the PSBackLog wrapper failed due to missing *" + BACKLOG_PLUGIN + "* parameter.");
			return false;
	    }
		
		// create SQL functions which are used to unpack some MIG messages
		try {
			getStorageManager().executeUpdate(new StringBuilder("DROP FUNCTION IF EXISTS to_tinyint"));
			getStorageManager().executeUpdate(new StringBuilder("CREATE FUNCTION to_tinyint(number bigint) RETURNS tinyint BEGIN return number; END"));

			getStorageManager().executeUpdate(new StringBuilder("DROP FUNCTION IF EXISTS to_smallint"));
			getStorageManager().executeUpdate(new StringBuilder("CREATE FUNCTION to_smallint(number bigint) RETURNS smallint BEGIN return number; END"));

			getStorageManager().executeUpdate(new StringBuilder("DROP FUNCTION IF EXISTS to_integer"));
			getStorageManager().executeUpdate(new StringBuilder("CREATE FUNCTION to_integer(number bigint) RETURNS integer BEGIN return number; END"));

			getStorageManager().executeUpdate(new StringBuilder("DROP FUNCTION IF EXISTS real_humidity"));
			getStorageManager().executeUpdate(new StringBuilder("CREATE FUNCTION real_humidity(humidity integer, real_temp double) RETURNS double BEGIN DECLARE rel_humidity double; SET rel_humidity = ((real_temp - 25) * (0.01 + (0.00008 * humidity))) + (-4 + (0.0405 * humidity) - 0.0000028 * POW(humidity,2)); SET rel_humidity = if(rel_humidity > 99, 100, rel_humidity); SET rel_humidity = if(rel_humidity < 0, 0, rel_humidity); return rel_humidity; END"));

			getStorageManager().executeUpdate(new StringBuilder("DROP FUNCTION IF EXISTS real_temperature"));
			getStorageManager().executeUpdate(new StringBuilder("CREATE FUNCTION real_temperature(temp integer) RETURNS double BEGIN return temp * 0.01 - 39.63; END"));

			getStorageManager().executeUpdate(new StringBuilder("DROP FUNCTION IF EXISTS real_sys_voltage"));
			getStorageManager().executeUpdate(new StringBuilder("CREATE FUNCTION real_sys_voltage(sys_voltage integer) RETURNS double BEGIN return sys_voltage  * (2.56/65536) * (39/24); END"));

			getStorageManager().executeUpdate(new StringBuilder("DROP FUNCTION IF EXISTS real_sdi_voltage"));
			getStorageManager().executeUpdate(new StringBuilder("CREATE FUNCTION real_sdi_voltage(sdi_voltage integer) RETURNS double BEGIN return sdi_voltage  * (2.56/65536) * (204/24); END"));

			getStorageManager().executeUpdate(new StringBuilder("DROP FUNCTION IF EXISTS real_msp_temperature"));
			getStorageManager().executeUpdate(new StringBuilder("CREATE FUNCTION real_msp_temperature(msp_temperature integer) RETURNS double BEGIN return (msp_temperature  * (1.5/4095) - 0.986) / 0.00355; END"));

			getStorageManager().executeUpdate(new StringBuilder("DROP FUNCTION IF EXISTS real_msp_voltage"));
			getStorageManager().executeUpdate(new StringBuilder("CREATE FUNCTION real_msp_voltage(msp_voltage integer) RETURNS double BEGIN return msp_voltage  * (1.5/4095) * 2; END"));

			getStorageManager().executeUpdate(new StringBuilder("DROP FUNCTION IF EXISTS real_sib_current"));
			getStorageManager().executeUpdate(new StringBuilder("CREATE FUNCTION real_sib_current(sib_current integer) RETURNS double BEGIN return sib_current  * (2.56/65536) * (10/0.15); END"));

			getStorageManager().executeUpdate(new StringBuilder("DROP FUNCTION IF EXISTS boottime"));
			getStorageManager().executeUpdate(new StringBuilder("CREATE FUNCTION boottime(ptime bigint, atime_low integer, atime_high smallint, uptime_low integer, uptime_high smallint) RETURNS varchar(20) BEGIN return FROM_UNIXTIME((ptime - ((atime_low | atime_high << 16) + (uptime_low | uptime_high << 16)) * 1000)/1000, '%d/%m/%Y %T'); END"));

			getStorageManager().executeUpdate(new StringBuilder("DROP FUNCTION IF EXISTS uptime"));
			getStorageManager().executeUpdate(new StringBuilder("CREATE FUNCTION uptime(uptime_low integer, uptime_high smallint) RETURNS varchar(20) BEGIN DECLARE uptime_total INTEGER; DECLARE days INTEGER; SET uptime_total = (uptime_low | uptime_high << 16); SET days = FLOOR(uptime_total/86400); return if(days > 0, concat(cast(days as CHAR), 'd ',SEC_TO_TIME(uptime_total-(days*86400))), SEC_TO_TIME(uptime_total)); END"));

			getStorageManager().executeUpdate(new StringBuilder("DROP FUNCTION IF EXISTS timestr"));
			getStorageManager().executeUpdate(new StringBuilder("CREATE FUNCTION timestr(time_sec bigint) RETURNS varchar(20) BEGIN return FROM_UNIXTIME(time_sec/1000, '%d/%m/%Y %T'); END"));

			getStorageManager().executeUpdate(new StringBuilder("DROP FUNCTION IF EXISTS mv_to_v"));
			getStorageManager().executeUpdate(new StringBuilder("CREATE FUNCTION mv_to_v(mv integer) RETURNS varchar(10) BEGIN return CONCAT(ROUND(mv/1000, 3), ' V'); END"));

			getStorageManager().executeUpdate(new StringBuilder("DROP FUNCTION IF EXISTS ua_to_ma"));
			getStorageManager().executeUpdate(new StringBuilder("CREATE FUNCTION ua_to_ma(ua integer) RETURNS varchar(10) BEGIN return CONCAT(ROUND(ua/1000, 3), ' mA'); END"));

			getStorageManager().executeUpdate(new StringBuilder("DROP FUNCTION IF EXISTS ua_to_a"));
			getStorageManager().executeUpdate(new StringBuilder("CREATE FUNCTION ua_to_a(ua integer) RETURNS varchar(10) BEGIN return CONCAT(ROUND(ua/1000000, 3), ' A'); END"));
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
			return false;
		}
		
		// instantiating the plugin class specified in the XML file
		logger.info("Loading BackLog plugin: >" + addressBean.getPredicateValue( BACKLOG_PLUGIN ) + "<");
		try {
			Class<?> cl = Class.forName(addressBean.getPredicateValue( BACKLOG_PLUGIN ));
			pluginObject = (AbstractPlugin) cl.getConstructor().newInstance();
		} catch (ClassNotFoundException e) {
			logger.error("The BackLog plugin class >" + addressBean.getPredicateValue( BACKLOG_PLUGIN ) + "< could not be found");
			return false;
		} catch (IllegalArgumentException e) {
			logger.error(e.getMessage(), e);
			return false;
		} catch (SecurityException e) {
			logger.error(e.getMessage(), e);
			return false;
		} catch (InstantiationException e) {
			logger.error(e.getMessage(), e);
			return false;
		} catch (IllegalAccessException e) {
			logger.error(e.getMessage(), e);
			return false;
		} catch (InvocationTargetException e) {
			logger.error(e.getMessage(), e);
			return false;
		} catch (NoSuchMethodException e) {
			logger.error(e.getMessage(), e);
			return false;
		}
		
		
		// initializing the plugin
        if( !pluginObject.initialize(this) ) {
    		logger.error("Could not load BackLog plugin: >" + addressBean.getPredicateValue( BACKLOG_PLUGIN ) + "<");
        	return false;
        }

		
		// create or reuse a semaphore for this deployment
		synchronized (deploymentSemaphoreList) {
			if (deploymentSemaphoreList.containsKey(addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ))) {
				deploymentSemaphore = deploymentSemaphoreList.get(addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ));
			}
			else {
				deploymentSemaphore = new Semaphore(1);
				deploymentSemaphoreList.put(addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ), deploymentSemaphore);
			}

	        // set the thread name
			setName( "BackLogWrapper-Thread:" + ( ++blWrapperthreadCounter ) );
		}

		
		// start or reuse a deployment client
		synchronized (deploymentClientList) {
			if (deploymentClientList.containsKey(addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ))) {
				logger.info("Reusing existing DeploymentClient for deployment: >" + addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ) + "<");
				deploymentClient = deploymentClientList.get(addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ));
				deploymentClient.registerListener(pluginObject.getMessageType(), this);
			}
			else {
				try {
					logger.info("Loading DeploymentClient for deployment: >" + addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ) + "<");
					deploymentClient = new DeploymentClient(addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ));
					deploymentClient.start();					
					deploymentClient.registerListener(pluginObject.getMessageType(), this);
				} catch (Exception e) {
					logger.error("Could not load DeploymentClient for deployment: >" + addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ) + "<");
					return false;
				}
				deploymentClientList.put(addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ), deploymentClient);
			}
		}
		
		// count active plugins per deployment
		synchronized (activePluginsCounterList) {
			if (activePluginsCounterList.containsKey(addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ))) {
				int n = activePluginsCounterList.get(addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ));
				activePluginsCounterList.put(addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ), n+1);
			}
			else {
				activePluginsCounterList.put(addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ), 1);
			}
		}
		
		// start optional local serial forwarder or check its port compliance if needed
		// do we need a serial forwarder for TinyOS-2.x
		if (addressBean.getPredicateValue(TINYOS1X_PLATFORM_NAME) == null) {
			synchronized (sfListenList) {
				if (sfListenList.containsKey(addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ))) {
					sfListen = sfListenList.get(addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ));
					if( addressBean.getPredicateValueAsInt( SF_LOCAL_PORT, -1 ) != sfListen.getLocalPort())
						logger.warn("serial forwarder port does not match: reusing port " + sfListen.getLocalPort());
				}
				else {
					int port = -1;
					try {
						port = addressBean.getPredicateValueAsInt( SF_LOCAL_PORT, -1 );
						if( port > 0 ) {
							sfListen = new SFListen(port, deploymentClient);
							logger.info("starting local serial forwarder on port " + port + " for deployment: >" + addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ) + "<");
							sfListen.start();
							sfListenList.put(addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ), sfListen);
						}
					} catch (Exception e) {
						logger.error("Could not start serial forwarder v2.x on port " + port + " for deployment: >" + addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ) + "<");
						return false;
					}
				}
			}
		}
		// or do we need one for TinyOS-1.x
		else {
			synchronized (sfv1ListenList) {
				if (sfv1ListenList.containsKey(addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ))) {
					sfv1Listen = sfv1ListenList.get(addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ));
					if( addressBean.getPredicateValueAsInt( SF_LOCAL_PORT, -1 ) != sfv1Listen.getLocalPort())
						logger.warn("serial forwarder port does not match: reusing port " + sfv1Listen.getLocalPort());
				}
				else {
					int port = -1;
					try {
						port = addressBean.getPredicateValueAsInt( SF_LOCAL_PORT, -1 );
						if( port > 0 ) {
							sfv1Listen = new SFv1Listen(port, deploymentClient, addressBean.getPredicateValue( TINYOS1X_PLATFORM_NAME ));
							logger.info("starting local serial forwarder v1.x on port " + port + " for deployment: >" + addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ) + "< using '" + addressBean.getPredicateValue( TINYOS1X_PLATFORM_NAME ) + "' platform");
							sfv1Listen.start();
							sfv1ListenList.put(addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ), sfv1Listen);
						}
					} catch (Exception e) {
						logger.error("Could not start serial forwarder on port " + port + " for deployment: >" + addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ) + "<");
						return false;
					}
				}
			}
		}
		
		return true;
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
		logger.debug("dataProcessed timestamp: " + timestamp);
		//TODO: change the following line as soon as GSN has fixed the unordered timestamp bug!!!
		// return postStreamElement(timestamp, data);
		return postStreamElement(data);
	}




	/**
	 * This function can be called by the plugin, if it has processed
	 * the data. Thus, the data will be forwarded to the corresponding
	 * virtual sensor by GSN.
	 * 
	 * 
	 * @param timestamp
	 * 			The timestamp in milliseconds this data has been
	 * 			generated.
	 * @param data 
	 * 			The data to be processed. Its format must correspond
	 * 			to the one specified by the plugin's getOutputFormat()
	 * 			function.
	 * @return if the message has been sent successfully true will be returned
	 * 			 else false (no working connection)
	 */
	public boolean sendRemote(byte[] data) {
		try {
			return deploymentClient.sendMessage(new BackLogMessage(pluginObject.getMessageType(), 0, data));
		} catch (IOException e) {
			logger.error(e.getMessage());
			return false;
		}
	}




	/**
	 * This function must be called by the plugin, to acknowledge
	 * incoming messages if it is using the backlog functionality
	 * on the deployment side.
	 * 
	 * The timestamp will be used at the deployment to remove the
	 * corresponding message backloged in the database.
	 * 
	 * @param timestamp
	 * 			The timestamp is used to acknowledge a message. Thus
	 * 			it has to be equal to the timestamp from the received
	 * 			message we want to acknowledge.
	 */
	public void ackMessage(long timestamp) {
		deploymentClient.sendAck(timestamp);
	}

	
	
	
	/**
	 * Retruns true if the deploymentClient is connected to the deployment.
	 * 
	 * @return true if the client is connected otherwise false
	 */
	public boolean isConnected() {
		if( deploymentClient != null )
			return deploymentClient.isConnected();
		else
			return false;
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
		return "BackLog wrapper";
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
		logger.debug("Upload command received.");
		return pluginObject.sendToPlugin(action, paramNames, paramValues);
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
		logger.debug("Upload object received.");
		return pluginObject.sendToPlugin(dataItem);
	}
	
	
	
	@Override
	public boolean messageReceived(BackLogMessage message) {
		int packetcode = pluginObject.packetReceived(message.getTimestamp(), message.getPayload());
		if(packetcode == pluginObject.PACKET_ERROR)
			logger.warn("Message with timestamp >" + message.getTimestamp() + "< and type >" + message.getType() + "< could not be processed! Skip message.");
		if (packetcode == pluginObject.PACKET_PROCESSED)
			return true;
		else
			return false;
	}



	/**
	 * Finalizes this BackLogWrapper. This function is called by GSN.
	 * <p>
	 * Finalizes the used plugin and deregisters itself from the 
	 * DeploymentClient.
	 *
	 * If this is the last BackLogWrapper for the used deployment,
	 * the DeploymentClient will be finalized, as well as an optional
	 * serial forwarder if needed.
	 */
	@Override
	public void dispose() {
		logger.info("Deregister this BackLogWrapper from the deployment >" + addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ) + "<");
		
		try {
			deploymentSemaphore.acquire();
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
		int n = activePluginsCounterList.get(addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ));
		activePluginsCounterList.put(addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ), n-1);
		
		deploymentClient.deregisterListener(pluginObject.getMessageType(), this);

		if( 1 == n ) {
			// if this is the last listener close the serial forwarder
			if( sfListen != null ) {
				sfListen.finalize();
				sfListenList.remove(addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ));
			}
			if( sfv1Listen != null ) {
				sfv1Listen.finalize();
				sfv1ListenList.remove(addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ));
			}
			// and the client
			deploymentClientList.remove(addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ));
			deploymentClient.finalize();
			
			// remove this deployment from the counter
			activePluginsCounterList.remove(addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ));
			
			logger.info("Final shutdown of the deployment >" + addressBean.getPredicateValue( BACKLOG_DEPLOYMENT ) + "<");
		}

		// tell the plugin to stop
		pluginObject.finalize();
		
		blWrapperthreadCounter--;
		deploymentSemaphore.release();
	}
   
	@Override
   	public boolean isTimeStampUnique() {
   		return false;
   	}

}
