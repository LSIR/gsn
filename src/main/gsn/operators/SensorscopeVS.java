package gsn.operators;

import gsn.beans.DataTypes;
import gsn.beans.Operator;
import gsn.beans.StreamElement;
import gsn.beans.DataField;
import gsn.channels.DataChannel;
import gsn.core.OperatorConfig;
import gsn2.conf.Parameters;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * This virtual sensor is used for accessing Sensorscope data with
 * MigMessageWrapper.
 */

public class SensorscopeVS  implements Operator {
	
	public void process ( String inputStreamName , List<StreamElement> data ) {
		for (StreamElement se: data)
			process(inputStreamName, se);
	}

  public DataField[] getStructure() {
    return new DataField[0];  //To change body of implemented methods use File | Settings | File Templates.
  }

  public void start() {}
	public void stop() {}


    private static final transient Logger logger = Logger.getLogger(SensorscopeVS.class);

    private int samplingTime = 30000;
    private static final int NO_VALUE = Short.MIN_VALUE;

    // default fieldnames
    private static final String NTW_SENDER_ID_Key = "NTWSENDERID";
    private static final String DEFAULT_NTW_DISTANCE_TO_BTS = "NTWDISTTOBS";
    private static final String DEFAULT_TSP_HOP_COUNT = "TSPHOPCOUNT";
    private static final String DEFAULT_TSP_PACKET_SN = "TSPPACKETSN";
    private static final String DEFAULT_REPORTER_ID = "REPORTERID";
    private static final String DEFAULT_TIMESTAMP = "TIMESTAMP";
    private static final String DEFAULT_RAIN_METER = "RAINMETER";
    private static final String DEFAULT_WIND_SPEED = "WINDSPEED";
    private static final String DEFAULT_WATERMARK = "WATERMARK";
    private static final String DEFAULT_SOLAR_RADIATION = "SOLARRADIATION";
    private static final String DEFAULT_AIR_TEMPERATURE = "AIRTEMPERATURE";
    private static final String DEFAULT_AIR_HUMIDITY = "AIRHUMIDITY";
    private static final String DEFAULT_SKIN_TEMPERATURE = "SKINTEMPERATURE";
    private static final String DEFAULT_SOIL_MOISTURE = "SOILMOISTURE";
    private static final String DEFAULT_WIND_DIRECTION = "WINDDIRECTION";
    private static final String DEFAULT_WIND_DIRECTION2 = "WINDDIRECTION2";
    private static final String DEFAULT_SOIL_CONDUCTIVITY_1 = "SOILCONDUCTIVITY1";
    private static final String DEFAULT_SOIL_CONDUCTIVITY_2 = "SOILCONDUCTIVITY2";
    private static final String DEFAULT_SOIL_CONDUCTIVITY_3 = "SOILCONDUCTIVITY3";
    private static final String DEFAULT_SOIL_MOISTURE_1 = "SOILMOISTURE1";
    private static final String DEFAULT_SOIL_MOISTURE_2 = "SOILMOISTURE2";
    private static final String DEFAULT_SOIL_MOISTURE_3 = "SOILMOISTURE3";
    private static final String DEFAULT_SOIL_TEMPERATURE_1 = "SOILTEMPERATURE1";
    private static final String DEFAULT_SOIL_TEMPERATURE_2 = "SOILTEMPERATURE2";
    private static final String DEFAULT_SOIL_TEMPERATURE_3 = "SOILTEMPERATURE3";
    private static final String DEFAULT_FOO = "FOO";

