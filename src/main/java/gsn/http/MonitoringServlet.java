/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
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
* File: src/gsn/http/MonitoringServlet.java
*
* @author Julien Eberle
*
*/

package gsn.http;

import gsn.Main;
import gsn.Monitorable;

import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;




public class MonitoringServlet extends HttpServlet {

    private static transient Logger logger = Logger.getLogger(MonitoringServlet.class);
    
    /**
     * Return statistics about the GSN server
     * if the "header" parameter is "true", returns the column names (comma separated).
     * Otherwise returns only the values (comma separated)
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setHeader("Cache-Control", "no-store");
        response.setDateHeader("Expires", 0);
        response.setHeader("Pragma", "no-cache");

        String header = HttpRequestUtils.getStringParameter("header", "false", request);

        StringBuilder values = new StringBuilder();
        StringBuilder head = new StringBuilder();
        
        for (Monitorable m : Main.getInstance().getToMonitor()){
        	Hashtable<String,Object> h = m.getStatistics();
        	for (Map.Entry<String,Object> e : h.entrySet()){
        		head.append(e.getKey()).append(",");
        		values.append(e.getValue()).append(",");
        	}
        }
        
		if (header.equalsIgnoreCase("true")){
			values = head.append("\n").append(values);
		}
	    response.getWriter().write(values.toString());
    }


    public void doPost(HttpServletRequest request, HttpServletResponse res) throws ServletException, IOException {
        doGet(request, res);
    }

}
