package gsn.vsensor;

import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.utils.Helpers;
import gsn.utils.models.ModelFitting;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.TreeMap;

import static gsn.utils.models.ModelFitting.getModelIdFromString;


public class DataCleanVirtualSensor extends AbstractVirtualSensor {

    private static final transient Logger logger = Logger.getLogger(BridgeVirtualSensor.class);

    private static final String OPERATOR = "_operator";
    private static final String DEPLOYMENT = "_deployment";
    private static final String STATION = "_station";
    private static final String SENSOR = "_sensor";
    private static final String FROM = "_from";
    private static final String TO = "_to";
    private static final String DIRTINESS = "_dirtiness";

    private static final String xml_template = "<metadata>\n" +
            "\t<deployement>" + DEPLOYMENT + "</deployment>\n" +
            "\t<operator>" + OPERATOR + "</operator>\n" +
            "\t<station>" + STATION + "</station>\n" +
            "\t<sensor>" + SENSOR + "</sensor>\n" +
            "\t<from>" + FROM + "</from>\n" +
            "\t<to>" + TO + "</to>\n" +
            "\t<dirtiness>" + DIRTINESS + "</dirtiness>\n" +
            "</metadata>";

    private static final String PARAM_MODEL = "model";
    private static final String PARAM_ERROR_BOUND = "error_bound";
    private static final String PARAM_WINDOW_SIZE = "window_size";

    // optional parameters
    private static final String PARAM_METADATA_SERVER = "metadata_server_url"; // metadata server for posting metadata, e.g. http://www.example.com/dataclean.php

    private static final String PARAM_METADATA_USERNAME = "user"; // username for metadata server
    private static final String PARAM_METADATA_PASSWORD = "password"; // password for metadata server

    private static final String PARAM_METADATA_OPERATOR = "operator"; // operator for metadata server, typically e-mail
    private static final String PARAM_METADATA_DEPLOYEMENT = "deployment"; // name of deployemnt for metadata server
    private static final String PARAM_METADATA_STATION = "station"; // name of station for metadata server
    private static final String PARAM_METADATA_SENSOR = "sensor"; // name of station for metadata server
    private static final String PARAM_LOGGING_INTERVAL = "logging-interval";

    private static final int NORMAL_RESULT = 200; // normal result after http post

    private int model = -1;
    private int window_size = 0;
    private double error_bound = 0;

    private double[] stream;
    private long[] timestamps;
    private double[] processed;
    private double[] dirtiness;
    private double[] quality;

    private String metadata_server_url;
    private String username;
    private String password;
    private String operator;
    private String deployement;
    private String station;
    private String sensor;

    private String prepared_xml_request;

    private boolean publish_to_metadata_server = false;
    private boolean metadata_server_requieres_password = false;


    private int bufferCount = 0;
    private boolean logging_timestamps = false;
    private long logging_interval = 1;
    private long logging_counter = 0;


    public boolean initialize() {

        TreeMap<String, String> params = getVirtualSensorConfiguration().getMainClassInitialParams();

        String model_str = params.get(PARAM_MODEL);

        if (model_str == null) {
            logger.warn("Parameter \"" + PARAM_MODEL + "\" not provided in Virtual Sensor file");
            return false;
        } else {
            model = getModelIdFromString(model_str.trim());
            if (model == -1) {
                logger.warn("Parameter \"" + PARAM_MODEL + "\" incorrect in Virtual Sensor file");
                return false;
            }
        }

        String window_size_str = params.get(PARAM_WINDOW_SIZE);

        if (window_size_str == null) {
            logger.warn("Parameter \"" + PARAM_WINDOW_SIZE + "\" not provided in Virtual Sensor file");
            return false;
        } else try {
            window_size = Integer.parseInt(window_size_str.trim());
        } catch (NumberFormatException e) {
            logger.warn("Parameter \"" + PARAM_WINDOW_SIZE + "\" incorrect in Virtual Sensor file");
            return false;
        }

        if (window_size < 0) {
            logger.warn("Window size should always be positive.");
            return false;
        }

        String error_bound_str = params.get(PARAM_ERROR_BOUND);

        if (error_bound_str == null) {
            logger.warn("Parameter \"" + PARAM_ERROR_BOUND + "\" not provided in Virtual Sensor file");
            return false;
        } else try {
            error_bound = Double.parseDouble(error_bound_str.trim());
        } catch (NumberFormatException e) {
            logger.warn("Parameter \"" + PARAM_ERROR_BOUND + "\" incorrect in Virtual Sensor file");
            return false;
        }

        metadata_server_url = params.get(PARAM_METADATA_SERVER);

        if (metadata_server_url != null) {
            publish_to_metadata_server = true;

            username = params.get(PARAM_METADATA_USERNAME);
            password = params.get(PARAM_METADATA_PASSWORD);

            if (username != null && password != null)
                metadata_server_requieres_password = true;

            operator = params.get(PARAM_METADATA_OPERATOR);
            deployement = params.get(PARAM_METADATA_DEPLOYEMENT);
            station = params.get(PARAM_METADATA_STATION);
            sensor = params.get(PARAM_METADATA_SENSOR);

            if ((operator == null) || (deployement == null) || (station == null) || (sensor == null)) {
                logger.warn("A parameter required for publishing metadata is missing. Couldn't publish to metadata server.");
                publish_to_metadata_server = false;
            } else {
                prepared_xml_request = xml_template.replaceAll(OPERATOR, operator);
                prepared_xml_request = prepared_xml_request.replaceAll(DEPLOYMENT, deployement);
                prepared_xml_request = prepared_xml_request.replaceAll(STATION, station);
                prepared_xml_request = prepared_xml_request.replaceAll(SENSOR, sensor);
            }

        }

        stream = new double[window_size];
        timestamps = new long[window_size];
        processed = new double[window_size];
        dirtiness = new double[window_size];
        quality = new double[window_size];

        String logging_interval_str = params.get(PARAM_LOGGING_INTERVAL);
        if (logging_interval_str != null) {
            logging_timestamps = true;
            try {
                logging_interval = Integer.parseInt(logging_interval_str.trim());
            } catch (NumberFormatException e) {
                logger.warn("Parameter \"" + PARAM_LOGGING_INTERVAL + "\" incorrect in Virtual Sensor file");
                logging_timestamps = false;
            }
        }

        return true;
    }

