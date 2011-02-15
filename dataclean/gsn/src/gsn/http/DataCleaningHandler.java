package gsn.http;

import gsn.utils.models.ModelFitting;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.apache.log4j.Logger;

public class DataCleaningHandler implements RequestHandler {
    private static transient Logger logger = Logger.getLogger(DataCleaningHandler.class);

    public boolean isValid(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return true;
    }

    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        //String reqName = request.getParameter("name");

        response.getWriter().write("<a>Servlet <b>Data</b>CleaningHandler</a>");
    }

    public void call_model() {
        //ModelFitting aModel;
        //aModel = new ModelFitting();
        double[] a = null;
        long[] b = null;
        ModelFitting.FitAndMarkDirty(0, 0.5, 0, a, b, a, a);
    }
}
