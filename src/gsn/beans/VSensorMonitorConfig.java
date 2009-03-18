package gsn.beans;

import org.apache.log4j.Logger;

public class VSensorMonitorConfig {

    private String SEPARATOR = "@";

    protected String name;
    protected String host;
    protected int port;
    protected long timeout;
    protected String path;
    protected boolean needspassword;
    protected String username;
    protected String password;

    private transient final Logger logger = Logger.getLogger( VSensorMonitorConfig.class );

    public VSensorMonitorConfig(String name, String host, int port, long timeout, String path, boolean needspassword, String username, String password) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.timeout = timeout;
        this.path = path;
        this.needspassword = needspassword;
        this.username = username;
        this.password = password;
    }

    public VSensorMonitorConfig() {
        this.password = "";
        this.name = "";
        this.host = "";
        this.port = 0;
        this.timeout = 0;
        this.path = "";
        this.needspassword = false;
        this.username = "";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean needsPassword() {
        return needspassword;
    }

    public void setNeedsPassword(boolean needspassword) {
        this.needspassword = needspassword;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTimeoutAsString() {
        return Long.toString(timeout); //TODO: compute timeout and return it as string, e.g. 10h20m5s30ms
    }

    public int hashCode(){
		if(name != null){
			return name.hashCode();
		}
		else{
			return super.hashCode();
		}
	}

    public String toString() {
        if (this.needspassword)
            return name+SEPARATOR+getTimeoutAsString()+SEPARATOR+"http://"+host+":"+port+path+SEPARATOR+username+":"+password;
        else
            return name+SEPARATOR+getTimeoutAsString()+SEPARATOR+"http://"+host+":"+port+path;
    }

    public boolean equals(Object obj){
		if (obj instanceof VSensorMonitorConfig) {
			VSensorMonitorConfig vSensorMonitorConfig = (VSensorMonitorConfig) obj;
			return name.equals(vSensorMonitorConfig.getName());
		}
		return false;
	}

    public static String timeOutToString(){
        return "";
    }
    
    public static long timeOutFromString(String s) {
        return Long.parseLong(s);
    }

    public static int getHoursFromTimeOut(String t){
        return 0;
    }
}
