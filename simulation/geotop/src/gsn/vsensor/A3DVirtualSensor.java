package gsn.vsensor;

import gsn.vsensor.AbstractVirtualSensor;
import gsn.utils.TimeOutThread;
import gsn.utils.RemoteExecThread;
import gsn.beans.StreamElement;
import org.apache.log4j.Logger;


import gsn.beans.InputStream;
import gsn.beans.DataField;
import gsn.storage.DataEnumerator;
import gsn.storage.StorageManager;

import java.util.*;
import java.sql.SQLException;
import java.io.*;
import java.text.ParseException;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.SCPClient;

public class A3DVirtualSensor extends AbstractVirtualSensor implements Runnable {

    private static final String A3D_HOSTNAME_PARAM = "hostname"; // hostname of server running Alpine3D
    private static final String A3D_USERNAME_PARAM = "username"; // username for running Alpine3D
    private static final String A3D_PASSWORD_PARAM = "password"; // password for running Alpine3D
    private static final String A3D_SCRIPTNAME_PARAM = "scriptname"; // name of script to be invoked through ssh
    private static final String A3D_WINDOWSIZE_PARAM = "window-size"; // size of the window needed for one simulation step (number of tuples)
    private static final long WINDOW_SIZE_UNIT = 1000; // window-size is specified in seconds (1000 msec)
    private static final String A3D_TIMEOUT_PARAM = "timeout"; // timedOut for A3D response
    private static final String A3D_CONTROL_PORT_PARAM = "control-port"; // UDP port for notifications
    private static final String A3D_START_FROM_PARAM = "start-from"; // initial timestamp
    private static final String A3D_REMOTE_BASE_DIR_PARAM = "base-dir"; // initial timestamp
    private static final String A3D_LOCAL_WORKING_DIR_PARAM = "local-working-dir"; // initial timestamp
    private static final String A3D_SCRIPT_TYPE_PARAM = "script-type"; // initial timestamp

    private static final int SCRIPT_TYPE_A3D = 0;
    private static final int SCRIPT_TYPE_GEOTOP = 1;

    private static final String ISO_8601_TIMEFORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    private static final String TIME_FORMAT = "yyyyMMdd'T'HHmm";

    // ./geotop ./ --recover=20040807T1400  --endDate=20040807T1600 --meteoio

    private String hostname;
    private String username;
    private String password;
    private String scriptname;
    private static long windowSize = -1;
    private long timeoutLimit = -1;
    private int controlPort;
    private String remoteBaseDir;
    private int scriptType;
    private String localWorkingDir;

    private final static long DEFAULT_INITIAL_TS = 1244592000000L;
    private static long initial_ts = DEFAULT_INITIAL_TS;

    //1248048000000L;  // Mon, 20 Jul 2009 00:00:00 GMT
    private static long beginTimestamp = -1;
    private static long endTimestamp = -1;

    private TimeOutThread timeOutThread = null;
    private RemoteExecThread remoteExecThread = null;

    private boolean timedOut = false;           // boolean for timeoutLimit of script execution
    private boolean finishedExecution = false; // remote execution of script finished
    private int result = -1;                 // result of remote execution

    private static final int NORMAL_EXEC_TERMINATION = 0;
    private static final int ABNORMAL_EXEC_TERMINATION = -1;

    private static final transient Logger logger = Logger.getLogger(A3DVirtualSensor.class);

    private Vector<StreamElement> streams;

    private static Set<String> monitoredSensors;                // must be static, shared by all Timer threads for updating input-streams
    private static HashMap<String, Long> latestProducedTimestamp;  // must be static, shared by all Timer threads for updating input-streams
    private static HashMap<String, Long> latestProcessedTimestamp; // must be static, shared by all Timer threads for updating input-streams

    private static int threadCount = 0;
    private static Thread schedulerThread; // must be static, shared by all Timer threads for updating input-streams

    private static int currentIteration = 0; // must be static
    private static boolean initialized = false;

    private boolean isActive = true;


    /**
     * **************************************
     * simple lock mechanism
     */
    private Object lock = new Object();
    private boolean locked = false;


