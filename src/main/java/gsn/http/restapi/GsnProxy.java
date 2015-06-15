package gsn.http.restapi;

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.ConfigFactory;

import java.net.URL;
import java.net.HttpURLConnection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.List;

public class GsnProxy extends HttpServlet {
    
	private static final long serialVersionUID = 1L;
	//private ServletContext servletContext;
    private Logger log;
    private String servicesUrl=ConfigFactory.load().getString("gsn.services.proxy");
    
    public void init(ServletConfig servletConfig) throws ServletException {
        //servletContext = servletConfig.getServletContext();
        log = LoggerFactory.getLogger(GsnProxy.class.getName());
    }
    
    public void doPost(HttpServletRequest request, HttpServletResponse response){
    	return;
    }
    
    public void doGet(HttpServletRequest request, HttpServletResponse response){
        BufferedInputStream webToProxyBuf = null;
        BufferedOutputStream proxyToClientBuf = null;
        HttpURLConnection con;
        
        try{
            int statusCode;
            int oneByte;
            String methodName;
            log.info(request.getLocalName());
            log.info(request.getPathInfo());
            log.info(request.getServletPath());
            log.info(request.getLocalAddr());
            String urlString = servicesUrl+request.getServletPath()+request.getPathInfo(); //request.getRequestURL().toString();
            String queryString = request.getQueryString();
            
            urlString += queryString==null?"":"?"+queryString;
            URL url = new URL(urlString);
            
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
            
            for( Iterator i = con.getHeaderFields().entrySet().iterator() ; i.hasNext() ;){
                Map.Entry mapEntry = (Map.Entry)i.next();
                if(mapEntry.getKey()!=null)
                    response.setHeader(mapEntry.getKey().toString(), ((List)mapEntry.getValue()).get(0).toString());
            }
            
            webToProxyBuf = new BufferedInputStream(con.getInputStream());
            proxyToClientBuf = new BufferedOutputStream(response.getOutputStream());

            while ((oneByte = webToProxyBuf.read()) != -1) 
                proxyToClientBuf.write(oneByte);

            proxyToClientBuf.flush();
            proxyToClientBuf.close();

            webToProxyBuf.close();
            con.disconnect();
            
        }catch(Exception e){
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        finally{
        }
    }
}
