package gsn.wrappers.xBow;

/**
 * This is wrapper is based on Lei Shu's xmlWrapper.
 * It works with a Crossbow eKo wireless sensor network.
 * The sensor boards contain:
 * 	a Sensirion SHT11 single chip humidity and temperature sensor;
 * 	an Intersema MS55ER barometric pressure and temperature sensor;
 * 	a TAOS Inc. TLS2550 digital light sensor and
 * 	an Analog Devices ADXL202 dual axis accelerometer.
 * @author bgpearn
 */

//TODO: This wrapper does not reconnect to a remote xServe if communication is lost.


import gsn.beans.ContainerConfig;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.beans.WrapperConfig;
import gsn.channels.DataChannel;
import gsn.wrappers.Wrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.StringReader;
import java.net.Socket;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class eKoWrapper implements Wrapper {


	private final WrapperConfig conf;

	private final DataChannel dataChannel;

	private int                      DEFAULT_RATE       = 5000;

	private final transient Logger     logger                 = Logger.getLogger ( eKoWrapper.class );

	private static final String [ ]  FIELD_NAMES           = new String [ ] { "amtype", "nodeid", "packetname",
		"batteryV", "solarV", "enTemp",
		"soilmoisture", "soiltemp",
		"es1201Temp","es1201humid","es1201Dp",
		"EtDp", "EtVWC", "EtEc", "EtTemp",
	"sensorTable"};

	private static final Byte [ ]    FIELD_TYPES           = new Byte [ ] { DataTypes.INTEGER, DataTypes.INTEGER, DataTypes.VARCHAR, 
		DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.DOUBLE,
		DataTypes.DOUBLE, DataTypes.DOUBLE, 
		DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.DOUBLE,
		DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.DOUBLE,
		DataTypes.VARCHAR };

//	private static final String [ ]  FIELD_DESCRIPTION     = new String [ ] { "amType", "Node ID", "Packet Type",
//		"Battery Volts", "Solar Volts", "Internal Temp",
//		"Soil Moisture", "Soil Temperature",
//		"Ambient Temperature", "Ambient Humidity", "Ambient Dewpoint",
//		"Delectric Permittivity", "Water Content VWC (%)", "Electrical Conductivity (accurate to 7 dS/m)", "Temperature (degC)",
//	"Sensor Table" };

	private static final String [ ]  FIELD_TYPES_STRING    = new String [ ] { "int", "int", "varchar(50)", 
		"double", "double", "double", 
		"double", "double", 
		"double", "double", "double", 
		"double", "double", "double", "double", 
	"varchar(50)" };

	private DataField[]                outputStructure      ;

	private String                     host                ;

	private int                        port                ;

	private int                        rate                ;

	private Integer	amType;
	private int                        nodeid              ;
	private String packetName;
	private Double batteryV;
	private Double solarV;
	private Double enTemp;
	private Double soilMoisture;
	private Double soilTemp;
	private Double es1201Temp;
	private Double es1201Humidity;
	private Double es1201Dp;

	private Double EtDp;
	private Double EtVWC;
	private Double EtEc;
	private Double EtTemp;
	private String sensorTable;

	// declare the socket object for client side   
	private Socket                     xmlSocket = null    ;

	private BufferedReader             rd                  ;

	private StreamElement              streamEle           ;

	private String                     s  = ""             ; // xml packet

	private DocumentBuilderFactory     domfac              ;

	private DocumentBuilder            dombuilder          ;

	private InputSource                ins                 ;

	private Document                   doc                 ;

	private String                     bs                  ;

	private int                        indexS              ;

	private int                        indexE              ;

	private boolean                    getxml              ;

	public eKoWrapper(WrapperConfig conf, DataChannel channel) {
		this.conf = conf;
		this.dataChannel= channel;

		/**
		 * check the host and port parameters.
		 */

		host = conf.getParameters().getValueWithException( "host" );
		port = conf.getParameters().getValueAsInt("port" ,ContainerConfig.DEFAULT_GSN_PORT);
		if ( port > 65000 || port <= 0 ) 
			throw new RuntimeException("Remote wrapper initialization failed, bad port number:"+port);

		rate = conf.getParameters().getValueAsInt( "rate" ,DEFAULT_RATE);

		ArrayList<DataField > output = new ArrayList < DataField >();
		for ( int i = 0 ; i < FIELD_NAMES.length ; i++ ) {
			output.add( new DataField( FIELD_NAMES[ i ] , FIELD_TYPES_STRING[ i ] ) );
		}
		outputStructure = output.toArray( new DataField[] {} ); // ISSUE BUG ! How does the output work .

	}

	public void start(){

		try {

			// setup the socket connection
			xmlSocket = new Socket(host, port);

			rd = new BufferedReader(new InputStreamReader(xmlSocket.getInputStream()));

		} catch (IOException e){
			logger.warn(" The xml socket connection is not set up.");
			logger.warn(" Cannot read from xmlSocket. ");}
		//		s = "";
		logger.info("Is Timestamp Unique: " + isTimeStampUnique());

		while ( isActive ) {

			getxml = false;

			try {
				Thread.sleep(rate);
			} catch (InterruptedException e) {
				logger.error(e.getMessage(),e);
			}  // sleep

			try { // try 1

				char[] c = new char[6000];

				// initialize this char[]
				for (int j = 0; j < c.length; j++){
					c[j] = 0;
				}

				try {
					rd.read(c);
				} catch (Exception e) {
					// TODO: handle exception
					logger.warn("** Read Exception **");

				}  // try read
				for (int j = 0; j < c.length; j++){
					s = s + c[j];
				} 

				s = s.trim();

				getxml = true;

				while (getxml) {
					getxml = false;

					try{
						indexS = s.indexOf("<?xml");
						indexE = s.indexOf("</MotePacket>", (indexS+17));
					}catch (Exception e){
						logger.error( e.getMessage( ) , e );}

					if ((indexS >= 0) && (indexE > indexS + 17)) {
						bs = s.substring(indexS, indexE + 13);
						processXmlString(bs);
						getxml = true;
					}

					if (s.length() > (indexE + 13)) {
						s = s.substring((indexE + 14));
					}
					else {
						s = "";
					}

				}


			} catch (Exception e) { // try 1
				logger.error( e.getMessage( ) , e );}

		}   
	}

	private boolean isActive=true;

	public void dispose ( ) {
		isActive = false;
	}

	public  DataField[] getOutputFormat ( ) {
		return outputStructure;
	}

	public void processXmlString(String xmls) {

		try {
			Thread.sleep(5);
		} catch (InterruptedException e) {
			logger.error(e.getMessage(),e);
		}

		try { // try 4

			logger.info("[Try 4] xmls: "+ xmls);
			//logger.info("getxml : "+ getxml);

			// Create instance of DocumentBuilderFactory
			domfac = DocumentBuilderFactory.newInstance();

			try { // try 3
				// Get the DocumentBuilder
				dombuilder = domfac.newDocumentBuilder();
			} catch (ParserConfigurationException e){ // try 3
				logger.info(e.getMessage( ) , e );
			}

			try { // try 2
				// Create instance of input source
				ins = new InputSource();

				// Initialize this input source as xmls
				ins.setCharacterStream(new StringReader(xmls));

				// Pass xmls stream to XML Parser
				doc = dombuilder.parse(ins);
			} catch (SAXException e){ // try 2
				logger.info(e.getMessage( ) , e );
			} catch (NullPointerException e){
				logger.info(e.getMessage( ) , e );}

			// Get the root element of XML packet
			Element root = doc.getDocumentElement();
			// Get the first level Node list
			NodeList fields = root.getChildNodes();

			// Get all fields' name
			// start of second layer for 3

			// Element 0 is <PacketName>eN2100 Internal Sensors</PacketName>
			packetName = fields.item(0).getTextContent();


			/* The remaining elements are in the form:
			 * 	<ParsedDataElement>
			 * 		<Name>nodeId</Name>
			 * 		<SpecialType>nodeid</SpecialType>    // only some element and not used here.
			 * 		<ConvertedValue>2</ConvertedValue>
			 * 		<ConvertedValueType>uint16</ConvertedValueType>
			 * 	</ParsedDataElement>
			 */


			// Reset sensor values
			batteryV = null;
			solarV = null;
			enTemp = null;
			soilMoisture = null;
			soilTemp = null;
			EtDp = null;
			EtVWC = null;
			EtEc = null;
			EtTemp = null;
			es1201Temp = null;
			es1201Humidity = null;
			es1201Dp = null;



			for(int i=1; i<fields.getLength() - 1; i++){

				Element field = (Element)fields.item(i);		        		
				String name;

				Element nameEle=(Element)field.getElementsByTagName("Name").item(0);

				name = nameEle.getTextContent();

				String value;

				Element valueEle=(Element)field.getElementsByTagName("ConvertedValue").item(0);

				value = valueEle.getTextContent();

				if (name.equalsIgnoreCase("amtype")){
					amType = Integer.parseInt(value);
				}

				if (name.equalsIgnoreCase("nodeid")){
					nodeid = Integer.parseInt(value);
				}

				if (name.equals("batteryV")){
					soilMoisture = Double.parseDouble(value);
				}

				if (name.equals("solarV")){
					soilTemp = Double.parseDouble(value);
				}

				if (name.equals("internalTemp")){
					EtDp = Double.parseDouble(value);
				}
				if (name.equals("soilMoisture")){
					soilMoisture = Double.parseDouble(value);
				}

				if (name.equals("soilTemperature")){
					soilTemp = Double.parseDouble(value);
				}

				if (name.equals("temperature")){
					es1201Temp = Double.parseDouble(value);
				}
				if (name.equals("humidity")){
					es1201Humidity = Double.parseDouble(value);
				}
				if (name.equals("dewPoint")){
					es1201Dp = Double.parseDouble(value);
				}
				if (name.equals("Dp")){
					EtDp = Double.parseDouble(value);
				}
				if (name.equals("VWC")){
					EtVWC = Double.parseDouble(value);
				}
				if (name.equals("Ec")){
					EtEc = Double.parseDouble(value);
				}
				if (name.equals("Temp")){
					EtTemp = Double.parseDouble(value);
				}
			}	
			/* The last element is:
			 * 	<internal>
			 * 		<nodeId>2</nodeId>		// only in eN2100
			 * 		<yieldAppId>1</yieldAppId>
			 * 		<sensorTable>eN2100_internal_sensor_results</sensorTable>
			 * 	</internal>
			 */ 
			sensorTable = null;
			if (amType.equals(11)) {
				Element internal_field = (Element)fields.item(fields.getLength() - 1);
				Element sensorTableEle=(Element)internal_field.getElementsByTagName("sensorTable").item(0);
				sensorTable = sensorTableEle.getTextContent();
			}


			// end of second layer for 3


			logger.info("amType: " +amType +" Node ID: " + nodeid + " Packet Name  " + packetName );

			try { // try 1
        Serializable[] values = new Serializable[]{amType, nodeid, packetName,
                batteryV, solarV, enTemp,
                soilMoisture, soilTemp,
                es1201Temp, es1201Humidity, es1201Dp,
                EtDp, EtVWC, EtEc, EtTemp,
                sensorTable};
				streamEle = StreamElement.from(this);
        for (int i=0;i<values.length;i++)
          streamEle.set(FIELD_NAMES[i],values[i]);

				dataChannel.write(streamEle);

			}catch (Exception e){ // try 1
				logger.info(e.getMessage( ) , e );}


		}catch (Exception e) { // try 4
			logger.error( e.getMessage( ) , e );}

	}

	public boolean isTimeStampUnique() {
		return false;
	}

	public void stop() {
		isActive=false;

	}
}
