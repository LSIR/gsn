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
* File: src/gsn/http/HBaseQueryHandler.java
*
* @author Ivo Dimitrov
*
*/

package gsn.http;

import gsn.Main;
import gsn.wrappers.storext.HBaseConnector;
import gsn.wrappers.storext.Pair;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: ivo
 * Date: 6/6/13
 * Time: 9:45 AM
 * To change this template use File | Settings | File Templates.
 */
public class HBaseQueryHandler implements RequestHandler {

    private static transient Logger logger = Logger.getLogger(HBaseQueryHandler.class);

    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String vsName = request.getParameter("name");
        String tableName = "measurements_"+vsName;
        String end = null;
        boolean noWindow = false; // assume there is a window
        boolean oneRecord = false;
        boolean isRange = false;

        String windowSize = request.getParameter("window");
        if (windowSize == null || windowSize.trim().length() == 0) noWindow = true;
        String recordKey = request.getParameter("record");
        if (recordKey != null) oneRecord = true;
        String start = request.getParameter("start");
        if (start != null) {
            isRange = true;
            end = request.getParameter("end");
        }
	boolean isCond = false;
        String cond = request.getParameter("condition");
        if (cond != null) {
            isCond = true;
        }

        // get the results
        HashMap<String, ArrayList<Pair>> results;
        HBaseConnector hbase = new HBaseConnector();

//System.out.println("^^^^^^^^^^^^^^^^^^^^^^The table name is: "+tableName);
        // formulate the rest of the response
        SimpleDateFormat sdf = new SimpleDateFormat(Main.getInstance().getContainerConfig().getTimeFormat());
        StringBuilder sb = new StringBuilder("<result>\n");

        if (oneRecord == false) {
            if (noWindow) {
                results = hbase.getAllRecords(tableName);
            } else if (isRange) {
                Long startL = Long.MAX_VALUE - (new Long(end));
                Long endL = Long.MAX_VALUE - (new Long(start));
                results = hbase.getIntervalRecords(tableName, startL.toString(), endL.toString());
            } else if (isCond && !noWindow) {
                results = hbase.getCondRecords(tableName, cond, Integer.parseInt(windowSize));
            } else {
                results = hbase.getNRecords(tableName, Integer.parseInt(windowSize));
            }
            Set<Map.Entry<String, ArrayList<Pair>>> entrySet = results.entrySet();
            for (Map.Entry entry : entrySet) {
                sb.append("<stream-element>\n");
                ArrayList<Pair> columns = (ArrayList<Pair>) entry.getValue();  // the columns associated with a particular row
                for(Pair p : columns) {
                    sb.append("<field name=\"").append(p.getFieldName()).append("\" >").append(p.getFieldValue()).append("</field>\n");
                }
                Long time = Long.MAX_VALUE - (new Long((String)entry.getKey()));
             System.out.println("Time is = "+ time);
                sb.append("<field name=\"timed\" >").append(sdf.format(new Date(time))).append("</field>\n");
                sb.append("</stream-element>\n");
            }
        } else {       // only one record is required
            Long time = Long.MAX_VALUE - (new Long(recordKey));
            ArrayList<Pair> columns = hbase.getOneRecord(tableName, time.toString());
            sb.append("<stream-element>\n");
            for(Pair p : columns) {
                sb.append("<field name=\"").append(p.getFieldName()).append("\" >").append(p.getFieldValue()).append("</field>\n");
            }
            sb.append("<field name=\"timed\" >").append(sdf.format(new Date(time))).append("</field>\n");
            sb.append("</stream-element>\n");
        }


        sb.append("</result>");

        response.setHeader("Cache-Control", "no-store");
        response.setDateHeader("Expires", 0);
        response.setHeader("Pragma", "no-cache");
        response.getWriter().write(sb.toString());
    }

    public boolean isValid(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String vsName = request.getParameter("name");
        if (vsName == null || vsName.trim().length() == 0) {
            response.sendError(WebConstants.MISSING_VSNAME_ERROR, "The virtual sensor name is missing");
            return false;
        }
       /* VSensorConfig sensorConfig = Mappings.getVSensorConfig(vsName);
        if (sensorConfig == null) {
            response.sendError(WebConstants.ERROR_INVALID_VSNAME, "The specified virtual sensor doesn't exist.");
            return false;
        }*/
        return true;
    }

}
