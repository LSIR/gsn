package ch.epfl.gsn.metadata.core.services;

import ch.epfl.gsn.metadata.core.model.gsnjson.VirtualSensor;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kryvych on 11/03/15.
 */
@Named
public class GSNVirtualSensorsReader {

    public List<VirtualSensor> read(InputStream in) throws IOException {
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
