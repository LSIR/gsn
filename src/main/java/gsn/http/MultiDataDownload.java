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
 * File: src/gsn/http/MultiDataDownload.java
 *
 * @author charliend
 * @author Ali Salehi
 * @author Timotee Maret
 * @author Milos Stojanovic
 *
 */

package gsn.http;

import gsn.Main;
import gsn.Mappings;
import gsn.beans.DataField;
import gsn.beans.VSensorConfig;
import gsn.http.ac.DataSource;
import gsn.http.ac.User;
import gsn.http.datarequest.DataRequestException;
import gsn.http.datarequest.DownloadReport;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.*;
import java.util.Map.Entry;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import gsn.http.datarequest.QueriesBuilder;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class MultiDataDownload extends HttpServlet {

    private static final long serialVersionUID = 4249739276150343437L;

    private static transient Logger logger = LoggerFactory.getLogger(MultiDataDownload.class);

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        doPost(req, res);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {

        //
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");

        res.setHeader("Cache-Control","no-store");
        res.setDateHeader("Expires", 0);
        res.setHeader("Pragma","no-cache");
        //

        SimpleDateFormat sdfWeb = new SimpleDateFormat ("dd/MM/yyyy HH:mm:ss") ; // 29/10/2008 22:25:07
        try {
            logger.debug("Query string: " + req.getQueryString());

            String downloadFormat = req.getParameter("download_format") == null ? "csv" : req.getParameter("download_format");
            String downloadMode = req.getParameter("download_mode") == null ? "attachement" : req.getParameter("download_mode");
            Map<String, String[]> parameterMap = parseParameters(req, downloadFormat, sdfWeb);

            String vsName = req.getParameter("vs[0]");
            String vsName1 = req.getParameter("vs[1]");
            if (vsName1 != null || "All".equals(vsName)){
                vsName = "multiple_sensors";
            }
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            Date currentDate = Calendar.getInstance().getTime();
            String filename = vsName+"_"+dateFormat.format(currentDate);

            if ("csv".equals(downloadFormat)) {
                gsn.http.datarequest.DownloadData dd = new gsn.http.datarequest.DownloadData(parameterMap);
                //
                if (Main.getContainerConfig().isAcEnabled()) {
                    ArrayList<String> noAccess = checkAccessControl(user, dd.getQueryBuilder());
                    if (noAccess != null && noAccess.size() > 0) {
                        res.sendError(WebConstants.ACCESS_DENIED, "Access Control failed for vsNames:" + noAccess + " and user: " + (user == null ? "not logged in" : user.getUserName()));
                        return;
                    }
                }
                //
                dd.process();
                if (! "inline".equals(downloadMode)) {
                    res.setContentType("application/x-download");
                    res.setHeader("content-disposition","attachment; filename="+filename+".csv");
                }
                else
                    res.setContentType("text");
                dd.outputResult(res.getOutputStream());
                //res.getOutputStream().flush();
            }
            else if ("xml".equals(downloadFormat)) {
                gsn.http.datarequest.DownloadData dd = new gsn.http.datarequest.DownloadData(parameterMap);
                //
                if (Main.getContainerConfig().isAcEnabled()) {
                    ArrayList<String> noAccess = checkAccessControl(user, dd.getQueryBuilder());
                    if (noAccess != null && noAccess.size() > 0) {
                        res.sendError(WebConstants.ACCESS_DENIED, "Access Control failed for vsNames:" + noAccess + " and user: " + (user == null ? "not logged in" : user.getUserName()));
                        return;
                    }
                }
                //
                dd.process();
                res.setContentType("text/xml");
                if (! "inline".equals(downloadMode))
                    res.setHeader("content-disposition","attachment; filename="+filename+".xml");
                dd.outputResult(res.getOutputStream());
                //res.getOutputStream().flush();
            }
            else if ("pdf".equals(downloadFormat)) {
                DownloadReport rpd = new DownloadReport (parameterMap) ;
                //
                if (Main.getContainerConfig().isAcEnabled()) {
                    ArrayList<String> noAccess = checkAccessControl(user, rpd.getQueryBuilder());
                    if (noAccess != null && noAccess.size() > 0) {
                        res.sendError(WebConstants.ACCESS_DENIED, "Access Control failed for vsNames:" + noAccess + " and user: " + (user == null ? "not logged in" : user.getUserName()));
                        return;
                    }
                }
                //
                rpd.process();
                res.setContentType("application/pdf");
                res.setHeader("content-disposition","attachment; filename="+filename+".pdf");
                rpd.outputResult(res.getOutputStream());
                res.getOutputStream().flush();
            }
            else {
                throw new DataRequestException("Unknown download_format >" + downloadFormat + "<");
            }
        } catch (DataRequestException e) {
            logger.error(e.getMessage());
            res.sendError(WebConstants.ERROR_INVALID_VSNAME, e.getMessage());
            return;
        }
    }

    public ArrayList<String> checkAccessControl (User user, QueriesBuilder qbuilder) {
        if(Main.getContainerConfig().isAcEnabled()){
            ArrayList<String> noAccess = new ArrayList<String>();
            for (String vsname : qbuilder.getSqlQueries().keySet()) {
                if (DataSource.isVSManaged(vsname)) {
                    if ( (user == null || (! user.isAdmin() && ! user.hasReadAccessRight(vsname)))) {
                        noAccess.add(vsname);
                    }
                }
            }
            return noAccess;
        }
        return null;
    }

    private Map<String, String[]> parseParameters (HttpServletRequest req, String downloadFormat, SimpleDateFormat sdfWeb) {

        Map<String, String[]> parameterMap = new Hashtable<String, String[]>();

        Hashtable<String, ArrayList<String>> vssfm = buildVirtualSensorsFieldsMapping(req.getParameterMap());

        // TIME FILTER
        if ("true".equalsIgnoreCase(req.getParameter("ifexists"))) {
            try {
                String from = req.getParameter("from");
                String to = req.getParameter("to");

                if (from != null && to != null && from != "" && to != ""){

                    Date date_from = sdfWeb.parse(from);
                    Date date_to = sdfWeb.parse(to);

                    Iterator< VSensorConfig > vsIterator = Mappings.getAllVSensorConfigs();
                    while ( vsIterator.hasNext( ) ) {
                        VSensorConfig sensorConfig = vsIterator.next( );
                        StringBuilder queryMax = new StringBuilder("select MAX(timed) from " + sensorConfig.getName());
                        StringBuilder queryMin = new StringBuilder("select MIN(timed) from " + sensorConfig.getName());
                        ResultSet resultMax = null, resultMin = null;
                        Connection conn = null;
                        try {
                            conn = Main.getStorage(sensorConfig.getName()).getConnection();
                            resultMax = Main.getStorage(sensorConfig.getName()).executeQueryWithResultSet(queryMax, conn);
                            resultMin = Main.getStorage(sensorConfig.getName()).executeQueryWithResultSet(queryMin, conn);

                            resultMax.next();
                            resultMin.next();

                            Date maxDate = new Date(resultMax.getLong(1));
                            Date minDate = new Date(resultMin.getLong(1));

                            if (!((minDate.after(date_from) && minDate.before(date_to)) || (maxDate.after(date_from) && minDate.before(date_to)))) {
                                vssfm.remove(sensorConfig.getName());
                            }
                        } catch (SQLException e) {
                            logger.error("ERROR IN EXECUTING, query: " + queryMax +": "+e.getMessage());
                        }  finally {
                            if (conn != null) conn.close();
                            if (resultMax != null) resultMax.close();
                            if (resultMin != null) resultMin.close();
                        }
                    }
                }
            } catch (Exception e) {
            	logger.error(e.getMessage(), e);
                //TODO
            }
        }

        // VS
        Iterator<Entry <String, ArrayList<String>>> vsAndFields = vssfm.entrySet().iterator();
        Iterator<String> fieldsIterator;
        Entry<String, ArrayList<String>> vsAndFieldsEntry;
        StringBuilder vsname;
        ArrayList<String> vsnames = new ArrayList<String> () ;
        while (vsAndFields.hasNext()) {
            vsAndFieldsEntry = vsAndFields.next();
            vsname = new StringBuilder();
            vsname.append(vsAndFieldsEntry.getKey());
            fieldsIterator = vsAndFieldsEntry.getValue().iterator();
            boolean hasPk = false;
            while (fieldsIterator.hasNext()) {
                vsname.append(":");
                String n = fieldsIterator.next();
                vsname.append(n);
                if ("pk".equalsIgnoreCase(n))
                    hasPk = true;
            }
            if (! hasPk)
                vsname.append(":pk");
            vsnames.add(vsname.toString());
        }
        parameterMap.put("vsname", vsnames.toArray(new String[] {}));


        // TIME FORMAT
        String req_time_format = req.getParameter("time_format");
        if (req_time_format != null) {
            parameterMap.put("timeformat", new String[] {req_time_format});
        }


        // Download format
        parameterMap.put("outputtype", new String[] { downloadFormat });

        // CRITFIELDS
        // TIME LIMITS
        ArrayList<String> critFields = new ArrayList<String> () ;
        String req_from = req.getParameter("from");
        String req_to = req.getParameter("to");
        Date timeLimit;
        try {
            if (req_from != null) {
                timeLimit = sdfWeb.parse(req_from);
                vsAndFields = vssfm.entrySet().iterator();
                while (vsAndFields.hasNext()) {
                    vsAndFieldsEntry = vsAndFields.next();
                    critFields.add("and::" + vsAndFieldsEntry.getKey() + ":timed:ge:" + timeLimit.getTime());
                }
            }
        } catch (ParseException e1) {
            logger.debug(e1.getMessage());
        }
        try {
            if (req_to != null) {
                timeLimit = sdfWeb.parse(req_to);
                vsAndFields = vssfm.entrySet().iterator();
                while (vsAndFields.hasNext()) {
                    vsAndFieldsEntry = vsAndFields.next();
                    critFields.add("and::" + vsAndFieldsEntry.getKey() + ":timed:leq:" + timeLimit.getTime());
                }
            }
        } catch (ParseException e1) {
            logger.debug(e1.getMessage());
        }

        // CONDITIONS
        Hashtable<String, String> cVss = buildWebParameterMapping("c_vs[", req.getParameterMap());
        Hashtable<String, String> cJoins = buildWebParameterMapping("c_join[", req.getParameterMap());
        Hashtable<String, String> cFields = buildWebParameterMapping("c_field[", req.getParameterMap());
        Hashtable<String, String> cMins = buildWebParameterMapping("c_min[", req.getParameterMap());
        Hashtable<String, String> cMaxs = buildWebParameterMapping("c_max[", req.getParameterMap());
        Iterator<Entry <String, String>> iter = cJoins.entrySet().iterator();
        Entry<String, String> entry;
        while (iter.hasNext()) {
            entry = iter.next();
            String cField = cFields.get(entry.getKey());
            String cVs = cVss.get(entry.getKey());
            String cJoin = cJoins.get(entry.getKey());
            String cMin = cMins.get(entry.getKey());
            String cMax = cMaxs.get(entry.getKey());

            // VS and Fields
            Hashtable<String, ArrayList<String>> vsAndFieldSelected = new Hashtable<String, ArrayList<String>> ();
            if (cVs.compareToIgnoreCase("All") == 0) {
                if (cField.compareToIgnoreCase("All") == 0) {
                    vsAndFieldSelected = vssfm;
                }
                else {
                    vsAndFieldSelected = (Hashtable<String, ArrayList<String>>) vssfm.clone();
                    ArrayList<String> toRetain = new ArrayList<String> () ;
                    toRetain.add(cField);
                    vsAndFieldSelected.values().retainAll(toRetain);
                }
            }
            else {
                if (cField.compareToIgnoreCase("All") == 0) {
                    vsAndFieldSelected.put(cVs, vssfm.get(cVs));
                }
                else {
                    ArrayList<String> tmp = new ArrayList<String> () ;
                    tmp.add(cField);
                    vsAndFieldSelected.put(cVs, tmp);
                }
            }

            Iterator<Entry <String,ArrayList<String>>> vsAndFieldsIterator = vsAndFieldSelected.entrySet().iterator();
            Iterator<String> fieldIterator;
            Entry<String, ArrayList<String>> entry2 ;
            String fieldName;
            String vsName;
            while (vsAndFieldsIterator.hasNext()) {
                entry2 = vsAndFieldsIterator.next();
                fieldIterator = entry2.getValue().iterator();
                while (fieldIterator.hasNext()) {
                    vsName = entry2.getKey();
                    fieldName = fieldIterator.next();
                    // Mins
                    if (cMin.compareToIgnoreCase("-inf") != 0) {
                        //criteria.add(cjoins[i] + "::" + vsnames[j] + ":" + cfields[i] + ":ge:" + cmins[i]);
                        critFields.add(cJoin + "::" + vsName + ":" + fieldName + ":ge:" + cMin);
                    }
                    // Maxs
                    if (cMax.compareToIgnoreCase("+inf") != 0) {
                        critFields.add(cJoin + "::" + vsName + ":" + fieldName + ":leq:" + cMax);
                    }
                }
            }
        }
        //
        parameterMap.put("critfield", critFields.toArray(new String[] {}));


        // NB
        String req_nb = req.getParameter("nb");
        if (req_nb != null) {
            if (req_nb.compareToIgnoreCase("SPECIFIED") == 0) {
                String req_nb_value = req.getParameter("nb_value");
                try {
                    Integer checked_nb = Integer.parseInt(req_nb_value);
                    parameterMap.put("nb", new String[] { "0:" + checked_nb });
                }
                catch (NumberFormatException e1) {
                    logger.debug("The specified nb of data >" + req_nb_value + "< is not a number.");
                }
            }
        }

        // AGGREGATION
        String req_agg_function = req.getParameter("agg_function");
        if (req_agg_function != null && req_agg_function.compareToIgnoreCase("-1") != 0) {
            String req_agg_period = req.getParameter("agg_period");
            String req_agg_unit = req.getParameter("agg_unit");
            try {
                long timerange = Long.parseLong(req_agg_unit) * Long.parseLong(req_agg_period);
                parameterMap.put("groupby", new String[] { timerange + ":" + req_agg_function });
            }
            catch (NumberFormatException e2) {
                logger.debug(e2.getMessage());
            }
        }

        // REPORT TEMPLATE
        String req_reportclass = req.getParameter("reportclass");
        if (req_reportclass != null) {
            parameterMap.put("reportclass", new String[] { req_reportclass });
        }

        //SAMPLE FOR PLOTS
        String req_sample = req.getParameter("sample");
        String req_sampling_perc = req.getParameter("sampling_percentage");
        if (req_sample != null) {
            parameterMap.put("sample", new String[] { req_sample });
            parameterMap.put("sampling_percentage", new String[] { req_sampling_perc });
        }

        return parameterMap;
    }


    private Hashtable<String, ArrayList<String>> buildVirtualSensorsFieldsMapping (Map<String, String[]> pm) {

        Hashtable<String, ArrayList<String>> vssfm = new Hashtable<String, ArrayList<String>> () ;
        //
        Hashtable<String, ArrayList<String>> allVsAndFieldsMapping = buildAllVirtualSensorsAndFieldsMapping () ;
        //
        Hashtable<String, String> vsnames = buildWebParameterMapping("vs[", pm); 	// key = [x], value = vsname
        Hashtable<String, String> fields = buildWebParameterMapping("field[", pm); 	// key = [x], value = fieldname
        //
        Iterator<Entry <String, String>> iter2 = vsnames.entrySet().iterator();
        String vsname;
        String field;
        Set<Entry <String, ArrayList<String>>> entries ;
        Entry<String, String> en2;
        ArrayList<String> availableFields;
        while (iter2.hasNext()) {
            en2 = iter2.next();
            vsname = (String) en2.getValue();
            field = fields.get(en2.getKey());
            if (vsname.compareToIgnoreCase("All") == 0) {
                entries = allVsAndFieldsMapping.entrySet();
                Iterator<Entry<String, ArrayList<String>>> inneriter = entries.iterator();
                Entry<String, ArrayList<String>> innerentry;
                if (field.compareToIgnoreCase("All") == 0) {
                    while (inneriter.hasNext()) {
                        innerentry = inneriter.next();
                        updateMapping(vssfm, (String)innerentry.getKey(), (ArrayList<String>)innerentry.getValue());
                    }
                }
                else {
                    while (inneriter.hasNext()) {
                        innerentry = inneriter.next();
                        availableFields = allVsAndFieldsMapping.get((String)innerentry.getKey());
                        if (availableFields != null && availableFields.contains(field)) {
                            updateMapping(vssfm, (String)innerentry.getKey(), field);
                        }
                    }
                }
            }
            else {
                if (field.compareToIgnoreCase("All") == 0) {
                    updateMapping(vssfm, vsname, allVsAndFieldsMapping.get(vsname));
                }
                else {
                    updateMapping(vssfm, vsname, field);
                }
            }
        }
        return vssfm;
    }

    private Hashtable<String, ArrayList<String>> buildAllVirtualSensorsAndFieldsMapping () {
        //
        Hashtable<String, ArrayList<String>> allVsAndFieldsMapping = new Hashtable<String, ArrayList<String>>();
        Iterator<VSensorConfig> iter = Mappings.getAllVSensorConfigs();
        VSensorConfig vsc ;
        ArrayList<String> allFields;
        while (iter.hasNext()) {
            vsc = (VSensorConfig) iter.next();
            allFields = new ArrayList<String> () ;
            DataField[] dfs = vsc.getOutputStructure();
            for (int i = 0 ; i < dfs.length ; i++) {
                allFields.add(dfs[i].getName());
            }
            allVsAndFieldsMapping.put(vsc.getName(), allFields);
        }
        return allVsAndFieldsMapping;
    }

    private void updateMapping (Hashtable<String, ArrayList<String>> vssfm, String vsname, String field) {
        ArrayList<String> tmp = new ArrayList<String> () ;
        tmp.add(field);
        updateMapping(vssfm, vsname, tmp);
    }

    private void updateMapping (Hashtable<String, ArrayList<String>> vssfm, String vsname, ArrayList<String> fields) {
        if ( ! vssfm.containsKey(vsname)) {
            vssfm.put(vsname, new ArrayList<String>());
        }
        ArrayList<String> vsnameFields = vssfm.get(vsname);
        Iterator<String> iter = fields.iterator();
        String fieldName ;
        while (iter.hasNext()) {
            fieldName = (String) iter.next();
            if (! vsnameFields.contains(fieldName)) {
                vsnameFields.add(fieldName);
            }
        }
        vssfm.put(vsname, vsnameFields);
    }

    private Hashtable<String, String> buildWebParameterMapping(String prefix, Map<String, String[]> pm) {
        Hashtable<String, String> mapping = new Hashtable<String, String> () ;
        Set<Entry <String,String[]>> sp = pm.entrySet();
        Iterator<Entry <String,String[]>> iter = sp.iterator();
        Entry<String, String[]> en;
        String key;
        while (iter.hasNext()) {
            en = iter.next();
            key = (String) en.getKey() ;
            if (key.length() > prefix.length() && key.substring(0,prefix.length()).compareToIgnoreCase(prefix) == 0) {	// look for "vs["
                String[] vals = (String[]) en.getValue();
                mapping.put(key.substring(prefix.length() - 1), vals[0]);
            }
        }
        return mapping;
    }

    private void plotMapping (Hashtable<String, ArrayList<String>> vssfm) {
        Iterator<Entry <String, ArrayList<String>>> myiter = vssfm.entrySet().iterator();
        Entry<String, ArrayList<String>> myentry;
        while (myiter.hasNext()) {
            myentry = myiter.next();
            logger.trace("VSNAME: " + myentry.getKey());
            Iterator<String> inneriter = myentry.getValue().iterator();
            while (inneriter.hasNext()) {
            	logger.trace("FIELD: " + inneriter.next());
            }
        }
    }

}