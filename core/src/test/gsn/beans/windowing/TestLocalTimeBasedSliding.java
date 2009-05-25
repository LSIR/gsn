package gsn.beans.windowing;

import gsn.Helpers;
import gsn.TestUtils;
import gsn.VirtualSensorPool;
import gsn.beans.*;
import gsn.storage.DataEnumerator;
import gsn.storage.StorageManager;
import gsn.vsensor.BridgeVirtualSensor;
import static org.testng.Assert.*;
import org.testng.annotations.*;

import java.sql.ResultSet;
import java.sql.SQLException;

@Test(sequential = true)
public class TestLocalTimeBasedSliding {
    private MockWrapper wrapper = new MockWrapper();
    private StorageManager sm = StorageManager.getInstance();
    private AddressBean[] addressing = new AddressBean[]{new AddressBean("wrapper-for-test")};

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initDB();
        Helpers.initLoggerToDebug();
    }

    private static void initDB() throws SQLException {
        StorageManager.getInstance().init("jdbc:h2:mem:gsn_mem_db");
    }

    @AfterClass
    public static void tearDownAfterClass() throws SQLException {
        StorageManager.getInstance().shutdown();
    }


    @BeforeMethod
    public void setup() throws SQLException {
        System.out.println("******************** before method ----------------------");
        sm.executeCreateTable(wrapper.getDBAliasInStr(), new DataField[]{}, true);
        try {
            wrapper.setActiveAddressBean(new AddressBean("system-time"));
        } catch (Exception e) {

        }
        assertTrue(wrapper.initialize());
    }

    @AfterMethod
    public void teardown() throws SQLException {
        System.out.println("================= after method ----------------------");
        sm.executeDropTable(wrapper.getDBAliasInStr());
    }

    /**
     * Testing time-based slide on each tuple
     */
    @Test(enabled = true)
    public void testTimeBasedWindowSlideOnEachTuple() {
        try {
            InputStream is = new InputStream();
            is.setQuery("select * from mystream");
            StreamSource ss = new StreamSource().setAlias("mystream").setAddressing(addressing).setSqlQuery("select * from wrapper")
                    .setRawHistorySize("2s").setInputStream(is);
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

            assertEquals(ss.getWindowingType(), WindowType.TIME_BASED_SLIDE_ON_EACH_TUPLE);
            assertTrue(SQLViewQueryHandler.class.isAssignableFrom(ss.getQueryHandler().getClass()));
            assertTrue(((SQLViewQueryHandler) ss.getQueryHandler()).createViewSQL().toString().toLowerCase().indexOf("mod") < 0);

            StringBuilder query = new StringBuilder(((SQLViewQueryHandler) ss.getQueryHandler()).createViewSQL());
            TestUtils.print(query.toString());

            long time = System.currentTimeMillis();
            wrapper.postStreamElement(TestUtils.createStreamElement(time));
            ResultSet rs = StorageManager.getInstance().executeQueryWithResultSet(query);
            assertTrue(rs.next());
            assertFalse(rs.next());
            StorageManager.close(rs);

            StringBuilder sb = new StringBuilder("SELECT timed from ").append(SQLViewQueryHandler.VIEW_HELPER_TABLE).append(" where u_id='")
                    .append(ss.getUIDStr()).append("'");
            rs = sm.executeQueryWithResultSet(sb);
            assertTrue(rs.next());
            assertEquals(rs.getLong(1), time);
            StorageManager.close(rs);

            long time1 = time + 1000;
            wrapper.postStreamElement(TestUtils.createStreamElement(time1));
            long time2 = time + 2500;
            wrapper.postStreamElement(TestUtils.createStreamElement(time2));

            try {
                Thread.sleep(2700);
            } catch (InterruptedException e) {
            }

            DataEnumerator dm = sm.executeQuery(query, true);
            rs = StorageManager.getInstance().executeQueryWithResultSet(query);
            assertTrue(rs.next());
            assertTrue(rs.next());
            assertFalse(rs.next());

            assertTrue(dm.hasMoreElements());
            assertEquals(dm.nextElement().getTimeStamp(), time2);
            assertTrue(dm.hasMoreElements());
            assertEquals(dm.nextElement().getTimeStamp(), time1);
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
     * Testing time-based window-slide
     */
    @Test(enabled = true, dependsOnMethods = {"testTimeBasedWindowSlideOnEachTuple"})
    public void testTimeBasedSlidingWindow() {
        try {
            InputStream is = new InputStream();
            is.setQuery("select * from mystream");
            StreamSource ss = new StreamSource().setAlias("mystream").setAddressing(addressing).setSqlQuery("select * from wrapper")
                    .setRawHistorySize("3s").setRawSlideValue("2s").setInputStream(is);
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

            assertEquals(ss.getWindowingType(), WindowType.TIME_BASED);
            assertTrue(SQLViewQueryHandler.class.isAssignableFrom(ss.getQueryHandler().getClass()));
            assertTrue(((SQLViewQueryHandler) ss.getQueryHandler()).createViewSQL().toString().toLowerCase().indexOf("mod") < 0);
            StringBuilder query = new StringBuilder(((SQLViewQueryHandler) ss.getQueryHandler()).createViewSQL());
            TestUtils.print(query.toString());

            long time = System.currentTimeMillis();
            wrapper.postStreamElement(TestUtils.createStreamElement(time));
            ResultSet rs = StorageManager.getInstance().executeQueryWithResultSet(query);
            assertFalse(rs.next());
            StorageManager.close(rs);

            StringBuilder sb = new StringBuilder("SELECT timed from ").append(SQLViewQueryHandler.VIEW_HELPER_TABLE).append(" where u_id='")
                    .append(ss.getUIDStr()).append("'");
            rs = sm.executeQueryWithResultSet(sb);
            assertTrue(rs.next());
            assertEquals(rs.getLong(1), -1L);
            StorageManager.close(rs);

            long time1 = time + 1500;
            wrapper.postStreamElement(TestUtils.createStreamElement(time1));
            long time2 = time + 2800;
            wrapper.postStreamElement(TestUtils.createStreamElement(time2));

            try {
                Thread.sleep(3200);
            } catch (InterruptedException e) {
            }

            long time3 = time + 3500;
            wrapper.postStreamElement(TestUtils.createStreamElement(time3));
            DataEnumerator dm = sm.executeQuery(query, true);
            rs = StorageManager.getInstance().executeQueryWithResultSet(query);
            assertTrue(rs.next());
            assertTrue(rs.next());
            assertFalse(rs.next());

            assertTrue(dm.hasMoreElements());
            assertEquals(dm.nextElement().getTimeStamp(), time1);
            assertTrue(dm.hasMoreElements());
            assertEquals(dm.nextElement().getTimeStamp(), time);
            assertFalse(dm.hasMoreElements());

            StorageManager.close(rs);

            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
            }

            dm = sm.executeQuery(query, true);
            assertTrue(dm.hasMoreElements());
            assertEquals(dm.nextElement().getTimeStamp(), time3);
            assertTrue(dm.hasMoreElements());
            assertEquals(dm.nextElement().getTimeStamp(), time2);
            assertTrue(dm.hasMoreElements());
            assertEquals(dm.nextElement().getTimeStamp(), time1);
            assertFalse(dm.hasMoreElements());

            rs = StorageManager.getInstance().executeQueryWithResultSet(query);
            assertTrue(rs.next());
            assertTrue(rs.next());
            assertTrue(rs.next());
            assertFalse(rs.next());

            StorageManager.close(rs);
            wrapper.removeListener(ss);
        } catch (Exception e) {
            AssertionError ae = new AssertionError();
            ae.initCause(e);
            throw ae;
        }
    }

    /**
     * Testing tuple-based-win-time-based-slide
     */
    @Test(enabled = true, dependsOnMethods = {"testTimeBasedSlidingWindow"})
    public void testTupleBasedWindowTimeBasedSlide() {
        try {
            InputStream is = new InputStream();
            is.setQuery("select * from mystream");
            StreamSource ss = new StreamSource().setAlias("mystream").setAddressing(addressing).setSqlQuery("select * from wrapper")
                    .setRawHistorySize("2").setRawSlideValue("2s").setInputStream(is);
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

            assertEquals(ss.getWindowingType(), WindowType.TUPLE_BASED_WIN_TIME_BASED_SLIDE);
            assertTrue(SQLViewQueryHandler.class.isAssignableFrom(ss.getQueryHandler().getClass()));
            assertTrue(((SQLViewQueryHandler) ss.getQueryHandler()).createViewSQL().toString().toLowerCase().indexOf("mod") < 0);
            StringBuilder query = new StringBuilder(((SQLViewQueryHandler) ss.getQueryHandler()).createViewSQL());
            TestUtils.print(query.toString());

            long time = System.currentTimeMillis();
            wrapper.postStreamElement(TestUtils.createStreamElement(time));
            ResultSet rs = StorageManager.getInstance().executeQueryWithResultSet(query);
            assertFalse(rs.next());

            StringBuilder sb = new StringBuilder("SELECT timed from ").append(SQLViewQueryHandler.VIEW_HELPER_TABLE).append(" where u_id='")
                    .append(ss.getUIDStr()).append("'");
            rs = sm.executeQueryWithResultSet(sb);
            assertTrue(rs.next());
            assertEquals(rs.getLong(1), -1L);
            StorageManager.close(rs);

            long time1 = time + 1500;
            wrapper.postStreamElement(TestUtils.createStreamElement(time1));
            long time2 = time + 2500;
            wrapper.postStreamElement(TestUtils.createStreamElement(time2));

            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            long time3 = time + 3500;
            wrapper.postStreamElement(TestUtils.createStreamElement(time3));

            DataEnumerator dm = sm.executeQuery(query, true);
            rs = StorageManager.getInstance().executeQueryWithResultSet(query);
            assertTrue(rs.next());
            assertTrue(rs.next());
            assertFalse(rs.next());

            assertTrue(dm.hasMoreElements());
            assertEquals(dm.nextElement().getTimeStamp(), time1);
            assertTrue(dm.hasMoreElements());
            assertEquals(dm.nextElement().getTimeStamp(), time);
            assertFalse(dm.hasMoreElements());

            StorageManager.close(rs);

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }

            dm = sm.executeQuery(query, true);
            rs = StorageManager.getInstance().executeQueryWithResultSet(query);
            assertTrue(rs.next());
            assertTrue(rs.next());
            assertFalse(rs.next());

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
}
