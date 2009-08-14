package gsn.tests;



import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.File;
import java.util.HashMap;
import java.util.ArrayList;

import static org.easymock.classextension.EasyMock.*;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;
import static org.testng.Assert.*;
import org.testng.annotations.*;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import static org.easymock.EasyMock.createMock;

import gsn2.wrappers.CSVHandler;
import gsn2.wrappers.CSVWrapper;
import gsn2.wrappers.WrapperConfig;
import gsn2.conf.Parameters;
import gsn.beans.StreamElement;
import gsn.utils.Param;
import gsn.channels.DataChannel;

@Test(sequential = true)
public class TestCSVWrapper {
    private final String TEST_DIR = System.getProperty("java.io.tmpdir")+"/csv-check-points.csv";
    private final String CSV_DATA_FILE =  TEST_DIR +"/test.csv.csv";
    private final String CSV_CHK_POINT_FILE =  TEST_DIR +"/test.csv.csv.check.point";

    @BeforeMethod
    public void setUp() throws Exception {
        new File(TEST_DIR).mkdirs();
        new File(CSV_DATA_FILE).createNewFile();
    }
    @AfterMethod
    public void tearDown() throws Exception {
        FileUtils.deleteQuietly(new File(TEST_DIR));
    }

    @Test
    public void testIsNull() {
        assertEquals(true, CSVHandler.isDeclaredAsNull(new String[] {"NaN","4343"}, null));
        assertEquals(true,CSVHandler.isDeclaredAsNull(new String[] {"NaN","4343"}, "nan"));
        assertEquals(true,CSVHandler.isDeclaredAsNull(new String[] {"NaN","4343"}, "4343"));
        assertFalse(CSVHandler.isDeclaredAsNull(new String[] {"NaN","4343"}, "43434"));
    }
    @Test
    public void testTimeStamp() {
        assertFalse(CSVHandler.isTimeStampFormat("timed"));
        assertFalse(CSVHandler.isTimeStampFormat("timestamp"));
        assertEquals(true,CSVHandler.isTimeStampFormat("timestamp(xyz)"));
        assertEquals("xyz", CSVHandler.getTimeStampFormat("timestamp(xyz)"));
    }

