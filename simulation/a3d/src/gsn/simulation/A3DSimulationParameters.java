package gsn.simulation;

import gsn.utils.GSNRuntimeException;
import org.apache.log4j.Logger;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static gsn.utils.Helpers.convertTimeFromIsoToLong;


public class A3DSimulationParameters {

    private static final String SIMULATION_CONFIG_FILE = "conf/simulation.properties";

    private static final transient Logger logger = Logger.getLogger(A3DSimulationParameters.class);

    private static final String PARAM_SERVERHOSTNAME = "hostname";
    private static final String PARAM_USERNAME = "username";
    private static final String PARAM_PASSWORD = "password";
    private static final String PARAM_SCRIPTNAME = "scriptname";
    private static final String PARAM_BASE_DIR = "base-dir";
    private static final String PARAM_SCRIPT_TYPE = "script-type";
    private static final String PARAM_LOCAL_WORKING_DIR = "local-working-dir";
    private static final String PARAM_TIMEOUT = "timeout";

    private String UUID;
    private long longStartFrom;
    private long longEndTo;

    public String getStartFrom() {
        return startFrom;
    }

  
    public String getEndTo() {
        return endTo;
    }

    public List<String> getStations() {
        return stations;
    }

    public void setStations(List<String> stations) {
        this.stations = stations;
    }

    private String startFrom;
    private String endTo;

    private long windowSize;
    private List<String> interpolations = new ArrayList<String>();
    private List<String> filters = new ArrayList<String>();;
    private String serverHostname;
    private String serverUsername;
    private String serverPassword;
    private String scriptName;
    private String basedir;

    public String getLocalWorkingDir() {
        return localWorkingDir;
    }

    public void setLocalWorkingDir(String localWorkingDir) {
        this.localWorkingDir = localWorkingDir;
    }

    public String getBasedir() {
        return basedir;
    }

    public void setBasedir(String basedir) {
        this.basedir = basedir;
    }

    private String localWorkingDir;
    private List<String> stations = new ArrayList<String>();

    /*
    * Constructor
    * */
    public A3DSimulationParameters() {
        setDefaults();
    }

    @Override
    public String toString() {
        return "A3DSimulationParameters{" +
                " UUID='" + UUID + '\'' +
                ", longStartFrom=" + longStartFrom +
                ", longEndTo=" + longEndTo +
                ", windowSize=" + windowSize +
                ", interpolations=" + interpolations +
                ", filters=" + filters +
                ", serverHostname='" + serverHostname + '\'' +
                ", serverUsername='" + serverUsername + '\'' +
                ", serverPassword='" + serverPassword + '\'' +
                ", stations=" + stations +
                '}';
    }

    /*
   * Load defaults from config file
   * and sets default parameters
   * */
    public int setDefaults() {
        Properties p = new Properties();
        try {
            FileInputStream fis = new FileInputStream(SIMULATION_CONFIG_FILE);
            p.load(fis);
            fis.close();
        } catch (FileNotFoundException e) {
            logger.warn("Couldn't run simulation. Config file not found: " + SIMULATION_CONFIG_FILE);
        } catch (IOException e) {
            logger.warn("IO error: " + SIMULATION_CONFIG_FILE + e);
        }
        setServerHostname(p.getProperty(PARAM_SERVERHOSTNAME));
        setServerUsername(p.getProperty(PARAM_USERNAME));
        setServerPassword(p.getProperty(PARAM_PASSWORD));
        setLocalWorkingDir(p.getProperty(PARAM_LOCAL_WORKING_DIR));
        setScriptName(p.getProperty(PARAM_SCRIPTNAME)); 
        setBasedir(p.getProperty(PARAM_BASE_DIR));
        return 0;
    }

    public String getServerHostname() {
        return serverHostname;
    }

    public void setServerHostname(String serverHostname) {
        this.serverHostname = serverHostname;
    }

    public String getServerUsername() {
        return serverUsername;
    }

    public void setServerUsername(String serverUsername) {
        this.serverUsername = serverUsername;
    }

    public String getServerPassword() {
        return serverPassword;
    }

    public void setServerPassword(String serverPassword) {
        this.serverPassword = serverPassword;
    }

    public long getLongStartFrom() {
        return longStartFrom;
    }

    public void setLongStartFrom(long longStartFrom) {
        this.longStartFrom = longStartFrom;
    }

    public long getLongEndTo() {
        return longEndTo;
    }

    public void setLongEndTo(long longEndTo) {
        this.longEndTo = longEndTo;
    }

    public long getWindowSize() {
        return windowSize;
    }

    public void setWindowSize(long windowSize) {
        this.windowSize = windowSize;
    }

    public List<String> getFilter() {
        return filters;
    }

    public void addInterpolation(String interpolation) {
        interpolations.add(interpolation);
    }

    public void addSpecificFilter(String filter) {
        filters.add(filter);
    }

    public List<String> getFilters() {
        return filters;
    }



    public List<String> getInterpolations() {
        return interpolations;
    }


    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }


    public String generateIO_INI() {
        return null;
    }


    private static final String IO_INI = "";

    public void setStartTime(String startFrom) {
        try {
            this.startFrom = startFrom;
            this.setLongStartFrom(convertTimeFromIsoToLong(startFrom,"yyyyMMdd'T'HHmm"));
        } catch (Exception e) {
            throw new GSNRuntimeException("Incorrect starting date: "+startFrom);
        }
    }

    public void setEndTo(String endTo) {
        try {
            this.endTo = endTo;
            this.setLongEndTo(convertTimeFromIsoToLong(endTo,"yyyyMMdd'T'HHmm"));
        } catch (Exception e) {
            throw new GSNRuntimeException("Incorrect starting date: "+endTo);
        }
    }

    public void setStations(String s) throws GSNRuntimeException {
        String[] _s = s.split(",");
        stations.clear();
        for (int i=0;i<_s.length;i++) {
            stations.add(_s[i]);
            logger.warn("station "+i+ " "+_s[i]);
        }
    }

    public void setType(String type) throws GSNRuntimeException {

    }

    public void setScriptName(String s) {
        scriptName = s;
    }

    public String getScriptName()  {
        return scriptName;
    }

    public void setScriptParams(String s) throws GSNRuntimeException {
        //

    }

    /* converts time from string to long
    * returns -1 if not successful
    * */
    private long strTime2Long(String s, String timeFormat) {

        long l = -1;
        try {
            DateTimeFormatter fmt = DateTimeFormat.forPattern(timeFormat);
            l = fmt.parseDateTime(s).getMillis();
        }
        catch (IllegalArgumentException e) {
            logger.warn(e.getMessage(), e);
        }
        return l;
    }


}
