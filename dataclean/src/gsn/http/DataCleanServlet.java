package gsn.http;

import gsn.Main;
import gsn.Mappings;
import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.storage.DataEnumerator;
import gsn.utils.Helpers;
import gsn.utils.models.ModelFitting;
import org.apache.commons.collections.KeyValue;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;

public class DataCleanServlet extends HttpServlet {
    private static transient Logger logger = Logger.getLogger(DataCleanServlet.class);

    /**
     * HTTP RETURN CODES :
     * ---------------------------------------------------------------------
     */

    public static final int CORRECT_REQUEST = 200;

    public static final int UNSUPPORTED_REQUEST_ERROR = 400;

    public static final int MISSING_VSNAME_ERROR = 401;

    public static final int ERROR_INVALID_VSNAME = 402;

    public static final int WRONG_VSFIELD_ERROR = 403;
    /**
     * HTTP REQUEST CODE ==================================================
     */

    public static final int REQUEST_APPLY_MODEL = 200; // applies model and creates a new table (stream, processed and dirtyness)
    public static final int REQUEST_GET_CHART = 201; // returns a chart showing the created table for a given time period
    public static final int REQUEST_DELETE_DIRTY_ELEMENTS = 202; // deletes a list of dirty elements specified by PK
    public static final int REQUEST_GET_CLEANED_DATA = 203; // returns the cleaned data (stream, processed and dirtyness)
    public static final int REQUEST_GET_DIRTY_DATA_ONLY = 204; // returns only the data considered as dirty
    public static final int REQUEST_CREATE_NEW_DATACLEAN_VS = 205; // creates a DataClean VS
    public static final int REQUEST_GET_SENSORS_INFO = 206; // creates a DataClean VS

    private static final String DEFAULT_TIME_FORMAT = "d/M/y H:m:s";

    public static final String dataclean_vsd_template = "<virtual-sensor name=\"$TABLE\" priority=\"10\">\n" +
            "\t<processing-class>\n" +
            "\t\t<class-name>gsn.vsensor.DataCleanVirtualSensor</class-name>\n" +
            "\t\t<init-params>\n" +
            "\t\t\t<param name=\"model\">$MODEL</param>\n" +
            "\t\t\t<param name=\"error_bound\">$ERROR_BOUND</param>\n" +
            "\t\t\t<param name=\"window_size\">$WINDOW_SIZE</param>\n" +
            "\t\t</init-params>\n" +
            "\t\t<output-structure>\n" +
            "\t\t\t<field name=\"stream\" type=\"double\"/>\n" +
            "\t\t\t<field name=\"processed\" type=\"double\"/>\n" +
            "\t\t</output-structure>\n" +
            "\t</processing-class>\n" +
            "\t<description>$DESCRIPTION</description>\n" +
            "\t<life-cycle pool-size=\"10\"/>\n" +
            "\t<addressing/>\n" +
            "\t<storage/>\n" +
            "\t<streams>\n" +
            "\t\t<stream name=\"input1\">\n" +
            "\t\t\t<source alias=\"source1\" sampling-rate=\"1\" storage-size=\"1\">\n" +
            "\t\t\t\t<address wrapper=\"replay\">\n" +
            "\t\t\t\t\t<predicate key=\"dbname\">$SOURCETABLE</predicate>\n" +
            "\t\t\t\t\t<predicate key=\"speed\">1000000</predicate>\n" +
            "\t\t\t\t</address>\n" +
            "\t\t\t\t<query>SELECT * FROM wrapper</query>\n" +
            "\t\t\t</source>\n" +
            "\t\t\t<query>SELECT $FIELD, timed FROM source1</query>\n" +
            "\t\t</stream>\n" +
            "\t</streams>\n" +
            "</virtual-sensor>";

