package gsn.vsensor;

import gsn.beans.DataField;
import gsn.beans.StreamElement;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.apache.log4j.Logger;

public class GPSLoggerDataParser extends BridgeVirtualSensorPermasense {
	
	private static final byte RAW_DATA_TYPE = 0;
	private static final byte STATUS_TYPE = 1;
	
	private static final byte[] rawHeader = {(byte) 0xB5, 0x62, 0x02, 0x10};
	private static final byte[] statusHeader = {0x6D, 0x74, 0x01, 0x01};

	private static final transient Logger logger = Logger.getLogger(GPSLoggerDataParser.class);

	private String storage_directory = null;
	
	private static DataField[] dataField = {
			new DataField("POSITION", "INTEGER"),
			new DataField("GENERATION_TIME", "BIGINT"),
			new DataField("TIMESTAMP", "BIGINT"),
			new DataField("DEVICE_ID", "INTEGER"),

			new DataField("DATA_TYPE", "TINYINT"),
			new DataField("SAMPLE_COUNT", "INTEGER"),
			new DataField("GPS_RAW_DATA_VERSION", "SMALLINT"),
			new DataField("GPS_SATS", "INTEGER"),
			new DataField("STATUS_SOLAR", "INTEGER"),
			new DataField("STATUS_HUMIDITY", "INTEGER"),
			new DataField("STATUS_TEMPERATURE", "SMALLINT"),
			new DataField("STATUS_INCL_X", "SMALLINT"),
			new DataField("STATUS_INCL_Y", "SMALLINT"),
			new DataField("RAW_DATA", "BINARY")};
	
	@Override
	public boolean initialize() {
		boolean ret = super.initialize();
		storage_directory = getVirtualSensorConfiguration().getStorage().getStorageDirectory();
		if (storage_directory != null) {
			storage_directory = new File(storage_directory, deployment).getPath();
		}
		return ret;
	}
	
	
	@Override
	public void dataAvailable(String inputStreamName, StreamElement data) {
		File file = new File(new File(storage_directory, Integer.toString((Integer)data.getData("device_id"))).getPath(), (String) data.getData("relative_file"));
		file = file.getAbsoluteFile();
		
		logger.info("new incoming file (" + file.getAbsolutePath() + ")");

		short GPS_RAW_DATA_VERSION = 1;
		int rawSampleCount = 1;
		int rawIncorrectChecksumCount = 0;
		int statusSampleCount = 1;
		int statusIncorrectChecksumCount = 0;
		try {
			DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(fixFile(file))));
			
