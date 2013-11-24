package gsn.http;

import gsn.Main;
import gsn.beans.DataTypes;
import gsn.utils.Helpers;
import gsn.utils.geo.GridTools;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.*;


public class GridDataServlet extends HttpServlet {

    private static transient Logger logger = Logger.getLogger(GridDataServlet.class);
    private static final String DEFAULT_TIMEFORMAT = "yyyyMMddHHmmss";
    private static final String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    private static final int GET_GRIDS = 0;
    private static final int GET_SUB_GRIDS = 1;
    private static final int GET_CELL_AS_TIMESERIES = 2;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        /*
        User user = null;
        if (Main.getContainerConfig().isAcEnabled()) {
            HttpSession session = request.getSession();
            user = (User) session.getAttribute("user");
            response.setHeader("Cache-Control", "no-store");
            response.setDateHeader("Expires", 0);
            response.setHeader("Pragma", "no-cache");
        }
        */

        String sensor = HttpRequestUtils.getStringParameter("sensor", null, request);

        String from = HttpRequestUtils.getStringParameter("from", null, request);
        String to = HttpRequestUtils.getStringParameter("to", null, request);

        String xminStr = HttpRequestUtils.getStringParameter("xmin", null, request);
        String yminStr = HttpRequestUtils.getStringParameter("ymin", null, request);
        String xmaxStr = HttpRequestUtils.getStringParameter("xmax", null, request);
        String ymaxStr = HttpRequestUtils.getStringParameter("ymax", null, request);

        int xmin = 0;
        int ymin = 0;
        int xmax = 0;
        int ymax = 0;

        String xcellStr = HttpRequestUtils.getStringParameter("xcell", null, request);
        String ycellStr = HttpRequestUtils.getStringParameter("ycell", null, request);

        int xcell = 0;
        int ycell = 0;

        String timeformat = HttpRequestUtils.getStringParameter("timeformat", null, request);
        String view = HttpRequestUtils.getStringParameter("view", null, request); // files or stream

        String debug = HttpRequestUtils.getStringParameter("debug", "false", request); // show debug information or not

        int request_id = GET_GRIDS;

        boolean hasBoundaries = false;
        boolean errorFlag = false;

        if (xminStr != null && xmaxStr != null && yminStr != null && ymaxStr != null) {
            request_id = GET_SUB_GRIDS;
            xmin = Integer.parseInt(xminStr);
            xmax = Integer.parseInt(xmaxStr);
            ymin = Integer.parseInt(yminStr);
            ymax = Integer.parseInt(ymaxStr);
        } else if (xcellStr != null && ycellStr != null) {
            request_id = GET_CELL_AS_TIMESERIES;
            xcell = Integer.parseInt(xcellStr);
            ycell = Integer.parseInt(ycellStr);
        } else request_id = GET_GRIDS;

        long fromAsLong = 0;
        long toAsLong = 0;
        try {
            fromAsLong = new java.text.SimpleDateFormat(ISO_FORMAT).parse(from).getTime();
            toAsLong = new java.text.SimpleDateFormat(ISO_FORMAT).parse(to).getTime();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            errorFlag = true;
        }

        String timeBounds = (from != null && to != null) ? " where timed >= " + fromAsLong + " and timed <= " + toAsLong : "";

        logger.warn("from: " + from);
        logger.warn("to:" + to);
        logger.warn("from != null && to != null =>" + from != null && to != null);
        logger.warn("timeBounds: \"" + timeBounds + "\"");

        String query = "select * from " + sensor + timeBounds;

        StringBuilder debugInformation = new StringBuilder();

        if (debug.equalsIgnoreCase("true")) {
            debugInformation.append("# sensor: " + sensor + "\n")
                    .append("# from: " + from + "\n")
                    .append("# to: " + to + "\n")
                    .append("# xcol: " + to + "\n")
                    .append("# ycol: " + to + "\n")
                    .append("# timeformat: " + to + "\n")
                    .append("# view: " + to + "\n")
                    .append("# Query: " + query + "\n");

            response.getWriter().write(debugInformation.toString());
            response.getWriter().flush();
        }

