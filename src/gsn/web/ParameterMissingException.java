package gsn.web ;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class ParameterMissingException extends Exception {

   public ParameterMissingException ( ) {
      super ( ) ;

   }

   public ParameterMissingException ( String message ) {
      super ( message ) ;

   }

   public ParameterMissingException ( String message , Throwable cause ) {
      super ( message , cause ) ;

   }

   public ParameterMissingException ( Throwable cause ) {
      super ( cause ) ;

   }

}
