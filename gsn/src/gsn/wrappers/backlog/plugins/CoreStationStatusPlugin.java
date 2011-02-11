package gsn.wrappers.backlog.plugins;

import java.io.Serializable;
import java.util.Hashtable;

import org.apache.log4j.Logger;

import gsn.beans.DataField;
import gsn.wrappers.BackLogWrapper;


/**
 * This plugin reads incoming CoreStationStatus messages and interpretes them properly.
 * <p>
 * Any new status information coming directly from the CoreStation should be implemented
 * in this class.
 * 
 * @author Tonio Gsell
 */
public class CoreStationStatusPlugin extends AbstractPlugin {
	private static final String STATUS_DATA_TYPE = "status-data-type";

	private static final String SW_NAMING = "software";
	private static final String HW_NAMING = "hardware";
	
	private static DataField[] hwDataField = {
			new DataField("TIMESTAMP", "BIGINT"),
			new DataField("GENERATION_TIME", "BIGINT"),
			new DataField("DEVICE_ID", "INTEGER"),
			
			new DataField("V_EXT2", "INTEGER"),
			new DataField("V_EXT1", "INTEGER"),
			new DataField("V_EXT3", "INTEGER"),
			new DataField("I_V12DC_EXT", "INTEGER"),
			new DataField("V12DC_IN", "INTEGER"),
			new DataField("I_V12DC_IN", "INTEGER"),
			new DataField("VCC_5_0", "INTEGER"),
			new DataField("VCC_NODE", "INTEGER"),
			new DataField("I_VCC_NODE", "INTEGER"),
			new DataField("VCC_4_2", "INTEGER"),
			new DataField("LM92_TEMP", "INTEGER")};

