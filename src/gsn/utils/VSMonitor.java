package gsn.utils;

import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.Iterator;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import gsn.beans.VSensorMonitorConfig;
import gsn.beans.GSNSessionAddress;

public class VSMonitor {

    public static String MONT_BLANC_HOSTNAME = "montblanc.slf.ch";
    public static int MONT_BLANC_HOSTNAME_PORT = 22003;
    public static String CONFIG_FILENAME = "conf/monitoring.cfg";
    public static final String CONFIG_SEPARATOR ="@";

    private static transient final Logger logger = Logger.getLogger( VSMonitor.class );

    public static HashMap<String, VSensorMonitorConfig> monitoredSensors = new HashMap<String, VSensorMonitorConfig>();
    public static HashMap<String, Long> sensorsData = new HashMap<String, Long>();
    public static List<GSNSessionAddress> listOfSessions = new Vector<GSNSessionAddress>();
    // list of servers to parse host+path+port

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

                        monitoredSensors.put(vsensorname, new VSensorMonitorConfig(vsensorname, host,port,timeout, path, needspassword,username,password));
                        sensorsData.put(vsensorname, new Long(-1)); // not yes initialized, to be initialized when web server is queried

                        GSNSessionAddress gsnSessionAdress = new GSNSessionAddress(host,path, port, needspassword,username, password); //TODO: insitialize it
                        
                        if (!listOfSessions.contains(gsnSessionAdress)){
                             listOfSessions.add(gsnSessionAdress);
                        }
                        

                        //System.out.println("VS: "+"\""+vsensorname+"\""+" timeout: " + Long.toString(timeout));
                        System.out.println("Added monitoring for:"+ monitoredSensors.get(vsensorname));
                        logger.warn("Added monitoring for:"+ monitoredSensors.get(vsensorname));
                    }
                }
            }
        }
        catch (IOException e) {
            logger.warn("IO Exception while trying to open file <"+ fileName + "> "+e);
        }
    }

    public static void readStatus() throws Exception {
         HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, "UTF-8");
            HttpProtocolParams.setUserAgent(params, "HttpComponents/1.1");
            HttpProtocolParams.setUseExpectContinue(params, true);

            BasicHttpProcessor httpproc = new BasicHttpProcessor();
            // Required protocol interceptors
            httpproc.addInterceptor(new RequestContent());
            httpproc.addInterceptor(new RequestTargetHost());
            // Recommended protocol interceptors
            httpproc.addInterceptor(new RequestConnControl());
            httpproc.addInterceptor(new RequestUserAgent());
            httpproc.addInterceptor(new RequestExpectContinue());

            HttpRequestExecutor httpexecutor = new HttpRequestExecutor();

            HttpContext context = new BasicHttpContext(null);
            HttpHost host = new HttpHost(MONT_BLANC_HOSTNAME, MONT_BLANC_HOSTNAME_PORT);

            DefaultHttpClientConnection conn = new DefaultHttpClientConnection();
            ConnectionReuseStrategy connStrategy = new DefaultConnectionReuseStrategy();

            context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
            context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, host);

            try {

                String[] targets = {"/gsn"};

                for (int i = 0; i < targets.length; i++) {
                    if (!conn.isOpen()) {
                        Socket socket = new Socket(host.getHostName(), host.getPort());
                        conn.bind(socket, params);
                    }
                    BasicHttpRequest request = new BasicHttpRequest("GET", targets[i]);
                    //TODO: consider password protected websites
                    System.out.println(">> Request URI: " + request.getRequestLine().getUri());

                    request.setParams(params);
                    httpexecutor.preProcess(request, httpproc, context);
                    HttpResponse response = httpexecutor.execute(request, conn, context);
                    response.setParams(params);
                    httpexecutor.postProcess(response, httpproc, context);


                    String myResponse = EntityUtils.toString(response.getEntity());
                    String[] lines = null;
                    lines = myResponse.split("\n");
                    System.out.println("Length of list:"+lines.length);

                    for (int lineNumber=0;lineNumber<lines.length;lineNumber++){
                        if (lines[lineNumber].toLowerCase().indexOf("virtual-sensor name")>0) //TODO: more robust with regular expressions
                            System.out.println(lineNumber+" : "+lines[lineNumber]);
                    }

                    System.out.println("LENGTH: "+myResponse.length());
                    System.out.println("<< Response: " + response.getStatusLine());
                    //System.out.println(myResponse);
                    System.out.println("==============");
                    if (!connStrategy.keepAlive(response, context)) {
                        conn.close();
                    } else {
                        System.out.println("Connection kept alive...");
                    }
                }
            } finally {
                conn.close();
            }
        }


    public static void main(String[] args)  {

        initFromFile(CONFIG_FILENAME);
        Iterator iter = listOfSessions.iterator();
        while (iter.hasNext()){
              System.out.println(iter.next());
        }
        try{
             readStatus();
        }
        catch( Exception e){

        }

    }
}
