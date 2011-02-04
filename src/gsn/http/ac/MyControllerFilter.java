package gsn.http.ac;


import gsn.Main;
import gsn.http.WebConstants;
import org.apache.log4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Behnaz Bostanipour
 * Date: Apr 15, 2010
 * Time: 7:25:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class MyControllerFilter implements Filter {

    private FilterConfig config = null;
    private static transient Logger logger = Logger.getLogger(MyControllerFilter.class);

    public void init(FilterConfig config) throws ServletException {
        this.config = config;
    }

    public void destroy() {
        config = null;
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest req = (HttpServletRequest) request;
            HttpServletResponse res = (HttpServletResponse) response;
            HttpSession session = req.getSession();
            User user = (User) session.getAttribute("user");
            if (Main.getContainerConfig().isAcEnabled() == false)// do as filter does not exist
            {
                chain.doFilter(request, response);
            } else {

                // check of username and password are given in the URL
                String reqUsername = req.getParameter("username");
                String reqPassword = req.getParameter("password");
                String reqVirtualSensorName = req.getParameter("name");
                String rawRequest = req.getParameter(WebConstants.REQUEST);
                int requestType = -1;
                if (rawRequest == null || rawRequest.trim().length() == 0) {
                    requestType = 0;
                } else
                    try {
                        requestType = Integer.parseInt(rawRequest);
                    } catch (Exception e) {
                        logger.debug(e.getMessage(), e);
                        requestType = -1;
                    }

                if ("/data".equals(req.getServletPath())) {   // /data request uses vsname instead of name
                    reqVirtualSensorName = req.getParameter("vsname");
                    if (reqVirtualSensorName == null)               // try the other accepted alternative: vsName
                        reqVirtualSensorName = req.getParameter("vsName");
                }

                if ((reqUsername != null) && (reqPassword != null) && (reqVirtualSensorName != null)) {
                    logger.warn("Detected URL-based login"); //TODO: DEBUG ONLY
                    logger.warn("User: " + reqUsername);    //TODO: DEBUG ONLY
                    logger.warn("Pass: " + reqPassword);      //TODO: DEBUG ONLY
                    logger.warn("Name: " + reqVirtualSensorName);      //TODO: DEBUG ONLY
                    logger.warn("Request type: " + requestType); //TODO: debug only

                    User userByURL = UserUtils.allowUserToLogin(reqUsername, reqPassword);

                    if (userByURL == null) {
                        res.sendError(WebConstants.ACCESS_DENIED, "Access denied to the specified user.");
                        return;
                    }

                    boolean flag = UserUtils.userHasAccessToVirtualSensor(reqUsername, reqPassword, reqVirtualSensorName);
                    logger.warn(flag);
                    if (flag) {
                        chain.doFilter(request, response);
                        return;
                    } else {
                        res.sendError(WebConstants.ACCESS_DENIED, "Access denied to the specified resource.");
                        return;
                    }
                }

                // bypass if servlet is gsn and request is for ContainerInfoHandler

                if (("/gsn".equals(req.getServletPath()) && (requestType == 0 || requestType == 901))
                        || ("/multidata".equals(req.getServletPath()))
                        || ("/field".equals(req.getServletPath()))
                        ) {
                    chain.doFilter(request, response);
                    return;
                }

                //

                if (user == null)// if user has not already logged-in
                {
                    if (req.getQueryString() == null) // if there is no query string in uri, we suppose that target is GSN home
                    {
                        session.setAttribute("login.target", null);

                    } else {

                        /* if there is query string, store it as a target, so to go back to it once logged-in */
                        session.setAttribute("login.target", req.getRequestURL() + "?" + req.getQueryString());
                    }
                    res.setHeader("Cache-Control", "no-cache");
                    res.setHeader("Pragma", "no-cache");
                    res.setHeader("Expires", "0");

                    // redirect to login
                    res.sendRedirect("/gsn/MyLoginHandlerServlet");

                    return;
                } else {
                    //if logged-in, go to the target directly
                    chain.doFilter(request, response);

                }
            }
        }
    }
}
