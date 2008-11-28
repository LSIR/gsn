package gsn.wrappers;


import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.wrappers.AbstractWrapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.log4j.Logger;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import au.com.bytecode.opencsv.CSVReader;

public class StsPiezometerWrapper extends AbstractWrapper {

	// The first line describes the data logger, had to check for each file it reads.
	// The 2nd, 3rd and 4th lines are going to have data structure information for rest of the output
	// Time stamp is always the first column in the output.
	private static final String DateFormat = "HH:mm:ss dd.MM.yyyy";
	private static final String SvnDateFormat = "yyyy-MM-ddTHH:mm:ss";

	private static final String QUOTE = "\"";
	private static final String SAMPLING = "sampling";
	private static final String SKIP_LINES = "skip_lines";
	private static final String SEPERATOR = "seperator";

	private int sampling = -1; //in milliseconds.
//	private int SAMPLING_DEFAULT = 10*60*1000; // 10 mins
	private int SAMPLING_DEFAULT = 1*60*1000; // 1 min for testing

	private static final String DIRECTORY = "directory";
	private String directory  =null; 

	private static final String SVNURL = "svnurl";
	private String svnurl =null; 

	private static final String SVNLOGIN = "svnlogin";
	private String svnlogin =null; 

	private static final String SVNPASSWD = "svnpasswd";
	private String svnpasswd =null; 

	private boolean file_handling = true;

	private final transient Logger   logger             = Logger.getLogger( StsPiezometerWrapper.class );
	private DataField[] structure = {
			new DataField( "pressure" , "double" , "pressure"),
			new DataField( "temperature" , "double" , "water temperatured"),
			new DataField( "conductivity" , "double" , "electric conductivity"),
			new DataField( "counter" , "double" , "counter") };
	private int threadCounter=0;
	private SimpleDateFormat dateTimeFormat ;
	private SimpleDateFormat svnDateTimeFormat ;
	private long lastModified= -1;
	private long lastEnteredStreamelement =0;
	private int skip_lines = 3;
	private char seperator = '\t';

	private File statusFile = null;

	public static final String NOT_A_NUMBER = "not_a_number";
	private List<String> not_a_number_constants = new ArrayList<String>() ;

	public boolean initialize() {
		setName( "StsPiezometerWrapper-Thread:" + ( ++threadCounter ) );
		dateTimeFormat = new SimpleDateFormat( DateFormat );
		svnDateTimeFormat = new SimpleDateFormat( SvnDateFormat );
		sampling = getActiveAddressBean( ).getPredicateValueAsInt(SAMPLING, SAMPLING_DEFAULT);
		directory = getActiveAddressBean().getPredicateValue(DIRECTORY);
		svnurl = getActiveAddressBean().getPredicateValue(SVNURL);
		if (svnurl!=null && svnurl.length()>0){
			if (directory==null||directory.length()==0){
				logger.error("The wrapper failed, the "+DIRECTORY+" parameter is missing.");
				return false;
			}
			file_handling = false;
			svnlogin = getActiveAddressBean().getPredicateValue(SVNLOGIN);
			svnpasswd = getActiveAddressBean().getPredicateValue(SVNPASSWD);
		}
		skip_lines = getActiveAddressBean().getPredicateValueAsInt(SKIP_LINES, 4);
		String seperator_text = getActiveAddressBean().getPredicateValue(SEPERATOR);

		String not_a_number_constant_val = getActiveAddressBean().getPredicateValue(NOT_A_NUMBER);

		if (not_a_number_constant_val != null && not_a_number_constant_val.trim().length()>0) {
			StringTokenizer st = new StringTokenizer(not_a_number_constant_val,",");
			while (st.hasMoreTokens()) 
				not_a_number_constants.add(st.nextToken().trim());
		}
		if (seperator_text.equals("tab")) seperator='\t';
		else if (seperator_text.equals("space")) seperator=' ';
		else seperator = seperator_text.charAt(0);

		// initialization of status of the wrapper
//		boolean ret;
		if (!readStatus()) return false; 

//		if (file_handling) ret = fileBasedInit();
//		else ret = svnBasedInit();

		logger.warn("wrapper correctly initialized");
		return true;
	}