    /**
     * getting the request from the web and handling it.
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {


        String rawRequest = request.getParameter(WebConstants.REQUEST);

        int requestType = -1;
        if (rawRequest == null || rawRequest.trim().length() == 0) {
            requestType = 0;
        } else
            try {
                requestType = Integer.parseInt((String) rawRequest);
            } catch (Exception e) {
                logger.debug(e.getMessage(), e);
                requestType = -1;
            }

        if (logger.isDebugEnabled()) logger.debug("Received a request with code : " + requestType);

        //OutputStream out = null;
        String result = null;

        switch (requestType) {

            case REQUEST_APPLY_MODEL:

                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("text/plain");

                result = applyModel(request);

                response.getWriter().write(result);

                break;

            case REQUEST_GET_CHART:

                //TODO:

                break;

            case REQUEST_DELETE_DIRTY_ELEMENTS:

                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("text/plain");

                result = cleanData(request);

                response.getWriter().write(result);

                break;

            case REQUEST_GET_CLEANED_DATA:

                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("text/plain");

                result = getCleanedData(request);

                response.getWriter().write(result);
                break;

            case REQUEST_GET_DIRTY_DATA_ONLY:

                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("text/plain");

                result = getDirtyData(request);

                response.getWriter().write(result);

                break;

            case REQUEST_CREATE_NEW_DATACLEAN_VS:
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("text/plain");

                result = createDataCleanVS(request);

                response.getWriter().write(result);
                break;

            case REQUEST_GET_SENSORS_INFO:
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("text/plain");

                result = getSensorsInfoAsJSON();

                response.getWriter().write(result);
                break;

            default:
                response.sendError(UNSUPPORTED_REQUEST_ERROR, "The requested operation is not supported.");
                break;
        }
    }

    private String createDataCleanVS(HttpServletRequest request) {
        String vs = request.getParameter("vs");
        String fieldname = request.getParameter("fieldname");

        String str_model = request.getParameter("model");
        int model = Integer.parseInt(str_model);

        String str_errorbound = request.getParameter("errorbound");
        String str_windowsize = request.getParameter("windowsize");
        String str_from = request.getParameter("from");
        String str_to = request.getParameter("to");
        String str_format = request.getParameter("format");

        String vsname = vs + "_" + fieldname + "_model_" + ModelFitting.MODEL_NAMES[model];
        String filename = "virtual-sensors/test.xml~";
        String description = "Data Cleaning Virtual Sensor for Station " + vs + ", "
                + "Sensor: " + fieldname + ". Model: "
                + ModelFitting.MODEL_NAMES[model]
                + ". Window size: " + str_windowsize
                + ". Error bound: " + str_errorbound;

        // $WINDOW_SIZE $ERROR_BOUND   $DESCRIPTION  $FIELD

        String vsdToWrite = dataclean_vsd_template;
        vsdToWrite = vsdToWrite.replaceAll("\\$SOURCETABLE", vs);
        vsdToWrite = vsdToWrite.replaceAll("\\$TABLE", vsname);

        vsdToWrite = vsdToWrite.replaceAll("\\$MODEL", str_model);
        vsdToWrite = vsdToWrite.replaceAll("\\$WINDOW_SIZE", str_windowsize);
        vsdToWrite = vsdToWrite.replaceAll("\\$DESCRIPTION", description);
        vsdToWrite = vsdToWrite.replaceAll("\\$ERROR_BOUND", str_errorbound);
        vsdToWrite = vsdToWrite.replaceAll("\\$FIELD", fieldname);


        try {
            FileWriter outFile = new FileWriter(filename);
            PrintWriter out = new PrintWriter(outFile);
            out.println(vsdToWrite);

            out.close();
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }


        return vsdToWrite;
    }

    private String cleanData(HttpServletRequest request) {

        String vs = request.getParameter("vs");
        String fieldname = request.getParameter("fieldname");
        String str_model = request.getParameter("model");
        String str_todelete = request.getParameter("todelete");
        String str_format = request.getParameter("format");

        if (str_format == null) str_format = "json";

        String[] todelete = str_todelete.split(",");

        int model = Integer.parseInt(str_model);

        logger.warn("VS = " + vs);
        logger.warn("fieldname = " + fieldname);
        logger.warn("model = " + model);
        logger.warn("str_todelete = " + str_todelete);

        StringBuilder sb = new StringBuilder();

        int n = todelete.length;

        if (n == 0 || str_todelete.isEmpty())
            return "\"nothing to delete\"";

        for (int i = 0; i < n; i++) {
            sb.append(i + ":" + todelete[i] + ",");
        }


        logger.warn("todelete = " + sb + " (" + str_todelete + ")");

        logger.warn("received (REQUEST_DELETE_DIRTY_ELEMENTS)");

        String tablename = vs + "_" + fieldname + "_model_" + model;

        for (int i = 0; i < n; i++) {
            String deleteSQL = "DELETE FROM " + tablename + " WHERE pk=" + todelete[i] + ";";
            logger.warn(deleteSQL);
            executeSQL(deleteSQL);
        }

        return "\"deleted " + n + " element(s)\"";
    }

    private String getCleanedData(HttpServletRequest request) {

        String vs = request.getParameter("vs");
        String fieldname = request.getParameter("fieldname");

        String str_model = request.getParameter("model");
        String str_errorbound = request.getParameter("errorbound");
        String str_windowsize = request.getParameter("windowsize");
        String str_from = request.getParameter("from");
        String str_to = request.getParameter("to");
        String str_format = request.getParameter("format");

        if (str_format == null) str_format = "json";

        int model = Integer.parseInt(str_model);
        double errorbound = Double.parseDouble(str_errorbound);
        int windowsize = Integer.parseInt(str_windowsize);

        long from = -1;
        long to = -1;

        try {
            from = Helpers.convertTimeFromIsoToLong(str_from, DEFAULT_TIME_FORMAT);
        } catch (Exception e) {
            logger.warn(e);
        }

        try {
            to = Helpers.convertTimeFromIsoToLong(str_to, DEFAULT_TIME_FORMAT);
        } catch (Exception e) {
            logger.warn(e);
        }

        logger.warn("VS = " + vs);
        logger.warn("fieldname = " + fieldname);
        logger.warn("model = " + model);
        logger.warn("errorbound = " + errorbound);
        logger.warn("windowsize = " + windowsize);

        logger.warn("received (REQUEST_GET_CLEANED_DATA)");

        //delete table if exists, create table, copy data inside
        String tablename = vs + "_" + fieldname + "_model_" + model;

        ArrayList<StreamElement> values = getValuesFor(tablename, fieldname + ",processed,dirtyness", from, to);

        Iterator<StreamElement> itr = values.iterator();

        Vector<Long> pk = new Vector<Long>();
        Vector<Long> timed = new Vector<Long>();
        Vector<Double> stream = new Vector<Double>();
        Vector<Double> processed = new Vector<Double>();
        Vector<Double> dirtyness = new Vector<Double>();

        while (itr.hasNext()) {
            StreamElement se = itr.next();
            pk.add(se.getInternalPrimayKey());
            timed.add(se.getTimeStamp());
            stream.add((Double) se.getData(fieldname));
            processed.add((Double) se.getData("processed"));
            dirtyness.add((Double) se.getData("dirtyness"));
        }

        StringBuilder sb = new StringBuilder();

        int n = stream.size();
        for (int i = 0; i < n; i++) {
            sb.append("[" + pk.elementAt(i) + ","
                    + timed.elementAt(i) + ","
                    + stream.elementAt(i) + ","
                    + processed.elementAt(i) + ","
                    + dirtyness.elementAt(i) + "]");
            if (i != n - 1)
                sb.append(",\n");
        }

        return "[" + sb.toString() + "]";
    }

    private String getDirtyData(HttpServletRequest request) {

        String vs = request.getParameter("vs");
        String fieldname = request.getParameter("fieldname");

        String str_model = request.getParameter("model");
        String str_errorbound = request.getParameter("errorbound");
        String str_windowsize = request.getParameter("windowsize");
        String str_from = request.getParameter("from");
        String str_to = request.getParameter("to");
        String str_format = request.getParameter("format");

        if (str_format == null) str_format = "json";

        int model = Integer.parseInt(str_model);
        double errorbound = Double.parseDouble(str_errorbound);
        int windowsize = Integer.parseInt(str_windowsize);

        long from = -1;
        long to = -1;

        try {
            from = Helpers.convertTimeFromIsoToLong(str_from, DEFAULT_TIME_FORMAT);
        } catch (Exception e) {
            logger.warn(e);
        }

        try {
            to = Helpers.convertTimeFromIsoToLong(str_to, DEFAULT_TIME_FORMAT);
        } catch (Exception e) {
            logger.warn(e);
        }

        logger.warn("VS = " + vs);
        logger.warn("fieldname = " + fieldname);
        logger.warn("model = " + model);
        logger.warn("errorbound = " + errorbound);
        logger.warn("windowsize = " + windowsize);

        logger.warn("received (REQUEST_GET_CLEANED_DATA)");

        //delete table if exists, create table, copy data inside
        String tablename = vs + "_" + fieldname + "_model_" + model;

        ArrayList<StreamElement> values = getValuesFor(tablename, fieldname + ",processed,dirtyness", from, to);

        Iterator<StreamElement> itr = values.iterator();

        Vector<Long> pk = new Vector<Long>();
        Vector<Long> timed = new Vector<Long>();
        Vector<Double> stream = new Vector<Double>();
        Vector<Double> processed = new Vector<Double>();
        Vector<Double> dirtyness = new Vector<Double>();

        while (itr.hasNext()) {
            StreamElement se = itr.next();
            if ((Double) se.getData("dirtyness") == 1.0) {
                pk.add(se.getInternalPrimayKey());
                timed.add(se.getTimeStamp());
                stream.add((Double) se.getData(fieldname));
                processed.add((Double) se.getData("processed"));
                dirtyness.add((Double) se.getData("dirtyness"));
            }
        }

        StringBuilder sb = new StringBuilder();

        int n = stream.size();
        for (int i = 0; i < n; i++) {
            sb.append("[" + pk.elementAt(i) + ","
                    + timed.elementAt(i) + ","
                    + stream.elementAt(i) + ","
                    + processed.elementAt(i) + ","
                    + dirtyness.elementAt(i) + "]");
            if (i != n - 1)
                sb.append(",\n");
        }

        return "[" + sb.toString() + "]";
    }

    private String applyModel(HttpServletRequest request) {

        String result = "";

        String vs = request.getParameter("vs");
        String fieldname = request.getParameter("fieldname");

        String str_model = request.getParameter("model");
        String str_errorbound = request.getParameter("errorbound");
        String str_windowsize = request.getParameter("windowsize");
        String str_from = request.getParameter("from");
        String str_to = request.getParameter("to");
        String str_format = request.getParameter("format");

        if (str_format == null) str_format = "json";

        int model = Integer.parseInt(str_model);
        double errorbound = Double.parseDouble(str_errorbound);
        int windowsize = Integer.parseInt(str_windowsize);

        long from = -1;
        long to = -1;

        try {
            from = Helpers.convertTimeFromIsoToLong(str_from, DEFAULT_TIME_FORMAT);
        } catch (Exception e) {
            logger.warn(e);
        }

        try {
            to = Helpers.convertTimeFromIsoToLong(str_to, DEFAULT_TIME_FORMAT);
        } catch (Exception e) {
            logger.warn(e);
        }

        logger.warn("VS = " + vs);
        logger.warn("fieldname = " + fieldname);
        logger.warn("model = " + model);
        logger.warn("errorbound = " + errorbound);
        logger.warn("windowsize = " + windowsize);

        logger.warn("received (REQUEST_APPLY_MODEL)");

        //delete table if exists, create table, copy data inside
        String tablename = vs + "_" + fieldname + "_model_" + model;

        String dropSQL = "drop table if exists " + tablename + ";";
        String createSQL = "create table " + tablename + " (pk BIGINT, timed BIGINT, " + fieldname + " DOUBLE, processed DOUBLE, dirtyness DOUBLE);";

        logger.warn(dropSQL);
        executeSQL(dropSQL);
        logger.warn("done");

        logger.warn(createSQL);
        executeSQL(createSQL);
        logger.warn("done");

        //init elements from DB
        ArrayList<StreamElement> values = getValuesFor(vs, fieldname, from, to);

        if (values == null) {
            return "Error in executing query";
        }

        Iterator<StreamElement> itr = values.iterator();

        Vector<Long> pk = new Vector<Long>();
        Vector<Long> timed = new Vector<Long>();
        Vector<Double> stream = new Vector<Double>();

        while (itr.hasNext()) {
            StreamElement se = itr.next();
            Double value = (Double) se.getData(fieldname);
            if (value != null) {
                pk.add(se.getInternalPrimayKey());
                timed.add(se.getTimeStamp());
                stream.add(value);
            }
        }

        int n = timed.size();
        int n2 = stream.size();

        logger.warn("Trying to run model... (timed:" + n + ", data:" + n2 + ") ");

        long[] _timed = new long[n];
        double[] _stream = new double[n];
        double[] _processed = new double[n];
        double[] _dirtyness = new double[n];

        for (int i = 0; i < n; i++) {
            _timed[i] = timed.elementAt(i);
            _stream[i] = stream.elementAt(i);
        }


        if (n > 0) {
            ModelFitting.FitAndMarkDirty(model, errorbound, windowsize, _stream, _timed, _processed, _dirtyness);
            logger.warn("done.");
        } else {
            logger.warn("Not enough data to run model");
            result = "Not enough data point in the selected time interval to run model.";
        }

        String insertSQL = "INSERT INTO " + tablename + "(pk, timed, " + fieldname + ", processed, dirtyness) VALUES (?,?,?,?,?)";

        StringBuilder sbCSV = new StringBuilder();
        StringBuilder jsonPk = new StringBuilder();
        StringBuilder jsonTimed = new StringBuilder();
        StringBuilder jsonStream = new StringBuilder();
        StringBuilder jsonProcessed = new StringBuilder();
        StringBuilder jsonDirtyness = new StringBuilder();

        StringBuilder jsonData = new StringBuilder();

        for (int i = 0; i < n; i++) {

            jsonStream.append("[")
                    .append(_timed[i])
                    .append(", ")
                    .append(_stream[i])
                    .append("]");
            jsonProcessed.append("[")
                    .append(_timed[i])
                    .append(", ")
                    .append(_processed[i])
                    .append("]");
            jsonDirtyness.append("[")
                    .append(_timed[i])
                    .append(", ")
                    .append(_dirtyness[i])
                    .append("]");

            if (i != n - 1) {
                jsonStream.append(",");
                jsonProcessed.append(",");
                jsonDirtyness.append(",");
            }

            //executePreparedSQL(insertSQL, pk.elementAt(i), _timed[i], _stream[i], _processed[i], _dirtyness[i]);
            executePreparedSQL(insertSQL, i, _timed[i], _stream[i], _processed[i], _dirtyness[i]);

            sbCSV.append(pk.elementAt(i))
                    .append(", ")
                    .append(_timed[i])
                    .append(", ")
                    .append(_stream[i])
                    .append(", ")
                    .append(_processed[i])
                    .append(", ")
                    .append(_dirtyness[i])
                    .append("\n")
            ;
        }

        /*
        String jsonReturn = "[[" + jsonPk + "],"
                + "[" + jsonTimed + "],"
                + "[" + jsonStream + "],"
                + "[" + jsonProcessed + "],"
                + "[" + jsonDirtyness + "]]";
                */

