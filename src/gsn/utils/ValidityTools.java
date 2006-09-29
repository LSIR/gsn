package gsn.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

public class ValidityTools {

    public static final int SMTP_PORT = 25;

    public static final transient Logger logger = Logger
	    .getLogger(ValidityTools.class);

    public static boolean isAccessibleSocket(String host, int port) {
	try {
	    Socket socket = new Socket(host, port);
	    InputStream io = socket.getInputStream();
	    io.close();
	} catch (UnknownHostException e) {
	    logger.warn(e.getMessage());
	    logger.debug(e.getMessage(), e);
	    return false;
	} catch (IOException e) {
	    logger.warn(e.getMessage());
	    logger.debug(e.getMessage(), e);
	    return false;
	}
	return true;
    }
    public static void checkAccessibilityOfDirs(String... args) {
	for (String name : args) {
	    File f = new File(name);
	    if (f.canRead() && f.canWrite() && f.isDirectory())
		continue;
	    else {
		System.out.println("The required directory : "
			+ f.getAbsolutePath() + " is not accessible.");
		System.exit(1);
	    }
	}
    }

    public static void checkAccessibilityOfFiles(String... args) {
	for (String name : args) {
	    File f = new File(name);
	    if (f.canRead() && f.canWrite() && f.isFile())
		continue;
	    else {
		System.out.println("The required file : " + f.getAbsolutePath()
			+ " is not accessible.");
		System.exit(1);
	    }
	}
    }

}
