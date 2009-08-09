package gsn.wrappers.general;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.beans.WrapperConfig;
import gsn.channels.DataChannel;
import gsn.wrappers.Wrapper;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.log4j.Logger;
/**
 * Timezones: http://joda-time.sourceforge.net/timezones.html
 * Formatting: http://joda-time.sourceforge.net/apidocs/org/joda/time/format/DateTimeFormat.html
 *
 */
public class CSVWrapper implements Wrapper {
	private final WrapperConfig conf;

	private final DataChannel dataChannel;

	private final transient Logger               logger        = Logger.getLogger( CSVWrapper.class );

	private DataField [] dataField  ;

	private CSVHandler handler = new CSVHandler();

	private int samplingPeriodInMsc;

	private String checkPointDir;

	private String dataFile;

	public CSVWrapper(WrapperConfig conf, DataChannel channel) {
		this.conf = conf;
		this.dataChannel= channel;

		dataFile = conf.getParameters().getValueWithException("file");
		String csvFields = conf.getParameters().getValueWithException("fields");
		String csvFormats = conf.getParameters().getValueWithException("formats");
		String csvSeparator = conf.getParameters().getValueWithDefault("separator",",");
		checkPointDir = conf.getParameters().getValueWithDefault("check-point-directory", "./csv-check-points");
		String csvStringQuote = conf.getParameters().getValueWithDefault("quote","\"");
		int skipFirstXLine = conf.getParameters().getValueAsInt("skip-first-lines", 0);
		String timezone = conf.getParameters().getValueWithDefault("timezone", handler.LOCAL_TIMEZONE_ID);
		String nullValues = conf.getParameters().getValueWithDefault("bad-values", "");
		samplingPeriodInMsc = conf.getParameters().getValueAsInt("sampling", 10000);

		if (csvSeparator!= null && csvSeparator.length()!=1) {
			logger.warn("The provided CSV separator:>"+csvSeparator+"< should only have  1 character, thus ignored and instead \",\" is used.");
			csvSeparator=",";
		}

		if (csvStringQuote.length()!=1) {
			logger.warn("The provided CSV quote:>"+csvSeparator+"< should only have 1 character, thus ignored and instead '\"' is used.");
			csvStringQuote="\"";
		}
		String checkPointFile = new File(checkPointDir).getAbsolutePath()+"/"+(new File(dataFile).getName())+"-"+conf.hashCode();

		if (handler.initialize(dataFile.trim(), csvFields, csvFormats, csvSeparator.toCharArray()[0], csvStringQuote.toCharArray()[0], skipFirstXLine, nullValues,timezone,checkPointFile)==false) 
			throw new RuntimeException("Handler's initialization failed.");

		dataField = handler.getDataFields();
	}

	public void start(){
		Exception preivousError = null;
		long previousModTime = -1;
		long previousCheckModTime = -1;
		while ( isActive ) {
			File dataFile =  new File(handler.getDataFile());
			File chkPointFile =  new File(handler.getCheckPointFile());
			long lastModified = -1;
			long lastModifiedCheckPoint = -1;
			if (dataFile.isFile())
				lastModified = dataFile.lastModified();
			if (chkPointFile.isFile())
				lastModifiedCheckPoint = chkPointFile.lastModified();
			FileReader reader = null;
			try {
				ArrayList<TreeMap<String, Serializable>> output = null;
				if (preivousError==null || (preivousError!=null && (lastModified != previousModTime || lastModifiedCheckPoint!=previousCheckModTime))){

					reader = new FileReader(handler.getDataFile());
					output = handler.run(reader, checkPointDir);
          for (TreeMap<String, Serializable> data : output) {
            StreamElement se = StreamElement.from(this);
            se.setTime(System.currentTimeMillis());
            for (String key:data.keySet())
              se.set(key,data.get(key));

						dataChannel.write(se);
						handler.updateCheckPointFile(se.getTimed());
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
			}finally {
				if (reader!=null)
					try {
						reader.close();
					} catch (IOException e) {
						logger.debug(e.getMessage(),e);
					}
			}
		}
	}

	public  DataField[] getOutputFormat ( ) {
		return dataField;
	}

	private boolean isActive=true;

	public void dispose ( ) {

	}

	public void stop() {
		isActive = false;
	}

}