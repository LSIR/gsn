package ch.ethz.permafrozer;

import net.tinyos1x.message.Message;

public class DozerBaseStatusMsg extends DozerAbstractMsg
{
	/** The default size of this message type in bytes. */
    public static final int DEFAULT_MESSAGE_SIZE = 35;

    /** The Active Message type associated with this message. */
    public static final int AM_TYPE = 0x88;

    private Integer sampleno = null;
    private Integer uptime = null;
    private Long boottime = null;
    private String boottime_date = null;
    private Integer temperature = null;
    private Double realtemperature_C = null;
    private Integer moisture = null;
    private Double approxmoisture_rel = null;
    private Double realmoisture_rel = null;
    private Integer mspvoltage = null;
    private Integer msptemperature = null;
    private Integer queuesize = null;
    private Integer packetssent = null;
    private Integer packetsreceived = null;
    private Integer childcount = null;
    private Integer rssi = null;
        
    public DozerBaseStatusMsg() {
        super(DEFAULT_MESSAGE_SIZE);
        amTypeSet(AM_TYPE);
    }

    public DozerBaseStatusMsg(int data_length) {
        super(data_length);
        amTypeSet(AM_TYPE);
    }

    public DozerBaseStatusMsg(int data_length, int base_offset) {
        super(data_length, base_offset);
        amTypeSet(AM_TYPE);
    }

    public DozerBaseStatusMsg(byte[] data) {
        super(data);
        amTypeSet(AM_TYPE);
    }

    public DozerBaseStatusMsg(byte[] data, int base_offset) {
        super(data, base_offset);
        amTypeSet(AM_TYPE);
    }

    public DozerBaseStatusMsg(byte[] data, int base_offset, int data_length) {
        super(data, base_offset, data_length);
        amTypeSet(AM_TYPE);
    }

    public DozerBaseStatusMsg(Message msg, int base_offset) {
        super(msg, base_offset, DEFAULT_MESSAGE_SIZE);
        amTypeSet(AM_TYPE);
    }

    public DozerBaseStatusMsg(Message msg, int base_offset, int data_length) {
        super(msg, base_offset, data_length);
        amTypeSet(AM_TYPE);
    }

    public int getSampleNo()
    {
    	if (sampleno == null)
    	{
    		sampleno = new Integer((int) ((getUIntElement(7 * 8, 8) << 8) + getUIntElement(8 * 8, 8)));
    	}
    	
    	return sampleno;
    }
    
    public int getUptime()
    {
    	if (uptime == null)
    	{
    		uptime = new Integer((int) ((getUIntElement(11 * 8, 8) << 16) + (getUIntElement(9 * 8, 8) << 8) + getUIntElement(10 * 8, 8)));
    	}
    	
    	return uptime;
    }
    
    public long getBootTime()
    {
    	if (boottime == null)
    	{
    		boottime = new Long(getTimeStamp() - ((getUptime() + getATime()) * 1000L));
    	}
    	
    	return boottime;
    }
    
    public String getBootTime_date()
    {
    	if (boottime_date == null)
    	{
    		boottime_date = MsgFormatter.DF.format(getBootTime());
    	}
    	
    	return boottime_date;
    }
    
    public int getTemperature()
    {
    	if (temperature == null)
    	{
    		temperature = new Integer((int) ((getUIntElement(12 * 8, 8) << 8) + getUIntElement(13 * 8, 8)));
    	}
    	
    	return temperature;
    }
    
    public double getRealTemperature_C() throws NumberFormatException
    {
    	if (getTemperature() == 0xffff)
    		throw new NumberFormatException();

    	if (realtemperature_C == null)
    	{
    		realtemperature_C = new Double(-39.63d + (0.01d * getTemperature()));
    	}
    	return realtemperature_C;
    }
    
    public int getMoisture()
    {
    	if (moisture == null)
    	{
    		moisture = new Integer((int) ((getUIntElement(14 * 8, 8) << 8) + getUIntElement(15 * 8, 8)));
    	}
    	
    	return moisture;
    }

