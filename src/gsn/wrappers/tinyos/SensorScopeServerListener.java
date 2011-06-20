package gsn.wrappers.tinyos;


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

    public static final int PORT = 3000;
    private static final int MAX_BUFFER_SIZE = 2048;
    private static final String PASSKEY = "FD83EC5EA68E2A5B";

    private static final int TX_BUFFER_SIZE = 10;
    private static final int RX_BUFFER_SIZE = 100000;

    byte[] mRxBuf;
    byte[] mTxBuf;


    public SensorScopeServerListener() {
        mRxBuf = new byte[RX_BUFFER_SIZE];
        mTxBuf = new byte[TX_BUFFER_SIZE];
    }

    Socket client = null;
    ServerSocket serverSocket = null;

    int receive(byte[] buffer) {
        try {
            return client.getInputStream().read(buffer);
        } catch (IOException e) {
            System.out.println("Exception\n" + e.toString());
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
            System.out.println("Exception\n"+e);
            success = false;
        }
        return success;
    }

    void ReceivePacket() {

    }

    void listArray(byte[] a, int len, String header) {
        System.out.println("* " + header + " *");
        listArray(a, len);
    }

    void listArray(byte[] a, int len) {

        StringBuilder hex_sb = new StringBuilder();
        StringBuilder hex_sb_2 = new StringBuilder();
        StringBuilder dec_sb = new StringBuilder();
        StringBuilder dec_sb_2 = new StringBuilder();
        for (int i = 0; (i < a.length && i < len); i++) {
            hex_sb.append(String.format("%02x", a[i])).append(" ");
            hex_sb_2.append(String.format("%02x", a[i] & 0xff)).append(" ");
            dec_sb.append(a[i]).append(" ");
            dec_sb_2.append(a[i] & 0xff).append(" ");
        }

        hex_sb.append("(").append(String.format("%2d", len)).append(")");
        hex_sb_2.append("(").append(String.format("%2d", len)).append(")");
        dec_sb.append("(").append(String.format("%2d", len)).append(")");
        dec_sb_2.append("(").append(String.format("%2d", len)).append(")");

        System.out.println(hex_sb.toString());
        System.out.println(hex_sb_2.toString());
        System.out.println(dec_sb.toString());
        System.out.println(dec_sb_2.toString());

    }


    public void entry() {

        int rssi;
        long rtt;

        byte[] challenge = new byte[25];


        long counter = 0;

        try {
            // Create a server socket
            serverSocket = new ServerSocket(PORT);

            while (true) {
                // Wait for a  request
                client = serverSocket.accept();

                logger.warn("Connection from: " + client.getRemoteSocketAddress().toString());

                // Get rssi
                logger.warn("Trying to receive RSSI...");
                byte[] buffer = new byte[MAX_BUFFER_SIZE];
                int n_read = receive(buffer);

                if (n_read < 2) {
                    logger.warn("Couldn't get RSSI");
                    listArray(buffer, n_read);
                    continue;
                }

                listArray(buffer, n_read);

                int buffer_1 = (buffer[1] & 0xff);

                if (buffer_1 <= 31) rssi = -113 + (2 * buffer_1);
                else rssi = -255;

                System.out.println("RSSI = " + rssi);

                // Send the authentication challenge
                FillAuthChallenge(challenge);
                rtt = System.currentTimeMillis();

                //send

                counter++;
            }
        } catch (IOException ioe) {
            logger.warn("Error in Server: " + ioe);
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                logger.warn("can't close streams" + e.getMessage());
            }
        }
    }

    private void FillAuthChallenge(byte[] challenge) {
        long utc;
        int crc;

        // Packet size
        challenge[0] = 24;

        Random randomGenerator = new Random();

        for (int i = 1; i < 17; ++i)
            challenge[i] = (byte) (randomGenerator.nextInt() & 0xff);

        utc = System.currentTimeMillis() / 1000;
        challenge[17] = (byte) ((utc >> 24) & 0xFF);
        challenge[18] = (byte) ((utc >> 16) & 0xFF);
        challenge[19] = (byte) ((utc >> 8) & 0xFF);
        challenge[20] = (byte) (utc & 0xFF);
        challenge[21] = 0;
        challenge[22] = 0;

        // CRC
        byte[] _challenge = new byte[22];

        System.arraycopy(challenge, 1, _challenge, 0, 22);
        listArray(challenge, 24, "challenge");
        listArray(_challenge, 22, "_challenge");

        crc = Crc16(_challenge, 22);
        challenge[23] = (byte) ((crc >> 8) & 0xFF);
        challenge[24] = (byte) (crc & 0xFF);

    }


    int Crc16Byte(int crc, byte _byte) {
        crc = ((crc >> 8) & 0xFF) | (crc << 8);
        crc ^= _byte;
        crc ^= (crc & 0xFF) >> 4;
        crc ^= crc << 12;
        crc ^= (crc & 0xFF) << 5;

        return crc;
    }


    int Crc16(byte[] buffer, int len) {
        int i;
        int crc = 0;

        for (i = 0; i < len; ++i)
            crc = Crc16Byte(crc, buffer[i]);

        return crc;
    }

    public static void main(java.lang.String[] args) {
        PropertyConfigurator.configure(Main.DEFAULT_GSN_LOG4J_PROPERTIES);
        SensorScopeServerListener server = new SensorScopeServerListener();
        logger.warn("Entering server mode...");
        server.entry();
    }
}

