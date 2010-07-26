package gsn.http;

import com.vividsolutions.jts.io.ParseException;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static gsn.http.GetSensorDataWithGeo.*;


public class GeoDataServlet extends HttpServlet {

    private static transient Logger logger = Logger.getLogger(GeoDataServlet.class);

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            GetSensorDataWithGeo g = GetSensorDataWithGeo.getInstance();
            g.buildGeoIndex();
            String env = HttpRequestUtils.getStringParameter("env", null, request); // e.g. "POLYGON ((0 0, 0 100, 100 100, 100 0, 0 0))";
            String query = HttpRequestUtils.getStringParameter("query", null, request);

            response.getWriter().write("List of all sensors: \n" + getListOfSensors() + "\n");
            response.getWriter().write("Envelope: " + env + "\n");

            response.getWriter().write("List of all sensors within envelope: \n" + g.getListOfSensors(env) + "\n");


            response.getWriter().write("Query:" + query + "\n");
            response.getWriter().write("Query result: \n");
            response.getWriter().write(executeQuery(env, query));
        } catch (ParseException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse res) throws ServletException, IOException {
        doGet(request, res);
    }


}
