package ch.ethz.permafrozer;

import net.tinyos1x.message.Message;

public abstract class DozerAbstractMsg extends Message
{
    private Integer seqnr = null;
    private Integer originatorid = null;
    private Integer atime = null;
    private Long gentime = null;
    private String gentime_date = null;
    private Long timestamp = null;
    private String timestamp_date = null;

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
    
    public int getSeqNr()
    {
    	if (seqnr == null)
    	{
    		seqnr = new Integer((int) ((getUIntElement(0 * 8, 8) << 8) + getUIntElement(1 * 8, 8)));
    	}

		return seqnr;
    }
    
    public int getOriginatorID()
    {
    	if (originatorid == null)
    	{
    		originatorid = new Integer((int) ((getUIntElement(2 * 8, 8) << 8) + getUIntElement(3 * 8, 8)));
    	}
    		
    	return originatorid;
    }
    
    public int getATime()
    {
    	if (atime == null)
    	{
    		atime = new Integer((int) ((getUIntElement(6 * 8, 8) << 16) + (getUIntElement(4 * 8, 8) << 8) + getUIntElement(5 * 8, 8)));
    	}
    	
    	return atime;
    }
    
    public long getGenTime()
    {
    	if (gentime == null)
    	{
    		gentime = new Long(getTimeStamp() - (getATime() * 1000L));
    	}
    	
    	return gentime;
    }
    
    public String getGenTime_date()
    {
    	if (gentime_date == null)
    	{
    		gentime_date = MsgFormatter.DF.format(getGenTime());
    	}

    	return gentime_date;
    }

    public long getTimeStamp()
    {
    	if (timestamp == null)
    	{
        	long tstamp = 0;
        	
        	for (int i = 0; i < (8 * 8); i += 8)
        	{
        		tstamp += (getUIntElement(((super.dataLength() - 8) * 8) + i, 8) << i);
        	}

        	timestamp = new Long(tstamp);
    	}
    	
    	return timestamp;	
    }
    
    public String getTimeStamp_date()
    {
    	if (timestamp_date == null)
    	{
    		timestamp_date = MsgFormatter.DF.format(getTimeStamp());
    	}
    	
    	return timestamp_date;
    }
    
    public long getTimed () {
        return getTimeStamp();
    }
    
    public String toString()
    {
    	return "seqNr:" + getSeqNr() + " originatorID:" + getOriginatorID() + 
    		" aTime:" + getATime() + "(" + getGenTime_date() + ")" + 
    		" tstamp:" + getTimeStamp() + "(" + getTimeStamp_date() + ")";
    }
}
