package gsn.wrappers;


import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn2.wrappers.WrapperConfig;
import gsn.channels.DataChannel;
import gsn.ManualDataConsumer;
import gsn.utils.Parameter;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.net.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.xerces.parsers.DOMParser;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import au.com.bytecode.opencsv.CSVReader;
import gsn2.wrappers.Wrapper;
import gsn2.conf.Parameters;
import gnu.io.*;

public class StsPiezometerWrapper implements Wrapper {

	private final WrapperConfig conf;

	private final DataChannel dataChannel;

	// The first line describes the data logger, had to check for each file it reads.
	// The 2nd, 3rd and 4th lines are going to have data structure information for rest of the output
	// Time stamp is always the first column in the output.
	private static final String DateFormat = "HH:mm:ss dd.MM.yyyy";
	private static final String SvnDateFormat = "yyyy-MM-dd'T'HH:mm:ss";

	private static final String QUOTE = "\"";
	private static final String SAMPLING = "sampling";
	private static final String SKIP_LINES = "skip_lines";
	private static final String SEPERATOR = "seperator";

	private int sampling = -1; //in milliseconds.
	//	private int SAMPLING_DEFAULT = 10*60*1000; // 10 mins
	private int SAMPLING_DEFAULT = 1*60*1000; // 1 min for testing

	private static final String DIRECTORY = "directory";
	private String directory  =null; 

	private static final String DATADIRECTORY = "data_directory";
	private String datadirectory  =null;

	private static final String SVNURL = "svnurl";
	private String svnurl =null; 

	private static final String SVNLOGIN = "svnlogin";
	private String svnlogin =null; 

	private static final String SVNPASSWD = "svnpasswd";
	private String svnpasswd =null; 

	private boolean file_handling = true;

	private final transient Logger   logger             = Logger.getLogger( StsPiezometerWrapper.class );
	private DataField[] structure = {
			new DataField( "pressure" , "double" ),
			new DataField( "temperature" , "double" ),
			new DataField( "conductivity" , "double") };
	private SimpleDateFormat dateTimeFormat ;
	private SimpleDateFormat svnDateTimeFormat ;
	private long lastModified= 0;
	private long lastEnteredStreamelement =0;
	private int skip_lines = 3;
	private char seperator = '\t';

	private File statusFile = null;

	public static final String NOT_A_NUMBER = "not_a_number";
	private List<String> not_a_number_constants = new ArrayList<String>() ;

	public StsPiezometerWrapper(WrapperConfig conf, DataChannel channel) {
		this.conf = conf;
		this.dataChannel= channel;

		dateTimeFormat = new SimpleDateFormat( DateFormat );
		svnDateTimeFormat = new SimpleDateFormat( SvnDateFormat );
		sampling = conf.getParameters().getValueAsInt(SAMPLING, SAMPLING_DEFAULT);
		directory = conf.getParameters().getValueWithException(DIRECTORY);
		datadirectory = conf.getParameters().getValueWithException("data_directory");
		svnurl = conf.getParameters().getValueWithException(SVNURL);
		svnlogin = conf.getParameters().getValueWithException(SVNLOGIN);
		svnpasswd = conf.getParameters().getValueWithException(SVNPASSWD);

		skip_lines = conf.getParameters().getValueAsInt(SKIP_LINES, 4);
		String seperator_text =conf.getParameters().getValue(SEPERATOR);

		String not_a_number_constant_val = conf.getParameters().getValue(NOT_A_NUMBER);

		if (not_a_number_constant_val != null && not_a_number_constant_val.trim().length()>0) {
			StringTokenizer st = new StringTokenizer(not_a_number_constant_val,",");
			while (st.hasMoreTokens()) 
				not_a_number_constants.add(st.nextToken().trim());
		}
		if (seperator_text.equals("tab")) seperator='\t';
		else if (seperator_text.equals("space")) seperator=' ';
		else seperator = seperator_text.charAt(0);

		if (!readStatus()) 
			throw new RuntimeException("Bad Status."); 

		logger.info("wrapper correctly initialized");
	}