        //String jsonReturn = "{\"data\":[["+jsonTimed+"],[" + jsonProcessed + "]]}";

        String jsonReturn = "{\"data\":[[" + jsonStream + "],[" + jsonProcessed + "],[" + jsonDirtyness + "]],\"message\":\"" + result + "\"}";

        //return sbCSV.toString();
        return jsonReturn;

        //return "{\"data\":[[[1258216500000,4.91],[1258212240000,4.39],[1258216920000,4.46],[1258211640000,4.39],[1258210980000,4.82] ]]}";
    }

    private boolean executePreparedSQL(String sql, long pk, long timed, double value, double pvalue, double dirt) {

        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = Main.getDefaultStorage().getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, pk);
            pstmt.setLong(2, timed);
            pstmt.setDouble(3, value);
            pstmt.setDouble(4, pvalue);
            pstmt.setDouble(5, dirt);
            pstmt.executeUpdate();
        } catch (SQLException error) {
            logger.error(error.getMessage() + " FOR: " + sql, error);
        } finally {
            try {
                if (pstmt != null)
                    pstmt.close();
                Main.getDefaultStorage().close(conn);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return false;  //To change body of created methods use File | Settings | File Templates.
    }

    private boolean executeSQL(String sql) {

        Connection conn = null;
        Statement stmt = null;
        try {
            conn = Main.getDefaultStorage().getConnection();
            stmt = conn.createStatement();
            stmt.execute(sql);
        } catch (SQLException error) {
            logger.error(error.getMessage() + " FOR: " + sql, error);
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
                Main.getDefaultStorage().close(conn);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    /**
     * returns null if there is an error.
     *
     * @param vsname
     * @return
     */
    public static ArrayList<StreamElement> getValuesFor(String vsname, String fieldname, long from, long to) {
        StringBuilder query = new StringBuilder("select pk, timed, ")
                .append(fieldname)
                .append(" from ")
                .append(vsname)
                .append(" where timed >= ")
                .append(from)
                .append(" and timed <=")
                .append(to);

        logger.warn(query);

        ArrayList<StreamElement> toReturn = new ArrayList<StreamElement>();
        try {
            DataEnumerator result = Main.getDefaultStorage().executeQuery(query, true);
            while (result.hasMoreElements())
                toReturn.add(result.nextElement());
        } catch (SQLException e) {
            logger.error("ERROR IN EXECUTING, query: " + query);
            logger.error(e.getMessage(), e);
            return null;
        }
        return toReturn;
    }

    public void doPost(HttpServletRequest request, HttpServletResponse res) throws ServletException, IOException {
        doGet(request, res);
    }


    public boolean getData(String vs, String fieldname, long from, long to, Vector<Double> stream, Vector<Long> timestamps) {
        Connection conn = null;
        ResultSet resultSet = null;

        try {
            conn = Main.getDefaultStorage().getConnection();
            StringBuilder query = new StringBuilder("select timed, ")
                    .append(fieldname)
                    .append(" from ")
                    .append(vs)
                    .append(" where timed >= ")
                    .append(from)
                    .append(" and timed<=")
                    .append(to);

            resultSet = Main.getDefaultStorage().executeQueryWithResultSet(query, conn);

            Vector<Double> v = new Vector<Double>();
            Vector<Long> t = new Vector<Long>();

            while (resultSet.next()) {
                int ncols = resultSet.getMetaData().getColumnCount();
                long timestamp = resultSet.getLong(1);
                double value = resultSet.getDouble(2);
                //logger.warn(ncols + " cols, value: " + value + " ts: " + timestamp);
                stream.add(value);
                timestamps.add(timestamp);
            }

            logger.warn("stream => " + stream.size());
            logger.warn("timestamps => " + timestamps.size());

        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        } finally {
            Main.getDefaultStorage().close(resultSet);
            Main.getDefaultStorage().close(conn);
        }

        return true;
    }

    public String getSensorsInfoAsJSON() {

        JSONArray sensorsInfo = new JSONArray();

        Iterator<VSensorConfig> vsIterator = Mappings.getAllVSensorConfigs();

        while (vsIterator.hasNext()) {

            JSONObject aSensor = new JSONObject();

            VSensorConfig sensorConfig = vsIterator.next();

            String vs_name = sensorConfig.getName();

            aSensor.put("name", vs_name);

            JSONArray listOfFields = new JSONArray();

            for (DataField df : sensorConfig.getOutputStructure()) {

                String field_name = df.getName().toLowerCase();
                String field_type = df.getType().toLowerCase();

                if (field_type.indexOf("double") >= 0) {
                    listOfFields.add(field_name);
                }
            }

            aSensor.put("fields", listOfFields);

            Double alt = 0.0;
            Double lat = 0.0;
            Double lon = 0.0;

            for (KeyValue df : sensorConfig.getAddressing()) {

                String adressing_key = df.getKey().toString().toLowerCase().trim();
                String adressing_value = df.getValue().toString().toLowerCase().trim();

                if (adressing_key.indexOf("altitude") >= 0)
                    alt = Double.parseDouble(adressing_value);

                if (adressing_key.indexOf("longitude") >= 0)
                    lon = Double.parseDouble(adressing_value);

                if (adressing_key.indexOf("latitude") >= 0)
                    lat = Double.parseDouble(adressing_value);
            }

            aSensor.put("lat", lat);
            aSensor.put("lon", lon);
            aSensor.put("alt", alt);

            sensorsInfo.add(aSensor);

        }

        return sensorsInfo.toJSONString();
    }

}



