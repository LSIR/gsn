package gsn.wrappers.backlog.plugins;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.wrappers.BackLogWrapper;
import gsn.wrappers.backlog.plugins.MigMessageParameters;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;

import net.tinyos.message.SerialPacket;
import net.tinyos.packet.Serial;
import net.tinyos1x.message.TOSMsg;

import org.apache.log4j.Logger;


/**
 * This plugin offers the functionality to read/send TinyOS messages by using
 * Mig generated java message classes. TinyOS1.x as well as TinyOS2.x
 * generated messages are supported.
 * <p>
 * If this class is used with 'unique-timestamps=false' (defined in the
 * virtual sensor's XML file) all message classes should implement at least
 * the following two functions:
 * 
 * <li>{@code int get_header_atime_low()} (or with different getter prefix as defined in the XML)</li>
 * <li>{@code short get_header_atime_high()} (or with different getter prefix as defined in the XML)</li>
 * <p>
 * Thus, the time the packet has been generated can be calculated and written to 
 * the SQL database.
 * 
 * @author Tonio Gsell
 */
public class MigMessagePlugin extends AbstractPlugin
{
	// only mandatory for TinyOS1.x messages
	private static final String TINYOS1X_PLATFORM_NAME = "tinyos1x-platformName";
	// optional for TinyOS1.x messages
	private static final String TINYOS1X_GROUP_ID = "tinyos1x-groupId";
	
	// has to be the same as in net.tinyos1x.message.MoteIF
	private static final int ANY_GROUP_ID = -1;
	
	

	private MigMessageParameters parameters = null;

	private int messageType = -1;
	private Constructor<?> messageConstructor = null;

	private final transient Logger logger = Logger.getLogger( MigMessagePlugin.class );
	
	private int tinos1x_groupId;
	private String tinos1x_platformName = null;

	private TOSMsg template ;
	

