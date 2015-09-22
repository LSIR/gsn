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
* File: src/gsn/http/GeoDataServlet.java
*
* @author Timotee Maret
* @author Sofiane Sarni
*
*/

package gsn.http;

import com.vividsolutions.jts.io.ParseException;
import gsn.Main;
import gsn.http.ac.User;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

public class GeoDataServlet extends HttpServlet {

    private static transient Logger logger = LoggerFactory.getLogger(GeoDataServlet.class);
    private boolean usePostGIS = true; // by default use JTS
    private User user = null;
    private boolean useUnion;
    private boolean debugMode;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        CheckGISToolkitToUse();

        try {

            if (Main.getContainerConfig().isAcEnabled()) {
                HttpSession session = request.getSession();
                user = (User) session.getAttribute("user");
                response.setHeader("Cache-Control", "no-store");
                response.setDateHeader("Expires", 0);
                response.setHeader("Pragma", "no-cache");
            }

            String env = HttpRequestUtils.getStringParameter("env", null, request); // e.g. "POLYGON ((0 0, 0 100, 100 100, 100 0, 0 0))";
            String query = HttpRequestUtils.getStringParameter("query", null, request);
            String union = HttpRequestUtils.getStringParameter("union", null, request);
            String debug = HttpRequestUtils.getStringParameter("debug", null, request);

            if (debug!= null && debug.trim().toLowerCase().compareTo("true") == 0)
                debugMode = true;
            else
                debugMode = false;

            if (union != null)
                useUnion = true;
            else
                useUnion = false;

            if (usePostGIS)
                response.getWriter().write(runPostGIS(env, query, union));
            else
                response.getWriter().write(runJTS(env, query, union));

        } catch (ParseException e) {
            logger.warn(e.getMessage(), e);
        }
    }

    /*
   * Searches within the list of provided sensors
   * for the ones matching AC credentials for the current user
   * */
    public String getMatchingSensors(ArrayList<String> sensors) {
        StringBuilder matchingSensors = new StringBuilder();

        for (String vsName : sensors) {
            if (!Main.getContainerConfig().isAcEnabled() || (user != null && (user.hasReadAccessRight(vsName) || user.isAdmin()))) {
                matchingSensors.append(vsName);
                matchingSensors.append(GetSensorDataWithGeo.SEPARATOR);
            }
        }
        if (matchingSensors.length()>0)
            matchingSensors.setLength(matchingSensors.length() - 1); // remove the last SEPARATOR
        return matchingSensors.toString();
    }

    public String runJTS(String env, String query, String union) throws ParseException {

        StringBuilder response = new StringBuilder();

        GetSensorDataWithGeo.buildGeoIndex();


        ArrayList<String> sensors = GetSensorDataWithGeo.getListOfSensors(env);
        String matchingSensors = getMatchingSensors(sensors);

        if (matchingSensors.length() == 0) {
            response.append("# No matching sensors for envelope: "+env);
            return response.toString();
        }

        if (debugMode) {
            response.append("# List of all sensors: \n# " + GetSensorDataWithGeo.getListOfSensors().replaceAll("\n","\n# ") + "\n");
            response.append("# Envelope: " + env + "\n");

            response.append("# List of all sensors within envelope: \n# " + matchingSensors + "\n");

            response.append("# Query:" + query + "\n");
            response.append("# Result: \n");
        }
        if (useUnion)
            response.append(GetSensorDataWithGeo.executeQueryWithUnion(env, matchingSensors.toString(), query, union));
        else
            response.append(GetSensorDataWithGeo.executeQuery(env, matchingSensors.toString(), query));

        return response.toString();

    }

    public String runPostGIS(String env, String query, String union) throws ParseException {

        StringBuilder response = new StringBuilder();

        GetSensorDataWithGeoPostGIS.buildGeoIndex();

        ArrayList<String> sensors = GetSensorDataWithGeoPostGIS.getListOfSensors(env);
        String matchingSensors = getMatchingSensors(sensors);

        if (matchingSensors.length() == 0) {
            response.append("# No matching sensors for envelope: "+env);
            return response.toString();
        }

        if (debugMode) {
            response.append("# List of all sensors: \n# " + GetSensorDataWithGeoPostGIS.getListOfSensors().replaceAll("\n","\n# ") + "\n");
            response.append("# Envelope: " + env + "\n");

            response.append("# List of all sensors within envelope: \n" + matchingSensors + "\n");

            response.append("# Query:" + query + "\n");
            response.append("# Result: \n");
        }
        if (useUnion)
            response.append(GetSensorDataWithGeoPostGIS.executeQueryWithUnion(env, matchingSensors.toString(), query, union));
        else
            response.append(GetSensorDataWithGeoPostGIS.executeQuery(env, matchingSensors.toString(), query));

        return response.toString();

    }

    public void doPost(HttpServletRequest request, HttpServletResponse res) throws ServletException, IOException {
        doGet(request, res);
    }

    public void CheckGISToolkitToUse() {

        usePostGIS = false; // use JTS by default

        Properties p = new Properties();
        try {
            p.load(new FileInputStream(GetSensorDataWithGeoPostGIS.CONF_SPATIAL_PROPERTIES_FILE));
        } catch (IOException e) {
            p = null;
            logger.warn(e.getMessage(), e);
        }

        if (p != null) {
            String typeOFGIS = p.getProperty("type");
            if (typeOFGIS != null)
                if (typeOFGIS.trim().toLowerCase().equals("postgis")) {
                    usePostGIS = true;
                }
        }

        if (usePostGIS)
            logger.warn("Using PostGIS");
        else
            logger.warn("Using JTS");
    }


}
