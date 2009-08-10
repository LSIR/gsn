package gsn2.wrappers;
import gsn.beans.DataField;
import gsn.beans.StreamElement;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import au.com.bytecode.opencsv.CSVReader;

/**
 * possible formats for the timestamp fields are available @ http://joda-time.sourceforge.net/api-release/org/joda/time/format/DateTimeFormat.html  
 * Possible timezone : http://joda-time.sourceforge.net/timezones.html
 */
public class CSVHandler {


    public static final String LOCAL_TIMEZONE_ID = DateTimeZone.getDefault().getID();

    private static Logger logger = Logger.getLogger(CSVHandler.class);

    private String timestampField;

    private CSVStreamElementFactory seFactory;


    public static DateTime parseTimestamp(String format,String value) throws IllegalArgumentException {
        DateTimeFormatter fmt = DateTimeFormat.forPattern(format);
        return fmt.parseDateTime(value);
    }

    private char stringSeparator,separator;
    private String dataFile;
    private DateTimeZone timeZone;
    private int skipFirstXLines ;
    private String[] fields , formats,nulls;

    private String checkPointFile;

    public CSVHandler(String dataFile, String inFields, String inFormats, char separator,char stringSeparator , int skipFirstXLines, String nullValues,String timeZone,String checkpointFile,String timestampField )   {

        this.stringSeparator = stringSeparator; // default to ,
        this.skipFirstXLines=skipFirstXLines;// default to 0
        this.dataFile = dataFile; // check if it exist.
        this.separator = separator;
        this.timeZone = DateTimeZone.forID(timeZone);
        this.checkPointFile=checkpointFile;
        this.timestampField = timestampField;
        File file = new File(dataFile);

        if (!file.isFile())
            throw new WrapperInitializationException("The specified CSV data file: "+dataFile+" doesn't exists.");
        try {
            getCheckPointFileIfAny(getCheckPointFile());
            this.fields=generateFieldIdx(inFields,true);
            this.formats = generateFieldIdx(inFormats,false);
            this.nulls = generateFieldIdx(nullValues,true);
            ////////////////////////
            // TODO: Check that the lengths are the same
            ////////////////////////

        }catch (IOException e) {
            throw new WrapperInitializationException(e);
        }
        if(fields.length!=formats.length) {
            throw new WrapperInitializationException("loading the csv-wrapper failed as the length of fields("+fields.length+") doesn't match the length of formats("+formats.length+")");
        }
        validateFormats(formats);
        validateFields(fields,formats);

        seFactory = new CSVStreamElementFactory(fields,formats);
    }

    private void validateFields(String[] fields, String[] types) {
        ArrayList<String> flds = new ArrayList<String>();
        for (int i=0;i<fields.length;i++){
            String f = fields[i].toLowerCase().trim();
            if(flds.contains(f) && !isTimeStampFormat(types[i]))
                throw new WrapperInitializationException("The field name: "+f+ " is used twice.");
            flds.add(f);
        }
        if (flds.isEmpty())
            throw new WrapperInitializationException("There isn't any field defined for the CSV wrapper.");
    }

    public static long getCheckPointFileIfAny(String checkPontFile) throws IOException {
        String chkPointDir = new File(new File(checkPontFile).getParent()).getAbsolutePath();
        new File(chkPointDir).mkdirs();
        File chkFile = new File(checkPontFile);
        chkFile.createNewFile();
        String val = FileUtils.readFileToString(chkFile,"UTF-8");
        if (val!=null && val.trim().length()>0)
            return Long.parseLong(val.trim());
        return 0;
    }

    public static void validateFormats(String[] formats) {
        for (int i=0;i<formats.length;i++) {
            if(formats[i].equalsIgnoreCase("numeric")||formats[i].equalsIgnoreCase("string"))
                continue;
            else if(isTimeStampFormat(formats[i])) {
                try {
                    String tmp = DateTimeFormat.forPattern(getTimeStampFormat(formats[i])).print(System.currentTimeMillis());
                }catch (IllegalArgumentException e) {
                    throw new WrapperInitializationException("Validating the time-format("+formats[i]+") used by the CSV-wrapper is failed. ");
                }
            }else {
                throw new WrapperInitializationException("The format ("+formats[i]+") used by the CSV-Wrapper doesn't exist.");
            }
        }
    }
    /**
     * Removes the space from the fields.
     * Split the rawFields using comma as the separator.
     * @param rawFields
     * @param toLowerCase, if false, the case is preserved. if true, the actual outputs will be in lower-case.
     * @return
     * @throws IOException
     */
    public static String[] generateFieldIdx(String rawFields,boolean toLowerCase) throws IOException {
        if(rawFields == null)
            return new String[0];
        String[] toReturn = new CSVReader(new StringReader(rawFields)).readNext();
        if(toReturn==null)
            return new String[0];
        for (int i=0;i<toReturn.length;i++) {
            toReturn[i] = toReturn[i].trim();
            if (toLowerCase)
                toReturn[i] = toReturn[i].toLowerCase();
        }
        return toReturn;
    }

