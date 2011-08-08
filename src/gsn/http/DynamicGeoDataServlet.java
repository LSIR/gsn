package gsn.http;

import com.vividsolutions.jts.io.ParseException;
import gsn.Main;
import gsn.Mappings;
import gsn.beans.VSensorConfig;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;


public class DynamicGeoDataServlet extends HttpServlet {

    private static transient Logger logger = Logger.getLogger(DynamicGeoDataServlet.class);
    private static final String SEPARATOR = ",";
    private static final String NEWLINE = "\n";

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String env = HttpRequestUtils.getStringParameter("env", null, request); // e.g. "POLYGON ((0 0, 0 100, 100 100, 100 0, 0 0))";
        String field = HttpRequestUtils.getStringParameter("field", null, request);
        String timed = HttpRequestUtils.getStringParameter("timed", null, request);
        String query = HttpRequestUtils.getStringParameter("query", "value", request);
        StringBuilder sb = new StringBuilder();
        String allSensorsAsString = getAllSensorsAsString();
        List allSensors = getAllSensors();
        StringBuilder sqlQueryStr = new StringBuilder();
        for (int i = 0; i < allSensors.size(); i++) {
            sqlQueryStr.append("select '" + allSensors.get(i) + "'")
                    .append(" as name, timed, from_unixtime(timed/1000), ")
                    .append(field)
                    .append(", latitude, longitude, altitude ")
                    .append(" from ")
                    .append(allSensors.get(i))
                    .append(" where timed = ( select max(timed) from ")
                    .append(allSensors.get(i))
                    .append(" )")
            //.append(timed)
            ;
            if (i < allSensors.size() - 1)
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
                .append("all_sensors = " + allSensorsAsString)
                .append("\n")
                .append(sqlQueryStr)
                .append("\n##############\n")
                .append(executeQuery(sqlQueryStr.toString()));
        logger.warn(sb.toString());
        response.getWriter().write(sb.toString());
    }

    public List<String> getAllSensors() {

        Iterator iter = Mappings.getAllVSensorConfigs();
        List<String> sensors = new Vector<String>();

        while (iter.hasNext()) {
            VSensorConfig sensorConfig = (VSensorConfig) iter.next();
            Double longitude = sensorConfig.getLongitude();
            Double latitude = sensorConfig.getLatitude();
            Double altitude = sensorConfig.getAltitude();
            String sensor = sensorConfig.getName();
            sensors.add(sensor);
        }

        return sensors;
    }

    public String getAllSensorsAsString() {
        StringBuilder matchingSensors = new StringBuilder();

        Iterator iter = Mappings.getAllVSensorConfigs();
        List<String> sensors = new Vector<String>();

        while (iter.hasNext()) {
            VSensorConfig sensorConfig = (VSensorConfig) iter.next();
            Double longitude = sensorConfig.getLongitude();
            Double latitude = sensorConfig.getLatitude();
            Double altitude = sensorConfig.getAltitude();
            String sensor = sensorConfig.getName();
            sensors.add(sensor);
        }

        for (String vsName : sensors) {
            matchingSensors.append(vsName);
            matchingSensors.append(GetSensorDataWithGeo.SEPARATOR);
        }
        if (matchingSensors.length() > 0)
            matchingSensors.setLength(matchingSensors.length() - 1); // remove the last SEPARATOR
        return matchingSensors.toString();
    }

    public static String executeQuery(String query) {


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
}
