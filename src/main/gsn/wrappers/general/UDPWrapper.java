package gsn.wrappers.general;

import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.beans.WrapperConfig;
import gsn.channels.DataChannel;
import gsn.wrappers.Wrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import org.apache.log4j.Logger;

/**
 * Links GSN to a Wisenet sensors network. The computer running this wrapper
 * should be connected to an IP network. One of the WSN nodes should forward
 * received packets through UDP to the host running this wrapper.
 */
public class UDPWrapper implements Wrapper {

	private final WrapperConfig conf;

	private final DataChannel dataChannel;

	private static final String    RAW_PACKET    = "RAW_PACKET";

	private final transient Logger logger        = Logger.getLogger( UDPWrapper.class );

	public InputStream             is;

	private int                    port;

	private DatagramSocket         socket;

	/*
	 * Needs the following information from XML file : port : the udp port it
	 * should be listening to rate : time to sleep between each packet
	 */
	public UDPWrapper (WrapperConfig conf, DataChannel channel) throws SocketException {
		this.conf = conf;
		this.dataChannel= channel;
		port = conf.getParameters().getPredicateValueAsIntWithException( "port" ) ;
		socket = new DatagramSocket( port );
	}

	public void start(){
		byte [ ] receivedData = new byte [ 50 ];
		DatagramPacket receivedPacket = null;
		while ( isActive ) {
			try {
				receivedPacket = new DatagramPacket( receivedData , receivedData.length );
				socket.receive( receivedPacket );
				String dataRead = new String( receivedPacket.getData( ) );
				if ( logger.isDebugEnabled( ) ) logger.debug( "UDPWrapper received a packet : " + dataRead );
				StreamElement streamElement = new StreamElement( new String [ ] { RAW_PACKET } , new Byte [ ] { DataTypes.BINARY } , new Serializable [ ] { receivedPacket.getData( ) } , System
						.currentTimeMillis( ) );
				dataChannel.write( streamElement );
			} catch ( IOException e ) {
				logger.warn( "Error while receiving data on UDP socket : " + e.getMessage( ) );
			}
		}
	}

	public  DataField [] getOutputFormat ( ) {
		return new DataField[] {new DataField( RAW_PACKET , "BINARY" , "The packet contains raw data received as a UDP packet." ) };

	}

	private boolean isActive=true;

	public void dispose ( ) {

	}

	public void stop() {
		isActive = false;
	}
}
