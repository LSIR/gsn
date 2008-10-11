package gsn.acquisition2.wrappers;

import java.util.ArrayList;

import gsn.beans.AddressBean;

public class CSVFileWrapperParameters {

	// Optional parameters

	private static final String CSV_SEPARATOR = "csv-separator";
	private static final String CSV_SEPARATOR_DEFAULT = ",";
	private Character csvSeparator;

	private static final String CSV_QUOTE_CHAR = "csv-quote";
	private static final String CSV_QUOTE_CHAR_DEFAULT = "\"";
	private Character csvQuoteChar;

	private static final String CSV_SKIP_LINES = "csv-skip-lines";
	private static final String CSV_SKIP_LINES_DEFAULT = "0";
	private int csvSkipLines;

	private static final String CSV_UPDATE_DELAY = "csv-update-delay";
	private static final String CSV_UPDATE_DELAY_DEFAULT = "160000";
	private long csvupdateDelay;

	private static final String CSV_FORMAT_LINE_NAMES = "csv-format-line-name";
	private static final String CSV_FORMAT_LINE_NAMES_DEFAULT = "2";
	private int csvFormatLineNames;

	private static final String CSV_FORMAT_LINE_DESCRIPTIONS = "csv-format-line-descriptions";
	private static final int[] CSV_FORMAT_LINE_DESCRIPTIONS_DEFAULT = new int[]{3,4};
	private int[] csvFormatLineDescriptions;

	private static final String CSV_FORMAT_LINE_FORMAT = "csv-format-line-format";
	private static final String CSV_FORMAT_LINE_FORMAT_DEFAULT = "5";
	private int csvFormatLineFormat;

	private static final String CSV_NOT_A_NUMBER = "csv-not-a-number";
	private static final String CSV_NOT_A_NUMBER_DEFAULT = "NaN,null";
	private ArrayList<String> csvNotANumber;
	
	// Mandatory parameters

	public static final String CSV_SOURCE_FILE_PATH = "csv-source-file-path";
	private String csvSourceFilePath = null;

	public static final String CSV_FORMAT_FILE_PATH ="csv-format-file-path";
	private String csvFormatFilePath = null;

	public CSVFileWrapperParameters () {}

	public void initParameters (AddressBean infos) {

		// Optional parameters

		csvSeparator = infos.getPredicateValueWithDefault(CSV_SEPARATOR, CSV_SEPARATOR_DEFAULT).charAt (0) ;

		csvQuoteChar = infos.getPredicateValueWithDefault(CSV_QUOTE_CHAR, CSV_QUOTE_CHAR_DEFAULT).charAt (0) ;

		csvSkipLines = Integer.parseInt(infos.getPredicateValueWithDefault(CSV_SKIP_LINES, CSV_SKIP_LINES_DEFAULT));

		csvupdateDelay = Long.parseLong(infos.getPredicateValueWithDefault(CSV_UPDATE_DELAY, CSV_UPDATE_DELAY_DEFAULT));

		csvFormatLineNames = Integer.parseInt(infos.getPredicateValueWithDefault(CSV_FORMAT_LINE_NAMES, CSV_FORMAT_LINE_NAMES_DEFAULT));

		String nanParams = infos.getPredicateValueWithDefault(CSV_NOT_A_NUMBER, CSV_NOT_A_NUMBER_DEFAULT);
		String[] splittedNanParams = nanParams.split(",");
		csvNotANumber = new ArrayList<String> () ;
		for (int i = 0 ; i < splittedNanParams.length ; i++) {
			csvNotANumber.add(splittedNanParams[i].trim().toUpperCase());
		}

		String[] tmpLineDescriptions = infos.getPredicateValue(CSV_FORMAT_LINE_DESCRIPTIONS).split(",");
		if (tmpLineDescriptions != null){
			csvFormatLineDescriptions = new int[tmpLineDescriptions.length];
			for (int i = 0 ; i < csvFormatLineDescriptions.length ; i++) {
				csvFormatLineDescriptions[i] = Integer.parseInt(tmpLineDescriptions[i]);
			}	
		}
		else csvFormatLineDescriptions = CSV_FORMAT_LINE_DESCRIPTIONS_DEFAULT;

		csvFormatLineFormat = Integer.parseInt(infos.getPredicateValueWithDefault(CSV_FORMAT_LINE_FORMAT, CSV_FORMAT_LINE_FORMAT_DEFAULT));

		// Mandatory parameters (may thow RuntimeException)

		csvSourceFilePath = infos.getPredicateValueWithException(CSV_SOURCE_FILE_PATH);

		csvFormatFilePath = infos.getPredicateValueWithException(CSV_FORMAT_FILE_PATH);
	}

	// Getters

	public Character getCsvSeparator() {
		return csvSeparator;
	}
	public Character getCsvQuoteChar() {
		return csvQuoteChar;
	}
	public int getCsvSkipLines() {
		return csvSkipLines;
	}
	public long getCsvUpdateDelay() {
		return csvupdateDelay;
	}
	public int getCsvFormatLineNames() {
		return csvFormatLineNames;
	}
	public int[] getCsvFormatLineDescriptions() {
		return csvFormatLineDescriptions;
	}
	public int getCsvFormatLineFormat() {
		return csvFormatLineFormat;
	}
	public String getCsvSourceFilePath() {
		return csvSourceFilePath;
	}
	public String getCsvFormatFilePath() {
		return csvFormatFilePath;
	}
	public ArrayList<String> getCsvNotANumber() {
		return csvNotANumber;
	}
}
