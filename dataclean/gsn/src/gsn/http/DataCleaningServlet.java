package gsn.http;

import gsn.utils.Helpers;
import org.apache.log4j.Logger;
import org.apache.commons.io.output.ByteArrayOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import java.io.*;
import java.util.HashMap;
import java.util.Date;
import java.util.Vector;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import gsn.Container;
import gsn.storage.StorageManager;
import gsn.beans.StreamElement;
import gsn.beans.DataTypes;
import gsn.utils.ParamParser;
import gsn.utils.models.ModelFitting;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.SeriesException;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;


public class DataCleaningServlet extends HttpServlet {
    private static transient Logger logger = Logger.getLogger(DataCleaningServlet.class);

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

    public static final int REQUEST_ONE_SHOT_QUERY_WITH_ADDRESSING = 116;

    public static final int REQUEST_ONE_SHOT_QUERY = 114;

    public static final int REQUEST_OUTPUT_FORMAT = 113;

    public static final int REQUEST_ADDRESSING = 115;

    public static final int REQUEST_GML = 901;

    public static final int REQUEST_DATA_CLEAN = 902;
    public static final int PLOT_IMAGE = 903;
    public static final int PLOT_CHART = 904;
    public static final int CREATE_VS = 905;

    private static final String DEFAULT_TIME_FORMAT = "d/M/y H:m:s";


    /**
     * getting the request from the web and handling it.
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {


        String rawRequest = request.getParameter(Container.REQUEST);
        String vs = request.getParameter("vs");
        String fieldname = request.getParameter("fieldname");

        String str_model = request.getParameter("model");
        String str_errorbound = request.getParameter("errorbound");
        String str_windowsize = request.getParameter("windowsize");
        String str_from = request.getParameter("from");
        String str_to = request.getParameter("to");

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

        Vector<Double> streams = new Vector<Double>();
        Vector<Long> ts = new Vector<Long>();

        getData(vs, fieldname, from, to, streams, ts);

        logger.warn("Streams => " + streams.size());
        logger.warn("TS => " + ts.size());

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

        RequestHandler handler;
        if (logger.isDebugEnabled()) logger.debug("Received a request with code : " + requestType);

        OutputStream out = null;

        switch (requestType) {

            case PLOT_CHART:
                response.setContentType("image/png");
                out = response.getOutputStream();

                double[] stream = new double[streams.size()];//{1.0, 2.0, 2.5, 4.0, 5.5, 6.6};
                long[] timestamps = new long[ts.size()];//{1, 2, 3, 4, 5, 10000006};

                for (int j=0;j<stream.length;j++) {
                    stream[j] = streams.get(j);
                    timestamps[j] = ts.get(j);
                }

                byte[] chart = plotChart(stream, timestamps, model, errorbound, windowsize);

                out.write(chart, 0, chart.length);

                out.close();

                break;

            case PLOT_IMAGE:

                response.setContentType("image/png");
                // to be sure it isn't cached
                response.setHeader("Expires", "Sat, 6 May 1995 12:00:00 GMT");
                response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
                response.addHeader("Cache-Control", "post-check=0, pre-check=0");
                response.setHeader("Pragma", "no-cache");
                // Get the MIME type of the image
                ServletContext sc = getServletContext();

                String filename = "chart.png";

                out = response.getOutputStream();

                FileInputStream in = new FileInputStream(filename);


                String mimeType = sc.getMimeType(filename);
                if (mimeType == null) {
                    sc.log("Could not get MIME type of " + filename);
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                }

                // Set content type
                response.setContentType(mimeType);

                // Set content size
                File file = new File(filename);
                response.setContentLength((int) file.length());

                // Copy the contents of the file to the output stream
                byte[] buf = new byte[1024];
                int count = 0;
                while ((count = in.read(buf)) >= 0) {
                    out.write(buf, 0, count);
                }
                in.close();
                out.close();

                break;

            case REQUEST_DATA_CLEAN:
                response.setContentType("text/html");
                handler = new DataCleaningHandler();
                if (handler.isValid(request, response)) handler.handle(request, response);
                break;

            default:
                response.sendError(UNSUPPORTED_REQUEST_ERROR, "The requested operation is not supported.");
                break;
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse res) throws ServletException, IOException {
        doGet(request, res);
    }


    public boolean getData(String vs, String fieldname, long from, long to, Vector<Double> stream, Vector<Long> timestamps) {
        Connection conn = null;
        ResultSet resultSet = null;

        try {
            conn = StorageManager.getInstance().getConnection();
            StringBuilder query = new StringBuilder("select timed, ")
                    .append(fieldname)
                    .append(" from ")
                    .append(vs)
                    .append(" where timed >= ")
                    .append(from)
                    .append(" and timed<=")
                    .append(to);

            resultSet = StorageManager.getInstance().executeQueryWithResultSet(query, conn);

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
            StorageManager.close(resultSet);
            StorageManager.close(conn);
        }

        return true;
    }

    public byte[] plotChart(double[] stream, long[] timestamps, int model, double errorbound, int windowsize) {
        ChartInfo chartInfo = new ChartInfo();
        chartInfo.setInputStreamName("input-stream");
        chartInfo.setPlotTitle("Stream");
        chartInfo.setType("type");
        chartInfo.setHeight(600);
        chartInfo.setWidth(800);
        chartInfo.setVerticalAxisTitle("Values");
        chartInfo.setHistorySize(timestamps.length);
        chartInfo.initialize();

        double[] processed = new double[stream.length];
        double[] dirtyness = new double[stream.length];

        ModelFitting.FitAndMarkDirty(model,errorbound,windowsize,stream,timestamps,processed, dirtyness);

        for (int i = 0; i < stream.length; i++) {
            String[] fieldNames = new String[]{"Stream"};
            Byte[] fieldTypes = new Byte[1];
            fieldTypes[0] = DataTypes.DOUBLE;
            Serializable[] values = new Serializable[1];
            values[0] = stream[i];
            StreamElement se = new StreamElement(fieldNames, fieldTypes, values, timestamps[i]);
            chartInfo.addData(se);

            fieldNames = new String[]{"Processed"};
            values[0] = processed[i];
            se = new StreamElement(fieldNames, fieldTypes, values, timestamps[i]);
            chartInfo.addData(se);

            fieldNames = new String[]{"Dirtyness"};
            values[0] = dirtyness[i]*10;
            se = new StreamElement(fieldNames, fieldTypes, values, timestamps[i]);
            chartInfo.addData(se);
        }

        return chartInfo.writePlot().toByteArray();

    }

}

/**
 * This class represents a chart. The class is initialized using a String with a
 * predefined syntax. The class acts as a proxy between the Virtual Sensor and
 * the JFreeChart library which is used for plotting diagrams.
 * Copied from ChartVirtualSensor.java
 */

class ChartInfo {

