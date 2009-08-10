package gsn.operators;

import gsn.beans.Operator;
import gsn.beans.StreamElement;
import gsn.beans.DataField;
import gsn.channels.DataChannel;
import gsn.storage.DataEnumerator;
import gsn.storage.StorageManager;
import gsn.core.OperatorConfig;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.swing.Timer;

import org.apache.log4j.Logger;

public class ClockedBridgeVirtualSensor  implements ActionListener , Operator {
	
	public void process ( String inputStreamName , List<StreamElement> data ) {
		for (StreamElement se: data)
			process(inputStreamName, se);
	}

  public DataField[] getStructure() {
    return new DataField[0];  //To change body of implemented methods use File | Settings | File Templates.
  }

  public void start() {}
	public void stop() {}


	private static final String RATE_PARAM       = "rate";
	private static final String TABLE_NAME_PARAM = "table_name";
	private static final String OUTPUT_TABLE_NAME_PARAM = "table_name";

	private Timer timer;
	private int clock_rate;
	private String table_name;
	private long last_updated;
	private String output_table_name;
	private DataChannel outputChannel;

	private static final transient Logger logger = Logger.getLogger( ClockedBridgeVirtualSensor.class );

	public ClockedBridgeVirtualSensor (OperatorConfig config ,DataChannel outputChannel ) throws SQLException {
		this.outputChannel = outputChannel;

		clock_rate = config.getParameters().getValueAsIntWithException(RATE_PARAM);
		table_name = config.getParameters().getValueWithException(TABLE_NAME_PARAM);
		output_table_name = config.getParameters().getValueWithException(OUTPUT_TABLE_NAME_PARAM);
		 
		timer = new Timer( clock_rate , this );

		timer.start( );

		last_updated = -1 ; // reading the whole table, this value can be overriden, if some tuples were already read 

		/*******************************************/
		// select latest update time of VS output table
		logger.warn("OUTPUT TABLE NAME: "+ output_table_name);

		StringBuilder query = new StringBuilder("select max(timed) from "+output_table_name);
		logger.warn("select max(timed) from "+output_table_name);

		Connection connection = null;


		connection = StorageManager.getInstance().getConnection();
		ResultSet rs = StorageManager.getInstance().executeQueryWithResultSet(query, connection);
		if (rs.next()) {
			Long i = rs.getLong(1); // get result from first column (1)
			logger.warn("LAST UPDATE: "+ Long.toString(i));
			last_updated = i;                          // override initial value -1
		} 

		StorageManager.close(connection);

	}

	public void process ( String inputStreamName , StreamElement data) {
		outputChannel.write( data );
		if ( logger.isDebugEnabled( ) ) logger.debug( "Data received under the name: " + inputStreamName );
	}

	public void dispose ( ) {
		timer.stop( );
	}

	public void actionPerformed ( ActionEvent actionEvent ) {

		// check if new data is available since last update then call dataProduced(StreamElement se)
		StringBuilder query = new StringBuilder("select * from "+table_name+" where timed > "+last_updated+" order by timed asc");

		try {
			DataEnumerator data = StorageManager.getInstance().executeQuery(query,true);
			while (data.hasMoreElements()){
				StreamElement se = data.nextElement();
				last_updated = se.getTimeInMillis();
				// TODO: BAD CALL, SHOULD BECOME A WRAPPER ! 
				//dataProduced(se);
			}
		} catch (SQLException e) {
			logger.error(e.getMessage(),e);
		}
	}
	
}