	private boolean readStatus(){
		String filename;
		if (file_handling) filename = datadirectory;
		else filename  = svnurl;
		filename = filename.replace('/','_');
		filename = filename.replace(':','_');
		filename = filename.replace('\\','_');
		statusFile = new File(directory+File.separator+filename+"_status.txt");
		String contents = null;
		if (statusFile.exists()){
			try {
				BufferedReader input =  new BufferedReader(new FileReader(statusFile));
				try {
					String line = null; //not declared within while loop
					while (( line = input.readLine()) != null){
						contents = line;
					}
				}
				finally {
					input.close();
				}
			}
			catch (IOException ex){
				ex.printStackTrace();
			}
			logger.warn("Content of the last line of the status file: "+contents);
			String[] list = contents.split(";");
			logger.warn("number of split elements: "+list.length+"  0:"+list[0]+"  1:"+list[1]);
			this.lastEnteredStreamelement = new Long(list[0]);
			this.lastModified = new Long(list[1]);
		} else {
			try {
				statusFile.createNewFile();
			} catch (IOException e) {
				logger.error("the status file can not be created "+statusFile.getAbsolutePath());
				return false;
			}
		}

		return true;
	}

	private void writeStatus(){
		try{
			FileWriter fstream = new FileWriter(statusFile);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(lastEnteredStreamelement+";"+lastModified);
			out.close();
		}catch (Exception e){//Catch exception if any
			logger.error("Error: " + e.getMessage());
		}
	}

	public  StreamElement rowToSE(String[] data) {
		Date date;
		StreamElement se = StreamElement.from(this);
		try {
			date = dateTimeFormat.parse(data[0]+" "+data[1]);
      se.setTime(date.getTime());
      Double[] values = removeTimestampFromRow(data);
      for (int i=0;i<getOutputFormat().length;i++)
        se.set(getOutputFormat()[i].getName(),values[i]);
		} catch (ParseException e) {
			logger.error("invalide date format! "+data[0]+" "+data[1]);
			logger.error(e.getMessage(),e);
		}finally {
			return se;
		}
	}

	public Double[] removeTimestampFromRow(String [] data) {
		Double[] toReturn = new Double[structure.length];
		next_val:for (int i=0;i<structure.length;i++) {
			String val = null;
			try{
				val = data[i+2].trim();
			}catch(Exception e){
				logger.warn("column out of bound: "+(i+2));
				toReturn[i] = null;
				continue next_val;
			}			
			for (String nan : not_a_number_constants) {
				if (val.equals(nan)) {
					toReturn[i] = null;
					continue next_val;
				}
			}
			try{
				toReturn[i] = Double.parseDouble(val);
			}catch(Exception e){
				logger.warn("data parsing exception " +e.toString());
				toReturn[i] = null;
			}
		}
		return toReturn;
	}

	private TreeMap<Long,String> getNewSvnDataAvailable(){
		TreeMap<Long,String> nameList = new TreeMap<Long,String>();
		try{
			logger.warn("start getNewSvnDataAvailable()");
			logger.warn("svnlogin:"+svnlogin+"   svnpasswd:"+svnpasswd);
			logger.warn("svnurl:"+svnurl);
			String cmd = "svn info "+svnurl+"/ --username '"+svnlogin+"' --password '"+svnpasswd+"' -R --xml";
			Process p = Runtime.getRuntime().exec(cmd);
			logger.warn("process initialized");
			InputStream in = p.getInputStream();

			DOMParser parser = new DOMParser();
			InputSource source = new InputSource(in);
			parser.parse(source);

			Document doc = parser.getDocument();
			NodeList entries = doc.getElementsByTagName("entry");
			logger.warn("entries: "+entries.getLength());
			for(int i=0;i<entries.getLength();i++){
				Element e = (Element) entries.item(i);
				logger.warn("kind: "+e.getAttribute("kind"));
				if( e.getAttribute("kind").equals("file")){
					String name = e.getAttribute("path");
					Element urlElem = (Element) e.getElementsByTagName("url").item(0);
					NodeList l = urlElem.getChildNodes();
					String url =null;
					String dateStr = null;
					for (int j=0;j<l.getLength();j++){
						Node n = l.item(j);
						if (n.getNodeType()==Node.TEXT_NODE){
							url = n.getNodeValue();
							break;
						}
					}
					Element dateElem = (Element) e.getElementsByTagName("date").item(0);
					l = dateElem.getChildNodes();
					for (int j=0;j<l.getLength();j++){
						Node n = l.item(j);
						if (n.getNodeType()==Node.TEXT_NODE){
							dateStr = n.getNodeValue();
							break;
						}
					}
					if (url!=null) logger.warn("url: "+url);
					if (dateStr!=null) logger.warn("dateStr: "+dateStr);
					Date date = svnDateTimeFormat.parse(dateStr.substring(0, SvnDateFormat.length()-2));
					if (date.getTime() > this.lastModified ){
						nameList.put(new Long(date.getTime()),url);
						logger.warn("add file: path name: "+name+"   url: "+url+"    date: "+dateStr);
					}
				}

			}

		} catch(IOException e){
			logger.error("the svn can not be updated: "+e.getMessage());
		} catch (SAXException e) {
			logger.error("the svn created XML is not valid: "+e.getMessage());
		} catch (DOMException e) {
			logger.error("the xml provided by the svn resulted in a DOM excoption "+e.getMessage());
		} catch (ParseException e) {
			logger.error("the date format provided by the svn resulted in a parsing exception "+e.getMessage());
		}

		return nameList;
	}

