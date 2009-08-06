package ch.ethz.permafrozer;

import net.tinyos1x.message.Message;

public class DozerDigitalDCXMsg extends DozerAbstractMsg
{
	/** The default size of this message type in bytes. */
    public static final int DEFAULT_MESSAGE_SIZE = 38;

    /** The Active Message type associated with this message. */
    public static final int AM_TYPE = 0x86;
    
    private Integer sampleno = null;
    private Integer battery = null;
    private Double Pdiff = null; // P1-P2
    private Double P2 = null;
    private Double TOB1 = null;
    private Double TOB2 = null;
    
    public DozerDigitalDCXMsg() {
        super(DEFAULT_MESSAGE_SIZE);
        amTypeSet(AM_TYPE);
    }

    public DozerDigitalDCXMsg(int data_length) {
        super(data_length);
        amTypeSet(AM_TYPE);
    }

    public DozerDigitalDCXMsg(int data_length, int base_offset) {
        super(data_length, base_offset);
        amTypeSet(AM_TYPE);
    }

    public DozerDigitalDCXMsg(byte[] data) {
        super(data);
        amTypeSet(AM_TYPE);
    }

    public DozerDigitalDCXMsg(byte[] data, int base_offset) {
        super(data, base_offset);
        amTypeSet(AM_TYPE);
    }

    public DozerDigitalDCXMsg(byte[] data, int base_offset, int data_length) {
        super(data, base_offset, data_length);
        amTypeSet(AM_TYPE);
    }

    public DozerDigitalDCXMsg(Message msg, int base_offset) {
        super(msg, base_offset, DEFAULT_MESSAGE_SIZE);
        amTypeSet(AM_TYPE);
    }

    public DozerDigitalDCXMsg(Message msg, int base_offset, int data_length) {
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
    
    public int getBattery()
    {
    	if (battery == null)
    	{
    		battery = new Integer((int) getUIntElement(9 * 8, 8));
    	}
    	
    	return battery;
    }

    private Double floatAt(int start)
    {
    	Double tmp;
    	
    	tmp = new Double(
				Math.pow(2, (-127 -23 + ((int)getUIntElement((start+3) * 8, 7)*2 + getUIntElement((start+2) * 8 + 7, 1)))) *
				(getUIntElement((start) * 8, 8) + getUIntElement((start+1) * 8, 8)*256 + (128+getUIntElement((start+2) * 8, 7))*65536)
					);

    	if (getUIntElement((start +3)* 8 + 7, 1) == 1)
    	{
    		tmp = -tmp;
    	}
    	
    	if (getUIntElement((start +4)* 8 , 8) != 0)
    	{
    		tmp = 0d;
    	}
    	
    	return tmp;
    }
        
    public double getPdiff()
    {
    	if (Pdiff == null)
    	{
    		Pdiff = floatAt(10);
    	}
    	return Pdiff;
    }
    
    public double getP2()
    {
    	if (P2 == null)
    	{
    		P2 = floatAt(15);
    	}
    	return P2;
    }
    
    public double getTOB1()
    {
    	if (TOB1 == null)
    	{
    		TOB1 = floatAt(20);
    	}
    	return TOB1;
    }
    
    public double getTOB2()
    {
    	if (TOB2 == null)
    	{
    		TOB2 = floatAt(25);
    	}
    	return TOB2;
    }

    public String toString()
    {
    	    	
    	return "\t" + super.toString() + "\n\t\t" +
    		" sampleno:" + getSampleNo() + " battery:"+getBattery()+"% P1-P2:"+getPdiff()+ " P2:" +getP2() + " TOB1:" +getTOB1()+ " TOB2:" +getTOB2()+"\n";
    }
}
