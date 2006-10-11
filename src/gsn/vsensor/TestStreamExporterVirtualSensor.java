/**
 * 
 * @author Jerome Rousselot
 */
package gsn.vsensor;

import static org.junit.Assert.*;

import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.utils.KeyValueImp;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

import org.apache.commons.collections.KeyValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import junit.framework.TestCase;

/**
 * @author jerome
 *
 */
public class TestStreamExporterVirtualSensor extends TestCase {
	
	private final String user="gsntest", passwd="gsntest", db="gsntest", 
		url="jdbc:mysql://localhost:3306/gsntest", streamName="aJUnitTestStream";
	private HashMap hashMap;
	private VSensorConfig config;
	/*
	 * To run some of these tests, a mysql server must be running on localhost
	 * with the following configuration:
	 * a database 'gsntest' must exist.
	 * a user 'gsntest' with password 'gsntest' must exist and have all privileges
	 * on the 'gsntest' database.
	 * 
	 * You can do it with the following:
	 * mysql -u -p root
	 *  create user 'gsntest' IDENTIFIED BY 'gsntest';
	 *  create database gsntest ;
	 *  grant ALL ON gsntest.* TO gsntest ;
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Before public void setUp() {
		hashMap = new HashMap();
		hashMap.put(VirtualSensorPool.CONTAINER, new ContainerImpl());
		config = new VSensorConfig();
		config.setVirtualSensorName("JUnitTestStreamExporterVS");
		config.setFileName("PlaceholderfileNameForJUNitTesting");
		config.setAuthor("Jerome Rousselot");
		hashMap.put(VirtualSensorPool.VSENSORCONFIG, config);
			
	}
	
	@After public void tearDown() {
		hashMap = null;
		config = null;
		try {
			DriverManager.registerDriver(new com.mysql.jdbc.Driver());
			Connection connection = DriverManager.getConnection(url, user, passwd);
			connection.createStatement().execute("DROP TABLE IF EXISTS " + streamName);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	/*
	 * Tries to instantiate a VS without the required arguments.
	 * Should always fail.
	 */
	@Test public void testMissingAllEssentialParameters() {
		StreamExporterVirtualSensor vs = new StreamExporterVirtualSensor();	
		assertFalse(vs.initialize(hashMap));
	}

	/*
	 * Tries to connect to a (supposedly) existing mysql db on local host.
	 * See class comments for more info.
	 * Should succeed.
	 */
	@Test public void testConnectToExistingMySQLDB() {

		StreamExporterVirtualSensor vs = new StreamExporterVirtualSensor();
		ArrayList<KeyValue> params = new ArrayList<KeyValue>();
		params.add(new KeyValueImp(StreamExporterVirtualSensor.PARAM_URL, url));
		params.add(new KeyValueImp(StreamExporterVirtualSensor.PARAM_USER, user));
		params.add(new KeyValueImp(StreamExporterVirtualSensor.PARAM_PASSWD, passwd));
		config.setMainClassInitialParams(params);
		assertTrue(vs.initialize(hashMap));
	}
	
	/*
	 * Tries to log a line into a Mysql table.
	 * The test stream generates data for each possible data type.
	 */
	@Test public void testLogStatementIntoMySQLDB() {
		
		StreamExporterVirtualSensor vs = new StreamExporterVirtualSensor();
		
		ArrayList<KeyValue> params = new ArrayList<KeyValue>();
		params.add(new KeyValueImp(StreamExporterVirtualSensor.PARAM_URL, url));
		params.add(new KeyValueImp(StreamExporterVirtualSensor.PARAM_USER, user));
		params.add(new KeyValueImp(StreamExporterVirtualSensor.PARAM_PASSWD, passwd));
		config.setMainClassInitialParams(params);
		vs.initialize(hashMap);
		
		Vector<DataField> fieldTypes = new Vector<DataField>();
		Object[] data = null;
		
		for(String type: DataTypes.TYPE_NAMES) 
			fieldTypes.add(new DataField(type, type, type));
		int i = 0;
		for(Object value: DataTypes.TYPE_SAMPLE_VALUES)
			data[i++]=value;

		long timeStamp = new Date().getTime();
		StreamElement streamElement = new StreamElement((Collection<DataField>)fieldTypes, 
									(Serializable[]) data, timeStamp);
		
		vs.dataAvailable(streamName, streamElement);
		
		boolean result=true;
		try {
			DriverManager.registerDriver(new com.mysql.jdbc.Driver());
			Connection connection = DriverManager.getConnection(url, user, passwd);
			Statement statement = connection.createStatement();
			statement.execute("SELECT * FROM " + streamName);
			System.out.println("result"+result);
			result=statement.getResultSet().last();
			System.out.println("result"+result);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			result=false;
		}
		
		assertTrue(result);
		
	}

	
}
