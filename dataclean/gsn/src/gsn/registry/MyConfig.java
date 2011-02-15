package gsn.registry;

import org.apache.xmlrpc.common.XmlRpcHttpRequestConfigImpl;

public class MyConfig extends XmlRpcHttpRequestConfigImpl {
   
   String remoteAddress;
   
   public MyConfig ( String remoteAddress ) {
      this.remoteAddress = remoteAddress;
   }
   
   public String getRemoteAddress ( ) {
      return remoteAddress;
   }
}
