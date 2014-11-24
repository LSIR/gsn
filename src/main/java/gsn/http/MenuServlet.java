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
* File: src/gsn/http/MenuServlet.java
*
* @author Timotee Maret
* @author ndawes
*
*/

package gsn.http;

import gsn.Main;
import gsn.http.ac.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.IOException;
import java.io.PrintWriter;

public class MenuServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private Config conf=ConfigFactory.load();
	private boolean newDataPanel=conf.getBoolean("menu.data.newVersion");
	
	public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        PrintWriter out = res.getWriter();
        String selected = req.getParameter("selected");
        out.println("<ul id=\"menu\">");
        out.println("<li" + ("index".equals(selected) ? " class=\"selected\"" : "") + "><a href=\"index.html#home\">home</a></li>");
        if (newDataPanel)
        	out.println("<li" + ("data".equals(selected) ? " class=\"selected\"" : "") + "><a href=\"data.html#data\">data</a></li>");
        else
        	out.println("<li" + ("data".equals(selected) ? " class=\"selected\"" : "") + "><a href=\"databrowse.html#data\">data</a></li>");
        out.println("<li" + ("map".equals(selected) ? " class=\"selected\"" : "") + "><a href=\"map.html#map\">map</a></li>");
        out.println("<li" + ("fullmap".equals(selected) ? " class=\"selected\"" : "") + "><a href=\"fullmap.html#fullmap\">fullmap</a></li>");
        if (Main.getContainerConfig().isAcEnabled()) {
            out.println("<li><a href=\"/gsn/MyAccessRightsManagementServlet\">access rights management</a></li>");
        }
        out.println("</ul>");
        if (Main.getContainerConfig().isAcEnabled()) {
            out.println("<ul id=\"logintext\">" + displayLogin(req) + "</ul>");
        } else {
            out.println("<ul id=\"linkWebsite\"><li><a href=\"https://github.com/LSIR/gsn/\">GSN Home</a></li></ul>");
        }
    }

    private String displayLogin(HttpServletRequest req) {
        String name;
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");
        if (user == null)
            name = "<li><a href=/gsn/MyLoginHandlerServlet> login</a></li>" + "<li><a href=/gsn/MyUserCandidateRegistrationServlet>register</a></li>";
        else {
            name = "<li><a href=/gsn/MyLogoutHandlerServlet> logout </a></li>" + "<li><div id=logintextprime >logged in as: " + user.getUserName() + "&nbsp" + "</div></li>";
        }
        return name;
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

}
