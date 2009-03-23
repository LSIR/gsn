package gsn.utils;

import java.net.UnknownHostException;
import java.net.ConnectException;
import java.util.*;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

import org.apache.log4j.Logger;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;

import org.apache.commons.mail.SimpleEmail;
import org.apache.commons.mail.EmailException;

import gsn.beans.VSensorMonitorConfig;
import gsn.beans.GSNSessionAddress;

/*
* VSMonitor
* Monitors a series of Virtual Sensors (last update time)
* and creates a report shown on standard output and sent by e-mail
* You can put this program in a Cron job and run it regularely to check status.
* Syntax for calling:
* java -jar VSMonitor.jar <config_file> <list_of_mails>
*  e.g.  java -jar VSMonitor.jar conf/monitoring.cfg user@gmail.com admin@gmail.com
* As input, a config file containing list sensors described by: 
* name of sensor,
* timeout (expressing expected update period, for example every 1h),
* gsn session address (e.g. www.server.com:22001/gsn)
* and possibly a username and password, in case session requieres authentication.
* See conf/monitoring.cfg for a sample file
* List of e-mails used for notification are given as arguments to the program
* e-mail account parameters (currently gmail only) are given in the config file
*
* */
public class VSMonitor {

    public static final String CONFIG_SEPARATOR ="@";

    private static transient final Logger logger = Logger.getLogger( VSMonitor.class );

    public static HashMap<String, VSensorMonitorConfig> monitoredSensors = new HashMap<String, VSensorMonitorConfig>();
    public static HashMap<String, Long> sensorsUpdateDelay = new HashMap<String, Long>();
    public static List<GSNSessionAddress> listOfSessions = new Vector<GSNSessionAddress>();
    public static List<String> listOfMails = new Vector<String>();

    public static StringBuilder errorsBuffer = new StringBuilder();
    public static StringBuilder noErrorsBuffer = new StringBuilder();
    public static StringBuilder summary = new StringBuilder();
    private static final String GSN_REALM = "GSNRealm";
    private static String gmail_username;
    private static String gmail_password;
    private static final String SMTP_GMAIL_COM = "smtp.gmail.com";
    private static int nHostsDown = 0;
    private static int nSensorsLate = 0;

    /*
    * Reads config file and initializes:
    * e-mail config (gmail_username, gmail_password)
    * lists
    * - monitoredSensors: list of monitored sensors (including expected update delay)
    * - sensorsUpdateDelay: list of sensors update delays (initialized to -1)
    * - listOfSessions: list of GSN sessions
    * */
    public static void initFromFile(String fileName){
        long timeout;
        String vsensorname;
        String host;
        String path;
        int port;
        boolean needspassword;
        String password;
        String username;
        logger.warn("Trying to initialize VSMonitor from file <"+ fileName + ">");
        try{
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            String str;
            while ((str = in.readLine()) != null) {
                // ignore comments starting with #
                if (str.trim().indexOf("#")==0) {
                   continue;
                }
                if (str.trim().indexOf("@gmail-username")>=0) {
                    gmail_username = str.trim().split(" ")[1];
                    //System.out.println("GMAIL Username: "+ gmail_username);
                    continue;
                }
                if (str.trim().indexOf("@gmail-password")>=0) {
                    gmail_password = str.trim().split(" ")[1];
                    //System.out.println("GMAIL password: "+ gmail_password);
                    continue;
                }
                //@gmail-username
                //@gmail-password
                String[] s = str.trim().split(CONFIG_SEPARATOR);
                if (s.length<3) {
                    logger.warn("Malformed monitoring line in file <"+ fileName + "> : "+str);
                    System.out.println("Malformed monitoring line in file <"+ fileName + "> : "+str);
                }
                else {
                    //System.out.println(s.length+" Elements found");

                    vsensorname = s[0].trim();
                    timeout= VSensorMonitorConfig.timeOutFromString(s[1].trim());
                    //System.out.println("\""+s[2]+"\"");
                    String[] host_port_path = s[2].split(":");
                    //System.out.println(host_port_path.length);
                    if (host_port_path.length!=2) {
                        logger.warn("Malformed monitoring line in file <"+ fileName + "> : "+str);
                        System.out.println("Malformed monitoring line in file <"+ fileName + "> : "+str);
                        continue;
                    }
                    else {
                        //System.out.println("["+host_port_path[0].trim()+"]["+host_port_path[1].trim()+"]");
                        host = host_port_path[0].trim();
                        int j= host_port_path[1].trim().indexOf("/");
                        String portStr = host_port_path[1].trim().substring(0,j);
                        //System.out.println("Port:"+portStr);
                        path = host_port_path[1].trim().substring(j);
                        //System.out.println("Path:"+"\""+path+"\"");

                        try {
                            port = Integer.parseInt(portStr);
                            //System.out.println(">>"+port);
                        }
                        catch (NumberFormatException e) {
                            logger.warn("Malformed monitoring line in file <"+ fileName + "> : "+str);
                            System.out.println("Malformed monitoring line in file <"+ fileName + "> : "+str);
                            continue;
                        }

                        if (s.length>3) { // needs password
                            needspassword = true;
                            String[] username_password = s[3].split(":");
                            if (username_password.length>1){
                                username = username_password[0].trim();
                                password = username_password[1].trim();
                            }
                            else {
                                logger.warn("Malformed monitoring line in file <"+ fileName + "> : "+str);
                                System.out.println("Malformed monitoring line in file <"+ fileName + "> : "+str);
                                continue;
                            }
                        }
                        else{
                            needspassword = false;
                            username = "";
                            password = "";
                        }
                        // DEBUG INFO
                        //System.out.println("TIMEOUT: "+timeout);
                        //System.out.println("Creating object with : "+vsensorname+" "+host+" "+port+" "+timeout+" "+path+" "+needspassword+" "+username+" "+password);
                        monitoredSensors.put(vsensorname, new VSensorMonitorConfig(vsensorname, host,port,timeout, path, needspassword,username,password));

                        // DEBUG INFO
                        //System.out.println("RESULT: "+ monitoredSensors.get(vsensorname).toString());

                        sensorsUpdateDelay.put(vsensorname, new Long(-1)); // not yes initialized, to be initialized when web server is queried



                        GSNSessionAddress gsnSessionAdress = new GSNSessionAddress(host,path, port, needspassword,username, password); //TODO: insitialize it

                        if (!listOfSessions.contains(gsnSessionAdress)){
                             listOfSessions.add(gsnSessionAdress);
                        }


                        //System.out.println("VS: "+"\""+vsensorname+"\""+" timeout: " + Long.toString(timeout));
                        //System.out.println("Added: "+ monitoredSensors.get(vsensorname));
                        logger.warn("Added:"+ monitoredSensors.get(vsensorname));
                    }
                }
            }
        }
        catch (IOException e) {
            logger.warn("IO Exception while trying to open file <"+ fileName + "> "+e);
        }
    }

