package gsn.simulation;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.Session;
import gsn.utils.RemoteExecThread;
import gsn.utils.TimeOutThread;
import org.apache.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class A3DSimulation implements GSNSimulation {

    private static final transient Logger logger = Logger.getLogger(A3DSimulation.class);

    public static final int STATUS_RUNNING = 0;
    public static final int STATUS_STOPPED = 1;

    public static final int ERROR_ALREADY_RUNNING = -2;
    public static final int ERROR_SIMULATION_NOT_INITIALIZED = -1;
    public static final int OK_SIMULATION_STARTED = 0;

    private TimeOutThread timeOutThread = null;
    private RemoteExecThread remoteExecThread = null;

    private boolean initialized = false;
    private boolean simulationIsRunning = false;

    private static long beginTimestamp = -1;
    private static long endTimestamp = -1;

    public A3DSimulationParameters params;
    public A3DSimulationResults results;


    public A3DSimulation(String UUID) {

        this.params = new A3DSimulationParameters();
        this.params.setUUID(UUID);
        this.results = new A3DSimulationResults();
    }

    /*
    * Return codes
    * OK_SIMULATION_STARTED => Ok
    * ERROR_SIMULATION_NOT_INITIALIZED cannot start
    * ERROR_ALREADY_RUNNING already running
    * */

    public int start() {
        if (simulationIsRunning)
            return ERROR_ALREADY_RUNNING;
        else {
            if (initialized) {
                simulationIsRunning = true;
                String cmd = "cd " + params.getBasedir() + " ; " + params.getScriptName() + " " + params.getStartFrom() + " " + params.getEndTo();

                //logger.warn(params.getScriptName());
                logger.warn("runnning => " + cmd);

                remoteExecThread = new RemoteExecThread(cmd, params.getServerHostname(), params.getServerUsername(), params.getServerPassword(), 0);
                remoteExecThread.start();

                //timeOutThread = new TimeOutThread(timeoutLimit);
                return OK_SIMULATION_STARTED;
            } else {
                logger.warn("Cannot start a simulation which is not initialized");
                return ERROR_SIMULATION_NOT_INITIALIZED;
            }
        }
    }

    /*
    * Return codes
    * 0 => Ok
    * -1 cannot stop
    * -2 already stopped
    * */

    public int stop() {
        return 0;
    }

    public void run() {

    }


    public boolean isSimulationRunning() {

        if (remoteExecThread != null)
            return remoteExecThread.isRunning();
        else
            return false;
    }

    public boolean isSimulationFinished() {

        if (remoteExecThread != null) {
           return remoteExecThread.isFinished();
        } else
            return false;
    }

    public boolean fetchResults() {
        if (remoteExecThread.isFinished()) {
            getAllRemoteFiles();
            return true;
        } else
            return false;
    }

    public boolean getAllRemoteFiles() {

        if (remoteExecThread != null) {
            if (remoteExecThread.isFinished() && !remoteExecThread.isRunning()) {
                getRemoteFile(params.getBasedir() + "plot_map/11_Txy/lay1/11_TxyL0001N0001.png", params.getLocalWorkingDir());
                getRemoteFile(params.getBasedir() + "plot_map/11_Txy/lay2/11_TxyL0002N0001.png", params.getLocalWorkingDir());
                getRemoteFile(params.getBasedir() + "plot_map/11_Txy/lay3/11_TxyL0003N0001.png", params.getLocalWorkingDir());
                getRemoteFile(params.getBasedir() + "plot_map/11_Txy/lay4/11_TxyL0004N0001.png", params.getLocalWorkingDir());
                getRemoteFile(params.getBasedir() + "plot_map/11_Txy/lay5/11_TxyL0005N0001.png", params.getLocalWorkingDir());
                getRemoteFile(params.getBasedir() + "plot_map/11_Txy/lay6/11_TxyL0006N0001.png", params.getLocalWorkingDir());
            }
        }
        return true;
    }




    private boolean putRemoteFile(String remoteFile, String content) {
        return true;
    }

    private boolean getRemoteFile(String remoteFile, String localTargetDirectory) {

        boolean success = true;
        String hostname = this.params.getServerHostname();
        String username = this.params.getServerUsername();
        String password = this.params.getServerPassword();

        try {


            logger.warn("Trying to fetch: " + remoteFile + " => " + localTargetDirectory);

            /* Create a connection instance */
            Connection conn = new Connection(hostname);

            /* Now connect */
            conn.connect();

            boolean isAuthenticated = conn.authenticateWithPassword(username, password);

            if (!isAuthenticated)
                logger.warn("Authentication failed.");

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
            logger.warn(e);
            success = false;
        }
        return success;
    }

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

    public String getSimulationStatus() {
        if (simulationIsRunning)
            return "Running";
        else
            return "Stopped";
    }

    public void setGeneralParameters(String startFrom, String endTo, String stations, String type, long windowsize) {
        this.params.setStartTime(startFrom);
        this.params.setEndTo(endTo);
        this.params.setStations(stations);
        this.params.setType(type);
        this.params.setWindowSize(windowsize);
        //this.params.setScriptName(scriptname);
        //this.params.setScriptParams(scriptparams);
    }


    @Override
    public String toString() {
        return "A3DSimulation{" +
                "initialized=" + initialized +
                ", simulationIsRunning=" + simulationIsRunning +
                ", params=" + params +
                '}';
    }

    public void init() {
        this.initialized = true;
    }

    public int getA3DExitCode() {
        if (remoteExecThread != null) {
            return remoteExecThread.getResult();
        }
        else return -1;
    }


    /*
    * parameter can be a list (comma separated)
    * */

    public String addSimulationParameter(String filter) {
        this.params.addSpecificFilter(filter);
        return filter;
    }

    public boolean uploadIoIniFileOnA3DServer(String file) {
        boolean success = true;
        FileWriter outFile = null;
        String filename = "io.ini";
        String remoteFolder = params.getBasedir();
        try {
            outFile = new FileWriter(filename);
            PrintWriter out = new PrintWriter(outFile);
            out.print(file);
            out.close();
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
            return false;
        }

        String hostname = this.params.getServerHostname();
        String username = this.params.getServerUsername();
        String password = this.params.getServerPassword();

        try {


            logger.warn("Trying to send: " + filename + " => " + remoteFolder);

            /* Create a connection instance */
            Connection conn = new Connection(hostname);

            /* Now connect */
            conn.connect();

            boolean isAuthenticated = conn.authenticateWithPassword(username, password);

            if (!isAuthenticated)
                logger.warn("Authentication failed.");

            /* Create a session */
            Session sess = conn.openSession();

            SCPClient scp = new SCPClient(conn);

            scp.put(filename, remoteFolder);

            /* Close this session */
            sess.close();

            /* Close the connection */
            conn.close();
            logger.warn("file sent");

        }
        catch (IOException e) {
            logger.warn(e);
            success = false;
        }
        
        return success;
    }


}
