package gsn.shared;

import java.util.ArrayList;

import org.apache.commons.collections.KeyValue;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public interface Registry {
   
   public final String REQUEST              = "REQUEST";
   
   public final int    REGISTER             = 100;
   
   public final int    DEREGISTER           = 101;
   
   public final int    QUERY                = 102;
   
   public final String VS_NAME              = "NAME";
   
   public final String VS_PORT              = "PORT";
   
   public final String VS_PREDICATES_VALUES = "VSensorPredicatesValues";
   
   public final String VS_PREDICATES_KEYS   = "VSensorPredicatesKeys";
   
   public final String VS_HOST              = "HOST";
   
   public abstract void addVirtualSensor ( VirtualSensorIdentityBean newVirtualSensorIdentity );
   
   public abstract void removeVirtualSensor ( VirtualSensorIdentityBean virtualSensorIdentity );
   
   public abstract ArrayList < VirtualSensorIdentityBean > findVSensor ( ArrayList < KeyValue > predicates );
   
}
