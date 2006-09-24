package gsn.fastnetwork ;

import java.util.Properties ;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class PostMethod {

   Properties headers = new Properties ( ) ;

   Properties parameters = new Properties ( ) ;

   private String remoteHostAddress ;

   private int remoteHostPort ;

   public PostMethod ( String remoteHostAddress , int remoteHostPort ) {
      this.remoteHostAddress = remoteHostAddress ;
      this.remoteHostPort = remoteHostPort ;
   }

   public void setParameter ( String paramName , byte paramValue ) {

   }

   public void setParameter ( String paramName , short paramValue ) {

   }

   public void setParameter ( String paramName , int paramValue ) {

   }

   public void setParameter ( String paramName , long paramValue ) {

   }

   public void setParameter ( String paramName , boolean paramValue ) {

   }

   public void setParameter ( String paramName , char paramValue ) {

   }

   public void setParameter ( String paramName , byte [ ] paramValue ) {

   }

   public void setParameter ( String paramName , String paramValue ) {

   }

}
