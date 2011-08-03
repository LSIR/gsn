package gsn.utils;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class RemoteExecThread extends Thread {

    private boolean finished = false;
    private boolean isRunning = false;
    private String cmd;
    private int result = -1;

    private String user;
    private String password;
    private String hostname;
    private int controlPort;

    private static int threadCounter = 0;

    private static final transient Logger logger = Logger.getLogger(RemoteExecThread.class);
    private static final int ERROR_SSH_AUTH_FAILED = -1;

    public RemoteExecThread(String ssh_cmd, String hostname, String user, String password, int controlPort) {

        this.hostname = hostname;
        this.user = user;
        this.password = password;
        this.controlPort = controlPort;
        this.cmd = ssh_cmd;
        this.setName("RemoteExec-Thread-" + (++threadCounter));
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isRunning() {
        logger.warn("Status => "+ isRunning);
        return isRunning;
    }

    public int getResult() {
        return result;
    }


    public boolean put_io_ini(String content, String folder) {
        boolean success = false;
        return success;
    }

    public boolean fetchFiles(String localFolder, String[] files) {
        boolean success = false;
        return success;
    }

    public boolean getRemoteFile(String remoteFile, String localTargetDirectory) {

        boolean success = true;

        try {
            logger.warn("Trying to fetch: " + remoteFile + " => " + localTargetDirectory);

            /* Create a connection instance */
            Connection conn = new Connection(hostname);

            /* Now connect */
            conn.connect();

            boolean isAuthenticated = conn.authenticateWithPassword(user, password);

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

    public void run() {
        try {
            isRunning = true;
            logger.warn("Thread started.");
            logger.warn(user + "@" + hostname);
            logger.warn("Listening on port: "+controlPort);
            logger.warn("Running > "+ cmd +" <");

            /* Create a connection instance */

			Connection conn = new Connection(hostname);

			/* Now connect */

			conn.connect();

			/* Authenticate.
			 * If you get an IOException saying something like
			 * "Authentication method password not supported by the server at this stage."
			 * then please check the FAQ.
			 */

			boolean isAuthenticated = conn.authenticateWithPassword(user, password);

			if (isAuthenticated == false) {
                result = ERROR_SSH_AUTH_FAILED;
				throw new IOException("SSH Authentication failed.");
            }

            /* Create a session */

			Session sess = conn.openSession();

            //sess.requestX11Forwarding("127.0.0.1",6000,null,true);

			sess.execCommand(cmd);

			logger.warn("Output:");

			/*
			 * This basic example does not handle stderr, which is sometimes dangerous
			 * (please read the FAQ).
			 */

			InputStream stdout = new StreamGobbler(sess.getStdout());

			BufferedReader br = new BufferedReader(new InputStreamReader(stdout));

			while (true)
			{
				String line = br.readLine();
				if (line == null)
					break;
				logger.warn(line);
                //System.out.println(line);
			}

            InputStream stderr = new StreamGobbler(sess.getStderr());

			BufferedReader brErr = new BufferedReader(new InputStreamReader(stderr));

			while (true)
			{
				String line = brErr.readLine();
				if (line == null)
					break;
				logger.warn(line);
                //System.out.println(line);
			}

			/* Show exit status, if available (otherwise "null") */

            if (sess.getExitStatus() !=null) { // avoid null pointer exceptions
                result = sess.getExitStatus();

            }
            else {
                logger.warn("Cannot get exit status for script");
                result = -1;
            }

			logger.warn("ExitCode: " + result);

			/* Close this session */

			sess.close();

			/* Close the connection */

			conn.close();


        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
            result = -1;
        }

        catch (NullPointerException e) {
            logger.warn(e.getMessage(), e);
            result = -1;
        }

        finished = true;
        logger.warn("Thread finished.");
        isRunning = false;

    }
}
