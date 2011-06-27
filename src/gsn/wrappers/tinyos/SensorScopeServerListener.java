package gsn.wrappers.tinyos;


import com.izforge.izpack.util.CleanupClient;
import gsn.Main;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class SensorScopeServerListener {

    private static transient Logger logger = Logger.getLogger(SensorScopeServerListener.class);

    private static final int MAX_BUFFER_SIZE = 2048;
    private static final String PASSKEY = "FD83EC5EA68E2A5B";

    private static final int TX_BUFFER_SIZE = 10;
    private static final int RX_BUFFER_SIZE = 100000;
    public static final String CONF_LOG4J_SENSORSCOPE_PROPERTIES = "conf/log4j_sensorscope.properties";

    byte[] mRxBuf;
    byte[] mTxBuf;
    private static int port;

    private int mStationID;
    private static final int CLIMAPS_ID = 0;
    private static final byte BYTE_SYNC = 0x7E;
    private static final byte BYTE_ESC = 0x7D;
    private static final byte PKT_TYPE_DATA = 0x00;
    private static final byte PKT_TYPE_CRC = 0x01;
    private static final byte BYTE_ACK = 0x00;
    private static final byte BYTE_NACK = 0x01;
    private static final byte BUFTYPE_GPRS = 0x00; // TODO: check exact value in sensor.h

    public SensorScopeServerListener() {
        // Create a server socket
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }
        mRxBuf = new byte[RX_BUFFER_SIZE];
        mTxBuf = new byte[TX_BUFFER_SIZE];
        logger.warn("Server initialized.");
    }

    Socket client = null;
    ServerSocket serverSocket = null;

    int receive(byte[] buffer) {
        try {
            return client.getInputStream().read(buffer);
        } catch (IOException e) {
            logger.warn("Exception\n" + e.toString());
            return -1;
        }
    }

    int receive(byte[] buffer, int n) {
        logger.warn("Trying to read " + n + " bytes...");
        try {
            int nb_read = client.getInputStream().read(buffer, 0, n);
            logger.info("Read (" + nb_read + ")");
            return nb_read;
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
            return -1;
        }
    }

    boolean send(byte[] buffer) {
        boolean success = true;
        try {
            OutputStream out = client.getOutputStream();
            out.write(buffer);
            out.flush();
        } catch (IOException e) {
            logger.warn("Exception while trying to send data\n" + e);
            success = false;
        }
        return success;
    }

    boolean send(byte[] buffer, int len) {
        boolean success = true;
        try {
            OutputStream out = client.getOutputStream();
            out.write(buffer, 0, len);
            out.flush();
        } catch (IOException e) {
            logger.warn("Exception while trying to send data\n" + e);
            success = false;
        }
        return success;
    }


    boolean ReceivePacket(int packet, int length) {

        logger.info("ReceivePacket(packet=" + packet + ",length=" + length + ")");


        boolean escape = false;
        boolean lengthOk = false;
        int idx = 0;
        byte _byte[] = new byte[1];
        _byte[0] = 0;


        while (true) {

            if (!ReceiveByte(_byte))
                return false;

            logger.info("byte => " + _byte[0]);

            // Synchronization byte?
            if (_byte[0] == BYTE_SYNC) {
                length = 1;
                mRxBuf[packet + 0] = BYTE_SYNC;    // packet[0] = BYTE_SYNC
                return true;
            }

            // Beware of escaped bytes
            if (escape) {
                _byte[0] ^= 0x20;
                escape = false;
            } else if (_byte[0] == BYTE_ESC) {
                escape = true;
                continue;
            }

            // First 'real' byte is the packet length
            if (lengthOk == false) {
                length = _byte[0];
                lengthOk = true;
            } else {
                mRxBuf[packet + idx++] = _byte[0]; // packet[idx++] = byte;

                // The GPRS sends '+++' upon disconnection
                if (mRxBuf[length] == '+' && mRxBuf[packet + 0] == '+' && mRxBuf[1] == '+') {
                    mRxBuf[length] = 3;
                    mRxBuf[packet + 2] = '+';

                    return true;
                }

                // Do we have a complete packet?
                if (idx == length)
                    return true;
            }
        }
    }

    private boolean ReceiveByte(byte b[]) {
        byte[] _oneByte = new byte[1];
        int n_bytes = receive(_oneByte, 1);
        logger.info("Read (" + n_bytes + ") => " + _oneByte[0]);
        if (n_bytes < 1)
            return false;
        else {
            b[0] = _oneByte[0];
            return true;
        }
    }

    void listArray(byte[] a, int len, String header) {
        logger.warn("* " + header + " *");
        listArray(a, len);
    }

    void listArray(byte[] a, int len) {

        //StringBuilder hex_sb = new StringBuilder();
        StringBuilder hex_sb_2 = new StringBuilder();
        //StringBuilder dec_sb = new StringBuilder();
        StringBuilder dec_sb_2 = new StringBuilder();
        for (int i = 0; (i < a.length && i < len); i++) {
            //hex_sb.append(String.format("%02x", a[i])).append(" ");
            hex_sb_2.append(String.format("%02x", a[i] & 0xff)).append(" ");
            //dec_sb.append(a[i]).append(" ");
            dec_sb_2.append(a[i] & 0xff).append(" ");
        }

        //hex_sb.append("(").append(String.format("%2d", len)).append(")");
        hex_sb_2.append("(").append(String.format("%2d", len)).append(")");
        //dec_sb.append("(").append(String.format("%2d", len)).append(")");
        dec_sb_2.append("(").append(String.format("%2d", len)).append(")");

        //logger.warn(hex_sb.toString());
        logger.info(hex_sb_2.toString());
        //logger.warn(dec_sb.toString());
        logger.info(dec_sb_2.toString());

    }

    void listArray(int[] a, int len, String header) {
        logger.info("* " + header + " *");
        listArray(a, len);
    }

    void listArray(int[] a, int len) {

        //StringBuilder hex_sb = new StringBuilder();
        StringBuilder hex_sb_2 = new StringBuilder();
        //StringBuilder dec_sb = new StringBuilder();
        StringBuilder dec_sb_2 = new StringBuilder();
        for (int i = 0; (i < a.length && i < len); i++) {
            //hex_sb.append(String.format("%02x", a[i])).append(" ");
            hex_sb_2.append(String.format("%02x", a[i] & 0xff)).append(" ");
            //dec_sb.append(a[i]).append(" ");
            dec_sb_2.append(a[i] & 0xff).append(" ");
        }

        //hex_sb.append("(").append(String.format("%2d", len)).append(")");
        hex_sb_2.append("(").append(String.format("%2d", len)).append(")");
        //dec_sb.append("(").append(String.format("%2d", len)).append(")");
        dec_sb_2.append("(").append(String.format("%2d", len)).append(")");

        //logger.warn(hex_sb.toString());
        logger.info(hex_sb_2.toString());
        //logger.warn(dec_sb.toString());
        logger.info(dec_sb_2.toString());

    }


    public int entry() {

        int rssi;
        long rtt;

        int[] challenge = new int[25];
        byte[] buffer = new byte[7];

        if (serverSocket == null) {
            logger.error("Failed connection");
            return 0;
        }

        long counter = 0;

        try {

            // Wait for a  request
            logger.warn("Server listening...");
            client = serverSocket.accept();

            logger.info("Connection from: " + client.getRemoteSocketAddress().toString());

            // Get rssi
            logger.info("Trying to receive RSSI...");

            int n_read = receive(buffer, 2);

            if (n_read < 2) {
                CleanUp("receiving RSSI");
                listArray(buffer, n_read);
                return 0;
            }

            listArray(buffer, n_read);

            int buffer_1 = ((int) buffer[1] & 0xff);

            if (buffer_1 <= 31) rssi = -113 + (2 * buffer_1);
            else rssi = -255;

            logger.info("RSSI = " + rssi);

            // Send the authentication challenge
            FillAuthChallenge(challenge);
            rtt = System.currentTimeMillis();

            if (!send(toByteArray(challenge))) {
                CleanUp("sending authentication challenge");
                return 0;
            }

            // Get the reply to the challenge
            if (receive(buffer, 7) < 0) {
                CleanUp("receiving authentication data");
                return 0;
            }

            listArray(buffer, 7, "Response to challenge");

            rtt = System.currentTimeMillis() - rtt;

            // Check that the station correctly authenticated itself
            if (!CheckAuthentication(PASSKEY, challenge[1], challenge[9], buffer[3], buffer[5])) {
                logger.error("Failed authentication from " + client.getRemoteSocketAddress().toString());
                client.close();
                return 0;
            }

            mStationID = ((int) buffer[1] << 8) + (int) buffer[2];

            if (mStationID == CLIMAPS_ID)
                logger.info("Climaps authenticated (RTT = " + rtt + " ms)");
            else
                logger.info("Station " + mStationID + " (RTT = " + rtt + " ms, RSSI = " + rssi + " dBm)");

            ProcessPackets();

            client.close();

            counter++;

        } catch (IOException ioe) {
            logger.warn("Error in Server: " + ioe);
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                logger.warn("can't close streams" + e.getMessage());
            }
        }
        return 0;
    }

    private void ProcessPackets() {

        logger.info("-- Processing packets --");

        int rxIdx = 0;
        int nbPkts = 0;
        int pktLen = 0;

        while (true) {

            int pkt = mRxBuf[rxIdx + 1];
            logger.info("Trying to receive packets with pkt=" + pkt + " rxIdx=" + rxIdx);
            if (!ReceivePacket(pkt, rxIdx)) {
                CleanUp("receiving packets");
                return;
            }

            // This is a (dirty?) hack to mimic MMC card buffers, where the length includes the length byte itself
            mRxBuf[rxIdx]++;

            pktLen = mRxBuf[rxIdx] - 1;
            rxIdx += pktLen + 1;

            // '+++' means that the GPRS has disconnected
            if (pktLen == 3 && mRxBuf[pkt + 0] == '+' && mRxBuf[pkt + 1] == '+' && mRxBuf[pkt + 2] == '+') {
                if (mStationID == CLIMAPS_ID)
                    logger.info("Climaps has disconnected");
                else
                    logger.info("Station " + mStationID + " has disconnected");
                return;
            }

            // A synchronization byte resets the reception
            if (pktLen == 1 && mRxBuf[pkt + 0] == BYTE_SYNC) {
                rxIdx = 0;
                nbPkts = 0;
                continue;
            }

            // A data packet?
            if (mRxBuf[pkt + 0] == PKT_TYPE_DATA) {
                ++nbPkts;
                continue;
            }

            // At this point, it must be a CRC (3 bytes long)
            if (pktLen != 3 || mRxBuf[pkt + 0] != PKT_TYPE_CRC) {
                mTxBuf[1] = BYTE_NACK;
                logger.warn("Corrupted CRC packet received");
            } else {
                // So far so good, let's check the crc
                int nextPktIdx = 0;
                int crc = 0;
                for (int i = 0; i < rxIdx - 3; ++i) {
                    if (i == nextPktIdx) nextPktIdx += mRxBuf[i];
                    else crc = Crc16Byte(crc, mRxBuf[i]);
                }

                if ((mRxBuf[pkt + 1] << 8 + mRxBuf[pkt + 2]) == crc) {
                    if (nbPkts == 1) {
                        if (mStationID == CLIMAPS_ID)
                            logger.warn("Successfully received a data packet from Climaps");
                        else
                            logger.warn("Successfully received a data packet from station " + mStationID);
                    } else {
                        if (mStationID == CLIMAPS_ID)
                            logger.warn("Successfully received " + nbPkts + " data packet from Climaps");
                        else
                            logger.warn("Successfully received " + nbPkts + "data packet from station " + mStationID);
                    }


                    LogData(ExtractData(mRxBuf, rxIdx - 4, BUFTYPE_GPRS));
                    mTxBuf[1] = BYTE_ACK;
                } else {
                    mTxBuf[1] = BYTE_NACK;
                    logger.error("Invalid CRC received");
                }

            }

            // Once here, in any case, we must send back an ACK or a NACK
            mTxBuf[0] = 1;

            if (!send(mTxBuf, 2)) {
                if (mTxBuf[1] == BYTE_ACK)
                    CleanUp("sending back an ACK");
                else
                    CleanUp("sending back a NACK");
                return;
            }

            // Done with the current batch of packets
            rxIdx = 0;
            nbPkts = 0;
        }
    }

    private void LogData(byte[] bytes) {
        logger.warn("\n\n ***** LOG DATA ***** \n\n");
        listArray(bytes, bytes.length);
    }

    private byte[] ExtractData(byte[] buffer, int len, byte type) {
        return buffer;
    }

    private boolean CheckAuthentication(String passkey, int i, int i1, byte b, byte b1) {
        return true;   //TODO: implement authentication checking method
    }

    private byte[] toByteArray(int[] challenge) {
        byte[] _array = new byte[challenge.length];
        for (int i = 0; i < challenge.length; i++)
            _array[i] = (byte) challenge[i];
        return _array;
    }

    private void FillAuthChallenge(int[] challenge) {
        long utc;
        int crc;

        // Packet size
        challenge[0] = 24;

        Random randomGenerator = new Random();

        for (int i = 1; i < 17; ++i)
            challenge[i] = randomGenerator.nextInt() & 0xff;

        utc = System.currentTimeMillis() / 1000;
        challenge[17] = (int) ((utc >> 24) & 0xFF);
        challenge[18] = (int) ((utc >> 16) & 0xFF);
        challenge[19] = (int) ((utc >> 8) & 0xFF);
        challenge[20] = (int) (utc & 0xFF);
        challenge[21] = 0;
        challenge[22] = 0;

        // CRC
        int[] _challenge = new int[22];

        System.arraycopy(challenge, 1, _challenge, 0, 22);
        listArray(challenge, 24, "challenge");
        listArray(_challenge, 22, "_challenge");

        crc = Crc16(_challenge, 22);
        challenge[23] = (crc >> 8) & 0xFF;
        challenge[24] = crc & 0xFF;

    }


    int Crc16Byte(int crc, int _byte) {
        crc = ((crc >> 8) & 0xFF) | (crc << 8);
        crc ^= _byte;
        crc ^= (crc & 0xFF) >> 4;
        crc ^= crc << 12;
        crc ^= (crc & 0xFF) << 5;

        return crc;
    }


    int Crc16(int[] buffer, int len) {
        int i;
        int crc = 0;

        for (i = 0; i < len; ++i)
            crc = Crc16Byte(crc, buffer[i]);

        return crc;
    }

    void CleanUp(String when) {
        logger.error("Error while " + when);
        try {
            client.close();
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }
    }

    public static void main(java.lang.String[] args) {
        PropertyConfigurator.configure(CONF_LOG4J_SENSORSCOPE_PROPERTIES);
        String str_port = args[0];

        port = Integer.parseInt(str_port);

        logger.warn("Server started on port: " + port);
        SensorScopeServerListener server = new SensorScopeServerListener();
        logger.warn("Entering server mode...");

        while (true) {
            server.entry();
            logger.warn("\n\n********************\n\n");
        }
    }

}