	public DataField[] getOutputFormat() {
		return structure;
	}

	private boolean isActive=true;

	public void dispose ( ) {
		writeStatus();
	}



	/**
	 * scan the directory for new files and return the files in an ordered list according
	 * to their modification date for inserting the data; an empty collection is returned 
	 * if there are no new files
	 * @return
	 */
	public TreeMap<Long,File> getNewFileDataAvailable(){
		File dir = new File(datadirectory);
		File[] list = dir.listFiles();
		TreeMap<Long,File> map = new TreeMap<Long,File>();
		long modified = this.lastModified;

		for (int i=0;i<list.length;i++){
			if ((list[i].getTotalSpace()>10) && !(list[i].getName().startsWith("."))){
				long l = list[i].lastModified();
				if (l>this.lastModified) {
					modified = l;
					map.put(new Long(l), list[i]);
				}
			}
		}
		return map;
	}

	public void start() {
		CSVReader reader = null;
		while (isActive) {
			try{
				Thread.sleep(sampling);
				logger.warn("new sampling started "+file_handling);
				if (file_handling){
					TreeMap<Long,File> list = getNewFileDataAvailable();
					for (Long modified: list.keySet()){
						File file = list.get(modified);
						logger.warn("processing the received file list "+file.getAbsolutePath());
						try {
							String[] data = null;
							reader = new CSVReader(new FileReader(file),seperator,'\"',skip_lines);
							logger.warn("parse file "+file.getAbsolutePath());
							while ((data =reader.readNext()) !=null) {
								//								if (data.length<(current_structure.length+1)) {
								//								logger.info("Possible empty line ignored.");
								//								continue;
								//								}
								StreamElement streamElement = rowToSE(data);
								if (streamElement.getTimeInMillis()>this.lastEnteredStreamelement){
									logger.warn("posting data");
									dataChannel.write(streamElement);
									this.lastEnteredStreamelement = streamElement.getTimeInMillis();
								}
							}
							this.lastModified = modified.longValue();
							writeStatus();
						} catch (Exception e) {
							logger.error("Error in reading/processing "+file);
							logger.error(e.getMessage(),e);
						} finally {
							if (reader!=null)
								try {
									reader.close();
								} catch (IOException e) {
								}
						}
					}
				} else {
					logger.warn("start svn data processing");
					TreeMap<Long,String> list = getNewSvnDataAvailable();
					logger.warn("the list has been derived; there are elements: "+list.size());
					for (Long modified:list.keySet()){
						String name = list.get(modified);
						logger.warn("processing the received file list "+name);
						try {
							String[] data = null;
							Process p = Runtime.getRuntime().exec("svn cat "+name+" --username '"+svnlogin+"' --password '"+svnpasswd+"' ");
							InputStream in = p.getInputStream();
							BufferedReader d = new BufferedReader(new InputStreamReader(in));
							reader = new CSVReader(d,seperator,'\"',skip_lines);
							logger.warn("parse file "+name);
							while ((data =reader.readNext()) !=null) {
								//								if (data.length<(current_structure.length+1)) {
								//								logger.info("Possible empty line ignored.");
								//								continue;
								//								}
								StreamElement streamElement = rowToSE(data);
								if (streamElement.getTimeInMillis()>this.lastEnteredStreamelement){
									logger.warn("posting data");
									dataChannel.write(streamElement);
									this.lastEnteredStreamelement = streamElement.getTimeInMillis();
								}
							}
							this.lastModified = modified.longValue();
							writeStatus();
						} catch (Exception e) {
							logger.error("Error in reading/processing "+name);
							logger.error(e.getMessage(),e);
						} finally {
							if (reader!=null)
								try {
									reader.close();
								} catch (IOException e) {
								}
						}
					}
				}
			} catch (InterruptedException e){
				logger.error(e.getMessage(), e);
			}
		}		
	}

