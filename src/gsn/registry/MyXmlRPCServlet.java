/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 * Creation time : Dec 7, 2006@8:08:29 PM<br> *
 */
package gsn.registry;

import gsn.GSNRequestHandler;
import gsn.Main;

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
public class MyXmlRPCServlet extends XmlRpcServlet{
   
   private final transient Logger   logger                = Logger.getLogger ( MyXmlRPCServlet.class );
   
   protected XmlRpcServletServer  newXmlRpcServer(ServletConfig pConfig ) throws XmlRpcException {
     return new XmlRpcServletServer ( ) {
         protected XmlRpcHttpRequestConfigImpl newConfig ( HttpServletRequest pRequest ) {
            return new MyConfig ( pRequest.getRemoteAddr ( ) );
         }
      };
   }
   
   protected PropertyHandlerMapping newPropertyHandlerMapping (java.net.URL url) throws java.io.IOException,  XmlRpcException{
      PropertyHandlerMapping mapping = new PropertyHandlerMapping ();
      /**
       * Stupid bug ?!!!, always setRequestProcessorFactoryFactory before starting to manupluate the mapping
       */
      mapping.setRequestProcessorFactoryFactory (factory);
      if (Main.getContainerConfig ()==null)
         mapping.addHandler ("registry",RegistryRequestHandler.class);
      else
         mapping.addHandler ("gsn", GSNRequestHandler.class);
      return mapping;
   }
   
   RequestProcessorFactoryFactory factory = new RequestProcessorFactoryFactory.RequestSpecificProcessorFactoryFactory (){
      protected Object getRequestProcessor ( Class arg0 , XmlRpcRequest arg1 ) throws XmlRpcException {
         Object output = super.getRequestProcessor ( arg0 , arg1 );
         ((RequestInitializableRequestProcessor)output).init (  ( MyConfig ) arg1.getConfig ( ) );
         return output;
      }
   };
   
}
