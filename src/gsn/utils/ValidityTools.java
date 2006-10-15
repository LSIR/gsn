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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    
    static Pattern hostAndPortPattern = Pattern.compile("(.+):(\\d+)$");

    /*
     * Returns the hostname part of a host:port String. This method is ipv6 compatible.
     * @param hostandport A string containing a host and a port number, separated by a ":"
     * @return host A string with the host name part (either name or ip address) of the input string.
     */
    public static String getHostName(String hostandport) {
    	String hostname="";
    	try {
    		Matcher m = hostAndPortPattern.matcher(hostandport);
    		m.matches();
    		hostname = m.group(1).toLowerCase().trim();
    	} catch(Exception e) { }
    	return hostname;
    }
    /*
     * Returns the port number of a host:port String. This method is ipv6 compatible.
     */
    public static int getPortNumber(String hostandport) {
    	int port = -1;
    	try {
    		port = Integer.parseInt(hostAndPortPattern.matcher(hostandport).group(2).toLowerCase().trim());
    	} catch(Exception e) { }
    	return port;
    }
    public static boolean isLocalhost(String host) {
    	//	 this allows us to be ipv6 compatible (we simply remove the port)
    	host = getHostName(host);
    	try {
			InetAddress hostAddress = InetAddress.getByName(host);
			for(InetAddress address: NETWORK_LOCAL_INETADDRESSES)
				if(address.equals(hostAddress))
					return true;
			return false;
		} catch (UnknownHostException e) {
			logger.debug(e);
			return false;
		}
    	/*
    	for (String addr : NETWORK_LOCAL_ADDRESS)
    		if (host.equals(addr.toLowerCase().trim()))
    			return true;
    	 */
    }

    public static final ArrayList<String > NETWORK_LOCAL_ADDRESS = new ArrayList<String>();
    public static final ArrayList<InetAddress> NETWORK_LOCAL_INETADDRESSES = new ArrayList<InetAddress>();
    static {
    	try {
    		Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
    		for (NetworkInterface netint : Collections.list(nets)) {
    			Enumeration<InetAddress> address = netint.getInetAddresses();
    			for (InetAddress addr : Collections.list(address)) {
    				if (! addr.isMulticastAddress()) {
    					NETWORK_LOCAL_ADDRESS.add(addr.getCanonicalHostName());
    					NETWORK_LOCAL_INETADDRESSES.add(addr);
    				}
    			}
    			//NETWORK_LOCAL_ADDRESS.add("localhost");
    			//NETWORK_LOCAL_ADDRESS.add("127.0.0.1");

    		}
    	}catch (Exception e) {
	e.printStackTrace();
    }
    }
	    

}
