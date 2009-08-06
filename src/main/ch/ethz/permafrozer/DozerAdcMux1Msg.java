package ch.ethz.permafrozer;

import net.tinyos1x.message.Message;

public class DozerAdcMux1Msg extends DozerAbstractMsg
{
	/** The default size of this message type in bytes. */
    public static final int DEFAULT_MESSAGE_SIZE = 37;

    /** The Active Message type associated with this message. */
    public static final int AM_TYPE = 0x8A;

    private Integer sampleno = null;
    private Integer[] sibAdc;
    
    public DozerAdcMux1Msg() {
        super(DEFAULT_MESSAGE_SIZE);
        amTypeSet(AM_TYPE);
    }

    public DozerAdcMux1Msg(int data_length) {
        super(data_length);
        amTypeSet(AM_TYPE);
    }

    public DozerAdcMux1Msg(int data_length, int base_offset) {
        super(data_length, base_offset);
        amTypeSet(AM_TYPE);
    }

    public DozerAdcMux1Msg(byte[] data) {
        super(data);
        amTypeSet(AM_TYPE);
    }

    public DozerAdcMux1Msg(byte[] data, int base_offset) {
        super(data, base_offset);
        amTypeSet(AM_TYPE);
    }

    public DozerAdcMux1Msg(byte[] data, int base_offset, int data_length) {
        super(data, base_offset, data_length);
        amTypeSet(AM_TYPE);
    }

    public DozerAdcMux1Msg(Message msg, int base_offset) {
        super(msg, base_offset, DEFAULT_MESSAGE_SIZE);
        amTypeSet(AM_TYPE);
    }

    public DozerAdcMux1Msg(Message msg, int base_offset, int data_length) {
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
        sibAdc = new Integer[10];
        for (int i=0;i<10;i++) {
        	sibAdc[i] = new Integer((int) ((getUIntElement((9+2*i) * 8, 8) << 8) + getUIntElement((10+2*i) * 8, 8)));        	
        }
    }
    
    public int getSibAdc0()
    {
    	if (sibAdc == null)
    		readSibAdc();
    	return sibAdc[0];
    }
    
    public int getSibAdc1()
    {
    	if (sibAdc == null)
    		readSibAdc();
    	return sibAdc[1];
    }

    public int getSibAdc2()
    {
    	if (sibAdc == null)
    		readSibAdc();
    	return sibAdc[2];
    }

    public int getSibAdc3()
    {
    	if (sibAdc == null)
    		readSibAdc();
    	return sibAdc[3];
    }

    public int getSibAdc4()
    {
    	if (sibAdc == null)
    		readSibAdc();
    	return sibAdc[4];
    }

    public int getSibAdc5()
    {
    	if (sibAdc == null)
    		readSibAdc();
    	return sibAdc[5];
    }

    public int getSibAdc6()
    {
    	if (sibAdc == null)
    		readSibAdc();
    	return sibAdc[6];
    }

    public int getSibAdc7()
    {
    	if (sibAdc == null)
    		readSibAdc();
    	return sibAdc[7];
    }

    public int getSibAdc8()
    {
    	if (sibAdc == null)
    		readSibAdc();
    	return sibAdc[8];
    }

    public int getSibAdc9()
    {
    	if (sibAdc == null)
    		readSibAdc();
    	return sibAdc[9];
    }

    public String toString()
    {
    	StringBuffer AdcString = new StringBuffer();
    	if (sibAdc == null)
    		readSibAdc();
    	for (int i=0;i<sibAdc.length;i++)
    		AdcString.append(sibAdc[i].toString()+" ");
    	return "\t" + super.toString() + "\n\t\t" +
    		" sampleno:" + getSampleNo() + " ADC: "+AdcString + "\n";
    }
}
