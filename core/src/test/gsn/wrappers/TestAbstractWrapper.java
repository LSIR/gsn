package gsn.wrappers;

import static org.junit.Assert.*;
import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.beans.InputStream;
import gsn.beans.StreamSource;
import gsn.storage.StorageManager;
import gsn.utils.GSNRuntimeException;
import gsn.utils.KeyValueImp;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.naming.OperationNotSupportedException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestAbstractWrapper {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        StorageManager.getInstance().init("jdbc:h2:mem:gsn_mem_db");
//		StorageManager.getInstance ( ).initialize ( "com.mysql.jdbc.Driver","root","" , "jdbc:mysql://localhost/gsn");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    private StorageManager sm;


    @Before
    public void setUp() throws Exception {
        sm = StorageManager.getInstance();
    }


    @Test(expected = OperationNotSupportedException.class)
    public void testSendToWrapper1() throws OperationNotSupportedException {
        SystemTime systemTimeWrapper = new SystemTime();
        systemTimeWrapper.sendToWrapper("bla");
    }

    /**
     * Test method for {@link gsn.wrappers.AbstractWrapper#sendToWrapper(java.lang.Object)}.
     * Test to see what is the behavior if the wrapper is disabled.
     *
     * @throws OperationNotSupportedException
     * @throws SQLException
     */
    @Test(expected = OperationNotSupportedException.class)
    public void testSendToWrapper2() throws OperationNotSupportedException, SQLException {
        SystemTime systemTimeWrapper = new SystemTime();
        systemTimeWrapper.setActiveAddressBean(new AddressBean("system-time"));
        assertTrue(systemTimeWrapper.initialize());
        Thread thread = new Thread(systemTimeWrapper);
        thread.start();
        systemTimeWrapper.releaseResources();
        systemTimeWrapper.sendToWrapper("bla");
    }

    @Test
    public void testRemovingUselessData() throws SQLException, InterruptedException {
        SystemTime wrapper = new SystemTime();
        StorageManager.getInstance().executeCreateTable(wrapper.getDBAliasInStr(), new DataField[]{}, true);
        wrapper.setActiveAddressBean(new AddressBean("system-time", new KeyValueImp(SystemTime.CLOCK_PERIOD_KEY, "100")));
        assertTrue(wrapper.initialize());
        Thread thread = new Thread(wrapper);
        InputStream is = new InputStream();
        StreamSource ss = new StreamSource().setAlias("my-stream").setAddressing(new AddressBean[]{new AddressBean("system-time")}).setSqlQuery("select * from wrapper where TIMED <0").setRawHistorySize("2").setInputStream(is);
        ss.setSamplingRate(1);
        ss.setWrapper(wrapper);
        assertTrue(ss.validate());
        assertEquals(wrapper.getTimerClockPeriod(), 100);
        thread.start();
        Thread.sleep(1000);
        ResultSet rs = StorageManager.getInstance().executeQueryWithResultSet(new StringBuilder("select count(*) from ").append(wrapper.getDBAliasInStr()));
        assertTrue(rs.next());
//    System.out.println(rs.getInt(1));
        assertTrue(rs.getInt(1) <= (AbstractWrapper.GARBAGE_COLLECT_AFTER_SPECIFIED_NO_OF_ELEMENTS * 2));
        wrapper.releaseResources();
    }

}
