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

    public int get_payload_sampleNo()
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
    
    public int[] get_payload_sibADCDiff() {
        int[] tmp = new int[4];
    	if (sibAdcDiff == null)
    		readSibAdc();
    	
    	for(int i=0; i<4; i++)
    		tmp[i] = sibAdcDiff[i];
    	
        return tmp;
    }

    public static int numElements_payload_sibADCDiff() {
        return 4;
    }
    
    public int[] get_payload_sibADCCom() {
        int[] tmp = new int[3];
    	if (sibAdcCom == null)
    		readSibAdc();
    	
    	for(int i=0; i<3; i++)
    		tmp[i] = sibAdcCom[i];
    	
        return tmp;
    }
    
    public static int numElements_payload_sibADCCom() {
        return 3;
    }
    
    public String toString() {
        String s = "Message <DataAdcDiffMsg> \n";
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
          s += "  [payload.sibADCDiff=";
          for (int i = 0; i < 4; i++) {
            s += "0x"+Long.toHexString(sibAdcDiff[i] & 0xffff)+" ";
          }
          s += "]\n";
        } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
        try {
          s += "  [payload.sibADCCom=";
          for (int i = 0; i < 3; i++) {
            s += "0x"+Long.toHexString(sibAdcCom[i] & 0xffff)+" ";
          }
          s += "]\n";
        } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
        return s;
      }
}
