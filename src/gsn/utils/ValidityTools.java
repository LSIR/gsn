package gsn.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.log4j.Logger;

public class ValidityTools {

    public static final int SMTP_PORT = 25;

    public static final transient Logger logger = Logger
	    .getLogger(ValidityTools.class);

    public static boolean isAccessibleSocket(String host, int port) throws UnknownHostException {
	try {
	    Socket socket = new Socket(host, port);
	    InputStream io = socket.getInputStream();
	    io.close();
	} catch (IOException e) {
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
    public static  void isDBAccessible(String driverClass,String url,String user,String password) throws ClassNotFoundException, SQLException {
	  Class.forName(driverClass);
	  Connection con= DriverManager.getConnection (url ,user,password );
	  con.close();
    }

}