	private static DataField[] swDataField = {
			new DataField("TIMESTAMP", "BIGINT"),
			new DataField("GENERATION_TIME", "BIGINT"),
			new DataField("DEVICE_ID", "INTEGER"),
			
			new DataField("USERS", "SMALLINT"),

			new DataField("LASTLOG", "VARCHAR(255)"),
             
			new DataField("CHRONY_STRATUM", "INTEGER"),
			new DataField("CHRONY_SYSTIME_ERROR", "DOUBLE"),
			new DataField("CHRONY_FREQUENCY", "DOUBLE"),
			new DataField("CHRONY_SKEW", "DOUBLE"),
			new DataField("CHRONY_RTC_ERROR", "DOUBLE"),
			new DataField("CHRONY_RTC_GAIN_RATE", "DOUBLE"),

			new DataField("STATVFS_ROOT_SIZE", "BIGINT"),
			new DataField("STATVFS_ROOT_FREE", "BIGINT"),
			new DataField("STATVFS_ROOT_INODES_SIZE", "BIGINT"),
			new DataField("STATVFS_ROOT_INODES_FREE", "BIGINT"),
			new DataField("STATVFS_VOL_SIZE", "BIGINT"),
			new DataField("STATVFS_VOL_FREE", "BIGINT"),
			new DataField("STATVFS_VOL_INODES_SIZE", "BIGINT"),
			new DataField("STATVFS_VOL_INODES_FREE", "BIGINT"),
			new DataField("STATVFS_CARD_SIZE", "BIGINT"),
			new DataField("STATVFS_CARD_FREE", "BIGINT"),
			new DataField("STATVFS_CARD_INODES_SIZE", "BIGINT"),
			new DataField("STATVFS_CARD_INODES_FREE", "BIGINT"),

			new DataField("DISKSTATS_READS_ISSUED", "BIGINT"),
			new DataField("DISKSTATS_SECTORS_READ", "BIGINT"),
			new DataField("DISKSTATS_SPENT_READING", "BIGINT"),
			new DataField("DISKSTATS_WRITES_COMPLETED", "BIGINT"),
			new DataField("DISKSTATS_SECTORS_WRITTEN", "BIGINT"),
			new DataField("DISKSTATS_SPENT_WRITING", "BIGINT"),
			new DataField("DISKSTATS_IO_IN_PROGRESS", "INTEGER"),
			new DataField("DISKSTATS_SPENT_DOING_IO", "BIGINT"),
			new DataField("DISKSTATS_WEIGHTED_DOING_IO", "BIGINT"),

			new DataField("INTERRUPT_OHCI_HCD_USB1", "BIGINT"),
			new DataField("INTERRUPT_PXA2XX_SPI2", "BIGINT"),
			new DataField("INTERRUPT_PXA_I2C_I2C0", "BIGINT"),
			new DataField("INTERRUPT_STUART", "BIGINT"),
			new DataField("INTERRUPT_BTUART", "BIGINT"),
			new DataField("INTERRUPT_FFUART", "BIGINT"),
			new DataField("INTERRUPT_PXA2XX_MCI", "BIGINT"),
			new DataField("INTERRUPT_DMA", "BIGINT"),
			new DataField("INTERRUPT_OST0", "BIGINT"),

			new DataField("LOADAVG_1_MIN", "DOUBLE"),
			new DataField("LOADAVG_5_MIN", "DOUBLE"),
			new DataField("LOADAVG_15_MIN", "DOUBLE"),
			new DataField("LOADAVG_RUNNABLE_PROCS", "INTEGER"),

			new DataField("MEMINFO_TOTAL", "INTEGER"),
			new DataField("MEMINFO_FREE", "INTEGER"),
			new DataField("MEMINFO_BUFFERS", "INTEGER"),
			new DataField("MEMINFO_CACHED", "INTEGER"),
			new DataField("MEMINFO_SHARED", "INTEGER"),
			new DataField("MEMINFO_KERNEL_STACK", "INTEGER"),
			new DataField("MEMINFO_SLAB", "INTEGER"),
			new DataField("MEMINFO_MAPPED", "INTEGER"),

			new DataField("SCHEDSTAT_YIELD_CALLED", "INTEGER"),
			new DataField("SCHEDSTAT_TASK_RUN_TIME", "BIGINT"),
			new DataField("SCHEDSTAT_TASK_WAIT_TIME", "BIGINT"),
			new DataField("SCHEDSTAT_TASKS_GIVEN", "BIGINT"),

			new DataField("INTERFACES", "SMALLINT"),
			new DataField("ETH0_IN", "BIGINT"),
			new DataField("ETH0_OUT", "BIGINT"),
			new DataField("PPP0_IN", "BIGINT"),
			new DataField("PPP0_OUT", "BIGINT"),
			new DataField("WLAN0_IN", "BIGINT"),
			new DataField("WLAN0_OUT", "BIGINT"),

			new DataField("IP_IN_RECEIVES", "BIGINT"),
			new DataField("IP_IN_HDR_ERRORS", "INTEGER"),
			new DataField("IP_IN_ADDR_ERRORS", "INTEGER"),
			new DataField("IP_IN_UNKNOWN_PROTOS", "INTEGER"),
			new DataField("IP_IN_DISCARDS", "INTEGER"),
			new DataField("IP_IN_DELIVERS", "BIGINT"),
			new DataField("IP_OUT_REQUESTS", "BIGINT"),
			new DataField("IP_OUT_DISCARDS", "INTEGER"),
			new DataField("IP_OUT_NO_ROUTES", "INTEGER"),

			new DataField("ICMP_IN_MESSAGES", "INTEGER"),
			new DataField("ICMP_IN_ERRORS", "INTEGER"),
			new DataField("ICMP_IN_DEST_UNREACHS", "INTEGER"),
			new DataField("ICMP_IN_ECHOS", "INTEGER"),
			new DataField("ICMP_IN_ECHOS_REPS", "INTEGER"),
			new DataField("ICMP_OUT_MESSAGES", "INTEGER"),
			new DataField("ICMP_OUT_ERRORS", "INTEGER"),
			new DataField("ICMP_OUT_DEST_UNREACHS", "INTEGER"),
			new DataField("ICMP_OUT_ECHOS", "INTEGER"),
			new DataField("ICMP_OUT_ECHOS_REPS", "INTEGER"),

			new DataField("TCP_ACTIVE_OPENS", "INTEGER"),
			new DataField("TCP_PASSIVE_OPENS", "INTEGER"),
			new DataField("TCP_ATTEMPT_FAILS", "INTEGER"),
			new DataField("TCP_ESTAB_RESETS", "INTEGER"),
			new DataField("TCP_CURR_ESTAB", "INTEGER"),
			new DataField("TCP_IN_SEGMENTS", "BIGINT"),
			new DataField("TCP_OUT_SEGMENTS", "BIGINT"),
			new DataField("TCP_RETRANS_SEGMENTS", "INTEGER"),
			new DataField("TCP_IN_ERRORS", "INTEGER"),
			new DataField("TCP_OUT_RSTS", "INTEGER"),

			new DataField("UDP_IN_DATAGRAMS", "BIGINT"),
			new DataField("UDP_NO_PORTS", "INTEGER"),
			new DataField("UDP_IN_ERRORS", "INTEGER"),
			new DataField("UDP_OUT_DATAGRAMS", "BIGINT"),
			new DataField("UDP_RCV_BUF_ERRORS", "INTEGER"),
			new DataField("UDP_SND_BUF_ERRORS", "INTEGER"),

			new DataField("TCP_LOSS", "INTEGER"),
			new DataField("TCP_LOSS_UNDO", "INTEGER"),
			new DataField("IP_IN_NO_ROUTES", "BIGINT"),
			new DataField("IP_IN_MCAST_OCTETS", "BIGINT"),
			new DataField("IP_OUT_MCAST_OCTETS", "BIGINT"),
			new DataField("IP_OUT_BCAST_PKTS", "BIGINT"),
			new DataField("IP_OUT_OCTETS", "BIGINT"),
			new DataField("IP_IN_OCTETS", "BIGINT"),
			new DataField("IP_IN_BCAST_OCTETS", "BIGINT"),
			new DataField("IP_IN_MCAST_PKTS", "BIGINT"),
			new DataField("IP_OUT_BCAST_OCTETS", "BIGINT"),
			new DataField("IP_IN_BCAST_PKTS", "BIGINT"),
			new DataField("IP_OUT_MCAST_PKTS", "BIGINT"),
			new DataField("IP_IN_TRUNCATED_PKTS", "BIGINT"),

			new DataField("SOCKSTAT_USED_SOCKETS", "INTEGER"),
			new DataField("SOCKSTAT_TCP_IN_USE", "INTEGER"),
			new DataField("SOCKSTAT_TCP_ORPHAN", "INTEGER"),
			new DataField("SOCKSTAT_TCP_TW", "INTEGER"),
			new DataField("SOCKSTAT_TCP_ALLOC", "INTEGER"),
			new DataField("SOCKSTAT_TCP_MEM", "INTEGER"),
			new DataField("SOCKSTAT_UDP_IN_USE", "INTEGER"),
			new DataField("SOCKSTAT_UPD_MEM", "INTEGER"),

			new DataField("SOFTIRQ_TIMER", "BIGINT"),
			new DataField("SOFTIRQ_TASKLET", "BIGINT"),
			new DataField("SOFTIRQ_HRTIMER", "BIGINT"),

			new DataField("STAT_PROCS_IN_USER_MODE", "BIGINT"),
			new DataField("STAT_NICED_IN_USER_MODE", "BIGINT"),
			new DataField("STAT_PROCS_IN_KERNEL_MODE", "BIGINT"),
			new DataField("STAT_PROCS_IDLE", "BIGINT"),
			new DataField("STAT_PROCS_IO_WAIT", "BIGINT"),
			new DataField("STAT_PROCS_SERVICING_IRQS", "BIGINT"),
			new DataField("STAT_PROCS_SERVICING_SOFTIRQS", "BIGINT"),
			new DataField("STAT_CONTEXT_SWITCHES", "BIGINT"),
			new DataField("STAT_PROCS_THREADS_CREATED", "INTEGER"),
			new DataField("STAT_PROCS_BLOCKED", "INTEGER"),

			new DataField("UPTIME", "DOUBLE"),
			new DataField("IDLETIME", "DOUBLE")};
	
