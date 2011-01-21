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

    public void init(FilterConfig config) throws ServletException
    {
        this.config = config;
    }

    public void destroy()
    {
        config = null;
    }

    public void doFilter(ServletRequest requset, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (requset instanceof HttpServletRequest)
        {
            HttpServletRequest req = (HttpServletRequest) requset;
            HttpServletResponse res = (HttpServletResponse) response;
            HttpSession session = req.getSession();
            User user = (User) session.getAttribute("user");
            if (Main.getContainerConfig().isAcEnabled() == false)// do as filter does not exist
            {
                chain.doFilter(requset, response);
            } else {

                // bypass if servlet is gsn and request is for ContainerInfoHandler
                String rawRequest = req.getParameter(WebConstants.REQUEST);
                int requestType = -1;
                if (rawRequest == null || rawRequest.trim().length() == 0) {
                    requestType = 0;
                } else
                    try {
                        requestType = Integer.parseInt((String) rawRequest);
                    } catch (Exception e) {
                        logger.debug(e.getMessage(), e);
                        requestType = -1;
                    }


                if ( ("/gsn".equals(req.getServletPath()) && (requestType == 0 || requestType == 901))
                        || ("/multidata".equals(req.getServletPath()))
                        || ("/field".equals(req.getServletPath())) 
                        ) {
                    chain.doFilter(requset, response);
                    return;
                }

                //

                if (user == null)// if user has not already loogged-in
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
                    chain.doFilter(requset, response);

                }
            }
        }
    }
}
