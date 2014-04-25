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
import gsn.http.ac.User;
import gsn.http.ac.UserUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

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

    private static final String PARAMETER_FORMAT = "format";
    private static final String PARAMETER_USERNAME = "username";
    private static final String PARAMETER_PASSWORD = "password";
    private static final String PARAMETER_DATE = "date";
    private static final String PARAMETER_FROM = "from";
    private static final String PARAMETER_TO = "to";
    private static final String PARAMETER_SIZE = "size";

    public static final String FORMAT_JSON = "json";
    public static final String FORMAT_CSV = "csv";

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        RestResponse restResponse = null;

        String sensor = null;
        String field = null;
        String str_from = null;
        String str_to = null;
        String str_size = null;
        String str_user = null;
        String str_pass = null;
        String str_date = null;

        User user = null;

        String format = request.getParameter(PARAMETER_FORMAT);
        if (format == null || !FORMAT_CSV.equals(format)){
            format = FORMAT_JSON;
        }
       
        RequestHandler requestHandler = new RequestHandler(format);

        if (Main.getContainerConfig().isAcEnabled()) {     // added
            str_user = request.getParameter(PARAMETER_USERNAME);
            str_pass = request.getParameter(PARAMETER_PASSWORD);
            if ((str_user != null) && (str_pass != null)) {
                user = UserUtils.allowUserToLogin(str_user, str_pass);
            }
        }

        switch (determineRequest(request.getRequestURI())) {
            case REQUEST_GET_ALL_SENSORS:
                restResponse = requestHandler.getAllSensors(user);
                break;
            case REQUEST_GET_MEASUREMENTS_FOR_SENSOR:
                sensor = parseURI(request.getRequestURI())[3];
                str_from = request.getParameter(PARAMETER_FROM);
                str_to = request.getParameter(PARAMETER_TO);
                str_size = request.getParameter(PARAMETER_SIZE);

                restResponse = requestHandler.getMeasurementsForSensor(user, sensor, str_from, str_to, str_size);
                break;
            case REQUEST_GET_MEASUREMENTS_FOR_SENSOR_FIELD:
                sensor = parseURI(request.getRequestURI())[3];
                field = parseURI(request.getRequestURI())[4];
                str_from = request.getParameter(PARAMETER_FROM);
                str_to = request.getParameter(PARAMETER_TO);
                str_size = request.getParameter(PARAMETER_SIZE);

                restResponse = requestHandler.getMeasurementsForSensorField(user, sensor, field, str_from, str_to, str_size);
                break;
            case REQUEST_GET_PREVIEW_MEASUREMENTS_FOR_SENSOR_FIELD:
                sensor = parseURI(request.getRequestURI())[3];
                field = parseURI(request.getRequestURI())[4];
                str_from = request.getParameter(PARAMETER_FROM);
                str_to = request.getParameter(PARAMETER_TO);
                str_size = request.getParameter(PARAMETER_SIZE);

                restResponse = requestHandler.getPreviewMeasurementsForSensorField(user, sensor, field, str_from, str_to, str_size);
                break;
            case REQUEST_GET_GRIDS:
                sensor = parseURI(request.getRequestURI())[3];
                str_date = request.getParameter(PARAMETER_DATE);

                restResponse = requestHandler.getGridData(user, sensor, str_date);
                break;
            default:
                restResponse = requestHandler.errorResponse(RequestHandler.ErrorType.UNKNOWN_REQUEST, null, null);
                break;
        }

        response.setStatus(restResponse.getHttpStatus());
        response.setContentType(restResponse.getType());
    	for (String key: restResponse.getHeaders().keySet()){
    		response.setHeader(key, restResponse.getHeaderValue(key));
    	}
        response.getWriter().write(restResponse.getResponse());

        requestHandler.finish();
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

        String[] parsedURI = parseURI(URI);

        if (parsedURI.length == 3 && parsedURI[2].equalsIgnoreCase("sensors"))
            return REQUEST_GET_ALL_SENSORS;
        if (parsedURI.length == 5 && parsedURI[2].equalsIgnoreCase("sensors"))
            return REQUEST_GET_MEASUREMENTS_FOR_SENSOR_FIELD;
        if (parsedURI.length == 4 && parsedURI[2].equalsIgnoreCase("sensors"))
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
