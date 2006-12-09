package gsn.registry;

import gsn.DirectoryRefresher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;  
import java.util.Vector;

import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 * Creation time : Dec 7, 2006@7:50:21 PM<br> *  
 */
public class RegistryRequestHandler implements RequestInitializableRequestProcessor {
   
   private String remoteHost;

   private static transient Logger logger             = Logger.getLogger( RegistryRequestHandler.class );
   
   private static  List < VSAddress >                     additionList           = Collections.synchronizedList( new ArrayList < VSAddress >( ) );
   
   private static  List < VSAddress >                    additionListTmp           = Collections.synchronizedList( new ArrayList < VSAddress >( ) );

   public void init ( MyConfig pConfig ) {
      this.remoteHost = pConfig.getRemoteAddress( );
   }   
   
   public boolean addVirtualSensor ( int port, String vsName, String description, Vector<Vector<String>> predicates, String usedResources) throws Exception {
        synchronized ( additionList ) {
         additionList.add( new VSAddress(port,vsName,description,predicates,usedResources,remoteHost) );
      }
      return true;
   }
   public static List<VSAddress> getList(){
      synchronized ( additionList ) {
         additionListTmp.clear( );
         List < VSAddress > tmp = additionList;
         additionList = additionListTmp;
         additionListTmp = tmp;
         return tmp;
      }
   }
    
}