	public void stop() {
		isActive=false;

	}

    public static class HttpGetWrapper implements Wrapper {
        private final WrapperConfig conf;

        private final DataChannel dataChannel;

        private int                      DEFAULT_RATE       = 2000;

        private final transient Logger   logger             = Logger.getLogger( HttpGetWrapper.class );

        private HttpURLConnection httpURLConnection;

        private URL url;

        private int                      rate;

        private transient final DataField [] outputStructure = new  DataField [] { new DataField( "data" , "binary:image/jpeg" ) };


        /**
         * From XML file it needs the followings :
         * <ul>
         * <li>url</li> The full url for retriving the binary data.
         * <li>rate</li> The interval in msec for updating/asking for new information.
         * <li>mime</li> Type of the binary data.
         * </ul>
         * @throws java.net.MalformedURLException
         */
        public HttpGetWrapper(WrapperConfig conf, DataChannel channel) throws MalformedURLException {
            this.conf = conf;
            this.dataChannel= channel;
            url = new URL( conf.getParameters().getValue( "url" ));

            rate = conf.getParameters().getValueAsInt( "rate",DEFAULT_RATE );
            logger.debug( "AXISWirelessCameraWrapper is now running @" + rate + " Rate." );
        }

        public void start(){
            ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream(1024*20);
            byte[] buffer = new byte[16*1024];
            BufferedInputStream content;
            while ( isActive ) {
                try {
                    Thread.sleep( rate );
                    httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.connect();
                    if ( httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_ACCEPTED ) continue;
                    content = new BufferedInputStream(httpURLConnection.getInputStream(),4096);
                    arrayOutputStream.reset();
                    int readIndex = -1;
                    while ( (readIndex= content.read(buffer))!=-1)
                        arrayOutputStream.write(buffer, 0, readIndex);
                    dataChannel.write(StreamElement.from(this).set(getOutputFormat()[0].getName(), arrayOutputStream.toByteArray()).setTime(System.currentTimeMillis()));
                } catch ( InterruptedException e ) {
                    logger.error( e.getMessage( ) , e );
                }catch (IOException e) {
                    logger.error( e.getMessage( ) , e );
                }
            }
        }


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

    /**
 * Modified to used RXTX (http://users.frii.com/jarvi/rxtx/) which a LGPL
     * replacement for javacomm. The Easiest way to install RXTX is from the binary
     * distribution which is available at
     * http://users.frii.com/jarvi/rxtx/download.html Links GSN to a sensor network
     * through serial port. <p/> The only needed parameter is the serial port
     * address, provided through xml. Default connection settings are 9600 8 N 1
     * Optional parameters for the XML file: - inputseparator: if set, use this as
     * divider between data 'packets' - baudrate: set serialport baudrate (default:
     * 9600) - flowcontrolmode: set serialport flowcontrol mode possible values are: -
     * FLOWCONTROL_NONE: Flow control off. - FLOWCONTROL_RTSCTS_IN: RTS/CTS flow
     * control on input. - FLOWCONTROL_RTSCTS_OUT: RTS/CTS flow control on output. -
     * FLOWCONTROL_XONXOFF_IN: XON/XOFF flow control on input. -
     * FLOWCONTROL_XONXOFF_OUT: XON/XOFF flow control on output. - databits: set
     * serialport databits (5, 6, 7 or 8) default is 8 - stopbits: set serialport
     * stopbits (1, 2 or 1.5) default is 1 - parity: set serialport parity possiblie
     * values are: - PARITY_EVEN: EVEN parity scheme. - PARITY_MARK: MARK parity
     * scheme. - PARITY_NONE: No parity bit. (default) - PARITY_ODD: ODD parity
     * scheme. - PARITY_SPACE: SPACE parity scheme.
     */
    public static class SerialWrapper implements Wrapper, SerialPortEventListener, ManualDataConsumer {

        private final WrapperConfig conf;

        private final DataChannel dataChannel;

        public static final String      RAW_PACKET    = "RAW_PACKET";

        private final transient Logger  logger        = Logger.getLogger( SerialWrapper.class );

        private SerialConnection wnetPort;

        public InputStream              is;

        private String                  serialPort;

        private String                  inputSeparator;

        private boolean                 useInputSeparator;

