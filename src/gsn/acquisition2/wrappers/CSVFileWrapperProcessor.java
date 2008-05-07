package gsn.acquisition2.wrappers;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;
import org.apache.log4j.Logger;
import gsn.acquisition2.messages.DataMsg;
import gsn.beans.DataField;
import gsn.beans.DataTypes;

public class CSVFileWrapperProcessor extends SafeStorageAbstractWrapper {

	private DataField[] structure;

	private final transient Logger logger = Logger.getLogger( CSVFileWrapperProcessor.class );

	private static final String CSV_SEPARATOR = "csv-separator";
	private static final String CSV_SEPARATOR_DEFAULT = ",";
	private Character csvSeparator;

	public boolean initialize() {
		super.initialize();
		structure = CSVFileWrapperFormat.parseFormatFile(getActiveAddressBean());
		if (structure == null) return false; 
		csvSeparator = getActiveAddressBean().getPredicateValueWithDefault(CSV_SEPARATOR, CSV_SEPARATOR_DEFAULT).charAt(0);
		return true;
	}

	@Override
	public DataField[] getOutputFormat() {
		return structure;
	}

	@Override
	public boolean messageToBeProcessed(DataMsg dataMessage) {

		Serializable[] serialized = new Serializable[structure.length];

		String[] nextLine = ((String) dataMessage.getData()[0]).trim().split(csvSeparator.toString());

		if (nextLine.length == structure.length) {

			Date date = null;
			long time = 0;
			for (int j = 0 ; j < nextLine.length ; j++) {

				logger.debug("Next line to parse: " + nextLine[j] + " dataType: " + structure[j].getDataTypeID());

				String tmp = null;
				if(structure[j].getDataTypeID() == DataTypes.BIGINT){
					try {
						tmp = nextLine[j].substring(1, nextLine[j].length() - 1); // Remove the " chars
						date = CSVFileWrapperFormat.getDateFormat().parse(tmp);
						time = date.getTime();
						serialized[j] = time;
					} catch (ParseException e) {
						logger.error("invalide date format! "+nextLine[j]);
						serialized[j] = null;
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
					serialized[j] = nextLine[j].substring(1, nextLine[j].length() - 1);
					logger.debug("string: "+nextLine[j]);
				}
			}

			if (logger.isDebugEnabled()) {
				for (int i = 0 ; i < serialized.length ; i++) {
					logger.debug("Next Serialized Item To send: " + serialized[i]);
				}
			}

			postStreamElement(time, serialized);

			logger.debug("Data Message Posted");
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
