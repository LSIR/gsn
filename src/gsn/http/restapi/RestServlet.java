package gsn.http.restapi;

import org.apache.log4j.Logger;
import org.eclipse.jetty.rewrite.handler.RegexRule;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

public class RestServlet extends HttpServlet {

    private static transient Logger logger = Logger.getLogger(RestServlet.class);

    private static final int REQUEST_UNKNOWN = -1;
    private static final int REQUEST_GET_ALL_SENSORS = 0;
    private static final int REQUEST_GET_MEASUREMENTS_FOR_SENSOR = 1;
    private static final int REQUEST_GET_MEASUREMENTS_FOR_SENSOR_FIELD = 2;
    private static final int REQUEST_GET_GEO_DATA_FOR_SENSOR = 3;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        GetRequestHandler getRequestHandler = new GetRequestHandler();

        response.getWriter().write("REST GET"
                + "\n"
                + "URI : "
                + formatURI(request.getRequestURI())
                + "\n"
        );

        List<String> requestParameterNames = Collections.list((Enumeration<String>) request.getParameterNames());

        for (String parameterName : requestParameterNames) {
            response.getWriter().write(parameterName + " : ");
            for (int i = 0; i < request.getParameterValues(parameterName).length; i++)
                response.getWriter().write(request.getParameterValues(parameterName)[i] + "\n");
        }

        response.getWriter().write(getRequestHandler.getSensors().getResponse());

        String str_sensor = request.getParameter("sensor");
        String str_field = request.getParameter("field");
        String str_from = request.getParameter("from");
        String str_to = request.getParameter("to");

        response.getWriter().write(getRequestHandler.getMeasurementsForSensorField(str_sensor,str_field,str_from,str_to).getResponse());

    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.getWriter().write("REST POST" + "\n" + request.getRequestURI());
    }

    public void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.getWriter().write("REST PUT" + "\n" + request.getRequestURI());
    }

    public void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.getWriter().write("REST DELETE" + "\n" + request.getRequestURI());
    }

    public String formatURI(String URI) {
        StringBuilder sb = new StringBuilder();
        String[] parsedURI = parseURI(URI);
        for (int i = 0; i < parsedURI.length; i++) {
            for (int j = 1; j <= i; j++)
                sb.append("..");
            sb.append(parsedURI[i]).append("\n");
        }
        return sb.toString();
    }

    public String[] parseURI(String URI) {
        return URI.split("/");
    }

    public int determineRequest(String URI) {
        return 0;
    }

}
