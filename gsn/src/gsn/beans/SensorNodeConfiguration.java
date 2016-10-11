package gsn.beans;

public class SensorNodeConfiguration {
	
	public Boolean info = null;		// from eventlogger packet
	public Boolean health = null;
	public Boolean adcmux = null;
	public Boolean adccomdiff = null;
	public Boolean dcx = null;
	public Boolean drift = null;
	public Boolean events = null;
	public Boolean rssi = null;
	public Boolean statecounter = null;
	public Boolean decagonmux = null;
	public Boolean powerswitch = null;	// from powerswitch packet
	public Boolean vaisala_wxt520 = null;
	public Boolean th3 = null;
	public Boolean enviroscan = null;
	public Boolean modbus = null;
	public Boolean wgps = null;
	public Boolean powerswitch_p1 = null;
	public Boolean powerswitch_p2 = null;
	
	public static final int QUERY_TYPE_OLD = 1;
	public static final int QUERY_TYPE_1 = 3;
	public static final int QUERY_TYPE_2 = 4;
	public Integer querytype = null;
	public Long timestamp;
	
	private Short nodetype = null;
	
	//data sources 1
	private final static int INDEX_INFO = 0;
	private final static int INDEX_HEALTH = 1;
	private final static int INDEX_ADCMUX = 2;
	private final static int INDEX_ADCCOMDIFF = 3;
	private final static int INDEX_DCX = 4;
	private final static int INDEX_DRIFT = 5;
	private final static int INDEX_EVENTS = 6;
	private final static int INDEX_RSSI = 7;
	private final static int INDEX_STATECOUNTER = 8;
	private final static int INDEX_DECAGONMUX = 9;
	private final static int INDEX_POWERSWITCH = 9;
	private final static int INDEX_VAISALA_WXT520 = 10;
	//data sources 2
	private final static int INDEX_TH3 = 0;
	private final static int INDEX_ENVIROSCAN = 1;
	@SuppressWarnings("unused")
	private final static int INDEX_UNUSED1 = 2;
	@SuppressWarnings("unused")
	private final static int INDEX_UNUSED2 = 3;
	private final static int INDEX_MODBUS = 4;
	private final static int INDEX_WGPS = 5;

	
	private final static int INDEX_POWERSWITCH_P1 = 0;
	private final static int INDEX_POWERSWITCH_P2 = 1;
	
	private final static int DATASOURCES2_FLAG = 0x800;
	
	/*public SensorNodeConfiguration() {
		timestamp = System.currentTimeMillis();
	}*/
	
	public SensorNodeConfiguration(SensorNodeConfiguration config, Short node_type) {
		if (node_type != null)
			nodetype = node_type;
		update(config.getConfiguration1());
		update((short)(config.getConfiguration2() + DATASOURCES2_FLAG));
		update(config.powerswitch_p1, config.powerswitch_p2);
		querytype = config.querytype;
		timestamp = config.timestamp;
	}

	public SensorNodeConfiguration(Short config, Short node_type, Long timestamp) {
		if (node_type != null)
			nodetype = node_type;
		update(config);
		this.timestamp = timestamp;
	}
	
	public SensorNodeConfiguration(Short config, Short node_type) {
		if (node_type != null)
			nodetype = node_type;
		update(config);
	}

	public SensorNodeConfiguration(Boolean p1, Boolean p2) {
		nodetype = SensorNode.POWERSWITCH_TN;
		update(p1, p2);
	}
	
	public SensorNodeConfiguration(Integer querytype) {
		this.querytype = querytype;
	}

	public void update(Short config, Short node_type) {
		if (node_type!=null)
			nodetype = node_type;
		update(config);
	}
	
