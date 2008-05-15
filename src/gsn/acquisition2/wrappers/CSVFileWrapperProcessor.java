package gsn.acquisition2.wrappers;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.text.ParseException;
import java.util.Date;
import org.apache.log4j.Logger;

import au.com.bytecode.opencsv.CSVReader;
import gsn.acquisition2.messages.DataMsg;
import gsn.beans.DataField;
import gsn.beans.DataTypes;

public class CSVFileWrapperProcessor extends SafeStorageAbstractWrapper {

	//private static final Pattern toTimePattern = Pattern.compile("^(TS:)[\\p{Print}]*") ;

	private DataField[] structure;

	private final transient Logger logger = Logger.getLogger( CSVFileWrapperProcessor.class );

	private static final String CSV_SEPARATOR = "csv-separator";
	private static final String CSV_SEPARATOR_DEFAULT = ",";
	private Character csvSeparator;

	private static final String CSV_QUOTE_CHAR = "csv-quote";
	private static final String CSV_QUOTE_CHAR_DEFAULT = "\"";
	private Character csvQuoteChar;

	public boolean initialize() {
		super.initialize();
		structure = CSVFileWrapperFormat.parseFormatFile(getActiveAddressBean());
		if (structure == null) return false; 
		csvSeparator = getActiveAddressBean().getPredicateValueWithDefault(CSV_SEPARATOR, CSV_SEPARATOR_DEFAULT).charAt(0);
		csvQuoteChar = getActiveAddressBean().getPredicateValueWithDefault(CSV_QUOTE_CHAR, CSV_QUOTE_CHAR_DEFAULT).charAt(0);
		return true;
	}

	@Override
	public DataField[] getOutputFormat() {
		return structure;
	}

	@Override
	public boolean messageToBeProcessed(DataMsg dataMessage) {

		Serializable[] serialized = new Serializable[structure.length];

		String msg = (String) dataMessage.getData()[0];
		CSVReader csvReader = new CSVReader (new StringReader(msg), csvSeparator, csvQuoteChar) ;

		logger.debug("Next Line to parse: " + msg);

		String[] nextLine = null;
		try {
			nextLine = csvReader.readNext();

			if (nextLine.length == structure.length) {

				Date date = null;
				long time = 0;
				boolean timefound = false;
				for (int j = 0 ; j < nextLine.length ; j++) {

					logger.debug("Next line to parse: " + nextLine[j] + " dataType: " + structure[j].getDataTypeID());

					String tmp = null;
					if(structure[j].getDataTypeID() == DataTypes.BIGINT){
						try {

							logger.debug("Timestamp field found. Associated Date Format is >" + CSVFileWrapperFormat.getDateFormat(j).toPattern() + "<");

							timefound = true;
							tmp = nextLine[j].replaceAll("^\"|\"$", ""); // Remove the " chars

							int patternLength = CSVFileWrapperFormat.getDateFormat(j).toPattern().length();
							if (tmp.length() < patternLength) {
								// Padd the field with 0's
								StringBuilder sb = new StringBuilder (tmp) ;
								for (int i = tmp.length() ; i < patternLength ; i++)
									sb.insert(0, "0");
								tmp = sb.toString();
								logger.debug("Next line padded with 0's: ->" + tmp);
							}
							date = CSVFileWrapperFormat.getDateFormat(j).parse(tmp);
							time += date.getTime();
							serialized[j] = date.getTime();
						} catch (ParseException e) {
							logger.error("invalide date format! (Pattern: " + CSVFileWrapperFormat.getDateFormat(j).toLocalizedPattern() + ") "+nextLine[j]);
							serialized[j] = new Date (0).getTime();
						}
						logger.debug("time: "+tmp);
					}
					if(structure[j].getDataTypeID() == DataTypes.DOUBLE){
						try{

							nextLine[j] = filterNAN(nextLine[j]);

							Double d = Double.valueOf(nextLine[j]);
							if (d==null) { 
								logger.error("invalide double format for "+nextLine[j]+" at timestamp "+time);
								serialized[j] = null;
							} else serialized[j] = d.doubleValue();
						}catch(NumberFormatException e){
							logger.error("wrong double format for :"+nextLine[j]+" at timestamp "+time);
							logger.error(e);
							serialized[j] = null;
						}
						logger.debug("double: "+nextLine[j]);
					}
					if(structure[j].getDataTypeID() == DataTypes.INTEGER){
						try{

							nextLine[j] = filterNAN(nextLine[j]);

							Integer d = Integer.valueOf(nextLine[j]);
							if (d==null) { 
								logger.error("invalide integer format for "+nextLine[j]+" at timestamp "+time);
								serialized[j] = null;
							} else serialized[j] = d.intValue();
						}catch(NumberFormatException e){
							logger.error("wrong integer format for :"+nextLine[j]+" at timestamp "+time);
							logger.error(e);
							serialized[j] = null;
						}
						logger.debug("integer: "+nextLine[j]);
					}
					if (structure[j].getDataTypeID() == DataTypes.VARCHAR) {
						serialized[j] = nextLine[j].replaceAll("^\"|\"$", "");
						logger.debug("string: "+nextLine[j]);
					}
					if (structure[j].getDataTypeID() == DataTypes.BINARY) {
						serialized[j] = nextLine[j].replaceAll("^\"|\"$", "").getBytes();
						logger.debug("blob:" + nextLine[j]);
					}
				}

				if (logger.isDebugEnabled()) {
					for (int i = 0 ; i < serialized.length ; i++) {
						logger.debug("Next Serialized Item To send: " + serialized[i]);
					}
				}

				if (! timefound) {
					time = System.currentTimeMillis(); 
				}

				postStreamElement(time, serialized);

				logger.debug("Data Message Posted with Timed: " + time);
			}
			else {
				logger.warn("The length of the line (" + nextLine.length + ") doesn't match the structure length (" + structure.length + ")");
			}
			csvReader.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
		return true;
	}

	private String filterNAN (String value) {
		if (value.compareToIgnoreCase("NaN") == 0)  value = "NaN";
		return value;
	}

	@Override
	public boolean isTimeStampUnique() {
		return false;
	}
}