        switch (request_id) {

            case GET_GRIDS:
                List<String> grids = GridTools.executeQueryForGridAsListOfStrings(query);

                StringBuilder sbGrids = new StringBuilder();

                System.out.println(grids.size());
                for (int i = 0; i < grids.size(); i++)
                    sbGrids.append(grids.get(i));

                System.out.println(sbGrids);

                response.getWriter().write(sbGrids.toString());
                break;

            case GET_SUB_GRIDS:
                logger.warn("xmin: " + xminStr);
                logger.warn("xmax: " + xmaxStr);
                logger.warn("ymin: " + yminStr);
                logger.warn("ymax: " + ymaxStr);

                Map<Long, String> subgrids = GridTools.executeQueryForSubGridAsListOfStrings(query, xmin, xmax, ymin, ymax);
                StringBuilder sbSubGrids = new StringBuilder();

                System.out.println(subgrids.size());

                Set<Long> keySet = (Set<Long>) subgrids.keySet();

                for (Long t : keySet) {
                    String fileName = sensor + "_" + t ;
                    sbSubGrids.append("Filename : " + fileName);
                    sbSubGrids.append("\n");
                    sbSubGrids.append(subgrids.get(t));
                }

                System.out.println(sbSubGrids);

                response.getWriter().write(sbSubGrids.toString());
                break;

            case GET_CELL_AS_TIMESERIES:
                logger.warn("xcell: " + xcellStr);
                logger.warn("ycell: " + ycellStr);

                Map<Long, Double> timeSeries = GridTools.executeQueryForCell2TimeSeriesAsListOfDoubles(query, xcell, ycell);
                Set<Long> keySetTimeSeries = (Set<Long>) timeSeries.keySet();

                StringBuilder sbTimeSeries = new StringBuilder();

                String fileName = sensor;
                sbTimeSeries.append("Filename : " + fileName);
                sbTimeSeries.append("\n");
                sbTimeSeries.append("\n");

                for (Long t : keySetTimeSeries) {
                    sbTimeSeries.append(t);
                    sbTimeSeries.append(", ");
                    sbTimeSeries.append(timeSeries.get(t));
                    sbTimeSeries.append("\n");
                }

                System.out.println(sbTimeSeries);

                response.getWriter().write(sbTimeSeries.toString());
                break;

        }


        /*
        for (String vsName : sensors) {
            if (!Main.getContainerConfig().isAcEnabled() || (user != null && (user.hasReadAccessRight(vsName) || user.isAdmin()))) {
                matchingSensors.append(vsName);
                matchingSensors.append(GetSensorDataWithGeo.SEPARATOR);
            }
        }
        */

    }

    public void doPost(HttpServletRequest request, HttpServletResponse res) throws ServletException, IOException {
        doGet(request, res);
    }

    private String generateASCIIFileName(String sensor, long timestamp, String timeFormat) {
        StringBuilder sb = new StringBuilder();
        sb.append(sensor).append("_").append(Helpers.convertTimeFromLongToIso(timestamp, timeFormat));
        return sb.toString();
    }

    private String generaACIIFIleName(String sensor, long timestamp) {
        return generateASCIIFileName(sensor, timestamp, DEFAULT_TIMEFORMAT);
    }

    private void writeASCIIFile(String fileName, String folder, String content) {
        try {
            FileWriter outFile = new FileWriter(folder + "/" + fileName);
            PrintWriter out = new PrintWriter(outFile);
            out.print(content);
            out.close();
        } catch (IOException e) {
            logger.warn(e);
        }
    }

    private void writeZipFile(String folder, String[] filenames, String outFilename) {

        byte[] buf = new byte[1024];

        try {
            ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(folder + "/" + outFilename));

            for (int i = 0; i < filenames.length; i++) {
                FileInputStream fileInputStream = new FileInputStream(filenames[i]);

                zipOutputStream.putNextEntry(new ZipEntry(filenames[i]));

                int len;
                while ((len = fileInputStream.read(buf)) > 0) {
                    zipOutputStream.write(buf, 0, len);
                }

                zipOutputStream.closeEntry();
                fileInputStream.close();
            }

            zipOutputStream.close();
        } catch (IOException e) {
        }
    }
}
