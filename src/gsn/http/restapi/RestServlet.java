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
* File: src/gsn/http/restapi/RestServlet.java
*
* @author Ivo Dimitrov
* @author Sofiane Sarni
* @author Milos Stojanovic
*
*/

package gsn.http.restapi;

import gsn.Main;
import gsn.Mappings;
import gsn.http.ac.DataSource;
import gsn.http.ac.User;
import gsn.http.ac.UserUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class RestServlet extends HttpServlet {

    private static transient Logger logger = Logger.getLogger(RestServlet.class);

    private static final int REQUEST_UNKNOWN = -1;
    private static final int REQUEST_GET_ALL_SENSORS = 0;
    private static final int REQUEST_GET_MEASUREMENTS_FOR_SENSOR = 1;
    private static final int REQUEST_GET_MEASUREMENTS_FOR_SENSOR_FIELD = 2;
    private static final int REQUEST_GET_GEO_DATA_FOR_SENSOR = 3;
    private static final int REQUEST_GET_PREVIEW_MEASUREMENTS_FOR_SENSOR_FIELD = 4;
    private static final int REQUEST_GET_GRIDS = 5;
    private static final int REQUEST_GET_GRID2CELL = 6;

    private static final int HTTP_STATUS_BAD = 203;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        GetRequestHandler getRequestHandler = new GetRequestHandler();

        //response.getWriter().write(debugRequest(request, response));
        RestResponse restResponse = null;

        String sensor = null;
        String field = null;
        String str_from = null;
        String str_to = null;
        String str_size = null;
        String str_date = null;
        String str_user = null;
        String str_pass = null;
        User user = null;
        boolean isJSON = true;
        int returnedCSVStatus = RestResponse.HTTP_STATUS_OK;

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        Date currentDate = Calendar.getInstance().getTime();
        String datetime = dateFormat.format(currentDate);

        if (Main.getContainerConfig().isAcEnabled()) {     // added
            str_user = request.getParameter("username");
            str_pass = request.getParameter("password");
            if ((str_user != null) && (str_pass != null)) {
                user = UserUtils.allowUserToLogin(str_user, str_pass);
            }
        }
        // String entireURL = request.getRequestURI()+"?"+request.getQueryString();
       // System.out.println("URL "+entireURL);
        PrintWriter out = response.getWriter();
        switch (determineRequest(request.getRequestURI())) {

            case REQUEST_GET_ALL_SENSORS:
                if (Main.getContainerConfig().isAcEnabled() && (user == null)) { // if there is no user with these credentials
                    isJSON = false;
                    response.setContentType("text/csv");
                    response.setHeader("Content-Disposition", "attachment;filename=\"error_no_user.csv\"");
                    out.print("# There is no user with the provided username and password");
                    returnedCSVStatus = HTTP_STATUS_BAD;
                } else if (Main.getContainerConfig().isAcEnabled() && (user != null)) {
                    // restResponse = getRequestHandler.getSensors(user);
                    isJSON = false;
                    response.setContentType("text/csv");
                    response.setHeader("Content-Disposition", "attachment;filename=\"multiple_sensors_"+datetime+".csv\"");
                    getRequestHandler.getSensorsInfo(user, out);
                } else {
                    restResponse = getRequestHandler.getSensors();
                }
                break;
            case REQUEST_GET_MEASUREMENTS_FOR_SENSOR_FIELD:
                sensor = parseURI(request.getRequestURI())[3];
                if ( Mappings.getConfig(sensor) != null ) {
                    field = parseURI(request.getRequestURI())[4];
                    str_from = request.getParameter("from");
                    str_to = request.getParameter("to");
                    if ( Main.getContainerConfig().isAcEnabled() && (user != null) )  { // if the AC is enabled and there is an user  // added
                        response.setContentType("text/csv");

                        if (!user.hasReadAccessRight(sensor) && !user.isAdmin() && DataSource.isVSManaged(sensor)) {  // if the user doesn't have access to this sensor
                            response.setHeader("Content-Disposition", "attachment;filename=\"error_no_sensor_access.csv\"");
                            out.print("# The user '"+user.getUserName()+"' doesn't have access to the sensor '"+sensor+"'");
                            returnedCSVStatus = HTTP_STATUS_BAD;
                        } else {
                            String size = request.getParameter("size");
                            response.setHeader("Content-Disposition", "attachment;filename=\"sensor_field_"+field+"_"+datetime+".csv\"");
                            returnedCSVStatus = getRequestHandler.getMeasurementsForSensorField(sensor, field, str_from, str_to, size, out);
                            if (returnedCSVStatus != RestResponse.HTTP_STATUS_OK) returnedCSVStatus = HTTP_STATUS_BAD;  // adapt to this error msg
                        }
                        isJSON = false;
                    } else if (Main.getContainerConfig().isAcEnabled() && (user == null)) { // if there is no user with these credentials
                        response.setContentType("text/csv");
                        response.setHeader("Content-Disposition", "attachment;filename=\"error_no_user.csv\"");
                        out.print("# There is no user with the provided username and password");
                        returnedCSVStatus = HTTP_STATUS_BAD;
                        isJSON = false;
                    }  else {    // do execution without AC
                        restResponse = getRequestHandler.getMeasurementsForSensorField(sensor, field, str_from, str_to);
                    }
                } else {
                    response.setContentType("text/csv");
                    response.setHeader("Content-Disposition", "attachment;filename=\"error_no_such_sensor.csv\"");
                    out.print("# The virtual sensor '"+sensor+"' doesn't exist in GSN!");
                    returnedCSVStatus = HTTP_STATUS_BAD;
                    isJSON = false;
                }
                break;
            case REQUEST_GET_MEASUREMENTS_FOR_SENSOR:
                sensor = parseURI(request.getRequestURI())[3];
                if ( Mappings.getConfig(sensor) != null ) {
                    str_from = request.getParameter("from");
                    str_to = request.getParameter("to");
                    if ( Main.getContainerConfig().isAcEnabled() && (user != null) )  { // if the AC is enabled and there is an user  // added
                        response.setContentType("text/csv");

                        if ( !user.hasReadAccessRight(sensor) && !user.isAdmin() && DataSource.isVSManaged(sensor)) {  // if the user doesn't have access to this sensor
                            response.setHeader("Content-Disposition", "attachment;filename=\"error_no_sensor_access.csv\"");
                            out.print("# The user '"+user.getUserName()+"' doesn't have access to the sensor '"+sensor+"'");
                            returnedCSVStatus = HTTP_STATUS_BAD;
                        } else {
                            response.setHeader("Content-Disposition", "attachment;filename=\"sensor_"+sensor+"_fields_"+datetime+".csv\"");
                            String size = request.getParameter("size");
                            returnedCSVStatus = getRequestHandler.getSensorFields(sensor, str_from, str_to, size, out);
                            if (returnedCSVStatus != RestResponse.HTTP_STATUS_OK) returnedCSVStatus = HTTP_STATUS_BAD;  // adapt to this error msg
                        }
                        isJSON = false;
                    } else if (Main.getContainerConfig().isAcEnabled() && (user == null)) { // if there is no user with these credentials
                        response.setContentType("text/csv");
                        response.setHeader("Content-Disposition", "attachment;filename=\"error_no_user.csv\"");
                        out.print("# There is no user with the provided username and password");
                        returnedCSVStatus = HTTP_STATUS_BAD;
                        isJSON = false;
                    }  // else do nothing for now
                } else {
                    response.setContentType("text/csv");
                    response.setHeader("Content-Disposition", "attachment;filename=\"error_no_such_sensor.csv\"");
                    out.print("# The virtual sensor '"+sensor+"' doesn't exist in GSN!");
                    returnedCSVStatus = HTTP_STATUS_BAD;
                    isJSON = false;
                }
                break;
            case REQUEST_GET_PREVIEW_MEASUREMENTS_FOR_SENSOR_FIELD:
                sensor = parseURI(request.getRequestURI())[3];
                field = parseURI(request.getRequestURI())[4];
                str_from = request.getParameter("from");
                str_to = request.getParameter("to");
                str_size = request.getParameter("size");
                restResponse = getRequestHandler.getPreviewMeasurementsForSensorField(sensor, field, str_from, str_to, str_size);
                break;
            case REQUEST_GET_GRIDS:
                sensor = parseURI(request.getRequestURI())[3];
                if ( Mappings.getConfig(sensor) != null ) {
                    str_date = request.getParameter("date");
                    if ( Main.getContainerConfig().isAcEnabled() && (user != null) )  { // if the AC is enabled and there is an user  // added
                        if ( !user.hasReadAccessRight(sensor) && !user.isAdmin() && DataSource.isVSManaged(sensor)) {  // if the user doesn't have access to this sensor
                            response.setContentType("text/csv");
                            response.setHeader("Content-Disposition", "attachment;filename=\"error_no_sensor_access.csv\"");
                            out.print("# The user '"+user.getUserName()+"' doesn't have access to the sensor '"+sensor+"'");
                            returnedCSVStatus = HTTP_STATUS_BAD;
                            isJSON = false;
                        } else {
                            //user authenticated
                            restResponse = getRequestHandler.getGridData(sensor, str_date);
                        }
                    } else if (Main.getContainerConfig().isAcEnabled() && (user == null)) { // if there is no user with these credentials
                        response.setContentType("text/csv");
                        response.setHeader("Content-Disposition", "attachment;filename=\"error_no_user.csv\"");
                        out.print("# There is no user with the provided username and password");
                        returnedCSVStatus = HTTP_STATUS_BAD;
                        isJSON = false;
                    } else {    // do execution without AC
                        restResponse = getRequestHandler.getGridData(sensor, str_date);
                    }
                } else {
                    response.setContentType("text/csv");
                    response.setHeader("Content-Disposition", "attachment;filename=\"error_no_such_sensor.csv\"");
                    out.print("# The virtual sensor '"+sensor+"' doesn't exist in GSN!");
                    returnedCSVStatus = HTTP_STATUS_BAD;
                    isJSON = false;
                }
                break;
            default:
                //restResponse = RestResponse.CreateErrorResponse(RestResponse.HTTP_STATUS_BAD_REQUEST, "Cannot interpret request.");
                isJSON = false;
                response.setContentType("text/csv");
                response.setHeader("Content-Disposition", "attachment;filename=\"error_unknown_request.csv\"");
                out.print("# Cannot interpret request.");
                break;
        }

        if ( (isJSON) && (restResponse != null) ) {
            response.setContentType(restResponse.getType());
            response.setStatus(restResponse.getHttpStatus());
            response.getWriter().write(restResponse.getResponse());
        }  else {
            response.setStatus(returnedCSVStatus);
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.getWriter().write("REST POST" + "\n" + request.getRequestURI());
    }

    public void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.getWriter().write("REST PUT" + "\n" + request.getRequestURI());
    }

    public void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.getWriter().write("REST DELETE" + "\n" + request.getRequestURI());
    }

    public String formatURI(String URI) {
        StringBuilder sb = new StringBuilder();
        String[] parsedURI = parseURI(URI);
        for (int i = 0; i < parsedURI.length; i++) {
            for (int j = 1; j <= i; j++)
                sb.append("..");
            sb.append("'").append(parsedURI[i]).append("'").append("\n");
        }
        return sb.toString();
    }

    public String[] parseURI(String URI) {
        return URI.split("/");
    }

    public int determineRequest(String URI) {
 //System.out.println("The input string is "+URI);
        String[] parsedURI = parseURI(URI);
 /*for(int i=0; i<parsedURI.length;i++)
     System.out.println("@@@@@@@@parsed["+i+"] ="+parsedURI[i]);
 System.out.println("@@@@@@@@@The second param is '"+parsedURI[2]+"' with Length = "+parsedURI.length);  */
        logger.warn(parsedURI.length);
        logger.warn(parsedURI[1]);

        if (parsedURI.length == 3 && parsedURI[2].equalsIgnoreCase("sensors"))
            return REQUEST_GET_ALL_SENSORS;
        if (parsedURI.length == 5 && parsedURI[2].equalsIgnoreCase("sensors"))
            return REQUEST_GET_MEASUREMENTS_FOR_SENSOR_FIELD;
        if (parsedURI.length == 4 && parsedURI[2].equalsIgnoreCase("sensors"))      // added
            return REQUEST_GET_MEASUREMENTS_FOR_SENSOR;
        if (parsedURI.length == 5 && parsedURI[2].equalsIgnoreCase("preview"))
            return REQUEST_GET_PREVIEW_MEASUREMENTS_FOR_SENSOR_FIELD;
        if (parsedURI.length == 4 && parsedURI[2].equalsIgnoreCase("grid"))
            return REQUEST_GET_GRIDS;

        return REQUEST_UNKNOWN;
    }

    public String debugRequest(HttpServletRequest request, HttpServletResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append("REST GET"
                + "\n"
                + "URI : "
                + formatURI(request.getRequestURI())
                + "\n");

        List<String> requestParameterNames = Collections.list((Enumeration<String>) request.getParameterNames());

        for (String parameterName : requestParameterNames) {
            sb.append(parameterName + " : ");
            for (int i = 0; i < request.getParameterValues(parameterName).length; i++)
                sb.append(request.getParameterValues(parameterName)[i] + "\n");
        }
        return sb.toString();
    }

}