        private int                     flowControlMode;

        private int                     baudRate      = 9600;

        private int                     dataBits      = SerialPort.DATABITS_8;

        private int                     stopBits      = SerialPort.STOPBITS_1;

        private int                     parity        = SerialPort.PARITY_NONE;

        private  DataField [] dataField     ;

        private int output_format;

        private int packet_length = 100;

        /*
         * Needs the following information from XML file : serialport : the name of
         * the serial port (/dev/ttyS0...) Optional parameters for the XML file: -
         * inputseparator: if set, use this as divider between data 'packets' -
         * baudrate: set serialport baudrate (default: 9600) - flowcontrolmode: set
         * serialport flowcontrol mode possible values are: - FLOWCONTROL_NONE: Flow
         * control off. - FLOWCONTROL_RTSCTS_IN: RTS/CTS flow control on input. -
         * FLOWCONTROL_RTSCTS_OUT: RTS/CTS flow control on output. -
         * FLOWCONTROL_XONXOFF_IN: XON/XOFF flow control on input. -
         * FLOWCONTROL_XONXOFF_OUT: XON/XOFF flow control on output. - databits: set
         * serialport databits (5, 6, 7 or 8) default is 8 - stopbits: set serialport
         * stopbits (1, 2 or 1.5) default is 1 - parity: set serialport parity
         * possiblie values are: - PARITY_EVEN: EVEN parity scheme. - PARITY_MARK:
         * MARK parity scheme. - PARITY_NONE: No parity bit. (default) - PARITY_ODD:
         * ODD parity scheme. - PARITY_SPACE: SPACE parity scheme.
         */
        public SerialWrapper(WrapperConfig conf, DataChannel channel) {
            this.conf = conf;
            this.dataChannel= channel;

            serialPort = conf.getParameters().getValueWithException( "serialport" );
            inputSeparator = conf.getParameters().getValue( "inputseparator" );
            if ( inputSeparator == null )
                useInputSeparator = false;
            else
                useInputSeparator = true;

            String representation = conf.getParameters().getValue( "representation" );

            if ( representation == null || representation.equalsIgnoreCase("binary") ){
                output_format=0;
            }else if (representation.startsWith("string"))
                output_format=1;
            else
                throw new RuntimeException("The provided representation >"+representation+"< is not valid, possible values are binary , string");

            packet_length= conf.getParameters().getValueAsInt("packet-length",100);

            String newBaudRate = conf.getParameters().getValue( "baudrate" );
            if ( newBaudRate != null && newBaudRate.trim( ).length( ) > 0 ) {
                baudRate = Integer.parseInt( newBaudRate ); // TODO: check validity of
                // baudrate?
            }

            String newDataBits = conf.getParameters().getValue( "databits" );
            if ( newDataBits != null && newDataBits.trim( ).length( ) > 0 ) {
                switch ( Integer.parseInt( newDataBits ) ) {
                case 5 :
                    dataBits = SerialPort.DATABITS_5;
                    break;
                case 6 :
                    dataBits = SerialPort.DATABITS_6;
                    break;
                case 7 :
                    dataBits = SerialPort.DATABITS_7;
                    break;
                case 8 :
                    dataBits = SerialPort.DATABITS_8;
                    break;
                }
            }

            String newStopBits = conf.getParameters().getValue( "stopbits" );
            if ( newStopBits != null && newStopBits.trim( ).length( ) > 0 ) {
                float newstopbits = Float.parseFloat( newStopBits );

                if ( newstopbits == 1.0 ) stopBits = SerialPort.STOPBITS_1;
                if ( newstopbits == 2.0 ) stopBits = SerialPort.STOPBITS_2;
                if ( newstopbits == 1.5 ) stopBits = SerialPort.STOPBITS_1_5;
            }

            String newParity = conf.getParameters().getValue( "parity" );
            if ( newParity != null && newParity.trim( ).length( ) > 0 ) {
                if ( newParity.equals( "PARITY_EVEN" ) ) parity = SerialPort.PARITY_EVEN;
                if ( newParity.equals( "PARITY_MARK" ) ) parity = SerialPort.PARITY_MARK;
                if ( newParity.equals( "PARITY_NONE" ) ) parity = SerialPort.PARITY_NONE;
                if ( newParity.equals( "PARITY_ODD" ) ) parity = SerialPort.PARITY_ODD;
                if ( newParity.equals( "PARITY_SPACE" ) ) parity = SerialPort.PARITY_SPACE;
            }

            String newflowControlMode = conf.getParameters().getValue( "flowcontrolmode" );
            if ( newflowControlMode != null && newflowControlMode.trim( ).length( ) > 0 ) {
                flowControlMode = 0;

                String modes[] = newflowControlMode.split( "\\|" );

                for ( int i = 0 ; i < modes.length ; i++ ) {
                    if ( modes[ i ].equals( "FLOWCONTROL_NONE" ) ) flowControlMode |= SerialPort.FLOWCONTROL_NONE;
                    if ( modes[ i ].equals( "FLOWCONTROL_RTSCTS_IN" ) ) flowControlMode |= SerialPort.FLOWCONTROL_RTSCTS_IN;
                    if ( modes[ i ].equals( "FLOWCONTROL_RTSCTS_OUT" ) ) flowControlMode |= SerialPort.FLOWCONTROL_RTSCTS_OUT;
                    if ( modes[ i ].equals( "FLOWCONTROL_XONXOFF_IN" ) ) flowControlMode |= SerialPort.FLOWCONTROL_XONXOFF_IN;
                    if ( modes[ i ].equals( "FLOWCONTROL_XONXOFF_OUT" ) ) flowControlMode |= SerialPort.FLOWCONTROL_XONXOFF_OUT;
                }

                if ( flowControlMode == 0 ) {
                    flowControlMode = -1; // don't set flow control mode if it is
                    // empty or is only composed of invalid
                    // arguments
                }
            } else {
                flowControlMode = -1;
            }

            // TASK : TRYING TO CONNECT USING THE ADDRESS
            wnetPort = new SerialConnection( serialPort );
            if ( wnetPort.openConnection( ) == false )
                throw new RuntimeException("Connection failed!.");
            wnetPort.addEventListener( this );
            is = wnetPort.getInputStream( );

            if ( logger.isDebugEnabled( ) ) {
                logger.debug( "Serial port wrapper successfully opened port and registered itself as listener." );
            }

            inputBuffer = new byte [ MAXBUFFERSIZE ];
            dataField = new DataField[] { new DataField( RAW_PACKET , (output_format==0?"binary":"varchar")+"("+packet_length+")") };

        }

