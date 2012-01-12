package gsn.utils;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.util.*;

public class SensorScopeListenerClient extends Thread {
    public static final String CONF_LOG4J_SENSORSCOPE_PROPERTIES = "conf/log4j_sensorscope.properties";
    private static final String CONF_SENSORSCOPE_SERVER_PROPERTIES = "conf/sensorscope_server.properties";
    private static final String DEFAULT_FOLDER_FOR_CSV_FILES = "logs";
    private static transient Logger logger = Logger.getLogger(SensorScopeListenerClient.class);

    private static String csvFolderName = null;
    private static String DEFAULT_NULL_STRING = "null";
    private static String nullString = DEFAULT_NULL_STRING;

    private static final byte BYTE_SYNC = 0x7E;
    private static final byte BYTE_ESC = 0x7D;

    private static final byte BYTE_ACK = 0x00;
    private static final byte BYTE_NACK = 0x01;

    private static final byte PACKET_DATA = 0x00;
    private static final byte PACKET_CRC = 0x01;

    private Socket mSocket;

    private static final int MAX_DUPN = 15; // maximum DUPN value = maximum number of extended sensors supported -1

    private static final int OFFSET_AIR_TEMP = 5 + (MAX_DUPN + 1) * 0;
    private static final int OFFSET_AIR_HUMID = 5 + (MAX_DUPN + 1) * 1;
    private static final int OFFSET_SOLAR_RAD = 5 + (MAX_DUPN + 1) * 2;
    private static final int OFFSET_RAIN_METER = 5 + (MAX_DUPN + 1) * 3;
    private static final int OFFSET_GROUND_TEMP_TNX = 5 + (MAX_DUPN + 1) * 4;
    private static final int OFFSET_AIR_TEMP_TNX = 5 + (MAX_DUPN + 1) * 5;
    private static final int OFFSET_SOIL_TEMP_ECTM = 5 + (MAX_DUPN + 1) * 6;
    private static final int OFFSET_SOIL_MOISTURE_ECTM = 5 + (MAX_DUPN + 1) * 7;
    private static final int OFFSET_SOIL_WATER_POTENTIAL = 5 + (MAX_DUPN + 1) * 8;
    private static final int OFFSET_SOIL_TEMP_DECAGON = 5 + (MAX_DUPN + 1) * 9;
    private static final int OFFSET_SOIL_MOISTURE_DECAGON = 5 + (MAX_DUPN + 1) * 10;
    private static final int OFFSET_SOIL_CONDUCT_DECAGON = 5 + (MAX_DUPN + 1) * 11;
    private static final int OFFSET_WIND_DIRECTION = 5 + (MAX_DUPN + 1) * 12;
    private static final int OFFSET_WIND_SPEED = 5 + (MAX_DUPN + 1) * 13;
    private static final int OFFSET_BATTERY_BOARD_VOLTAGE = 5 + (MAX_DUPN + 1) * 14;
    private static final int OFFSET_SOLAR_RAD_SP212 = 5 + (MAX_DUPN + 1) * 15;
    private static final int OFFSET_DECAGON_10HS_MV = 5 + (MAX_DUPN + 1) * 16;
    private static final int OFFSET_DECAGON_10HS_VWC = 5 + (MAX_DUPN + 1) * 17;

    public static void config() {
        Properties propertiesFile = new Properties();
        try {
            propertiesFile.load(new FileInputStream(CONF_SENSORSCOPE_SERVER_PROPERTIES));
        } catch (IOException e) {
            logger.error("Couldn't load configuration file: " + CONF_SENSORSCOPE_SERVER_PROPERTIES);
            logger.error(e.getMessage(), e);
            System.exit(-1);
        }

        csvFolderName = propertiesFile.getProperty("csvFolder", DEFAULT_FOLDER_FOR_CSV_FILES);
        nullString = propertiesFile.getProperty("nullString", DEFAULT_NULL_STRING);

    }

    public SensorScopeListenerClient(Socket socket) {
        PropertyConfigurator.configure(CONF_LOG4J_SENSORSCOPE_PROPERTIES);
        mSocket = socket;
        config();
        start();
    }

    int crc16Byte(int crc, int b) {
        crc = ((crc >> 8) & 0xFF) | (crc << 8);
        crc ^= b;
        crc ^= (crc & 0xFF) >> 4;
        crc ^= crc << 12;
        crc ^= (crc & 0xFF) << 5;

        return crc;
    }

    int crc16(byte[] buffer, int offset, int len) {
        int i;
        int crc = 0;

        for (i = offset; i < offset + len; ++i)
            crc = crc16Byte(crc, (int) buffer[i]);

        return crc;
    }

    private byte[] read(int len) {
        byte data[] = new byte[len];

        try {
            mSocket.getInputStream().read(data);
        } catch (Exception e) {
            return null;
        }

        return data;
    }

    private byte[] readPacket() {
        int idx = 0;
        int len = 0;
        byte[] data = null;
        byte[] b = null;
        boolean escape = false;

        while (true) {
            b = read(1);

            if (b == null)
                return null;

            if (b[0] == BYTE_SYNC)
                return b;

            if (escape) {
                b[0] ^= 0x20;
                escape = false;
            } else if (b[0] == BYTE_ESC) {
                escape = true;
                continue;
            }

            if (data == null) {
                len = b[0];
                data = new byte[len];
            } else {
                data[idx++] = b[0];

                if (len == '+' && data[0] == '+' && data[1] == '+') {
                    data = new byte[3];

                    data[0] = '+';
                    data[1] = '+';
                    data[2] = '+';

                    return data;
                }

                if (idx == len)
                    return data;
            }
        }
    }

