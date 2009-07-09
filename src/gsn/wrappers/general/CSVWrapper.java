package gsn.wrappers.general;

import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.wrappers.AbstractWrapper;

import java.io.File;
import java.io.FileReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.log4j.Logger;
/**
 * Timezones: http://joda-time.sourceforge.net/timezones.html
 * Formatting: http://joda-time.sourceforge.net/apidocs/org/joda/time/format/DateTimeFormat.html
 *
 */
public class CSVWrapper extends AbstractWrapper {

	private final transient Logger               logger        = Logger.getLogger( CSVWrapper.class );

	private int                                  threadCounter = 0;

	private DataField [] dataField  ;

	private CSVHandler handler = new CSVHandler();

	private int samplingPeriodInMsc;

	private String checkPointDir;

	private String dataFile;

	public boolean initialize (  ) {
		setName( "CSVWrapper-Thread" + ( ++threadCounter ) );
		AddressBean addressBean = getActiveAddressBean( );
		dataFile = addressBean.getPredicateValueWithException("file");
		String csvFields = addressBean.getPredicateValueWithException("fields");
		String csvFormats = addressBean.getPredicateValueWithException("formats");
		String csvSeparator = addressBean.getPredicateValueWithDefault("separator",",");
		checkPointDir = addressBean.getPredicateValueWithDefault("check-point-directory", "./csv-check-points");
		String csvStringQuote = addressBean.getPredicateValueWithDefault("quote","\"");
		int skipFirstXLine = addressBean.getPredicateValueAsInt("skip-first-lines", 0);
		String timezone = addressBean.getPredicateValueWithDefault("timezone", handler.LOCAL_TIMEZONE_ID);
		String nullValues = addressBean.getPredicateValueWithDefault("bad-values", "");
		samplingPeriodInMsc = addressBean.getPredicateValueAsInt("sampling", 10000);

		if (csvSeparator!= null && csvSeparator.length()!=1) {
			logger.warn("The provided CSV separator:>"+csvSeparator+"< should only have  1 character, thus ignored and instead \",\" is used.");
			csvSeparator=",";
		}

		if (csvStringQuote.length()!=1) {
			logger.warn("The provided CSV quote:>"+csvSeparator+"< should only have 1 character, thus ignored and instead '\"' is used.");
			csvStringQuote="\"";
		}
		try {
			String checkPointFile = new File(checkPointDir).getAbsolutePath()+"/"+(new File(dataFile).getName())+"-"+addressBean.hashCode();

			if (handler.initialize(dataFile.trim(), csvFields, csvFormats, csvSeparator.toCharArray()[0], csvStringQuote.toCharArray()[0], skipFirstXLine, nullValues,timezone,checkPointFile)==false) 
				return false;
		}catch (Exception e) {
			logger.error("Loading the csv-wrapper failed:" +e.getMessage(),e);  
			return false;
		}

		dataField = handler.getDataFields();
		return true;
	}

	public void run ( ) {
		Exception preivousError = null;
		long previousModTime = -1;
		long previousCheckModTime = -1;
		while ( isActive( ) ) {
			File dataFile =  new File(handler.getDataFile());
			File chkPointFile =  new File(handler.getCheckPointFile());
			long lastModified = -1;
			long lastModifiedCheckPoint = -1;
			if (dataFile.isFile())
				lastModified = dataFile.lastModified();
			if (chkPointFile.isFile())
				lastModifiedCheckPoint = chkPointFile.lastModified();
			
			try {
				ArrayList<TreeMap<String, Serializable>> output = null;
				if (preivousError==null || (preivousError!=null && (lastModified != previousModTime || lastModifiedCheckPoint!=previousCheckModTime))){
					output = handler.run(new FileReader(handler.getDataFile()), checkPointDir);
					for (TreeMap<String, Serializable> se : output) {
						StreamElement streamElement = new StreamElement(se,getOutputFormat());
						boolean insertionSuccess = postStreamElement(streamElement);
						handler.updateCheckPointFile(streamElement.getTimeStamp());
					}
				}
				if (output==null || output.size()==0) //More intelligent sleeping, being more proactive once the wrapper receives huge files.
					Thread.sleep(samplingPeriodInMsc);
			}catch (Exception e) {
				if (preivousError!=null && preivousError.getMessage().equals(e.getMessage()))
					continue;
				logger.error(e.getMessage()+" :: "+dataFile,e);
				preivousError = e;
				previousModTime=lastModified;
				previousCheckModTime=lastModifiedCheckPoint;
			}
		}
	}

	public  DataField[] getOutputFormat ( ) {
		return dataField;
	}
	public String getWrapperName() {
		return this.getClass().getName();
	}
	public void finalize ( ) {
		threadCounter--;
	}

}