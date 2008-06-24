package ch.ethz.permafrozer;

import net.tinyos1x.message.Message;

public class DozerBaseStatusMsg extends DozerAbstractMsg
{
	/** The default size of this message type in bytes. */
    public static final int DEFAULT_MESSAGE_SIZE = 22;

    /** The Active Message type associated with this message. */
    public static final int AM_TYPE = 0x88;

    private Integer uptime = null;
    private Long boottime = null;
    private String boottime_date = null;
    private Integer temperature = null;
    private Double realtemperature_C = null;
    private Integer moisture = null;
    private Double approxmoisture_rel = null;
    private Double realmoisture_rel = null;
    
    /** Create a new LogMsg of size 38. */
    public DozerBaseStatusMsg() {
        super(DEFAULT_MESSAGE_SIZE);
        amTypeSet(AM_TYPE);
    }

    /** Create a new LogMsg of the given data_length. */
    public DozerBaseStatusMsg(int data_length) {
        super(data_length);
        amTypeSet(AM_TYPE);
    }
    /**
     * Create a new LogMsg with the given data_length
     * and base offset.
     */
    public DozerBaseStatusMsg(int data_length, int base_offset) {
        super(data_length, base_offset);
        amTypeSet(AM_TYPE);
    }

    /**
     * Create a new LogMsg using the given byte array
     * as backing store.
     */
    public DozerBaseStatusMsg(byte[] data) {
        super(data);
        amTypeSet(AM_TYPE);
    }

    /**
     * Create a new LogMsg using the given byte array
     * as backing store, with the given base offset.
     */
    public DozerBaseStatusMsg(byte[] data, int base_offset) {
        super(data, base_offset);
        amTypeSet(AM_TYPE);
    }

    /**
     * Create a new LogMsg using the given byte array
     * as backing store, with the given base offset and data length.
     */
    public DozerBaseStatusMsg(byte[] data, int base_offset, int data_length) {
        super(data, base_offset, data_length);
        amTypeSet(AM_TYPE);
    }

    /**
     * Create a new LogMsg embedded in the given message
     * at the given base offset.
     */
    public DozerBaseStatusMsg(Message msg, int base_offset) {
        super(msg, base_offset, DEFAULT_MESSAGE_SIZE);
        amTypeSet(AM_TYPE);
    }

    /**
     * Create a new LogMsg embedded in the given message
     * at the given base offset and length.
     */
    public DozerBaseStatusMsg(Message msg, int base_offset, int data_length) {
        super(msg, base_offset, data_length);
        amTypeSet(AM_TYPE);
    }
    
    public int getUptime()
    {
    	if (uptime == null)
    	{
    		uptime = new Integer((int) ((getUIntElement(72, 8) << 16) + (getUIntElement(56, 8) << 8) + getUIntElement(64, 8)));
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
    		// TODO bug on tos1 (its not LSB as far)
    		temperature = new Integer((int) ((getUIntElement(80, 8) << 8) + getUIntElement(88, 8)));
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
    		// TODO bug on tos1 (its not LSB as far)
    		moisture = new Integer((int) ((getUIntElement(96, 8) << 8) + getUIntElement(104, 8)));
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

    public long getTimeStamp()
    {
    	return super.getTimeStamp(112);
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

    	return "\t" + super.toString() + "\n\t\t uptime:" + getUptime() + "(" + getBootTime_date() + ")" +
    		" temperature:" + getTemperature() + "(" + temp + ") moisture:" + getMoisture() + "(" + hum + ")\n";
    }
}
