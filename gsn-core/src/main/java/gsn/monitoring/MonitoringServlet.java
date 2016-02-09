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

package gsn.monitoring;

import gsn.Main;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;



public class MonitoringServlet extends HttpServlet {


	private static final long serialVersionUID = 4151528601585755923L;
    
    /**
     * Return statistics about the GSN server
     * 
     * the protocol is similar to the carbon protocol used by Graphite, except the timestamp
     * http://matt.aimonetti.net/posts/2013/06/26/practical-guide-to-graphite-monitoring/
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setHeader("Cache-Control", "no-store");
        response.setDateHeader("Expires", 0);
        response.setHeader("Pragma", "no-cache");

        StringBuilder values = new StringBuilder();
        
        for (Monitorable m : Main.getInstance().getToMonitor()){
        	Hashtable<String,Object> h = m.getStatistics();
        	for (Map.Entry<String,Object> e : h.entrySet()){
        		values.append(e.getKey()).append(" ");
        		values.append(e.getValue()).append("\n");
        	}
        }
        
	    response.getWriter().write(values.toString());
    }


    public void doPost(HttpServletRequest request, HttpServletResponse res) throws ServletException, IOException {
        doGet(request, res);
    }

}