	private static final Hashtable<String, NameDataFieldPair> statusNamingTable = new Hashtable<String, NameDataFieldPair>();
	static
	{
		statusNamingTable.put(HW_NAMING, new NameDataFieldPair(1, hwDataField));
		statusNamingTable.put(SW_NAMING, new NameDataFieldPair(2, swDataField));
	}

	private final transient Logger logger = Logger.getLogger( CoreStationStatusPlugin.class );

	private String statusDataType;
	
	
	@Override
	public boolean initialize ( BackLogWrapper backlogwrapper, String coreStationName, String deploymentName) {
		super.activeBackLogWrapper = backlogwrapper;
		try {
			statusDataType = getActiveAddressBean().getPredicateValueWithException(STATUS_DATA_TYPE).toLowerCase();
		} catch (Exception e) {
			logger.error(statusDataType);
			logger.error(e.getMessage());
			return false;
		}
		if (statusNamingTable.get(statusDataType) == null) {
			logger.error("wrong " + STATUS_DATA_TYPE + " predicate key specified in virtual sensor XML file! (" + STATUS_DATA_TYPE + "=" + statusDataType + ")");
			return false;
		}
		logger.info("using CoreStationStatus data type: " + statusDataType);
        
        registerListener();

        if (statusDataType.equalsIgnoreCase(HW_NAMING))
        	setName("CoreStationStatusPlugin-HW-" + coreStationName + "-Thread");
        else if (statusDataType.equalsIgnoreCase(SW_NAMING))
        	setName("CoreStationStatusPlugin-SW-" + coreStationName + "-Thread");
		
		return true;
	}

	@Override
	public byte getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.CORESTATION_STATUS_MESSAGE_TYPE;
	}

	@Override
	public DataField[] getOutputFormat() {
		return statusNamingTable.get(statusDataType).dataField;
	}


	@Override
	public String getPluginName() {
		return "CoreStationStatusPlugin";
	}

	@Override
	public boolean messageReceived(int deviceId, long timestamp, Serializable[] data) {
		Serializable[] header = {timestamp, timestamp, deviceId};
		
		try {
			short msgType = toShort(data[0]);
			
			if (msgType == statusNamingTable.get(statusDataType).typeNumber) {
				if (statusDataType.equalsIgnoreCase(HW_NAMING)) {
					data = checkAndCastData(data, 1, hwDataField, 3);
				}
				else if (statusDataType.equalsIgnoreCase(SW_NAMING)) {
					data = checkAndCastData(data, 1, swDataField, 3);
				}
				else {
					logger.warn("Wrong CoreStationStatus data type spedified.");
					return true;
				}
				
				if( dataProcessed(System.currentTimeMillis(), concat(header, data)) ) {
					ackMessage(timestamp, super.priority);
				} else {
					logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database.");
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		
		return true;
	}
}
