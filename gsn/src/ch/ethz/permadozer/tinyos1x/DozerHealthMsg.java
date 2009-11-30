package ch.ethz.permadozer.tinyos1x;

import net.tinyos1x.message.Message;

public class DozerHealthMsg extends DozerAbstractMsg
{
	/** The default size of this message type in bytes. */
    public static final int DEFAULT_MESSAGE_SIZE = 38;

    /** The Active Message type associated with this message. */
    public static final int AM_TYPE = 0x80;

    private Integer sampleno = null;
    private Integer uptime_low = null;
    private Short uptime_high = null;
    private Integer sysvoltage = null;
    private Integer sdivoltage = null;
    private Integer temperature = null;
    private Integer moisture = null;
    private Integer mspvoltage = null;
    private Integer msptemperature = null;
    private Integer flashstatus = null;
    private Short queuesize = null;
    private Integer parentid = null;
    private Short hopchildcount = null;
    
    public DozerHealthMsg() {
        super(DEFAULT_MESSAGE_SIZE);
        amTypeSet(AM_TYPE);
    }

    public DozerHealthMsg(int data_length) {
        super(data_length);
        amTypeSet(AM_TYPE);
    }

    public DozerHealthMsg(int data_length, int base_offset) {
        super(data_length, base_offset);
        amTypeSet(AM_TYPE);
    }

    public DozerHealthMsg(byte[] data) {
        super(data);
        amTypeSet(AM_TYPE);
    }

    public DozerHealthMsg(byte[] data, int base_offset) {
        super(data, base_offset);
        amTypeSet(AM_TYPE);
    }

    public DozerHealthMsg(byte[] data, int base_offset, int data_length) {
        super(data, base_offset, data_length);
        amTypeSet(AM_TYPE);
    }

    public DozerHealthMsg(Message msg, int base_offset) {
        super(msg, base_offset, DEFAULT_MESSAGE_SIZE);
        amTypeSet(AM_TYPE);
    }

    public DozerHealthMsg(Message msg, int base_offset, int data_length) {
        super(msg, base_offset, data_length);
        amTypeSet(AM_TYPE);
    }

    public int get_payload_sampleNo()
    {
    	if (sampleno == null)
    	{
    		sampleno = new Integer((int) ((getUIntElement(7 * 8, 8) << 8) + getUIntElement(8 * 8, 8)));
    	}
    		
    	return sampleno;
    }

    public short get_payload_uptime_high() {
    	if (uptime_high == null)
    	{
    		uptime_high = new Short((short) (getUIntElement(11 * 8, 8)));
    	}
    	
    	return uptime_high;
    }

    public int get_payload_uptime_low() {
    	if (uptime_low == null)
    	{
    		uptime_low = new Integer((int) ((getUIntElement(9 * 8, 8) << 8) + getUIntElement(10 * 8, 8)));
    	}
    	
    	return uptime_low;
    }
    
    public int get_payload_sysvoltage()
    {
    	if (sysvoltage == null)
    	{
    		sysvoltage = new Integer((int) ((getUIntElement(12 * 8, 8) << 8) + getUIntElement(13 * 8, 8)));
    	}
    		
    	return sysvoltage;
    }
    
    public int get_payload_sdivoltage()
    {
    	if (sdivoltage == null)
    	{
    		sdivoltage = new Integer((int) ((getUIntElement(14*8, 8) << 8) + getUIntElement(15*8, 8)));
    	}
    	
    	return sdivoltage;
    }
    
    public int get_payload_temperature()
    {
    	if (temperature == null)
    	{
    		temperature = new Integer((int) ((getUIntElement(16 * 8, 8) << 8) + getUIntElement(17 * 8, 8)));
    	}
    	
    	return temperature;
    }
    
    public int get_payload_humidity()
    {
    	if (moisture == null)
    	{
    		moisture = new Integer((int) ((getUIntElement(18 * 8, 8) << 8) + getUIntElement(19 * 8, 8)));
    	}
    	
    	return moisture;
    }
    