        /**
         * A class that handles the details of the serial connection.
         */

        public class SerialConnection {

            protected OutputStream     os;

            protected InputStream      is;

            private CommPortIdentifier portId;

            public SerialPort          sPort;

            private String             serialPort;

            private boolean            open;

            /**
             * Creates a SerialConnection object and initialiazes variables passed in
             * as params.
             *
             * @param serialPort A SerialParameters object.
             */
            public SerialConnection ( String serialPort ) {
                open = false;
                this.serialPort = serialPort;
            }

            /**
             * Attempts to open a serial connection (9600 8N1). If it is unsuccesfull
             * at any step it returns the port to a closed state, throws a
             * <code>SerialConnectionException</code>, and returns. <p/> Gives a
             * timeout of 30 seconds on the portOpen to allow other applications to
             * reliquish the port if have it open and no longer need it.
             */
            public boolean openConnection ( ) {
                // Obtain a CommPortIdentifier object for the port you want to open.
                try {
                    portId = CommPortIdentifier.getPortIdentifier( serialPort );
                } catch ( NoSuchPortException e ) {
                    logger.error( "Port doesn't exist : " + serialPort , e );
                    return false;
                }
                // Open the port represented by the CommPortIdentifier object.
                // Give the open call a relatively long timeout of 30 seconds to
                // allow a different application to reliquish the port if the user
                // wants to.
                if ( portId.isCurrentlyOwned( ) ) {
                    logger.error( "port owned by someone else" );
                    return false;
                }
                try {
                    sPort = ( SerialPort ) portId.open( "GSNSerialConnection" , 30 * 1000 );

                    sPort.setSerialPortParams( baudRate , dataBits , stopBits , parity );
                    if ( flowControlMode != -1 ) {
                        sPort.setFlowControlMode( flowControlMode );
                    }
                } catch ( PortInUseException e ) {
                    logger.error( e.getMessage( ) , e );
                    return false;
                } catch ( UnsupportedCommOperationException e ) {
                    logger.error( e.getMessage( ) , e );
                    return false;
                }

                // Open the input and output streams for the connection. If they
                // won't
                // open, close the port before throwing an exception.
                try {
                    os = sPort.getOutputStream( );
                    is = sPort.getInputStream( );
                } catch ( IOException e ) {
                    sPort.close( );
                    logger.error( e.getMessage( ) , e );
                    return false;
                }
                sPort.notifyOnDataAvailable( true );
                sPort.notifyOnBreakInterrupt( false );

                // Set receive timeout to allow breaking out of polling loop
                // during
                // input handling.
                try {
                    sPort.enableReceiveTimeout( 30 );
                } catch ( UnsupportedCommOperationException e ) {

                }
                open = true;
                return true;
            }

