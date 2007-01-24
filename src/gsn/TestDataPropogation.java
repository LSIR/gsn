package gsn;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;
import static org.junit.Assert.*;
import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.beans.InputStream;
import gsn.beans.StreamElement;
import gsn.beans.StreamSource;
import gsn.storage.StorageManager;
import gsn.wrappers.AbstractWrapper;
import gsn.wrappers.MockWrapper;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.DriverManager;
import java.util.ArrayList;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestDataPropogation {
	private final DataField[] df = new DataField[] {new DataField("data","int","some sample data.")};
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		DriverManager.registerDriver( new org.hsqldb.jdbcDriver( ) );
		StorageManager.getInstance ( ).initialize ( "org.hsqldb.jdbcDriver","sa","" ,"jdbc:hsqldb:mem:." );
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	private StorageManager sm;
	private MockWrapper wrapper;
	private StreamSource streamSource;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		ArrayList<AddressBean> addressing;
		sm = StorageManager.getInstance();
		addressing = new ArrayList<AddressBean>();
		PropertiesConfiguration propertiesConfiguration = new PropertiesConfiguration();
		propertiesConfiguration.addProperty("wrapper.name", "mock-test");
		propertiesConfiguration.addProperty("wrapper.class", "gsn.wrappers.MockWrapper");
		Main.loadWrapperList(propertiesConfiguration);
		VSensorLoader loader = new VSensorLoader();
		AddressBean addressBean= new AddressBean("mock-test");
		addressing.add(addressBean);
		wrapper = (MockWrapper) loader.findWrapper(addressBean);
		wrapper.setOutputFormat(df);
		InputStream is = new InputStream();
		streamSource= createMock(StreamSource.class, new Method[] {StreamSource.class.getMethod("dataAvailable",new Class[] {})});
		streamSource.setAlias("test");
		streamSource.setRawHistorySize("1");
		streamSource.setAddressing(addressing);
		streamSource.setInputStream(is);
		streamSource.setSamplingRate(1);
		streamSource.setSqlQuery("select * from wrapper where data <> 1");
		assertTrue(loader.prepareStreamSource(streamSource,wrapper));
		assertNotNull(streamSource.toSql());
		assertNotNull(streamSource.getUIDStr());
		assertEquals(wrapper.getListeners().size(),1);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	 wrapper.removeListener(streamSource);
	 wrapper.releaseResources();
	 Main.resetWrapperList();
	
	}

	/**
	 * Test method for {@link gsn.wrappers.AbstractWrapper#postStreamElement(gsn.beans.StreamElement)}.
	 * Testing to fix the duplication in the calls for the virtual sensor.
	 * We add one stream element which satisfies the query condition and we expect to receive true indicating
	 * that one of the clients should be notified.
	 * 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 */
	@Test
	public void testPostOneStreamElement() throws ClassNotFoundException, InstantiationException, IllegalAccessException, SecurityException, NoSuchMethodException {
		StreamElement se = new StreamElement(df,new Serializable[] {10},System.currentTimeMillis());
		expect(streamSource.dataAvailable()).andStubReturn(true);
		replay(streamSource);
		assertTrue(streamSource.validate());
		assertTrue(wrapper.updateWrapperTable(se));
		assertEquals(sm.executeUpdate(new StringBuilder("delete from "+wrapper.getDBAliasInStr()+ " where TIMED="+se.getTimeStamp())),1);
		assertTrue(wrapper.publishStreamElement(se));
		verify(streamSource);
	}
	/**
	 * Test method for {@link gsn.wrappers.AbstractWrapper#postStreamElement(gsn.beans.StreamElement)}.
	 * Testing to fix the duplication in the calls for the virtual sensor.
	 * We add two stream element which are satisfying the query condition and we expect to receive (two) true indicating
	 * that one of the clients should be notified.
	 * 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 */
	@Test
	public void testPostTwoStreamElements() throws ClassNotFoundException, InstantiationException, IllegalAccessException, SecurityException, NoSuchMethodException {
		StreamElement se1 = new StreamElement(df,new Serializable[] {9},System.currentTimeMillis());
		StreamElement se2 = new StreamElement(df,new Serializable[] {10},System.currentTimeMillis()+10);
		expect(streamSource.dataAvailable()).andReturn(true).times(2);
		replay(streamSource);
		assertTrue(streamSource.validate());
		assertTrue(wrapper.publishStreamElement(se1));
		assertTrue(wrapper.publishStreamElement(se2));
		verify(streamSource);
	}
	/**
	 * Test method for {@link gsn.wrappers.AbstractWrapper#postStreamElement(gsn.beans.StreamElement)}.
	 * Testing to fix the duplication in the calls for the virtual sensor.
	 * We add two stream element which are satisfying the query condition and we expect to receive (two) true indicating
	 * that one of the clients should be notified.
	 * 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 */
	@Test
	public void testPostTwoStreamElementsDropOne() throws ClassNotFoundException, InstantiationException, IllegalAccessException, SecurityException, NoSuchMethodException {
		StreamElement se1 = new StreamElement(df,new Serializable[] {9},System.currentTimeMillis());
		StreamElement se2 = new StreamElement(df,new Serializable[] {10},System.currentTimeMillis()+10);
		StreamElement se3 = new StreamElement(df,new Serializable[] {1},System.currentTimeMillis()+11);
		expect(streamSource.dataAvailable()).andReturn(true).times(2);
		replay(streamSource);
		assertTrue(streamSource.validate());
		assertTrue(wrapper.publishStreamElement(se1));
		assertTrue(wrapper.publishStreamElement(se2));
		assertFalse(wrapper.publishStreamElement(se3));
		assertTrue(streamSource.toSql().toString().toLowerCase().indexOf("mod")<0);
		verify(streamSource);
	}
	
	
}
