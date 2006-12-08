package gsn.registry;

import org.apache.xmlrpc.common.XmlRpcHttpRequestConfigImpl;
/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 * Creation time : Dec 7, 2006@7:40:05 PM<br> *  
 */

public class MyConfig extends XmlRpcHttpRequestConfigImpl {
   
   String remoteAddress;
   
   public MyConfig ( String remoteAddress ) {
      this.remoteAddress = remoteAddress;
   }
   
   public String getRemoteAddress ( ) {
      return remoteAddress;
   }
}
