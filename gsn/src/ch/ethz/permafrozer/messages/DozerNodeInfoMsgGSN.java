package ch.ethz.permafrozer.messages;

public class DozerNodeInfoMsgGSN extends DozerNodeInfoMsg {

    public DozerNodeInfoMsgGSN() {
        super();
    }

    public DozerNodeInfoMsgGSN(int data_length) {
        super(data_length);
    }

    public DozerNodeInfoMsgGSN(int data_length, int base_offset) {
        super(data_length, base_offset);
    }

    public DozerNodeInfoMsgGSN(byte[] data) {
        super(data);
    }

    public DozerNodeInfoMsgGSN(byte[] data, int base_offset) {
        super(data, base_offset);
    }

    public DozerNodeInfoMsgGSN(byte[] data, int base_offset, int data_length) {
        super(data, base_offset, data_length);
    }

    public DozerNodeInfoMsgGSN(net.tinyos.message.Message msg, int base_offset) {
        super(msg, base_offset);
    }

    public DozerNodeInfoMsgGSN(net.tinyos.message.Message msg, int base_offset, int data_length) {
        super(msg, base_offset, data_length);
    }

    
    public long[] get_payload_ID_longArray() {
    	long[] ret = {0, 0};
        short[][] tmp = super.get_payload_ID();
        
        for(int i=0; i<2; i++) {
        	for(int j=0; j<6; j++) {
        		ret[i] = (long) tmp[i][j] << (40-j*8);
        	}
        }
        
        return ret;
    }
    
    public static int numElements_payload_ID_longArray() {
        return 2;
    }
}