	private boolean readStatus(){
		statusFile = new File(directory+File.pathSeparator+"status.txt");
		String contents = null;
		if (statusFile.exists()){
			try {
				//use buffering, reading one line at a time
				//FileReader always assumes default encoding is OK!
				BufferedReader input =  new BufferedReader(new FileReader(statusFile));
				try {
					String line = null; //not declared within while loop
					/*
					 * readLine is a bit quirky :
					 * it returns the content of a line MINUS the newline.
					 * it returns null only for the END of the stream.
					 * it returns an empty String if two newlines appear in a row.
					 */
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
			this.lastEnteredStreamelement = Long.getLong(list[0]).longValue();
			this.lastModified = Long.getLong(list[1]).longValue();
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
			// Create file 
			FileWriter fstream = new FileWriter(statusFile);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(lastEnteredStreamelement+";"+lastModified);
			//Close the output stream
			out.close();
		}catch (Exception e){//Catch exception if any
			logger.error("Error: " + e.getMessage());
		}
	}

//	private boolean fileBasedInit(){
//	File dir = new File(directory);
//	if (!dir.exists()){
//	logger.error("the specified directory "+directory+"does not link to a valid directory!");
//	return false;
//	}

//	File[] list = dir.listFiles();
//	File file = null;
//	for (int i=0;i<list.length;i++){
//	if (list[i].getTotalSpace()>10 && !list[i].equals(statusFile)){
//	long l = list[i].lastModified();
//	if (l>this.lastModified) {
//	this.lastModified=l;
//	file = list[i];
//	}
//	}
//	}
//	if (file==null){
//	this.lastEnteredStreamelement = 0;
//	this.lastModified = 0;
//	} else {
//	logger.warn("last file modified "+file.getAbsolutePath());
//	CSVReader reader;
//	try {
//	reader = new CSVReader(new FileReader(file),seperator,'\"',skip_lines);
//	String[] data;
//	String timeString = null;
//	String dateString = null;
//	while ((data =reader.readNext()) !=null) {
//	timeString = data[0];
//	dateString = data[1];
//	}
//	if ((timeString!=null) && (dateString !=null)){
//	Date date = dateTimeFormat.parse(timeString+" "+dateString);
//	this.lastEnteredStreamelement = date.getTime();
//	}
//	} catch (FileNotFoundException e) {
//	logger.error("last file during initialization not found!");
//	e.printStackTrace();
//	} catch (IOException e) {
//	logger.error("IO exception while reading the last modified file during initialization!");
//	e.printStackTrace();
//	} catch (ParseException e) {
//	logger.error("exception while parsing the timestamp of the last modified file during initialization!");
//	e.printStackTrace();
//	}

//	}
//	return true;
//	}

//	private boolean svnBasedInit(){
//	return true;
//	}

	public  StreamElement rowToSE(String[] data) {
		Date date;
		StreamElement se = null;
		try {
			date = dateTimeFormat.parse(data[0]+" "+data[1]);
			se = new StreamElement(structure,removeTimestampFromRow(data),date.getTime());
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

	public void run() {
		CSVReader reader = null;
		while (true) {
			try{
				Thread.sleep(sampling);
				logger.warn("new sampling started");
				if (file_handling){
					Collection<File> list = getNewFileDataAvailable();
					list.remove(statusFile);
					for(File file: list){
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
								if (streamElement.getTimeStamp()>this.lastEnteredStreamelement){
									logger.warn("posting data");
									postStreamElement(streamElement);
									this.lastEnteredStreamelement = streamElement.getTimeStamp();
								}
							}
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
					Collection<String> list = getNewSvnDataAvailable();
					for(String name: list){
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
								if (streamElement.getTimeStamp()>this.lastEnteredStreamelement){
									logger.warn("posting data");
									postStreamElement(streamElement);
									this.lastEnteredStreamelement = streamElement.getTimeStamp();
								}
							}
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

	private Collection<String> getNewSvnDataAvailable(){
		TreeSet<String> nameList = new TreeSet<String>();
		try{
			Process p = Runtime.getRuntime().exec("svn info "+svnurl+" --username '"+svnlogin+"' --password '"+svnpasswd+"' -R --xml");
			InputStream in = p.getInputStream();
			DOMParser parser = new DOMParser();
			InputSource source = new InputSource(in);
			parser.parse(source);

			Document doc = parser.getDocument();
			NodeList entries = doc.getElementsByTagName("entry");
			for(int i=0;i<entries.getLength();i++){
				Element e = (Element) entries.item(i);
				if( e.getAttribute("kind").equals("file")){
					String name = e.getAttribute("path");
					Element urlElem = (Element) e.getElementsByTagName("url").item(0);
					String url = urlElem.getNodeValue();
					Element dateElem = (Element) e.getElementsByTagName("date").item(0);
					Date date = svnDateTimeFormat.parse(dateElem.getNodeValue().substring(0, SvnDateFormat.length()));
					if (date.getTime() > this.lastModified && !name.equals("status.txt"))
						nameList.add(url);
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

	public String getWrapperName() {
		return "STS Piezometer Wrapper";
	}

	public void finalize() {
		threadCounter--; 
		writeStatus();
	}


	/**
	 * scan the directory for new files and return the files in an ordered list according
	 * to their modification date for inserting the data; an empty collection is returned 
	 * if there are no new files
	 * @return
	 */
	public Collection<File> getNewFileDataAvailable(){
		File dir = new File(directory);
		File[] list = dir.listFiles();
		TreeMap<Long,File> map = new TreeMap<Long,File>();
		long modified = this.lastModified;

		for (int i=0;i<list.length;i++){
			if (list[i].getTotalSpace()>10){
				long l = list[i].lastModified();
				if (l>this.lastModified) {
					modified = l;
					map.put(new Long(l), list[i]);
				}
			}
		}
		this.lastModified = modified;
		return map.values();
	}
}