    private static final String SYNTAX = "INPUT_STREAM_VAR_NAME:CHART_NAME:VERTICAL_AXIS_TITLE [TYPE@SIZE] {WIDTH;HEIGHT}";

    private final transient Logger logger = Logger.getLogger(this.getClass());

    private String plotTitle;

    private int width;

    private int height;

    private int historySize;

    private String type;

    private String rowData;

    private String inputStreamName;

    private TimeSeriesCollection dataCollectionForTheChart;

    private HashMap<String, TimeSeries> dataForTheChart = new HashMap<String, TimeSeries>();

    private ByteArrayOutputStream byteArrayOutputStream;

    private JFreeChart chart;

    private boolean changed = true;

    private boolean ready = false;

    private String verticalAxisTitle;

    public ChartInfo() {
        byteArrayOutputStream = new ByteArrayOutputStream(64 * 1024); // Grows
        // as
        // needed
        byteArrayOutputStream.reset();
        dataCollectionForTheChart = new TimeSeriesCollection();
        rowData = "";
    }

    public void setWidth(int width) {
        if (!ready) this.width = width;
    }

    public void setHeight(int height) {
        if (!ready) this.height = height;
    }

    public void setHistorySize(int history) {
        if (!ready) historySize = history;
    }

    public void setVerticalAxisTitle(String title) {
        if (!ready) verticalAxisTitle = title;
    }

    public void setType(String type) {
        if (!ready) this.type = type;
    }

    public void setPlotTitle(String plotTitle) {
        if (!ready) this.plotTitle = plotTitle;
    }

    public void setInputStreamName(String inputStreamName) {
        if (!ready) this.inputStreamName = inputStreamName;
    }

    public void initialize() {
        if (!ready) {
            chart = ChartFactory.createTimeSeriesChart(plotTitle, "Time", verticalAxisTitle, dataCollectionForTheChart, true, true, false);
            chart.setBorderVisible(true);
            ready = true;
            if (logger.isDebugEnabled()) logger.debug("The Chart Virtual Sensor is ready.");
        }
    }

    /**
     * This method adds the specified stream elements to the timeSeries of the
     * appropriate plot.
     *
     * @param streamElement
     */
    public synchronized void addData(StreamElement streamElement) {
        for (int i = 0; i < streamElement.getFieldNames().length; i++) {
            TimeSeries timeSeries = dataForTheChart.get(streamElement.getFieldNames()[i]);
            if (timeSeries == null) {
                dataForTheChart.put(streamElement.getFieldNames()[i], timeSeries = new TimeSeries(streamElement.getFieldNames()[i], org.jfree.data.time.FixedMillisecond.class));
                timeSeries.setMaximumItemCount(historySize);
                dataCollectionForTheChart.addSeries(timeSeries);
            }
            try {
                timeSeries.addOrUpdate(new FixedMillisecond(new Date(streamElement.getTimeStamp())), Double.parseDouble(streamElement.getData()[i].toString()));
            } catch (SeriesException e) {
                logger.warn(e.getMessage(), e);
            }

        }
        changed = true;
    }

    /**
     * Plots the chart and sends it in the form of ByteArrayOutputStream to
     * outside.
     *
     * @return Returns the byteArrayOutputStream.
     */
    public synchronized ByteArrayOutputStream writePlot() {
        if (!changed) return byteArrayOutputStream;
        byteArrayOutputStream.reset();
        try {
            ChartUtilities.writeChartAsPNG(byteArrayOutputStream, chart, width, height, false, 8);

        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }
        return byteArrayOutputStream;
    }

    public boolean equals(Object obj) {
        if (obj == null && !(obj instanceof ChartInfo)) return false;
        return (obj.hashCode() == hashCode());
    }

    int cachedHashCode = -1;

    public int hashCode() {
        if (rowData != null && cachedHashCode == -1) cachedHashCode = rowData.hashCode();
        return cachedHashCode;
    }

    /**
     * @return Returns the inputStreamName.
     */
    public String getInputStreamName() {
        return inputStreamName;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        try {
            if (plotTitle != null) buffer.append("Plot-Title : ").append(plotTitle).append("\n");
            if (inputStreamName != null) {
                buffer.append("Input-Stream Name : ").append(inputStreamName).append("\n");
            }
            buffer.append("Width : ").append(width).append("\n");
            buffer.append("Height : ").append(height).append("\n");
            if (type != null) buffer.append("Type : ").append(type).append("\n");
            buffer.append("History-size : ").append(historySize).append("\n");
        } catch (Exception e) {
            buffer.insert(0, "ERROR : Till now the ChartVirtualSensor instance could understand the followings : \n");
        }
        return buffer.toString();
    }
}