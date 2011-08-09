package gsn.http;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import gsn.Main;
import gsn.Mappings;
import gsn.beans.VSensorConfig;
import gsn.http.ac.UserUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;


public class DynamicGeoDataServlet extends HttpServlet {

    private static GeometryFactory geometryFactory;
    private static STRtree geoIndex;

    private static transient Logger logger = Logger.getLogger(DynamicGeoDataServlet.class);
    private static final String SEPARATOR = ",";
    private static final String NEWLINE = "\n";

    private static List<SensorGeoReading> sensorReadingsList = new Vector<SensorGeoReading>();

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String env = HttpRequestUtils.getStringParameter("env", null, request); // e.g. "POLYGON ((0 0, 0 100, 100 100, 100 0, 0 0))";
        String field = HttpRequestUtils.getStringParameter("field", null, request);
        String timed = HttpRequestUtils.getStringParameter("timed", null, request);
        String query = HttpRequestUtils.getStringParameter("query", "value", request);
        String username = HttpRequestUtils.getStringParameter("username", null, request);
        String password = HttpRequestUtils.getStringParameter("password", null, request);

        List<String> allowedSensors = new Vector<String>();

        if (Main.getContainerConfig().isAcEnabled()) {
            if (username != null && password != null) {
                if (UserUtils.allowUserToLogin(username, password) == null) {
                    response.getWriter().write("ERROR: incorrect login for user '" + username + "'. Check your credentials.");
                    return;
                } else { // user authenticated correctly
                    allowedSensors = UserUtils.getAllowedVirtualSensorsForUser(username, password, getAllSensors());
                }

            } else { // username or password is null
                response.getWriter().write("ERROR: username and password required.");
                return;
            }
        } else { // No access control
            allowedSensors = getAllSensors();
        }

        StringBuilder sb = new StringBuilder();

        StringBuilder sqlQueryStr = new StringBuilder();
        for (int i = 0; i < allowedSensors.size(); i++) {
            sqlQueryStr.append("select '" + allowedSensors.get(i) + "'")
                    .append(" as name, timed, from_unixtime(timed/1000), ")
                    .append(field)
                    .append(", latitude, longitude, altitude ")
                    .append(" from ")
                    .append(allowedSensors.get(i))
                    .append(" where timed = ( select max(timed) from ")
                    .append(allowedSensors.get(i))
                    .append(" )")
            //.append(timed)
            ;
            if (i < allowedSensors.size() - 1)
                sqlQueryStr.append("\n union \n");
        }


        sb.append("env = " + env)
                .append("\n")
                .append("field = " + field)
                .append("\n")
                .append("timed = " + timed)
                .append("\n")
                .append("query = " + query)
                .append("\n")
                .append("all_sensors = " + sensorsToString(allowedSensors))
                .append("\n")
                .append(sqlQueryStr)
                .append("\n##############\n")
                .append(executeQuery(sqlQueryStr.toString(), field));
        logger.warn(sb.toString());

        buildGeoIndex();

        String sensorsWithinEnvelope = "";

        try {
            sensorsWithinEnvelope = sensorsToString(getListOfSensorsWithinEnvelope(env));
        } catch (ParseException e) {
            logger.warn(e.getMessage(), e);
            response.getWriter().write("ERROR: cannot create geographic index");
            return;
        }

