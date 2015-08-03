package ch.epfl.gsn.metadata.web.services;

import ch.epfl.gsn.metadata.core.model.ObservedProperty;
import ch.epfl.gsn.metadata.core.model.VirtualSensorMetadata;
import com.google.gson.stream.JsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Map;

/**
 * Created by kryvych on 04/05/15.
 */
@Named
public class UIJsonConverter {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    public String convertObservedProperties(Map<String, ObservedProperty> properties) {
        try (StringWriter stringWriter = new StringWriter()) {
            JsonWriter writer = new JsonWriter(stringWriter);
            writer.beginObject();
            for (String colName : properties.keySet()) {
                ObservedProperty observedProperty = properties.get(colName);
                writer.name(colName).beginObject()
                        .name("name").value(observedProperty.getName())
                        .name("unit").value(observedProperty.getUnit());
                writer.endObject();
            }
            writer.endObject();
            writer.close();
            return stringWriter.toString();
        } catch (IOException e) {
            logger.error("Cannot write JSON! ", e);
            return "";
        }
    }


}