			int b = dis.readByte();
			while (true) {
				boolean readOn = false;
				if (b == rawHeader[0]) {
					b = dis.readByte();
					if (b == rawHeader[1]) {
						b = dis.readByte();
						if (b == rawHeader[2]) {
							b = dis.readByte();
							if (b == rawHeader[3]) {
								// gps logger raw data
								byte [] rawPacket = getRawPacket(dis, rawHeader, file.getAbsolutePath());
								if (checkChecksum(rawPacket)) {
									long timestamp = getGPSRawTimestamp(rawPacket);
									data = new StreamElement(dataField, new Serializable[]{
											data.getData(dataField[0].getName()),
											timestamp,
											timestamp,
											data.getData(dataField[3].getName()),
											RAW_DATA_TYPE,
											rawSampleCount,
											GPS_RAW_DATA_VERSION,
											(int)getUByte(rawPacket[12]),
											null,
											null,
											null,
											null,
											null,
											rawPacket});

									super.dataAvailable(inputStreamName, data);
								}
								else {
									rawIncorrectChecksumCount++;
									logger.warn("checksum for gps raw data sample " + rawSampleCount + " is not correct in " + file.getAbsolutePath());
									dis.reset();
								}
								
								rawSampleCount++;
								if (rawSampleCount==Integer.MAX_VALUE)
									rawSampleCount = 1;
								
								readOn = true;
							}
						}
					}
				}
				else if (b == statusHeader[0]) {
					b = dis.readByte();
					if (b == statusHeader[1]) {
						b = dis.readByte();
						if (b == statusHeader[2]) {
							b = dis.readByte();
							if (b == statusHeader[3]) {
								// gps logger status data
								byte [] rawPacket = getRawPacket(dis, statusHeader, file.getAbsolutePath());
								if (checkChecksum(rawPacket)) {
									ByteBuffer buf = ByteBuffer.wrap(rawPacket);
									buf.order(ByteOrder.LITTLE_ENDIAN);
									buf.position(6);
									long timestamp = getUInt(new byte[]{buf.get(),buf.get(),buf.get(),buf.get()})*1000L;
									data = new StreamElement(dataField, new Serializable[]{
											data.getData(dataField[0].getName()),
											timestamp,
											timestamp,
											data.getData(dataField[3].getName()),
											STATUS_TYPE,
											statusSampleCount,
											null,
											null,
											getUShort(new byte[]{buf.get(),buf.get()}),
											getUShort(new byte[]{buf.get(),buf.get()})/10,
											(short)(buf.getShort()/10),
											buf.getShort(),
											buf.getShort(),
											rawPacket});

									super.dataAvailable(inputStreamName, data);
								}
								else {
									statusIncorrectChecksumCount++;
									logger.warn("checksum for gps status data sample " + statusSampleCount + " is not correct in " + file.getAbsolutePath());
									dis.reset();
								}
								
								statusSampleCount++;
								if (statusSampleCount==Integer.MAX_VALUE)
									statusSampleCount = 1;
								
								readOn = true;
							}
						}
					}
				}
				else
					readOn = true;
				
				if (readOn)
					b = dis.readByte();
			}
		} catch (EOFException e) {
			logger.debug("end of file reached");
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

		logger.info((rawSampleCount-1) + " raw samples and " + (statusSampleCount-1) + " status samples read");
		if (rawIncorrectChecksumCount > 0)
			logger.warn(rawIncorrectChecksumCount + " checksums did not match for raw data samples in " + file.getAbsolutePath());
		if (statusIncorrectChecksumCount > 0)
			logger.warn(statusIncorrectChecksumCount + " checksums did not match for status data samples in " + file.getAbsolutePath());
	}
	
	
	private byte[] getRawPacket(DataInputStream dis, byte[] header, String filename) throws IOException, EOFException {
		dis.mark(65538);
		byte [] length = new byte [2];
		dis.read(length);
		byte [] rest = new byte [((length[0]&0xFF) | ((length[1]&0xFF) << 8)) + 2];
		int plength = dis.read(rest);
		if (plength != rest.length) {
			if (plength != -1)
				logger.warn("could not read a whole data packet at the end of file " + filename + " -> drop " + plength + " bytes");
			throw new EOFException();
		}
		
		return concatAll(header, new byte [] {length[0], length[1]}, rest);
	}
	
	
	private long getGPSRawTimestamp(byte[] rawPacket) {
		ByteBuffer buf = ByteBuffer.wrap(rawPacket);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		return (315964800L+(604800L*(long)buf.getShort(10)))*1000L+(long)buf.getInt(6);
	}


	private File fixFile(File f) throws IOException {
		FileInputStream fis = new FileInputStream(f);
		FileOutputStream fos = new FileOutputStream(f.getAbsolutePath() + ".fix");
		
		byte[] tmp = new byte [1016];
		while (fis.skip(8) == 8) {
			int i = fis.read(tmp);
			if (i == -1)
				break;
			else if (i != 1016)
				logger.warn("less than 1016 bytes have been read at the end of file " + f.getAbsolutePath() + " -> drop " + i + " bytes");
			
			fos.write(tmp);
		}
		fis.close();
		fos.close();
		
		return new File(f.getAbsolutePath() + ".fix");
	}
	
	
	private boolean checkChecksum(byte [] check) {
		long ck_a = 0x00;
		long ck_b = 0x00;
		
		for (int i=2; i<check.length-2; i++) {
			ck_a += (long) check[i] & 0xFF;
			ck_b += ck_a;
		}
		ck_a &= 0xFF;
		ck_b &= 0xFF;
		return ((check[check.length-2] & 0xFF) == ck_a) && ((check[check.length-1] & 0xFF) == ck_b);
	}
	
	private static short getUByte(byte b) {
		return (short) (b&0xFF);
	}
	
	private static int getUShort(byte[] array) {
		return (array[1]&0xFF) << 8 | (array[0]&0xFF);
	}
	
	private static long getUInt(byte[] array) {
		return (long) ((array[3]&0xFF) << 24 | (array[2]&0xFF) << 16 | (array[1]&0xFF) << 8 | (array[0]&0xFF));
	}
	
	private static byte[] concatAll(byte[] first, byte[]... rest) {
		int totalLength = first.length;
		for (byte[] array : rest) {
			totalLength += array.length;
		}
		byte[] result = Arrays.copyOf(first, totalLength);
		int offset = first.length;
		for (byte[] array : rest) {
			System.arraycopy(array, 0, result, offset, array.length);
			offset += array.length;
		}
		return result;
	}
}
