package gsn.web;

import gsn.Container;
import gsn.vsensor.http.AddressingReqHandler;
import gsn.vsensor.http.ContainerInfoHandler;
import gsn.vsensor.http.OneShotQueryHandler;
import gsn.vsensor.http.OneShotQueryWithAddressingHandler;
import gsn.vsensor.http.OutputStructureHandler;
import gsn.vsensor.http.RequestHandler;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 * Creation time : Dec 5, 2006@5:46:41 PM<br>
 */
public class ControllerServlet extends HttpServlet {
   
   private static transient Logger                                      logger                             = Logger.getLogger ( ControllerServlet.class );
   
   /**
    * HTTP RETURN CODES :
    * ---------------------------------------------------------------------
    */
   
   public static final int    CORRECT_REQUEST                        = 200;
   
   public static final int    UNSUPPORTED_REQUEST_ERROR              = 400;
   
   public static final int    MISSING_VSNAME_ERROR                   = 401;
   
   public static final int    ERROR_INVALID_VSNAME                   = 402;
   
   public static final int    WRONG_VSFIELD_ERROR                    = 403;
   /**
    * HTTP REQUEST CODE ==================================================
    */
   
   public static final int    REQUEST_ONE_SHOT_QUERY_WITH_ADDRESSING = 116;
   
   public static final int    REQUEST_ONE_SHOT_QUERY                 = 114;
   
   public static final int    REQUEST_OUTPUT_FORMAT                  = 113;
   
   public static final int    REQUEST_ADDRESSING                     = 115;
   
   /**
    * getting the request from the web and handling it.
    */
   public void doGet ( HttpServletRequest request , HttpServletResponse response ) throws ServletException , IOException {
      response.setContentType ( "text/xml" );
      // to be sure it isn't cached
      response.setHeader ( "Expires" , "Sat, 6 May 1995 12:00:00 GMT" );
      response.setHeader ( "Cache-Control" , "no-store, no-cache, must-revalidate" );
      response.addHeader ( "Cache-Control" , "post-check=0, pre-check=0" );
      response.setHeader ( "Pragma" , "no-cache" );
      
      String rawRequest = request.getParameter ( Container.REQUEST );
      int requestType = -1;
      if ( rawRequest == null || rawRequest.trim ( ).length ( ) == 0 ) {
         requestType = 0;
      } else
         try {
            requestType = Integer.parseInt ( ( String ) rawRequest );
         } catch ( Exception e ) {
            logger.debug ( e.getMessage ( ) , e );
            requestType = -1;
         }
      StringBuilder sb = new StringBuilder ( "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" );
      response.getWriter ( ).write ( sb.toString ( ) );
      RequestHandler handler;
      if ( logger.isDebugEnabled ( ) ) logger.debug ( "Received a request with code : " + requestType );
      
      switch ( requestType ) {
      case 0 :
          handler = new ContainerInfoHandler ( );
          if ( handler.isValid ( request , response ) ) handler.handle ( request , response );
          break;   
      case REQUEST_ONE_SHOT_QUERY :
            handler = new OneShotQueryHandler ( );
            if ( handler.isValid ( request , response ) ) handler.handle ( request , response );
            break;
         case REQUEST_ONE_SHOT_QUERY_WITH_ADDRESSING :
            handler = new OneShotQueryWithAddressingHandler ( );
            if ( handler.isValid ( request , response ) ) handler.handle ( request , response );
            break;
         case REQUEST_OUTPUT_FORMAT :
            handler = new OutputStructureHandler ( );
            if ( handler.isValid ( request , response ) ) handler.handle ( request , response );
            break;
         case REQUEST_ADDRESSING :
            handler = new AddressingReqHandler ( );
            if ( handler.isValid ( request , response ) ) handler.handle ( request , response );
            break;
         default :
            response.sendError ( UNSUPPORTED_REQUEST_ERROR , "The requested operation is not supported." );
            break;
      }
   }
   
   public void doPost ( HttpServletRequest request , HttpServletResponse res ) throws ServletException , IOException {
      doGet ( request , res );
   }
 
}
