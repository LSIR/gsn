package gsn.http.restapi;

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.HttpURLConnection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;

public class GsnProxy extends HttpServlet {
    
	private static final long serialVersionUID = 1L;
	//private ServletContext servletContext;
    private Logger log;
    
    public void init(ServletConfig servletConfig) throws ServletException {
//    	super.init(servletConfig);
        //servletContext = servletConfig.getServletContext();
        log = LoggerFactory.getLogger(GsnProxy.class.getName());
    }
    
    public void doPost(HttpServletRequest request, HttpServletResponse response){
    	return;
    }
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException{
    	FileOutputStream os = new FileOutputStream("/home/michael/dev/semester_project/gsn_perso_log.txt");
    	
    	log.info("	");
        HttpURLConnection con;
        
        try{
            int statusCode;
            String methodName;
            
            String urlString = "http://localhost:9000" + request.getPathInfo().toString(); //request.getRequestURL().toString();
            String queryString = request.getQueryString();
            
            urlString += queryString==null?"":"?"+queryString;
            URL url = new URL(urlString);
            
            IOUtils.write("urlString = " + urlString + "\n", os);
            
            log.info("Fetching >"+url.toString());
            
            con =(HttpURLConnection) url.openConnection();
            
            methodName = request.getMethod();
            con.setRequestMethod(methodName);
            con.setDoOutput(true);
            con.setDoInput(true);
            HttpURLConnection.setFollowRedirects(false);
            con.setUseCaches(true);

            for( Enumeration<String> e = request.getHeaderNames() ; e.hasMoreElements();){
                String headerName = e.nextElement().toString();
                con.setRequestProperty(headerName,    request.getHeader(headerName));
            }
            
            con.connect();
                   
            statusCode = con.getResponseCode();
            response.setStatus(statusCode);
            
            for (Entry<String, List<String>> e : con.getHeaderFields().entrySet()){
            	if (e.getKey() != null)
            		response.setHeader(e.getKey().toString(), e.getValue().get(0).toString());
            }
            
            IOUtils.copy(con.getInputStream(), response.getOutputStream());

            con.disconnect();
            
        }catch(Exception e){
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        finally{
        }
    }
}