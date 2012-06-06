package gsn.wrappers.general;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.Logger;

import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.wrappers.AbstractWrapper;

public class TCPWrapper extends AbstractWrapper {

	private static final String			PACKET_SEPARATOR = System.getProperty("line.separator");
	private static final String			RAW_PACKET    = "RAW_PACKET";
	private static final Integer		DEFAULT_BUFFER_SIZE = 4096;
	
	private final transient Logger		logger        = Logger.getLogger( TCPWrapper.class );
	
	private int							threadCounter = 0;
	private int							bufferSize;
	private ServerSocket				serverSocket;
	   
	/*
	 * Needs the following information from XML file : port : the tcp port it
	 * should be listening to
	 */
	public boolean initialize (  ) {
		try {
			serverSocket = new ServerSocket( Integer.parseInt( getActiveAddressBean( ).getPredicateValue( "port" ) ) );
		} catch ( Exception e ) {
			logger.warn( e.getMessage( ) , e );
			return false;
		}
		
		bufferSize = Integer.parseInt( getActiveAddressBean( ).getPredicateValueWithDefault("buffer-size", DEFAULT_BUFFER_SIZE.toString()) );
		setName( "TCPWrapper-Thread" + ( ++threadCounter ) );
		return true;
		}
		
	public void run ( ) {
		char [ ] receivedData = new char [ bufferSize ];
		String rest = "";
		BufferedReader bufferedReader = null;
		while ( isActive( ) ) {
			try {
				Socket socket = serverSocket.accept();
				bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				logger.info("connection received from " + socket.getInetAddress().getCanonicalHostName());
			} catch (IOException e) {
				logger.error(e);
			}
			
			int anzahlZeichen;
			try {
				while ((anzahlZeichen = bufferedReader.read(receivedData, 0, bufferSize)) != -1) {
		            String data = new String(receivedData, 0, anzahlZeichen);
	
					if ( logger.isDebugEnabled( ) ) logger.debug( "TCPWrapper received a data : " + data );
					
					data = rest + data;
					
					String elements[] = data.split(PACKET_SEPARATOR);
					int stop = elements.length;
					rest = "";
					if (!data.endsWith(PACKET_SEPARATOR)) {
						stop--;
						rest = elements[elements.length-1];
					}
					
					for (int i=0; i<stop; i++) {
						StreamElement streamElement = new StreamElement( new String [ ] { RAW_PACKET } , new Byte [ ] { DataTypes.BINARY } , new Serializable [ ] { elements[i].getBytes() } , System
								.currentTimeMillis( ) );
						postStreamElement( streamElement );
					}
				}
			} catch ( IOException e ) {
				logger.warn( "Error while receiving data on TCP socket : " + e.getMessage( ) );
			}
		}
	}
	
	public  DataField [] getOutputFormat ( ) {
		return new DataField[] {new DataField( RAW_PACKET , "BINARY" , "The packet contains raw data received as a TPC packet." ) };
	}
	
	public void dispose (  ) {
		try {
			serverSocket.close();
		} catch (IOException e) {
			logger.error(e);
		}
		threadCounter--;
	}
	
	public String getWrapperName() {
		return "network tcp";
	}
}
