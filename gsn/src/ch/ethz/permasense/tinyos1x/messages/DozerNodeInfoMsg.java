package ch.ethz.permasense.tinyos1x.messages;

import net.tinyos1x.message.Message;

public class DozerNodeInfoMsg extends DozerAbstractMsg
{
	//nx_uint8_t ID[2][6]; // 12
	//nx_uint8_t XE1205Frequency;
	/** The default size of this message type in bytes. */
    public static final int DEFAULT_MESSAGE_SIZE = 28;

    /** The Active Message type associated with this message. */
    public static final int AM_TYPE = 0x82;

    private Long Id1, Id2; // = new Integer[2][6];
    private Short XE1205Frequency = null;
        
    public DozerNodeInfoMsg() {
        super(DEFAULT_MESSAGE_SIZE);
        amTypeSet(AM_TYPE);
    }

    public DozerNodeInfoMsg(int data_length) {
        super(data_length);
        amTypeSet(AM_TYPE);
    }

    public DozerNodeInfoMsg(int data_length, int base_offset) {
        super(data_length, base_offset);
        amTypeSet(AM_TYPE);
    }

    public DozerNodeInfoMsg(byte[] data) {
        super(data);
        amTypeSet(AM_TYPE);
    }

    public DozerNodeInfoMsg(byte[] data, int base_offset) {
        super(data, base_offset);
        amTypeSet(AM_TYPE);
    }

    public DozerNodeInfoMsg(byte[] data, int base_offset, int data_length) {
        super(data, base_offset, data_length);
        amTypeSet(AM_TYPE);
    }

    public DozerNodeInfoMsg(Message msg, int base_offset) {
        super(msg, base_offset, DEFAULT_MESSAGE_SIZE);
        amTypeSet(AM_TYPE);
    }

    public DozerNodeInfoMsg(Message msg, int base_offset, int data_length) {
        super(msg, base_offset, data_length);
        amTypeSet(AM_TYPE);
    }

    
    public long[] get_payload_ID_longArray() {
    	long[] ret = {0, 0};
    	
    	ret[0] = getId1();
    	ret[1] = getId2();
        
        return ret;
    }
    
    public static int numElements_payload_ID_longArray() {
        return 2;
    }
    
    public long getId1()
    {
    	if (Id1 == null)
    	{
    		Id1=new Long( (long) (getUIntElement(7 * 8, 8) << 40) +
    				(long) (getUIntElement(8 * 8, 8) << 32) +
    				(long) (getUIntElement(9 * 8, 8) << 24) +
    				(long) (getUIntElement(10 * 8, 8) << 16) +
    				(long) (getUIntElement(11 * 8, 8) << 8) +
    				(long) (getUIntElement(12 * 8, 8) << 0));
    	}	
    	return Id1;
    }
    
    public long getId2()
    {
    	if (Id2 == null)
    	{
    		Id2=new Long( (long) (getUIntElement(13 * 8, 8) << 40) +
    				(long) (getUIntElement(14 * 8, 8) << 32) +
    				(long) (getUIntElement(15 * 8, 8) << 24) +
    				(long) (getUIntElement(16 * 8, 8) << 16) +
    				(long) (getUIntElement(17 * 8, 8) << 8) +
    				(long) (getUIntElement(18 * 8, 8) << 0));
    	}	
    	return Id2;
    }
    
    public short get_payload_XE1205Frequency()
    {
    	if (XE1205Frequency == null)
    	{
    		XE1205Frequency=new Short( (short) getUIntElement(19 * 8, 8));
    	}	
    	return XE1205Frequency;
    }
    
    public String toString()
    {
    	return "\t" + super.toString() + "\n\t\t Id1:" + String.format("%02x", getId1()) + 
    	 	" Id2:" + String.format("%02x", getId2()) + " FRQ:" + get_payload_XE1205Frequency() + "\n";
    }
}
