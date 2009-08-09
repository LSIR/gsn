package gsn.wrappers.xBow;

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

/**
 * This is a modified version of Lei Shu's xmlWrapper.
 * It works with a MicaZ WSN - WSN-PRO2400CA - MICAZ PROFESSIONAL KIT
 * The sensor boards contain:
 * 	a Sensirion SHT11 single chip humidity and temperature sensor;
 * 	an Intersema MS55ER barometric pressure and temperature sensor;
 * 	a TAOS Inc. TLS2550 digital light sensor and
 * 	an Analog Devices ADXL202 dual axis accelerometer.
 * @author bgpearn
 */

//TODO: This wrapper does not reconnect to a remote xServe if communication is lost.

public class xBowWrapper implements Wrapper {


  private final WrapperConfig conf;

  private final DataChannel dataChannel;

  private int                      DEFAULT_RATE       = 5000;

  private final transient Logger     logger                 = Logger.getLogger ( xBowWrapper.class );

  private static final String [ ]  FIELD_NAMES           = new String [ ] {
          "amType", "nodeid" , "voltage" ,
          "humid" , "humtemp" , "prtemp" ,"press",
          "taosch0", "taosch1", "taoch0"};

  private static final Byte [ ]    FIELD_TYPES           = new Byte [ ] {
          DataTypes.INTEGER ,DataTypes.INTEGER , DataTypes.INTEGER ,
          DataTypes.INTEGER ,DataTypes.INTEGER , DataTypes.DOUBLE , DataTypes.DOUBLE,
          DataTypes.INTEGER, DataTypes.INTEGER,DataTypes.DOUBLE};

  private static final String [ ]  FIELD_DESCRIPTION     = new String [ ] {
          "amType" ,"Node ID" , "Voltage of This Node" ,
          "Humidity" , "Temperature" , "PrTemp", "Pressure",
          "taosch0", "taosch1", "taoch0"};

  private static final String [ ]  FIELD_TYPES_STRING    = new String [ ] {
          "int" , "int" , "int" ,
          "int" , "int" , "double", "double",
          "int" , "int" , "double"};

  private DataField[]                outputStructure      ;

  private String                     host                ;

  private int                        port                ;

  private int                        rate                ;

  // fields of sensor node

  private int						amType ;
  private int                        nodeid              ;

  private int                        parent              ;

  private int                        group               ;

  private int                        voltage             ;

  private int                        humid               ;

  private int                        humtemp             ;
  private int						  taosch0;
  private int						  taosch1;
  private double						  taoch0;


  private double                     prtemp               ;
  private double                     press               ;

  private double                     accel_x             ;

  private double                     accel_y             ;

  // declare the socket object for client side
  private Socket                     xmlSocket = null    ;

  private BufferedReader             rd                  ;

  private StreamElement              streamEle           ;

  private  boolean                   add = false         ;

  private String                     s  = ""             ; // xml packet

  private String                     xmls                ;

  private DocumentBuilderFactory     domfac              ;

  private DocumentBuilder            dombuilder          ;

  private InputSource                ins                 ;

  private Document                   doc                 ;

  private boolean                    notEnd = true       ;

  private int                        k                   ;

  private String                     bs                  ;

  private int                        indexS              ;

  private int                        indexE              ;

  private boolean                    getxml              ;

  public xBowWrapper(WrapperConfig conf, DataChannel channel) {
    this.conf = conf;
    this.dataChannel= channel;

    /**
     * check the host and port parameters.
     */

    host = conf.getParameters().getValueWithException( "host" );
    port = conf.getParameters().getValueAsInt("port" ,ContainerConfig.DEFAULT_GSN_PORT);
    if ( port > 65000 || port <= 0 )
      throw new RuntimeException("Remote wrapper initialization failed, bad port number:"+port);

    rate = conf.getParameters().getValueAsInt( "rate" , DEFAULT_RATE);

    ArrayList<DataField > output = new ArrayList < DataField >();
    for ( int i = 0 ; i < FIELD_NAMES.length ; i++ )
      output.add( new DataField( FIELD_NAMES[ i ] , FIELD_TYPES_STRING[ i ]  ) );
    outputStructure = output.toArray( new DataField[] {} );

  }

