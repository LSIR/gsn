package gsn.vsensor;

import java.io.Serializable;

import gsn.beans.DataField;
import gsn.beans.StreamElement;

import org.apache.log4j.Logger;

public class IButtonDataParser extends BridgeVirtualSensorPermasense {

    private static final transient Logger logger = Logger.getLogger(IButtonDataParser.class);

    private static final short PAYLOAD_TYPE_MISSION_DATA = 0;
    private static final short PAYLOAD_TYPE_TEMPERATURE_MEASUREMENTS = 1;
    private static final short PAYLOAD_TYPE_HUMIDITY_MEASUREMENTS = 2;

    private static DataField[] dataFieldMission = {
        new DataField("TIMESTAMP", "BIGINT"),
        new DataField("MISSION_ID", "BIGINT"),
        new DataField("DEVICE_ID", "INTEGER"),
        new DataField("SESSION_NO", "INTEGER"),
        new DataField("PROGRAMMING_TIME", "BIGINT"),
        new DataField("COLLECTING_TIME_HOST", "BIGINT"),
        new DataField("COLLECTING_TIME_BUTTON", "BIGINT"),
        new DataField("NUM_SAMPLES", "INTEGER"),
        new DataField("NUM_ROLLED_OVER", "INTEGER"),
        new DataField("SAMPLING_RATE", "INTEGER"),
        new DataField("SAMPLING_START_TIME", "BIGINT"),
        new DataField("ENABLE_TEMP", "SMALLINT"),
        new DataField("TEMP_HI_RES", "SMALLINT"),
        new DataField("TEMP_USED_SW_CORR", "SMALLINT"),
        new DataField("TEMP_COEFF_A", "DOUBLE"),
        new DataField("TEMP_COEFF_B", "DOUBLE"),
        new DataField("TEMP_COEFF_C", "DOUBLE"),
        new DataField("ENABLE_HUMID", "SMALLINT"),
        new DataField("HUMID_HI_RES", "SMALLINT"),
        new DataField("HUMID_USED_SW_CORR", "SMALLINT"),
        new DataField("HUMID_COEFF_A", "DOUBLE"),
        new DataField("HUMID_COEFF_B", "DOUBLE"),
        new DataField("HUMID_COEFF_C", "DOUBLE"), 
        new DataField("LON", "DOUBLE"),
        new DataField("LAT", "DOUBLE"),
        new DataField("ALT", "DOUBLE"),
        new DataField("ACCURACY", "DOUBLE"),
        new DataField("BUTTON_PREFIX", "INTEGER"),
        new DataField("BUTTON_NO", "INTEGER"),
        new DataField("SERIAL_NO", "VARCHAR(23)"),
        new DataField("SYNC_TIME", "BIGINT"), 

        new DataField("DATA_IMPORT_SOURCE", "SMALLINT")};

    private static DataField[] dataFieldMeasurements = {
        new DataField("TIMESTAMP", "BIGINT"),
        new DataField("MISSION_ID", "BIGINT"),
        new DataField("DEVICE_ID", "INTEGER"),
        new DataField("GENERATION_TIME", "BIGINT"),
        new DataField("SAMPLE_NO", "SMALLINT"),
        new DataField("MEASUREMENT", "DOUBLE"),

        new DataField("DATA_IMPORT_SOURCE", "SMALLINT")};

    @Override
    public void dataAvailable(String inputStreamName, StreamElement data) {
        try
        {
            Short payloadType = (Short)data.getData("payload_type");
            byte[] payloadBytes = (byte[])data.getData("payload");
            String payload = new String(payloadBytes);

            String[] payloadParts = payload.split(",");
            if(payloadType == PAYLOAD_TYPE_MISSION_DATA) {
                if(payloadParts.length != 30) {
                    logger.error("Invalid payload format.");
                    return;
                }
                Serializable[] streamData = new Serializable[] {
                        data.getData(dataFieldMission[0].getName()),
                        Long.valueOf(payloadParts[0]),
                        Integer.valueOf(payloadParts[1]),
                        Integer.valueOf(payloadParts[2]),
                        Long.valueOf(payloadParts[3]),
                        ((!payloadParts[4].isEmpty()) ? Long.valueOf(payloadParts[4]) : null),
                        ((!payloadParts[5].isEmpty()) ? Long.valueOf(payloadParts[5]) : null),
                        ((!payloadParts[6].isEmpty()) ? Integer.valueOf(payloadParts[6]) : null),
                        ((!payloadParts[7].isEmpty()) ? Integer.valueOf(payloadParts[7]) : null),
                        Integer.valueOf(payloadParts[8]),
                        Long.valueOf(payloadParts[9]),
                        Short.valueOf(payloadParts[10]),
                        Short.valueOf(payloadParts[11]),
                        ((!payloadParts[12].isEmpty()) ? Short.valueOf(payloadParts[12]) : null),
                        ((!payloadParts[13].isEmpty()) ? Double.valueOf(payloadParts[13]) : null),
                        ((!payloadParts[14].isEmpty()) ? Double.valueOf(payloadParts[14]) : null),
                        ((!payloadParts[15].isEmpty()) ? Double.valueOf(payloadParts[15]) : null),
                        Short.valueOf(payloadParts[16]),
                        Short.valueOf(payloadParts[17]),
                        ((!payloadParts[18].isEmpty()) ? Short.valueOf(payloadParts[18]) : null),
                        ((!payloadParts[19].isEmpty()) ? Double.valueOf(payloadParts[19]) : null),
                        ((!payloadParts[20].isEmpty()) ? Double.valueOf(payloadParts[20]) : null),
                        ((!payloadParts[21].isEmpty()) ? Double.valueOf(payloadParts[21]) : null),
                        ((!payloadParts[22].isEmpty()) ? Double.valueOf(payloadParts[22]) : null),
                        ((!payloadParts[23].isEmpty()) ? Double.valueOf(payloadParts[23]) : null),
                        ((!payloadParts[24].isEmpty()) ? Double.valueOf(payloadParts[24]) : null),
                        ((!payloadParts[25].isEmpty()) ? Double.valueOf(payloadParts[25]) : null),
                        Integer.valueOf(payloadParts[26]),
                        Integer.valueOf(payloadParts[27]),
                        payloadParts[28],
                        Long.valueOf(payloadParts[29]),
                        null
                };
                StreamElement curr_data = new StreamElement(dataFieldMission, streamData);
                super.dataAvailable(inputStreamName, curr_data);
            } else if(payloadType == PAYLOAD_TYPE_TEMPERATURE_MEASUREMENTS || payloadType == PAYLOAD_TYPE_HUMIDITY_MEASUREMENTS) {
                if(payloadParts.length < 3) {
                    logger.error("Invalid payload format.");
                    return;
                }
                Long missionId = Long.valueOf(payloadParts[0]);
                Integer buttonId = Integer.valueOf(payloadParts[1]);
                // The number of included measurements is at index 2
                for(int i=0; i<Integer.valueOf(payloadParts[2]); i++) {
                    Serializable[] streamData = new Serializable[] {
                            data.getData(dataFieldMeasurements[0].getName()),
                            missionId,
                            buttonId,
                            Long.valueOf(payloadParts[(i*3)+3+0]),
                            Short.valueOf(payloadParts[(i*3)+3+1]),
                            Double.valueOf(payloadParts[(i*3)+3+2]),
                            null
                    };
                    StreamElement curr_data = new StreamElement(dataFieldMeasurements, streamData);
                    super.dataAvailable(inputStreamName, curr_data);
                }
    
            } else {
                logger.error("Unknown payload_type value "+payloadType);
                return;
            }
        } catch(Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
