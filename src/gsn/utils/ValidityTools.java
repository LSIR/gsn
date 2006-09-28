package gsn.utils;

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

}
