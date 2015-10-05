package ch.epfl.gsn.metadata.web.services;

import ch.epfl.gsn.metadata.core.model.GeoData;
import ch.epfl.gsn.metadata.core.model.ObservedProperty;
import ch.epfl.gsn.metadata.core.model.VirtualSensorMetadata;
import ch.epfl.gsn.metadata.core.model.WikiInfo;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.*;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by kryvych on 26/03/15.
 */
@Service
public class GeoJsonConverter {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private final Properties configuration;


    @Inject
    public GeoJsonConverter(Properties configuration) {
        this.configuration = configuration;
    }

    public String convertMeasurementRecords(Collection<VirtualSensorMetadata> records, boolean detailed) {
        try (StringWriter writer = new StringWriter()) {
            writeRecordstoJsonStream(writer, records, detailed);
            return writer.toString();
        } catch (IOException e) {
            logger.error("Cannot write JSON! ", e);
            return "";
        }
    }


    protected void writeRecordstoJsonStream(StringWriter out, Collection<VirtualSensorMetadata> records, boolean detailed) throws IOException {
        JsonWriter writer = new JsonWriter(out);
        writer.beginObject();
        writer.name("type").value("FeatureCollection");
        writer.name("features");
        writer.beginArray();
        for (VirtualSensorMetadata record : records) {
            writeRecord(writer, record, detailed);
        }
        writer.endArray();
        writer.endObject();
        writer.close();
    }

//    protected void writeRecordsArray(JsonWriter writer, Collection<T> records) throws IOException {
//        writer.beginArray();
//        for (T record : records) {
//            writeRecord(writer, record);
//        }
//        writer.endArray();
//    }

    protected void writeRecord(JsonWriter writer, VirtualSensorMetadata record, boolean detailed) throws IOException {
        if (record == null || record.getLocation() == null) {
            return;
        }
        writer.beginObject();
        writer.name("type").value("Feature");
        writer.name("geometry");
        //Geometry
        writer.beginObject();
        writer.name("type").value("Point");
        writer.name("coordinates");
        writePoint(writer, record.getLocation());
        writer.endObject();

        writer.name("properties");
        //Properties
        writer.beginObject();

        if (detailed) {
            writeDetailed(writer, record);
        } else {
            writeShort(writer, record);
        }


        WikiInfo wikiInfo = record.getWikiInfo();


        writeExtra(writer, record);

        writer.endObject();

        writer.endObject();
    }

    protected void writeExtra(JsonWriter writer, VirtualSensorMetadata record) throws IOException {
    }

    protected void writeShort(JsonWriter writer, VirtualSensorMetadata record) throws IOException {

        writer.name("sensorName").value(record.getName());
        WikiInfo wikiInfo = record.getWikiInfo();
        if (wikiInfo != null) {
            String organisation = wikiInfo.getOrganisation();
            if (organisation != null) {
                writer.name("organisation").value(organisation);
            }
            writer.name("deployment").value(wikiInfo.getDeploymentName());
        }

        writer.name("fromDate").value(DATE_FORMAT.format(record.getFromDate() == null ? new Date() : record.getFromDate()));
        writer.name("toDate").value(DATE_FORMAT.format(record.getToDate() == null ? new Date() : record.getToDate()));

        writer.name("serverLink").value(record.getServer());
        writer.name("metadataLink")
                .value(configuration.getProperty("metadata.server") + "metadata/virtualSensors/" + record.getName());
        String dataLink = MessageFormat.format(configuration.getProperty("gsn.server.vs"), record.getName().toLowerCase());
        writer.name("dataLink").value(dataLink);
        if (wikiInfo != null) {
            writer.name("wikiLink").value(configuration.getProperty("wiki.server") + wikiInfo.getWikiLink());
        } else {
            writer.name("wikiLink").value(record.getMetadataLink());
        }

        writePlotLink(writer, record);

        writeObservedProperties(writer, record);

        writeGeoData(writer, record);



    }

