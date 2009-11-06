package gsn.wrappers.backlog.plugins;

import java.io.Serializable;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import gsn.beans.DataField;
import gsn.wrappers.BackLogWrapper;


/**
 * This plugin offers the functionality to send commands to the Backlog Python program
 * on the deployment side. It also listens for incoming BackLogStatus messages.
 * <p>
 * Any new command pointed directly to the Backlog Python program should be implemented
 * in this class.
 * 
 * @author Tonio Gsell
 */
public class BackLogStatusPlugin extends AbstractPlugin {
	
	private DataField[] dataField = {	new DataField("TIMESTAMP", "BIGINT"), 
						new DataField("CONNECTED", "SMALLINT"),
						new DataField("ERROR_NUMBER", "INTEGER"),
						new DataField("BACKLOG_DB_ENTRIES", "INTEGER"),
						new DataField("BACKLOG_DB_SIZE_KB", "INTEGER"),
						new DataField("BACKUP_DB_ENTRIES", "INTEGER"),
						new DataField("BACKUP_DB_SIZE_KB", "INTEGER"),
						new DataField("COMMANDS_SENT", "INTEGER")};

	private final transient Logger logger = Logger.getLogger( BackLogStatusPlugin.class );
	
	private int commands_sent = 0;
	private Integer error_count = null;
	private Integer backlog_db_entries = null;
	private Integer backlog_db_size = null;
	private Integer backup_db_entries = null;
	private Integer backup_db_size = null;
	private Timer checkConnection = null;



	/**
	 * Check if the connection to the deployment still exists.
	 * 
	 * @author Tonio Gsell
	 */
	class CheckConnection extends TimerTask {
		public void run() {
			short connected = 0;
			if( isConnected() )
				connected = 1;
			Serializable[] data = {System.currentTimeMillis(), connected, error_count, backlog_db_entries, backlog_db_size, backup_db_entries, backup_db_size, commands_sent};
			dataProcessed(System.currentTimeMillis(), data);
		}
	}

	@Override
	public boolean initialize(BackLogWrapper backLogWrapper) {
		super.initialize(backLogWrapper);
		//checkConnection = new Timer("BackLogCommandTimer");
		//checkConnection.schedule( new CheckConnection(), 2000, 30000 );
        
		return true;
	}

	@Override
	public byte getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.BACKLOG_STATUS_MESSAGE_TYPE;
	}

	@Override
	public DataField[] getOutputFormat() {
		return dataField;
	}

	@Override
	public int packetReceived(long timestamp, byte[] packet) {
		if(packet.length >= 4)
			error_count = arr2int(packet, 0);
		if(packet.length >= 8)
			backlog_db_entries = arr2int(packet, 4);
		if(packet.length >= 12)
			backlog_db_size = arr2int(packet, 8);
		if(packet.length >= 16)
			backup_db_entries = arr2int(packet, 12);
		if(packet.length == 20)
			backup_db_size = arr2int(packet, 16);
		
		short connected = 0;
		if( isConnected() )
			connected = 1;

		Serializable[] data = {timestamp, connected, error_count, backlog_db_entries, backlog_db_size, backup_db_entries, backup_db_size, commands_sent};
		
		if( dataProcessed(System.currentTimeMillis(), data) )
			ackMessage(timestamp);
		else
			logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database.");
		return PACKET_PROCESSED;
	}

	
	
	/** 
	 * Implements the interpretation and packaging of the status
	 * commands.
	 * <p>
	 * The incoming commands, e.g. by the web input, arrive here and have
	 * to be packed into a byte array in such a way, that the Python Backlog
	 * program can unpack it.
	 */
	@Override
	public boolean sendToPlugin(String action, String[] paramNames, Object[] paramValues) {
		if( action.compareToIgnoreCase("backlog") == 0 ) {
			byte[] command = {0, 1};
			

			for (int i = 0 ; i < paramNames.length ; i++) {
				if ( paramNames[i].compareToIgnoreCase("resend_data") == 0 ) {
					command[0] = 1;
				}
				else if ( paramNames[i].compareToIgnoreCase("backup_data") == 0 ) {
					if( "no".compareToIgnoreCase((String)paramValues[i]) == 0 )
						command[1] = 0;
				}
			}
			if( sendRemote(command) ) {
				logger.debug("Upload command sent (resend data " + command[0] + " / backup data " + command[1] + ")");
			}
			else {
				logger.warn("Upload command (resend data " + command[0] + " / backup data " + command[1] + ") has not been sent!");
				return false;
			}
		}

		short connected = 0;
		if( isConnected() )
			connected = 1;
		Serializable[] data = {System.currentTimeMillis(), connected, error_count, backlog_db_entries, backlog_db_size, backup_db_entries, backup_db_size, commands_sent++};
		dataProcessed(System.currentTimeMillis(), data);
		return true;
	}

	@Override
	public void finalize() {
		checkConnection.cancel();
	}
	
	
	private static int arr2int (byte[] arr, int start) {
		int i = 0;
		int len = 4;
		int cnt = 0;
		byte[] tmp = new byte[len];
		for (i = start; i < (start + len); i++) {
			tmp[cnt] = arr[i];
			cnt++;
		}
		int accum = 0;
		i = 0;
		for ( int shiftBy = 0; shiftBy < 32; shiftBy += 8 ) {
			accum |= ( (int)( tmp[i] & 0xff ) ) << shiftBy;
			i++;
		}
		return accum;
	}

}