    // initialisation with default fieldnames
    private String NTW_SENDER_ID_NAME = NTW_SENDER_ID_Key;
    private String NTW_DISTANCE_TO_BTS = DEFAULT_NTW_DISTANCE_TO_BTS;
    private String TSP_HOP_COUNT = DEFAULT_TSP_HOP_COUNT;
    private String TSP_PACKET_SN = DEFAULT_TSP_PACKET_SN;
    private String REPORTER_ID = DEFAULT_REPORTER_ID;
    private String TIMESTAMP = DEFAULT_TIMESTAMP;
    private String RAIN_METER = DEFAULT_RAIN_METER;
    private String WIND_SPEED = DEFAULT_WIND_SPEED;
    private String WATERMARK = DEFAULT_WATERMARK;
    private String SOLAR_RADIATION = DEFAULT_SOLAR_RADIATION;
    private String AIR_TEMPERATURE = DEFAULT_AIR_TEMPERATURE;
    private String AIR_HUMIDITY = DEFAULT_AIR_HUMIDITY;
    private String SKIN_TEMPERATURE = DEFAULT_SKIN_TEMPERATURE;
    private String SOIL_MOISTURE = DEFAULT_SOIL_MOISTURE;
    private String WIND_DIRECTION = DEFAULT_WIND_DIRECTION;
    private String WIND_DIRECTION2 = DEFAULT_WIND_DIRECTION2;
    private String SOIL_CONDUCTIVITY_1 = DEFAULT_SOIL_CONDUCTIVITY_1;
    private String SOIL_CONDUCTIVITY_2 = DEFAULT_SOIL_CONDUCTIVITY_2;
    private String SOIL_CONDUCTIVITY_3 = DEFAULT_SOIL_CONDUCTIVITY_3;
    private String SOIL_MOISTURE_1 = DEFAULT_SOIL_MOISTURE_1;
    private String SOIL_MOISTURE_2 = DEFAULT_SOIL_MOISTURE_2;
    private String SOIL_MOISTURE_3 = DEFAULT_SOIL_MOISTURE_3;
    private String SOIL_TEMPERATURE_1 = DEFAULT_SOIL_TEMPERATURE_1;
    private String SOIL_TEMPERATURE_2 = DEFAULT_SOIL_TEMPERATURE_2;
    private String SOIL_TEMPERATURE_3 = DEFAULT_SOIL_TEMPERATURE_3;
    private String FOO = DEFAULT_FOO;

	private DataChannel outputChannel;

