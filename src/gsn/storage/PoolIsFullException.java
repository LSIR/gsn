package gsn.storage;

/**
 * This exception can occur in two places : 1. When the upper limit of the pool
 * size for DataBase reached (which is specified in the container's
 * configuraion) 2. When the upper limit of the pool size for a virtual sensor
 * reached (which is specified in the each virtual sensor's configuration file)
 */
public class PoolIsFullException extends Exception {
   
   public PoolIsFullException ( ) {
      super( "Pool is Full exception occured" );
   }
   
   public PoolIsFullException ( String name ) {
      super( "Pool is Full exception occured for : " + name );
   }
}
