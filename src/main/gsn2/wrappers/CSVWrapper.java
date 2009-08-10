package gsn2.wrappers;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn2.wrappers.WrapperConfig;
import gsn.channels.DataChannel;
import gsn2.wrappers.Wrapper;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TimerTask;
import java.util.Timer;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

/**
 * Timezones: http://joda-time.sourceforge.net/timezones.html
 * Formatting: http://joda-time.sourceforge.net/apidocs/org/joda/time/format/DateTimeFormat.html
 *
 */
public class CSVWrapper extends TimerTask implements Wrapper {

    private ScheduledThreadPoolExecutor scheduler = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(5);

    private final WrapperConfig conf;

    private boolean loggedNoChange=false; // to avoid duplicate logging messages when there is no change

    private final DataChannel dataChannel;

    private final transient Logger               logger        = Logger.getLogger( CSVWrapper.class );

    private DataField [] dataField  ;

    private CSVHandler handler ;

    private int samplingPeriodInMsc;

    private String checkPointFile;

    private String dataFile;

    private String timestampField;
    
    private ScheduledFuture localScheduler;

    public CSVWrapper(WrapperConfig conf, DataChannel channel) {
        this.conf = conf;
        this.dataChannel= channel;

        dataFile = conf.getParameters().getValueWithException("file");
        String csvFields = conf.getParameters().getValueWithException("fields");
        timestampField = conf.getParameters().getValueWithDefault("timestamp-field","timed");
        String csvFormats = conf.getParameters().getValueWithException("formats");
        String csvSeparator = conf.getParameters().getValueWithDefault("separator",",");
        checkPointFile = conf.getParameters().getValueWithException("check-point-path");
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
        handler = new CSVHandler(dataFile.trim(), csvFields, csvFormats, csvSeparator.toCharArray()[0], csvStringQuote.toCharArray()[0], skipFirstXLine, nullValues,timezone,checkPointFile,timestampField);
        dataField = handler.getDataFields();
    }

    public void start(){
        localScheduler = scheduler.schedule(this, 0, TimeUnit.MICROSECONDS);
    }

    public  DataField[] getOutputFormat ( ) {
        return dataField;
    }

    public void dispose ( ) {

    }

    public void stop() {
        localScheduler.cancel(true);
    }

    private Exception preivousError = null;
    private long previousModTime = -1;
    private long previousCheckModTime = -1;

    public void run() {
        File dataFile =  new File(handler.getDataFile());
        File chkPointFile =  new File(handler.getCheckPointFile());
        long lastModified = -1;
        long lastModifiedCheckPoint = -1;
        lastModified = dataFile.lastModified();
        lastModifiedCheckPoint = chkPointFile.lastModified();

        FileReader reader = null;
        try {
            ArrayList<StreamElement> output = null;
            if (preivousError==null || (preivousError!=null && (lastModified != previousModTime || lastModifiedCheckPoint!=previousCheckModTime))){
                reader = new FileReader(handler.getDataFile());
                output = handler.process(reader,handler.getCheckPointFileIfAny(handler.getCheckPointFile()));
                if (output.isEmpty()){
                    if (logger.isDebugEnabled() && output.size()==0 && loggedNoChange==false) {
                        logger.debug("There is no new item after most recent checkpoint(previousCheckPoint:"+new DateTime(handler.getCheckPointFileIfAny(handler.getCheckPointFile()))+").");
                        loggedNoChange=true;
                    }
                }
                else{
                    for (StreamElement data : output) {
                        dataChannel.write(data);
                        handler.updateCheckPointFile(handler.getCheckPointFile(),data.getTimeInMillis());
                    }
                    loggedNoChange=false;
                }
            }
            if (output!=null && output.size()>0){
                localScheduler = scheduler.schedule(this, 0, TimeUnit.MICROSECONDS);
                //More intelligent sleeping, being more proactive once the wrapper receives huge files.
            }else{
                  localScheduler = scheduler.schedule(this, samplingPeriodInMsc, TimeUnit.MICROSECONDS);
            }


        }catch (Exception e) {
            if (preivousError!=null && preivousError.getMessage().equals(e.getMessage()))
                return;
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