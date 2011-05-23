package gsn.http;

import gsn.simulation.GEOtopSimulation;
import gsn.utils.Helpers;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;


public class SimulationServlet extends HttpServlet {
    private static transient Logger logger = Logger.getLogger(SimulationServlet.class);

    public static final String REQUEST = "REQUEST";
    public static final String LAYER = "layer";
    public static final int REQUEST_INIT_SIMULATION = 200;
    public static final int REQUEST_STOP_SIMULATION = 201;
    public static final int REQUEST_START_SIMULATION = 202;
    public static final int REQUEST_SET_PARAMETERS = 203;
    public static final int REQUEST_GET_LATEST_STATUS = 204;
    public static final int REQUEST_GET_LATEST_RESULT = 205;
    public static final int REQUEST_GET_IMAGE = 206;
    public static final int REQUEST_GET_STATUS = 207;
    public static final int REQUEST_FETCH_RESULTS = 208;

    public static final int UNSUPPORTED_REQUEST_ERROR = 400;
    private static final String PARAM_STATIONS = "stations";
    private static final String PARAM_FROM = "from";
    private static final String PARAM_TO = "to";
    private static final String PARAM_INTERPOLATIONS = "interpolations";
    private static final String PARAM_FILTERS = "filters";
    private static final String PARAM_WINDOWSIZE = "window";
    private static final String SEPARATOR = ",";
    private static final String DEFAULT_TIME_FORMAT = "d/M/y H:m:s";

    private static boolean simulation_running = false;
    private static boolean simulation_initialized = false;

    private static final int SIMULATION_UNKNOWN_STATUS = -1;
    public static final int SIMULATION_STARTED = 0;
    public static final int SIMULATION_ALREADY_RUNNING = 1;
    public static final int SIMULATION_CANNOT_START = 2;
    public static final int SIMULATION_NOT_INITIALIZED = 3;

    GEOtopSimulation aSimulation;

    private String io_ini_section4_filters = "";       // in io.ini (on geottop server)
    private String io_ini_section5_interpolations = "";
    private String io_ini_section2_stations = "";

    private static final String io_ini_section1_general = "[General]\n" +
            "\n" +
            "PLUGINPATH\t= /usr/local/lib/meteoio/plugins\n" +
            "\n" +
            "[Input]\n" +
            "COORDSYS\t= CH1903\n" +
            "\n" +
            "DEM\t= ARC\n" +
            "DEMFILE  \t= morpho/_dem.asc\n" +
            "\n" +
            "GRID2D\t= GRASS" +
            "\n" +
            "METEO\t= GSN\n";

    private static final String io_ini_section3_meteo_path = "\n#METEO      \t= GEOTOP\n" +
            "METEOPATH     \t= meteo/\n" +
            "METEOPREFIX\t= _meteo\n" +
            "\n" +
            "[OUTPUT]\n" +
            "METEO\t\t= WSMDF\n" +
            "METEOPATH\t= wsmdf\n" +
            "#METEO\t\t= GEOTOP\n" +
            "#METEOPATH\t= mymeteo\n" +
            "#METEOSEQ\t= WindS WindDir RelHum AirT SWglobal Iprec" +
            "\n\n";

