package gsn.wrappers;

import gsn.beans.AddressBean;
import gsn.storage.StorageManager;
import gsn.utils.GSNRuntimeException;

import java.sql.DriverManager;
import java.util.ArrayList;

import javax.naming.OperationNotSupportedException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author ali
 *
 */
public class TestAbstractWrapper {

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
	private ArrayList<AddressBean> addressing;


	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		sm = StorageManager.getInstance();
		addressing = new ArrayList<AddressBean>();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link gsn.wrappers.AbstractWrapper#getTableSizeEnforce()}.
	 */
	@Test
	public void testGetTableSizeEnforce() {

	}

	/**
	 * Test method for {@link gsn.wrappers.AbstractWrapper#isActive()}.
	 */
	@Test
	public void testIsActive() {

	}

	/**
	 * Test method for {@link gsn.wrappers.AbstractWrapper#postStreamElement(java.io.Serializable[])}.
	 */
	@Test
	public void testPostStreamElementSerializableArray() {

	}


	/**
	 * Test method for {@link gsn.wrappers.AbstractWrapper#sendToWrapper(java.lang.Object)}.
	 * @throws OperationNotSupportedException 
	 */
	@Test (expected=OperationNotSupportedException.class)
	public void testSendToWrapper1() throws OperationNotSupportedException {
		SystemTime systemTimeWrapper = new SystemTime();
		systemTimeWrapper.sendToWrapper("bla");
	}
	/**
	 * Test method for {@link gsn.wrappers.AbstractWrapper#sendToWrapper(java.lang.Object)}.
	 * Test to see what is the behavior if the wrapper is disabled.
	 * @throws OperationNotSupportedException 
	 */
	@Test (expected=GSNRuntimeException.class)
	public void testSendToWrapper2() throws OperationNotSupportedException {
		SystemTime systemTimeWrapper = new SystemTime();
		systemTimeWrapper.releaseResources();
		systemTimeWrapper.sendToWrapper("bla");
	}

	/**
	 * Test method for {@link gsn.wrappers.AbstractWrapper#releaseResources()}.
	 */
	@Test
	public void testReleaseResources() {
	}

	@Test
	public void testCleaning() {
		
	}

}
