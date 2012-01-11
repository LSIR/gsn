package gsn.utils;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.net.*;
import java.util.*;

public class SensorScopeListenerClient extends Thread
{
    public static final String CONF_LOG4J_SENSORSCOPE_PROPERTIES = "conf/log4j_sensorscope.properties";
    private static transient Logger logger = Logger.getLogger(SensorScopeListenerClient.class);

    private static final byte BYTE_SYNC = 0x7E;
    private static final byte BYTE_ESC  = 0x7D;

    private static final byte BYTE_ACK  = 0x00;
    private static final byte BYTE_NACK = 0x01;

    private static final byte PACKET_DATA = 0x00;
    private static final byte PACKET_CRC  = 0x01;

    private Socket mSocket;

    public SensorScopeListenerClient(Socket socket)
    {
        PropertyConfigurator.configure(CONF_LOG4J_SENSORSCOPE_PROPERTIES);
        mSocket = socket;

        start();
    }

    int crc16Byte(int crc, int b)
    {
        crc = ((crc >> 8) & 0xFF) | (crc << 8);
        crc ^= b;
        crc ^= (crc & 0xFF) >> 4;
        crc ^= crc << 12;
        crc ^= (crc & 0xFF) << 5;

        return crc;
    }

    int crc16(byte[] buffer, int offset, int len)
    {
        int i;
        int crc = 0;

        for(i=offset; i<offset+len; ++i)
            crc = crc16Byte(crc, (int) buffer[i]);

        return crc;
    }

    private byte[] read(int len)
    {
        byte data[] = new byte[len];

        try
        {
            mSocket.getInputStream().read(data);
        }
        catch(Exception e)
        {
            return null;
        }

        return data;
    }

    private byte[] readPacket()
    {
        int     idx    = 0;
        int     len    = 0;
        byte[]  data   = null;
        byte[]  b      = null;
        boolean escape = false;

        while(true)
        {
            b = read(1);

            if(b == null)
                return null;

            if(b[0] == BYTE_SYNC)
                return b;

            if(escape)
            {
                b[0]   ^= 0x20;
                escape  = false;
            }
            else if(b[0] == BYTE_ESC)
            {
                escape = true;
                continue;
            }

            if(data == null)
            {
                len  = b[0];
                data = new byte[len];
            }
            else
            {
                data[idx++] = b[0];

                if(len == '+' && data[0] == '+' && data[1] == '+')
                {
                    data = new byte[3];

                    data[0] = '+';
                    data[1] = '+';
                    data[2] = '+';

                    return data;
                }

                if(idx == len)
                    return data;
            }
        }
    }

    private void printPacket(byte[] pkt)
    {
        if(pkt[1] == 1)
        {
            int bytes[] = new int[pkt.length];

            for(int i=0; i<pkt.length; ++i)
            {
                byte b = pkt[i];

                if(b >= 0) bytes[i] = b;
                else       bytes[i] = 256 + b;

                System.out.print(String.format("%02X ", bytes[i]));
            }

            System.out.println();

            int     id     = (bytes[3] << 8) + bytes[4];
            int     idx    = 5;
            boolean fullTS = true;

            while(true)
            {
                if(idx >= bytes.length)
                    break;

                if(fullTS)
                {
                    idx    += 4;
                    fullTS  = false;
                }
                else
                    ++idx;

                int len     = bytes[idx++];
                int nbBytes = 0;

                while(true)
                {
                    if(nbBytes >= len)
                        break;

                    int sid  = bytes[idx];
                    int dupn = 0;
                    int size = 2;

                    if(sid >= 128)
                    {
                        sid -= 128;

                        if(sid >= 108)
                        {
                            idx     += 2;
                            nbBytes += 2;

                            sid = (sid - 108) * 256 + bytes[idx-1];
                        }
                        else
                        {
                            ++nbBytes;
                            ++idx;
                        }

                        dupn = (bytes[idx] >> 4) & 0x0F;
                        size = (bytes[idx] & 0x0F) + 1;

                        ++idx;
                        ++nbBytes;
                    }
                    else
                    {
                        if(sid >= 108)
                        {
                            idx     += 2;
                            nbBytes += 2;

                            sid  = (sid - 108) * 256 + bytes[idx-1];
                        }
                        else
                        {
                            ++idx;
                            ++nbBytes;
                        }
                    }

                    System.out.print("Station " + id + ": SID = " + sid + ", dupn = " + dupn + ", len = " + size + ", data = ");

                    for(int i=0; i<size; ++i)
                        System.out.print(String.format("%02X ", bytes[idx++]));

                    System.out.println();

                    nbBytes += size;
                }
            }
        }
    }

    private void getPackets()
    {
        byte[]            ack        = new byte[2];
        ArrayList<byte[]> allPackets = new ArrayList<byte[]>();

        while(true)
        {
            byte[] packet = readPacket();

            if(packet == null)
            {
                System.out.println("Error: null packet");
                return;
            }

            if(packet.length == 3 && packet[0] == '+' && packet[1] == '+' && packet[2] == '+')
            {
                System.out.println("Disconnection");
                return;
            }

            if(packet.length == 1 && packet[0] == BYTE_SYNC)
            {
                allPackets.clear();
                continue;
            }

            if(packet[0] == PACKET_DATA)
            {
                System.out.println("Got a data packet");
                allPackets.add(packet);
                continue;
            }

            if(packet[0] != PACKET_CRC)
            {
                ack[0] = 1;
                ack[1] = BYTE_NACK;

                System.out.println("Error: Expected CRC but got something else");
            }
            else
            {
                System.out.println("Got a CRC");

                ack[0] = 1;
                ack[1] = BYTE_ACK;

                for(byte[] pkt: allPackets)
                    printPacket(pkt);

                allPackets.clear();
            }

            if(!write(ack))
            {
                System.out.println("Error: Could not send ACK");
                return;
            }
        }
    }

    private boolean write(byte data[])
    {
        try
        {
            mSocket.getOutputStream().write(data);
            mSocket.getOutputStream().flush();
        }
        catch(Exception e)
        {
            return false;
        }

        return true;
    }


    public void run()
    {
        System.out.println("New connection from " + mSocket.getInetAddress());

        // RSSI
        byte[] rssi = read(2);

        if(rssi == null)
        {
            System.out.println("Error: Could not receive RSSI");
            return;
        }

        // Auth challenge
        long   utc       = System.currentTimeMillis() / 1000;
        byte[] challenge = new byte[25];
        Random random    = new Random();

        challenge[0] = 24;

        for(int i=1; i<17; ++i)
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

        if(!write(challenge))
        {
            System.out.println("Error: Could not send challenge");
            return;
        }

        // Reply to challenge
        byte[] authReply = read(7);

        if(authReply == null)
        {
            System.out.println("Error: Could not receive the reply to the challenge");
            return;
        }

        // Process packets
        getPackets();
    }
}