    /**
     * getting the request from the web and handling it.
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String rawRequest = request.getParameter(REQUEST);

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

        if (logger.isDebugEnabled()) logger.debug("Received a request with code : " + requestType);

        String result = null;

        switch (requestType) {

            case REQUEST_INIT_SIMULATION:

                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json");

                result = "{ output:\"ok\", message: \"" + initSimulation(request) + "\"}";

                response.getWriter().write(result);

                break;

            case REQUEST_GET_STATUS:

                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json");

                String message="Message";
                if (!simulation_initialized) {
                    message = "Simulation not yet initialized.";
                }  else {
                    message = "Simulation initialized";
                }

                if (isSimulationRunning()) {
                    message = "Simulation running.";
                }

                if (isSimulationFinished()) {
                    message = "Simulation finished with code: " + getExitCode();
                }

                result = "{ output:\"Ok\", running:"+isSimulationRunning()
                        + ", finished: "+ isSimulationFinished()
                        + ", exitcode: " + getExitCode()
                        +", message: \""+  message + "\"}";

                response.getWriter().write(result);

                break;

            case REQUEST_START_SIMULATION:

                int rs = startSimulation();

                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json");

                switch (rs) {

                    case SIMULATION_NOT_INITIALIZED:
                        result = "{ output:\"error\", message: \"" + "cannot start simulation without initialization" + "\"}";
                        break;

                    case SIMULATION_STARTED:

                        result = "{ output:\"ok\", message: \"" + "simulation started" + "\"}";
                        break;
                }

                response.getWriter().write(result);

                break;

            case REQUEST_FETCH_RESULTS:
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json");

                if (aSimulation.fetchResults())
                    result = "{ output:\"ok\", message: \"" + "results ready" + "\"}";
                else
                    result = "{ output:\"ok\", message: \"" + "results not yet ready" + "\"}";

                response.getWriter().write(result);
                break;

            case REQUEST_GET_IMAGE:
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("image/png");

                OutputStream out = response.getOutputStream();

                String layer = request.getParameter(LAYER);

                String fileName = "results/11_TxyL000" + layer + "N0001.png";

                logger.warn("Sending "+ fileName);

                byte[] imageBytes = load(fileName);
                out.write(imageBytes, 0, imageBytes.length);

                out.close();

                break;

            default:
                response.sendError(UNSUPPORTED_REQUEST_ERROR, "The requested operation is not supported.");
                break;
        }

    }

    private int getExitCode() {
        if (simulation_initialized)
            return aSimulation.getGeoTopExitCode();
        else
            return -1;
    }

    public final static byte[] load(String fileName) {
        try {
            FileInputStream fin = new FileInputStream(fileName);
            return load(fin);
        }
        catch (Exception e) {

            return new byte[0];
        }
    }

    public final static byte[] load(FileInputStream fin) {
        byte readBuf[] = new byte[512 * 1024];

        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();

            int readCnt = fin.read(readBuf);
            while (0 < readCnt) {
                bout.write(readBuf, 0, readCnt);
                readCnt = fin.read(readBuf);
            }

            fin.close();

            return bout.toByteArray();
        }
        catch (Exception e) {

            return new byte[0];
        }
    }

    private String initSimulation(HttpServletRequest request) {

        String stations = request.getParameter(PARAM_STATIONS);
        String fromStr = request.getParameter(PARAM_FROM);
        String toStr = request.getParameter(PARAM_TO);
        String interpolations = request.getParameter(PARAM_INTERPOLATIONS);
        String filters = request.getParameter(PARAM_FILTERS);
        String strWindowsize = request.getParameter(PARAM_WINDOWSIZE);

        String listOfStations[] = stations.split(SEPARATOR);
        String listOfFilters[] = filters.split(SEPARATOR);
        String listOfInterpolations[] = interpolations.split(SEPARATOR);

        long from = -1;
        long to = -1;

        long windowSize = Long.parseLong(strWindowsize.trim());

        logger.warn("Initializing simulation with parameters:");
        logger.warn(fromStr);
        logger.warn(toStr);
        logger.warn(interpolations);
        logger.warn(filters);
        logger.warn(strWindowsize);

        StringBuilder sb_stations = new StringBuilder();
        StringBuilder sb_filters = new StringBuilder("[Filters]\n");
        StringBuilder sb_interpolations = new StringBuilder("\n[Interpolations2D]\n");

        sb_stations.append("NROFSTATIONS = ")
                .append(listOfStations.length)
                .append("\n");

        for (int i = 0; i < listOfStations.length; i++) {
            sb_stations.append("STATION")
                    .append(i + 1)
                    .append(" = ")
                    .append(listOfStations[i])
                    .append("\n");
            logger.warn("station " + i + " : " + listOfStations[i]);
        }

        for (int i = 0; i < listOfFilters.length; i++) {
            logger.warn("filter " + i + " : " + listOfFilters[i]);
            String[] s = listOfFilters[i].split(";");
            sb_filters.append(s[0].trim())
                    .append("\n")
                    .append(s[1].trim())
                    .append("\n");
        }

        for (int i = 0; i < listOfInterpolations.length; i++) {
            logger.warn("interpolation " + i + " : " + listOfInterpolations[i]);
            sb_interpolations.append(listOfInterpolations[i])
                    .append("\n");
        }

        io_ini_section2_stations = sb_stations.toString();
        io_ini_section4_filters = sb_filters.toString();
        io_ini_section5_interpolations = sb_interpolations.toString();

        try {
            from = Helpers.convertTimeFromIsoToLong(fromStr, DEFAULT_TIME_FORMAT);
        } catch (Exception e) {
            logger.warn(e);
        }

        try {
            to = Helpers.convertTimeFromIsoToLong(toStr, DEFAULT_TIME_FORMAT);
        } catch (Exception e) {
            logger.warn(e);
        }

        String from_formatted = Helpers.convertTimeFromLongToIso(from, "yyyyMMdd'T'HHmm");
        String to_formatted = Helpers.convertTimeFromLongToIso(to, "yyyyMMdd'T'HHmm");

        aSimulation = new GEOtopSimulation("demo");
        aSimulation.setGeneralParameters(from_formatted, to_formatted, stations, "geotop", windowSize);
        aSimulation.init();

        logger.warn(aSimulation);
        simulation_initialized = true;

        String io_ini_file = generateIoIniFile();
        logger.warn(io_ini_file);

        aSimulation.uploadIoIniFileOnGeoTopServer(io_ini_file);

        String jsonReturn = "Simulation initialized with parameters: " + from_formatted + " , " + to_formatted + " " + aSimulation.toString();

        return jsonReturn;
    }

    public boolean isSimulationRunning() {
        if (!simulation_initialized)
            return false;
        else
            return aSimulation.isSimulationRunning();
    }

    public boolean isSimulationFinished() {
        if (!simulation_initialized)
            return false;
        else
            return aSimulation.isSimulationFinished();
    }

    public int startSimulation() {
        int result = SIMULATION_UNKNOWN_STATUS;

        if (!simulation_initialized) {
            logger.warn("SIMULATION_NOT_INITIALIZED");
            return SIMULATION_NOT_INITIALIZED;
        }

        if (simulation_running) {
            logger.warn("SIMULATION_ALREADY_RUNNING");
            return SIMULATION_ALREADY_RUNNING;
        }

        /*

         start simulation

         result = SIMULATION_CANNOT_START;

         or

         result = SIMULATION_STARTED;
         simulation_running = true;

        */
        logger.warn("SIMULATION started");
        aSimulation.start();

        return SIMULATION_STARTED;

    }

    public void doPost(HttpServletRequest request, HttpServletResponse res) throws ServletException, IOException {
        doGet(request, res);
    }


    public String generateIoIniFile() {
        return io_ini_section1_general +
                io_ini_section2_stations +
                io_ini_section3_meteo_path +
                io_ini_section4_filters +
                io_ini_section5_interpolations;
    }
}