	public void update(Short config) {
		if ((config & DATASOURCES2_FLAG) == 0) {
			info = (config & (1 << INDEX_INFO)) > 0;
			health = (config & (1 << INDEX_HEALTH)) > 0;
			drift = (config & (1 << INDEX_DRIFT)) > 0;
			events = (config & (1 << INDEX_EVENTS)) > 0;
			rssi = (config & (1 << INDEX_RSSI)) > 0;
			statecounter = (config & (1 << INDEX_STATECOUNTER)) > 0;
			if (getNodeType() == SensorNode.SIB_TINYNODE || getNodeType() == SensorNode.POWERSWITCH_TN) {
				adcmux = (config & (1 << INDEX_ADCMUX)) > 0;
				adccomdiff = (config & (1 << INDEX_ADCCOMDIFF)) > 0;
				dcx = (config & (1 << INDEX_DCX)) > 0;
				decagonmux = (config & (1 << INDEX_DECAGONMUX)) > 0;
				vaisala_wxt520 = (config & (1 << INDEX_VAISALA_WXT520)) > 0;
				powerswitch= (config & (1 << INDEX_POWERSWITCH)) > 0;
			}
			else if (getNodeType() == SensorNode.AE_TINYNODE)
				adccomdiff = (config & (1 << INDEX_ADCCOMDIFF)) > 0; // ae data
		}
		else {
			if (getNodeType() == SensorNode.SIB_TINYNODE) {
				th3 = (config & (1 << INDEX_TH3)) > 0;
				enviroscan = (config & (1 << INDEX_ENVIROSCAN)) > 0;
				modbus = (config & (1 << INDEX_MODBUS)) > 0;
			}
			else if (getNodeType() == SensorNode.WGPS_TINYNODE)
				wgps = (config & (1 << INDEX_WGPS)) > 0;
		}
		timestamp = System.currentTimeMillis();
	}
	
	public void update(Boolean p1, Boolean p2) {
		nodetype = SensorNode.POWERSWITCH_TN;
		powerswitch_p1 = p1;
		powerswitch_p2 = p2;
		timestamp = System.currentTimeMillis();
	}
	
	public boolean hasPortConfig() {
		return powerswitch_p1!=null && powerswitch_p2!=null;
	}
	
	public void removePortConfig() {
		powerswitch_p1=null;
		powerswitch_p2=null;
	}
	
	public boolean hasDataConfig1() {
		if (nodetype == SensorNode.DPP)
			return false;
		boolean hasconfig =
			info!=null && 
			health!=null &&
			drift!=null &&
			events!=null &&
			rssi!=null &&
			statecounter!=null;	
		switch (getNodeType()) {
		case SensorNode.SIB_TINYNODE:
			return hasconfig &&
			adcmux!=null &&
			adccomdiff!=null &&
			dcx!=null &&
			decagonmux!=null &&
			powerswitch!=null;
		case SensorNode.POWERSWITCH_TN:
			return hasconfig &&
			powerswitch!=null;
		case SensorNode.AE_TINYNODE:
			return hasconfig &&
			adccomdiff!=null;
		default:
			return hasconfig;
		}
	}
	
	public boolean hasDataConfig2() {
		if (nodetype == SensorNode.DPP)
			return false;
		return
			th3!=null && 
			enviroscan!=null &&
			modbus!=null &&
			wgps!=null;
	}
	
	public void removeDataConfig1() {
		info=null; 
		health=null;
		adcmux=null;
		adccomdiff=null;
		dcx=null;
		drift=null;
		events=null;
		rssi=null;
		statecounter=null;
		decagonmux=null;
		powerswitch=null;
		vaisala_wxt520=null;
	}
	
	public void removeDataConfig2() {
		th3=null; 
		enviroscan=null;
		modbus=null;
		wgps=null;
	}
	
	public Short getConfiguration1() {
		return (short) (
			(info == null || !info ? 0: 1 << INDEX_INFO) + 
			(health == null || !health ? 0: 1 << INDEX_HEALTH) +
			(adcmux == null || !adcmux ? 0: 1 << INDEX_ADCMUX) +
			(adccomdiff == null || !adccomdiff ? 0: 1 << INDEX_ADCCOMDIFF) +
			(dcx == null || !dcx ? 0: 1 << INDEX_DCX) +
			(drift == null || !drift ? 0: 1 << INDEX_DRIFT) +
			(events == null || !events ? 0: 1 << INDEX_EVENTS) +
			(rssi == null || !rssi ? 0: 1 << INDEX_RSSI) +
			(statecounter == null || !statecounter ? 0: 1 << INDEX_STATECOUNTER) +
			(decagonmux == null || !decagonmux ? 0: 1 << INDEX_DECAGONMUX) +
			(powerswitch == null || !powerswitch ? 0: 1 << INDEX_POWERSWITCH) +
			(vaisala_wxt520 == null || !vaisala_wxt520 ? 0: 1 << INDEX_VAISALA_WXT520)
		);
	}
	