            /**
             * Close the port and clean up associated elements.
             */
            public void closeConnection ( ) {
                // If port is alread closed just return.
                if ( !open ) { return; }
                // Check to make sure sPort has reference to avoid a NPE.
                if ( sPort != null ) {
                    try {
                        os.close( );
                        is.close( );
                    } catch ( IOException e ) {
                        System.err.println( e );
                    }
                    sPort.close( );
                }
                open = false;
            }

            /**
             * Send a one second break signal.
             */
            public void sendBreak ( ) {
                sPort.sendBreak( 1000 );
            }

            /**
             * Reports the open status of the port.
             *
             * @return true if port is open, false if port is closed.
             */
            public boolean isOpen ( ) {
                return open;
            }

            public void addEventListener ( SerialPortEventListener listener ) {
                try {
                    sPort.addEventListener( listener );
                } catch ( TooManyListenersException e ) {
                    sPort.close( );
                    logger.warn( e.getMessage( ) , e );
                }
            }

            /**
             * Send a byte.
             */
            public void sendByte ( int i ) {
                try {
                    os.write( i );
                } catch ( IOException e ) {
                    System.err.println( "OutputStream write error: " + e );
                }
            }

            public InputStream getInputStream ( ) {
                return is;
            }

            public OutputStream getOutputStream ( ) {
                return os;
            }

        }

        public void start(){

        }

        public boolean handleExternalInput(String action, String[] paramNames,		Serializable[] paramValues) {
            if ( logger.isDebugEnabled( ) ) logger.debug( "Serial wrapper received a serial port sending..." );
            if ( !wnetPort.isOpen( ) ) throw new RuntimeException( "The connection is closed." );
            try {
                if ( logger.isDebugEnabled( ) ) logger.debug( "Serial wrapper performing a serial port sending." );
                if ( paramValues[0] instanceof byte [ ] )
                    wnetPort.getOutputStream( ).write( ( byte [ ] ) paramValues[0] );
                else { // general case, writes using the printwriter.
                    PrintWriter pw = new PrintWriter( wnetPort.getOutputStream( ) );
                    pw.write( paramValues[0].toString( ) );
                    pw.flush( );
                    pw.close( );
                }
                return true;
            } catch ( IOException e ) {
                logger.warn( "OutputStream write error. " , e );
                return false;
            }
        }

        public DataField [] getOutputFormat ( ) {
            return dataField;
        }

        public void dispose ( ) {
        }

        private static final int MAXBUFFERSIZE = 1024;

        private byte [ ]         inputBuffer;

        public void serialEvent ( SerialPortEvent e ) {
            //		if ( logger.isDebugEnabled( ) ) logger.debug( "Serial wrapper received a serial port event, reading..." );
            //		if ( !isActive || listeners.isEmpty( ) ) {
            //		if ( logger.isDebugEnabled( ) ) logger.debug( "Serial wrapper dropped the input b/c there is no listener there or the wrapper is inactive." );
            //		return;
            //		}
            // Determine type of event.
            switch ( e.getEventType( ) ) {
            // Read data until -1 is returned.
            case SerialPortEvent.DATA_AVAILABLE :
                /*
                 * int index = 0; while (newData != -1) { try { if (is == null) { if
                 * (logger.isDebugEnabled ()) logger.debug("SerialWrapper: Warning,
                 * is == null !"); is = wnetPort.getInputStream(); } else newData =
                 * is.read(); if (newData > -1 && newData < 256) {
                 * inputBuffer[index++] = (byte) newData; } } catch (IOException ex) {
                 * System.err.println(ex); return; } }
                 */
                try {
                    is.read( inputBuffer );
                } catch ( IOException ex ) {
                    logger.warn( "Serial port wrapper couldn't read data : " + ex );
                    return;
                }
                break;
                // If break event append BREAK RECEIVED message.
            case SerialPortEvent.BI :
                // messageAreaIn.append("\n--- BREAK RECEIVED ---\n");
            }

            if ( logger.isDebugEnabled( ) )
                logger.debug( new StringBuilder( "Serial port wrapper processed a serial port event, stringbuffer is now : " ).append( new String(inputBuffer) ).toString( ) );
            if ( useInputSeparator ) {
                for ( String chunk : new String(inputBuffer).split( inputSeparator ) )
                    if ( chunk.length( ) > 0 )
                        post_item(chunk);
            } else { //without separator character.
                post_item(new String(inputBuffer) );
            }
        }