    private void lock() {
        while (locked) {
            try {
                lock.wait();
            } catch (InterruptedException ie) {
            }
            locked = true;
        }
    }

    private void unlock() {
        locked = false;
        lock.notifyAll();
    }
    /*
    * simple lock mechanism
    *****************************************/

    public boolean initialize() {

        if (initialized) // already initialized through another instance
            return true;
        else
            initialized = true;

        TreeMap<String, String> params = getVirtualSensorConfiguration().getMainClassInitialParams();

        // Initializing parameters from VSD file

        int nbsensors = getVirtualSensorConfiguration().getInputStreams().size(); // number of sensors being watched

        logger.warn("Number of sensors being monitored: " + nbsensors);

        if (monitoredSensors == null) // if not already initialized by another thread
            monitoredSensors = new TreeSet<String>();

        if (latestProducedTimestamp == null) // if not already initialized by another thread
            latestProducedTimestamp = new HashMap<String, Long>();

        if (latestProcessedTimestamp == null)// if not already initialized by another thread
            latestProcessedTimestamp = new HashMap<String, Long>();

        for (int i = 0; i < nbsensors; i++) {
            String key = ((InputStream) (getVirtualSensorConfiguration().getInputStreams().toArray()[i])).getInputStreamName();
            monitoredSensors.add(key);
            logger.warn(i + " => " + key);

            //getNumberofElementsWithinBounds(key, 0, 2248048000000L);
        }

        hostname = params.get(A3D_HOSTNAME_PARAM);
        if (hostname == null) {
            logger.error("The required parameter: >" + A3D_HOSTNAME_PARAM + "< is missing from the virtual sensor configuration file.");
            return false;
        }

        username = params.get(A3D_USERNAME_PARAM);
        if (username == null) {
            logger.error("The required parameter: >" + A3D_USERNAME_PARAM + "< is missing from the virtual sensor configuration file.");
            return false;
        }

        password = params.get(A3D_PASSWORD_PARAM);
        if (password == null) {
            logger.error("The required parameter: >" + A3D_PASSWORD_PARAM + "< is missing from the virtual sensor configuration file.");
            return false;
        }

        scriptname = params.get(A3D_SCRIPTNAME_PARAM);
        if (scriptname == null) {
            logger.error("The required parameter: >" + A3D_SCRIPTNAME_PARAM + "< is missing from the virtual sensor configuration file.");
            return false;
        }

        remoteBaseDir = params.get(A3D_REMOTE_BASE_DIR_PARAM);
        if (remoteBaseDir == null) {
            logger.error("The required parameter: >" + A3D_REMOTE_BASE_DIR_PARAM + "< is missing from the virtual sensor configuration file.");
            return false;
        }
        logger.warn("Remote base dir: " + remoteBaseDir);

        localWorkingDir = params.get(A3D_LOCAL_WORKING_DIR_PARAM);
        if (localWorkingDir == null) {
            logger.error("The required parameter: >" + A3D_LOCAL_WORKING_DIR_PARAM + "< is missing from the virtual sensor configuration file.");
            return false;
        }
        logger.warn("Local working dir: " + localWorkingDir);

        String str_scriptType = params.get(A3D_SCRIPT_TYPE_PARAM);
        if (str_scriptType == null) {
            logger.error("The required parameter: >" + A3D_SCRIPT_TYPE_PARAM + "< is missing from the virtual sensor configuration file.");
            return false;
        } else if (str_scriptType.equalsIgnoreCase("a3d"))
            scriptType = SCRIPT_TYPE_A3D;
        else if (str_scriptType.equalsIgnoreCase("geotop"))
            scriptType = SCRIPT_TYPE_GEOTOP;
        else {
            logger.error("Unknown script type: >" + str_scriptType + "<");
            return false;
        }
        logger.warn("Script type: " + str_scriptType + " (" + scriptType + ")");

        String window_size_str = params.get(A3D_WINDOWSIZE_PARAM);
        if (window_size_str != null) {
            windowSize = Long.parseLong(window_size_str) * WINDOW_SIZE_UNIT;
            logger.warn("Window Size = " + windowSize + " ms");
        } else {
            logger.error("The required parameter: >" + A3D_WINDOWSIZE_PARAM + "< is missing from the virtual sensor configuration file.");
            return false;
        }

        String timeout_str = params.get(A3D_TIMEOUT_PARAM);
        if (timeout_str != null) {
            timeoutLimit = Long.parseLong(timeout_str);
        } else {
            logger.error("The required parameter: >" + A3D_TIMEOUT_PARAM + "< is missing from the virtual sensor configuration file.");
            return false;
        }

        String control_port_str = params.get(A3D_CONTROL_PORT_PARAM);
        if (control_port_str != null) {
            controlPort = Integer.parseInt(control_port_str);
            logger.warn("Control port = " + controlPort);
        } else {
            logger.error("The required parameter: >" + A3D_CONTROL_PORT_PARAM + "< is missing from the virtual sensor configuration file.");
            return false;
        }

        String start_from_str = params.get(A3D_START_FROM_PARAM);
        if (start_from_str != null) {
            long epoch = -1;
            try {
                epoch = new java.text.SimpleDateFormat(ISO_8601_TIMEFORMAT).parse(start_from_str).getTime();
            } catch (ParseException e) {
                logger.error(e.getMessage(), e);
            }
            if (epoch != -1)
                initial_ts = epoch;
            else
                logger.warn("using default initial timestamp");
            logger.warn("Initial timestamp = " + initial_ts + " => " + timestampInAlpine3DFormat(initial_ts));
        } else {
            logger.error("The required parameter: >" + A3D_START_FROM_PARAM + "< is missing from the virtual sensor configuration file.");
            return false;
        }

        streams = new Vector<StreamElement>();

        if (schedulerThread == null) { // allow only one thread ~Singelton
            schedulerThread = new Thread(this);
            schedulerThread.setName("A3DVirtualSensor-Scheduler-" + threadCount++);
            schedulerThread.start();
        }

        if (beginTimestamp == -1) { // if not yet initialized by any other thread
            beginTimestamp = initial_ts;
        }

        if (endTimestamp == -1) { // if not yet initialized by any other thread
            endTimestamp = initial_ts + windowSize;
        }

        logger.warn("Begin timestamp: " + timestampInAlpine3DFormat(beginTimestamp) + " " + beginTimestamp); //TODO: read from xml file
        logger.warn("Next timestamp: " + timestampInAlpine3DFormat(endTimestamp) + " " + endTimestamp);

        return true;
    }


