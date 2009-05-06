package gsn.beans.windowing;

import gsn.TestUtils;
import gsn.VirtualSensorPool;
import gsn.Helpers;
import gsn.beans.*;
import gsn.storage.DataEnumerator;
import gsn.storage.StorageManager;
import gsn.vsensor.BridgeVirtualSensor;
import org.junit.*;
import static org.junit.Assert.*;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TestTupleBasedSliding {
    private MockWrapper wrapper = new MockWrapper();

    private StorageManager sm = StorageManager.getInstance();

    private AddressBean[] addressing = new AddressBean[]{new AddressBean("wrapper-for-test")};

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        Helpers.initLoggerToDebug();
        initDB();
    }

    private static void initDB() throws SQLException {
        StorageManager.getInstance().init("jdbc:h2:mem:gsn_mem_db");
    }

    @AfterClass
    public static void tearDownAfterClass() throws SQLException {
        StorageManager.getInstance().shutdown();
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

    /**
     * Testing tuple-based slide on each tuple
     */
    @Test
    public void testTupleBasedWindowSlideOnEachTuple() {
        try {
            InputStream is = new InputStream();
            is.setQuery("select * from mystream limit 1 offset 0");
            StreamSource ss = new StreamSource().setAlias("mystream").setAddressing(addressing).setSqlQuery("select * from wrapper")
                    .setRawHistorySize("2").setRawSlideValue("1").setInputStream(is);
            ss.setSamplingRate(1);
            is.setSources(ss);
            assertTrue(ss.validate());
            ss.setWrapper(wrapper);

            VSensorConfig config = new VSensorConfig();
            config.setName("testvs");
            config.setMainClass(BridgeVirtualSensor.class.getName());
            config.setInputStreams(is);
            config.setStorageHistorySize("10");
            config.setOutputStructure(new DataField[]{});
            config.setFileName("dummy-vs-file");
            assertTrue(config.validate());

            VirtualSensorPool pool = new VirtualSensorPool(config);
            is.setPool(pool);
            if (sm.tableExists(config.getName()))
                sm.executeDropTable(config.getName());
            sm.executeCreateTable(config.getName(), config.getOutputStructure(), true);
            // Mappings.addVSensorInstance ( pool );
            pool.start();
            assertNotNull(pool.borrowVS());

            assertTrue(is.validate());
            assertTrue(ss.rewrite(is.getQuery()).indexOf(ss.getUIDStr().toString()) > 0);

            assertEquals(ss.getWindowingType(), WindowType.TUPLE_BASED_SLIDE_ON_EACH_TUPLE);
            assertTrue(SQLViewQueryHandler.class.isAssignableFrom(ss.getQueryHandler().getClass()));
            assertTrue(((SQLViewQueryHandler) ss.getQueryHandler()).createViewSQL().toString().toLowerCase().indexOf("mod") < 0);

            StringBuilder query = new StringBuilder(((SQLViewQueryHandler) ss.getQueryHandler()).createViewSQL());

            TestUtils.print(query.toString());

            long time = System.currentTimeMillis();
            wrapper.postStreamElement(TestUtils.createStreamElement(time));
            ResultSet rs = StorageManager.getInstance().executeQueryWithResultSet(query);
            assertFalse(rs.next());

            StorageManager.close(rs);

            StringBuilder vsQuery = new StringBuilder("select * from ").append(config.getName());
            StringBuilder sb = new StringBuilder("SELECT timed from ").append(SQLViewQueryHandler.VIEW_HELPER_TABLE).append(" where u_id='")
                    .append(ss.getUIDStr()).append("'");
            rs = sm.executeQueryWithResultSet(sb);
            assertTrue(rs.next());
            assertEquals(rs.getLong(1), time);

            StorageManager.close(rs);

            long time1 = time + 10;
            wrapper.postStreamElement(TestUtils.createStreamElement(time1));
            long time2 = time + 100;
            wrapper.postStreamElement(TestUtils.createStreamElement(time2));

            DataEnumerator dm = sm.executeQuery(query, true);
            rs = StorageManager.getInstance().executeQueryWithResultSet(query);
            assertNotNull(rs);
            assertTrue(rs.next());
            assertTrue(rs.next());
            assertFalse(rs.next());

            assertTrue(dm.hasMoreElements());
            assertEquals(dm.nextElement().getTimeStamp(), time2);
            assertTrue(dm.hasMoreElements());
            assertEquals(dm.nextElement().getTimeStamp(), time1);
            assertFalse(dm.hasMoreElements());

            StorageManager.close(rs);

            rs = sm.executeQueryWithResultSet(vsQuery);
            assertTrue(rs.next());

            StorageManager.close(rs);

            wrapper.removeListener(ss);
        } catch (Exception e) {
            AssertionError ae = new AssertionError();
            ae.initCause(e);
            throw ae;
        }
    }

    /**
     * Testing tuple-based window-slide
     */
    @Test
    public void testTupleBasedSlidingWindow() {
        try {
            InputStream is = new InputStream();
            is.setQuery("select * from mystream");
            StreamSource ss = new StreamSource().setAlias("mystream").setAddressing(addressing).setSqlQuery("select * from wrapper")
                    .setRawHistorySize("2").setRawSlideValue("2").setInputStream(is);
            ss.setSamplingRate(1);
            is.setSources(ss);
            assertTrue(ss.validate());
            ss.setWrapper(wrapper);

            VSensorConfig config = new VSensorConfig();
            config.setName("testvs");
            config.setMainClass(BridgeVirtualSensor.class.getName());
            config.setInputStreams(is);
            config.setStorageHistorySize("10");
            config.setOutputStructure(new DataField[]{});
            config.setFileName("dummy-vs-file");
            assertTrue(config.validate());

            VirtualSensorPool pool = new VirtualSensorPool(config);
            is.setPool(pool);
            if (sm.tableExists(config.getName()))
                sm.executeDropTable(config.getName());
            sm.executeCreateTable(config.getName(), config.getOutputStructure(), false);
            // Mappings.addVSensorInstance ( pool );
            pool.start();

            assertTrue(is.validate());
            assertTrue(ss.rewrite(is.getQuery()).indexOf(ss.getUIDStr().toString()) > 0);

            assertEquals(ss.getWindowingType(), WindowType.TUPLE_BASED);
            assertTrue(SQLViewQueryHandler.class.isAssignableFrom(ss.getQueryHandler().getClass()));
            assertTrue(((SQLViewQueryHandler) ss.getQueryHandler()).createViewSQL().toString().toLowerCase().indexOf("mod") < 0);
            StringBuilder query = new StringBuilder(((SQLViewQueryHandler) ss.getQueryHandler()).createViewSQL());
            TestUtils.print(query.toString());

            long time = System.currentTimeMillis();
            wrapper.postStreamElement(TestUtils.createStreamElement(time));
            ResultSet rs = StorageManager.getInstance().executeQueryWithResultSet(query);
            assertFalse(rs.next());

            StorageManager.close(rs);

            StringBuilder vsQuery = new StringBuilder("select * from ").append(config.getName());
            StringBuilder sb = new StringBuilder("SELECT timed from ").append(SQLViewQueryHandler.VIEW_HELPER_TABLE).append(" where u_id='")
                    .append(ss.getUIDStr()).append("'");
            rs = sm.executeQueryWithResultSet(sb);
            assertTrue(rs.next());
            assertEquals(rs.getLong(1), -1L);

            StorageManager.close(rs);

            long time1 = time + 10;
            wrapper.postStreamElement(TestUtils.createStreamElement(time1));

            rs = sm.executeQueryWithResultSet(sb);
            assertTrue(rs.next());
            assertEquals(rs.getLong(1), time1);

            StorageManager.close(rs);

            rs = sm.executeQueryWithResultSet(vsQuery);
            assertTrue(rs.next());
            assertTrue(rs.next());
            assertFalse(rs.next());

            StorageManager.close(rs);

            long time2 = time + 100;
            wrapper.postStreamElement(TestUtils.createStreamElement(time2));

            rs = sm.executeQueryWithResultSet(sb);
            assertTrue(rs.next());
            assertEquals(rs.getLong(1), time1);

            StorageManager.close(rs);

            rs = sm.executeQueryWithResultSet(vsQuery);
            assertTrue(rs.next());
            assertTrue(rs.next());
            assertFalse(rs.next());

            StorageManager.close(rs);

            DataEnumerator dm = sm.executeQuery(query, true);
            rs = sm.executeQueryWithResultSet(query);
            assertTrue(rs.next());
            assertTrue(rs.next());
            assertFalse(rs.next());

            assertTrue(dm.hasMoreElements());
            assertEquals(dm.nextElement().getTimeStamp(), time1);
            assertTrue(dm.hasMoreElements());
            assertEquals(dm.nextElement().getTimeStamp(), time);
            assertFalse(dm.hasMoreElements());

            StorageManager.close(rs);

            long time3 = time + 200;
            wrapper.postStreamElement(TestUtils.createStreamElement(time3));

            rs = sm.executeQueryWithResultSet(sb);
            assertTrue(rs.next());
            assertEquals(rs.getLong(1), time3);

            StorageManager.close(rs);

            rs = sm.executeQueryWithResultSet(vsQuery);
            assertTrue(rs.next());
            assertTrue(rs.next());
            assertTrue(rs.next());
            assertTrue(rs.next());
            assertFalse(rs.next());

            dm = sm.executeQuery(query, true);
            assertTrue(dm.hasMoreElements());
            assertEquals(dm.nextElement().getTimeStamp(), time3);
            assertTrue(dm.hasMoreElements());
            assertEquals(dm.nextElement().getTimeStamp(), time2);
            assertFalse(dm.hasMoreElements());

            StorageManager.close(rs);

            wrapper.removeListener(ss);
        } catch (Exception e) {
            AssertionError ae = new AssertionError();
            ae.initCause(e);
            throw ae;
        }
    }

    /**
     * Testing time-based-win-tuple-based-slide
     */
    @Test
    public void testTimeBasedWindowTupleBasedSlide() {
        try {
            InputStream is = new InputStream();
            is.setQuery("select * from mystream");
            StreamSource ss = new StreamSource().setAlias("mystream").setAddressing(addressing).setSqlQuery("select * from wrapper")
                    .setRawHistorySize("2s").setRawSlideValue("2").setInputStream(is);
            ss.setSamplingRate(1);
            is.setSources(ss);
            assertTrue(ss.validate());
            ss.setWrapper(wrapper);

            VSensorConfig config = new VSensorConfig();
            config.setName("testvs");
            config.setMainClass(BridgeVirtualSensor.class.getName());
            config.setInputStreams(is);
            config.setStorageHistorySize("10");
            config.setOutputStructure(new DataField[]{});
            config.setFileName("dummy-vs-file");
            assertTrue(config.validate());

            VirtualSensorPool pool = new VirtualSensorPool(config);
            is.setPool(pool);
            if (sm.tableExists(config.getName()))
                sm.executeDropTable(config.getName());
            sm.executeCreateTable(config.getName(), config.getOutputStructure(), false);
            // Mappings.addVSensorInstance ( pool );
            pool.start();

            assertTrue(is.validate());
            assertTrue(ss.rewrite(is.getQuery()).indexOf(ss.getUIDStr().toString()) > 0);

            assertEquals(ss.getWindowingType(), WindowType.TIME_BASED_WIN_TUPLE_BASED_SLIDE);
            assertTrue(SQLViewQueryHandler.class.isAssignableFrom(ss.getQueryHandler().getClass()));
            assertTrue(((SQLViewQueryHandler) ss.getQueryHandler()).createViewSQL().toString().toLowerCase().indexOf("mod") < 0);
            StringBuilder query = new StringBuilder(((SQLViewQueryHandler) ss.getQueryHandler()).createViewSQL());
            TestUtils.print(query.toString());

            long time = System.currentTimeMillis();
            wrapper.postStreamElement(TestUtils.createStreamElement(time));
            ResultSet rs = StorageManager.getInstance().executeQueryWithResultSet(query);
            assertFalse(rs.next());

            StorageManager.close(rs);

            StringBuilder vsQuery = new StringBuilder("select * from ").append(config.getName());
            StringBuilder sb = new StringBuilder("SELECT timed from ").append(SQLViewQueryHandler.VIEW_HELPER_TABLE).append(" where u_id='")
                    .append(ss.getUIDStr()).append("'");
            rs = sm.executeQueryWithResultSet(sb);
            assertTrue(rs.next());
            assertEquals(rs.getLong(1), -1L);

            StorageManager.close(rs);

            long time1 = time + 1000;
            wrapper.postStreamElement(TestUtils.createStreamElement(time1));

            rs = sm.executeQueryWithResultSet(sb);
            assertTrue(rs.next());
            assertEquals(rs.getLong(1), time1);

            StorageManager.close(rs);

            rs = sm.executeQueryWithResultSet(vsQuery);
            assertTrue(rs.next());
            assertTrue(rs.next());
            assertFalse(rs.next());

            StorageManager.close(rs);

            long time2 = time1 + 1500;
            wrapper.postStreamElement(TestUtils.createStreamElement(time2));

            rs = sm.executeQueryWithResultSet(sb);
            assertTrue(rs.next());
            assertEquals(rs.getLong(1), time1);

            StorageManager.close(rs);

            long time3 = time2 + 1000;
            wrapper.postStreamElement(TestUtils.createStreamElement(time3));

            rs = sm.executeQueryWithResultSet(sb);
            assertTrue(rs.next());
            assertEquals(rs.getLong(1), time3);

            StorageManager.close(rs);

            rs = sm.executeQueryWithResultSet(vsQuery);
            assertTrue(rs.next());
            assertTrue(rs.next());
            assertTrue(rs.next());
            assertTrue(rs.next());
            assertFalse(rs.next());

            DataEnumerator dm = sm.executeQuery(query, true);
            assertTrue(dm.hasMoreElements());
            assertEquals(dm.nextElement().getTimeStamp(), time3);
            assertTrue(dm.hasMoreElements());
            assertEquals(dm.nextElement().getTimeStamp(), time2);
            assertFalse(dm.hasMoreElements());

            StorageManager.close(rs);

            wrapper.removeListener(ss);
        } catch (Exception e) {
            AssertionError ae = new AssertionError();
            ae.initCause(e);
            throw ae;
        }
    }

    /**
     * Testing non triggering tuple-based slide on each tuple
     */
    @Test
    public void testNonTriggeringTupleBasedWindowSlideOnEachTuple() {
        try {
            InputStream is = new InputStream();
            is.setQuery("select * from mystream limit 1 offset 0");
            StreamSource ss = new StreamSource().setAlias("mystream").setAddressing(addressing).setSqlQuery("select * from wrapper")
                    .setRawHistorySize("2").setRawSlideValue("1").setInputStream(is);
            ss.setSamplingRate(1);
            ss.setTriggerSliding(false);
            is.setSources(ss);
            assertTrue(ss.validate());
            ss.setWrapper(wrapper);

            VSensorConfig config = new VSensorConfig();
            config.setName("testvs");
            config.setMainClass(BridgeVirtualSensor.class.getName());
            config.setInputStreams(is);
            config.setStorageHistorySize("10");
            config.setOutputStructure(new DataField[]{});
            config.setFileName("dummy-vs-file");
            assertTrue(config.validate());

            VirtualSensorPool pool = new VirtualSensorPool(config);
            is.setPool(pool);
            if (sm.tableExists(config.getName()))
                sm.executeDropTable(config.getName());
            sm.executeCreateTable(config.getName(), config.getOutputStructure(), true);
            // Mappings.addVSensorInstance ( pool );
            pool.start();
            assertNotNull(pool.borrowVS());

            assertTrue(is.validate());

            StringBuilder query = new StringBuilder(((SQLViewQueryHandler) ss.getQueryHandler()).createViewSQL());
            TestUtils.print(query.toString());

            long time = System.currentTimeMillis();
            wrapper.postStreamElement(TestUtils.createStreamElement(time));
            ResultSet rs = StorageManager.getInstance().executeQueryWithResultSet(query);
            assertFalse(rs.next());

            StorageManager.close(rs);

            StringBuilder vsQuery = new StringBuilder("select * from ").append(config.getName());
            StringBuilder sb = new StringBuilder("SELECT timed from ").append(SQLViewQueryHandler.VIEW_HELPER_TABLE).append(" where u_id='")
                    .append(ss.getUIDStr()).append("'");
            rs = sm.executeQueryWithResultSet(sb);
            assertTrue(rs.next());
            assertEquals(rs.getLong(1), time);

            StorageManager.close(rs);

            long time1 = time + 10;
            wrapper.postStreamElement(TestUtils.createStreamElement(time1));
            long time2 = time + 100;
            wrapper.postStreamElement(TestUtils.createStreamElement(time2));

            DataEnumerator dm = sm.executeQuery(query, true);
            rs = StorageManager.getInstance().executeQueryWithResultSet(query);
            assertNotNull(rs);
            assertTrue(rs.next());
            assertTrue(rs.next());
            assertFalse(rs.next());

            assertTrue(dm.hasMoreElements());
            assertEquals(dm.nextElement().getTimeStamp(), time2);
            assertTrue(dm.hasMoreElements());
            assertEquals(dm.nextElement().getTimeStamp(), time1);
            assertFalse(dm.hasMoreElements());

            StorageManager.close(rs);

            rs = sm.executeQueryWithResultSet(vsQuery);
            assertFalse(rs.next());

            StorageManager.close(rs);

            wrapper.removeListener(ss);
        } catch (Exception e) {
            AssertionError ae = new AssertionError();
            ae.initCause(e);
            throw ae;
        }
    }
}
