package gsn.acquisition2.wrappers;

import gsn.beans.AddressBean;
import gsn.beans.DataField;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import au.com.bytecode.opencsv.CSVReader;

public class CSVFileWrapperFormat {

	private static final transient Logger logger = Logger.getLogger ( CSVFileWrapperFormat.class );

	public static final String CSV_SOURCE_FILE_PATH = "csv-source-file-path";

	public static final String CSV_FORMAT_FILE_PATH ="csv-format-file-path";

	private static DataField[] fields = null;

	private static final Pattern removeParenthesisPattern = Pattern.compile("[()]") ;

	private static final Pattern toTimePattern = Pattern.compile("^(TS:)[\\p{Print}]*") ;

	private static final Pattern floatToDoublePattern = Pattern.compile("(float)") ;

	private static final String CSV_FORMAT_LINE_NAMES = "csv-format-line-name";
	private static final String CSV_FORMAT_LINE_NAMES_DEFAULT = "2";
	private static int csvFormatLineNames;

	private static final String CSV_FORMAT_LINE_DESCRIPTIONS = "csv-format-line-descriptions";
	private static final int[] CSV_FORMAT_LINE_DESCRIPTIONS_DEFAULT = new int[]{3,4};
	private static int[] csvFormatLineDescriptions;

	private static final String CSV_FORMAT_LINE_FORMAT = "csv-format-line-format";
	private static final String CSV_FORMAT_LINE_FORMAT_DEFAULT = "5";
	private static int csvFormatLineFormat;
	
	private static SimpleDateFormat dateFormat = null;

	public static DataField[] parseFormatFile (AddressBean infos){

		String filepath = infos.getPredicateValue(CSV_FORMAT_FILE_PATH);

		// Get the parameter values or set default values
		csvFormatLineNames = Integer.parseInt(infos.getPredicateValueWithDefault(CSV_FORMAT_LINE_NAMES, CSV_FORMAT_LINE_NAMES_DEFAULT));
		String[] tmpLineDescriptions = infos.getPredicateValue(CSV_FORMAT_LINE_DESCRIPTIONS).split(",");
		if (tmpLineDescriptions != null){
			csvFormatLineDescriptions = new int[tmpLineDescriptions.length];
			for (int i = 0 ; i < csvFormatLineDescriptions.length ; i++) {
				csvFormatLineDescriptions[i] = Integer.parseInt(tmpLineDescriptions[i]);
			}	
		}
		else csvFormatLineDescriptions = CSV_FORMAT_LINE_DESCRIPTIONS_DEFAULT;
		csvFormatLineFormat = Integer.parseInt(infos.getPredicateValueWithDefault(CSV_FORMAT_LINE_FORMAT, CSV_FORMAT_LINE_FORMAT_DEFAULT));

		ArrayList<String> field_names = new ArrayList<String> () ;
		ArrayList<String> field_description = new ArrayList<String> () ;
		ArrayList<String> field_formats = new ArrayList<String> () ;

		try {
			String[] nextLine;
			CSVReader reader = new CSVReader(new FileReader(filepath),',','"',0);
			int i = 0;
			while ((nextLine = reader.readNext()) != null){
				i++;
				if (i == csvFormatLineNames){
					for (int j = 0 ; j < nextLine.length ; j++) {
						field_names.add(nextLine[j].replaceAll(removeParenthesisPattern.pattern(), ""));
					}
				}
				else if (contains(csvFormatLineDescriptions, i)){
					for (int j = 0 ; j < nextLine.length ; j++) {
						if (field_description.size() > j) field_description.set(
								j, 
								field_description.get(j) + " " + nextLine[j]);
						else field_description.add(nextLine[j]);
					}
				}
				else if (i == csvFormatLineFormat){
					String tmp;
					for (int j = 0 ; j < nextLine.length ; j++) {
						tmp = new String () ;
						if (nextLine[j].startsWith("TS:")) dateFormat = new SimpleDateFormat (nextLine[j].substring(3)) ;
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
		} catch (FileNotFoundException e) {
			logger.warn(e.getMessage(), e);
			return null;
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
			return null;
		} 
		if (logger.isDebugEnabled()){
			for (int i = 0 ; i < fields.length ; i++) {
				logger.debug(fields[i]);
			}
		}
		return fields;
	}
	
	public static SimpleDateFormat getDateFormat () {
		return dateFormat;
	}

	private static boolean contains (int[] ar, int value) {
		for (int i = 0 ; i < ar.length ; i++) {
			if (ar[i] == value) return true;
		}
		return false;
	}
}
