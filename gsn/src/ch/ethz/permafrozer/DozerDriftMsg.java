package ch.ethz.permafrozer;

import net.tinyos1x.message.Message;

public class DozerDriftMsg extends DozerAbstractMsg
{
	/** The default size of this message type in bytes. */
    public static final int DEFAULT_MESSAGE_SIZE = 35;

    /** The Active Message type associated with this message. */
    public static final int AM_TYPE =  0x8E;

    private Integer myTemp0 = null;
    private Integer myTemp1 = null;
    private Integer myTemp2 = null;
    private Integer myTemp3 = null;
    private Integer parentTemp0 = null;
    private Integer parentTemp1 = null;
    private Integer parentTemp2 = null;
    private Integer parentTemp3 = null;
    private Integer driftPPM0 = null;
    private Integer driftPPM1 = null;
    private Integer driftPPM2 = null;
    private Integer driftPPM3 = null;
    
	/*nx_uint16_t myTemp[4];
	nx_uint16_t parentTemp[4];
	nx_uint8_t driftPPM[4];*/  
    public DozerDriftMsg() {
        super(DEFAULT_MESSAGE_SIZE);
        amTypeSet(AM_TYPE);
    }

    public DozerDriftMsg(int data_length) {
        super(data_length);
        amTypeSet(AM_TYPE);
    }

    public DozerDriftMsg(int data_length, int base_offset) {
        super(data_length, base_offset);
        amTypeSet(AM_TYPE);
    }

    public DozerDriftMsg(byte[] data) {
        super(data);
        amTypeSet(AM_TYPE);
    }

    public DozerDriftMsg(byte[] data, int base_offset) {
        super(data, base_offset);
        amTypeSet(AM_TYPE);
    }

    public DozerDriftMsg(byte[] data, int base_offset, int data_length) {
        super(data, base_offset, data_length);
        amTypeSet(AM_TYPE);
    }

    public DozerDriftMsg(Message msg, int base_offset) {
        super(msg, base_offset, DEFAULT_MESSAGE_SIZE);
        amTypeSet(AM_TYPE);
    }

    public DozerDriftMsg(Message msg, int base_offset, int data_length) {
        super(msg, base_offset, data_length);
        amTypeSet(AM_TYPE);
    }

    /****************** myTemp ************************************************/
    public int getmyTemp0()
    {
    	if (myTemp0 == null)
    	{
    		myTemp0 = new Integer((int) ((getUIntElement(7 * 8, 8) << 8) + getUIntElement(8 * 8, 8)));
    	}
    	
    	return myTemp0;
    }
    public int getmyTemp1()
    {
    	if (myTemp1 == null)
    	{
    		myTemp1 = new Integer((int) ((getUIntElement(9 * 8, 8) << 8) + getUIntElement(10 * 8, 8)));
    	}
    	
    	return myTemp1;
    }
    public int getmyTemp2()
    {
    	if (myTemp2 == null)
    	{
    		myTemp2 = new Integer((int) ((getUIntElement(11 * 8, 8) << 8) + getUIntElement(12 * 8, 8)));
    	}
    	
    	return myTemp2;
    }
    public int getmyTemp3()
    {
    	if (myTemp3 == null)
    	{
    		myTemp3 = new Integer((int) ((getUIntElement(13 * 8, 8) << 8) + getUIntElement(14 * 8, 8)));
    	}
    	
    	return myTemp3;
    }
    
    /****************** parentTemp ************************************************/
    public int getparentTemp0()
    {
    	if (parentTemp0 == null)
    	{
    		parentTemp0 = new Integer((int) ((getUIntElement(15 * 8, 8) << 8) + getUIntElement(16 * 8, 8)));
    	}
    	
    	return parentTemp0;
    }
    public int getparentTemp1()
    {
    	if (parentTemp1 == null)
    	{
    		parentTemp1 = new Integer((int) ((getUIntElement(17 * 8, 8) << 8) + getUIntElement(18 * 8, 8)));
    	}
    	
    	return parentTemp1;
    }
    public int getparentTemp2()
    {
    	if (parentTemp2 == null)
    	{
    		parentTemp2 = new Integer((int) ((getUIntElement(19 * 8, 8) << 8) + getUIntElement(20 * 8, 8)));
    	}
    	
    	return parentTemp2;
    }
    public int getparentTemp3()
    {
    	if (parentTemp3 == null)
    	{
    		parentTemp3 = new Integer((int) ((getUIntElement(21 * 8, 8) << 8) + getUIntElement(22 * 8, 8)));
    	}
    	
    	return parentTemp3;
    }
    
    /****************** driftPPM ************************************************/
    public int getdriftPPM0()
    {
    	if (driftPPM0 == null)
    	{
    		driftPPM0 = new Integer((int) getUIntElement(23 * 8, 8));
    	}
    	
    	return driftPPM0;
    }
    public int getdriftPPM1()
    {
    	if (driftPPM1 == null)
    	{
    		driftPPM1 = new Integer((int) getUIntElement(24 * 8, 8));
    	}
    	
    	return driftPPM1;
    }
    public int getdriftPPM2()
    {
    	if (driftPPM2 == null)
    	{
    		driftPPM2 = new Integer((int) getUIntElement(25 * 8, 8));
    	}
    	
    	return driftPPM2;
    }
    public int getdriftPPM3()
    {
    	if (driftPPM3 == null)
    	{
    		driftPPM3 = new Integer((int) getUIntElement(26 * 8, 8));
    	}
    	
    	return driftPPM3;
    }   
    
    
    public String toString()
    {
    	return "\t" + super.toString() + "\n\t\t" +
    	"Tp,T,drift:" + getparentTemp0() + " " + getmyTemp0() + " " + getdriftPPM0() +"\n\t\t"+
    	"Tp,T,drift:" + getparentTemp1() + " " + getmyTemp1() + " " + getdriftPPM1() +"\n\t\t"+
    	"Tp,T,drift:" + getparentTemp2() + " " + getmyTemp2() + " " + getdriftPPM2() +"\n\t\t"+
    	"Tp,T,drift:" + getparentTemp3() + " " + getmyTemp3() + " " + getdriftPPM3() +
    	"\n";
    }
}
