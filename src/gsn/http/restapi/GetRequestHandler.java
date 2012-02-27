package gsn.http.restapi;

import gsn.Mappings;
import gsn.beans.DataField;
import gsn.beans.VSensorConfig;

import java.util.Iterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.apache.commons.collections.KeyValue;

public class GetRequestHandler {



    public RestResponse getSensors() {
        RestResponse restResponse = new RestResponse();

        restResponse.setHttpStatus(RestResponse.HTTP_STATUS_OK);
        restResponse.setResponse(getSensorsInfoAsJSON());
        restResponse.setType("application/json");

        return restResponse;
    }

public String getSensorsInfoAsJSON() {

        JSONArray sensorsInfo = new JSONArray();

        Iterator<VSensorConfig> vsIterator = Mappings.getAllVSensorConfigs();

        while (vsIterator.hasNext()) {

            JSONObject aSensor = new JSONObject();

            VSensorConfig sensorConfig = vsIterator.next();

            String vs_name = sensorConfig.getName();

            aSensor.put("name", vs_name);

            JSONArray listOfFields = new JSONArray();

            for (DataField df : sensorConfig.getOutputStructure()) {

                String field_name = df.getName().toLowerCase();
                String field_type = df.getType().toLowerCase();

                if (field_type.indexOf("double") >= 0) {
                    listOfFields.add(field_name);
                }
            }

            aSensor.put("fields", listOfFields);

            Double alt = 0.0;
            Double lat = 0.0;
            Double lon = 0.0;

            for (KeyValue df : sensorConfig.getAddressing()) {

                String adressing_key = df.getKey().toString().toLowerCase().trim();
                String adressing_value = df.getValue().toString().toLowerCase().trim();

                if (adressing_key.indexOf("altitude") >= 0)
                    alt = Double.parseDouble(adressing_value);

                if (adressing_key.indexOf("longitude") >= 0)
                    lon = Double.parseDouble(adressing_value);

                if (adressing_key.indexOf("latitude") >= 0)
                    lat = Double.parseDouble(adressing_value);
            }

            aSensor.put("lat", lat);
            aSensor.put("lon", lon);
            aSensor.put("alt", alt);

            sensorsInfo.add(aSensor);

        }

        return sensorsInfo.toJSONString();
    }


    public RestResponse getGeoDataForSensor() {
        RestResponse restResponse = new RestResponse();

        return restResponse;
    }

    public RestResponse getMeasurementsForSensor() {
        RestResponse restResponse = new RestResponse();

        return restResponse;
    }

    public RestResponse getMeasurementsForSensorField() {
        RestResponse restResponse = new RestResponse();

        return restResponse;
    }
}