    public SensorscopeVS(OperatorConfig config,DataChannel outputChannel ) {
		this.outputChannel = outputChannel;
        Parameters params = config.getParameters();
        samplingTime = params.getValueAsInt("sampling",samplingTime);

        logger.warn("Sampling time : > " + samplingTime + " <");

        //initialize field names from virtual sensor file

        NTW_SENDER_ID_NAME = params.getValueWithDefault(NTW_SENDER_ID_Key.toLowerCase(), NTW_SENDER_ID_Key );
        NTW_DISTANCE_TO_BTS = params.getValueWithDefault(DEFAULT_NTW_DISTANCE_TO_BTS.toLowerCase(),DEFAULT_NTW_DISTANCE_TO_BTS.toLowerCase());
        
        logger.warn("NTW_SENDER_ID : > "+ NTW_SENDER_ID_NAME + " <");
        logger.warn("NTW_DISTANCE_TO_BTS : > "+ NTW_DISTANCE_TO_BTS + " <");
        
        TSP_HOP_COUNT = params.getValueWithDefault(DEFAULT_TSP_HOP_COUNT.toLowerCase(),DEFAULT_TSP_HOP_COUNT.toLowerCase());
        logger.warn("TSP_HOP_COUNT : > "+ TSP_HOP_COUNT + " <");

        TSP_PACKET_SN = params.getValueWithDefault(DEFAULT_TSP_PACKET_SN.toLowerCase(),DEFAULT_TSP_PACKET_SN.toLowerCase());
        logger.warn("TSP_PACKET_SN : > "+ TSP_PACKET_SN + " <");

        REPORTER_ID = params.getValueWithDefault(DEFAULT_REPORTER_ID.toLowerCase(),DEFAULT_REPORTER_ID.toLowerCase());
        logger.warn("REPORTER_ID : > "+REPORTER_ID + " <");

        TIMESTAMP = params.getValueWithDefault(DEFAULT_TIMESTAMP.toLowerCase(),DEFAULT_TIMESTAMP.toLowerCase());
        logger.warn("TIMESTAMP : > "+TIMESTAMP + " <");

        RAIN_METER = params.getValueWithDefault(DEFAULT_RAIN_METER.toLowerCase(),DEFAULT_RAIN_METER.toLowerCase());
        logger.warn("RAIN_METER : > "+ RAIN_METER + " <");

        WIND_SPEED = params.getValueWithDefault(DEFAULT_WIND_SPEED.toLowerCase(),DEFAULT_WIND_SPEED.toLowerCase());
        logger.warn("WIND_SPEED : > "+ WIND_SPEED + " <");

        WATERMARK = params.getValueWithDefault(DEFAULT_WATERMARK.toLowerCase(),DEFAULT_WATERMARK.toLowerCase());
        logger.warn("WATERMARK : > "+ WATERMARK + " <");

        SOLAR_RADIATION = params.getValueWithDefault(DEFAULT_SOLAR_RADIATION.toLowerCase(),DEFAULT_SOLAR_RADIATION.toLowerCase());
        logger.warn("SOLAR_RADIATION : > "+ SOLAR_RADIATION + " <");

        AIR_TEMPERATURE = params.getValueWithDefault(DEFAULT_AIR_TEMPERATURE.toLowerCase(),DEFAULT_AIR_TEMPERATURE.toLowerCase());
        logger.warn("AIR_TEMPERATURE : > "+ AIR_TEMPERATURE + " <");

        AIR_HUMIDITY = params.getValueWithDefault(DEFAULT_AIR_HUMIDITY.toLowerCase(),DEFAULT_AIR_HUMIDITY.toLowerCase());
        logger.warn("AIR_HUMIDITY : > "+ AIR_HUMIDITY + " <");

        SKIN_TEMPERATURE = params.getValueWithDefault(DEFAULT_SKIN_TEMPERATURE.toLowerCase(),DEFAULT_SKIN_TEMPERATURE.toLowerCase());
        logger.warn("SKIN_TEMPERATURE : > "+ SKIN_TEMPERATURE + " <");

        SOIL_MOISTURE = params.getValueWithDefault(DEFAULT_SOIL_MOISTURE.toLowerCase(),DEFAULT_SOIL_MOISTURE.toLowerCase());
        logger.warn("SOIL_MOISTURE : > "+ SOIL_MOISTURE + " <");

        WIND_DIRECTION = params.getValueWithDefault(DEFAULT_WIND_DIRECTION.toLowerCase(),DEFAULT_WIND_DIRECTION.toLowerCase());
        logger.warn("WIND_DIRECTION : > "+ WIND_DIRECTION + " <");

        WIND_DIRECTION2 = params.getValueWithDefault(DEFAULT_WIND_DIRECTION2.toLowerCase(),DEFAULT_WIND_DIRECTION2.toLowerCase());
        logger.warn("WIND_DIRECTION2 : > "+ WIND_DIRECTION2 + " <");

        SOIL_CONDUCTIVITY_1 = params.getValueWithDefault(DEFAULT_SOIL_CONDUCTIVITY_1.toLowerCase(),DEFAULT_SOIL_CONDUCTIVITY_1.toLowerCase());
        logger.warn("SOIL_CONDUCTIVITY_1 : > "+ SOIL_CONDUCTIVITY_1 + " <");

        SOIL_CONDUCTIVITY_2 = params.getValueWithDefault(DEFAULT_SOIL_CONDUCTIVITY_2.toLowerCase(),DEFAULT_SOIL_CONDUCTIVITY_2.toLowerCase());
        logger.warn("SOIL_CONDUCTIVITY_2 : > "+ SOIL_CONDUCTIVITY_2 + " <");

        SOIL_CONDUCTIVITY_3 = params.getValueWithDefault(DEFAULT_SOIL_CONDUCTIVITY_3.toLowerCase(),DEFAULT_SOIL_CONDUCTIVITY_3.toLowerCase());
        logger.warn("SOIL_CONDUCTIVITY_3 : > "+ SOIL_CONDUCTIVITY_3 + " <");

        SOIL_MOISTURE_1 = params.getValueWithDefault(DEFAULT_SOIL_MOISTURE_1.toLowerCase(),DEFAULT_SOIL_MOISTURE_1.toLowerCase());
        logger.warn("SOIL_MOISTURE_1 : > "+ SOIL_MOISTURE_1 + " <");

        SOIL_MOISTURE_2 = params.getValueWithDefault(DEFAULT_SOIL_MOISTURE_2.toLowerCase(),DEFAULT_SOIL_MOISTURE_2.toLowerCase());
        logger.warn("SOIL_MOISTURE_2 : > "+ SOIL_MOISTURE_2 + " <");

        SOIL_MOISTURE_3 = params.getValueWithDefault(DEFAULT_SOIL_MOISTURE_3.toLowerCase(),DEFAULT_SOIL_MOISTURE_3.toLowerCase());
        logger.warn("SOIL_MOISTURE_3 : > "+ SOIL_MOISTURE_3 + " <");

        SOIL_TEMPERATURE_1 = params.getValueWithDefault(DEFAULT_SOIL_TEMPERATURE_1.toLowerCase(),DEFAULT_SOIL_TEMPERATURE_1.toLowerCase());
        logger.warn("SOIL_TEMPERATURE_1 : > "+ SOIL_TEMPERATURE_1 + " <");

        SOIL_TEMPERATURE_2 = params.getValueWithDefault(DEFAULT_SOIL_TEMPERATURE_2.toLowerCase(),DEFAULT_SOIL_TEMPERATURE_2.toLowerCase());
        logger.warn("SOIL_TEMPERATURE_2 : > "+ SOIL_TEMPERATURE_2 + " <");

        SOIL_TEMPERATURE_3 = params.getValueWithDefault(DEFAULT_SOIL_TEMPERATURE_3.toLowerCase(),DEFAULT_SOIL_TEMPERATURE_3.toLowerCase());
        logger.warn("SOIL_TEMPERATURE_3 : > "+ SOIL_TEMPERATURE_3 + " <");

        FOO = params.getValueWithDefault(DEFAULT_FOO.toLowerCase(),DEFAULT_FOO.toLowerCase());
        logger.warn("FOO : > "+ FOO + " <");
    
    }

