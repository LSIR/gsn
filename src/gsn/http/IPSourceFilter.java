package gsn.http;

import org.apache.log4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class IPSourceFilter implements Filter {

    public final static String ALLOWED_IP = "131.107.151.211";

    private static transient Logger logger = Logger.getLogger( FilterConfig.class );

    private FilterConfig filterConfig;

    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        String ip = request.getRemoteAddr();

        logger.warn("IP: " + ip);

        HttpServletResponse httpResp = null;

        if (response instanceof HttpServletResponse)
            httpResp = (HttpServletResponse) response;

        if (ALLOWED_IP.equals(ip)) {
            chain.doFilter(request, response);
        } else {
            logger.warn("IP: " + ip + " not allowed.");
            httpResp.sendError(HttpServletResponse.SC_FORBIDDEN);
        }

    }

    public void destroy() {}
}
