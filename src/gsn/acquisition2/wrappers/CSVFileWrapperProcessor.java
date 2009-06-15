package gsn.acquisition2.wrappers;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.log4j.Logger;
import au.com.bytecode.opencsv.CSVReader;
import gsn.acquisition2.messages.DataMsg;
import gsn.beans.DataField;
import gsn.beans.DataTypes;

public class CSVFileWrapperProcessor extends SafeStorageAbstractWrapper {

	CSVFileWrapperParameters parameters = null;

	private final transient Logger logger = Logger.getLogger( CSVFileWrapperProcessor.class );

	private CSVFileWrapperFormat csvFormat = null;
	
	public boolean initialize() {

		logger.warn("cvsfile processor wrapper initialize started...");

		if (! super.initialize()) return false; 

		try {
			parameters = new CSVFileWrapperParameters () ;
			logger.debug("Getting parameters from config file.");
			parameters.initParameters(getActiveAddressBean());
			logger.debug("done");
			csvFormat = new CSVFileWrapperFormat () ;
			logger.debug("Parsing Format file.");
			csvFormat.parseFormatFile(parameters);
			logger.debug("done");
		}
		catch (RuntimeException e) {
			logger.error(e.getMessage());
			return false;
		} catch (IOException e) {
			logger.error(e.getMessage());
			return false;
		}

		logger.warn("cvsfile processor wrapper initialize completed...");

		return true;
	}

	@Override
	public DataField[] getOutputFormat() {
		return csvFormat.getFields();
	}

