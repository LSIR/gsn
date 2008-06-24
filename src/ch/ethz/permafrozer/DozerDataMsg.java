package ch.ethz.permafrozer;

import net.tinyos1x.message.Message;

public class DozerDataMsg extends DozerAbstractMsg
{
	/** The default size of this message type in bytes. */
    public static final int DEFAULT_MESSAGE_SIZE = 38;

    /** The Active Message type associated with this message. */
    public static final int AM_TYPE = 0x80;

    private Integer sysvoltage = null;
    private Double realsysvoltage_V = null;
    private Integer sdivoltage = null;
    private Double realsdivoltage_V = null;
    private Integer temperature = null;
    private Double realtemperature_C = null;
    private Integer moisture = null;
    private Double approxmoisture_rel = null;
    private Double realmoisture_rel = null;
    
    /** Create a new LogMsg of size 38. */
    public DozerDataMsg() {
        super(DEFAULT_MESSAGE_SIZE);
        amTypeSet(AM_TYPE);
    }

    /** Create a new LogMsg of the given data_length. */
    public DozerDataMsg(int data_length) {
        super(data_length);
        amTypeSet(AM_TYPE);
    }
    /**
     * Create a new LogMsg with the given data_length
     * and base offset.
     */
    public DozerDataMsg(int data_length, int base_offset) {
        super(data_length, base_offset);
        amTypeSet(AM_TYPE);
    }

    /**
     * Create a new LogMsg using the given byte array
     * as backing store.
     */
    public DozerDataMsg(byte[] data) {
        super(data);
        amTypeSet(AM_TYPE);
    }

    /**
     * Create a new LogMsg using the given byte array
     * as backing store, with the given base offset.
     */
    public DozerDataMsg(byte[] data, int base_offset) {
        super(data, base_offset);
        amTypeSet(AM_TYPE);
    }

    /**
     * Create a new LogMsg using the given byte array
     * as backing store, with the given base offset and data length.
     */
    public DozerDataMsg(byte[] data, int base_offset, int data_length) {
        super(data, base_offset, data_length);
        amTypeSet(AM_TYPE);
    }

    /**
     * Create a new LogMsg embedded in the given message
     * at the given base offset.
     */
    public DozerDataMsg(Message msg, int base_offset) {
        super(msg, base_offset, DEFAULT_MESSAGE_SIZE);
        amTypeSet(AM_TYPE);
    }

    /**
     * Create a new LogMsg embedded in the given message
     * at the given base offset and length.
     */
    public DozerDataMsg(Message msg, int base_offset, int data_length) {
        super(msg, base_offset, data_length);
        amTypeSet(AM_TYPE);
    }
    
    public int getSysVoltage()
    {
    	if (sysvoltage == null)
    	{
    		sysvoltage = new Integer((int) ((getUIntElement(64, 8) << 8) + getUIntElement(56, 8)));
    	}
    		
    	return sysvoltage;
    }

    public double getRealSysVoltage_V() throws NumberFormatException
    {
    	if (getSysVoltage() == 0xffff)
    		throw new NumberFormatException();

    	if (realsysvoltage_V == null)
    	{
    		realsysvoltage_V = new Double(getSysVoltage() * (2.56d/65536d) * (39d/24d));
    	}
    	
    	return realsysvoltage_V; 
    }
    
    public int getSdiVoltage()
    {
    	if (sdivoltage == null)
    	{
    		sdivoltage = new Integer((int) ((getUIntElement(80, 8) << 8) + getUIntElement(72, 8)));
    	}
    	
    	return sdivoltage;
    }
    
    public double getRealSdiVoltage_V() throws NumberFormatException
    {
    	if (getSdiVoltage() == 0xffff)
    		throw new NumberFormatException();

    	if (realsdivoltage_V == null)
    	{
    		realsdivoltage_V = new Double(getSdiVoltage() * (2.56d/65536d) * (204d/24d));
    	}
    	
    	return realsdivoltage_V; 
    }
    
    public int getTemperature()
    {
    	if (temperature == null)
    	{
    		temperature = new Integer((int) ((getUIntElement(96, 8) << 8) + getUIntElement(88, 8)));
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
    		moisture = new Integer((int) ((getUIntElement(112, 8) << 8) + getUIntElement(104, 8)));
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
    	return super.getTimeStamp(120);
    }

    public String toString()
    {
    	String sys = "invalid";
    	try
    	{
    		sys = MsgFormatter.DECIMAL_2FRAC.format(getRealSysVoltage_V()) + "V";
    	}
    	catch (NumberFormatException e) { }
    	
    	String sdi = "invalid";
    	try
    	{
    		sdi = MsgFormatter.DECIMAL_2FRAC.format(getRealSdiVoltage_V()) + "V";
    	}
    	catch (NumberFormatException e) { }
    	
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
    	
    	return "\t" + super.toString() + "\n\t\t" +
    		" sysvoltage:" + getSysVoltage() + "(" + sys + ") sdivoltage:" + getSdiVoltage() + "(" + sdi + ")" +
    		" temperature:" + getTemperature() + "(" + temp + ") moisture:" + getMoisture() + "(" + hum + ")\n";
    }
}
