package gsn.http.restapi;


import gsn.Main;
import gsn.Mappings;
import gsn.beans.DataField;
import gsn.beans.VSensorConfig;
import gsn.http.ac.DataSource;
import gsn.http.ac.User;
import gsn.utils.geo.GridTools;
import org.apache.commons.collections.KeyValue;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

public class GetRequestHandler {

    private static transient Logger logger = Logger.getLogger(GetRequestHandler.class);
    private static final String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    private static final long DEFAULT_PREVIEW_SIZE = 1000;
    public static final int HTTP_STATUS_OK = 200;
    public static final int HTTP_STATUS_BAD_REQUEST = 400;


    public RestResponse getSensors() {
        RestResponse restResponse = new RestResponse();

        restResponse.setHttpStatus(RestResponse.HTTP_STATUS_OK);
        restResponse.setResponse(getSensorsInfoAsJSON());
        restResponse.setType(RestResponse.JSON_CONTENT_TYPE);

        return restResponse;
    }

    public RestResponse getSensors(User user) {             // added
        RestResponse restResponse = new RestResponse();

        restResponse.setHttpStatus(RestResponse.HTTP_STATUS_OK);
        restResponse.setResponse(getSensorsInfoAsJSON(user));
        restResponse.setType(RestResponse.JSON_CONTENT_TYPE);

        return restResponse;
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


    //////////////////////////// added
    public String getSensorsInfoAsJSON(User user) {

        JSONArray sensorsInfo = new JSONArray();

        Iterator<VSensorConfig> vsIterator = Mappings.getAllVSensorConfigs();

        while (vsIterator.hasNext()) {

            JSONObject aSensor = new JSONObject();

            VSensorConfig sensorConfig = vsIterator.next();

            String vs_name = sensorConfig.getName();
            if (!user.hasReadAccessRight(vs_name) && !user.isAdmin() && DataSource.isVSManaged(vs_name)) {   // if you dont have access to this sensor
                 continue;
            }

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

    //////////////////////////// added
    public void getSensorsInfo(User user, PrintWriter out) {
        int is_public;
        Iterator<VSensorConfig> vsIterator = Mappings.getAllVSensorConfigs();
        out.println("## is_public == 1 if the VS is publicly accessed and 0, otherwise");
        out.println("##vsname,is_public,altitude,longitude,latitude,List(attribute_name,attribute_type)");
        while (vsIterator.hasNext()) {

            VSensorConfig sensorConfig = vsIterator.next();

            String vs_name = sensorConfig.getName();
            if (!user.hasReadAccessRight(vs_name) && !user.isAdmin() && DataSource.isVSManaged(vs_name)) {   // if you dont have access to this sensor
                continue;
            }

            String alt = "0";
            String lat = "0";
            String lon = "0";

            for (KeyValue df : sensorConfig.getAddressing()) {

                String adressing_key = df.getKey().toString().toLowerCase().trim();
                String adressing_value = df.getValue().toString().toLowerCase().trim();

                if (adressing_key.indexOf("altitude") >= 0)
                    alt = adressing_value;

                if (adressing_key.indexOf("longitude") >= 0)
                    lon = adressing_value;

                if (adressing_key.indexOf("latitude") >= 0)
                    lat = adressing_value;
            }
            is_public = (DataSource.isVSManaged(vs_name)) ? 0 : 1;
            out.print(vs_name+","+is_public+","+alt+","+lon+","+lat);  // add the coords of the VS

            for (DataField df : sensorConfig.getOutputStructure()) {
                String field_name = df.getName().toLowerCase();
                String field_type = df.getType().toLowerCase();
                out.print(","+field_name+","+field_type);    // add its fields
            }
            out.println(); // prepare for the new entry
        }
    }

    public int getSensorFields(String sensor, String from, String to, String size, PrintWriter out) {

        boolean errorFlag = false;

        long fromAsLong = 0;
        long toAsLong = 0;
        try {
            fromAsLong = new java.text.SimpleDateFormat(ISO_FORMAT).parse(from).getTime();
            toAsLong = new java.text.SimpleDateFormat(ISO_FORMAT).parse(to).getTime();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            errorFlag = true;
        }

        if (errorFlag) {
            out.print("## Malformed date for from or to field.");
            return HTTP_STATUS_BAD_REQUEST;
        }

     /*   out.println("## sensor: " + sensor);
        out.println("## field, value, timestamp, epoch");
        out.println();              */
        Vector<Double> stream = new Vector<Double>();
        Vector<Long> timestamps = new Vector<Long>();
        ArrayList<Vector<Double>> elements  = new ArrayList<Vector<Double>>();;
        VSensorConfig sensorConfig = Mappings.getConfig(sensor);    // get the configuration for this vs
        ArrayList<String> fields = new ArrayList<String>();
        for (DataField df : sensorConfig.getOutputStructure()) {
            fields.add(df.getName().toLowerCase());   // get the field name that is going to be processed
        }
        out.print("#");
        int j;
        for (j=0; j < (fields.size()-1); j++) {
            out.print(fields.get(j)+",");
        }
        out.println(fields.get(j));
        ///////////////////////   Connection to the DB to get the data

        Connection conn = null;
        ResultSet resultSet = null;
        boolean restrict = false;

        if (size != null)  {
            restrict = true;
        }

        try {
            conn = Main.getStorage(sensor).getConnection();

            StringBuilder query;
            if (restrict) {
                Integer window = new Integer(size);
                query = new StringBuilder("select * from ")
                        .append(sensor)
                        .append(" where timed >= ")
                        .append(fromAsLong)
                        .append(" and timed <=")
                        .append(toAsLong)
                        .append(" order by timed desc")
                        .append(" limit 0,"+(window+1));
            } else {
                query = new StringBuilder("select * from ")
                        .append(sensor)
                        .append(" where timed >= ")
                        .append(fromAsLong)
                        .append(" and timed <=")
                        .append(toAsLong);
            }
            resultSet = Main.getStorage(sensor).executeQueryWithResultSet(query, conn);

            while (resultSet.next()) {
                if (restrict) {
                    Vector<Double> stream2 = new Vector<Double>();
                    timestamps.add(resultSet.getLong("timed"));
                    for (String fieldname : fields) {
                        stream2.add(resultSet.getDouble(fieldname));
                    }
                    elements.add(stream2);
                } else {
                    long timestamp = resultSet.getLong("timed");

                    for (String fieldname : fields) {
                        stream.add(resultSet.getDouble(fieldname));
                    }
                    for (int i = 0; i < stream.size(); i++) {
                        out.print(stream.get(i)+",");
                    }
                    out.println((new java.text.SimpleDateFormat(ISO_FORMAT).format(new java.util.Date(timestamp))).toString().replace('T', ' ')+","+timestamp);
                    stream.clear();
                }
            }
            if (restrict) {
                for (int k = elements.size()-1; k > 0; k--) {       // for each one of the results
                    Vector<Double> streamTemp = elements.get(k);
                    for (int i = 0; i < streamTemp.size(); i++)  {
                        out.print(streamTemp.get(i)+",");
                    }
                    out.println((new java.text.SimpleDateFormat(ISO_FORMAT).format(new java.util.Date(timestamps.get(k)))).toString().replace('T', ' ')+","+ timestamps.get(k));
                }

            }

        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        } finally {
            Main.getStorage(sensor).close(resultSet);
            Main.getStorage(sensor).close(conn);
        }
        return HTTP_STATUS_OK;
    }

    public RestResponse getGeoDataForSensor() {
        RestResponse restResponse = new RestResponse();

        return restResponse;
    }

    public RestResponse getMeasurementsForSensor() {
        RestResponse restResponse = new RestResponse();

        return restResponse;
    }

    public RestResponse getGridData(String sensor, String date) {
        logger.warn(sensor);
        logger.warn(date);
        RestResponse restResponse = new RestResponse();
        long timestamp = -1;
        try {
            timestamp = new java.text.SimpleDateFormat(ISO_FORMAT).parse(date).getTime();
        } catch (ParseException e) {
            logger.warn("Timestamp is badly formatted: " + date);
        }
        if (timestamp == -1) {
            restResponse = RestResponse.CreateErrorResponse(RestResponse.HTTP_STATUS_BAD_REQUEST, "Malformed date for 'date' field.");
            logger.warn(restResponse.toString());
            return restResponse;
        }

        restResponse.setHttpStatus(RestResponse.HTTP_STATUS_OK);
        restResponse.setType(RestResponse.JSON_CONTENT_TYPE);
        restResponse.setResponse(GridTools.executeQueryForGridAsJSON(sensor, timestamp));

        logger.warn(restResponse.toString());
        return restResponse;
    }


    public RestResponse getPreviewMeasurementsForSensorField(String sensor, String field, String from, String to, String size) {

        RestResponse restResponse = new RestResponse();

        Vector<Double> stream = new Vector<Double>();
        Vector<Long> timestamps = new Vector<Long>();

        boolean errorFlag = false;

        long n = -1;
        long fromAsLong = -1;
        long toAsLong = -1;

        if (size == null)
            n = DEFAULT_PREVIEW_SIZE;
        else
            try {
                n = Long.parseLong(size);
            } catch (NumberFormatException e) {
                logger.error(e.getMessage(), e);
            }

        if (n < 1) n = DEFAULT_PREVIEW_SIZE; // size should be strictly larger than 0

        if (from == null) { // no lower bound provided
            fromAsLong = getMinTimestampForSensorField(sensor, field);
        } else try {
            fromAsLong = new java.text.SimpleDateFormat(ISO_FORMAT).parse(from).getTime();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            errorFlag = true;
        }

        if (to == null) { // no lower bound provided
            toAsLong = getMaxTimestampForSensorField(sensor, field);
        } else try {
            toAsLong = new java.text.SimpleDateFormat(ISO_FORMAT).parse(to).getTime();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            errorFlag = true;
        }

        if (errorFlag) {
            restResponse = RestResponse.CreateErrorResponse(RestResponse.HTTP_STATUS_BAD_REQUEST, "Malformed date for from or to field.");
            return (restResponse);
        }

        errorFlag = !getDataPreview(sensor, field, fromAsLong, toAsLong, stream, timestamps, n);

        if (errorFlag) {
            restResponse = RestResponse.CreateErrorResponse(RestResponse.HTTP_STATUS_BAD_REQUEST, "Error in request.");
            return (restResponse);
        }

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("sensor", sensor);
        jsonResponse.put("field", field);
        jsonResponse.put("from", from);
        jsonResponse.put("to", to);
        JSONArray streamArray = new JSONArray();
        JSONArray timestampsArray = new JSONArray();
        JSONArray epochsArray = new JSONArray();
        for (int i = 0; i < stream.size(); i++) {
            streamArray.add(stream.get(i));
            timestampsArray.add(new java.text.SimpleDateFormat(ISO_FORMAT).format(new java.util.Date(timestamps.get(i))));
            epochsArray.add(timestamps.get(i));
        }
        jsonResponse.put("timestamps", timestampsArray);
        jsonResponse.put("values", streamArray);
        jsonResponse.put("epochs", epochsArray);
        restResponse.setHttpStatus(RestResponse.HTTP_STATUS_OK);
        restResponse.setType(RestResponse.JSON_CONTENT_TYPE);
        restResponse.setResponse(jsonResponse.toJSONString());

        return restResponse;
    }

    private long getMinTimestampForSensorField(String sensor, String field) {
        return getTimestampBoundForSensorField(sensor, field, "min");
    }

    private long getMaxTimestampForSensorField(String sensor, String field) {
        return getTimestampBoundForSensorField(sensor, field, "max");
    }

    private long getTimestampBoundForSensorField(String sensor, String field, String boundType) {
        Connection conn = null;
        ResultSet resultSet = null;

        boolean result = true;
        long timestamp = -1;

        try {
            conn = Main.getDefaultStorage().getConnection();
            StringBuilder query = new StringBuilder("select ").append(boundType).append("(timed) from ").append(sensor);

            resultSet = Main.getStorage(sensor).executeQueryWithResultSet(query, conn);

            if (resultSet.next()) {

                timestamp = resultSet.getLong(1);
            }

        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            result = false;
        } finally {
            Main.getStorage(sensor).close(resultSet);
            Main.getStorage(sensor).close(conn);
        }

        return timestamp;
    }

    private long getTableSize(String sensor) {
        Connection conn = null;
        ResultSet resultSet = null;

        boolean result = true;
        long timestamp = -1;

        try {
            conn = Main.getDefaultStorage().getConnection();
            StringBuilder query = new StringBuilder("select count(*) from ").append(sensor);

            resultSet = Main.getStorage(sensor).executeQueryWithResultSet(query, conn);

            if (resultSet.next()) {

                timestamp = resultSet.getLong(1);
            }

        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            result = false;
        } finally {
            Main.getStorage(sensor).close(resultSet);
            Main.getStorage(sensor).close(conn);
        }

        return timestamp;
    }


    public RestResponse getMeasurementsForSensorField(String sensor, String field, String from, String to) {

      //System.out.println("@@@@@@@MeasurementForSensorField with field ='"+field+"' from = '"+from+"' and to='"+to+"'");

        RestResponse restResponse = new RestResponse();

        Vector<Double> stream = new Vector<Double>();
        Vector<Long> timestamps = new Vector<Long>();

        boolean errorFlag = false;

        long fromAsLong = 0;
        long toAsLong = 0;
        try {
            fromAsLong = new java.text.SimpleDateFormat(ISO_FORMAT).parse(from).getTime();
            toAsLong = new java.text.SimpleDateFormat(ISO_FORMAT).parse(to).getTime();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            errorFlag = true;
        }

        if (errorFlag) {
            restResponse = RestResponse.CreateErrorResponse(RestResponse.HTTP_STATUS_BAD_REQUEST, "Malformed date for from or to field.");
            return (restResponse);
        }

        errorFlag = !getData(sensor, field, fromAsLong, toAsLong, stream, timestamps);

        if (errorFlag) {
            restResponse = RestResponse.CreateErrorResponse(RestResponse.HTTP_STATUS_BAD_REQUEST, "Error in request.");
            return (restResponse);
        }

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("sensor", sensor);
        jsonResponse.put("field", field);
        jsonResponse.put("from", from);
        jsonResponse.put("to", to);
        JSONArray streamArray = new JSONArray();
        JSONArray timestampsArray = new JSONArray();
        JSONArray epochsArray = new JSONArray();
        for (int i = 0; i < stream.size(); i++) {
            streamArray.add(stream.get(i));
            epochsArray.add(timestamps.get(i));
            timestampsArray.add(new java.text.SimpleDateFormat(ISO_FORMAT).format(new java.util.Date(timestamps.get(i))));
        }
        jsonResponse.put("timestamps", timestampsArray);
        jsonResponse.put("epochs", epochsArray);
        jsonResponse.put("values", streamArray);
        restResponse.setHttpStatus(RestResponse.HTTP_STATUS_OK);
        restResponse.setType(RestResponse.JSON_CONTENT_TYPE);
        restResponse.setResponse(jsonResponse.toJSONString());

        return restResponse;
    }

    public int getMeasurementsForSensorField(String sensor, String field, String from, String to, String size, PrintWriter out) {           // added

        Vector<Double> stream = new Vector<Double>();
        Vector<Long> timestamps = new Vector<Long>();

        boolean errorFlag = false;

        long fromAsLong = 0;
        long toAsLong = 0;
        try {
            fromAsLong = new java.text.SimpleDateFormat(ISO_FORMAT).parse(from).getTime();
            toAsLong = new java.text.SimpleDateFormat(ISO_FORMAT).parse(to).getTime();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            errorFlag = true;
        }

        if (errorFlag) {
            out.print("## Malformed date for from or to field.");
            return HTTP_STATUS_BAD_REQUEST;
        }

        if (size != null)  {
            Integer window = new Integer(size);
            ///////////
            Connection conn = null;
            ResultSet resultSet = null;

            try {
                conn = Main.getStorage(sensor).getConnection();
                StringBuilder query = new StringBuilder("select timed, ")
                        .append(field)
                        .append(" from ")
                        .append(sensor)
                        .append(" where timed >= ")
                        .append(fromAsLong)
                        .append(" and timed <=")
                        .append(toAsLong)
                        .append(" order by timed desc")
                        .append(" limit 0,"+(window+1));

                resultSet = Main.getStorage(sensor).executeQueryWithResultSet(query, conn);
                while (resultSet.next()) {
                    timestamps.add(resultSet.getLong(1));
                    stream.add(resultSet.getDouble(2));
                    /*long timestamp = resultSet.getLong(1);
                    out.println(resultSet.getDouble(2)+","+(new java.text.SimpleDateFormat(ISO_FORMAT).format(new java.util.Date(timestamp))).toString().replace('T', ' ')+","+timestamp);
*/
                }
                for (int i=stream.size()-1; i > 0; i--) {
                    long timestamp = timestamps.get(i);
                    out.println(stream.get(i)+","+(new java.text.SimpleDateFormat(ISO_FORMAT).format(new java.util.Date(timestamp))).toString().replace('T', ' ')+","+timestamp);
                }
                timestamps.clear();stream.clear();
            } catch (SQLException e) {
                logger.error(e.getMessage(), e);
            } finally {
                Main.getStorage(sensor).close(resultSet);
                Main.getStorage(sensor).close(conn);
            }
            ///////////
        } else {
            errorFlag = !getData(sensor, field, fromAsLong, toAsLong, stream, timestamps);
                 /*   out.println("##vsname: "+sensor);
        out.println("##field: "+field);
        out.println("##value,timestamp,epoch");   */
            for (int i = 0; i < stream.size(); i++) {
                out.print(stream.get(i)+","+(new java.text.SimpleDateFormat(ISO_FORMAT).format(new java.util.Date(timestamps.get(i)))).toString().replace('T', ' ')+","+timestamps.get(i));
                out.println();
            }
        }

        if (errorFlag) {
            out.print("## Error in request.");
            return HTTP_STATUS_BAD_REQUEST;
        }

        return HTTP_STATUS_OK;
    }



    public boolean getData(String sensor, String field, long from, long to, Vector<Double> stream, Vector<Long> timestamps) {
        Connection conn = null;
        ResultSet resultSet = null;

        boolean result = true;

        try {
            conn = Main.getStorage(sensor).getConnection();
            StringBuilder query = new StringBuilder("select timed, ")
                    .append(field)
                    .append(" from ")
                    .append(sensor)
                    .append(" where timed >= ")
                    .append(from)
                    .append(" and timed<=")
                    .append(to);

            resultSet = Main.getStorage(sensor).executeQueryWithResultSet(query, conn);
            while (resultSet.next()) {
                //int ncols = resultSet.getMetaData().getColumnCount();
                long timestamp = resultSet.getLong(1);
                double value = resultSet.getDouble(2);
                stream.add(value);
                timestamps.add(timestamp);
            }

        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            result = false;
        } finally {
            Main.getStorage(sensor).close(resultSet);
            Main.getStorage(sensor).close(conn);
        }

        return result;
    }


    public boolean getDataPreview(String sensor, String field, long from, long to, Vector<Double> stream, Vector<Long> timestamps, long size) {
        Connection conn = null;
        ResultSet resultSet = null;

        boolean result = true;

        long skip = getTableSize(sensor) / size;

        /*
        logger.warn("skip = " + skip);
        logger.warn("size = " + size);
        logger.warn("getTableSize(sensor) = " + getTableSize(sensor));
        */

        try {
            conn = Main.getStorage(sensor).getConnection();
            StringBuilder query = new StringBuilder("select timed, ")
                    .append(field)
                    .append(" from ")
                    .append(sensor);
            if (skip > 1)
                query.append(" where mod(pk,")
                        .append(skip)
                        .append(")=1");

            resultSet = Main.getStorage(sensor).executeQueryWithResultSet(query, conn);

            while (resultSet.next()) {
                //int ncols = resultSet.getMetaData().getColumnCount();
                long timestamp = resultSet.getLong(1);
                double value = resultSet.getDouble(2);
                //logger.warn(ncols + " cols, value: " + value + " ts: " + timestamp);
                stream.add(value);
                timestamps.add(timestamp);
            }

        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            result = false;
        } finally {
            Main.getStorage(sensor).close(resultSet);
            Main.getStorage(sensor).close(conn);
        }

        return result;
    }
}
