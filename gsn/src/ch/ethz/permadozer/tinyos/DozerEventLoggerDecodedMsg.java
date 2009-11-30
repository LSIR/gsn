package ch.ethz.permadozer.tinyos;

import java.util.Vector;

public class DozerEventLoggerDecodedMsg extends DozerEventLoggerMsg {
	
	private class EventLog {
		int time;
		short id;
		int type;
		int value;
	}
	
	public DozerEventLoggerDecodedMsg() {
        super();
    }

    public DozerEventLoggerDecodedMsg(int data_length) {
        super(data_length);
    }

    public DozerEventLoggerDecodedMsg(int data_length, int base_offset) {
        super(data_length, base_offset);
    }

    public DozerEventLoggerDecodedMsg(byte[] data) {
        super(data);
    }

    public DozerEventLoggerDecodedMsg(byte[] data, int base_offset) {
        super(data, base_offset);
    }

    public DozerEventLoggerDecodedMsg(byte[] data, int base_offset, int data_length) {
        super(data, base_offset, data_length);
    }

    public DozerEventLoggerDecodedMsg(net.tinyos.message.Message msg, int base_offset) {
        super(msg, base_offset);
    }

    public DozerEventLoggerDecodedMsg(net.tinyos.message.Message msg, int base_offset, int data_length) {
        super(msg, base_offset, data_length);
    }
	
	Vector<EventLog> log;
	
	public final int EVENT_TYPE_NOVALUE = 0;
	public final int EVENT_TYPE_VALUE_16 = 1;
	
	private void parseEventLog() {
		log = new Vector<EventLog>(7);
		try {
			for (int i=0;i<DozerEventLoggerMsg.numElements_payload_eventlog();) {
				EventLog el = new EventLog();
				el.time = (int)getUIntBEElement(offsetBits_payload_eventlog(i), 16) & 0x7fff;
				el.id = (short)getUIntBEElement(offsetBits_payload_eventlog(i+2), 8);
				el.type = (short)getUIntBEElement(offsetBits_payload_eventlog(i), 8) >> 7;
				if (el.type == EVENT_TYPE_VALUE_16) {
					el.value = (int)getUIntBEElement(offsetBits_payload_eventlog(i+3), 16);
					i+=5;
				}
				else {
					el.value = 0;
					i+=3;
				}
				log.add(el);
			}
		} catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
	}
	
    public static int numElements_payload_eventlog_time() {
        return 7;
    }
    public static int numElements_payload_eventlog_id() {
        return 7;
    }
    public static int numElements_payload_eventlog_type() {
        return 7;
    }
    public static int numElements_payload_eventlog_value() {
        return 7;
    }
    
    public static int numElements_payload_eventlog() {
    	return 0;
    }
    
	public int[] get_payload_eventlog_time() {
		if(log == null)
			parseEventLog();
		int[] tmp = new int[numElements_payload_eventlog_time()];
		int i;
		for (i=0;i<log.size();i++)
			tmp[i] = log.get(i).time;
		for (;i<numElements_payload_eventlog_time();i++)
			tmp[i] = -1;		
		return tmp;
	}
	
	public short[] get_payload_eventlog_id() {
		if(log == null)
			parseEventLog();
		short[] tmp = new short[numElements_payload_eventlog_id()];
		int i;
		for (i=0;i<log.size();i++)
			tmp[i] = log.get(i).id;
		for (;i<numElements_payload_eventlog_id();i++)
			tmp[i] = -1;
		return tmp;
	}
	
	public int[] get_payload_eventlog_type() {
		if(log == null)
			parseEventLog();
		int[] tmp = new int[numElements_payload_eventlog_type()];
		int i;
		for (i=0;i<log.size();i++)
			tmp[i] = log.get(i).type;
		for (;i<numElements_payload_eventlog_type();i++)
			tmp[i] = -1;
		return tmp;
	}
	
	public int[] get_payload_eventlog_value() {
		if(log == null)
			parseEventLog();
		int[] tmp = new int[numElements_payload_eventlog_value()];
		int i;
		for (i=0;i<log.size();i++)
			tmp[i] = log.get(i).value;
		for (;i<numElements_payload_eventlog_value();i++)
			tmp[i] = -1;
		return tmp;
	}
	
	/**
     * Return the entire array 'payload.eventlog' as a short[]
     */
    public short[] get_payload_eventlog() {
        short[] tmp = new short[0];
        return tmp;
    }
	
	/**
    /* Return a String representation of this message. Includes the
     * message type name and the non-indexed field values.
     */
    public String toString() {
      String s = "Message <DozerEventLoggerMsg> \n";
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
        s += "  [payload.eventlog_time=";
        	int[] tmp0 = get_payload_eventlog_time();
        	for (int i=0;i<tmp0.length;i++)
        		s += tmp0[i]+" ";
        s += "]\n";
        s += "  [payload.eventlog_id=";
    		short[] tmp1 = get_payload_eventlog_id();
    		for (int i=0;i<tmp1.length;i++)
    			s += tmp1[i]+" ";
        s += "]\n";
        s += "  [payload.eventlog_type=";
    		int[] tmp2 = get_payload_eventlog_type();
    		for (int i=0;i<tmp2.length;i++)
    			s += tmp2[i]+" ";
        s += "]\n";
        s += "  [payload.eventlog_value=";
    	int[] tmp3 = get_payload_eventlog_value();
    	for (int i=0;i<tmp3.length;i++)
    		s += tmp3[i]+" ";
        s += "]\n";
      } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
      return s;
    }

}
