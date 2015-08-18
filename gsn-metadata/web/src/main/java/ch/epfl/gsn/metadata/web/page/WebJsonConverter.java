package ch.epfl.gsn.metadata.web.page;

import ch.epfl.gsn.metadata.core.model.ObservedProperty;
import ch.epfl.gsn.metadata.core.model.VirtualSensorMetadata;
import ch.epfl.gsn.metadata.web.services.GeoJsonConverter;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by kryvych on 08/05/15.
 */
@Service
public class WebJsonConverter extends GeoJsonConverter {


    @Inject
    public WebJsonConverter(Properties configuration) {
        super(configuration);
    }

    @Override
    protected void writeExtra(JsonWriter writer, VirtualSensorMetadata record) throws IOException {

        writer.name("allProperties").beginArray();
        List<ObservedProperty> properties =record.getSortedProperties();
        for (ObservedProperty observedProperty : properties) {
                writer.beginObject();
                writer.name("name").value(observedProperty.getName());
                writer.name("columnName").value(observedProperty.getColumnName());
                writer.name("unit").value(observedProperty.getUnit());
                writer.endObject();
        }
        writer.endArray();

        writer.name("fromDate").value(DATE_FORMAT.format(record.getFromDate() == null ? new Date() : record.getFromDate()));
        writer.name("untilDate").value(DATE_FORMAT.format(record.getToDate() == null ? new Date() : record.getToDate()));
        Iterable<String> nameParts = Splitter.on('_').split(record.getName());
        if (nameParts.iterator().hasNext()) {
            writer.name("group").value(nameParts.iterator().next());
        }
        writer.name("isPublic").value(record.isPublic());
    }

//    public String writeTableModel(Collection<VirtualSensorMetadata> records){
//
//        try (StringWriter stringWriter = new StringWriter()) {
//            JsonWriter writer = new JsonWriter(stringWriter);
//            writer.beginObject();
//
//            writer.name("treeModel").beginArray();
//
//            for (VirtualSensorMetadata record : records) {
//                writer.beginObject();
//                writer.name("label").value(record.getName())
//                        .name("children").beginArray();
//
//                Multimap<String, String> termToColumn = getTermToColumnMultimap(record);
//
//                for (String term : termToColumn.keySet()) {
//                    writer.beginObject();
//                    writer.name("label").value(term);
//                    writer.name("children");
//                    writer.beginArray();
//                    for (String column : termToColumn.get(term)) {
//                        writer.beginObject();
//                        writer.name("label").value(column);
//                        writer.endObject();
//                    }
//                    writer.endArray();
//                    writer.endObject();
//                }
//                writer.endArray();
//                writer.endObject();
//            }
//
//            writer.endArray();
//            writer.endObject();
//            return stringWriter.toString();
//        } catch (IOException e) {
//            logger.error("Cannot write JSON! ", e);
//            return "";
//        }
//
//
//    }
}
