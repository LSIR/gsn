package gsn.tests;

import static org.junit.Assert.*;
import gsn.acquisition2.wrappers.CSVFileWrapperProcessor;
import org.junit.BeforeClass;
import org.junit.Test;

/*
* Unit test for
* */
public class TestCSVFileWrapperProcessor {
    @BeforeClass
	public static void init(){

	}

	@Test
	public void testEmpty2Null() {

        String QUOTE = "\"";
        String SEPARATOR = ",";
        String NULL_STR = QUOTE+"NULL"+QUOTE;

        // CSV lines to be tested
        String[] csvLine = {
             ",1,2,3",
             "1,2,3,",
             "1,,2,3",
             ",,1,2,3",
             "1,2,3,,",
             "1,,,,2,3"
        };

        // Expected CSV lines after replacement with "NULL"
        String[] _csvLine = {
             NULL_STR + ",1,2,3",
             "1,2,3" + SEPARATOR + NULL_STR,
             "1" + SEPARATOR + NULL_STR + SEPARATOR + "2,3",
             NULL_STR + SEPARATOR + NULL_STR + SEPARATOR + "1,2,3",
             "1,2,3" + SEPARATOR + NULL_STR + SEPARATOR + NULL_STR,
             "1" + SEPARATOR + NULL_STR + SEPARATOR + NULL_STR + SEPARATOR + NULL_STR + SEPARATOR + "2,3"
        };

        for (int i=0;i<csvLine.length;i++) {
            //System.out.println( csvLine[i] + " => " + _csvLine[i] +" <==> " + CSVFileWrapperProcessor.empty2null(csvLine[i], SEPARATOR, QUOTE));
		    assertEquals(_csvLine[i], CSVFileWrapperProcessor.empty2null(csvLine[i], SEPARATOR, QUOTE));
        }

        // Testing using a different separator
        
        SEPARATOR = ";";

        for (int i=0;i<csvLine.length;i++) {
            csvLine[i] = csvLine[i].replace(",",SEPARATOR);
            _csvLine[i] = _csvLine[i].replace(",",SEPARATOR);

            //System.out.println( csvLine[i] + " => " + _csvLine[i] +" <==> " + CSVFileWrapperProcessor.empty2null(csvLine[i], SEPARATOR, QUOTE));
		    assertEquals(_csvLine[i], CSVFileWrapperProcessor.empty2null(csvLine[i], SEPARATOR, QUOTE));
        }
 
	}

}
