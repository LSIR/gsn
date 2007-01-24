package gsn.beans;

import static org.junit.Assert.*;

import java.io.Serializable;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import gsn.storage.DataEnumerator;
import gsn.storage.StorageManager;
import gsn.utils.GSNRuntimeException;
import gsn.wrappers.AbstractWrapper;
import gsn.wrappers.SystemTime;

import org.apache.commons.httpclient.protocol.SSLProtocolSocketFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mysql.jdbc.SQLError;

public class TestStreamSource {

	private AbstractWrapper wrapper = new SystemTime();
	private StorageManager sm =  StorageManager.getInstance();


	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		DriverManager.registerDriver( new org.hsqldb.jdbcDriver( ) );
		StorageManager.getInstance ( ).initialize ( "org.hsqldb.jdbcDriver","sa","" ,"jdbc:hsqldb:mem:." );
	}

	@Before
	public void setup() throws SQLException {
		sm.createTable(wrapper.getDBAliasInStr(), new DataField[] {});
	}
	@After
	public void teardown() {
		sm.dropTable(wrapper.getDBAliasInStr());
	}

	@Test
	public void testGetStartAndEndTime() {
		StreamSource ss = new StreamSource().setAlias("my-stream").setRawHistorySize("10  s");
		assertFalse(new Date(System.currentTimeMillis()).before(ss.getStartDate()));
		assertTrue(new Date(System.currentTimeMillis()).before(ss.getEndDate()));
	}

	@Test
	public void testCanStart() {
		InputStream is = new InputStream();
		ArrayList<AddressBean> addressing = new ArrayList<AddressBean>();
		addressing.add(new AddressBean("system-time"));
		StreamSource ss = new StreamSource().setAlias("my-stream").setRawHistorySize("10  s").setAddressing(addressing);
		ss.setInputStream(is);
		assertTrue(ss.canStart());
	}


	@Test
	public void testGetSQLQuery() {
		ArrayList<AddressBean> addressing = new ArrayList<AddressBean>();
		addressing.add(new AddressBean("system-time"));
		InputStream is = new InputStream();
		StreamSource ss = new StreamSource();
		ss.setAddressing(addressing);
		ss.setAlias("my-stream");
		ss.setRawHistorySize("10m");
		ss.setInputStream(is);
		assertTrue(ss.getSqlQuery().trim().equals("select * from wrapper"));
		ss = new StreamSource();
		ss.setAddressing(addressing);
		ss.setAlias("my-stream");
		ss.setRawHistorySize("10m");
		ss.setSqlQuery(" ");
		ss.setInputStream(is);
		assertEquals(ss.getSqlQuery().trim(),"select * from wrapper");
	}

	@Test
	public void testValidate() {
		InputStream is = new InputStream();
		ArrayList<AddressBean> addressing = new ArrayList<AddressBean>();
		addressing.add(new AddressBean("system-time"));
		StreamSource ss = new StreamSource().setAlias("my-stream").setAddressing(addressing).setSqlQuery("select * from wrapper").setRawHistorySize("10m");
		assertTrue(ss.validate());
		assertFalse(ss.isStorageCountBased());
		assertEquals(ss.getParsedStorageSize(),10*60*1000);
		ss = new StreamSource().setAlias("my-stream").setAddressing(addressing).setSqlQuery("select * from wrapper").setRawHistorySize("10  m").setInputStream(is);
		assertFalse(ss.isStorageCountBased());
		assertEquals(ss.getParsedStorageSize(),10*60*1000);


		ss = new StreamSource().setAlias("my-stream").setAddressing(addressing).setSqlQuery("select * from wrapper").setRawHistorySize("10  s").setInputStream(is);
		assertFalse(ss.isStorageCountBased());
		assertEquals(ss.getParsedStorageSize(),10*1000);
		assertFalse(ss.isStorageCountBased());


		ss = new StreamSource().setAlias("my-stream").setAddressing(addressing).setSqlQuery("select * from wrapper").setRawHistorySize("2 h").setInputStream(is);
		assertFalse(ss.isStorageCountBased());
		assertEquals(ss.getParsedStorageSize(),2*60*60*1000);
	}
	@Test (expected=GSNRuntimeException.class)
	public void testBadStreamSources() throws GSNRuntimeException{
		InputStream is = new InputStream();

		ArrayList<AddressBean> addressing = new ArrayList<AddressBean>();
		addressing.add(new AddressBean("system-time"));
		StreamSource 	ss = new StreamSource().setAlias("my-stream").setAddressing(addressing).setSqlQuery("select * from wrapper").setRawHistorySize("10  min").setInputStream(is);
	}
	@Test
	public void testBadWindowSize() throws GSNRuntimeException{
		ArrayList<AddressBean> addressing = new ArrayList<AddressBean>();
		addressing.add(new AddressBean("system-time"));
		StreamSource ss = new StreamSource().setAlias("my-stream").setAddressing(addressing).setSqlQuery("select * from wrapper").setRawHistorySize("10  sec");
		assertFalse(ss.validate());
	}
	@Test
	public void testUID() {
		InputStream is = new InputStream();
		ArrayList<AddressBean> addressing = new ArrayList<AddressBean>();
		addressing.add(new AddressBean("system-time"));
		StreamSource ss = new StreamSource().setAlias("my-stream").setAddressing(addressing).setSqlQuery("select * from wrapper").setRawHistorySize("10  s").setInputStream(is);
		assertTrue(ss.validate());
		assertNotNull(ss.getUIDStr());
		ss = new StreamSource().setAlias("my-stream").setAddressing(addressing).setSqlQuery("select * from wrapper").setRawHistorySize("10min");
		assertFalse(ss.validate());
		assertNull(ss.getUIDStr());
	}
	@Test
	public void testRateZeroQueries() throws SQLException{
		InputStream is = new InputStream();
		ArrayList<AddressBean> addressing = new ArrayList<AddressBean>();
		addressing.add(new AddressBean("system-time"));
		StreamSource ss = new StreamSource().setAlias("my-stream").setAddressing(addressing).setSqlQuery("select * from wrapper").setRawHistorySize("10  m").setInputStream(is);
		ss.setSamplingRate(0);
		ss.setWrapper(wrapper);
		assertTrue(ss.validate());
		String query = ss.toSql().toString();
		assertTrue(query.toLowerCase().indexOf("mod")<0);
		assertTrue(query.toLowerCase().indexOf("false")>0);
		sm.insertData(ss.getWrapper().getDBAliasInStr(), new StreamElement(new DataField[] {},new Serializable[] {},System.currentTimeMillis()/2) );
		DataEnumerator dm = sm.executeQuery(query, true);
		assertFalse(dm.hasMoreElements());
		sm.insertData(ss.getWrapper().getDBAliasInStr(), new StreamElement(new DataField[] {},new Serializable[] {},System.currentTimeMillis()) );
		dm = sm.executeQuery(query, true);
		assertFalse(dm.hasMoreElements());
		wrapper.removeListener(ss);
	}

	@Test(expected=GSNRuntimeException.class)
	public void badSamplingRate() {
		StreamSource 	ss = new StreamSource().setAlias("my-stream").setSqlQuery("select * from wrapper").setRawHistorySize("10  s");
		ss.setSamplingRate(-0.1f);
	}
	@Test(expected=GSNRuntimeException.class)
	public void badSamplingRateBadOrder() {
		StreamSource ss = new StreamSource().setAlias("my-stream").setSqlQuery("select * from wrapper").setRawHistorySize("10  s");
		ss.setWrapper(wrapper);
		ss.setSamplingRate(0.2f);
	}
	@Test
	public void testCountWindowSizeZero() {
		InputStream is = new InputStream();
		ArrayList<AddressBean> addressing = new ArrayList<AddressBean>();
		addressing.add(new AddressBean("system-time"));
		StreamSource 	ss = new StreamSource().setAlias("my-stream").setAddressing(addressing).setSqlQuery("select * from wrapper").setRawHistorySize("0 ").setInputStream(is);

		ss.setWrapper(wrapper);
		assertTrue(ss.validate());
		assertTrue(ss.toSql().toString().toLowerCase().indexOf("false")>0);
		wrapper.removeListener(ss);
	}

	@Test(expected=GSNRuntimeException.class)
	public void testNullWrapper() {
		StreamSource ss = new StreamSource().setAlias("my-stream").setSqlQuery("select * from wrapper").setRawHistorySize("10  s");
		ss.toSql();
	}

	@Test(expected=GSNRuntimeException.class)
	public void testInvalidStreamSource() {
		InputStream is = new InputStream();
		StreamSource ss = new StreamSource().setAlias("my-stream").setSqlQuery("select * from wrapper").setRawHistorySize("10  s").setInputStream(is);
		ss.setWrapper(wrapper);
		ss.toSql();
		wrapper.removeListener(ss);
	}

	@Test
	public void testTimeBasedWindow() throws SQLException{
		InputStream is = new InputStream();
		ArrayList<AddressBean> addressing = new ArrayList<AddressBean>();
		addressing.add(new AddressBean("system-time"));
		StreamSource ss = new StreamSource().setAlias("my-stream").setAddressing(addressing).setSqlQuery("select * from wrapper").setRawHistorySize("1  s").setInputStream(is);
		ss.setSamplingRate(1);
		ss.setWrapper(wrapper );
		assertTrue(ss.validate());
		String query = ss.toSql().toString();
		assertTrue(query.toLowerCase().indexOf("mod")<0);
		assertTrue(sm.insertData(ss.getWrapper().getDBAliasInStr(), new StreamElement(new DataField[] {},new Serializable[] {},System.currentTimeMillis()/2) ));
		DataEnumerator dm = sm.executeQuery(query, true);
		assertFalse(dm.hasMoreElements());
		assertTrue(sm.insertData(ss.getWrapper().getDBAliasInStr(), new StreamElement(new DataField[] {},new Serializable[] {},System.currentTimeMillis())) );
		ResultSet rs =StorageManager.getInstance().executeQueryWithResultSet(query);
		assertTrue(rs.next());
		assertFalse(rs.next());
		dm = sm.executeQuery(query, true);
		assertTrue(dm.hasMoreElements());
		dm.nextElement();
		assertFalse(dm.hasMoreElements());
		sm.insertData(ss.getWrapper().getDBAliasInStr(), new StreamElement(new DataField[] {},new Serializable[] {},System.currentTimeMillis()) );
		dm = sm.executeQuery(query, true);
		assertTrue(dm.hasMoreElements());
		dm.nextElement();
		assertTrue(dm.hasMoreElements());
		dm.nextElement();
		assertFalse(dm.hasMoreElements());
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		dm = sm.executeQuery(query, true);
		assertFalse(dm.hasMoreElements());
		sm.insertData(ss.getWrapper().getDBAliasInStr(), new StreamElement(new DataField[] {},new Serializable[] {},System.currentTimeMillis()) );
		dm = sm.executeQuery(query, true);
		assertTrue(dm.hasMoreElements());
		dm.nextElement();
		assertFalse(dm.hasMoreElements());
		wrapper.removeListener(ss);
	}
	@Test
	public void testCountBasedWindowSize1() throws SQLException{
		InputStream is = new InputStream();
		ArrayList<AddressBean> addressing = new ArrayList<AddressBean>();
		addressing.add(new AddressBean("system-time"));
		StreamSource 	ss = new StreamSource().setAlias("my-stream").setAddressing(addressing).setSqlQuery("select * from wrapper").setRawHistorySize("1").setInputStream(is);
		ss.setSamplingRate(1);
		assertTrue(ss.validate());
		ss.setWrapper(wrapper );
		String query = ss.toSql().toString();
		assertTrue(query.toLowerCase().indexOf("mod")<0);
		sm.insertData(ss.getWrapper().getDBAliasInStr(), new StreamElement(new DataField[] {},new Serializable[] {},System.currentTimeMillis()) );
		DataEnumerator dm = sm.executeQuery(query, true);
		assertTrue(dm.hasMoreElements());
		sm.insertData(ss.getWrapper().getDBAliasInStr(), new StreamElement(new DataField[] {},new Serializable[] {},System.currentTimeMillis()) );
		dm = sm.executeQuery(query, true);
		assertTrue(dm.hasMoreElements());
		dm.nextElement();
		assertFalse(dm.hasMoreElements());
		long timed = System.currentTimeMillis()+100;
		sm.insertData(ss.getWrapper().getDBAliasInStr(), new StreamElement(new DataField[] {},new Serializable[] {},timed) );
		dm = sm.executeQuery(query, true);
		assertTrue(dm.hasMoreElements());
		assertEquals(dm.nextElement().getTimeStamp(), timed);
		assertFalse(dm.hasMoreElements());
		wrapper.removeListener(ss);
	}

	@Test
	public void testCountBasedWindowSize2() throws SQLException{
		InputStream is = new InputStream();
		ArrayList<AddressBean> addressing = new ArrayList<AddressBean>();
		addressing.add(new AddressBean("system-time"));
		StreamSource 	ss = new StreamSource().setAlias("my-stream").setAddressing(addressing).setSqlQuery("select * from wrapper").setRawHistorySize("2").setInputStream(is);		
		ss.setSamplingRate(1);
		ss.setWrapper(wrapper );
		assertTrue(ss.validate());
		String query = ss.toSql().toString();
		assertTrue(query.toLowerCase().indexOf("mod")<0);
		sm.insertData(ss.getWrapper().getDBAliasInStr(), new StreamElement(new DataField[] {},new Serializable[] {},System.currentTimeMillis()) );
		long time1 = System.currentTimeMillis()+10;
		sm.insertData(ss.getWrapper().getDBAliasInStr(), new StreamElement(new DataField[] {},new Serializable[] {},time1) );
		long time2 = System.currentTimeMillis()+100;
		sm.insertData(ss.getWrapper().getDBAliasInStr(), new StreamElement(new DataField[] {},new Serializable[] {},time2) );
		DataEnumerator dm = sm.executeQuery(query, true);
		ResultSet rs =StorageManager.getInstance().executeQueryWithResultSet(query);
		assertTrue(rs.next());
		assertTrue(rs.next());
		assertFalse(rs.next());
		assertTrue(dm.hasMoreElements());
		assertEquals(dm.nextElement().getTimeStamp(), time2);
		assertTrue(dm.hasMoreElements());
		assertEquals(dm.nextElement().getTimeStamp(), time1);
		assertFalse(dm.hasMoreElements());
		wrapper.removeListener(ss);
	}

	/**
	 * This method is only used for testing purposes.
	 * @param query
	 */
	public static void printTable(String query) {
		System.out.println("Printing for Query : "+query);
		DataEnumerator dm = StorageManager.getInstance().executeQuery(query, true); 
		while (dm.hasMoreElements()) {
			StreamElement se = dm.nextElement();
			for (int i=0;i<se.getData().length;i++) {
				System.out.print(se.getFieldNames()[i]+"="+se.getData()[i]+" , ");
			}
			System.out.println("TimeStamp="+se.getTimeStamp());
		}
	}
}