  public void start(){

    //		   int n=0;

    try {

      // setup the socket connection
      xmlSocket = new Socket(host, port);

      rd = new BufferedReader(new InputStreamReader(xmlSocket.getInputStream()));

    } catch (IOException e){
      logger.warn(" The xml socket connection is not set up.");
      logger.warn(" Cannot read from xmlSocket. ");
    }

    while ( isActive ) {

      getxml = false;

      try {
        Thread.sleep(rate);
      } catch (InterruptedException e) {
        logger.error(e.getMessage(),e);
      }

      try { // try

        s = "";

        char[] c = new char[3000];

        // initialize this char[]
        for (int j = 0; j < c.length; j++){
          c[j] = 0;
        }

        rd.read(c);

        for (int j = 0; j < c.length; j++){
          s = s + c[j];
        }

        s = s.trim();

        if (s != ""){

          try{
            indexS = s.indexOf("<?xml");
            indexE = s.indexOf("</MotePacket>");
          }catch (Exception e){
            logger.error( e.getMessage( ) , e );
          }

          if (indexS < indexE) {
            if (indexS >= 0){
              bs = s.substring(indexS,(indexE+13) );
              if (bs.length() > 2000) {
                xmls = bs;
                getxml = true;
              }
              if (bs.length() < 2000) {
                try{
                  indexS = s.indexOf("<?xml", indexE);
                  indexE = s.indexOf("</MotePacket>", indexS);
                } catch (Exception e){
                  logger.error( e.getMessage( ) , e );
                }
                if (indexS < indexE) {
                  if (indexS >= 0){
                    bs = s.substring(indexS,(indexE+13) );
                    if (bs.length() > 2000) {
                      xmls = bs;
                      getxml = true;
                    }
                  }
                }

              }

            }


          }

        }


        try { // try 4

          if (getxml){ // if 1

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

              //logger.info(ins);

              // Pass xmls stream to XML Parser
              doc = dombuilder.parse(ins);
            } catch (SAXException e){ // try 2
              logger.info(e.getMessage( ) , e );
            } catch (NullPointerException e){
              logger.info(e.getMessage( ) , e );
            }


            // Get the root element of XML packet
            Element root = doc.getDocumentElement();

            // Get the first level Node list
            NodeList fields = root.getChildNodes();

            // Get all fields' name
            // start of second layer for 3
            for(int i=0;i<fields.getLength();i++){

              Element field = (Element)fields.item(i);

              String name;

              Element nameEle=(Element)field.getElementsByTagName("Name").item(0);

              name = nameEle.getTextContent();

              String value;

              Element valueEle=(Element)field.getElementsByTagName("ConvertedValue").item(0);

              value = valueEle.getTextContent();

              if (name.equals("amtype")){
                amType = Integer.parseInt(value);
              }

              if (name.equals("nodeid")){
                nodeid = Integer.parseInt(value);
              }

              if (name.equals("parent")){
                parent = Integer.parseInt(value);
              }

              if (name.equals("group")){
                group = Integer.parseInt(value);
              }

              if (name.equals("voltage")){
                voltage = Integer.parseInt(value);
              }

              if (name.equals("humid")){
                humid = Integer.parseInt(value);
              }

              if (name.equals("humtemp")){
                humtemp = Integer.parseInt(value);
              }

              if (name.equals("taosch0")){
                taosch0 = Integer.parseInt(value);
              }

              if (name.equals("taosch1")){
                taosch1 = Integer.parseInt(value);
              }

              if (name.equals("taoch0")){
                if (Double.isNaN(taoch0 = Double.parseDouble(value))){
                  taoch0 = 0 ;
                }
              }
              if (name.equals("prtemp")){
                prtemp = Double.parseDouble(value);
              }

              if (name.equals("press")){
                press = Double.parseDouble(value);
              }

              if (name.equals("accel_x")){
                accel_x = Double.parseDouble(value);
              }

              if (name.equals("accel_y")){

                accel_y = Double.parseDouble(value);
              }

            } // end of second layer for 3

            if (amType == 11) {
              try { // try 1
                Serializable[] values = new Serializable[]{
                        amType, nodeid, voltage,
                        humid, humtemp, prtemp,
                        press, taosch0, taosch1, taoch0};
                streamEle = StreamElement.from(this);
                for (int i=0;i<values.length;i++)
                  streamEle.set(FIELD_NAMES[i],values[i]);

                dataChannel.write(streamEle);

              }catch (Exception e){ // try 1
                logger.info(e.getMessage( ) , e );
              }

            }
          }; // end of if

        }catch (Exception e) { // try 4
          logger.error( e.getMessage( ) , e );
        }

      } catch (Exception e) { // try
        logger.error( e.getMessage( ) , e );
      }

    }   // while

  }  // run

  private boolean isActive=true;

  public void dispose ( ) {
  }

  public  DataField[] getOutputFormat ( ) {
    return outputStructure;
  }

  public void stop() {
    isActive = false;

  }

}
