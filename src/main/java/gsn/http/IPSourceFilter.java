/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
* 
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: src/gsn/http/IPSourceFilter.java
*
* @author Timotee Maret
* @author Behnaz Bostanipour
*
*/

package gsn.http;

import org.apache.log4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class IPSourceFilter implements Filter {

    private static transient Logger logger = Logger.getLogger( IPSourceFilter.class );

    private FilterConfig filterConfig;

    private String[] allowedSrcIps = null;

    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
        allowedSrcIps = filterConfig.getInitParameter("allowedIps").split(";");
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        String ip = request.getRemoteAddr();

        HttpServletResponse httpResp = null;

        if (response instanceof HttpServletResponse)
            httpResp = (HttpServletResponse) response;

        if (isAllowed(ip)) {
            chain.doFilter(request, response);
        } else {
            logger.warn("IP: " + ip + " not allowed.");
            httpResp.sendError(HttpServletResponse.SC_FORBIDDEN);
        }

    }

    public void destroy() {}

    private boolean isAllowed (String ip) {
        for (String aip : allowedSrcIps) {
            if (aip.equals(ip))
                return true;
        }
        return false;
    }
}
