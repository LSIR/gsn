package gsn.http;

import gsn.Mappings;
import gsn.beans.VSensorConfig;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;


public class DynamicGeoDataServlet extends HttpServlet {

    private static transient Logger logger = Logger.getLogger(DynamicGeoDataServlet.class);

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
        sqlQueryStr.append("select ");
        for (int i = 0; i < allSensors.size(); i++) {
            sqlQueryStr.append(allSensors.get(i))
                    .append(" as name, ")
                    .append(field)
                    .append(" from ")
                    .append(allSensors.get(i))
                    .append(" where timed = ")
                    .append(timed);
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
                .append(sqlQueryStr);
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
}
