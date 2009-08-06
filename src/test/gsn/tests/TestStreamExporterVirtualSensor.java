package gsn.tests;

import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.beans.VSFile;
import gsn.utils.Parameter;
import gsn.operators.StreamExporterVirtualSensor;
import gsn2.conf.OperatorConfig;
import gsn2.conf.Parameters;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

import static org.testng.Assert.*;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.AfterTest;

public class TestStreamExporterVirtualSensor {

	private final String  user = "sa" , passwd = "" , db = "." , url = "jdbc:hsqldb:mem:." , streamName = "aJUnitTestStream";

	private VSFile config;

	/*
	 * To run some of these tests, a mysql server must be running on localhost
	 * with the following configuration: a database 'gsntest' must exist. a user
	 * 'gsntest' with password 'gsntest' must exist and have all privileges on
	 * the 'gsntest' database. You can do it with the following: mysql -u -p root
	 * create user 'gsntest' IDENTIFIED BY 'gsntest'; create database gsntest ;
	 * grant ALL ON gsntest.* TO gsntest ; (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	public void setUp ( ) {
		config = new VSFile( );
		config.setName( "JUnitTestStreamExporterVS" );
		config.setFileName( "PlaceholderfileNameForJUNitTesting" );

	}

	public void tearDown ( ) {
		config = null;
		try {
			DriverManager.registerDriver( new org.h2.Driver() );
			Connection connection = DriverManager.getConnection( url , user , passwd );
			connection.createStatement( ).execute( "DROP TABLE IF EXISTS " + streamName );
		} catch ( SQLException e ) {
			e.printStackTrace( );
		}

	}

	/*
	 * Tries to connect to a (supposedly) existing mysql db on local host. See
	 * class comments for more info. Should succeed.
	 */
	public void testConnectToExistingMySQLDB ( ) throws ClassNotFoundException, SQLException {
		ArrayList < Parameter > params = new ArrayList < Parameter >( );
		params.add( new Parameter( StreamExporterVirtualSensor.PARAM_URL , url ) );
		params.add( new Parameter( StreamExporterVirtualSensor.PARAM_USER , user ) );
		params.add( new Parameter( StreamExporterVirtualSensor.PARAM_PASSWD , passwd )); 

		OperatorConfig pcConfig = new OperatorConfig();
		pcConfig.setParameters(new Parameters(params.toArray(new Parameter[]{})));
		
		StreamExporterVirtualSensor vs = new StreamExporterVirtualSensor( pcConfig,null);
		
	}

	/*
	 * Tries to log a line into a Mysql table. The test stream generates data for
	 * each possible data type.
	 */
	public void testLogStatementIntoMySQLDB ( ) throws ClassNotFoundException, SQLException {
		ArrayList < Parameter> params = new ArrayList < Parameter >( );
		params.add( new Parameter( StreamExporterVirtualSensor.PARAM_URL , url ) );
		params.add( new Parameter( StreamExporterVirtualSensor.PARAM_USER , user ) );
		params.add( new Parameter( StreamExporterVirtualSensor.PARAM_PASSWD , passwd )); 

		OperatorConfig pcConfig = new OperatorConfig();
		pcConfig.setParameters(new Parameters(params.toArray(new Parameter[]{})));
		
		StreamExporterVirtualSensor vs = new StreamExporterVirtualSensor( pcConfig,null);
		
		// configure datastream
		Vector < DataField > fieldTypes = new Vector < DataField >( );
		Object [ ] data = null;

		for ( String type : DataTypes.TYPE_NAMES )
			fieldTypes.add( new DataField( type , type , type ) );
		int i = 0;
		for ( Object value : DataTypes.TYPE_SAMPLE_VALUES )
			data[ i++ ] = value;

		long timeStamp = new Date( ).getTime( );
		StreamElement streamElement = new StreamElement( fieldTypes.toArray( new DataField[] {} ) , ( Serializable [ ] ) data , timeStamp );

		// give datastream to vs
		vs.process( streamName , streamElement );

		// clean up and control
		boolean result = true;
		try {
			DriverManager.registerDriver( new com.mysql.jdbc.Driver( ) );
			Connection connection = DriverManager.getConnection( url , user , passwd );
			Statement statement = connection.createStatement( );
			statement.execute( "SELECT * FROM " + streamName );
			System.out.println( "result" + result );
			result = statement.getResultSet( ).last( );
			System.out.println( "result" + result );
		} catch ( SQLException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace( );
			result = false;
		}
		assertTrue( result );
	}

}