	@Override
	public boolean messageToBeProcessed(DataMsg dataMessage) {

		Serializable[] serialized = new Serializable[csvFormat.getFields().length];

		String msg = (String) dataMessage.getData()[0];

        // add support for empty fields, e.g. ,value,,,value => null, value, null, null, value
        msg = empty2null(msg, Character.toString(parameters.getCsvSeparator()), Character.toString(parameters.getCsvQuoteChar()));

		CSVReader csvReader = new CSVReader (new StringReader(msg), parameters.getCsvSeparator(), parameters.getCsvQuoteChar()) ;

        logger.debug("Message to be processed: " + msg );

		String[] nextLine = null;
		try {
			nextLine = csvReader.readNext();

			if (nextLine.length == csvFormat.getFields().length) {

				Date date = null;
				long time = 0;
				boolean timefound = false;
				
				StringBuilder timeVal = new StringBuilder();
				StringBuilder timeFor = new StringBuilder();
				
				for (int j = 0 ; j < nextLine.length ; j++) {

					logger.debug("Next item to parse: " + nextLine[j] + " dataType: " + csvFormat.getFields()[j].getDataTypeID());

					String tmp = null;
					if(csvFormat.getFields()[j].getDataTypeID() == DataTypes.BIGINT){
						try {

							logger.debug("Timestamp field found. Associated Date Format is >" + csvFormat.getDateFormat(j).toPattern() + "<");

							timefound = true;
							tmp = nextLine[j].replaceAll("^\"|\"$", ""); // Remove the " chars

							int patternLength = csvFormat.getDateFormat(j).toPattern().length();
							if (tmp.length() < patternLength) {
								// Padd the field with 0's
								StringBuilder sb = new StringBuilder (tmp) ;
								for (int i = tmp.length() ; i < patternLength ; i++)
									sb.insert(0, "0");
								tmp = sb.toString();
								logger.debug("Next line padded with 0's: ->" + tmp);
							}
							
							timeVal.append("-").append(tmp);
							timeFor.append("-").append(csvFormat.getDateFormat(j).toPattern());
							
							date = csvFormat.getDateFormat(j).parse(tmp);
							time += date.getTime();
							serialized[j] = date.getTime();
						} catch (ParseException e) {
							logger.error("invalide date format! (Pattern: " + csvFormat.getDateFormat(j).toLocalizedPattern() + ") "+nextLine[j] + " , file: "+ parameters.getCsvSourceFilePath());
							serialized[j] = new Date (0).getTime();
						}
						logger.debug("time: "+tmp);
					}
					if(csvFormat.getFields()[j].getDataTypeID() == DataTypes.DOUBLE){
						try{

                            nextLine[j] = filterNAN(nextLine[j]);

                            Double d = nextLine[j] == null ? null : Double.valueOf(nextLine[j]);
							if (d==null) {
                                // no ! this is not an error, actually null is put on purpose
								//logger.error("invalid double format for "+nextLine[j]+" at timestamp "+time);
								serialized[j] = null;
							} else serialized[j] = d.doubleValue();
						}catch(NumberFormatException e){
							logger.error("wrong double format for :"+nextLine[j]+" at timestamp "+time+ " , file: "+ parameters.getCsvSourceFilePath());
							logger.error(e);
							serialized[j] = null;
						}
						logger.debug("double: "+nextLine[j]);
					}
					if(csvFormat.getFields()[j].getDataTypeID() == DataTypes.INTEGER){
						try{

							nextLine[j] = filterNAN(nextLine[j]);

							Integer d = nextLine[j] == null ? null : Integer.valueOf(nextLine[j]);
							if (d==null) { 
								// no ! this is not an error, actually null is put on purpose
                                //logger.error("invalid integer format for "+nextLine[j]+" at timestamp "+time);
								serialized[j] = null;
							} else serialized[j] = d.intValue();
						}catch(NumberFormatException e){
							logger.error("wrong integer format for :"+nextLine[j]+" at timestamp "+time + " , file: "+ parameters.getCsvSourceFilePath());
							logger.error(e);
							serialized[j] = null;
						}
						logger.debug("integer: "+nextLine[j]);
					}
					if (csvFormat.getFields()[j].getDataTypeID() == DataTypes.VARCHAR) {
						serialized[j] = nextLine[j].replaceAll("^\"|\"$", "");
						logger.debug("string: "+nextLine[j]);
					}
					if (csvFormat.getFields()[j].getDataTypeID() == DataTypes.BINARY) {
						serialized[j] = nextLine[j].replaceAll("^\"|\"$", "").getBytes();
						logger.debug("blob:" + nextLine[j]);
					}
				}

				if (logger.isDebugEnabled()) {
					for (int i = 0 ; i < serialized.length ; i++) {
						logger.debug("Next Serialized Item To send: " + serialized[i]);
					}
				}

				// Adjust time
				if (! timefound) {
					time = System.currentTimeMillis(); 
				}
				else {
					SimpleDateFormat s = new SimpleDateFormat (timeFor.toString().substring(1)) ;
					time = s.parse(timeVal.substring(1)).getTime();
				}

				postStreamElement(time, serialized);

				logger.debug("Data Message Posted with Timed: " + time);
			}
			else {
				logger.error("The length of the line (" + nextLine.length + ") doesn't match the structure length (" + csvFormat.getFields().length + ")");
                logger.error("Line: >> "+msg + "<<");
                logger.error("File: " + parameters.getCsvSourceFilePath());
			}
			csvReader.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
		} catch (ParseException e) {
			logger.error(e.getMessage());
		}
		return true;
	}

	private String filterNAN (String value) {
		if (parameters.getCsvNotANumber().contains(value.toUpperCase())) value = null;
		return value;
	}

    /*
    * Transforms empty fields into null values
    * e.g. ,value,,,value => "null", value, "null", "null", value
    * */
    public static String empty2null( String line, String separator, String quote) {

        String newLine = line;
        String NULL_STR = quote + "NULL" + quote;

        newLine = newLine.replaceAll( "^" + separator, NULL_STR + separator );   // begin of line

        newLine = newLine.replaceAll( separator + "$", separator + NULL_STR );  // end of line

        while (newLine.contains( separator + separator ))  {
            newLine = newLine.replaceAll( separator + separator, separator + NULL_STR + separator ); // middle
        }

        return newLine;
    }

    /*
    * Transforms empty fields into null values
    * e.g. ,value,,,value => "null", value, "null", "null", value
    * */
    public static String empty2null( String line) {
        return empty2null(line, ",","\"");
    }
}