    private void printPacket(byte[] pkt) {
        if (pkt[1] == 1) {
            int bytes[] = new int[pkt.length];

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < pkt.length; ++i) {
                byte b = pkt[i];

                if (b >= 0) bytes[i] = b;
                else bytes[i] = 256 + b;

                sb.append(String.format("%02X ", bytes[i]));

            }
            logger.info(sb.toString());
            //System.out.println();

            int id = (bytes[3] << 8) + bytes[4];
            int idx = 5;
            boolean fullTS = true;

            long base_timestamp = -1;
            long timestamp = -1;

            while (true) {
                if (idx >= bytes.length)
                    break;

                if (fullTS) {
                    base_timestamp = bytes[idx] * 16777216 + bytes[idx + 1] * 65536 + bytes[idx + 2] * 256 + bytes[idx + 3];
                    String date = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new java.util.Date(base_timestamp * 1000));
                    logger.info("base timestamp = " + base_timestamp + " ( " + date + " )");
                    idx += 4;
                    fullTS = false;
                    timestamp = base_timestamp;
                } else {
                    int timeshift = bytes[idx];  //TODO: verify
                    String date = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new java.util.Date((base_timestamp + timeshift) * 1000));
                    logger.info("time shift = " + timeshift + " ( " + date + " )");
                    ++idx;
                    timestamp = base_timestamp + timeshift * 60000;
                }

                int len = bytes[idx++];
                int nbBytes = 0;

                while (true) {
                    if (nbBytes >= len)
                        break;

                    int sid = bytes[idx];
                    int dupn = 0;
                    int size = 2;

                    if (sid >= 128) {
                        sid -= 128;

                        if (sid >= 108) {
                            idx += 2;
                            nbBytes += 2;

                            sid = (sid - 108) * 256 + bytes[idx - 1];
                        } else {
                            ++nbBytes;
                            ++idx;
                        }

                        dupn = (bytes[idx] >> 4) & 0x0F;
                        size = (bytes[idx] & 0x0F) + 1;

                        ++idx;
                        ++nbBytes;
                    } else {
                        if (sid >= 108) {
                            idx += 2;
                            nbBytes += 2;

                            sid = (sid - 108) * 256 + bytes[idx - 1];
                        } else {
                            ++idx;
                            ++nbBytes;
                        }
                    }

                    String date = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new java.util.Date(timestamp * 1000));
                    logger.info("timestamp = " + timestamp + " ( " + date + " )");

                    logger.info("Station " + id + ": SID = " + sid + ", dupn = " + dupn + ", len = " + size + ", data = ");

                    sb = new StringBuilder();
                    for (int i = 0; i < size; ++i)
                        sb.append(String.format("%02X ", bytes[idx++]));
                    logger.info(sb.toString());

                    //System.out.println();

                    nbBytes += size;
                }
            }
        }
    }

    private void getPackets() {
        byte[] ack = new byte[2];
        ArrayList<byte[]> allPackets = new ArrayList<byte[]>();

        while (true) {
            byte[] packet = readPacket();

            if (packet == null) {
                logger.error("Error: null packet");
                return;
            }

            if (packet.length == 3 && packet[0] == '+' && packet[1] == '+' && packet[2] == '+') {
                logger.info("Disconnection");
                return;
            }

            if (packet.length == 1 && packet[0] == BYTE_SYNC) {
                allPackets.clear();
                continue;
            }

            if (packet[0] == PACKET_DATA) {
                logger.info("Got a data packet");
                allPackets.add(packet);
                continue;
            }

            if (packet[0] != PACKET_CRC) {
                ack[0] = 1;
                ack[1] = BYTE_NACK;

                logger.error("Error: Expected CRC but got something else");
            } else {
                logger.info("Got a CRC");

                ack[0] = 1;
                ack[1] = BYTE_ACK;

                for (byte[] pkt : allPackets) {
                    //processPacket(pkt);
                    printPacket(pkt);
                }

                allPackets.clear();
            }

            if (!write(ack)) {
                logger.error("Error: Could not send ACK");
                return;
            }
        }
    }

    private boolean write(byte data[]) {
        try {
            mSocket.getOutputStream().write(data);
            mSocket.getOutputStream().flush();
        } catch (Exception e) {
            return false;
        }

        return true;
    }


    public void run() {
        logger.info("New connection from " + mSocket.getInetAddress());

        // RSSI
        byte[] rssi = read(2);

        if (rssi == null) {
            logger.error("Error: Could not receive RSSI");
            return;
        }

        // Auth challenge
        long utc = System.currentTimeMillis() / 1000;
        byte[] challenge = new byte[25];
        Random random = new Random();

        challenge[0] = 24;

        for (int i = 1; i < 17; ++i)
            challenge[i] = (byte) (random.nextInt() & 0xFF);

        challenge[17] = (byte) ((utc >> 24) & 0xFF);
        challenge[18] = (byte) ((utc >> 16) & 0xFF);
        challenge[19] = (byte) ((utc >> 8) & 0xFF);
        challenge[20] = (byte) (utc & 0xFF);
        challenge[21] = 0;
        challenge[22] = 0;

        int crc = crc16(challenge, 1, 22);

        challenge[23] = (byte) ((crc >> 8) & 0xFF);
        challenge[24] = (byte) (crc & 0xFF);

        if (!write(challenge)) {
            logger.error("Error: Could not send challenge");
            return;
        }

        // Reply to challenge
        byte[] authReply = read(7);

        if (authReply == null) {
            logger.error("Error: Could not receive the reply to the challenge");
            return;
        }

        // Process packets
        getPackets();
    }

    private void processPacket(byte[] pkt) {

    }
}