    public void run() {
        while (isActive) // while isactive
        {
            logger.warn("Checking if enough data is available ...");

            listSensorStatus();

            if (allSensorsExceededCurrentWindow()) {

                if (enoughData()) { // enough elements to launch simulation


                    // ./geotop ./ --recover=20040807T1400  --endDate=20040807T1600 --meteoio

                    String cmd = "cd " + remoteBaseDir + " ; " + scriptname + " " + timestampInAlpine3DFormat(beginTimestamp) + " " + timestampInAlpine3DFormat(endTimestamp);

                    logger.warn(" ENOUGH DATA : runnning => " + cmd);

                    remoteExecThread = new RemoteExecThread(cmd, hostname, username, password, controlPort);
                    timeOutThread = new TimeOutThread(timeoutLimit);

                    remoteExecThread.start();
                    timeOutThread.start();

                    finishedExecution = false;
                    timedOut = false;

                    while (!timedOut && !finishedExecution) {
                        try {
                            Thread.sleep(10000);
                            finishedExecution = remoteExecThread.isFinished();
                            timedOut = timeOutThread.isTimedOut();
                        } catch (InterruptedException e) {
                            logger.warn(e.getMessage(), e);
                        }
                        logger.warn("(timedOut, finished) => (" + timedOut + "," + finishedExecution + ")");
                    }

                    logger.warn("(timedOut, finished) => (" + timedOut + "," + finishedExecution + ")");

                    //verify what have actually happened

                    result = remoteExecThread.getResult();

                    logger.warn("Remote script terminated with : " + result);

                    if (finishedExecution && result == NORMAL_EXEC_TERMINATION) {

                        timeOutThread.stopMe();
                        boolean dataReceivedCorrectly = fetchData();

                        if (dataReceivedCorrectly) {
                            postData(result);
                            //prepare for next iteration
                            logger.warn("Everything ok");
                            gotoNextIteration();
                        } else {
                            logger.warn("Needs to run current iteration again (results not received correctly)");
                        }

                        // else : start over again !!!


                    } else if (finishedExecution && result != NORMAL_EXEC_TERMINATION) {
                        timeOutThread.stopMe();
                        logger.warn("Needs to run current iteration again (remote script terminated abnormaly)");
                        // repeat iteration
                    } else if (timedOut) {
                        logger.warn("Needs to run current iteration again (remote script timed out)");
                    }
                    finishedExecution = false;
                    timedOut = false;

                } else {
                    logger.warn("Should skip current iteration (not enough data in some sensors)");
                    gotoNextIteration();
                    // not enough data : skeep current timestamp ?
                }
            } else {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }


    ///////////////////////////////////
    private String getRemotePathForFlows() {
        return remoteBaseDir + "output/tabs/01_flows.txt";
    }

    private String getRemotePathForChart() {
        switch(scriptType) {
            case SCRIPT_TYPE_A3D: return remoteBaseDir + "output/images/"+timestampInRFormat(beginTimestamp)+".alb.png";
            case SCRIPT_TYPE_GEOTOP: return remoteBaseDir + "output/chart.png";
            default: return remoteBaseDir + "output/chart.png";
        }
    }

    private String getLocalPathForFlows() {
        return localWorkingDir + "01_flows.txt";
    }

    private String getLocalPathForChart() {
        return localWorkingDir + timestampInRFormat(beginTimestamp)+".alb.png";
    }
    ///////////////////////////////////

    /*
    * Fetching data remotely with SCP
    * */

    private boolean fetchData() {

        boolean status = true;

        switch (scriptType) {

            case SCRIPT_TYPE_GEOTOP:
                if (!getRemoteFile(getRemotePathForFlows(), localWorkingDir)) {
                    logger.warn("Failed in fetching file: " + getRemotePathForFlows());
                    status = false;
                }
                if (!getRemoteFile(getRemotePathForChart(), localWorkingDir)) {
                    logger.warn("Failed in fetching file: " + getRemotePathForChart());
                    status = false;
                }
                break;

            case SCRIPT_TYPE_A3D:
                if (!getRemoteFile(getRemotePathForChart(), localWorkingDir)) {
                    logger.warn("Failed in fetching file: " + getRemotePathForChart());
                    status = false;
                }
                break;
        }

        return status;
    }

    /*
    * Posting data to database
    * */
    private boolean postData(int result) {

        boolean success = true;

        Serializable[] stream = new Serializable[2];

        stream[0] = result;

        try {
            FileInputStream fileinputstream = null;
            fileinputstream = new FileInputStream(getLocalPathForChart());
            int numberBytes = fileinputstream.available();
            logger.warn("Chart file has size: " + numberBytes + " bytes");
            byte bytearray[] = new byte[numberBytes];
            stream[1] = bytearray;
            fileinputstream.read(bytearray);
            fileinputstream.close();
        } catch (FileNotFoundException e) {
            logger.warn("Couldn't find chart file: " + getLocalPathForChart());
            logger.warn(e.getMessage(), e);
            success = false;
        } catch (IOException e) {
            logger.warn("Couldn't read chart file: " + getLocalPathForChart());
            logger.warn(e.getMessage(), e);
            success = false;
        }

        StreamElement se = new StreamElement(new DataField[]{new DataField("status", "INTEGER", "status"),
                new DataField("chart", "binary:image/png", "chart")},
                stream,
                beginTimestamp);
        if (success)
            dataProduced(se);

        if (scriptType == SCRIPT_TYPE_GEOTOP) {
            parseFlowFile();
        }

        return success;
    }

    /*
    * Parses flow file, received from GeoTop
    * */
    private boolean parseFlowFile() {
        boolean success = true;

        try {
            File f = new File(getLocalPathForFlows());
            FileReader fr = null;
            fr = new FileReader(f);
            BufferedReader br = new BufferedReader(fr);
            StringBuffer sb = new StringBuffer();
            String eachLine = br.readLine();
            int j = 0;
            String lastestLine = null;
            while (eachLine != null) {
                sb.append(j++ + " " + eachLine);
                sb.append("\n");
                lastestLine = eachLine;
                eachLine = br.readLine();
            }
            //System.out.println(sb.toString());

            if (lastestLine != null) {
                String[] s = lastestLine.trim().split(",");
                /*for (int i = 0; i < s.length; i++) {
                    System.out.println(i + " " + s[i]);
                } */
                if (s.length >= 7) {
                    double q_tot = Double.parseDouble(s[3]);
                    double q_sub_ch = Double.parseDouble(s[4]);
                    double q_sup_ch = Double.parseDouble(s[5]);
                    double q_g = Double.parseDouble(s[6]);

                    logger.warn("q_tot = "+ q_tot);
                    logger.warn("q_sub_ch = "+ q_sub_ch);
                    logger.warn("q_sup_ch = "+ q_sup_ch);
                    logger.warn("q_g = "+ q_g);

                } else success = false;
            } else {
                success = false;
            }

        } catch (FileNotFoundException e) {
            logger.warn("Couldn't find flows file: " + getLocalPathForFlows());
            logger.warn(e.getMessage(), e);
            success = false;
        } catch (IOException e) {
            logger.warn("Couldn't read from flows file: " + getLocalPathForFlows());
            logger.warn(e.getMessage(), e);
            success = false;
        } catch (NumberFormatException e) {
            logger.warn("Error while parsing flows file: " + getLocalPathForFlows());
            logger.warn(e.getMessage(), e);
            success = false;
        }


        // read latest line of file
        // parse as CSV
        // initialize variables:
        // q_tot
        // q_sub_ch
        // q_sup_ch
        // q_g

        return success;
    }

    private boolean getRemoteFile(String remoteFile, String localTargetDirectory) {

        boolean success = true;

        try {
            logger.warn("Trying to fetch: " + remoteFile + " => " + localTargetDirectory);

            /* Create a connection instance */
            Connection conn = new Connection(hostname);

            /* Now connect */
            conn.connect();

            boolean isAuthenticated = conn.authenticateWithPassword(username, password);

            if (isAuthenticated == false)
                throw new IOException("Authentication failed.");

            /* Create a session */
            Session sess = conn.openSession();

            SCPClient scp = new SCPClient(conn);

            scp.get(remoteFile, localTargetDirectory);

            /* Close this session */
            sess.close();

            /* Close the connection */
            conn.close();

        }
        catch (IOException e) {
            e.printStackTrace(System.err);
            success = false;
        }
        return success;
    }


    private void removeWindow() {
        logger.warn("Cleaning window");
        synchronized (lock) {
            try {
                lock();
                streams = null;
                streams = new Vector<StreamElement>();
                unlock();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                unlock();
            }
        }
        logger.warn("Cleaning window. Size=" + streams.size());


        // while (vector is locked) wait();
    }

    private void addElement(StreamElement data) {
        logger.warn("Adding " + data + " to queue.");
        synchronized (lock) {
            try {
                lock();
                streams.add(data);
                unlock();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                unlock();
            }
        }
        logger.warn("Added. Size=" + streams.size());
    }

    public static void gotoNextIteration() {
        beginTimestamp += windowSize;
        endTimestamp += windowSize;
        // increment counters;
        logger.warn("Going to next iteration: "
                + timestampInAlpine3DFormat(beginTimestamp)
                + " "
                + timestampInAlpine3DFormat(endTimestamp)
        );
    }


    public static boolean allSensorsExceededCurrentWindow() {


        return true; // TODO: for test only
        /*
        boolean exceeded = true;
        for (Iterator iter = monitoredSensors.iterator(); iter.hasNext() && exceeded;) {
            String key = (String) iter.next();
            if ((latestProducedTimestamp.get(key) != null)) {
                if (latestProducedTimestamp.get(key) < endTimestamp) // sensor should have data reached at least up to the endTimestamp
                    exceeded = false;
            } else
                exceeded = false;
        }

        if (exceeded)
            logger.warn("All sensors exceeded current window");
        else
            logger.warn("Some sensors haven't yet exceeded current window");

        return exceeded;
        */

    }

    public static boolean enoughData() {
        // consider using a tolerance threshold

        //return true;

        //TODO: FIXE ME

        boolean enough = true;
        for (Iterator iter = monitoredSensors.iterator(); iter.hasNext() && enough;) {
            String key = (String) iter.next();
            if ((latestProducedTimestamp.get(key) != null)) {
                if (getNumberofElementsWithinBounds(key, beginTimestamp, endTimestamp) < 1) {
                    enough = false; //  sensor must have at list 01 tuple within the window
                }
            } else
                enough = false;
        }

        if (enough)
            logger.warn("Enough data for all sensors");
        else
            logger.warn("Not enough data for some sensors");

        return enough;

    }

    public void dataAvailable(String inputStreamName, StreamElement data) {

        long ts = data.getTimeStamp();

        //logger.warn(inputStreamName + " => " + ts + " => " + timestampInAlpine3DFormat(ts));

        latestProducedTimestamp.put(inputStreamName, ts);

        // check if this is the very first timestamp received

        if (latestProcessedTimestamp.get(inputStreamName) == null) {
            latestProcessedTimestamp.put(inputStreamName, ts);
        }

        //listSensorStatus();
        //enoughData();
        //allSensorsExceededCurrentWindow();

        return;

    }

    // returns the timestamp as a string in an Alpine3D-friendly format
    private static String timestampInAlpine3DFormat(long t) {
        String date = new java.text.SimpleDateFormat(TIME_FORMAT).format(new java.util.Date(t));
        return date;
    }

    // returns the timestamp as a string in an Alpine3D-friendly format
    private static String timestampInRFormat(long t) {
        String date = new java.text.SimpleDateFormat("yyyyMMddHHmm").format(new java.util.Date(t));
        return date;
    }

    public void dispose() {
        isActive = false; // stops child thread
        if (timeOutThread != null)
            timeOutThread.stopMe(); // stop this thread in case it was forgotten
    }




    /*
    * Return the number of tuples for a given sensor within given bounds
    * Uses calls to database
    * */
    public static long getNumberofElementsWithinBounds(String sensor, long from, long to) {

        StringBuilder query = new StringBuilder("select timed from " + sensor + " where timed>= " + from + " and timed <= " + to);
        //logger.warn("Query => "+ query);
        DataEnumerator result;
        try {
            result = StorageManager.getInstance().executeQuery(query, false);
        } catch (SQLException e) {
            logger.error("ERROR IN EXECUTING, query: " + query);
            logger.error(e.getMessage(), e);
            return -1;
        }

        long count = 0;

        while (result.hasMoreElements()) {
            StreamElement se = result.nextElement();
            count++;
            //
        }

        //logger.warn("Number of elements within bounds for " + sensor + " : " + count);

        return count;
    }

    public static void listSensorStatus() {

        logger.warn("*** Sensors Status *** ");

        for (Iterator iter = monitoredSensors.iterator(); iter.hasNext();) {
            String key = (String) iter.next();
            if (latestProducedTimestamp.get(key) != null)
                logger.warn(key
                        + " : ts = "
                        + timestampInAlpine3DFormat(latestProducedTimestamp.get(key))
                        + " "
                        + getNumberofElementsWithinBounds(key, beginTimestamp, endTimestamp)
                        + " elements in [ "
                        + timestampInAlpine3DFormat(beginTimestamp)
                        + " : "
                        + timestampInAlpine3DFormat(endTimestamp)
                        + " ] => ");

        }
        logger.warn("*** --- ***");
    }


}
