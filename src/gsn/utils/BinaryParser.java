package gsn.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BinaryParser {
	
	private InputStream in;
	private int s1,s2 = 0;
	
	public BinaryParser(InputStream i){
		in = i;
	}
	
	public byte[] readBytes(int len) throws IOException{
		byte[] b = new byte[len];
		in.read(b);
		addSum(b);
		return b;
	}
	
	public int readNextChar(boolean signed) throws IOException{
		byte[] buf = new byte[1];
		in.read(buf);
		addSum(buf);
		if (signed || buf[0] < 0) return (int)buf[0];
		else return buf[0] & 0xFF;
	}
	
	public int readNextShort(boolean signed) throws IOException{
		byte[] buf = new byte[2];
		in.read(buf);
		addSum(buf);
		short s = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).getShort();
		if (signed || s < 0) return (int)s;
		else return s & 0xFFFF;
	}
	
	public long readNextLong(boolean signed) throws IOException{
		byte[] buf = new byte[4];
		in.read(buf);
		addSum(buf);
		int i = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).getInt();
		if (signed || i < 0) return (long)i;
		else return i & 0xFFFFFFFF;
	}
	
	/**
	 * 
	 * @param in: the input stream to read from. The first byte indicates the length of the string.
	 * @param maxLength: in case the read length is longer than maxLength, returns an empty string.
	 * @return the string object
	 * @throws IOException if errors happens when reading
	 */
	public String readNextString(int maxLength) throws IOException{
		int length = readNextChar(false);
		if (length > maxLength || length <= 0) throw new IOException("String length out of bounds: " + length);
		byte[] b = new byte[length];
		in.read(b);
		addSum(b);
		return new String(b);
	}

	public void resetChecksum() {
		s1 = 0;
		s2 = 0;
		
	}
	
	public void addSum(byte[] b){
		for(int i=0;i<b.length;i++){
			s1 = (s1 + b[i]) % 255;
			s2 = (s2 + s1) % 255;
		}
	}

	public boolean checkSum() throws IOException {
		/*byte[] buf = new byte[2];
		in.read(buf);
		if (s1 != buf[1] || s2 != buf[0]){
			return false;
		}*/
		return true;
	}

}
