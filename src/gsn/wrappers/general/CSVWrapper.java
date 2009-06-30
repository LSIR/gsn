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

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
/**
 * Timezones: http://joda-time.sourceforge.net/timezones.html
 * Formatting: http://joda-time.sourceforge.net/apidocs/org/joda/time/format/DateTimeFormat.html
 *
 */
public class CSVWrapper extends AbstractWrapper {

	private final transient Logger               logger        = Logger.getLogger( CSVWrapper.class );

	private int                                  threadCounter = 0;

	private static   DataField [] dataField  ;

	private CSVHandler handler = new CSVHandler();

	private int samplingPeriodInMsc;

	private String checkPointDir;

	public boolean initialize (  ) {
		setName( "CSVWrapper-Thread" + ( ++threadCounter ) );
		AddressBean addressBean = getActiveAddressBean( );
		String dataFile = addressBean.getPredicateValueWithException("file");
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
		while ( isActive( ) ) {
			try {
				ArrayList<TreeMap<String, Serializable>> output = handler.run(new FileReader(handler.getDataFile()), checkPointDir);
				for (TreeMap<String, Serializable> se : output) {
					StreamElement streamElement = new StreamElement(se,getOutputFormat());
					boolean insertionSuccess = postStreamElement(streamElement);
					System.out.println(insertionSuccess+" -- "+streamElement.toString());
					handler.updateCheckPointFile(streamElement.getTimeStamp());
				}
				Thread.sleep(samplingPeriodInMsc);
			}catch (Exception e) {
				logger.error(e.getMessage(),e);
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