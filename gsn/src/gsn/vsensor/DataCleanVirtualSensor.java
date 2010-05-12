package gsn.vsensor;

import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.utils.models.ModelFitting;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.TreeMap;


public class DataCleanVirtualSensor extends AbstractVirtualSensor {

    private static final transient Logger logger = Logger.getLogger(BridgeVirtualSensor.class);

    private static final String PARAM_MODEL = "model";
    private static final String PARAM_ERROR_BOUND = "error_bound";
    private static final String PARAM_WINDOW_SIZE = "window_size";

    private int model = -1;
    private int window_size = 0;
    private double error_bound = 0;

    private double[] stream;
    private long[] timestamps;
    private double[] processed;
    private double[] dirtiness;

    private int bufferCount = 0;

    public boolean initialize() {

        TreeMap<String, String> params = getVirtualSensorConfiguration().getMainClassInitialParams();

        String model_str = params.get(PARAM_MODEL);

        if (model_str == null) {
            logger.warn("Parameter \"" + PARAM_MODEL + "\" not provided in Virtual Sensor file");
            return false;
        } else try {
            model = Integer.parseInt(model_str.trim());
        } catch (NumberFormatException e) {
            logger.warn("Parameter \"" + PARAM_MODEL + "\" incorrect in Virtual Sensor file");
            return false;
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

        stream = new double[window_size];
        timestamps = new long[window_size];
        processed = new double[window_size];
        dirtiness = new double[window_size];

        return true;
    }

    public void dataAvailable(String inputStreamName, StreamElement data) {

        if (bufferCount < window_size) {
            timestamps[bufferCount] = data.getTimeStamp();
            stream[bufferCount] = (Double) data.getData()[0];
            bufferCount++;
        } else {
            ModelFitting.FitAndMarkDirty(model, error_bound, window_size, stream, timestamps, processed, dirtiness);

            for (int j = 0; j < processed.length; j++) {
                StreamElement se = new StreamElement(new String[]{"stream", "processed"},
                        new Byte[]{DataTypes.DOUBLE, DataTypes.DOUBLE},
                        new Serializable[]{stream[j], processed[j]},
                        timestamps[j]);
                dataProduced(se);
            }
            bufferCount = 0;
        }
    }

    public void dispose() {

    }

}
