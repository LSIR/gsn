package gsn.registry;

import gsn.beans.ContainerConfig;
import gsn.beans.VSensorConfig;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import org.apache.commons.collections.KeyValue;

/**
 * Created on December 3, 2006, 10:48 AM
 * @author ali
 */
public class VSAddress implements Serializable{
   
   private  String addresses;
   private  String addressKeys;
   private  String addressValues;
   private  String guid;
   private  String addressUses;
   private transient long   creationTime ;
   
   
   public VSAddress (ContainerConfig cc, VSensorConfig vs) {
      int port = cc.getContainerPort ();
      String name = vs.getVirtualSensorName ();
      guid = new StringBuffer (Registry.COL).append (port ).append ( "/").append (name).toString ( );
      StringBuffer tempAddrs = new StringBuffer ();
      StringBuffer tempKeys = new StringBuffer ();
      StringBuffer tempValues = new StringBuffer ();
      tempAddrs.append ( name ).append ( Registry.SPACE_CHARACTER );
      ArrayList < KeyValue > predicates = vs.getAddressing ();
      for ( KeyValue predicate :predicates) {
         tempAddrs.append ( predicate.getKey ( ) ).append ( Registry.SPACE_CHARACTER ).append ( predicate.getValue ( ) ).append ( Registry.SPACE_CHARACTER );
         tempKeys.append ( predicate.getKey ( ) ).append ( Registry.SPACE_CHARACTER );
         tempValues.append ( predicate.getValue ( ) ).append ( Registry.SPACE_CHARACTER );
      }
      this.addresses=tempAddrs.toString ();
      this.addressKeys=tempKeys.toString ();
      this.addressValues=tempKeys.toString ();
      this.addressUses=vs.getUsedSources ().toString ();
   }
   
   public final String getAddress (){
      return addresses;
   }
   public final String getAddressKeys (){
      return addressKeys;
   }
   public final String getAddressValues (){
      return addressValues;
   }
   public final String getUses (){
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
      return System.currentTimeMillis ()-creationTime;
   }
   public void setCreationTime (long creationTime){
      this.creationTime=creationTime;
   }
   public void initGUID (String ipAddress){
      // TODO, This implementation is wrong.
      guid = new StringBuffer (ipAddress).append (this.guid).toString ();
   }
   
   public int hashCode ( ) {
      return guid.hashCode ( );
   }
   
   public boolean equals ( Object obj ) {
      if ( this == obj ) return true;
      if ( obj == null ) return false;
      if ( getClass ( ) != obj.getClass ( ) ) return false;
      final VSAddress other = ( VSAddress ) obj;
      if ( guid == null ) {
         if ( other.guid != null ) return false;
      } else if ( !guid.equals ( other.guid ) ) return false;
      return true;
   }
   public String toString (){
      StringBuilder sb = new StringBuilder ();
      sb.append ("VSAddress [ GUID : ").append (guid).append (",");
      sb.append ("Creation : ").append (new Date (creationTime)).append (",");
      sb.append ("Address : ").append (addresses).append (",");
      sb.append ("Address keys : ").append (addressKeys).append (",");
      sb.append ("Address Values : ").append (addressValues).append (",");
      sb.append ("Uses : ").append (addressUses).append ("]");
      return sb.toString ();
   }   
}
