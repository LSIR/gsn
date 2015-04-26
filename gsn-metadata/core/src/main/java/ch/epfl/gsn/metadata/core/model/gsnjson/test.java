package ch.epfl.gsn.metadata.core.model.gsnjson;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kryvych on 03/03/15.
 */
public class test {

    public static final String feature = "{\n" +
            "type: \"Feature\",\n" +
            "properties: {\n" +
            "vs_name: \"WFJ_VF_Lysimeter\",\n" +
            "values: [\n" +
            "[\n" +
            "1355173140000,\n" +
            "0,\n" +
            "0,\n" +
            "0,\n" +
            "735.9,\n" +
            "0,\n" +
            "0,\n" +
            "0\n" +
            "]\n" +
            "],\n" +
            "fields: [\n" +
            "{\n" +
            "name: \"timestamp\",\n" +
            "type: \"time\",\n" +
            "unit: \"ms\"\n" +
            "},\n" +
            "{\n" +
            "name: \"lysimeter_current\",\n" +
            "type: \"DOUBLE\",\n" +
            "unit: \"buckets\"\n" +
            "},\n" +
            "{\n" +
            "name: \"lysimeter_since_0700\",\n" +
            "type: \"DOUBLE\",\n" +
            "unit: \"buckets\"\n" +
            "},\n" +
            "{\n" +
            "name: \"lysimeter_daily_sum\",\n" +
            "type: \"DOUBLE\",\n" +
            "unit: \"buckets\"\n" +
            "},\n" +
            "{\n" +
            "name: \"atm_pressure\",\n" +
            "type: \"DOUBLE\",\n" +
            "unit: \"mbar\"\n" +
            "},\n" +
            "{\n" +
            "name: \"snow_water_equiv_mm\",\n" +
            "type: \"DOUBLE\",\n" +
            "unit: \"mm\"\n" +
            "},\n" +
            "{\n" +
            "name: \"swe_since_0700_mm\",\n" +
            "type: \"DOUBLE\",\n" +
            "unit: \"mm\"\n" +
            "},\n" +
            "{\n" +
            "name: \"swe_daily_sum_mm\",\n" +
            "type: \"DOUBLE\",\n" +
            "unit: \"mm\"\n" +
            "}\n" +
            "],\n" +
            "stats: {\n" +
            "start-datetime: 704588400000,\n" +
            "end-datetime: 1355173140000\n" +
            "},\n" +
            "altitude: \"2585\",\n" +
            "name: \"WFJ_VF_Lysimeter\",\n" +
            "latitude: \"46.829535\",\n" +
            "description: \"SLF sensor in the research field on Weissfluhjoch. Lysimeter values are the number of bucket tips (0.8l bucket,5m^2), Lysimeter current is a 10min value. Snow Water Equivalent (SWE) values are calculated as (Lysimeter*0.8/5). SWE values are in mm. Data Copyright SLF\",\n" +
            "exposition: \"NULL\",\n" +
            "longitude: \"9.809225\",\n" +
            "accessProtected: \"false\",\n" +
            "gps_precision: \"10\",\n" +
            "geographical: \"Davos, Switzerland.\",\n" +
            "slope: \"0\"\n" +
            "},\n" +
            "geometry: {\n" +
            "type: \"Point\",\n" +
            "coordinates: [\n" +
            "46.829535,\n" +
            "9.809225,\n" +
            "2585\n" +
            "]\n" +
            "}\n" +
            "}";
//    public static final String features = "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"properties\":{\"vs_name\":\"clisp_gr_ETHZ_2099_lhf\",\"values\":[],\"fields\":[{\"name\":\"ncols\",\"type\":\"int\",\"unit\":null},{\"name\":\"nrows\",\"type\":\"int\",\"unit\":null},{\"name\":\"xllcorner\",\"type\":\"double\",\"unit\":null},{\"name\":\"yllcorner\",\"type\":\"double\",\"unit\":null},{\"name\":\"cellsize\",\"type\":\"double\",\"unit\":null},{\"name\":\"nodata_value\",\"type\":\"double\",\"unit\":null},{\"name\":\"grid\",\"type\":\"binary\",\"unit\":null}],\"stats\":{\"start-datetime\":null,\"end-datetime\":null},\"altitude\":\"1166\",\"latitude\":\"46.1747\",\"description\":\"Alpine3D simulations (based on IPCC climate scenario A1B) of latent heat flux (lhf) over the whole of\\n    Graubuenden for the CLISP project. \\n\\nFor more information: \\n\\nBavay, M., T. Grünewald, and M. Lehning. \\\"Response of snow cover and runoff to climate change in high Alpine catchments of Eastern Switzerland.\\\" Advances in Water Resources (2013), http://dx.doi.org/10.1016/j.advwatres.2012.12.009.\\n\\nCLISP: http://www.clisp.eu\\n\\nTo download data from this virtual sensor, use the griddata\\nquery (see http://sourceforge.net/apps/trac/gsn/wiki/web-interfacev1-server)\",\"longitude\":\"8.63905\",\"accessProtected\":\"false\"},\"geometry\":{\"type\":\"Point\",\"coordinates\":[46.1747,8.63905,1166.0]}},{\"type\":\"Feature\",\"properties\":{\"vs_name\":\"imis_bev_1\",\"values\":[],\"fields\":[{\"name\":\"vw\",\"type\":\"DOUBLE\",\"unit\":\"m/s\"},{\"name\":\"vw_max\",\"type\":\"DOUBLE\",\"unit\":\"m/s\"},{\"name\":\"dw\",\"type\":\"DOUBLE\",\"unit\":\"°\"},{\"name\":\"ta\",\"type\":\"DOUBLE\",\"unit\":\"°C\"},{\"name\":\"rh\",\"type\":\"DOUBLE\",\"unit\":\"%\"},{\"name\":\"rswr\",\"type\":\"DOUBLE\",\"unit\":\"W/m²\"},{\"name\":\"hs1\",\"type\":\"DOUBLE\",\"unit\":\"cm\"},{\"name\":\"ts0\",\"type\":\"DOUBLE\",\"unit\":\"°C\"},{\"name\":\"ts1\",\"type\":\"DOUBLE\",\"unit\":\"°C\"},{\"name\":\"ts2\",\"type\":\"DOUBLE\",\"unit\":\"°C\"},{\"name\":\"ts3\",\"type\":\"DOUBLE\",\"unit\":\"°C\"},{\"name\":\"tss\",\"type\":\"DOUBLE\",\"unit\":\"°C\"},{\"name\":\"pint\",\"type\":\"DOUBLE\",\"unit\":\"mm/h\"}],\"stats\":{\"start-datetime\":null,\"end-datetime\":null},\"altitude\":\"2538.1279\",\"latitude\":\"46.54898293\",\"description\":\"imis station bev_1\",\"longitude\":\"9.853761199\",\"accessProtected\":\"true\"},\"geometry\":{\"type\":\"Point\",\"coordinates\":[46.54898293,9.853761199,2538.1279]}}]}";
    public static final String features = "{type: \"FeatureCollection\",\n" +
        "features: [\n" +
        "{\n" +
        "type: \"Feature\",\n" +
        "properties: {\n" +
        "vs_name: \"clisp_gr_ETHZ_2099_lhf\",\n" +
        "values: [\n" +
        "[\n" +
        "1279537200000,\n" +
        "706,\n" +
        "504,\n" +
        "692699.885,\n" +
        "114399.406,\n" +
        "200,\n" +
        "-999,\n" +
        "\"[B@f9b01b5\"\n" +
        "]\n" +
        "],\n" +
        "fields: [\n" +
        "{\n" +
        "name: \"timestamp\",\n" +
        "type: \"time\",\n" +
        "unit: \"ms\"\n" +
        "},\n" +
        "{\n" +
        "name: \"ncols\",\n" +
        "type: \"int\",\n" +
        "unit: null\n" +
        "},\n" +
        "{\n" +
        "name: \"nrows\",\n" +
        "type: \"int\",\n" +
        "unit: null\n" +
        "},\n" +
        "{\n" +
        "name: \"xllcorner\",\n" +
        "type: \"double\",\n" +
        "unit: null\n" +
        "},\n" +
        "{\n" +
        "name: \"yllcorner\",\n" +
        "type: \"double\",\n" +
        "unit: null\n" +
        "},\n" +
        "{\n" +
        "name: \"cellsize\",\n" +
        "type: \"double\",\n" +
        "unit: null\n" +
        "},\n" +
        "{\n" +
        "name: \"nodata_value\",\n" +
        "type: \"double\",\n" +
        "unit: null\n" +
        "},\n" +
        "{\n" +
        "name: \"grid\",\n" +
        "type: \"binary\",\n" +
        "unit: null\n" +
        "}\n" +
        "],\n" +
        "stats: {\n" +
        "start-datetime: 970398000000,\n" +
        "end-datetime: 1279537200000\n" +
        "},\n" +
        "altitude: \"1166\",\n" +
        "parameter: \"latent heat flux\",\n" +
        "latitude: \"46.1747\",\n" +
        "description: \"Alpine3D simulations (based on IPCC climate scenario A1B) of latent heat flux (lhf) over the whole of Graubuenden for the CLISP project. For more information: Bavay, M., T. Grünewald, and M. Lehning. \"Response of snow cover and runoff to climate change in high Alpine catchments of Eastern Switzerland.\" Advances in Water Resources (2013), http://dx.doi.org/10.1016/j.advwatres.2012.12.009. CLISP: http://www.clisp.eu To download data from this virtual sensor, use the griddata query (see http://sourceforge.net/apps/trac/gsn/wiki/web-interfacev1-server)\",\n" +
        "longitude: \"8.63905\",\n" +
        "accessProtected: \"false\"\n" +
        "},\n" +
        "geometry: {\n" +
        "type: \"Point\",\n" +
        "coordinates: [\n" +
        "46.1747,\n" +
        "8.63905,\n" +
        "1166\n" +
        "]\n" +
        "}\n" +
        "}]\n" +
        "}";
    public static void main(String[] args) throws IOException {
        Gson gson = new Gson();
        VirtualSensor virtualSensor = gson.fromJson(feature, VirtualSensor.class);
        System.out.println("virtualSensor = " + virtualSensor);

        virtualSensor.getProperties().getStats().getStartDatetime();
        virtualSensor.getProperties().getAccessProtected();
        InputStream in = new ByteArrayInputStream(features.getBytes(StandardCharsets.UTF_8));
        List<VirtualSensor> virtualSensors = readJsonStream(in);
        System.out.println("virtualSensors = " + virtualSensors);
    }

    public static List<VirtualSensor> readJsonStream(InputStream in) throws IOException {
        Gson gson = new Gson();

        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        reader.beginObject();
        reader.nextName();
        reader.nextString();
        reader.nextName();

        List<VirtualSensor> sensors = new ArrayList();
        reader.beginArray();
        while (reader.hasNext()) {
            VirtualSensor sensor = gson.fromJson(reader, VirtualSensor.class);
            sensors.add(sensor);
        }
        reader.endArray();
        reader.endObject();
        reader.close();
        return sensors;
    }
}