        response.getWriter().write(sb.toString());
        response.getWriter().write("\n\n");
        response.getWriter().write(sensorsWithinEnvelope);
    }

    public List<String> getAllSensors() {

        Iterator iter = Mappings.getAllVSensorConfigs();
        List<String> sensors = new Vector<String>();

        while (iter.hasNext()) {
            VSensorConfig sensorConfig = (VSensorConfig) iter.next();
            //Double longitude = sensorConfig.getLongitude();
            //Double latitude = sensorConfig.getLatitude();
            //Double altitude = sensorConfig.getAltitude();
            String sensor = sensorConfig.getName();
            sensors.add(sensor);
        }

        return sensors;
    }

    public String sensorsToString(List<String> sensors) {
        StringBuilder sensorsAsString = new StringBuilder();
        for (String sensor : sensors) {
            sensorsAsString.append(sensor);
            sensorsAsString.append(SEPARATOR);
        }
        if (sensorsAsString.length() > 0)
            sensorsAsString.setLength(sensorsAsString.length() - 1);  // remove the last SEPARATOR

        return sensorsAsString.toString();
    }


    public String executeQuery(String query, String fieldName) {

        sensorReadingsList.clear(); // reset global sensor readings
        geometryFactory = new GeometryFactory();

        StringBuilder sb = new StringBuilder();
        Connection connection = null;

        try {
            connection = Main.getDefaultStorage().getConnection();
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet results = statement.executeQuery(query);
            ResultSetMetaData metaData;    // Additional information about the results
            int numCols, numRows;          // How many rows and columns in the table
            metaData = results.getMetaData();       // Get metadata on them
            numCols = metaData.getColumnCount();    // How many columns?
            results.last();                         // Move to last row
            numRows = results.getRow();             // How many rows?

            String s;

            sb.append("# ");

            for (int col = 0; col < numCols; col++) {
                sb.append(metaData.getColumnLabel(col + 1));
                if (col < numCols - 1)
                    sb.append(SEPARATOR);
            }
            sb.append(NEWLINE);

            for (int row = 0; row < numRows; row++) {
                results.absolute(row + 1);                // Go to the specified row

                Double latitude = results.getDouble("latitude");
                Double longitude = results.getDouble("longitude");
                Double altitude = results.getDouble("altitude");

                Double value = results.getDouble(fieldName);
                String sensorName = results.getString("name");
                Long timeStamp = results.getLong("timed");

                logger.warn("longitude = " + longitude + " , latitude = " + latitude);

                Point coordinates = geometryFactory.createPoint(new Coordinate(longitude, latitude));

                SensorGeoReading sensorReadings = new SensorGeoReading(sensorName, coordinates, timeStamp, value, fieldName);
                sensorReadingsList.add(sensorReadings);

                //String
                logger.warn(sensorReadings);

                for (int col = 0; col < numCols; col++) {
                    Object o = results.getObject(col + 1); // Get value of the column
                    //logger.warn(row + " , "+col+" : "+ o.toString());
                    if (o == null)
                        s = "null";
                    else
                        s = o.toString();
                    if (col < numCols - 1)
                        sb.append(s).append(SEPARATOR);
                    else
                        sb.append(s);
                }
                sb.append(NEWLINE);
            }
        } catch (SQLException e) {
            sb.append("ERROR in execution of query: " + e.getMessage());
        } finally {
            Main.getDefaultStorage().close(connection);
        }

        return sb.toString();
    }

    /*
   * Builds geographic geoIndex from list of sensors currently loaded in the system
   * */

    public static void buildGeoIndex() {

        geoIndex = new STRtree();
        //geometryFactory = new GeometryFactory();

        for (int i = 0; i < sensorReadingsList.size(); i++) {
            geoIndex.insert(sensorReadingsList.get(i).coordinates.getEnvelopeInternal(), sensorReadingsList.get(i).coordinates);
            //logger.warn(sensors.get(i) + " : " + coordinates.get(i) + " : " + searchForSensors_String(coordinates.get(i)));
        }
        geoIndex.build();
    }

    public static List<String> getListOfSensorsWithinEnvelope(String envelope) throws ParseException {
        Geometry geom = new WKTReader().read(envelope);
        List listEnvelope = geoIndex.query(geom.getEnvelopeInternal());
        List<String> sensors = new ArrayList<String>();
        for (int i = 0; i < listEnvelope.size(); i++) {
            sensors.add(searchForSensors_String((Point) listEnvelope.get(i)));
        }
        return sensors;
    }

    /*
    * Searches for the list of sensors which are located at the given point (comma separated)
    * */
    public static String searchForSensors_String(Point p) {
        StringBuilder s = new StringBuilder("");
        for (int i = 0; i < sensorReadingsList.size(); i++) {
            if (sensorReadingsList.get(i).coordinates == p) {
                return sensorReadingsList.get(i).sensorName;
            }
        }
        return "";
    }


    public class SensorGeoReading {
        public SensorGeoReading(String name, Point coords, Long ts, double v, String field) {
            sensorName = name;
            coordinates = coords;
            timestamp = ts;
            value = v;
            fieldName = field;
        }

        String sensorName;
        Point coordinates;
        Long timestamp;
        Double value;
        String fieldName;

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[")
                    .append(sensorName)
                    .append("] (lat:")
                    .append(coordinates.getY())
                    .append(",lon:")
                    .append(coordinates.getX())
                    .append(") @ ")
                    .append(timestamp)
                    .append(" => ")
                    .append(fieldName)
                    .append(" = ")
                    .append(value);
            return sb.toString();
        }
    }
}
