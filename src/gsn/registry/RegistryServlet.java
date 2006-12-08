/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 * Creation time : Dec 7, 2006@8:08:29 PM<br> *  
 */
package gsn.registry;

import java.io.IOException;
import java.net.URL;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.common.XmlRpcHttpRequestConfigImpl;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory;
import org.apache.xmlrpc.webserver.XmlRpcServlet;
import org.apache.xmlrpc.webserver.XmlRpcServletServer;


/**
 * @author alisalehi
 *
 */
public class RegistryServlet extends XmlRpcServlet{
   
   private final transient Logger   logger                = Logger.getLogger( RegistryServlet.class );
   
   protected XmlRpcServletServer newXmlRpcServer ( ServletConfig pConfig ) throws XmlRpcException {
      return new XmlRpcServletServer( ) {
         protected XmlRpcHttpRequestConfigImpl newConfig ( HttpServletRequest pRequest ) {
            return new MyConfig( pRequest.getRemoteAddr( ) );
         }
      };
   }
   
   protected PropertyHandlerMapping newPropertyHandlerMapping(URL url) throws IOException, XmlRpcException {
      PropertyHandlerMapping mapping = super.newPropertyHandlerMapping(url);
      RequestProcessorFactoryFactory factory = new RequestProcessorFactoryFactory.RequestSpecificProcessorFactoryFactory(){
         protected Object getRequestProcessor ( Class arg0 , XmlRpcRequest arg1 ) throws XmlRpcException {
            Object output = super.getRequestProcessor( arg0 , arg1 );
            ((RequestInitializableRequestProcessor)output).init(  ( MyConfig ) arg1.getConfig( ) );
            return output;
         }
       };
       mapping.setRequestProcessorFactoryFactory(factory);
       return mapping;
   }
}
