package ch.ethz.permafrozer;

import net.tinyos1x.message.Message;

public class DozerAdcMux2Msg extends DozerAbstractMsg
{
	/** The default size of this message type in bytes. */
    public static final int DEFAULT_MESSAGE_SIZE = 37;

    /** The Active Message type associated with this message. */
    public static final int AM_TYPE = 0x8C;

    private Integer sampleno = null;
    private Integer[] sibAdc;
    
    public DozerAdcMux2Msg() {
        super(DEFAULT_MESSAGE_SIZE);
        amTypeSet(AM_TYPE);
    }

    public DozerAdcMux2Msg(int data_length) {
        super(data_length);
        amTypeSet(AM_TYPE);
    }

    public DozerAdcMux2Msg(int data_length, int base_offset) {
        super(data_length, base_offset);
        amTypeSet(AM_TYPE);
    }

    public DozerAdcMux2Msg(byte[] data) {
        super(data);
        amTypeSet(AM_TYPE);
    }

    public DozerAdcMux2Msg(byte[] data, int base_offset) {
        super(data, base_offset);
        amTypeSet(AM_TYPE);
    }

    public DozerAdcMux2Msg(byte[] data, int base_offset, int data_length) {
        super(data, base_offset, data_length);
        amTypeSet(AM_TYPE);
    }

    public DozerAdcMux2Msg(Message msg, int base_offset) {
        super(msg, base_offset, DEFAULT_MESSAGE_SIZE);
        amTypeSet(AM_TYPE);
    }

    public DozerAdcMux2Msg(Message msg, int base_offset, int data_length) {
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
    private void readSibAdc() {
        sibAdc = new Integer[10];
        for (int i=0;i<10;i++) {
        	sibAdc[i] = new Integer((int) ((getUIntElement((9+2*i) * 8, 8) << 8) + getUIntElement((10+2*i) * 8, 8)));        	
        }
    }
    
    public int[] get_payload_sibADC0() {
        int[] tmp = new int[10];
    	if (sibAdc == null)
    		readSibAdc();
    	
    	for(int i=0; i<10; i++)
    		tmp[i] = sibAdc[i];
    	
        return tmp;
    }
    
    public static int numElements_payload_sibADC0() {
        return 10;
    }

    /**
    /* Return a String representation of this message. Includes the
     * message type name and the non-indexed field values.
     */
    public String toString() {
      String s = "Message <DataAdcMux2Msg> \n";
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
        s += "  [payload.sibADC0=";
        for (int i = 0; i < 10; i++) {
          s += "0x"+Long.toHexString(sibAdc[i] & 0xffff)+" ";
        }
        s += "]\n";
      } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
      return s;
    }
}
