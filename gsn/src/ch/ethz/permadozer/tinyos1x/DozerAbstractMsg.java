package ch.ethz.permadozer.tinyos1x;

import net.tinyos1x.message.Message;

public abstract class DozerAbstractMsg extends Message
{
    private Integer seqnr = null;
    private Integer originatorid = null;
    private Short atime_high = null;
    private Integer atime_low = null;

    protected DozerAbstractMsg(int data_length) {
        super(data_length);
    }

    protected DozerAbstractMsg(int data_length, int base_offset) {
        super(data_length, base_offset);
    }

    protected DozerAbstractMsg(byte[] data) {
        super(data);
    }

    protected DozerAbstractMsg(byte[] data, int base_offset) {
        super(data, base_offset);
    }

    protected DozerAbstractMsg(byte[] data, int base_offset, int data_length) {
        super(data, base_offset, data_length);
    }

    protected DozerAbstractMsg(Message msg, int base_offset, int data_length) {
        super(msg, base_offset, data_length);
    }
    
    public int get_header_seqNr()
    {
    	if (seqnr == null)
    	{
    		seqnr = new Integer((int) ((getUIntElement(0 * 8, 8) << 8) + getUIntElement(1 * 8, 8)));
    	}

		return seqnr;
    }
    
    public int get_header_originatorID()
    {
    	if (originatorid == null)
    	{
    		originatorid = new Integer((int) ((getUIntElement(2 * 8, 8) << 8) + getUIntElement(3 * 8, 8)));
    	}
    		
    	return originatorid;
    }


    public short get_header_aTime_high() {
    	if (atime_high == null)
    	{
    		atime_high = new Short((short) (getUIntElement(6 * 8, 8)));
    	}
    	
    	return atime_high;
    }


    public int get_header_aTime_low() {
    	if (atime_low == null)
    	{
    		atime_low = new Integer((int) ((getUIntElement(4 * 8, 8) << 8) + getUIntElement(5 * 8, 8)));
    	}
    	
    	return atime_low;
    }
    
    
    public String toString()
    {
    	return "seqNr:" + get_header_seqNr() + " originatorID:" + get_header_originatorID() + 
    		" aTime_low:" + get_header_aTime_low() + " aTime_high:" + get_header_aTime_high();
    }
}
