package ch.ethz.permafrozer.messages;

public class DozerDigitalDCXMsgGSN extends DozerDigitalDCXMsg {

    public DozerDigitalDCXMsgGSN() {
        super();
    }

    public DozerDigitalDCXMsgGSN(int data_length) {
        super(data_length);
    }

    public DozerDigitalDCXMsgGSN(int data_length, int base_offset) {
        super(data_length, base_offset);
    }

    public DozerDigitalDCXMsgGSN(byte[] data) {
        super(data);
    }

    public DozerDigitalDCXMsgGSN(byte[] data, int base_offset) {
        super(data, base_offset);
    }

    public DozerDigitalDCXMsgGSN(byte[] data, int base_offset, int data_length) {
        super(data, base_offset, data_length);
    }

    public DozerDigitalDCXMsgGSN(net.tinyos.message.Message msg, int base_offset) {
        super(msg, base_offset);
    }

    public DozerDigitalDCXMsgGSN(net.tinyos.message.Message msg, int base_offset, int data_length) {
        super(msg, base_offset, data_length);
    }


    public double[] get_payload_dcxData_floatArray() {
        double[] tmp = new double[4];

        for( int i=0; i<4; i++) {
        	tmp[i] = floatAt(offset_payload_dcxData_floatbyte(0,0) + i*5);
        }
        
        return tmp;
    }
    
    public static int numElements_payload_dcxData_floatArray() {
        return 4;
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
}
