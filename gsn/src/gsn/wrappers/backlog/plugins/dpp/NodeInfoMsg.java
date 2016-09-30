package gsn.wrappers.backlog.plugins.dpp;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;

import gsn.beans.DataField;

public class NodeInfoMsg implements Message {
	
	private static DataField[] dataField = {
			new DataField("COMPONENT_ID", "SMALLINT"),
			new DataField("RST_CNT", "SMALLINT"),
			new DataField("RST_FLAG", "SMALLINT"),
			new DataField("MCU_DESC", "VARCHAR(12)"),
			new DataField("COMPILER_DESC", "VARCHAR(3)"),
			new DataField("COMPILER_VER", "INTEGER"),
			new DataField("COMPILE_DATE", "BIGINT"),
			new DataField("FW_NAME", "VARCHAR(8)"),
			new DataField("FW_VER", "INTEGER"),
			new DataField("SW_REV_ID", "INTEGER")};

	@Override
	public Serializable[] receivePayload(ByteBuffer payload) throws Exception {
		Short component_id = null;
		Short rst_cnt = null;
		Short rst_flag = null;
		byte [] mcuDesc = new byte [12];
		String mcu_desc = null;
		byte [] compilerDesc = new byte [3];
		String compiler_desc = null;
		Integer compiler_ver = null;
		Long compile_date = null;
		byte [] fwName = new byte [8];
		String fw_name = null;
		Integer fw_ver = null;
		byte [] swRevId = new byte [] {0,0,0,0};
		Integer sw_rev_id = null;
		
		try {
			component_id = (short) (payload.get() & 0xff); 		// uint8_t: component ID
			rst_cnt = (short) (payload.get() & 0xff); 			// uint8_t: component reset count
			rst_flag = (short) (payload.get() & 0xff); 			// uint8_t: reset flag
			payload.get(mcuDesc);								// char[12]: MCU description
			mcu_desc = byteArrayToString(mcuDesc);
			payload.get(compilerDesc);							// char[3]: compiler description
			compiler_desc = byteArrayToString(compilerDesc);
			compiler_ver = payload.getShort() & 0xffff; 		// uint16_t: compiler version
			compile_date = payload.getInt() & 0xffffffffL; 		// uint32_t: compile date
			payload.get(fwName);								// char[8]: firmware name
			fw_name = byteArrayToString(fwName);
			fw_ver = payload.getShort() & 0xffff; 				// uint16_t: firmware version
			payload.get(swRevId,0,3);							// uint8_t[3]: software (GIT/SVN) revision ID
			sw_rev_id = ByteBuffer.wrap(swRevId).order(ByteOrder.LITTLE_ENDIAN).getInt();
		} catch (Exception e) {
		}
        
		return new Serializable[]{component_id, rst_cnt, rst_flag, mcu_desc, compiler_desc, compiler_ver, compile_date, fw_name, fw_ver, sw_rev_id};
	}
	
	private String byteArrayToString(byte[] array) {
		int i;
		for (i=0; i<array.length && array[i] != 0; i++) {}
		return new String(Arrays.copyOfRange(array, 0, i), Charset.forName("UTF-8"));
	}

	@Override
	public ByteBuffer sendPayload(String action, String[] paramNames, Object[] paramValues) throws Exception {
		throw new Exception("sendPayload not implemented");
	}

	@Override
	public DataField[] getOutputFormat() {
		return dataField;
	}

	@Override
	public int getType() {
		return gsn.wrappers.backlog.plugins.dpp.MessageTypes.MSG_TYPE_NODE_INFO;
	}

	@Override
	public boolean isExtended() {
		return false;
	}

	@Override
	public Serializable[] sendPayloadSuccess(boolean success) {
		return null;
	}
}