    /*
    * Queries a GSN session for status
    * Uses Http Get method to read xml status (usually under /gsn)
    * parses the xml and initializes global variables :
    * - errorsBuffer
    * - noErrrorsBuffer
    * - nHostsDown
    * - nSensorsLate
    * */
    public static void readStatus(GSNSessionAddress gsnSessionAddress) throws Exception {

        String httpAddress = gsnSessionAddress.getURL();

        HttpClient client = new HttpClient();

        if (gsnSessionAddress.needsPassword()) {
            client.getState().setCredentials(
                        new AuthScope(gsnSessionAddress.getHost(), gsnSessionAddress.getPort(), GSN_REALM),
                        new UsernamePasswordCredentials(gsnSessionAddress.getUsername(), gsnSessionAddress.getPassword())
                    );
        }

        //System.out.println("Querying server: "+httpAddress);
        logger.warn("Querying server: "+httpAddress);
        GetMethod get = new GetMethod(httpAddress);

        try {
            // execute the GET
            int status = client.executeMethod( get );

            if (status==404){
                System.out.println("Http Error 404 Not Found: "+ httpAddress);
                errorsBuffer.append("Http Error 404 Not Found: "+ httpAddress);
                nHostsDown++;
            }

            else if (status==401){
                    System.out.println("Http Error 401 Authentication Needed: "+ httpAddress);
                    errorsBuffer.append("Http Error 401 Authentication Needed: "+ httpAddress);
                }

                else {
                    String myResponse = get.getResponseBodyAsString();
                    String[] lines = myResponse.split("\n");
                    //System.out.println("Length of list:"+lines.length);
                    for (int lineNumber=0;lineNumber<lines.length;lineNumber++){
                        if (lines[lineNumber].toLowerCase().indexOf("virtual-sensor name")>0) {
                            //System.out.println(lineNumber+" : "+lines[lineNumber]);
                            String sensorName = parseSensorName(lines[lineNumber]);

                            if (sensorsUpdateDelay.containsKey(sensorName)) {
                                long lastUpdate = parseLastModified(lines[lineNumber]);
                                long lastUpdateDelay =GregorianCalendar.getInstance().getTimeInMillis()- lastUpdate;
                                //System.out.println(sensorName+" last updated ("+VSensorMonitorConfig.ms2dhms(lastUpdateDelay) +" ago) "+new Date(lastUpdate).toString());
                                sensorsUpdateDelay.put(sensorName, lastUpdateDelay);

                                if (sensorsUpdateDelay.get(sensorName)>monitoredSensors.get(sensorName).getTimeout()) {

                                    //System.out.println("Sensor "+sensorName+" delayed: "+sensorsUpdateDelay.get(sensorName)+" >> "+monitoredSensors.get(sensorName).getTimeout());
                                    errorsBuffer.append(sensorName+"@"+gsnSessionAddress.getURL()+" not updated for "+VSensorMonitorConfig.ms2dhms(sensorsUpdateDelay.get(sensorName))
                                            + " (expected <"+VSensorMonitorConfig.ms2dhms(monitoredSensors.get(sensorName).getTimeout())
                                            +")\n");
                                    nSensorsLate++;
                                }
                                else {
                                    //System.out.println("Sensor "+sensorName+" in time");
                                    noErrorsBuffer.append(sensorName+"@"+gsnSessionAddress.getURL()+"\n");
                                }
                            }
                        }
                    }
                }
            }
        catch (UnknownHostException e) {
            //System.out.println("Error: unknown host <"+gsnSessionAddress.getHost()+">\n");
            errorsBuffer.append("Error: unknown host <"+gsnSessionAddress.getHost()+">\n");
        }
        catch (ConnectException e) {
            //System.out.println("Error: connection refused to host <"+gsnSessionAddress.getHost()+">\n");
            errorsBuffer.append("Error: connection refused to host <"+gsnSessionAddress.getHost()+">\n");
        }
        finally {
            // release any connection resources used by the method
            get.releaseConnection();
        }
    }

