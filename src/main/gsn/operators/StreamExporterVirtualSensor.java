package gsn.operators;

import gsn.beans.DataField;
import gsn.beans.Operator;
import gsn.beans.StreamElement;
import gsn.channels.DataChannel;
import gsn.storage.StorageManager;
import gsn.core.OperatorConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * This virtual sensor saves its input stream to any JDBC accessible source.
 */
public class StreamExporterVirtualSensor implements Operator {
	
	public void process ( String inputStreamName , List<StreamElement> data ) {
		for (StreamElement se: data)
			process(inputStreamName, se);
	}

  public DataField[] getStructure() {
    return new DataField[0];  //To change body of implemented methods use File | Settings | File Templates.
  }

  public void start() {}
	public void stop() {}


	public static final String            PARAM_USER    = "user" , PARAM_PASSWD = "password" , PARAM_URL = "url" , TABLE_NAME = "table",PARAM_DRIVER="driver";

	public static final String[] OBLIGATORY_PARAMS = new String[] {PARAM_USER,PARAM_URL,PARAM_DRIVER};

	private static final transient Logger logger        = Logger.getLogger( StreamExporterVirtualSensor.class );

	private Connection                    connection;

	private CharSequence table_name;

	private String password;

	private String user;
	
	private String url;

	private DataField[] output_structure;

	private DataChannel outputChannel;

	public StreamExporterVirtualSensor (OperatorConfig config,DataChannel outputChannel ) throws ClassNotFoundException, SQLException {
//		this.outputChannel = outputChannel;
//		output_structure = config.getOutputFormat();
//		table_name = config.getParameters().getPredicateValueWithException(  TABLE_NAME );
//		user = config.getParameters().getPredicateValueWithException( PARAM_USER);
//		password = config.getParameters().getPredicateValueWithException( PARAM_PASSWD);
//		url =config.getParameters().getPredicateValueWithException( PARAM_URL);
//		try {
//			Class.forName(config.getParameters().getPredicateValueWithException(PARAM_DRIVER));
//			connection = getConnection();
//			logger.debug( "jdbc connection established." );
//			if (!StorageManager.tableExists(table_name,output_structure , connection))
//				StorageManager.executeCreateTable(table_name, output_structure, false,connection);
//		}catch (GSNRuntimeException e) {
//			throw new RuntimeException("Initialization failed. There is a table called " + TABLE_NAME+ " Inside the database but the structure is not compatible with what GSN expects.");
//		}
	}

	public void process ( String inputStreamName , StreamElement streamElement) {
		StringBuilder query = StorageManager.getStatementInsert(table_name, output_structure);
		try {
			StorageManager.executeInsert(table_name ,output_structure,streamElement,getConnection() );
		} catch (SQLException e) {
			logger.error(e.getMessage(),e);
			logger.error("Insertion failed! ("+ query+")");
		}finally {
			outputChannel.write(streamElement );
		}
		
	}


	public Connection getConnection() throws SQLException {
		if (this.connection==null || this.connection.isClosed())
			this.connection=DriverManager.getConnection(url,user,password);
		return connection;
	}


	public void dispose ( ) {
	}

}
