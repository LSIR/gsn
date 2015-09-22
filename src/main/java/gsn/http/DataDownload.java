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
* File: src/gsn/http/DataDownload.java
*
* @author Ali Salehi
* @author Timotee Maret
* @author Sofiane Sarni
* @author Milos Stojanovic
*
*/

package gsn.http;

import gsn.Main;
import gsn.Mappings;
import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.http.ac.DataSource;
import gsn.http.ac.User;
import gsn.http.ac.UserUtils;
import gsn.storage.DataEnumerator;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections.KeyValue;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class DataDownload extends HttpServlet {

    private static transient Logger logger = LoggerFactory.getLogger(DataDownload.class);

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, java.io.IOException {
        doPost(req, res);
    }

    /**
     * List of the parameters for the requests:
     * url : /data
     * Example: Getting all the data in CSV format => http://localhost:22001/data?vsName=memoryusage4&fields=heap&display=CSV
     * another example: http://localhost:22001/data?vsName=memoryusage4&fields=heap&fields=timed&display=CSV&delimiter=other&otherdelimiter=,
     * <p/>
     * param-name: vsName : the name of the virtual sensor we need.
     * param-name: fields [there can be multiple parameters with this name pointing to different fields in the stream element].
     * param-name: commonReq (always true !)
     * param-name: display , if there is a value it should be CSV.
     * param-name: delimiter, useful for CSV output (can be "tab","space","other")
     * param-name: otherdelimiter useful in the case of having delimiter=other
     * param-name: groupby can point to one of the fields in the stream element. In case groupby=timed then the parameter groupbytimed points to the period for which data should be aggregated [in milliseconds].
     * param-name: nb give the maximum number of elements to be outputed (most recent values first).
     * param-name:
     */
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, java.io.IOException {

        //
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");

        res.setHeader("Cache-Control","no-store");
        res.setDateHeader("Expires", 0);
        res.setHeader("Pragma","no-cache");
        //

        PrintWriter respond = res.getWriter();
        DataEnumerator result = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(Main.getInstance().getContainerConfig().getTimeFormat());
            SimpleDateFormat sdf_from_ui = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            TimeZone timeZone = GregorianCalendar.getInstance().getTimeZone();
            boolean responseCVS = false;
            boolean wantTimeStamp = false;
            boolean wantPk = false;
            boolean commonReq = true;
            boolean groupByTimed = false;

            String vsName = HttpRequestUtils.getStringParameter("vsName", null, req);
            if (vsName == null)
                vsName = HttpRequestUtils.getStringParameter("vsname", null, req);
            if (vsName == null) {
                res.sendError(WebConstants.MISSING_VSNAME_ERROR, "The virtual sensor name is missing");
                return;
            }

            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            Date currentDate = Calendar.getInstance().getTime();
            String filename = vsName+"_"+dateFormat.format(currentDate);

            if (Main.getContainerConfig().isAcEnabled() == true) {
            if (user != null) // meaning, that a login session is active, otherwise we couldn't get there
                if (user.hasReadAccessRight(vsName) == false && user.isAdmin() == false)  // ACCESS_DENIED
                {
                    res.sendError(WebConstants.ACCESS_DENIED, "Access denied to the specified virtual sensor .");
                    return;
                }
        }

            if (req.getParameter("display") != null && req.getParameter("display").equals("CSV")) {
                responseCVS = true;
                res.setContentType("text/csv");
                //res.setContentType("text/html");
            } else {
                res.setContentType("text/xml");
            }
            if (req.getParameter("commonReq") != null && req.getParameter("commonReq").equals("false")) {
                commonReq = false;
            }
            String separator = ";";
            if (req.getParameter("delimiter") != null && !req.getParameter("delimiter").equals("")) {
                String reqSeparator = req.getParameter("delimiter");
                if (reqSeparator.equals("tab")) {
                    separator = "\t";
                } else if (reqSeparator.equals("space")) {
                    separator = " ";
                } else if (reqSeparator.equals("other") && req.getParameter("otherdelimiter") != null && !req.getParameter("otherdelimiter").equals("")) {
                    separator = req.getParameter("otherdelimiter");
                }
            }
            String generated_request_query = "";
            String expression = "";
            String line = "";
            String groupby = "";
            String[] fields = req.getParameterValues("fields");
            if (commonReq) {
                if (req.getParameter("fields") != null) {
                    for (int i = 0; i < fields.length; i++) {
                        if (fields[i].equals("timed")) {
                            wantTimeStamp = true;
                        }
                        if ("pk".equalsIgnoreCase(fields[i]))
                            wantPk = true;
                        generated_request_query += ", " + fields[i];
                    }
                    if (!wantPk)
                        generated_request_query += ", pk";
                }
            } else {
                if (req.getParameter("fields") == null) {
                    respond.println("Request ERROR");
                    return;
                } else {
                    for (int i = 0; i < fields.length; i++) {
                        if (fields[i].equals("timed")) {
                            wantTimeStamp = true;
                        }
                        if ("pk".equalsIgnoreCase(fields[i]))
                            wantPk = true;
                        generated_request_query += ", " + fields[i];
                    }
                    if (!wantPk)
                        generated_request_query += ", pk";
                }
                if (req.getParameter("groupby") != null) {
                    if (req.getParameter("groupby").equals("timed")) {
                        groupByTimed = true;
                        int periodmeasure = 1;
                        if (req.getParameter("groupbytimed") != null) {
                            periodmeasure = new Integer(req.getParameter("groupbytimed"));
                            periodmeasure = java.lang.Math.max(periodmeasure, 1);
                        }
                        generated_request_query += ", Min(timed), FLOOR(timed/" + periodmeasure + ") period ";
                        groupby = "GROUP BY period";
                    } else {
                        groupby = "GROUP BY " + req.getParameter("groupby");
                    }
                }
            }

            String where = "";
            if (req.getParameter("critfield") != null) {
                try {
                    String[] critJoin = req.getParameterValues("critJoin");
                    String[] neg = req.getParameterValues("neg");
                    String[] critfields = req.getParameterValues("critfield");
                    String[] critop = req.getParameterValues("critop");
                    String[] critval = req.getParameterValues("critval");
                    for (int i = 0; i < critfields.length; i++) {
                        if (critop[i].equals("LIKE")) {
                            if (i > 0) {
                                where += " " + critJoin[i - 1] + " " + neg[i] + " " + critfields[i] + " LIKE '%"; // + critval[i] + "%'";
                            } else {
                                where += neg[i] + " " + critfields[i] + " LIKE '%"; // + critval[i] + "%'";
                            }
                            if (critfields[i].equals("timed")) {
                                try {
                                    //Date d = sdf.parse(critval[i]);
                                    Date d = sdf_from_ui.parse(critval[i]);
                                    where += d.getTime();
                                } catch (Exception e) {
                                    where += "0";
                                }
                            } else {
                                where += critval[i];
                            }
                            where += "%'";
                        } else {
                            if (i > 0) {
                                where += " " + critJoin[i - 1] + " " + neg[i] + " " + critfields[i] + " " + critop[i] + " "; //critval[i];
                            } else {
                                where += neg[i] + " " + critfields[i] + " " + critop[i] + " "; //critval[i];
                            }
                            if (critfields[i].equals("timed")) {
                                try {
                                    //Date d = sdf.parse(critval[i]);
                                    Date d = sdf_from_ui.parse(critval[i]);
                                    where += d.getTime();
                                } catch (Exception e) {
                                    where += "0";
                                }
                            } else {
                                where += critval[i];
                            }
                        }
                    }
                    where = " WHERE " + where;
                } catch (NullPointerException npe) {
                    where = " ";
                }
            }

            if (!generated_request_query.equals("")) {
                generated_request_query = generated_request_query.substring(2);
                if (!commonReq) {
                    expression = generated_request_query;
                }
                generated_request_query = "select " + generated_request_query + " from " + vsName + where + "  order by timed DESC  ";
                if (commonReq)
                    if (req.getParameter("nb") != null && req.getParameter("nb") != "") {
                        int nb = new Integer(req.getParameter("nb"));
                        if (nb < 0)
                            nb = 0;
                        String limit = "";
                        if (Main.getStorage(vsName).isH2() || Main.getStorage(vsName).isMysqlDB()) {
                            if (nb >= 0)
                                limit = "LIMIT " + nb + "  offset 0";
                            generated_request_query += limit;
                        } else if (Main.getStorage(vsName).isOracle()) {
                            generated_request_query = "select * from (" + generated_request_query + " ) where rownum <" + (nb + 1);
                        }
                    }

                generated_request_query += " " + groupby;
                generated_request_query += ";";

                if (req.getParameter("sql") != null) {
                    res.setContentType("text/html");
                    respond.println("# " + generated_request_query);
                    return;
                }


                try {
                    result = Main.getStorage(vsName).streamedExecuteQuery(generated_request_query, true);
                } catch (SQLException e) {
                    logger.error("ERROR IN EXECUTING, query: "+generated_request_query+" from " + req.getRemoteAddr() + "- " + req.getRemoteHost() +": "+e.getMessage());
                    return;
                }
                if (!result.hasMoreElements()) {
                    res.setContentType("text/html");
                    respond.println("No data corresponds to your request");
                    return;
                }

                //get units in hash map
                Iterator< VSensorConfig > vsIterator = Mappings.getAllVSensorConfigs();
                HashMap<String, String> fieldToUnitMap = new HashMap<String, String>();
                VSensorConfig sensorConfig = null;
                while ( vsIterator.hasNext( ) ) {
                    VSensorConfig senConfig = vsIterator.next( );
                    if (vsName.equalsIgnoreCase(senConfig.getName())){
                        sensorConfig = senConfig;
                        DataField[] dataFieldArray = senConfig.getOutputStructure();
                        for (DataField df: dataFieldArray){
                            String unit = df.getUnit();
                            if (unit == null || unit.trim().length() == 0)
                                unit = "";

                            fieldToUnitMap.put(df.getName().toLowerCase(), unit);
                        }
                        break;
                    }
                }

                line = "";
                int nbFields = 0;
                if (responseCVS) {
                    boolean firstLine = true;
                    res.setHeader("content-disposition","attachment; filename="+filename+".csv");
                    respond.println("# " + generated_request_query);
                    for ( KeyValue df : sensorConfig.getAddressing()){
                        respond.println("# " + df.getKey().toString().toLowerCase() + ":" + df.getValue().toString());
                    }
                    respond.println("# description:" + sensorConfig.getDescription());
                    LinkedList<StreamElement> streamElements = new LinkedList<StreamElement>();
                    while (result.hasMoreElements()) {
                        streamElements.add(result.nextElement());
                    }
                    while (!streamElements.isEmpty()) {
                        StreamElement se = streamElements.removeLast();
                        if (firstLine) {
                            nbFields = se.getFieldNames().length;
                            if (groupByTimed) {
                                nbFields--;
                            }
                            if (wantTimeStamp) {
                                line += separator + "time";
                            }
                            for (int i = 0; i < nbFields; i++)
                                //line += delimiter + se.getFieldNames()[i].toString();
                                if ((!groupByTimed) || (i != fields.length)) {
                                    line += separator + fields[i];
                                } else {
                                    line += separator + "time";
                                }

                            firstLine = false;
                            respond.println(line.substring(separator.length()));

                            line="";

                            //units (second line)
                            if (wantTimeStamp) {
                                line += separator + "";
                            }
                            for (int i = 0; i < nbFields; i++){
                                if ((!groupByTimed) || (i != fields.length)){
                                    line += separator + fieldToUnitMap.get(fields[i].toLowerCase());
                                } else {
                                    line += separator + "";
                                }
                            }
                            respond.println(line.substring(separator.length()));
                        }

                        line = "";
                        if (wantTimeStamp) {
                            Date d = new Date(se.getTimeStamp());
                            line += separator + sdf.format(d);
                        }
                        for (int i = 0; i < nbFields; i++)
                            //line += delimiter+se.getData( )[ i ].toString( );

                            if (!commonReq && ((i >= fields.length) || (fields[i].contains("timed")))) {
                                line += separator + sdf.format(se.getData()[i]);
                            } else {
                                line += separator + se.getData()[i].toString();
                            }
                        respond.println(line.substring(separator.length()));
                    }
                } else {
                    boolean firstLine = true;
                    res.setHeader("content-disposition","attachment; filename="+filename+".xml");
                    for ( KeyValue df : sensorConfig.getAddressing()){
                        respond.println("\t<!-- " + StringEscapeUtils.escapeXml(df.getKey().toString().toLowerCase()) + ":" + StringEscapeUtils.escapeXml(df.getValue().toString()) + " -->");
                    }
                    respond.println("\t<!-- description:" + StringEscapeUtils.escapeXml(sensorConfig.getDescription()) + " -->");
                    respond.println("<data>");
                    LinkedList<StreamElement> streamElements = new LinkedList<StreamElement>();
                    while (result.hasMoreElements()) {
                        streamElements.add(result.nextElement());
                    }
                    while (!streamElements.isEmpty()) {
                        StreamElement se = streamElements.removeLast();
                        if (firstLine) {
                            respond.println("\t<line>");
                            nbFields = se.getFieldNames().length;
                            if (groupByTimed) {
                                nbFields--;
                            }
                            if (wantTimeStamp) {
                                respond.println("\t\t<field unit=\"\">time</field>");
                            }
                            for (int i = 0; i < nbFields; i++) {
                                if ((!groupByTimed) || (i != fields.length)){
                                    respond.print("\t\t<field unit=\"" + fieldToUnitMap.get(fields[i].toLowerCase()));
                                    respond.println("\">" + fields[i] + "</field>");
                                } else {
                                    respond.println("\t\t<field unit=\"\">time</field>");
                                }
                            }
                            //} else {
                            //	 out.println("\t\t<field>"+expression+"</field>");
                            //}
                            respond.println("\t</line>");
                            firstLine = false;
                        }
                        line = "";
                        respond.println("\t<line>");
                        if (wantTimeStamp) {
                            Date d = new Date(se.getTimeStamp());
                            respond.println("\t\t<field>" + sdf.format(d) + "</field>");
                        }
                        for (int i = 0; i < nbFields; i++) {

                            //if ( !commonReq && expression.contains("timed")) {
                            if (!commonReq && ((i >= fields.length) || (fields[i].contains("timed")))) {
                                respond.println("\t\t<field>" + sdf.format(se.getData()[i]) + "</field>");
                            } else {
                                if (se.getData()[i] == null)
                                    respond.println("\t\t<field>Null</field>");
                                else
                                    respond.println("\t\t<field>" + se.getData()[i].toString() + "</field>");
                            }
                        }
                        respond.println("\t</line>");
                    }
                    respond.println("</data>");
                }
            }
            //*/
            else {
                res.setContentType("text/html");
                respond.println("Please select some fields");
            }
        } finally {
            if (result != null) result.close();
            respond.flush();
        }
    }
}