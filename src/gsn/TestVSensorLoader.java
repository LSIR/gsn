package gsn;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.beans.InputStream;
import gsn.beans.StreamSource;
import gsn.beans.VSensorConfig;
import gsn.storage.StorageManager;
import gsn.utils.ValidityTools;
import gsn.wrappers.MockWrapper;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestVSensorLoader {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		DriverManager.registerDriver( new org.hsqldb.jdbcDriver( ) );
		StorageManager.getInstance ( ).initialize ( "org.hsqldb.jdbcDriver","sa","" ,"jdbc:hsqldb:mem:." );
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	ArrayList<AddressBean> addressing;
	AddressBean addressBean;
	
	@Before
	public void setUp() throws Exception {
		Main.resetWrapperList();
		PropertiesConfiguration propertiesConfiguration = new PropertiesConfiguration();
		propertiesConfiguration.addProperty("wrapper.name", "mock-test");
		propertiesConfiguration.addProperty("wrapper.class", "gsn.wrappers.MockWrapper");
		propertiesConfiguration.addProperty("wrapper.name", "system-time");
		propertiesConfiguration.addProperty("wrapper.class", "gsn.wrappers.SystemTime");
		Main.loadWrapperList(propertiesConfiguration);
		addressing = new ArrayList<AddressBean>();
		addressBean= new AddressBean("mock-test");
		addressing.add(addressBean);
		
	}

	@After
	public void tearDown() throws Exception {
		
	}

	@Test
	public void testCreateInputStreams() {

	}

	@Test
	public void testPrepareWrapper() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		VSensorLoader loader = new VSensorLoader();
		MockWrapper wrapper = (MockWrapper) loader.findWrapper(addressBean);
		assertNotNull(wrapper);
	}

	@Test
	public void testPrepareStreamSource() {

	}

	@Test
	public void testStopLoading() throws IOException {
		VSensorConfig  sensorConfig = new VSensorConfig();
		sensorConfig.setVirtualSensorName("test");
		File someFile = File.createTempFile("bla", ".xml");
		sensorConfig.setMainClass("gsn.vsensor.BridgeVirtualSensor");
		sensorConfig.setFileName(someFile.getAbsolutePath());
		VirtualSensorPool pool = new VirtualSensorPool(sensorConfig);
		InputStream is = new InputStream();
		is.setInputStreamName("t1");
		is.setQuery("select * from my-stream1");
		ArrayList<AddressBean> addressing = new ArrayList<AddressBean>();
		addressing.add(new AddressBean("mock-test"));
		StreamSource 	ss1 = new StreamSource().setAlias("my-stream1").setAddressing(addressing).setSqlQuery("select * from wrapper").setRawHistorySize("2").setInputStream(is);		
		ss1.setSamplingRate(1);
		assertTrue(ss1.validate());
		is.setSources(ss1);
		assertTrue(is.validate());
		sensorConfig.setInputStreams(is);
		assertTrue(sensorConfig.validate());
		
	}
	@Test
	public void testOneInputStreamUsingTwoStreamSources() throws InstantiationException, IllegalAccessException, SQLException {
		VSensorLoader loader = new VSensorLoader();
		InputStream is = new InputStream();
		ArrayList<AddressBean> addressing = new ArrayList<AddressBean>();
		addressing.add(new AddressBean("mock-test"));
		StreamSource 	ss1 = new StreamSource().setAlias("my-stream1").setAddressing(addressing).setSqlQuery("select * from wrapper").setRawHistorySize("2").setInputStream(is);		
		ss1.setSamplingRate(1);
		assertTrue(ss1.validate());
		assertTrue(loader.prepareStreamSource(is,ss1));
		StreamSource 	ss2 = new StreamSource().setAlias("my-stream2").setAddressing(addressing).setSqlQuery("select * from wrapper").setRawHistorySize("20").setInputStream(is);		
		ss2.setSamplingRate(1);
		assertTrue(ss2.validate());
		assertTrue(loader.prepareStreamSource(is,ss2));
//		assertTrue(StorageManager.getInstance().getTableSchema(ss1.getWrapper().getDBAliasInStr()));
		ss1.getWrapper().releaseResources();
		assertFalse(ValidityTools.tableExists(ss1.getWrapper().getDBAliasInStr()));
		
	}

}