    public void dataAvailable(String inputStreamName, StreamElement data) {

        if (logging_timestamps && logging_counter % logging_interval == 0) {
            logger.warn(getVirtualSensorConfiguration().getName() + " , " + logging_counter + " , " + System.currentTimeMillis());
        }

        logging_counter++;

        if (bufferCount < window_size) {
            timestamps[bufferCount] = data.getTimeStamp();
            stream[bufferCount] = (Double) data.getData()[0];
            bufferCount++;
        } else {
            ModelFitting.FitAndMarkDirty(model, error_bound, window_size, stream, timestamps, processed, dirtiness, quality);

            for (int j = 0; j < processed.length; j++) {
                StreamElement se = new StreamElement(new String[]{"stream", "processed", "dirtiness", "distance", "quality"},
                        new Byte[]{DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.DOUBLE},
                        new Serializable[]{stream[j], processed[j], dirtiness[j], processed[j] - stream[j], quality[j]},
                        timestamps[j]);
                dataProduced(se);
                if ((dirtiness[j] > 0) && publish_to_metadata_server) {
                    try {
                        String request = outputAsXML(stream[j], processed[j], dirtiness[j], timestamps[j], timestamps[j]);
                        boolean result = httpPost(metadata_server_url, request);
                        if (!result) {
                            logger.warn("Couldn't post request => " + request);
                        } else {
                            logger.warn("Posted => " + request);
                        }
                    } catch (Exception e) {
                        logger.warn("Error while trying to post to metadata server. " + e.getMessage() + e);
                    }
                }
            }
            bufferCount = 0;
        }
    }


    public boolean httpPost(String url, String xmlString) {

        boolean success = true;
        PostMethod post = new PostMethod(url);
        RequestEntity entity;
        HttpClient httpclient = new HttpClient();

        if (metadata_server_requieres_password) {
            httpclient.getState().setCredentials(
                    new AuthScope(metadata_server_url, 80),
                    new UsernamePasswordCredentials(username, password)
            );
        }

        try {
            entity = new StringRequestEntity(xmlString, "text/xml", "ISO-8859-1");

            post.setRequestEntity(entity);
            int result = httpclient.executeMethod(post);

            if (result != NORMAL_RESULT) {
                logger.warn("Response status code: " + result);

                // Display response
                logger.warn("Response body: ");
                logger.warn(post.getResponseBodyAsString());
                success = false;
            }
        }

        catch (UnsupportedEncodingException e) {
            logger.warn(new StringBuilder("Unsupported encoding for: ").append(url));
            success = false;
        }

        catch (HttpException e) {
            logger.warn(new StringBuilder("Error for: ")
                    .append(url).append(e));
            success = false;
        }

        catch (IOException e) {
            logger.warn(new StringBuilder("Error for: ")
                    .append(url).append(e));
            success = false;
        }

        finally {
            post.releaseConnection();
        }


        return success;
    }

    /*
   * Returns an xml string from the given parameters
   * */
    public String outputAsXML(double stream, double processed, double dirtiness, long from_date, long to_date) throws Exception {
        String output = prepared_xml_request.replaceAll(DIRTINESS, Double.toString(dirtiness));
        output = output.replaceAll(FROM, Helpers.convertTimeFromLongToIso(from_date, "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"));
        output = output.replaceAll(TO, Helpers.convertTimeFromLongToIso(to_date, "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"));

        return output;
    }

    public void dispose() {

    }


}
