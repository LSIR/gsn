package gsn.registry;

import gsn.utils.KeyValueImp;

import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

/**
 * Created on December 3, 2006, 10:48 AM
 * 
 * @author ali
 */
public class VSAddress {
   
   private String         defaultField;
   
   private String         addressKeys;
   
   private String         addressValues;
   
   private String         guid;
   
   private String         addressUses;
   
   private transient long creationTime = System.currentTimeMillis( );
   
   private String         description;
   
   private int            remotePort;
   
   private String         remoteVSName;
   
   private String remoteHost;
   
   private ArrayList<KeyValueImp> predicates;
   
   /**
    * @param port Port of the GSN server
    * @param name The name of the virtual sensor we want to register.
    * @param description
    * @param Addressing predicates
    * @param usedResources
    */
   public VSAddress ( int port , String name , String description , Vector predicates , String usedSources,String remoteHost ) {
      this.remotePort = port;
      this.remoteVSName = name;
      StringBuffer tempAddrs = new StringBuffer( );
      StringBuffer tempKeys = new StringBuffer( );
      StringBuffer tempValues = new StringBuffer( );
      tempAddrs.append( name ).append( Registry.SPACE_CHARACTER );
      ArrayList<KeyValueImp> tempPredicates = new ArrayList<KeyValueImp>();
      for ( Object  pred : predicates ) {
          Object[] prediate = (Object[]) pred;
    	 tempAddrs.append( prediate[0].toString() ).append( Registry.SPACE_CHARACTER ).append(  prediate[ 1].toString()  ).append( Registry.SPACE_CHARACTER );
         tempKeys.append(  prediate[ 0 ].toString()  ).append( Registry.SPACE_CHARACTER );
         tempValues.append(  prediate[ 1 ].toString()  ).append( Registry.SPACE_CHARACTER );
         tempPredicates.add(new KeyValueImp(prediate[0].toString(),prediate[1].toString()));
      }
      this.predicates = tempPredicates;
      this.description = ( description == null ? "" : description );
      tempAddrs.append( this.description ).append( Registry.SPACE_CHARACTER );
      this.defaultField = tempAddrs.toString( );
      this.addressKeys = tempKeys.toString( );
      this.addressValues = tempKeys.toString( );
      this.addressUses = usedSources;
      this.remoteHost=remoteHost;
      guid = new StringBuilder( remoteHost ).append( ":" ).append( remotePort ).append( "/" ).append( remoteVSName ).toString( );
         }
   
   public final String getDefaultField ( ) {
      return defaultField;
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
      sb.append( "Address : " ).append( defaultField ).append( "," );
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
   public ArrayList<KeyValueImp> getPredicates ( ) {
      return predicates;
   }
   
}
