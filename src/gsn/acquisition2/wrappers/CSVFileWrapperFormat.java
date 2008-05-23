package gsn.acquisition2.wrappers;

import gsn.beans.DataField;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import au.com.bytecode.opencsv.CSVReader;

public class CSVFileWrapperFormat {

	private static final transient Logger logger = Logger.getLogger ( CSVFileWrapperFormat.class );

	private DataField[] fields = null;

	private static final Pattern closeParenthesisPattern = Pattern.compile("[)]") ;

	private static final Pattern openParenthesisPattern = Pattern.compile("[(]") ;

	private static final Pattern toTimePattern = Pattern.compile("^(TS:)[\\p{Print}]*") ;

	private static final Pattern floatToDoublePattern = Pattern.compile("(float)") ;

	private SimpleDateFormat dateFormat[] = null;

	public DataField[] parseFormatFile (CSVFileWrapperParameters parameters) throws IOException {

		logger.debug("Parsing format file >" + parameters.getCsvFormatFilePath() + "<");
		
		ArrayList<String> field_names = new ArrayList<String> () ;
		ArrayList<String> field_description = new ArrayList<String> () ;
		ArrayList<String> field_formats = new ArrayList<String> () ;

		String[] nextLine;
		CSVReader reader = new CSVReader(new FileReader(parameters.getCsvFormatFilePath()),',','"',0);
		int i = 0;
		while ((nextLine = reader.readNext()) != null){
			i++;
			if (i == parameters.getCsvFormatLineNames()){
				for (int j = 0 ; j < nextLine.length ; j++) {
					field_names.add(nextLine[j].replaceAll(closeParenthesisPattern.pattern(), "").replaceAll(openParenthesisPattern.pattern(), "_"));
				}
			}
			else if (contains(parameters.getCsvFormatLineDescriptions(), i)){
				for (int j = 0 ; j < nextLine.length ; j++) {
					if (field_description.size() > j) field_description.set(
							j, 
							field_description.get(j) + " " + nextLine[j]);
					else field_description.add(nextLine[j]);
				}
			}
			else if (i == parameters.getCsvFormatLineFormat()){
				String tmp;
				dateFormat = new SimpleDateFormat[nextLine.length] ;
				for (int j = 0 ; j < nextLine.length ; j++) {
					tmp = new String () ;
					if (nextLine[j].startsWith("TS:")) dateFormat[j] = new SimpleDateFormat (nextLine[j].substring(3)) ;
					tmp = nextLine[j].replaceAll(toTimePattern.pattern(), "BIGINT");
					tmp = tmp.replaceAll(floatToDoublePattern.pattern(), "DOUBLE");
					field_formats.add(tmp);
				}
			}
		}

		reader.close();

		int nb_fields = Math.min(field_names.size(), field_description.size());
		nb_fields = Math.min(nb_fields, field_formats.size());

		fields = new DataField[nb_fields];

		for (int j = 0 ; j < nb_fields ; j++) {
			fields[j] = new DataField(
					field_names.get(j), 
					field_formats.get(j),
					(field_description.get(j)));
		}
		if (logger.isDebugEnabled()){
			logger.debug("Format: ");
			for (int k = 0 ; k < fields.length ; k++) {
				logger.debug("Field " + k + " " + fields[k]);
			}
		}
		return fields;
	}

	public SimpleDateFormat getDateFormat (int column) {
		return dateFormat[column];
	}
	
	public DataField[] getFields () {
		return fields;
	}

	private static boolean contains (int[] ar, int value) {
		for (int i = 0 ; i < ar.length ; i++) {
			if (ar[i] == value) return true;
		}
		return false;
	}
}
