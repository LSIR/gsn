package gsn.http;

import gsn.Main;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class LoginReqHandler implements RequestHandler{

	private static transient Logger                                      logger                             = Logger.getLogger( LoginReqHandler.class );

	private static String userpass =  Main.getContainerConfig( ).getVs_protected_user()+":"+Main.getContainerConfig( ).getVs_protected_password();

	public void handle ( HttpServletRequest request , HttpServletResponse response ) throws IOException {
		response.setContentType("text/plain");
		PrintWriter out = response.getWriter();
		String auth = request.getHeader("Authorization");
		if (!allowUser(auth)) {
			//Not allowed, so report he is unauthorized.
			response.setHeader("WWW-Authenticate", "BASIC realm=\"users\"");
			response.sendError(response.SC_UNAUTHORIZED);
			// Could offer to add him to the allowed user list
		}else {
			// Allowed, redirecting him to the list of the sensors.
			response.sendRedirect("/gsn");
		}
	}
	private boolean allowUser(String auth) throws IOException {
		if (auth == null) return false; // no auth
		if (!auth.toUpperCase().startsWith("BASIC "))
			return false; // we only do BASIC
		// Get encoded user and password, comes after "BASIC "
		String userpassEncoded = auth.substring(6);
		// Decode it, using any base 64 decoder
		sun.misc.BASE64Decoder dec = new sun.misc.BASE64Decoder();
		String userpassDecoded = new String(dec.decodeBuffer(userpassEncoded));
		// Check our user list to see if that user and password are "allowed"
		if (userpass.equals(userpassDecoded))
			return true;
		else
			return false;
	}
	public boolean isValid ( HttpServletRequest request , HttpServletResponse response ) throws IOException {
		return true;
	}
}