    @Test
    public void testBadFields() {
        CSVHandler.validateFormats(new String[] {"numeric"});
        CSVHandler.validateFormats(new String[]  {"Timestamp(d.M.y k:m)"});
        CSVHandler.validateFormats(new String[] {"Timestamp(d.M.y k:m)" ,"Numeric", "numeric" , "numeric"});
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testBadFields2() {
        CSVHandler.validateFormats(new String[] {"Timestamp(d.Mjo0o.y k:m)"});
    }
    @Test(expectedExceptions = RuntimeException.class)
    public void testBadFields3() {
        CSVHandler.validateFormats(new String[] {"doubble"});
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testBadFormat1() {
        String badFormat = "Timestamp(d.M.y k:m) , numeric , numeric, numeric,numeric,dollluble ";
        String fields = "TIMED, air_temp , TIMED , AiR_TeMp2";
        new CSVHandler(CSV_DATA_FILE, fields,badFormat,',','\"',0,"NaN,-1234,4321","Etc/GMT-1", CSV_CHK_POINT_FILE,"timed");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testBadFormat3() {
        String fields = "TIMED, air_temp , TIMED , AiR_TeMp2";
        String formats = "Timestamp(d.M.y ) , Numeric , timestamp(k:m) , numeric,numeric    ";
        new CSVHandler(CSV_DATA_FILE, fields,formats,',','\"',0,"NaN,-1234,4321","Etc/GMT-1", CSV_CHK_POINT_FILE,"timed");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testNonExistingDataFile() {
        String badFormat = "Timestamp(d.M.y k:m) , numeric , numeric, numeric,numeric,dollluble ";
        String fields = "TIMED, air_temp , TIMED , AiR_TeMp2";
        new CSVHandler(CSV_DATA_FILE+"blabla", fields,badFormat,',','\"',0,"NaN,-1234,4321","Etc/GMT-1", CSV_CHK_POINT_FILE,"timed");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testBadDataFile() {
        String badFormat = "Timestamp(d.M.y k:m) , numeric , numeric, numeric,numeric,dollluble ";
        String fields = "TIMED, air_temp , TIMED , AiR_TeMp2";
        new CSVHandler(System.getProperty("java.io.tmpdir"), fields,badFormat,',','\"',0,"NaN,-1234,4321","Etc/GMT-1", CSV_CHK_POINT_FILE,"timed");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testBadChkPontFile() {
        String badFormat = "Timestamp(d.M.y k:m) , numeric , numeric, numeric,numeric,dollluble ";
        String fields = "TIMED, air_temp , TIMED , AiR_TeMp2";
        new CSVHandler(CSV_DATA_FILE, fields,badFormat,',','\"',0,"NaN,-1234,4321","Etc/GMT-1", System.getProperty("java.io.tmpdir"),"timed");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testBadFormat2() {
        String badFormat2 ="Timestamp(d.Mjo0o.y k:m) , numeric, numeric, numeric";
        String fields = "TIMED, air_temp , TIMED , AiR_TeMp2";
        new CSVHandler(CSV_DATA_FILE, fields,badFormat2,',','\"',0,"NaN,-1234,4321","Etc/GMT-1", CSV_CHK_POINT_FILE,"timed");
    }

    @Test
    public void testNoTimestampField() {
        String badFormat2 ="numeric, numeric, numeric";
        String fields = "air_temp , air_temp3 , AiR_TeMp2";
        new CSVHandler(CSV_DATA_FILE, fields,badFormat2,',','\"',0,"NaN,-1234,4321","Etc/GMT-1", CSV_CHK_POINT_FILE,"timed");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testDoublicatedFileNames() {
        String badFormat2 ="numeric, numeric, numeric";
        String fields = "air_temp2 , air_temp3 , AiR_TeMp2";
        new CSVHandler(CSV_DATA_FILE, fields,badFormat2,',','\"',0,"NaN,-1234,4321","Etc/GMT-1", CSV_CHK_POINT_FILE,"timed");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testNoFields() {
        String badFormat2 ="";
        String fields = "";
        new CSVHandler(CSV_DATA_FILE, fields,badFormat2,',','\"',0,"NaN,-1234,4321","Etc/GMT-1", CSV_CHK_POINT_FILE,"timed");
    }


    @Test
    public void testFieldConverting() throws IOException {
        String fields = "TIMED, air_temp , TIMED , AiR_TeMp2";
        String formats = "Timestamp(d.M.y ) , Numeric , timestamp(k:m) , numeric    ";

        CSVHandler wrapper = new CSVHandler(CSV_DATA_FILE, fields,formats,',','\"',0,"NaN,-1234,4321","Etc/GMT-1", CSV_CHK_POINT_FILE,"timed");

        FileUtils.writeStringToFile(new File(wrapper.getCheckPointFile()),  "","UTF-8");
        String[] formatsParsed = wrapper.getFormats();
        String[] fieldsParsed =  wrapper.getFields();
        assertEquals(true,isEqual(fieldsParsed, new String[] {"timed","air_temp","timed","air_temp2"}));
        assertEquals(true,isEqual(formatsParsed, new String[] {"Timestamp(d.M.y )","Numeric","timestamp(k:m)","numeric"}));

        StreamElement se = wrapper.convertTo(wrapper.getFormats(),wrapper.getFields(),wrapper.getNulls(),new String[] {} , wrapper.getSeparator());
        assertTrue(se.isEmpty());
        se = wrapper.convertTo(wrapper.getFormats(),wrapper.getFields(),wrapper.getNulls(),new String[] {"","","","-1234","4321","NaN"} , wrapper.getSeparator());
        assertTrue(se.isEmpty());

        se = wrapper.convertTo(wrapper.getFormats(),wrapper.getFields(),wrapper.getNulls(),new String[] {"","","","-1234","4321","NaN"} , wrapper.getSeparator());
        assertTrue(se.isEmpty());

        se = wrapper.convertTo(wrapper.getFormats(),wrapper.getFields(),wrapper.getNulls(),new String[] {"01.01.2009","1234","","-4321","ignore-me","NaN"} , wrapper.getSeparator());
        assertTrue(!se.isEmpty());
        assertEquals(1234.0, se.getValue("air_temp"));
        assertEquals(-4321.0, se.getValue("air_temp2"));

        se = wrapper.convertTo(wrapper.getFormats(),wrapper.getFields(),wrapper.getNulls(),new String[] {"01.01.2009","-1234","10:10","-4321","ignore-me","NaN"} , wrapper.getSeparator());
        assertFalse(se.isEmpty());
        assertNull(se.getValue("air_temp"));
    }


    @Test
    public void testTimeStampParsingFails() throws IOException, InterruptedException {
        String fields = "TIMED, air_temp , TIMEd , AiR_TeMp2, comments";
        String formats = "Timestamp(d.M.y ) , Numeric , timestamp(k:m) , numeric ,String   ";
        String data = "01.01.2009,1,10:10,10,\"Ali Salehi\"\n"+
                "01.01.2009,2,10:11,11,\"Ali Salehi\"\n"+
                "01.01.2009,3,10,12,12,\"Ali Salehi\"\n"+ // <------- BAD LINE
                "01.01.2009,2,10:13,11,\"Ali Salehi\"\n"+
                "01.01.2009,2,10:14,11,\"Ali Salehi\"\n"+
                "01.01.2009,2,10:15,11,\"Ali Salehi\"\n";

        WrapperConfig config = new WrapperConfig("gsn2.wrappers.CSVWrapper",new Parameters(
                new Param("file",CSV_DATA_FILE),new Param("fields",fields),new Param("timestamp-field","timed"),
                new Param("formats",formats),new Param("check-point-path",CSV_CHK_POINT_FILE),new Param("bad-values","NaN,-1234,4321"),
                new Param("sampling","1"),new Param("quote","''"),new Param("separator",",,")
        ));

        DataChannel mock = createMock(DataChannel.class);
        mock.write((StreamElement)anyObject());
        expectLastCall().times(2);
        replay(mock);
        CSVWrapper wrapper = new CSVWrapper(config,mock);
        wrapper.start();
        FileUtils.writeStringToFile(new File(CSV_DATA_FILE),data);
        Thread.sleep(250);
        wrapper.stop();
        verify(mock);
    }

    @Test
    public void testBadData() throws IOException, InterruptedException {
        String fields = "TIMED, air_temp , TIMEd , AiR_TeMp2, comments";
        String formats = "Timestamp(d.M.y ) , Numeric , timestamp(k:m) , numeric ,String   ";
        String data = "01.01.2009,1,10:10,10,\"Ali Salehi\"\n"+
                "01.01.2009,2,10:11,11,\"Ali Salehi\"\n"+
                "01.01.2009,3,10:12,\'abc\',\"Ali Salehi\"\n"+ // <------- BAD LINE
                "01.01.2009,2,10:13,11,\"Ali Salehi\"\n"+
                "01.01.2009,2,10:14,11,\"Ali Salehi\"\n"+
                "01.01.2009,2,10:15,11,\"Ali Salehi\"\n";

        WrapperConfig config = new WrapperConfig("gsn2.wrappers.CSVWrapper",new Parameters(
                new Param("file",CSV_DATA_FILE),new Param("fields",fields),new Param("timestamp-field","timed"),
                new Param("formats",formats),new Param("check-point-path",CSV_CHK_POINT_FILE),new Param("bad-values","NaN,-1234,4321"),
                new Param("sampling","1")
        ));

        DataChannel mock = createMock(DataChannel.class);
        mock.write((StreamElement)anyObject());
        expectLastCall().times(2);
        replay(mock);
        CSVWrapper wrapper = new CSVWrapper(config,mock);
        wrapper.start();
        FileUtils.writeStringToFile(new File(CSV_DATA_FILE),data);
        Thread.yield();
        Thread.sleep(500);
        wrapper.stop();
        verify(mock);
    }


    @Test
    public void testDataFileWithNoValueOnlyTimeStamps() throws IOException, InterruptedException {
        String fields = "TIMED,  TIMEd ";
        String formats = "Timestamp(d.M.y )  , timestamp(k:m)   ";
        String data = "01.01.2009,10:10,1\n"+
                "01.01.2009,10:11,1\n";

        WrapperConfig config = new WrapperConfig("gsn2.wrappers.CSVWrapper",new Parameters(
                new Param("file",CSV_DATA_FILE),new Param("fields",fields),new Param("timestamp-field","timed"),
                new Param("formats",formats),new Param("check-point-path",CSV_CHK_POINT_FILE),new Param("bad-values","NaN,-1234,4321"),
                new Param("sampling","1")
        ));

        DataChannel mock = createMock(DataChannel.class);
        mock.write((StreamElement)anyObject());
        expectLastCall().times(2);
        replay(mock);
        CSVWrapper wrapper = new CSVWrapper(config,mock);
        wrapper.start();
        FileUtils.writeStringToFile(new File(CSV_DATA_FILE),data);
        Thread.yield();
        Thread.sleep(500);
        wrapper.stop();
        verify(mock);
    }


    @Test
    public void testCheckpoints() throws IOException {
        String fields = "TIMED, air_temp , TIMEd , AiR_TeMp2, comments";
        String formats = "Timestamp(d.M.y ) , Numeric , timestamp(k:m) , numeric ,String   ";
        String data = "01.01.2009,1,10:10,10,\"Ali Salehi\"\n"+
                "01.01.2009,2,10:11,11,\"Ali Salehi\"\n"+
                "01.01.2009,3,10:12,12,\"Ali Salehi\"\n";

        assertEquals(CSVHandler.getCheckPointFileIfAny(CSV_CHK_POINT_FILE),0);
        CSVHandler handler = new CSVHandler(CSV_DATA_FILE, fields,formats,',','\"',0,"NaN,-1234,4321","Etc/GMT-1", CSV_CHK_POINT_FILE,"timed");
        ArrayList<StreamElement> parsed = handler.process(new StringReader(data), -1);
        assertEquals(3, parsed.size());

        assertTrue(parsed.get(0).getTimeInMillis()<parsed.get(1).getTimeInMillis());
        long recentTimestamp = parsed.get(parsed.size()-1).getTimeInMillis();
        data+="01.01.2009,3,10:12,12,\"Ali Salehi\"\n"; // duplicated timestamp, should be ignored.
        assertEquals(0,handler.process(new StringReader(data), recentTimestamp).size());

        data+="01.01.2009,3,10:12,12,\"Ali Salehi\"\n";
        data+="01.01.2009,3,10:11,12,\"Ali Salehi\"\n";
        data+="01.01.2009,3,10:10,12,\"Ali Salehi\"\n"; //time goes back, should be ignored.
        assertEquals(0,handler.process(new StringReader(data), recentTimestamp).size());
        data+="01.01.2009,3,10:13,13,\"Ali Salehi\"\n";
        assertEquals(1,handler.process(new StringReader(data), recentTimestamp).size());

        data="###########################\n\n\n\n,,,,,,,,,\n\n\n"; // Empty File.
        handler.setSkipFirstXLines(1);
        assertEquals(0,handler.process(new StringReader(data), recentTimestamp).size());
    }

    @Test
    public void testTimeStampParser() throws IOException {
        DateTime toReturn = CSVHandler.parseTimestamp("d.M.y k:m","01.10.2008 06:20");
        assertEquals(new DateTime(2008,10,01,6,20,0,0), toReturn);
        assertEquals(0, CSVHandler.generateFieldIdx("", false).length);
    }

    @Test
    public void testFileUtils() throws IOException {
        assertTrue(CSVHandler.getCheckPointFileIfAny(CSV_CHK_POINT_FILE)==0);
        FileUtils.writeStringToFile(new File(CSV_CHK_POINT_FILE),"","UTF-8");
        assertTrue(CSVHandler.getCheckPointFileIfAny(CSV_CHK_POINT_FILE)==0);
        int time = 1234567600;
        CSVHandler.updateCheckPointFile(CSV_CHK_POINT_FILE,time);
        assertEquals(CSVHandler.getCheckPointFileIfAny(CSV_CHK_POINT_FILE),time);
    }

    @Test
    public void testGenerateFieldIdx() throws IOException {
        assertEquals(CSVHandler.generateFieldIdx("",false),new String[0]);
        assertEquals(CSVHandler.generateFieldIdx(null,false),new String[0]);
        assertTrue(isEqual(CSVHandler.generateFieldIdx(",",false),new String[]{"",""}));
        assertTrue(isEqual(CSVHandler.generateFieldIdx("\"a\",\"b\"",false),new String[]{"a","b"}));

    }

    public boolean isEqual(String[] a,String[] b) {
        if (a.length!=b.length)
            return false;
        for (int i=0;i<a.length;i++)
            if (!a[i].equals(b[i]))
                return false;
        return true;
    }
    public boolean compare(HashMap<String, Serializable> a,HashMap<String, Serializable> b) {
        if (a.size()!=b.size())
            return false;
        for (String key : a.keySet())
            if(!a.get(key).equals(b.get(key)))
                return false;
        return true;
    }
}
