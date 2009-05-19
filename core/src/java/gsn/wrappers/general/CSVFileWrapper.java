package gsn.wrappers.general;

import au.com.bytecode.opencsv.CSVReader;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.wrappers.AbstractWrapper;
import org.apache.commons.collections.KeyValue;
import org.apache.log4j.Logger;

import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

public class CSVFileWrapper extends AbstractWrapper {


    private static final String PARAM_FILE = "file";

    private static final String PARAM_SKIP_LINES = "skip_lines";

    private static final String PARAM_SEPERATOR = "seperator";

    private static final String PARAM_DATE_FORMAT = "dateFormat";

    private static final String PARAM_QUOTE = "quote";

    private static int threadCounter = 0;

    private final transient Logger logger = Logger.getLogger(CSVFileWrapper.class);

    private String dateFormat = "HH:mm:ss-dd.MM.yyyy";

    private static DataField[] structure;

    private String filename;

    private int columns;

    private int skip_lines = 0;

    private char seperator = '\t';

    private char quote = '#';

    private long lastTime = 0L;

    /**
     */
    public boolean initialize() {
        logger.warn("cvsfile wrapper initialize started...");
        int column = 0;
        Vector v = new Vector();
        for (KeyValue predicate : getActiveAddressBean().getPredicates()) {
            String key = (String) predicate.getKey();
            String value = (String) predicate.getValue();
            logger.debug(v.size() + ": type value: " + key);
            if (key.equals(PARAM_FILE))
                filename = value;
            else if (key.equals(PARAM_SKIP_LINES))
                skip_lines = Integer.valueOf(value).intValue();
            else if (key.equals(PARAM_SEPERATOR))
                seperator = value.charAt(0);
            else if (key.equals(PARAM_FILE))
                quote = value.charAt(0);
            else if (key.equals(PARAM_DATE_FORMAT))
                dateFormat = value;
            else if (key.equals("time"))
                v.add(new DataField(value, "bigint"));
            else v.add(new DataField(value, key));
        }
        this.columns = v.size();
        this.structure = new DataField[columns];
        for (int i = 0; i < columns; i++)
            structure[i] = (DataField) v.get(i);
        logger.warn("cvsfile wrapper initialize completed ...");
        return true;
    }

    public void run() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        logger.warn("cvsfile wrapper run started...");
        // Parse the data
        try {//i/o may go wrong
            CSVReader reader = new CSVReader(new FileReader(filename), seperator, quote, skip_lines);
            String[] nextLine;
            SimpleDateFormat dateTimeForm = new SimpleDateFormat(dateFormat);
            Date date = null;
            long time;
            Serializable[] serialized = new Serializable[columns];

            while ((nextLine = reader.readNext()) != null) {
                int k = 0;
                time = 0;
                logger.debug("length: " + nextLine.length);
                for (int j = 0; j < Math.min(columns, nextLine.length); j++) {
                    logger.debug("Type ID of " + nextLine[j] + "  : " + structure[j].getDataTypeID());
                    if (structure[j].getDataTypeID() == DataTypes.NUMERIC) {
                        date = dateTimeForm.parse(nextLine[j]);
                        if (date == null) {
                            logger.error("invalide date format! " + nextLine[j]);
                            serialized[k++] = null;
                        } else {
                            time = date.getTime();
                            serialized[k++] = time;
                        }
                    }
                    if (structure[j].getDataTypeID() == DataTypes.NUMERIC) {
                        try {
                            Double d = Double.valueOf(nextLine[j]);
                            if (d == null) {
                                logger.error("invalide double format for " + nextLine[j] + " at timestamp " + time);
                                serialized[k++] = null;
                            } else serialized[k++] = d.doubleValue();
                        } catch (NumberFormatException e) {
                            logger.error("wrong double format for :" + nextLine[j] + " at timestamp " + time);
                            logger.error(e);
                            serialized[k++] = null;
                        }

                        logger.debug("double: " + nextLine[j]);
                    }
                }// end of j loop
//				logger.debug("-----");
                String str = "";
                for (int j = 0; j < serialized.length; j++)
                    str = str + "," + serialized[j];
                logger.debug("serialized: " + str);
                logger.debug("time: " + time);
                logger.debug("system time: " + System.currentTimeMillis());
                lastTime = time;
                try {
                    StreamElement stream = new StreamElement(structure, serialized);
                    postStreamElement(stream);
                } catch (ArrayIndexOutOfBoundsException e) {
                    logger.error("the number of columns for the row with timestamp " + time + " is not sufficient");
                }
            }// end of while loop

        } catch (IOException e) {
            logger.error("the file " + filename + " is not accessible!", e);
        } catch (ParseException e) {
            logger.error("there has been a parse excpetion! ", e);
        }
        logger.warn("processing of file " + filename + " completed.....");
    }


    public String getWrapperName() {
        return "CSV File Wrapper";
    }

    public void finalize() {
        logger.warn("cvsfile wrapper initialize completed ...");

        threadCounter--;
    }

    @Override
    public DataField[] getOutputFormat() {
        logger.debug("getOutputFormat called addressBean" + structure.toString());
        return structure;
    }
}
