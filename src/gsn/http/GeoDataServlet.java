package gsn.http;

import com.vividsolutions.jts.io.ParseException;
import gsn.Main;
import gsn.http.ac.DataSource;
import gsn.http.ac.User;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;

import static gsn.http.GetSensorDataWithGeo.*;


public class GeoDataServlet extends HttpServlet {

    private static transient Logger logger = Logger.getLogger(GeoDataServlet.class);

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            User user = null;
            if (Main.getContainerConfig().isAcEnabled()) {
                HttpSession session = request.getSession();
                user = (User) session.getAttribute("user");
                response.setHeader("Cache-Control","no-store");
                response.setDateHeader("Expires", 0);
                response.setHeader("Pragma","no-cache");
            }
            GetSensorDataWithGeo g = GetSensorDataWithGeo.getInstance();
            g.buildGeoIndex();
            String env = HttpRequestUtils.getStringParameter("env", null, request); // e.g. "POLYGON ((0 0, 0 100, 100 100, 100 0, 0 0))";
            String query = HttpRequestUtils.getStringParameter("query", null, request);

            response.getWriter().write("List of all sensors: \n" + getListOfSensors() + "\n");
            response.getWriter().write("Envelope: " + env + "\n");

            //
            ArrayList<String> sensors = g.getListOfSensors(env);
            StringBuilder matchingSensors = new StringBuilder();
            for (String vsName : sensors) {
                if ( ! Main.getContainerConfig().isAcEnabled() || (user != null && (user.hasReadAccessRight(vsName) || user.isAdmin()))) {
                    matchingSensors.append(vsName);
                    matchingSensors.append(GetSensorDataWithGeo.SEPARATOR);
                }
            }
            matchingSensors.setLength(matchingSensors.length() - 1); // remove the last SEPARATOR
            //
            //response.getWriter().write("List of all sensors within envelope: \n" + g.getListOfSensorsAsString(env) + "\n");
            response.getWriter().write("List of all sensors within envelope: \n" + matchingSensors + "\n");


            response.getWriter().write("Query:" + query + "\n");
            response.getWriter().write("Query result: \n");
            response.getWriter().write(executeQuery(env, matchingSensors.toString(), query));
        } catch (ParseException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse res) throws ServletException, IOException {
        doGet(request, res);
    }


}