	/**
     * This method is called whenever there is new data coming to
     * virtual sensor.
     * In this case the data structure should apply the one used
     * in sensorscope.
     * If some data can't be read, then a value of smallest short
     * -32768 is returned for ints, shorts and doubles.
     */

    public void process(String inputStreamName, StreamElement data) {
        Serializable[] dataFields =null;// data.getData();
        short ntwSenderId = NO_VALUE;
        short ntwDistToBts = NO_VALUE;
        short tspHopCount = NO_VALUE;
        short tspPacketSn = NO_VALUE;
        short reporterId = NO_VALUE;
        long timestamp = NO_VALUE;
        double rainMeter = NO_VALUE;
        double windSpeed = NO_VALUE;
        double watermark = NO_VALUE;
        double solarRadiation = NO_VALUE;
        double airTemperature = NO_VALUE;
        double airHumidity = NO_VALUE;
        double skinTemperature = NO_VALUE;
        double soilMoisture = NO_VALUE;
        double windDirection = NO_VALUE;
        double windDirection2 = NO_VALUE;
        double soilConductivity1 = NO_VALUE;
        double soilConductivity2 = NO_VALUE;
        double soilConductivity3 = NO_VALUE;
        short soilMoisture1 = NO_VALUE;
        short soilMoisture2 = NO_VALUE;
        short soilMoisture3 = NO_VALUE;
        double soilTemperature1 = NO_VALUE;
        double soilTemperature2 = NO_VALUE;
        double soilTemperature3 = NO_VALUE;
        short foo = 0;

        // Air temperature is needed afterwards by watermark and humidity,
        // so it has to be calculated first
        int i = 0;
        for (String fieldName : data.getFieldNames()) {
            fieldName = fieldName.toUpperCase();
            if (fieldName.equals(AIR_TEMPERATURE)) {
                airTemperature = getTemperature((Integer) dataFields[i]);
            }
            i++;
        }

        // Output structure is adjusted dynamically depending on what input fields
        // are got.
        // fieldNames is for the field names of input stream element, they
        //    will be the same for output as well
        // dataTypes is for datatypes of the output values. These are set
        //    statically, because the input values are all int or short and
        //    outputs are short or double
        // datas has the actual data, which is calculated here
        ArrayList<String> fieldNames = new ArrayList<String>();
        ArrayList<Byte> dataTypes = new ArrayList<Byte>();
        ArrayList<Serializable> datas = new ArrayList<Serializable>();
        i = 0;
        for (String fieldName : data.getFieldNames()) {
            fieldName = fieldName.toUpperCase();
            if (fieldName.equals(SOIL_CONDUCTIVITY_1)) {
                soilConductivity1 = getSoilConductivity((Short) dataFields[i]);
                fieldNames.add(SOIL_CONDUCTIVITY_1);
                dataTypes.add(DataTypes.NUMERIC);
                datas.add(soilConductivity1);
            } else if (fieldName.equals(SOIL_CONDUCTIVITY_2)) {
                soilConductivity2 = getSoilConductivity((Short) dataFields[i]);
                fieldNames.add(SOIL_CONDUCTIVITY_2);
                dataTypes.add(DataTypes.NUMERIC);
                datas.add(soilConductivity2);
            } else if (fieldName.equals(SOIL_CONDUCTIVITY_3)) {
                soilConductivity3 = getSoilConductivity((Short) dataFields[i]);
                fieldNames.add(SOIL_CONDUCTIVITY_3);
                dataTypes.add(DataTypes.NUMERIC);
                datas.add(soilConductivity3);
            }
            //
            else if (fieldName.equals(SOIL_MOISTURE_1)) {
                soilMoisture1 = (Short) dataFields[i];
                fieldNames.add(SOIL_MOISTURE_1);
                dataTypes.add(DataTypes.NUMERIC);
                datas.add(soilMoisture1);
            } else if (fieldName.equals(SOIL_MOISTURE_2)) {
                soilMoisture2 = (Short) dataFields[i];
                fieldNames.add(SOIL_MOISTURE_2);
                dataTypes.add(DataTypes.NUMERIC);
                datas.add(soilMoisture2);
            } else if (fieldName.equals(SOIL_MOISTURE_3)) {
                soilMoisture3 = (Short) dataFields[i];
                fieldNames.add(SOIL_MOISTURE_3);
                dataTypes.add(DataTypes.NUMERIC);
                datas.add(soilMoisture3);
            }
            //
            else if (fieldName.equals(SOIL_TEMPERATURE_1)) {
                soilTemperature1 = getSoilTemperature((Short) dataFields[i]);
                fieldNames.add(SOIL_TEMPERATURE_1);
                dataTypes.add(DataTypes.NUMERIC);
                datas.add(soilTemperature1);
            } else if (fieldName.equals(SOIL_TEMPERATURE_2)) {
                soilTemperature2 = getSoilTemperature((Short) dataFields[i]);
                fieldNames.add(SOIL_TEMPERATURE_2);
                dataTypes.add(DataTypes.NUMERIC);
                datas.add(soilTemperature2);
            } else if (fieldName.equals(SOIL_TEMPERATURE_3)) {
                soilTemperature3 = getSoilTemperature((Short) dataFields[i]);
                fieldNames.add(SOIL_TEMPERATURE_3);
                dataTypes.add(DataTypes.NUMERIC);
                datas.add(soilTemperature3);
            }
            //
            else if (fieldName.equals(NTW_SENDER_ID_NAME)) {
                ntwSenderId = (Short) dataFields[i];
                fieldNames.add(NTW_SENDER_ID_NAME);
                dataTypes.add(DataTypes.NUMERIC);
                datas.add(ntwSenderId);
            } else if (fieldName.equals(NTW_DISTANCE_TO_BTS)) {
                ntwDistToBts = (Short) dataFields[i];
                fieldNames.add(NTW_DISTANCE_TO_BTS);
                dataTypes.add(DataTypes.NUMERIC);
                datas.add(ntwDistToBts);
            } else if (fieldName.equals(TSP_HOP_COUNT)) {
                tspHopCount = (Short) dataFields[i];
                fieldNames.add(TSP_HOP_COUNT);
                dataTypes.add(DataTypes.NUMERIC);
                datas.add(tspHopCount);
            } else if (fieldName.equals(TSP_PACKET_SN)) {
                tspPacketSn = (Short) dataFields[i];
                fieldNames.add(TSP_PACKET_SN);
                dataTypes.add(DataTypes.NUMERIC);
                datas.add(tspPacketSn);
            } else if (fieldName.equals(REPORTER_ID)) {
                reporterId = (Short) dataFields[i];
                fieldNames.add(REPORTER_ID);
                dataTypes.add(DataTypes.NUMERIC);
                datas.add(reporterId);
            } else if (fieldName.equals(TIMESTAMP)) {
                timestamp = (Long) dataFields[i];
                fieldNames.add(TIMESTAMP);
                dataTypes.add(DataTypes.TIME);
                datas.add(timestamp);
            } else if (fieldName.equals(RAIN_METER)) {
                rainMeter = getRainMeter((Short) dataFields[i]);
                fieldNames.add(RAIN_METER);
                dataTypes.add(DataTypes.NUMERIC);
                datas.add(rainMeter);
            } else if (fieldName.equals(WIND_SPEED)) {
                windSpeed = getWindSpeed((Integer) dataFields[i]);
                fieldNames.add(WIND_SPEED);
                dataTypes.add(DataTypes.NUMERIC);
                datas.add(windSpeed);
            } else if (fieldName.equals(WATERMARK)) {
                watermark = getWatermark((Integer) dataFields[i], airTemperature);
                fieldNames.add(WATERMARK);
                dataTypes.add(DataTypes.NUMERIC);
                datas.add(watermark);
            } else if (fieldName.equals(SOLAR_RADIATION)) {
                solarRadiation = getSolarRadiation((Integer) dataFields[i]);
                fieldNames.add(SOLAR_RADIATION);
                dataTypes.add(DataTypes.NUMERIC);
                datas.add(solarRadiation);
            } else if (fieldName.equals(AIR_TEMPERATURE)) {
                fieldNames.add(AIR_TEMPERATURE);
                dataTypes.add(DataTypes.NUMERIC);
                datas.add(airTemperature);
            } else if (fieldName.equals(AIR_HUMIDITY)) {
                airHumidity = getHumidity((Integer) ((Number) dataFields[i]).intValue(), airTemperature);
                fieldNames.add(AIR_HUMIDITY);
                dataTypes.add(DataTypes.NUMERIC);
                datas.add(airHumidity);
            } else if (fieldName.equals(SKIN_TEMPERATURE)) {
                skinTemperature = getSkinTemperature((Integer) ((Number) dataFields[i]).intValue());
                fieldNames.add(SKIN_TEMPERATURE);
                dataTypes.add(DataTypes.NUMERIC);
                datas.add(skinTemperature);
            } else if (fieldName.equals(SOIL_MOISTURE)) {
                soilMoisture = getSoilMoisture((Integer) ((Number) dataFields[i]).intValue());
                fieldNames.add(SOIL_MOISTURE);
                dataTypes.add(DataTypes.NUMERIC);
                datas.add(soilMoisture);
            } else if (fieldName.equals(WIND_DIRECTION)) {
                windDirection = getWindDirection((Integer) ((Number) dataFields[i]).intValue());
                fieldNames.add(WIND_DIRECTION);
                dataTypes.add(DataTypes.NUMERIC);
                datas.add(windDirection);
            } else if (fieldName.equals(WIND_DIRECTION2)) {
                windDirection2 = getWindDirection2((Integer) ((Number) dataFields[i]).intValue());
                fieldNames.add(WIND_DIRECTION2);
                dataTypes.add(DataTypes.NUMERIC);
                datas.add(windDirection2);
            } else if (fieldName.equals(FOO)) {
                foo = (Short) dataFields[i];
                fieldNames.add(FOO);
                dataTypes.add(DataTypes.NUMERIC);
                datas.add(foo);
            } else {
                logger.error("FIELD NOT FOUND IN THE LIST >" + fieldName + "<");
            }
            i++;
        }

        long t = data.isTimestampSet() ? data.getTimeInMillis() : System.currentTimeMillis();


//        StreamElement out = new StreamElement(
//                fieldNames.toArray(new String[]{}),
//                dataTypes.toArray(new Byte[]{}),
//                datas.toArray(new Serializable[]{}),
//                t);
//        outputChannel.write(out);//flexibile output.
    }

