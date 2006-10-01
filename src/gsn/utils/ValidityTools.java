package gsn.utils;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

import org.apache.log4j.Logger;

public class ValidityTools {

    public static final int SMTP_PORT = 25;

    public static final transient Logger logger = Logger
	    .getLogger(ValidityTools.class);

    public static boolean isAccessibleSocket(String host, int port) throws UnknownHostException {
	try {    		
	    Socket socket = new Socket();
	    InetSocketAddress inetSocketAddress = new  InetSocketAddress(host,port);
	    socket.connect(inetSocketAddress,3000);
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
    public static boolean isLocalhost(String host) {
	String hostPrepared = host.trim().toLowerCase();
	for (String addr : NETWORK_LOCAL_ADDRESS)
	    if (addr.equals(hostPrepared))
		return true;
	if (host.indexOf("127.")>=0)
	    return true;
	return false;
    }
    
    private static final ArrayList<String > NETWORK_LOCAL_ADDRESS = new ArrayList<String>();
    static {
    try {
	    Enumeration<NetworkInterface> nets = NetworkInterface
		    .getNetworkInterfaces();
	    for (NetworkInterface netint : Collections.list(nets)) {
		Enumeration<InetAddress> address = netint.getInetAddresses();
		for (InetAddress addr : Collections.list(address)) {
		    if (addr.isSiteLocalAddress()) {
			NETWORK_LOCAL_ADDRESS.add(addr.getHostAddress().trim().toLowerCase());
			NETWORK_LOCAL_ADDRESS.add(addr.getHostName().trim().toLowerCase());
		    }
		}
		NETWORK_LOCAL_ADDRESS.add("localhost");
		NETWORK_LOCAL_ADDRESS.add("127.0.0.1");
		
	    }
    }catch (Exception e) {
	e.printStackTrace();
    }
    }
	    

}
