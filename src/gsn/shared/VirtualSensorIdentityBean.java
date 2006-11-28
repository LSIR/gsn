package gsn.shared;

import gsn.registry.RegistryImp;
import gsn.utils.KeyValueImp;

import java.util.ArrayList;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.KeyValue;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class VirtualSensorIdentityBean {
   
   private String                 name;
   
   private String                 remoteAddress;
   
   private int                    remotePort;
   
   private ArrayList < KeyValue > predicates;
   
   private long                   latestVisit = 0;
   
   public VirtualSensorIdentityBean ( String name , String address , int port , ArrayList < KeyValue > predicates ) {
      this.name = name;
      this.remoteAddress = address;
      this.remotePort = port;
      this.predicates = predicates;
      this.latestVisit = System.currentTimeMillis( );
   }
   
   public VirtualSensorIdentityBean ( String name , String address , int port ) {
      this.name = name;
      this.remoteAddress = address;
      this.remotePort = port;
      this.latestVisit = System.currentTimeMillis( );
   }
   
   public VirtualSensorIdentityBean ( HttpServletRequest req ) {
      String name = req.getHeader( RegistryImp.VS_NAME );
      int port = Integer.parseInt( req.getHeader( RegistryImp.VS_PORT ) );
      String address = req.getRemoteAddr( );
      Enumeration < String > keys = req.getHeaders( RegistryImp.VS_PREDICATES_KEYS );
      Enumeration < String > values = req.getHeaders( RegistryImp.VS_PREDICATES_VALUES );
      ArrayList < KeyValue > predicates = new ArrayList < KeyValue >( );
      if ( keys != null ) {
         while ( keys.hasMoreElements( ) ) {
            String key = keys.nextElement( );
            String value = values.nextElement( );
            predicates.add( new KeyValueImp( key , value ) );
         }
      }
      
      this.name = name;
      this.remoteAddress = address;
      this.remotePort = port;
      this.predicates = predicates;
      this.latestVisit = System.currentTimeMillis( );
   }
   
   public boolean matches ( ArrayList < KeyValue > input ) {
      nextRequirement : for ( KeyValue askedPredicate : input ) {
         for ( KeyValue myPredicates : predicates )
            if ( ( ( String ) myPredicates.getKey( ) ).equalsIgnoreCase( ( String ) askedPredicate.getKey( ) ) ) if ( ( ( String ) myPredicates.getValue( ) )
                  .equalsIgnoreCase( ( String ) askedPredicate.getValue( ) ) ) continue nextRequirement;
         return false;
      }
      return true;
   }
   
   public boolean equals ( Object obj ) {
      if ( obj == null || !( obj instanceof VirtualSensorIdentityBean ) ) return false;
      VirtualSensorIdentityBean input = ( VirtualSensorIdentityBean ) obj;
      return ( input.name.equals( name ) && input.remoteAddress.equals( remoteAddress ) && ( input.remotePort == remotePort ) );
   }
   
   /**
    * @return Returns the name.
    */
   public String getVSName ( ) {
      return name;
   }
   
   /**
    * @return Returns the predicates.
    */
   public ArrayList < KeyValue > getPredicates ( ) {
      return predicates;
   }
   
   /**
    * @return Returns the remoteAddress.
    */
   public String getRemoteAddress ( ) {
      return remoteAddress;
   }
   
   /**
    * @return Returns the remotePort.
    */
   public int getRemotePort ( ) {
      return remotePort;
   }
   
   /**
    * @return Returns the latestVisit.
    */
   public long getLatestVisit ( ) {
      return latestVisit;
   }
   
   /**
    * @param latestVisit The latestVisit to set.
    */
   public void setLatestVisit ( long latestVisit ) {
      this.latestVisit = latestVisit;
   }

   private String guid;
   public synchronized String getGUID ( ) {
      if (guid ==null)
         guid = new StringBuffer(getRemoteAddress( )).append(  ":").append(getRemotePort( ) ).append( "/").append(getVSName( )).toString( );
      return guid;
   }
   
}