    public double getSoilTemperature(short rawValue) {
        return ((double) rawValue - 400.0) / 10.0;
    }

    public double getSoilConductivity(short rawValue) {
        if (rawValue <= 1000) return rawValue / 10;
        return (rawValue / 10) - 90;
    }

    public double getRainMeter(short rawValue) {
        return rawValue * 0.254;
    }

    public double getWatermark(int rawGot, double temperature) {
        double rawValue = rawGot * 1500.0 / 4095.0;

        if (rawValue >= 200 && temperature != NO_VALUE) {

            double p1 = -3.171e-23;
            double p2 = 1.868e-19;
            double p3 = -4.779e-16;
            double p4 = 6.957e-13;
            double p5 = -6.337e-10;
            double p6 = 3.735e-7;
            double p7 = -0.0001421;
            double p8 = 0.03357;
            double p9 = -4.463;
            double p10 = 258.4;

            double T = p1 * Math.pow(rawValue, 9) + p2 * Math.pow(rawValue, 8) + p3 * Math.pow(rawValue, 7) + p4 * Math.pow(rawValue, 6) + p5 * Math.pow(rawValue, 5) + p6
                    * Math.pow(rawValue, 4) + p7 * Math.pow(rawValue, 3) + p8 * Math.pow(rawValue, 2) + p9 * rawValue + p10;

            T = Math.pow(10, T) / 1000.0;

            if (T <= 1) {
                T = -20.0 * (T * (1.0 + 0.018 * (temperature - 24.0)) - 0.55);
            } else if (T <= 8) {
                T = (-3.213 * T - 4.093) / (1.0 - 0.009733 * T - 0.01205 * temperature);
            } else {
                T = -2.246 - 5.239 * T * (1.0 + 0.018 * (temperature - 24.0)) - 0.06756 * T * T * Math.pow(1.0 + 0.018 * (temperature - 24.0), 2);
            }
            return T;
        } else {
            return NO_VALUE;
        }
    }

