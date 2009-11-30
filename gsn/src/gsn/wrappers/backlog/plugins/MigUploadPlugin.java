package gsn.wrappers.backlog.plugins;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import net.tinyos.message.SerialPacket;
import net.tinyos.packet.Serial;
import net.tinyos1x.message.TOSMsg;

import org.apache.log4j.Logger;

import gsn.beans.DataField;
import gsn.wrappers.BackLogWrapper;


/**
 * This plugin offers the functionality to upload TOS messages to a
 * deployment. It supports TinyOS1.x as well as TinyOS2.x messages.
 * <p>
 * If the TINYOS1X_PLATFORM_NAME (default 'tinyos1x-platformName' is
 * specified in the virtual sensor's XML file, TinyOS1.x messages
 * will be generated otherwise TinyOS2.x messages.
 * 
 * @author Tonio Gsell
 */
public class MigUploadPlugin extends AbstractPlugin {
	
	// only mandatory for TinyOS1.x messages
	private static final String TINYOS1X_PLATFORM = "tinyos1x-platform";

	private int commands_sent = 0;
	
	private final static int tinyos1x_groupId = -1;
	private String tinyos1x_platform = null;

	private TOSMsg template ;

	private final transient Logger logger = Logger.getLogger( MigUploadPlugin.class );


	@Override
	public boolean initialize(BackLogWrapper backLogWrapper) {
		super.initialize(backLogWrapper);
		tinyos1x_platform = getActiveAddressBean().getPredicateValue(TINYOS1X_PLATFORM);

		// a template message for this platform has to be instantiated to be able to get the data offset
		// if a message has to be sent to the deployment
	   	Class<?> msgCls;
		try {
			msgCls = Class.forName ( "net.tinyos1x.message." + tinyos1x_platform + ".TOSMsg" );
		   	Constructor<?> c = msgCls.getConstructor () ;
			template = (TOSMsg) c.newInstance () ;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return false;
		}
		
		return true;
	}

	@Override
	public byte getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.TOS_MESSAGE_TYPE;
	}

	@Override
	public DataField[] getOutputFormat() {
		DataField[] dataField = {new DataField("CONNECTED", "SMALLINT"), new DataField("COMMANDS_SENT", "INTEGER")};
		return dataField;
	}

	@Override
	public int packetReceived(long timestamp, byte[] packet) {
		return PACKET_PROCESSED;
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
				return false;
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
					return false;
				}
			}
			else {
				logger.error("binary_packet upload action needs a 'binary packet' field.");
				return false;
			}
		}
		else
			logger.error("Unknown action");

		short connected = 0;
		if( isConnected() )
			connected = 1;
		Serializable[] output = {connected, commands_sent++};
		dataProcessed(System.currentTimeMillis(), output);
		
		return ret;
	}

	
    private byte[] createTOSpacket(int moteId, int amType, byte[] data) throws IOException {
		if (amType < 0) {
		    throw new IOException("unknown AM type for message");
		}
	
		// which TinyOS messages version are we generating?
		if (tinyos1x_platform != null) {
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

    public TOSMsg createTOSMsg ( int data_length ) {
    	Object [] initArgs = new Object [ 1 ] ;
    	Class<?> [] cArgs = new Class [ 1 ] ;
    	cArgs [ 0 ] = Integer.TYPE ;
    	initArgs [ 0 ] = new Integer ( data_length ) ;
    	
    	return instantiateTOSMsg ( cArgs , initArgs ) ;
    }

}