        private void post_item (String val){
            switch (output_format){
            case 0: // for binary data
                dataChannel.write( StreamElement.from(this).set(RAW_PACKET,val.length()>packet_length?val.substring(0,packet_length).getBytes():val.getBytes() ).setTime(System.currentTimeMillis()));
                break;
            case 1: // for strings
                dataChannel.write( StreamElement.from(this).set(RAW_PACKET,val.length()>packet_length ? val.substring(0,packet_length):val).setTime(System.currentTimeMillis()) );
                break;
            }
        }
        public static void main ( String [ ] args ) {
            Properties properties = new Properties( );
            properties.put( "log4j.rootLogger" , "DEBUG,console" );
            properties.put( "log4j.appender.console" , "org.apache.log4j.ConsoleAppender" );
            properties.put( "log4j.appender.console.Threshold" , "DEBUG" );
            properties.put( "log4j.appender.console.layout" , "org.apache.log4j.PatternLayout" );
            properties.put( "log4j.appender.console.layout.ConversionPattern" , "%-6p[%d] [%t] (%13F:%L) %3x - %m%n" );
            PropertyConfigurator.configure( properties );
            Logger logger = Logger.getLogger( SerialWrapper.class );
            logger.info( "SerialWrapper Test Started" );

            ArrayList <Parameter> predicates = new ArrayList < Parameter >( );
            predicates.add( new Parameter( "serialport" , "/dev/ttyUSB0" ) );
            predicates.add( new Parameter( "inputseparator" , "(\n|\r|\f)" ) );
            predicates.add( new Parameter( "baudrate" , "57600" ) );
            predicates.add( new Parameter( "flowcontrolmode" , "FLOWCONTROL_NONE" ) );
            predicates.add( new Parameter( "databits" , "8" ) );
            predicates.add( new Parameter( "stopbits" , "1" ) );
            predicates.add( new Parameter( "parity" , "PARITY_NONE" ) );
            predicates.add( new Parameter( "host" , "localhost" ) );
            predicates.add( new Parameter( "port" , "22001" ) );
            SerialWrapper serialWrapper = new SerialWrapper( new WrapperConfig( "SerialWrapper" , new Parameters(predicates.toArray(new Parameter[] {}) ) ),null);
            serialWrapper.start();
        }

        public void stop() {
            wnetPort.closeConnection( );
        }




    }

    /**
 * Links GSN to a Wisenet sensors network. The computer running this wrapper
     * should be connected to an IP network. One of the WSN nodes should forward
     * received packets through UDP to the host running this wrapper.
     */
    public static class UDPWrapper implements Wrapper {

        private final WrapperConfig conf;

        private final DataChannel dataChannel;

        private static final String    RAW_PACKET    = "RAW_PACKET";

        private final transient Logger logger        = Logger.getLogger( UDPWrapper.class );

        public InputStream             is;

        private int                    port;

        private DatagramSocket socket;

        /*
         * Needs the following information from XML file : port : the udp port it
         * should be listening to rate : time to sleep between each packet
         */
        public UDPWrapper (WrapperConfig conf, DataChannel channel) throws SocketException {
            this.conf = conf;
            this.dataChannel= channel;
            port = conf.getParameters().getValueAsIntWithException( "port" ) ;
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
                    StreamElement streamElement = StreamElement.from(this).set(RAW_PACKET,receivedPacket.getData( )).setTime(System.currentTimeMillis( ) );
                    dataChannel.write( streamElement );
                } catch ( IOException e ) {
                    logger.warn( "Error while receiving data on UDP socket : " + e.getMessage( ) );
                }
            }
        }

        public  DataField [] getOutputFormat ( ) {
            return new DataField[] {new DataField( RAW_PACKET , "BINARY" ) };

        }

        private boolean isActive=true;

        public void dispose ( ) {

        }

        public void stop() {
            isActive = false;
        }
    }
}