    public int get_payload_mspvoltage()
    {
    	if (mspvoltage == null)
    	{
    		mspvoltage = new Integer((int) ((getUIntElement(20 * 8, 8) << 8) + getUIntElement(21 * 8, 8)));
    	}
    		
    	return mspvoltage;
    }

    public int get_payload_msptemperature()
    {
    	if (msptemperature == null)
    	{
    		msptemperature = new Integer((int) ((getUIntElement(22 * 8, 8) << 8) + getUIntElement(23 * 8, 8)));
    	}
    		
    	return msptemperature;
    }

    public int get_payload_flashStatus()
    {
    	if (flashstatus == null)
    	{
    		flashstatus = new Integer((int) ((getUIntElement(24 * 8, 8) << 8) + getUIntElement(25 * 8, 8)));
    	}
    		
    	return flashstatus;
    }

    public short get_payload_queueSize()
    {
    	if (queuesize == null)
    	{
    		queuesize = new Short((short) getUIntElement(26 * 8, 8));
    	}
    		
    	return queuesize;
    }

    public int get_payload_parentId()
    {
    	if (parentid == null)
    	{
    		parentid = new Integer((int) ((getUIntElement(27 * 8, 8) << 8) + getUIntElement(28 * 8, 8)));
    	}
    		
    	return parentid;
    }

    public short get_payload_hopCountChildCount()
    {
    	if (hopchildcount == null)
    	{
    		hopchildcount = new Short((short) getUIntElement(29 * 8, 8));
    	}
    		
    	return hopchildcount;
    }
    
    public String toString() {
        String s = "Message <DataNodeHealthMsg> \n";
        try {
          s += "  [header.seqNr=0x"+Long.toHexString(get_header_seqNr())+"]\n";
        } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
        try {
          s += "  [header.originatorID=0x"+Long.toHexString(get_header_originatorID())+"]\n";
        } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
        try {
          s += "  [header.aTime.low=0x"+Long.toHexString(get_header_aTime_low())+"]\n";
        } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
        try {
          s += "  [header.aTime.high=0x"+Long.toHexString(get_header_aTime_high())+"]\n";
        } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
        try {
          s += "  [payload.sampleNo=0x"+Long.toHexString(get_payload_sampleNo())+"]\n";
        } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
        try {
          s += "  [payload.uptime.low=0x"+Long.toHexString(get_payload_uptime_low())+"]\n";
        } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
        try {
          s += "  [payload.uptime.high=0x"+Long.toHexString(get_payload_uptime_high())+"]\n";
        } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
        try {
          s += "  [payload.sysvoltage=0x"+Long.toHexString(get_payload_sysvoltage())+"]\n";
        } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
        try {
          s += "  [payload.sdivoltage=0x"+Long.toHexString(get_payload_sdivoltage())+"]\n";
        } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
        try {
          s += "  [payload.temperature=0x"+Long.toHexString(get_payload_temperature())+"]\n";
        } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
        try {
          s += "  [payload.humidity=0x"+Long.toHexString(get_payload_humidity())+"]\n";
        } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
        try {
          s += "  [payload.mspvoltage=0x"+Long.toHexString(get_payload_mspvoltage())+"]\n";
        } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
        try {
          s += "  [payload.msptemperature=0x"+Long.toHexString(get_payload_msptemperature())+"]\n";
        } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
        try {
          s += "  [payload.flashStatus=0x"+Long.toHexString(get_payload_flashStatus())+"]\n";
        } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
        try {
          s += "  [payload.queueSize=0x"+Long.toHexString(get_payload_queueSize())+"]\n";
        } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
        try {
          s += "  [payload.parentId=0x"+Long.toHexString(get_payload_parentId())+"]\n";
        } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
        try {
          s += "  [payload.hopCountChildCount=0x"+Long.toHexString(get_payload_hopCountChildCount())+"]\n";
        } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
        return s;
      }
}