	public Short getConfiguration2() {
		return (short) (
			(th3 == null || !th3 ? 0: 1 << INDEX_TH3) + 
			(enviroscan == null || !enviroscan ? 0: 1 << INDEX_ENVIROSCAN) + 
			(modbus == null || !modbus ? 0: 1 << INDEX_MODBUS) + 
			(wgps == null || !wgps ? 0: 1 << INDEX_WGPS)
		);
	}
	
	public Short getPortConfiguration() {
		return (short) (
			(powerswitch_p1 == null || !powerswitch_p1 ? 0: 1 << INDEX_POWERSWITCH_P1)+
			(powerswitch_p2 == null || !powerswitch_p2 ? 0: 1 << INDEX_POWERSWITCH_P2)
		);
	}
	
	public boolean isQuery() {
		return querytype != null;
	}
	
	private static boolean bothNullOrEqual(Object x, Object y) {
		  return ( x == null ? y == null : x.equals(y) );
	}


	@Override
	public boolean equals(Object o) {
		if (o instanceof SensorNodeConfiguration) {
			SensorNodeConfiguration sc = (SensorNodeConfiguration) o;
			return
				bothNullOrEqual(sc.info, this.info) &&
				bothNullOrEqual(sc.health, this.health) &&
				bothNullOrEqual(sc.adcmux, this.adcmux) &&
				bothNullOrEqual(sc.adccomdiff, this.adccomdiff) &&
				bothNullOrEqual(sc.dcx, this.dcx) &&
				bothNullOrEqual(sc.drift, this.drift) &&
				bothNullOrEqual(sc.events, this.events) &&
				bothNullOrEqual(sc.rssi, this.rssi) &&
				bothNullOrEqual(sc.statecounter, this.statecounter) &&
				bothNullOrEqual(sc.decagonmux, this.decagonmux) &&
				bothNullOrEqual(sc.powerswitch, this.powerswitch) &&
				bothNullOrEqual(sc.powerswitch_p1, this.powerswitch_p1) &&
				bothNullOrEqual(sc.powerswitch_p2, this.powerswitch_p2) &&
				bothNullOrEqual(sc.vaisala_wxt520, this.vaisala_wxt520) &&
				bothNullOrEqual(sc.th3, this.th3) &&
				bothNullOrEqual(sc.enviroscan, this.enviroscan) &&
				bothNullOrEqual(sc.modbus, this.modbus) &&
				bothNullOrEqual(sc.wgps, this.wgps) &&
				bothNullOrEqual(sc.querytype, this.querytype);
		}
		else
			return false;
	}
	
	@Override
	public String toString() {
		return "info: "+info+
			"\nhealth: " +health+
			"\nadcmux: "+adcmux+
			"\nadccomdiff: "+adccomdiff+
			"\ndcx: "+dcx+
			"\ndrift: "+drift+
			"\nevents: "+events+
			"\nrssi: "+rssi+
			"\nstatecounter: "+statecounter+
			"\ndecagonmux: "+decagonmux+
			"\npowerswitch: "+powerswitch+
			"\npowerswitch_p1: "+powerswitch_p1+
			"\npowerswitch_p2: "+powerswitch_p2+
			"\nvaisala_wxt520: "+vaisala_wxt520+
			"\nth3: "+th3+
			"\nenviroscan: "+enviroscan+
			"\nmodbus: "+modbus+
			"\nwgps: "+wgps+
			"\nisquery: "+querytype+
			"\ntimestamp:"+timestamp;
	}
	
	private Short getNodeType() {
		return nodetype!=null?nodetype:SensorNode.NODE_TYPE_UNKNOWN;
	}

}