    public double getSoilMoisture(int rawValue) {
        if (rawValue >= 400) {
            return (((rawValue * 2.5 * 1.7) / 4095.0) - 0.4) * 100.0;
        } else {
            return NO_VALUE;
        }
    }

    public double getSolarRadiation(int rawValue) {
        return ((rawValue * 2.5 * 1.4545) / 4095.0) * 1000.0 / 1.67;
    }

    public double getWindDirection(int rawValue) {
        return ((rawValue * 2.5 * 1.4545) / 4095.0) * 360.0 / 3.3;
    }

    public double getWindDirection2(int rawValue) {
        double v = rawValue * 2.5 / 4095.0;
        double vcc = 3.0;
        double R1 = 20.0;
        double R2 = 10.0;
        double R30 = 22.0;
        double R31 = 10.0;
        double k = 360.0 / 337.0;
        double R3;
        if (R31 > 0) {
            R3 = (R30 * R31) / (R30 + R31);
        } else {
            R3 = R30;
        }
        double a = R1 * v;

        double b = 360.0 * (vcc * R3 - v * R1);
        double c = -360.0 * 360.0 * v * (R2 + R3);
        double d = Math.sqrt((b * b) - (4.0 * a * c));
        if (v > 0.0) {
            return (((d - b) / (2.0 * a)) * k) % 360.0;
        } else {
            return NO_VALUE;
        }
    }

    public double getWindSpeed(int rawValue) {
        return (rawValue * 2250.0 / samplingTime) * 1.609 * 1000.0 / 3600.0;
    }

    public double getTemperature(int rawValue) {
        if (rawValue != 0) {
            return (rawValue * 0.01) - 39.6;
        } else {
            return NO_VALUE;
        }
    }

    public double getSkinTemperature(int rawValue) {
        if (rawValue != 0) {
            return (rawValue / 16.0) - 273.15;
        } else {
            return NO_VALUE;
        }
    }

    public double getHumidity(int rawValue, double temperature) {
        if (rawValue != 0 && temperature != NO_VALUE) {
            return ((rawValue * 0.0405) - 4.0 - (0.0000028 * rawValue * rawValue)) + (((rawValue * 0.00008) + 0.01) * (temperature - 25.0));
        } else {
            return NO_VALUE;
        }
    }

    public void dispose() {

    }

}
