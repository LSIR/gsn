package ch.ethz.permafrozer;

import net.tinyos1x.message.Message;

public class DozerAdcComDiffMsg extends DozerAbstractMsg
{
	/** The default size of this message type in bytes. */
    public static final int DEFAULT_MESSAGE_SIZE = 31;

    /** The Active Message type associated with this message. */
    public static final int AM_TYPE = 0x84;

    private Integer sampleno = null;
    private Integer[] sibAdcDiff;
    private Integer[] sibAdcCom;
    
    public DozerAdcComDiffMsg() {
        super(DEFAULT_MESSAGE_SIZE);
        amTypeSet(AM_TYPE);
    }

    public DozerAdcComDiffMsg(int data_length) {
        super(data_length);
        amTypeSet(AM_TYPE);
    }

    public DozerAdcComDiffMsg(int data_length, int base_offset) {
        super(data_length, base_offset);
        amTypeSet(AM_TYPE);
    }

    public DozerAdcComDiffMsg(byte[] data) {
        super(data);
        amTypeSet(AM_TYPE);
    }

    public DozerAdcComDiffMsg(byte[] data, int base_offset) {
        super(data, base_offset);
        amTypeSet(AM_TYPE);
    }

    public DozerAdcComDiffMsg(byte[] data, int base_offset, int data_length) {
        super(data, base_offset, data_length);
        amTypeSet(AM_TYPE);
    }

    public DozerAdcComDiffMsg(Message msg, int base_offset) {
        super(msg, base_offset, DEFAULT_MESSAGE_SIZE);
        amTypeSet(AM_TYPE);
    }

    public DozerAdcComDiffMsg(Message msg, int base_offset, int data_length) {
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
    
    private void readSibAdc() {
        sibAdcDiff = new Integer[4];
        sibAdcCom = new Integer[3];
        for (int i=0;i<7;i++) {
        	if (i<4)
        		sibAdcDiff[i] = new Integer((int) ((getUIntElement((9+2*i) * 8, 8) << 8) + getUIntElement((10+2*i) * 8, 8)));
        	else
        		sibAdcCom[i-4] = new Integer((int) ((getUIntElement((9+2*i) * 8, 8) << 8) + getUIntElement((10+2*i) * 8, 8)));
        }
    }
    
    public int getSibAdcDiff0()
    {
    	if (sibAdcDiff == null)
    		readSibAdc();
    	return sibAdcDiff[0];
    }
    
    public int getSibAdcDiff1()
    {
    	if (sibAdcDiff == null)
    		readSibAdc();
    	return sibAdcDiff[1];
    }

    public int getSibAdcDiff2()
    {
    	if (sibAdcDiff == null)
    		readSibAdc();
    	return sibAdcDiff[2];
    }

    public int getSibAdcDiff3()
    {
    	if (sibAdcDiff == null)
    		readSibAdc();
    	return sibAdcDiff[3];
    }

    public int getSibAdcCom0()
    {
    	if (sibAdcCom == null)
    		readSibAdc();
    	return sibAdcCom[0];
    }

    public int getSibAdcCom1()
    {
    	if (sibAdcCom == null)
    		readSibAdc();
    	return sibAdcCom[1];
    }

    public int getSibAdcCom2()
    {
    	if (sibAdcCom == null)
    		readSibAdc();
    	return sibAdcCom[2];
    }

    public String toString()
    {
    	StringBuffer AdcDiffString = new StringBuffer();
    	StringBuffer AdcComString = new StringBuffer();
    	if (sibAdcDiff == null)
    		readSibAdc();
    	for (int i=0;i<sibAdcDiff.length;i++)
    		AdcDiffString.append(sibAdcDiff[i].toString()+" ");
    	for (int i=0;i<sibAdcCom.length;i++)
    		AdcComString.append(sibAdcCom[i].toString()+" ");
    	
    	return "\t" + super.toString() + "\n\t\t" +
    		" sampleno:" + getSampleNo() + " ADCDiff: "+AdcDiffString + "ADCCom: "+AdcComString + "\n";
    }
}