    /*
    * returns last modification time of a sensor from a string (a line in the xml file)
    * */
    private static long parseLastModified(String s){
        int start = s.indexOf("last-modified=\"")+15;
        int end = s.indexOf("\" description");
        
        return Long.parseLong(s.substring(start,end));
    }

    /*
    * returns sensor name of a sensor from a string (a line in the xml file)
    * */
    private static String parseSensorName(String s){
        int start = s.indexOf("name=\"")+6;
        int end = s.indexOf("\" last-modified");
        return s.substring(start,end);
    }

    /*
    * Sends an e-mail to recipients specified in command line
    * (through global listOfMails)
    * with the global summary as subject
    * and global errorsBuffer as body
    * */
    private static void sendMail() throws EmailException {
        // hardcoded send mail
        SimpleEmail email = new SimpleEmail();
        //email.setDebug(true);
        email.setHostName(SMTP_GMAIL_COM);
        email.setAuthentication(gmail_username, gmail_password);
        //System.out.println(gmail_username +" "+ gmail_password);
        email.getMailSession().getProperties().put("mail.smtp.starttls.enable", "true");
        email.getMailSession().getProperties().put("mail.smtp.auth", "true");
        email.getMailSession().getProperties().put("mail.debug", "true");
        email.getMailSession().getProperties().put("mail.smtp.port", "465");
        email.getMailSession().getProperties().put("mail.smtp.socketFactory.port", "465");
        email.getMailSession().getProperties().put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        email.getMailSession().getProperties().put("mail.smtp.socketFactory.fallback", "false");
        email.getMailSession().getProperties().put("mail.smtp.starttls.enable", "true");

        for (String s: listOfMails) {
            email.addTo(s);
        }
          email.setFrom(gmail_username +"@gmail.com", gmail_username);
        
          email.setSubject("[GSN Alert] "+summary.toString());
          email.setMsg(errorsBuffer.toString());
          email.send();

    }

    public static void main(String[] args)  {

        String configFileName;
        
        if (args.length>=2) {
            configFileName = args[0];
            System.out.println("Using config file: "+ configFileName);
            for (int i=1;i<args.length;i++){
                 System.out.println("Adding e-mail: "+args[i]);
                 listOfMails.add(args[i]);
            }
        }
        else {
            System.out.println("Usage java -jar VSMonitor.jar <config_file> <list_of_mails>");
            System.out.println("e.g.  java -jar VSMonitor.jar conf/monitoring.cfg user@gmail.com admin@gmail.com");
            return;
        }

        initFromFile(configFileName);

        Iterator iter = listOfSessions.iterator();
        while (iter.hasNext()){

                try{
                    readStatus((GSNSessionAddress)iter.next());
                }
                catch( Exception e){
                    System.out.println("Exception: "+ e.getMessage());
                    e.printStackTrace();
                }
            }

        if ((nSensorsLate>0)||(nHostsDown>0)){
            summary.append("WARNING: ");
            if (nHostsDown>0)
                summary.append(nHostsDown+ " host(s) down. ");
            if (nSensorsLate>0)
                summary.append(nSensorsLate+ " sensor(s) not updated. ");

            // Send e-mail only if there are errors
            try {
                sendMail();
            } catch (EmailException e) {
                logger.warn("Cannot send e-mail.");
                e.printStackTrace();
            }
        }

        // Showing report
        System.out.println(summary);
        System.out.println("[WARNINGS]\n"+errorsBuffer);
        System.out.println("[INFO]\n"+ noErrorsBuffer);

    }
}
