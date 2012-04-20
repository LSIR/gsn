package ch.ethz.permasense.tinyos1x.messages;

import net.tinyos1x.message.Message;

public class DozerDigitalDCXMsg extends DozerAbstractMsg
{
	/** The default size of this message type in bytes. */
    public static final int DEFAULT_MESSAGE_SIZE = 38;

    /** The Active Message type associated with this message. */
    public static final int AM_TYPE = 0x86;
    
    private Integer sampleno = null;
    private Short battery = null;
    
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

    public int get_payload_sampleNo()
    {
    	if (sampleno == null)
    	{
    		sampleno = new Integer((int) ((getUIntElement(7 * 8, 8) << 8) + getUIntElement(8 * 8, 8)));
    	}
    		
    	return sampleno;
    }
    
    public short get_payload_batteryStatus()
    {
    	if (battery == null)
    	{
    		battery = new Short((short) getUIntElement(9 * 8, 8));
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


    public double[] get_payload_dcxData_floatArray() {
        double[] tmp = new double[4];

        for( int i=0; i<4; i++) {
        	tmp[i] = floatAt(i*5 + 10);
        }
        
        return tmp;
    }
    
    public static int numElements_payload_dcxData_floatArray() {
        return 4;
    }
    
    public short[] get_payload_dcxData_status() {
        short[] tmp = new short[4];
        
        for(int i=0; i<4; i++)
        	tmp[i] = new Short((short) ((getUIntElement((i*5+14) * 8, 8))));
        
        return tmp;
    }
    
    public static int numElements_payload_dcxData_status() {
        return 4;
    }

    public String toString() {
      String s = "Message <DataDigitalDCXMsg> \n";
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
        s += "  [payload.batteryStatus=0x"+Long.toHexString(get_payload_batteryStatus())+"]\n";
      } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
      try {
      } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
      try {
        s += "  [payload.dcxData.status=";
        for (int i = 0; i < 4; i++) {
          s += "0x"+floatAt(i)+" ";
        }
        s += "]\n";
      } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
      return s;
    }
}
