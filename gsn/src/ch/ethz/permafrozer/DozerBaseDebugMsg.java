package ch.ethz.permafrozer;

import net.tinyos1x.message.Message;

public class DozerBaseDebugMsg extends DozerAbstractMsg
{
	/** The default size of this message type in bytes. */
    public static final int DEFAULT_MESSAGE_SIZE = 26;

    /** The Active Message type associated with this message. */
    public static final int AM_TYPE = 0x8E;

    private Short RAState = null;
    private Long radiosleep = null;
    private Long radioup = null;
    private Integer queue = null;

    public DozerBaseDebugMsg() {
        super(DEFAULT_MESSAGE_SIZE);
        amTypeSet(AM_TYPE);
    }

    public DozerBaseDebugMsg(int data_length) {
        super(data_length);
        amTypeSet(AM_TYPE);
    }

    public DozerBaseDebugMsg(int data_length, int base_offset) {
        super(data_length, base_offset);
        amTypeSet(AM_TYPE);
    }

    public DozerBaseDebugMsg(byte[] data) {
        super(data);
        amTypeSet(AM_TYPE);
    }

    public DozerBaseDebugMsg(byte[] data, int base_offset) {
        super(data, base_offset);
        amTypeSet(AM_TYPE);
    }

    public DozerBaseDebugMsg(byte[] data, int base_offset, int data_length) {
        super(data, base_offset, data_length);
        amTypeSet(AM_TYPE);
    }

    public DozerBaseDebugMsg(Message msg, int base_offset) {
        super(msg, base_offset, DEFAULT_MESSAGE_SIZE);
        amTypeSet(AM_TYPE);
    }

    public DozerBaseDebugMsg(Message msg, int base_offset, int data_length) {
        super(msg, base_offset, data_length);
        amTypeSet(AM_TYPE);
    }

    public short get_payload_RAState()
    {
    	if (RAState == null)
    	{
    		RAState = new Short((short) (getUIntElement(7 * 8, 8)));
    	}
    	
    	return RAState;
    }
    
    public long get_payload_radiosleep()
    {
    	if (radiosleep == null)
    	{
    		radiosleep = new Long((long) ((getUIntElement(8 * 8, 8) << 24) + (getUIntElement(9 * 8, 8) << 16) + (getUIntElement(10 * 8, 8)<<8) + (getUIntElement(11 * 8, 8))));
    	}
    	
    	return radiosleep;
    }
    
    public long get_payload_radioup()
    {
    	if (radioup == null)
    	{
    		radioup = new Long((long) ((getUIntElement(12 * 8, 8) << 24) + (getUIntElement(13 * 8, 8) << 16) + (getUIntElement(14 * 8, 8)<<8) + (getUIntElement(15 * 8, 8))));
    	}
    	
    	return radioup;
    }
    
    public int get_payload_queue()
    {
    	if (queue == null)
    	{
    		queue = new Integer((int) ((getUIntElement(16 * 8, 8) << 8) + getUIntElement(17 * 8, 8)));
    	}
    	
    	return queue;
    }


    public String toString() {
      String s = "Message <DozerBaseDebugMsg> \n";
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
        s += "  [payload.RAState=0x"+Long.toHexString(get_payload_RAState())+"]\n";
      } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
      try {
        s += "  [payload.radiosleep=0x"+Long.toHexString(get_payload_radiosleep())+"]\n";
      } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
      try {
        s += "  [payload.radioup=0x"+Long.toHexString(get_payload_radioup())+"]\n";
      } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
      try {
        s += "  [payload.queue=0x"+Long.toHexString(get_payload_queue())+"]\n";
      } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
      return s;
    }
}
