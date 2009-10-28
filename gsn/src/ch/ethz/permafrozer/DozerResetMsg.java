package ch.ethz.permafrozer;

import net.tinyos1x.message.Message;

public class DozerResetMsg extends DozerAbstractMsg
{
	/** The default size of this message type in bytes. */
    public static final int DEFAULT_MESSAGE_SIZE = 18;

    /** The Active Message type associated with this message. */
    public static final int AM_TYPE = 0x90;

    private Short cause = null;
    private Integer count = null;
    
    public DozerResetMsg() {
        super(DEFAULT_MESSAGE_SIZE);
        amTypeSet(AM_TYPE);
    }

    public DozerResetMsg(int data_length) {
        super(data_length);
        amTypeSet(AM_TYPE);
    }

    public DozerResetMsg(int data_length, int base_offset) {
        super(data_length, base_offset);
        amTypeSet(AM_TYPE);
    }

    public DozerResetMsg(byte[] data) {
        super(data);
        amTypeSet(AM_TYPE);
    }

    public DozerResetMsg(byte[] data, int base_offset) {
        super(data, base_offset);
        amTypeSet(AM_TYPE);
    }

    public DozerResetMsg(byte[] data, int base_offset, int data_length) {
        super(data, base_offset, data_length);
        amTypeSet(AM_TYPE);
    }

    public DozerResetMsg(Message msg, int base_offset) {
        super(msg, base_offset, DEFAULT_MESSAGE_SIZE);
        amTypeSet(AM_TYPE);
    }

    public DozerResetMsg(Message msg, int base_offset, int data_length) {
        super(msg, base_offset, data_length);
        amTypeSet(AM_TYPE);
    }

    public short get_payload_Cause()
    {
    	if (cause == null)
    	{
    		cause = new Short((short) getUIntElement(7 * 8, 8));
    	}
    		
    	return cause;
    }
    
    public int get_payload_Count()
    {
    	if (count == null)
    	{
    		count = new Integer((int) ((getUIntElement(8 * 8, 8) << 8) + (getUIntElement(9 * 8, 8))));
    	}
    		
    	return count;
    }

    public String toString()
    {
    	return "\t" + super.toString() + "\n\t\t" + " ResetCause:" + get_payload_Cause() +
    		" Bootcount:" + get_payload_Count() + "\n";
    }
}