	@Override
	public boolean initialize(BackLogWrapper backLogWrapper) {
		super.initialize(backLogWrapper);
		try {
			// get the Mig message class for the specified TOS packet
			parameters = new MigMessageParameters();
			parameters.initParameters(getActiveAddressBean());
			Class<?> classTemplate = Class.forName(parameters.getTinyosMessageName());
			parameters.buildOutputStructure(classTemplate, new ArrayList<DataField>(), new ArrayList<Method>());
			messageConstructor = classTemplate.getConstructor(byte[].class) ;
			
			// if it is a TinyOS1.x message class we need the platform name
			if (parameters.getTinyosVersion() == MigMessageParameters.TINYOS_VERSION_1) {
				tinos1x_groupId = getActiveAddressBean().getPredicateValueAsInt(TINYOS1X_GROUP_ID, -1);
				try {
					tinos1x_platformName = getActiveAddressBean().getPredicateValueWithException(TINYOS1X_PLATFORM_NAME);
				}
				catch (Exception e) {
					logger.error(e.getMessage());
					return false;
				}
				messageType = ((net.tinyos1x.message.Message) messageConstructor.newInstance(new byte [1])).amType();

				// a template message for this platform has to be instantiated to be able to get the data offset
				// if a message has to be sent to the deployment
			   	Class<?> msgCls = Class.forName ( "net.tinyos1x.message." + tinos1x_platformName + ".TOSMsg" ) ;
			   	Constructor<?> c = msgCls.getConstructor () ;
				template = (TOSMsg) c.newInstance () ;
			}
			else {
				messageType = ((net.tinyos.message.Message) messageConstructor.newInstance(new byte [1])).amType();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return false;
		}
		
		return true;
	}

	
	@Override
	public int packetReceived(long timestamp, byte[] packet) {
		// which TinyOS messages are we looking for?
		if (parameters.getTinyosVersion() == MigMessageParameters.TINYOS_VERSION_1) {
			// the following functionality has been extracted from net.tinyos1x.message.Receiver
			// from the packetReceived function
			
			// create a TOS message (TinyOS1.x)
			final TOSMsg msg = createTOSMsg ( packet ) ;

			// are we interested in this group id?
			if ( tinos1x_groupId == ANY_GROUP_ID || msg.get_group () == tinos1x_groupId ) {
				Integer type = new Integer ( msg.get_type () );

				// are we also interested in this message type
				if( type == messageType ) {
					net.tinyos1x.message.Message received ;
					int length = msg.get_length () ;
					
					try {
						// clone the message for further processing
						received = msg.clone( length ) ;
						received.dataSet ( msg.dataGet () , msg.offset_data ( 0 ) , 0 , length ) ;
					}
					catch ( ArrayIndexOutOfBoundsException e ) {
						/*
					    * Note: this will not catch messages whose length is incorrect,
					    * but less than DATA_LENGTH (see AM.h) + 2
					    */
						logger.error( "invalid length message received (too long)" ) ;
				    	return PACKET_ERROR;
					}
					catch ( Exception e ) {
						logger.error( "couldn't clone message!, TinyOS 1x" ) ;
						return PACKET_ERROR;
					}
					
					// process the message
					messageToBeProcessed(timestamp, received.dataGet());					
					return PACKET_PROCESSED;
				}

			} else {
		    	  logger.error("Dropping packet with bad group ID");
		    	  return PACKET_ERROR;
			}
		}
		else {
			// the following functionality has been extracted from net.tinyos.message.Receiver
			// from the packetReceived function
			
			if (packet[0] != Serial.TOS_SERIAL_ACTIVE_MESSAGE_ID)
				return PACKET_SKIPPED; // not for us.
	
			// create a SerialPacket message
			SerialPacket msg = new SerialPacket(packet, 1);
			int type = msg.get_header_type();

			// are we interested in this message type
			if( type == messageType ) {
			    int length = msg.get_header_length();
			    net.tinyos.message.Message received;
			    try {
					// clone the message for further processing
				    received = msg.clone(length);
				    received.dataSet(msg.dataGet(), SerialPacket.offset_data(0) + msg.baseOffset(), 0, length);
			    } catch (ArrayIndexOutOfBoundsException e) {
			    	logger.error("invalid length message received (too long)");
			    	return PACKET_ERROR;
			    } catch (Exception e) {
			    	logger.error("couldn't clone message!");
			    	return PACKET_ERROR;
			    }
			    
				// process the message
				messageToBeProcessed(timestamp, received.dataGet());
				return PACKET_PROCESSED;
			}
		}
		return PACKET_SKIPPED;
	}

	
	@Override
	public DataField[] getOutputFormat() {
		DataField[] tmp = new DataField[ parameters.getOutputStructure().length+1];
		tmp[0] = new DataField("TIMESTAMP", "BIGINT");
		for(int i=0; i<parameters.getOutputStructure().length; i++)
			tmp[i+1] = parameters.getOutputStructure()[i];
		return tmp;
	}

	
	@Override
	public byte getMessageType() {
		// this plugin operates on the TOS_MESSAGE_TYPE
		return gsn.wrappers.backlog.BackLogMessage.TOS_MESSAGE_TYPE;
	}



	@Override
	public boolean sendToPlugin(String action, String[] paramNames, Object[] paramValues) {
		boolean ret = false;
		logger.debug("action: " + action);
		if( action.compareToIgnoreCase("payload") == 0 ) {
			int moteId = -257;
			int amType = -257;
			byte[] data = null;
			
			if( paramNames.length != 3 ) {
				logger.error("upload action must have three parameter names: 'moteid', 'amtype' and 'data'");
				return false;
			}
			if( paramValues.length != 3 ) {
				logger.error("upload action must have three parameter values");
				return false;
			}
			
			for( int i=0; i<3; i++ ) {
				try {
					String tmp = paramNames[i];
					if( tmp.compareToIgnoreCase("mote id") == 0 )
						moteId = Integer.parseInt((String) paramValues[i]);
					else if( tmp.compareToIgnoreCase("am type") == 0 )
						amType = Integer.parseInt((String) paramValues[i]);
					else if( tmp.compareToIgnoreCase("payload") == 0 )
						data = ((String) paramValues[i]).getBytes();
				} catch(Exception e) {
					logger.error("Could not interprete upload arguments: " + e.getMessage());
					return false;
				}
			}
			
			if( moteId < -256 | amType < -256 | data == null ) {
				logger.error("upload action must contain all three parameter names: 'mote id', 'am type' and 'payload'");
				return false;
			}
			
			if(data.length == 0) {
				logger.warn("Upload message's payload is empty");
			}
			
			try {
				ret = sendRemote(createTOSpacket(moteId, amType, data));
				logger.debug("Mig message sent to mote id " + moteId + " with AM type " + amType);
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		}
		else if( action.compareToIgnoreCase("binary_packet") == 0 ) {
			if(((String)paramNames[0]).compareToIgnoreCase("binary packet") == 0) {
				byte [] packet = ((String) paramValues[0]).getBytes();
				if(packet.length > 0) {
					ret = sendRemote(packet);
					logger.debug("Mig binary message sent with length " + ((String) paramValues[0]).length());
				}
				else {
					logger.error("Upload failed due to empty 'binary packet' field");
				}
			}
			else {
				logger.error("binary_packet upload action needs a 'binary packet' field.");
			}
		}
		else
			logger.error("Unknown action");
		
		return ret;
	}

	
    private byte[] createTOSpacket(int moteId, int amType, byte[] data) throws IOException {
		if (amType < 0) {
		    throw new IOException("unknown AM type for message");
		}
	
		// which TinyOS messages version are we generating?
		if (parameters.getTinyosVersion() == MigMessageParameters.TINYOS_VERSION_1) {
			// the following functionality has been extracted from net.tinyos1x.message.Sender
			// from the send function
			TOSMsg packet ;
		      
			// normal case, a PhoenixSource
			// hack: we don't leave any space for the crc, so
			// numElements_data() will be wrong. But we access the
			// data area via dataSet/dataGet, so we're ok.
			packet = createTOSMsg ( template.offset_data ( 0 ) + data.length ) ;

			// message header: destination, group id, and message type
			packet.set_addr ( moteId ) ;
			packet.set_group ( (short) tinos1x_groupId ) ;
			packet.set_type ( ( short ) amType ) ;
			packet.set_length ( ( short ) data.length ) ;
		      
			packet.dataSet ( data , 0 , packet.offset_data ( 0 ) , data.length ) ;

			return packet.dataGet();
		}
		else {
			// the following functionality has been extracted from net.tinyos.message.Sender
			// from the send function
			SerialPacket packet =
			    new SerialPacket(SerialPacket.offset_data(0) + data.length);
			packet.set_header_dest(moteId);
			packet.set_header_type((short)amType);
			packet.set_header_length((short)data.length);
			packet.dataSet(data, 0, SerialPacket.offset_data(0), data.length);
		
			byte[] packetData = packet.dataGet();
			byte[] fullPacket = new byte[packetData.length + 1];
			fullPacket[0] = Serial.TOS_SERIAL_ACTIVE_MESSAGE_ID;
			System.arraycopy(packetData, 0, fullPacket, 1, packetData.length);
			return fullPacket;
		}
    }
	

	private boolean messageToBeProcessed(long timestamp, byte[] rmsg) {
		Method getter = null;
		Object res = null;
		Serializable resarray = null;
			
		if (logger.isDebugEnabled()) {
			StringBuilder rawmsgoutput = new StringBuilder ();
			for (int i = 0 ; i < rmsg.length ; i++) {
				rawmsgoutput.append(rmsg[i]);
				rawmsgoutput.append(" ");
			}
			logger.debug("new message to be processed: " + rawmsgoutput.toString());
		}

		ArrayList<Serializable> output = new ArrayList<Serializable> () ;
		try {
			Object msg = (Object) messageConstructor.newInstance(rmsg);

			Iterator<Method> iter = parameters.getGetters().iterator();
			while (iter.hasNext()) {
				getter = (Method) iter.next();
				getter.setAccessible(true);
				res = getter.invoke(msg);
				if (getter.getReturnType().isArray()) {
					for(int i = 0 ; i < Array.getLength(res) ; i++) {
						resarray = (Serializable) Array.get(res, i);
						output.add(resarray);
						logger.debug("> " + getter.getName() + ": " + resarray);
					}
				}
				else {
					output.add((Serializable)res);
					logger.debug("> " + getter.getName() + ": " + res);
				}
			}
			
		} catch (InstantiationException e) {
			logger.error("Unable to instanciate the message");
			return false;
		} catch (IllegalAccessException e) {
			logger.error("Illegal Access to >" + getter + "<");
			return false;
		} catch (IllegalArgumentException e) {
			logger.error("Illegal argument to >" + getter + "<");
			return false;
		} catch (InvocationTargetException e) {
			logger.error("Invocation Target Exception " + e.getMessage(), e);
			return false;
		} catch (SecurityException e) {
			logger.error("Security Exception " + e.getMessage());
			return false;
		}

		output.add(0, timestamp);
		if (dataProcessed(System.currentTimeMillis(), output.toArray(new Serializable[] {})))
			ackMessage(timestamp);
		else
			logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database.");

		return true;
	}

//	
// following functions are only used for TinyOS1.x messages	
//
	
	TOSMsg instantiateTOSMsg ( Class<?> [] cArgs , Object [] args ) {
	   	try {
		   	Class<?> msgCls ;
         
		   	msgCls = Class.forName ( "net.tinyos1x.message." + tinos1x_platformName + ".TOSMsg" ) ;
         
		   	Constructor<?> c = msgCls.getConstructor ( cArgs ) ;
		   	return (TOSMsg) c.newInstance ( args ) ;
	   	}
	   	catch ( ClassNotFoundException e ) {
		   	System.err.println ( "Could not find a platform specific version of TOSMsg" ) ;
		   	System.err.println ( e ) ;
		   	e.printStackTrace () ;
	   	}
	   	catch ( NoSuchMethodException e ) {
		   	System.err.println ( "Could not locate the appropriate constructor; check the class " + "net.tinyos1x.message." + tinos1x_platformName
                              + ".TOSMsg" ) ;
		   	e.printStackTrace () ;
	   	}
	   	catch ( InstantiationException e ) {
		   	System.err.println ( "Could not instantiate the class: " + e ) ;
		   	e.printStackTrace () ;
      	}
      	catch ( IllegalAccessException e ) {
    	  	System.err.println ( "Illegal access: " + e ) ;
         	e.printStackTrace () ;
      	}
      	catch ( InvocationTargetException e ) {
      		System.err.println ( "Reflection problems: " + e ) ;
         	e.printStackTrace () ;
      	}
      	return null ;
   	}

   	public TOSMsg createTOSMsg ( byte data[] ) {
   		Object initArgs[] = new Object [ 1 ] ;
   		Class<?> cArgs[] = new Class [ 1 ] ;
   		cArgs [ 0 ] = data.getClass () ;
   		initArgs [ 0 ] = data ;

   		return instantiateTOSMsg ( cArgs , initArgs ) ;
   	}

    public TOSMsg createTOSMsg ( int data_length ) {
    	Object [] initArgs = new Object [ 1 ] ;
    	Class<?> [] cArgs = new Class [ 1 ] ;
    	cArgs [ 0 ] = Integer.TYPE ;
    	initArgs [ 0 ] = new Integer ( data_length ) ;
    	
    	return instantiateTOSMsg ( cArgs , initArgs ) ;
    }

}
