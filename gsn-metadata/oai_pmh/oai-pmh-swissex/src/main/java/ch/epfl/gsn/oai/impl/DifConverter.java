package ch.epfl.gsn.oai.impl;

import ch.epfl.gsn.metadata.core.model.VirtualSensorMetadata;
import ch.epfl.gsn.metadata.core.repositories.VirtualSensorMetadataRepository;
import ch.epfl.gsn.oai.interfaces.Converter;
import ch.epfl.gsn.oai.interfaces.MetadataFormat;
import ch.epfl.gsn.oai.model.OsperRecord;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;

import javax.inject.Inject;
import javax.inject.Named;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

/**
 * Created by kryvych on 24/09/15.
 */
@Named
public class DifConverter implements Converter<OsperRecord> {

    protected final VirtualSensorMetadataRepository metadataRepository;

    protected final Properties osperConfiguration;

    protected static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    @Inject
    public DifConverter(VirtualSensorMetadataRepository metadataRepository, Properties osperConfiguration) {
        this.metadataRepository = metadataRepository;
        this.osperConfiguration = osperConfiguration;
    }

    @Override
    public String convert(OsperRecord record) {
        String oaiRecord = record.getXmlRecord();
        VirtualSensorMetadata metadata = metadataRepository.findOneByName(record.getName());
        Map<String, String> parameters = Maps.newHashMap();
        parameters.put("landingPage", osperConfiguration.getProperty("landingPage") + record.getName());
        parameters.put("dataCenterURL", osperConfiguration.getProperty("dataCenterURL"));
        if (metadata != null) {
            parameters.put("fromDate", DATE_FORMAT.format(metadata.getFromDate() == null ? new Date() : metadata.getFromDate()));
            parameters.put("toDate", DATE_FORMAT.format(metadata.getToDate() == null ? new Date() : metadata.getToDate()));
            parameters.put("latitude", String.valueOf(metadata.getLocation().getY()));
            parameters.put("longitude", String.valueOf(metadata.getLocation().getX()));
            if (StringUtils.isNotBlank(metadata.getMetadataLink())) {
                parameters.put("WIKIPAGE", metadata.getMetadataLink());
            } else {
                parameters.put("WIKIPAGE", "NA");
            }
            parameters.put("longitude", String.valueOf(metadata.getLocation().getX()));
        }

        StrSubstitutor substitutor = new StrSubstitutor(parameters);
        return substitutor.replace(oaiRecord);
    }

    @Override
    public <M extends MetadataFormat> boolean isForFormat(Class<M> formatClass) {
        return formatClass.equals(DifFormat.class);
    }

    @Override
    public boolean canConvertRecord(OsperRecord record) {
        return true;
    }
}
