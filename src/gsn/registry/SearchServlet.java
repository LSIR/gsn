/**
 * @author m_jost
 * Creation time : Dec 12, 2006@2:43:35 PM<br>
 * @web.servlet-mapping url-pattern="/search"
 */

package gsn.registry;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import gsn.vsensor.http.RequestHandler;

public class SearchServlet extends HttpServlet {
	
	private static final transient Logger logger = Logger.getLogger( Registry.class );
	
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
	   
	   public static final int REQUEST_SEARCH_VIRTUAL_SENSORS         = 117;
	   
	   public static final String REQUEST                                = "REQUEST";
	   
	
	   public void doGet ( HttpServletRequest request , HttpServletResponse response ) throws ServletException , IOException {
		      response.setContentType( "text/plain" );
		      //to be sure it isn't cached
		      response.setHeader("Expires", "Sat, 6 May 1995 12:00:00 GMT");
		      response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
		      response.addHeader("Cache-Control", "post-check=0, pre-check=0");
		      response.setHeader("Pragma", "no-cache");
		      
		      String rawRequest = request.getParameter( REQUEST );
		      int requestType = -1;
		      if ( rawRequest == null || rawRequest.trim( ).length( ) == 0 ) {
		         requestType = REQUEST_SEARCH_VIRTUAL_SENSORS;
		      } else
		         try {
		            requestType = Integer.parseInt( ( String ) rawRequest );
		         } catch ( Exception e ) {
		            logger.debug( e.getMessage( ),e );
		            requestType = -1;
		         }
		      RequestHandler handler;
		      if (logger.isDebugEnabled( )) logger.debug("Received a request with code : "+requestType  );
		      
		      switch ( requestType ) {
		         case REQUEST_SEARCH_VIRTUAL_SENSORS :
		            handler = new RegistryVSHandler( );
		            if ( handler.isValid( request , response ) ) handler.handle( request , response );
		            break;
		         default :
		            response.sendError( UNSUPPORTED_REQUEST_ERROR , "The requested operation is not supported." );
		            break;
		      }
		   }
	   public void doPost ( HttpServletRequest request , HttpServletResponse res ) throws ServletException , IOException {
		      doGet ( request , res );
	   }
}
