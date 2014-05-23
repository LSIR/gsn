package gsn.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BinaryParser {
	
	public static int readNextChar(InputStream in, boolean signed) throws IOException{
		byte[] buf = new byte[1];
		in.read(buf);
		if (signed || buf[0] < 0) return (int)buf[0];
		else return buf[0] & 0xFF;
	}
	
	public static int readNextShort(InputStream in, boolean signed) throws IOException{
		byte[] buf = new byte[2];
		in.read(buf);
		short s = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).getShort();
		if (signed || s < 0) return (int)s;
		else return s & 0xFFFF;
	}
	
	public static long readNextLong(InputStream in, boolean signed) throws IOException{
		byte[] buf = new byte[4];
		in.read(buf);
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
	public static String readNextString(InputStream in, int maxLength) throws IOException{
		int length = readNextChar(in, false);
		if (length > maxLength || length <= 0) throw new IOException("String length out of bounds: " + length);
		byte[] b = new byte[length];
		in.read(b);
		return new String(b);
	}

}
