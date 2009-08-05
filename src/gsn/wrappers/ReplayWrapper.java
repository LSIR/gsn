package gsn.wrappers;
import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.beans.WrapperConfig;
import gsn.channels.DataChannel;
import gsn.storage.DataEnumerator;
import gsn.storage.StorageManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

/**
 * This wrapper enables one to reply the existing stream from a database.
 * parameters: (dbname,speed [integer >=1, default is 1], for instance speed=2 means play 2 times faster).
 */
public class ReplayWrapper  implements Wrapper{
	private final WrapperConfig conf;

	private final DataChannel dataChannel;

	private  transient Logger        logger            = Logger.getLogger( this.getClass() );

	private DataField[] output;

	private String dbname;

	private int speed;

	private final Timer timer = new Timer(true);

	private DelayedDataEnumerator dt ;
	
	public void dispose ( ) {
		StorageManager.close(connection);
	}

	public DataField[] getOutputFormat() {
		return output;
	}
	Connection connection = null;

	public ReplayWrapper(WrapperConfig conf, DataChannel channel) throws SQLException {
		this.conf = conf;
		this.dataChannel= channel;
		dbname = conf.getParameters().getPredicateValueWithException("dbname");
		speed= conf.getParameters().getPredicateValueAsInt("speed", 1);

		logger.info("Initializing the ReplayWrapper with : "+dbname +". Loading the table structure ...");
		connection = StorageManager.getInstance().getConnection();
		output = StorageManager.tableToStructure(dbname,connection );

		dt= new DelayedDataEnumerator(dbname,speed);
	}

	public void start_publishing() {
		if (!dt.hasMoreElements())
			return;
		final ScheduledStreamElement item = dt.nextElement();
		final long delay = item.getExecutionTime();
		timer.schedule(new TimerTask() {
			public void run() {
				ReplayWrapper.this.dataChannel.write(item.getStreamElement());   
				start_publishing();
			}
		}, delay);
	}

	public void start() {
		timer.schedule(new TimerTask() {
			public void run() {
				start_publishing();
			}
		}, 1000);// 1000ms is the initial delay to have everything initialized.
	}

	public void stop() {
		timer.cancel();
	}

}


class DelayedDataEnumerator implements Enumeration<ScheduledStreamElement>{
	private int speed;
	private DataEnumerator data;
	private StreamElement previousElement =null;
	private  transient Logger        logger            = Logger.getLogger( this.getClass() );

	public DelayedDataEnumerator(String dbName, int speed) {
		this.speed = speed;
		StringBuilder query = new StringBuilder("select * from ").append(dbName).append(" order by TIMED asc");
		try {
			data = StorageManager.getInstance().executeQuery(query,false);

		}catch (SQLException e) {
			logger.error(e.getMessage(),e);
		}
	}

	public boolean hasMoreElements() {
		return data.hasMoreElements();
	}

	public ScheduledStreamElement nextElement() {
		StreamElement currentSe = data.nextElement();
		long delay = 500;// First time execution is delayed for 500ms.
		if (previousElement!=null) 
			delay =  (currentSe.getTimeStamp()-previousElement.getTimeStamp())/speed;
		previousElement = currentSe;
		return new ScheduledStreamElement(currentSe,delay);
	}

}

class ScheduledStreamElement {
	private StreamElement se;
	private long executionTime;
	public StreamElement getStreamElement() {
		return se;
	}
	public long getExecutionTime() {
		return executionTime;
	}
	public ScheduledStreamElement(StreamElement se, long executionTime) {
		this.se = se;
		this.executionTime = executionTime;
	}
}
