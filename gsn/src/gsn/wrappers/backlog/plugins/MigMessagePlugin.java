package gsn.wrappers.backlog.plugins;

import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.wrappers.BackLogWrapper;
import gsn.wrappers.backlog.plugins.MigMessageParameters;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
	private MigMessageParameters parameters = null;

	private int messageType = -1;
	private Constructor<?> messageConstructor = null;

	private final transient Logger logger = Logger.getLogger( MigMessagePlugin.class );
	
	private DataField[] outputstructure = null;
	private String[] outputstructurenames;
	
	private final static int tinyos1x_groupId = -1;
	private String tinyos1x_platform = null;

	private TOSMsg template ;	

	@Override
	public boolean initialize(BackLogWrapper backlogwrapper) {
		super.initialize(backlogwrapper);
		try {
			// get the Mig message class for the specified TOS packet
			parameters = new MigMessageParameters();
			parameters.initParameters(getActiveAddressBean());
			Class<?> classTemplate = Class.forName(parameters.getTinyosMessageName());
			parameters.buildOutputStructure(classTemplate, new ArrayList<DataField>(), new ArrayList<Method>());
			messageConstructor = classTemplate.getConstructor(byte[].class) ;
			
			// if it is a TinyOS1.x message class we need the platform name
			if (parameters.getTinyosVersion() == MigMessageParameters.TINYOS_VERSION_1) {
				tinyos1x_platform = backlogwrapper.getTinyos1xPlatform();
				messageType = ((net.tinyos1x.message.Message) messageConstructor.newInstance(new byte [1])).amType();

				// a template message for this platform has to be instantiated to be able to get the data offset
				// if a message has to be sent to the deployment
			   	Class<?> msgCls = Class.forName ( "net.tinyos1x.message." + tinyos1x_platform + ".TOSMsg" ) ;
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
	public String getPluginName() {
		return "MigMessagePlugin";
	}

	
	@Override
	public int packetReceived(long timestamp, byte[] packet) {
		// which TinyOS messages are we looking for?
		if (parameters.getTinyosVersion() == MigMessageParameters.TINYOS_VERSION_1) {
			// the following functionality has been extracted from net.tinyos1x.message.Receiver
			// from the packetReceived function
			
			// create a TOS message (TinyOS1.x)
			final TOSMsg msg = createTOSMsg ( packet ) ;

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
		if (outputstructure == null) {
			String s;
			LinkedHashMap<String, DataField> outputstructuremap = 
				new LinkedHashMap<String, DataField>(2 + parameters.getOutputStructure().length);
			outputstructuremap.put("timestamp", new DataField("timestamp", DataTypes.BIGINT));
			outputstructuremap.put("generationtime", new DataField("generationtime", DataTypes.BIGINT));
			for(int i=0; i<parameters.getOutputStructure().length; i++) {
				outputstructuremap.put(parameters.getOutputStructure()[i].getName(), 
						parameters.getOutputStructure()[i]);
			}

			// special merge of "*_low" and "*_high" fields
			outputstructurenames = outputstructuremap.keySet().toArray(new String[] {});
			for (int i=0; i < outputstructurenames.length; i++) {
				s = outputstructurenames[i];
				if (s.endsWith("_low") || s.endsWith("_high")) {
					s = s.substring(0, s.lastIndexOf('_')).toLowerCase();
					if (!outputstructuremap.containsKey(s))
						outputstructuremap.put(s, new DataField(s, DataTypes.INTEGER));
				}					
			}

			outputstructurenames = outputstructuremap.keySet().toArray(new String[] {});
			outputstructure = outputstructuremap.values().toArray(new DataField[] {});
		}
		return outputstructure;
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
			packet.set_group ( (short) tinyos1x_groupId ) ;
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
	

	private boolean messageToBeProcessed(long timestamp, byte[] rawmsg) {
		Method getter = null;
		Object res = null;

		LinkedHashMap<String, Serializable> outputvaluesmap = 
			new LinkedHashMap<String, Serializable>(outputstructure.length);
		
		outputvaluesmap.put(parameters.getTinyosGetterPrefix() + outputstructurenames[0], timestamp);
		outputvaluesmap.put(parameters.getTinyosGetterPrefix() + outputstructurenames[1], null);
		
		try {
			Object msg = (Object) messageConstructor.newInstance(rawmsg);

			Iterator<Method> iter = parameters.getGetters().iterator();
			while (iter.hasNext()) {
				getter = iter.next();
				getter.setAccessible(true);
				res = getter.invoke(msg);
				if (getter.getReturnType().isArray()) {
					String name = getter.getName().toLowerCase();
					for (int i=0; i < Array.getLength(res); i++) {
						//outputvaluesmap.put(name + "[" + i + "]", (Serializable) Array.get(res, i));
						addValue(outputvaluesmap, name + "[" + i + "]", (Serializable) Array.get(res, i));
					}
				}
				else {
					//outputvaluesmap.put(getter.getName().toLowerCase(), (Serializable) res);
					addValue(outputvaluesmap, getter.getName().toLowerCase(), (Serializable) res);
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return false;
		}
		
		// special merge of "*_low" and "*_high" fields
		for (int i=outputvaluesmap.size(); i < outputstructure.length; i++) {
			String key = parameters.getTinyosGetterPrefix() + outputstructurenames[i];
			int merged = 0;
			if (outputvaluesmap.containsKey(key + "_low"))
				merged += ((Integer) outputvaluesmap.get(key + "_low")).intValue();
			if (outputvaluesmap.containsKey(key + "_high"))
				merged += ((Short) outputvaluesmap.get(key + "_high")).intValue() << 16;
			outputvaluesmap.put(parameters.getTinyosGetterPrefix() + outputstructurenames[i], merged);
		}
		
		if (outputvaluesmap.containsKey(parameters.getTinyosGetterPrefix() + "header_atime")) {
			long atime = ((Integer) outputvaluesmap.get(parameters.getTinyosGetterPrefix() + "header_atime")).longValue();
			outputvaluesmap.put(parameters.getTinyosGetterPrefix() + outputstructurenames[1], timestamp - (atime * 1000));
		} else {
			// should never happen
			logger.warn("invalid gentime field");
		}
		
		if (dataProcessed(System.currentTimeMillis(), outputvaluesmap.values().toArray(new Serializable[] {})))
			ackMessage(timestamp);
		else
			logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database.");

		return true;
	}

	private void addValue(LinkedHashMap<String, Serializable> map, String name, Serializable obj) {
		// convert Float/Double NaN to null
		if (obj.getClass().isAssignableFrom(Float.class)) {
			if (((Float) obj).isNaN())
				obj = null;
		} else if (obj.getClass().isAssignableFrom(Double.class)) {
			if (((Double) obj).isNaN())
				obj = null;
		}
		map.put(name, obj);
	}
	
//	
// following functions are only used for TinyOS1.x messages	
//
	
	TOSMsg instantiateTOSMsg ( Class<?> [] cArgs , Object [] args ) {
	   	try {
		   	Class<?> msgCls ;
         
		   	msgCls = Class.forName ( "net.tinyos1x.message." + tinyos1x_platform + ".TOSMsg" ) ;
         
		   	Constructor<?> c = msgCls.getConstructor ( cArgs ) ;
		   	return (TOSMsg) c.newInstance ( args ) ;
	   	}
	   	catch ( ClassNotFoundException e ) {
		   	System.err.println ( "Could not find a platform specific version of TOSMsg" ) ;
		   	System.err.println ( e ) ;
		   	e.printStackTrace () ;
	   	}
	   	catch ( NoSuchMethodException e ) {
		   	System.err.println ( "Could not locate the appropriate constructor; check the class " + "net.tinyos1x.message." + tinyos1x_platform
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