    private void writePlotLink(JsonWriter writer, VirtualSensorMetadata record) throws IOException {
        StringBuilder link = new StringBuilder();
        link.append(configuration.getProperty("gsn.plot"));
        link.append("sensors=").append(record.getName()).append("&parameters=");
        final List<ObservedProperty> observedProperties = record.getSortedProperties();
        int count = 0;
        List<String> columnNames = Lists.newArrayList();

        for (ObservedProperty observedProperty : observedProperties) {
            if (count++ < 5) {
                columnNames.add(observedProperty.getColumnName());
            }
        }
        Joiner.on(",").appendTo(link, columnNames);

        writer.name("plotLink").value(link.toString());
    }

    private void writeGeoData(JsonWriter writer, VirtualSensorMetadata record) throws IOException {
        GeoData geoData = record.getGeoData();
        if (geoData != null) {
            writer.name("aspect").value(geoData.getAspect());
            writer.name("slopeAngle").value(geoData.getSlope());
            writer.name("elevation").value(geoData.getElevation());
        }
    }

    protected void writeDetailed(JsonWriter writer, VirtualSensorMetadata record) throws IOException {

        writer.name("sensorName").value(record.getName());
        if (StringUtils.isNotEmpty(record.getSamplingFrequency())) {
            writer.name("samplingFreq").value(record.getSamplingFrequency());
        }
        if (record.getSensor() != null) {
            writer.name("serialNumber").value(record.getSensor().getSerialNumber());
        }

        if (StringUtils.isNotEmpty(record.getDescription())) {
            writer.name("description").value(record.getDescription());
        }

        WikiInfo wikiInfo = record.getWikiInfo();
        if (wikiInfo != null) {
            writer.name("deployment").value(wikiInfo.getDeploymentName());
            writer.name("organisation").value(wikiInfo.getOrganisation());
            writer.name("e-mail").value(wikiInfo.getEmail());
            writer.name("relativeCoordinates").value(wikiInfo.getRelativePosition().getPosition());
        }

//        writer.name("server").value(record.getServer());
        writeObservedProperties(writer, record);

        writeGeoData(writer, record);

        writer.name("deployed").value(buildDeploymentDatesString(record.getFromDate(), record.getToDate()));
        String dataLink = MessageFormat.format(configuration.getProperty("gsn.server.vs"), record.getName().toLowerCase());
        writer.name("dataLink").value(dataLink);
        if (wikiInfo != null) {
            writer.name("wikiLink").value(configuration.getProperty("wiki.server") + wikiInfo.getWikiLink());
        } else {
            writer.name("wikiLink").value(record.getMetadataLink());
        }

    }

    private void writeObservedProperties(JsonWriter writer, VirtualSensorMetadata record) throws IOException {

        Multimap<String, String> termToColumn = getTermToColumnMultimap(record);

        writer.name("observed_properties");
        writer.beginArray();
        for (String term : termToColumn.keySet()) {
            writer.value(term);
        }
        writer.endArray();

        for (String term : termToColumn.keySet()) {
            writer.name(term);
            writer.beginArray();
            for (String column : termToColumn.get(term)) {
                writer.value(column);
            }
            writer.endArray();
        }
    }

    protected Multimap<String, String> getTermToColumnMultimap(VirtualSensorMetadata record) {
        Multimap<String, String> termToColumn = TreeMultimap.create();

        for (ObservedProperty property : record.getObservedProperties()) {
            String term = property.getName();
            if (StringUtils.isNotEmpty(term) && !term.equals("NA")) {
                termToColumn.put(term, property.getColumnName());
            }
        }
        return termToColumn;
    }

    protected void writePoint(JsonWriter writer, Point point) throws IOException {
        writer.beginArray();
        writer.value(point.getX());
        writer.value(point.getY());
        writer.endArray();
    }

    protected String buildDeploymentDatesString(Date from, Date to) {
        StringBuilder sb = new StringBuilder();
        sb.append("from ");
        sb.append(from == null ? "Unspecified " : DATE_FORMAT.format(from));
        sb.append(" to ");
        sb.append(to == null ? "ongoing " : DATE_FORMAT.format(to));
        return sb.toString();
    }
//    protected void writeLink(JsonWriter writer)
}
