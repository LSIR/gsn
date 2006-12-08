package gsn.registry;

import java.util.Date;
import java.util.Vector;

/**
 * Created on December 3, 2006, 10:48 AM
 * 
 * @author ali
 */
public class VSAddress {
   
   private String         addresses;
   
   private String         addressKeys;
   
   private String         addressValues;
   
   private String         guid;
   
   private String         addressUses;
   
   private transient long creationTime = System.currentTimeMillis( );
   
   private String         description;
   
   private int            remotePort;
   
   private String         remoteVSName;
   
   private String remoteHost;
   
   private Vector<Vector<String>>  predicates;
   
   /**
    * @param port Port of the GSN server
    * @param name The name of the virtual sensor we want to register.
    * @param description
    * @param Addressing predicates
    * @param usedResources
    */
   public VSAddress ( int port , String name , String description , Vector<Vector<String>> predicates , String usedSources,String remoteHost ) {
      this.remotePort = port;
      this.remoteVSName = name;
      StringBuffer tempAddrs = new StringBuffer( );
      StringBuffer tempKeys = new StringBuffer( );
      StringBuffer tempValues = new StringBuffer( );
      tempAddrs.append( name ).append( Registry.SPACE_CHARACTER );
      for ( Vector<String > pred : predicates ) {
         tempAddrs.append( pred.get( 0 ) ).append( Registry.SPACE_CHARACTER ).append(  pred.get( 1 )  ).append( Registry.SPACE_CHARACTER );
         tempKeys.append(  pred.get( 0 )  ).append( Registry.SPACE_CHARACTER );
         tempValues.append(  pred.get( 1 )  ).append( Registry.SPACE_CHARACTER );
      }
      this.predicates = predicates;
      this.description = ( description == null ? "" : description );
      this.addresses = tempAddrs.toString( );
      this.addressKeys = tempKeys.toString( );
      this.addressValues = tempKeys.toString( );
      this.addressUses = usedSources;
      this.remoteHost=remoteHost;
   }
   
   public final String getAddress ( ) {
      return addresses;
   }
   
   public final String getAddressKeys ( ) {
      return addressKeys;
   }
   
   public final String getAddressValues ( ) {
      return addressValues;
   }
   
   public final String getUses ( ) {
      return addressUses;
   }
   
   /**
    * @return the key
    */
   public String getGUID ( ) {
      return guid;
   }
   
   /**
    * @return the Creation time of this object;
    */
   public long getAge ( ) {
      return System.currentTimeMillis( ) - creationTime;
   }
   
   public void initGUID ( String ipAddress ) {
      guid = new StringBuffer( ipAddress ).append( ":" ).append( remotePort ).append( "/" ).append( remoteVSName ).toString( );
   }
   
   public int hashCode ( ) {
      return guid.hashCode( );
   }
   
   public boolean equals ( Object obj ) {
      if ( this == obj ) return true;
      if ( obj == null ) return false;
      if ( getClass( ) != obj.getClass( ) ) return false;
      final VSAddress other = ( VSAddress ) obj;
      if ( guid == null ) {
         if ( other.guid != null ) return false;
      } else if ( !guid.equals( other.guid ) ) return false;
      return true;
   }
   
   public String toString ( ) {
      StringBuilder sb = new StringBuilder( );
      sb.append( "VSAddress [ GUID : " ).append( guid ).append( "," );
      sb.append( "Creation : " ).append( new Date( creationTime ) ).append( "," );
      sb.append( "Address : " ).append( addresses ).append( "," );
      sb.append( "Address keys : " ).append( addressKeys ).append( "," );
      sb.append( "Address Values : " ).append( addressValues ).append( "," );
      sb.append( "Uses : " ).append( addressUses ).append( "]" );
      return sb.toString( );
   }
   
   /**
    * @return the description
    */
   public String getDescription ( ) {
      return description;
   }

   
   /**
    * @return the predicates
    */
   public Vector<Vector<String>> getPredicates ( ) {
      return predicates;
   }
   
}
