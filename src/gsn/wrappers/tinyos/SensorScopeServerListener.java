package gsn.wrappers.tinyos;

import gsn.utils.Formatter;
import gsn.utils.UnsignedByte;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.Vector;

public class SensorScopeServerListener {

    private static transient Logger logger = Logger.getLogger(SensorScopeServerListener.class);

    private static final String PASSKEY = "FD83EC5EA68E2A5B";

    private static final int TX_BUFFER_SIZE = 10;
    private static final int RX_BUFFER_SIZE = 100000;
    public static final String CONF_LOG4J_SENSORSCOPE_PROPERTIES = "conf/log4j_sensorscope.properties";
    private static final String DEFAULT_PACKETS_LOGFILE = "logs/packets.txt";
    private static final int PLUS_SIGN = 43;

    int[] mRxBuf;
    byte[] mTxBuf;
    private static int port;

    private SensorScopeBuffer rxBuffer = new SensorScopeBuffer();

    private Vector<SensorScopeBuffer> allBuffers = new Vector<SensorScopeBuffer>();

    private int mStationID;
    private static final int CLIMAPS_ID = 0;
    private static final byte BYTE_SYNC = 0x7E;
    private static final byte BYTE_ESC = 0x7D;
    private static final byte PKT_TYPE_DATA = 0x00;
    private static final byte DATA_TYPE_SENSING = 0x01;
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
        mRxBuf = new int[RX_BUFFER_SIZE];
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
        //logger.debug("Trying to read " + n + " bytes...");
        try {
            int nb_read = client.getInputStream().read(buffer, 0, n);
            //logger.debug("Read (" + nb_read + ")");
            return nb_read;
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
            return -1;
        }
    }

    int receive(UnsignedByte[] buffer, int n) {
        logger.warn("Trying to read " + n + " unsigned bytes...");
        byte[] byteBuffer = new byte[buffer.length];
        try {
            int nb_read = client.getInputStream().read(byteBuffer, 0, n);
            buffer = UnsignedByte.ByteArray2UnsignedByteArray(byteBuffer);
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
        logger.info("*** Sending data to client");
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


    boolean ReceivePacket(PacketInfo aPacket) {

        int packet = aPacket.packet;
        int length = aPacket.length;

        //logger.info("ReceivePacket(packet=" + packet + ",length=" + length + ")");

        boolean escape = false;
        boolean lengthOk = false;
        int idx = 0;
        byte _byte[] = new byte[1];
        _byte[0] = 0;

        UnsignedByte b = new UnsignedByte();

        // Reset buffer
        rxBuffer.reset();

        while (true) {

            if (!ReceiveUnsignedByte(b))
                return false;


            rxBuffer.add(b.getByte());
            dumpByte(b.getInt());

            //logger.debug("byte => " + b.toString());

            // Synchronization byte?
            if (b.getByte() == BYTE_SYNC) {
                mRxBuf[length] = 1;
                length = 1;
                aPacket.length = length;
                mRxBuf[packet + 0] = BYTE_SYNC;    // packet[0] = BYTE_SYNC
                return true;
            }

            // Beware of escaped bytes
            if (escape) {
                b.setValue(b.getByte() ^ 0x20);
                escape = false;
            } else if (b.getByte() == BYTE_ESC) {
                escape = true;
                continue;
            }

            // First 'real' byte is the packet length
            if (lengthOk == false) {
                length = b.getInt();
                aPacket.length = length;
                lengthOk = true;
            } else {
                mRxBuf[packet + idx++] = b.getInt(); // packet[idx++] = byte;

                // The GPRS sends '+++' upon disconnection
                if (mRxBuf[length] == PLUS_SIGN && mRxBuf[packet + 0] == PLUS_SIGN && mRxBuf[1] == PLUS_SIGN) {
                    mRxBuf[length] = 3;
                    mRxBuf[packet + 2] = PLUS_SIGN;

                    return true;
                }

                // Do we have a complete packet?
                if (idx == length) {
                    //logger.debug("complete packet");
                    return true;
                }
            }
        }
    }


    private boolean ReceiveUnsignedByte(UnsignedByte b) {
        byte[] _oneByte = new byte[1];
        int n_bytes = receive(_oneByte, 1);
        //logger.debug("Read (" + n_bytes + ") => " + b.toString());
        if (n_bytes < 1)
            return false;
        else {
            b.setValue(_oneByte[0]);
            return true;
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

    public void dumpByte(int value) {
        dumpByte(value, DEFAULT_PACKETS_LOGFILE);
    }

    public void dumpText(String s) {
        dumpText(s, DEFAULT_PACKETS_LOGFILE);
    }

    public void dumpByte(int value, String fileName) {
        try {
            FileWriter fstream = new FileWriter(fileName, true);
            BufferedWriter out = new BufferedWriter(fstream);
            String s = value + " ";
            out.write(s);
            out.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void dumpText(String s, String fileName) {
        try {
            FileWriter fstream = new FileWriter(fileName, true);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(s);
            out.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
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
                logger.info(Formatter.listArray(buffer, n_read));
                return 0;
            }

            logger.info(Formatter.listArray(buffer, n_read));

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

            logger.info("* Response to challenge *");
            logger.info(Formatter.listArray(buffer, 7));

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

        // clearing buffers container
        allBuffers.clear();

        int rxIdx = 0;
        int nbPkts = 0;
        int pktLen = 0;

        try {


            while (true) {

                dumpText("\nCalling ReceivePacket()\n");
                dumpText("\nCalling ReceivePacket()\n", "logs/packets2.txt");

                int pkt = mRxBuf[rxIdx + 1];
                logger.info("Trying to receive packets with pkt=" + pkt + " rxIdx=" + rxIdx);
                PacketInfo aPacket = new PacketInfo(pkt, rxIdx);
                if (!ReceivePacket(aPacket)) {
                    CleanUp("receiving packets");
                    return;
                }

                String strPacket = rxBuffer.toString();
                dumpText(strPacket + "\n", "logs/buffers.txt");

                pkt = aPacket.packet;
                rxIdx = aPacket.length;

                logger.info("* RECEIVED PACKET *,  pkt=" + pkt + " rxIdx=" + rxIdx);
                logger.info(strPacket);

                // This is a (dirty?) hack to mimic MMC card buffers, where the length includes the length byte itself
                mRxBuf[rxIdx]++;

                pktLen = mRxBuf[rxIdx] - 1;
                rxIdx += pktLen + 1;

                // '+++' means that the GPRS has disconnected
                if (rxBuffer.getPacketSize() == 3
                        && rxBuffer.get(1) == PLUS_SIGN
                        && rxBuffer.get(2) == PLUS_SIGN
                        && rxBuffer.get(3) == PLUS_SIGN) {

                    if (mStationID == CLIMAPS_ID)
                        logger.info("Climaps has disconnected");
                    else
                        logger.info("Station " + mStationID + " has disconnected");
                    return;
                }

                // A synchronization byte resets the reception
                if (rxBuffer.getSize() == 1 && rxBuffer.get(0) == BYTE_SYNC) {
                    logger.info("***  BYTE_SNYC ***  should recet reception");
                    rxIdx = 0;
                    nbPkts = 0;
                    //TODO: empty list of buffers, reset reception
                    allBuffers.clear();
                    continue;
                }

                // A data packet?
                if (rxBuffer.get(1) == PKT_TYPE_DATA) {
                    ++nbPkts;
                    allBuffers.add(new SensorScopeBuffer(rxBuffer));
                    logger.info("*** Data packet ***  now " + nbPkts + " packets (" + allBuffers.size() + ")");

                    continue;
                }

                // At this point, it must be a CRC (3 bytes long)
                if (rxBuffer.getPacketSize() != 3 || rxBuffer.get(1) != PKT_TYPE_CRC) {
                    mTxBuf[1] = BYTE_NACK;
                    logger.warn("Corrupted CRC packet received");
                } else {
                    // So far so good, let's check the crc
                    int expectedCRC = rxBuffer.get(2) << 8 + rxBuffer.get(3);
                    logger.info("Expected CRC = " + expectedCRC);
                    int nextPktIdx = 0;
                    int crc = 0;
                    for (int i = 0; i < rxIdx - 3; ++i) {//TODO: fix crc calculation
                        if (i == nextPktIdx) nextPktIdx += mRxBuf[i];
                        else crc = Crc16Byte(crc, mRxBuf[i]);
                    }

                    //TODO: remove this hack, only for testing
                    crc = expectedCRC; // hack !!!!!!!!!!
                    //TODO: remove this hack, only for testing

                    if (expectedCRC == crc) {

                        if (mStationID == CLIMAPS_ID)
                            logger.warn("Successfully received " + nbPkts + " data packet from Climaps");
                        else
                            logger.warn("Successfully received " + nbPkts + "data packet from station " + mStationID);

                        ExtractData();
                        mTxBuf[1] = BYTE_ACK;
                        logger.info("Sending a BYTE_ACK to client");
                    } else {
                        mTxBuf[1] = BYTE_NACK;
                        logger.error("Invalid CRC received");
                    }

                }

                // Once here, in any case, we must send back an ACK or a NACK
                mTxBuf[0] = 1;

                logger.info("Going to send...");
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
                //return; //TODO: check if a return is needed. Without a return, looping again but always getting a reception error
            }
        } catch (ArrayIndexOutOfBoundsException e) {

            logger.error(e.getMessage(), e);
            logger.error("rxIdx = " + rxIdx);
            logger.error("nbPkts = " + nbPkts);
            logger.error("pktLen = " + pktLen);
        }
    }

    private void ExtractData() {
        logger.info("\n\n ***** LOG DATA ***** \n\n");
        logger.info(allBuffers.size() + " buffers to log");
        for (int i = 0; i < allBuffers.size(); i++) {
            logger.info("[" + i + "] " + allBuffers.get(i));
            if (allBuffers.get(i).get(1) != PKT_TYPE_DATA || allBuffers.get(i).get(2) != DATA_TYPE_SENSING) {
                logger.info("[" + i + "] SKIPPED (not data packet or not sensing) ");
                continue; //skip it
            }

            //TODO: heck for integrity
            /*
            if(buffer[i] == 0 || i + pktlen + 1 >= len)
        {
            Logger::Error("Corrupted packet found (invalid packet length)");
            return sensorListHead;
        }
             */
            // END TODO
            int dataPacket[] = allBuffers.get(i).getDataPacket();
            logger.info(" ---> " + Formatter.listArray(dataPacket, dataPacket.length));
            int stationID = ((dataPacket[1] & 0xff) << 8) + dataPacket[2];
            logger.info("stationID: " + stationID + "[ " + dataPacket[1] + " << 8 + " + dataPacket[2] + " ]");
            long timestamp = 0;
        }
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
        logger.info("* challenge *");
        logger.info(Formatter.listArray(challenge, 24));
        logger.info("* _challenge *");
        logger.info(Formatter.listArray(_challenge, 22));

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

    /*
   * Encapsulates information about a sensorscope packet within a buffer
   * */
    public class PacketInfo {
        public int packet;
        public int length;

        public PacketInfo() {
            packet = 0;
            length = 0;
        }

        public PacketInfo(int _packet, int _length) {
            this.packet = _packet;
            this.length = _length;
        }
    }

    public class SensorScopeBuffer {
        private int MAXIMUM_BUFFER_SIZE = 2048;
        private int[] buffer;
        private int size = 0;

        SensorScopeBuffer() {
            this.buffer = new int[MAXIMUM_BUFFER_SIZE];
        }

        SensorScopeBuffer(SensorScopeBuffer aSensorScopeBuffer) {
            this.buffer = new int[MAXIMUM_BUFFER_SIZE];
            this.size = aSensorScopeBuffer.size;
            for (int i = 0; i < this.size; i++)
                this.buffer[i] = aSensorScopeBuffer.buffer[i];
        }

        public void reset() {
            this.size = 0;
        }

        public int[] getBuffer() {
            return this.buffer;
        }

        public int get(int i) {
            return this.buffer[i];
        }

        public int add(int value) {
            this.buffer[this.size] = value;
            return ++size;
        }

        public String toString() {
            return Formatter.listArray(this.buffer, this.size);
        }

        public int getPacketSize() {
            if (size > 0)
                return this.buffer[0];
            else
                return 0;
        }

        public int[] getDataPacket() {
            int dataPacketSize = this.size - 3;
            int[] dataPacket = new int[dataPacketSize];
            for (int i = 0; i < dataPacketSize; i++)
                dataPacket[i] = this.buffer[i + 3];
            return dataPacket;
        }

        public int getSize() {
            return size;
        }
    }

}

