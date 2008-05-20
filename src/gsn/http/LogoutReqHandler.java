package gsn.http;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class LogoutReqHandler implements RequestHandler{

	   private static transient Logger                                      logger                             = Logger.getLogger( LogoutReqHandler.class );

	   public void handle ( HttpServletRequest request , HttpServletResponse response ) throws IOException {
		   
	   }
	   
	   public boolean isValid ( HttpServletRequest request , HttpServletResponse response ) throws IOException {
		   return false;
	   }
}