    public ArrayList<StreamElement> process(Reader dataFile,long previousCheckPoint ) throws IOException {
        ArrayList<StreamElement>  toReturn = new ArrayList<StreamElement>();
        CSVReader reader = new CSVReader(dataFile,getSeparator(),getStringSeparator(),getSkipFirstXLines());
        String[] values = null;
        while ((values= reader.readNext())!=null) {
            StreamElement se=null ;
            try{
                se = convertTo(formats, fields, getNulls(), values, getSeparator());
            }catch (Exception e){
                logger.error(e.getMessage(),e);
            }
            if (se==null && !toReturn.isEmpty())
              break;
           
            if (se.isEmpty() || se.getTimeInMillis()<= previousCheckPoint)
                continue;

            toReturn.add(se);
            if(toReturn.size()>250)
                break; // Move outside the loop as in each call we only read 250 values;
        }
        reader.close();
        return toReturn;
    }

    public static void updateCheckPointFile(String checkPointFile, long timestamp) throws IOException {
        FileUtils.writeStringToFile(new File(checkPointFile), Long.toString(timestamp),"UTF-8");
    }

    public  StreamElement convertTo(String[] formats,String[] fields,String nullValues[], String[] values,char separator)   {
        StreamElement toReturn = seFactory.createStreamElement();

        HashMap<String, String> timeStampFormats = new HashMap<String, String>();
        HashMap<String, String> timeStampValues = new HashMap<String, String>();

        for (int i=0;i<Math.min(fields.length,values.length);i++) {
            if (isDeclaredAsNull(nullValues, values[i]) ) {
                continue;
            }else if (formats[i].equalsIgnoreCase("numeric")) {
                try {
                    toReturn.set(fields[i], Double.parseDouble(values[i]));
                }catch (java.lang.NumberFormatException e) {
                    logger.error("Parsing to Numeric fails: Value to parse="+values[i]);
                    throw e;
                }
            }else if (formats[i].equalsIgnoreCase("string"))
                toReturn.set(fields[i], values[i]);
            else if (isTimeStampFormat(formats[i])) {
                String value = "";
                String format = "";
                if (timeStampValues.get(fields[i])!=null ) {
                    value = timeStampValues.get(fields[i]);
                    format = timeStampFormats.get(fields[i]);
                    value+=separator;
                    format+=separator;
                }
                if (isTimeStampLeftPaddedFormat(formats[i]))
                    values[i]=StringUtils.leftPad(values[i], getTimeStampFormat(formats[i]).length(), '0');

                value+=values[i];
                format+=getTimeStampFormat(formats[i]);
                timeStampValues.put(fields[i], value);
                timeStampFormats.put(fields[i], format);
            }
        }

        for (String timeField: timeStampFormats.keySet()) {
            String timeValue =  timeStampValues.get(timeField);
            String timeFormat = timeStampFormats.get(timeField);
            try {
                DateTime x = DateTimeFormat.forPattern(timeFormat).withZone(getTimeZone()).parseDateTime(timeValue);
                if (timeField.equalsIgnoreCase(timestampField))
                    toReturn.setTime(x);
                toReturn.set(timeField,x);
            }catch (IllegalArgumentException e) {
                logger.error("Parsing error: TimeFormat="+timeFormat+" , TimeValue="+timeValue);
                logger.error(e.getMessage(),e);
                throw e;
            }
        }

        return toReturn;
    }

    public static String getTimeStampFormat(String input) {
        if (input.indexOf("timestampl(")>=0)
            return input.substring("timestampl(".length(),input.indexOf(")")).trim();
        else
            return input.substring("timestamp(".length(),input.indexOf(")")).trim();
    }

    public static boolean isTimeStampFormat(String input) {
        return (input.toLowerCase().startsWith("timestamp(") || input.toLowerCase().startsWith("timestampl(") )&& input.endsWith(")") ;
    }

    public static boolean isTimeStampLeftPaddedFormat(String input) {
        return input.toLowerCase().startsWith("timestampl(")&& input.endsWith(")") ;
    }


    public char getSeparator() {
        return separator;
    }

    public char getStringSeparator() {
        return stringSeparator;
    }

    public int getSkipFirstXLines() {
        return skipFirstXLines;
    }

    public static boolean isDeclaredAsNull(String[] possibleNullValues, String value) {
        if (value==null || value.length()==0)
            return true;
        for (int i=0;i<possibleNullValues.length;i++)
            if (possibleNullValues[i].equalsIgnoreCase(value.trim()))
                return true;
        return false;
    }

    public String[] getFields() {
        return fields;
    }

    public String[] getFormats() {
        return formats;
    }
    public String getDataFile() {
        return dataFile;
    }
    public String[] getNulls() {
        return nulls;
    }
    public void setSkipFirstXLines(int skipFirstXLines) {
        this.skipFirstXLines = skipFirstXLines;
    }
    public DateTimeZone getTimeZone() {
        return timeZone;
    }

    public String getCheckPointFile() {
        return checkPointFile;
    }

    public DataField[] getDataFields() {
        return seFactory.getFields();
    }

    private class CSVStreamElementFactory{
        private DataField[] fields;

        protected CSVStreamElementFactory(String fieldNames[],String formats[]) {
            HashMap<String, String> fields = new HashMap<String, String>();
            for (int i=0;i<fieldNames.length;i++) {
                String field = fieldNames[i];
                String type = formats[i];
                if (isTimeStampFormat(type))
                    fields.put(field, "time");
                else if (type.equalsIgnoreCase("numeric"))
                    fields.put(field,"numeric");
                else
                    fields.put(field,"string");
            }
            this.fields = new DataField[fields.size()];
            int i=0;
            for (String key : fields.keySet())
                this.fields[i++]=new DataField(key,fields.get(key));

        }
        public StreamElement createStreamElement(){
            return new StreamElement(fields){};
        }

        public DataField[] getFields() {
            return fields;
        }
    }

}
