package gsn.utils;

import static org.junit.Assert.*;
import gsn.beans.DataField;
import gsn.storage.StorageManager;

import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestValidityTools {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		DriverManager.registerDriver( new org.hsqldb.jdbcDriver( ) );
		StorageManager.getInstance ( ).initialize ( "org.hsqldb.jdbcDriver","sa","" ,"jdbc:hsqldb:mem:." );
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testIsAccessibleSocketStringInt() {

	}

	@Test
	public void testIsAccessibleSocketStringIntInt() {

	}

	@Test
	public void testCheckAccessibilityOfDirs() {

	}

	@Test
	public void testCheckAccessibilityOfFiles() {

	}

	@Test
	public void testIsDBAccessible() {

	}

	@Test
	public void testGetHostName() {

	}

	@Test
	public void testGetPortNumber() {

	}

	@Test
	public void testIsLocalhost() {
	
	}

	@Test
	public void testIsInt() {

	}

	@Test (expected=GSNRuntimeException.class)
	public void testTableExists() throws SQLException{
		assertFalse(ValidityTools.tableExists("myTable"));
		StorageManager.getInstance().createTable("table1",new DataField[]{});
		assertTrue(ValidityTools.tableExists("table1"));
		StorageManager.getInstance().dropTable("table1");
		assertFalse(ValidityTools.tableExists("table1"));
		assertFalse(ValidityTools.tableExists(""));
		assertFalse(ValidityTools.tableExists(null));
	}
	@Test (expected=GSNRuntimeException.class)
	public void testTableExistsWithEmptyTableName() throws SQLException{
		assertFalse(ValidityTools.tableExists(""));
	}
	@Test (expected=GSNRuntimeException.class)
	public void testTableExistsWithBadParameters() throws SQLException{
		assertFalse(ValidityTools.tableExists("'f\\"));
	}
}
