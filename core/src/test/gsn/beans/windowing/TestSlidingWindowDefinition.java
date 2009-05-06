package gsn.beans.windowing;

import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.beans.InputStream;
import gsn.beans.StreamSource;
import gsn.storage.StorageManager;
import gsn.utils.GSNRuntimeException;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Testing windowing, part one. <br>
 * Notes:
 * <ol>
 * <li>The tests for time-based windows may not pass because of the
 * complication of testing time and the dependence on runtime.</li>
 * <li>As SQL Server does not support order by clause in view queries, some of
 * the tests won't be passed for SQL Server</li>
 * </ol>
 */
public class TestSlidingWindowDefinition {

    private MockWrapper wrapper = new MockWrapper();

    private StorageManager sm = StorageManager.getInstance();

    private AddressBean[] addressing = new AddressBean[]{new AddressBean("wrapper-for-test")};

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initDB();
    }

    private static void initDB() throws SQLException {
        StorageManager.getInstance().init("jdbc:h2:mem:gsn_mem_db");
    }

    @Before
    public void setup() throws SQLException {
        sm.executeCreateTable(wrapper.getDBAliasInStr(), new DataField[]{}, true);
        wrapper.setActiveAddressBean(new AddressBean("system-time"));
        assertTrue(wrapper.initialize());
    }

    @After
    public void teardown() throws SQLException {
        sm.executeDropTable(wrapper.getDBAliasInStr());
    }

    @Test(expected = GSNRuntimeException.class)
    public void testBadStreamSources() throws GSNRuntimeException {
        InputStream is = new InputStream();
        StreamSource ss = new StreamSource().setAlias("mystream").setAddressing(addressing).setSqlQuery("select * from wrapper")
                .setRawHistorySize("10  min").setInputStream(is);
    }

    @Test(expected = GSNRuntimeException.class)
    public void testBadStreamSources2() throws GSNRuntimeException {
        InputStream is = new InputStream();
        StreamSource ss = new StreamSource().setAlias("mystream").setAddressing(addressing).setSqlQuery("select * from wrapper")
                .setRawHistorySize("10  m20").setInputStream(is);
    }

    @Test(expected = GSNRuntimeException.class)
    public void testBadStreamSources3() throws GSNRuntimeException {
        InputStream is = new InputStream();
        StreamSource ss = new StreamSource().setAlias("mystream").setAddressing(addressing).setSqlQuery("select * from wrapper")
                .setRawHistorySize("m").setInputStream(is);
    }

    @Test
    public void testBadWindowSize() throws GSNRuntimeException {
        StreamSource ss = new StreamSource().setAlias("mystream").setAddressing(addressing).setSqlQuery("select * from wrapper")
                .setRawHistorySize("10  sec");
        assertFalse(ss.validate());
    }

    @Test
    public void testBadSlideValue() throws GSNRuntimeException {
        StreamSource ss = new StreamSource().setAlias("mystream").setAddressing(addressing).setSqlQuery("select * from wrapper")
                .setRawHistorySize("10  s").setRawSlideValue("5 sec");
        assertFalse(ss.validate());
    }

    @Test
    public void testTimeBasedWindowTypes() {
        StreamSource ss = new StreamSource().setAlias("mystream").setAddressing(addressing).setSqlQuery("select * from wrapper")
                .setRawHistorySize("10 s");
        assertEquals(ss.getWindowingType(), WindowType.TIME_BASED_SLIDE_ON_EACH_TUPLE);
        ss.setRawSlideValue("5 s");
        assertEquals(ss.getWindowingType(), WindowType.TIME_BASED);
        ss.setRawSlideValue("2");
        assertEquals(ss.getWindowingType(), WindowType.TIME_BASED_WIN_TUPLE_BASED_SLIDE);
        ss.setRawSlideValue("");
        assertEquals(ss.getWindowingType(), WindowType.TIME_BASED_SLIDE_ON_EACH_TUPLE);
    }

    @Test
    public void testTupleBasedWindowType() {
        StreamSource ss = new StreamSource().setAlias("mystream").setAddressing(addressing).setSqlQuery("select * from wrapper")
                .setRawHistorySize("10 ");
        assertTrue(ss.validate());
        assertTrue(ss.validate());
        assertEquals(ss.getWindowingType(), WindowType.TUPLE_BASED_SLIDE_ON_EACH_TUPLE);
        ss.setRawSlideValue("5 s");
        assertEquals(ss.getWindowingType(), WindowType.TUPLE_BASED_WIN_TIME_BASED_SLIDE);
        ss.setRawSlideValue("2");
        assertEquals(ss.getWindowingType(), WindowType.TUPLE_BASED);
        ss.setRawSlideValue("");
        assertEquals(ss.getWindowingType(), WindowType.TUPLE_BASED_SLIDE_ON_EACH_TUPLE);
        ss.setRawHistorySize("");
        ss.setRawSlideValue("");
        assertEquals(ss.getWindowingType(), WindowType.TUPLE_BASED_SLIDE_ON_EACH_TUPLE);
    }

}
