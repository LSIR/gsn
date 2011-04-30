package gsn.wrappers.tinyos;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SensorScope2Listener implements Runnable {

    private static final int MAX_BUFFER_SIZE = 128;
    private boolean initialized = false;
    private boolean running = false;

    private static SensorScope2Listener instance = null;

    private transient Logger logger = Logger.getLogger(this.getClass());
    private int server_port;
    long counter = 0;

    private ServerSocket serverSocket = null;
    private Socket client = null;

    private Thread thread;

    public ConcurrentLinkedQueue<SensorScope2Packet> queue = new ConcurrentLinkedQueue<SensorScope2Packet>();

    //public SensorScope2Packet

    public void start() {
        if (!running && initialized) {
            this.thread.start();
            running = true;
        } else {
            logger.warn("Listener thread already running or not initialized.");
        }
    }

    public void setPort(int server_port) {
        if (initialized) {
            logger.warn("Port already initialized. (Singleton can be initialized only once)");
            return; // initialize only once
        }
        this.server_port = server_port;
        this.initialized = true;
        // Create a server socket
        logger.warn("Trying to open a server socket on port " + server_port);
        try {
            serverSocket = new ServerSocket(server_port);
        } catch (IOException e) {
            logger.error("Cannot open a server socket on port " + server_port + ".");
            this.initialized = false;
        }
    }

    public static SensorScope2Listener getInstance() {
         if (instance != null) {
             return instance;
         } else {
             instance = new SensorScope2Listener();
             return instance;
         }
    }

    private SensorScope2Listener() {
        this.thread = new Thread(this);
    }

    public void run() {
        if (!initialized)
            return;

        logger.warn("Started SensorScope listener on port " + server_port + ".");

        while (running) {
            byte[] packet = null;

            try {


                // Wait for a  request
                client = serverSocket.accept();

                // Get the streams
                packet = new byte[MAX_BUFFER_SIZE];

                int n_read = client.getInputStream().read(packet);

                byte[] real_packet = new byte[n_read];

                System.arraycopy(packet, 0, real_packet, 0, n_read);

                SensorScope2Packet _packet = new SensorScope2Packet(System.currentTimeMillis(), real_packet);

                queue.add(_packet);

                String order = "[" + String.format("%8d", counter) + "]";

                logger.warn(order + " " + _packet.toString());

                counter++;

            } catch (IOException e) {
                logger.error("Error in Server: " + e.getMessage(), e);
            } finally {
                try {
                    //inbound.close();
                    //outbound.close();
                    client.close();
                    //serverSocket.close();
                } catch (IOException e) {
                    logger.warn("Cannot close stream " + e.getMessage(), e);
                }
            }
        }

    }

    public void stopAcquisition() {
        running = false;
    }
}