    public double getApproxMoisture_rel() throws NumberFormatException
    {
    	if (getMoisture() == 0xffff)
    		throw new NumberFormatException();

    	if (approxmoisture_rel == null)
    	{
    		approxmoisture_rel = new Double(-4 + (0.0405d * getMoisture()) - 0.0000028d * Math.pow(getMoisture(), 2));
    		
    		if (approxmoisture_rel > 99d)
    		{
    			approxmoisture_rel = new Double(100d);
    		}
    		else if (approxmoisture_rel < 0d)
    		{
    			approxmoisture_rel = new Double(0d);
    		}
    	}
    	
		return approxmoisture_rel;
    }
    
    public double getRealMoisture_rel() throws NumberFormatException
    {
    	if (getMoisture() == 0xffff)
    		throw new NumberFormatException();

    	if (realmoisture_rel == null)
    	{
    		realmoisture_rel = new Double(((getRealTemperature_C() - 25) * (0.01d + (0.00008d * getMoisture()))) + getApproxMoisture_rel());

    		if (realmoisture_rel > 99d)
    		{
    			realmoisture_rel = new Double(100d);
    		}
    		else if (realmoisture_rel < 0d)
    		{
    			realmoisture_rel = new Double(0d);
    		}
    	}
    	
    	return realmoisture_rel;
    }

    public int getMspVoltage()
    {
    	if (mspvoltage == null)
    	{
    		mspvoltage = new Integer((int) ((getUIntElement(16 * 8, 8) << 8) + getUIntElement(17 * 8, 8)));
    	}
    		
    	return mspvoltage;
    }

    public int getMspTemperature()
    {
    	if (msptemperature == null)
    	{
    		msptemperature = new Integer((int) ((getUIntElement(18 * 8, 8) << 8) + getUIntElement(19 * 8, 8)));
    	}
    		
    	return msptemperature;
    }

    public int getQueueSize()
    {
    	if (queuesize == null)
    	{
    		queuesize = new Integer((int) getUIntElement(20 * 8, 8));
    	}
    		
    	return queuesize;
    }
    
    public int getPacketsSent()
    {
    	if (packetssent == null)
    	{
    		packetssent = new Integer((int) ((getUIntElement(21 * 8, 8) << 8) + getUIntElement(22 * 8, 8)));
    	}
    		
    	return packetssent;
    }

    public int getPacketsReceived()
    {
    	if (packetsreceived == null)
    	{
    		packetsreceived = new Integer((int) ((getUIntElement(23 * 8, 8) << 8) + getUIntElement(24 * 8, 8)));
    	}
    		
    	return packetsreceived;
    }
    
    public int getchildcount()
    {
    	if (childcount == null)
    	{
    		childcount = new Integer((int) getUIntElement(25 * 8, 8));
    	}
    	
    	return childcount;
    }
    
    public int getrssi()
    {
    	if (rssi == null)
    	{
    		rssi = new Integer((int) getUIntElement(26 * 8, 8));
    	}
    	
    	return rssi;
    }
    
    public String toString()
    {
    	String temp = "invalid";
    	try
    	{
    		temp = MsgFormatter.DECIMAL_1FRAC.format(getRealTemperature_C()) + "°C";
    	}
    	catch (NumberFormatException e) { }
    	
    	String hum = "invalid";
    	try
    	{
    	    hum = MsgFormatter.DECIMAL_1FRAC.format(getRealMoisture_rel()) + "%";
    	}
    	catch (NumberFormatException e) { }

    	return "\t" + super.toString() + "\n\t\t sampleno:" + getSampleNo() + 
    		" uptime:" + getUptime() + "(" + getBootTime_date() + ")" +	
    		" temperature:" + getTemperature() + "(" + temp + ") moisture:" + getMoisture() + "(" + hum + ")" +
    		" mspvoltage:" + getMspVoltage() + " msptemperature:" + getMspTemperature() + " queuesize:" + getQueueSize() +
    		" packetssent:" + getPacketsSent() + " packetsreceived:" + getPacketsReceived() + " childcount:" + getchildcount()+ " rssi:" + getrssi()+ "\n";
    }
}
