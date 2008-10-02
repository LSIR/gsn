package gsn.acquisition2.wrappers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.lang.reflect.Constructor;
import org.apache.log4j.Logger;

public class MigMessageWrapper2  extends AbstractWrapper2 implements net.tinyos1x.message.MessageListener, net.tinyos.message.MessageListener {

	private static int threadCounter = 0;

	private MigMessageParameters parameters = null;

	private net.tinyos1x.message.MoteIF moteIFTinyOS1x = null;
	private net.tinyos.message.MoteIF moteIFTinyOS2x = null;

	private net.tinyos1x.message.Message messageTemplateTinyOS1x = null;
	private net.tinyos.message.Message messageTemplateTinyOS2x = null;

	private static Map<String,Semaphore> moteIFList1xSemaphore = new HashMap<String,Semaphore>();
	private static Map<String,net.tinyos1x.message.MoteIF> moteIFList1x = new HashMap<String,net.tinyos1x.message.MoteIF>();
	
	private static Map<String,Semaphore> moteIFList2xSemaphore = new HashMap<String,Semaphore>();
	private static Map<String,net.tinyos.message.MoteIF> moteIFList2x = new HashMap<String,net.tinyos.message.MoteIF>();

	private final transient Logger logger = Logger.getLogger( MigMessageWrapper2.class );

	@Override
	public boolean initialize() {
		logger.warn("tinyos wrapper initialize started...");
		try {

			parameters = new MigMessageParameters();
			parameters.initParameters(getActiveAddressBean());

			//
			logger.debug("Connecting to " + parameters.getTinyosSource());
			if (parameters.getTinyosVersion() == MigMessageParameters.TINYOS_VERSION_1) {
				// create a semaphore for each source/address
				Semaphore sem;
				synchronized (moteIFList1xSemaphore) {
					if (moteIFList1xSemaphore.containsKey(parameters.getTinyosSource())) {
						sem = moteIFList1xSemaphore.get(parameters.getTinyosSource());
					}
					else {
						sem = new Semaphore(1);
						moteIFList1xSemaphore.put(parameters.getTinyosSource(), sem);
					}
				}

				sem.acquire();

				if (!moteIFList1x.containsKey(parameters.getTinyosSource())) {
					// Create the source
					logger.debug("Create new source >" + parameters.getTinyosSource() + "<.");
					net.tinyos1x.packet.PhoenixSource phoenixSourceTinyOS1x = net.tinyos1x.packet.BuildSource.makePhoenix(parameters.getTinyosSource(), net.tinyos1x.util.PrintStreamMessenger.err);
					if (phoenixSourceTinyOS1x == null) throw new IOException ("The source >" + parameters.getTinyosSource() + "< is not valid.");
					phoenixSourceTinyOS1x.setResurrection();
					moteIFTinyOS1x = new net.tinyos1x.message.MoteIF(phoenixSourceTinyOS1x);
					moteIFList1x.put(parameters.getTinyosSource(), moteIFTinyOS1x);
				}
				else {
					logger.debug("Reusing source >" + parameters.getTinyosSource() + "<.");
					moteIFTinyOS1x = (net.tinyos1x.message.MoteIF) moteIFList1x.get(parameters.getTinyosSource());
				}

				sem.release();

				// Register to the message type
				logger.debug("Register message >" + parameters.getTinyosMessageName() + "< to source.");
				Class<?> messageClass = Class.forName(parameters.getTinyosMessageName());
				if (parameters.getTinyOSMessageLength() == -1) {
					moteIFTinyOS1x.registerListener((net.tinyos1x.message.Message) messageClass.newInstance(), this);
				}
				else {
					Constructor<?> messageConstructor = messageClass.getConstructor(int.class);
					moteIFTinyOS1x.registerListener((net.tinyos1x.message.Message) messageConstructor.newInstance(parameters.getTinyOSMessageLength()), this);
				}
			}
			else {
				
				// create a semaphore for each source/address
				Semaphore sem;
				synchronized (moteIFList2xSemaphore) {
					if (moteIFList2xSemaphore.containsKey(parameters.getTinyosSource())) {
						sem = moteIFList2xSemaphore.get(parameters.getTinyosSource());
					}
					else {
						sem = new Semaphore(1);
						moteIFList2xSemaphore.put(parameters.getTinyosSource(), sem);
					}
				}

				sem.acquire();
				
				if (! moteIFList2x.containsKey(parameters.getTinyosSource())) {
					// Create the source
					net.tinyos.packet.PhoenixSource phoenixSourceTinyOS2x = net.tinyos.packet.BuildSource.makePhoenix(parameters.getTinyosSource(), net.tinyos.util.PrintStreamMessenger.err) ;
					if (phoenixSourceTinyOS2x == null) throw new IOException ("The source >" + parameters.getTinyosSource() + "< is not valid.") ; 
					phoenixSourceTinyOS2x.setResurrection() ;
					phoenixSourceTinyOS2x.start();
					moteIFTinyOS2x = new net.tinyos.message.MoteIF (phoenixSourceTinyOS2x) ;
					moteIFList2x.put(parameters.getTinyosSource(), moteIFTinyOS2x);
				}
				else {
					logger.debug("Reusing source >" + parameters.getTinyosSource() + "<.");
					moteIFTinyOS2x = (net.tinyos.message.MoteIF) moteIFList2x.get(parameters.getTinyosSource());
				}
				
				sem.release();
				
				// Register to message type
				logger.debug("Register message >" + parameters.getTinyosMessageName() + "< to source.");
				Class<?> messageClass = Class.forName(parameters.getTinyosMessageName());
				if (parameters.getTinyOSMessageLength() == -1) {
					moteIFTinyOS2x.registerListener((net.tinyos.message.Message) messageClass.newInstance(), this);
				}
				else {
					Constructor<?> messageConstructor = messageClass.getConstructor(int.class);
					moteIFTinyOS2x.registerListener((net.tinyos.message.Message) messageConstructor.newInstance(parameters.getTinyOSMessageLength()), this);
				}
			}
			logger.debug("Connected to >" + parameters.getTinyosSource() + "<");
		}
		catch (ClassNotFoundException e) {
			logger.error("Unable to find the >" + parameters.getTinyosMessageName() + "< class.");
			return false;
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
			return false;
		}
		logger.warn("tinyos wrapper initialize completed ...");
		setName( "TinyOSWrapper-Thread:" + ( ++threadCounter ) );
		return true;
	}

	@Override
	public void run() {}

	@Override
	public void finalize() {
		if (moteIFTinyOS1x != null && messageTemplateTinyOS1x != null) moteIFTinyOS1x.deregisterListener(messageTemplateTinyOS1x, this);
		if (moteIFTinyOS2x != null && messageTemplateTinyOS2x != null) moteIFTinyOS2x.deregisterListener(messageTemplateTinyOS2x, this);
		threadCounter--;
	}

	@Override
	public String getWrapperName() {
		return "TinyOS packet wrapper";
	}

	public MigMessageParameters getParameters() {
		return parameters;
	}

	@Override
	public void messageReceived(int to, net.tinyos1x.message.Message tosmsg) {
		logger.debug("TinyOS 1.x Message received");
		postStreamElement(tosmsg.dataGet(), System.currentTimeMillis( ));
	}

	@Override
	public void messageReceived(int to, net.tinyos.message.Message tosmsg) {
		logger.debug("TinyOS 2.x Message received");
		postStreamElement(tosmsg.dataGet(), System.currentTimeMillis( ));
	}
}
