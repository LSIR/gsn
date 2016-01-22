package ch.epfl.gsn.metadata.web.page;

import ch.epfl.gsn.metadata.core.model.VirtualSensorMetadata;
import ch.epfl.gsn.metadata.core.model.exchange.ExchangeMetadata;
import ch.epfl.gsn.metadata.core.repositories.ExchangeMetadataRepository;
import ch.epfl.gsn.metadata.core.repositories.VirtualSensorMetadataRepository;
import ch.epfl.gsn.oai.impl.DifConverter;
import ch.epfl.gsn.oai.model.OaiRecordRepository;
import ch.epfl.gsn.oai.model.OsperRecord;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import org.json.JSONObject;
import org.json.XML;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

/**
 * Created by kryvych on 03/09/15.
 */
@Named
public class MetadataService {

    private VirtualSensorMetadataRepository sensorMetadataRepository;

    private ExchangeMetadataRepository exchangeMetadataRepository;

    private final OaiRecordRepository oaiRecordRepository;
    
    private final DifConverter difConverter;

    @Inject
    public MetadataService(VirtualSensorMetadataRepository sensorMetadataRepository, ExchangeMetadataRepository exchangeMetadataRepository, OaiRecordRepository oaiRecordRepository, DifConverter difConverter) {
        this.sensorMetadataRepository = sensorMetadataRepository;
        this.exchangeMetadataRepository = exchangeMetadataRepository;
        this.oaiRecordRepository = oaiRecordRepository;
        this.difConverter = difConverter;
    }

    public String getMetadataDifJson(String sensorName) {
        OsperRecord osperRecord = oaiRecordRepository.findByName(sensorName);

        if(osperRecord == null) {
//            JSONObject jsonObject = new JSONObject();
//            jsonObject.append("Failed", "Record not found");
//            return jsonObject.toString();
            return "";
        }

        String difString = difConverter.convert(osperRecord);

        JSONObject jsonObject = XML.toJSONObject(difString);
        return jsonObject.toString();

    }

    public String getMetadataJson(String sensorName) {
        VirtualSensorMetadata sensorMetadata = sensorMetadataRepository.findOneByName(sensorName);
        ExchangeMetadata exchangeMetadata = exchangeMetadataRepository.findOneByName(sensorName);

        Gson gson = new Gson();
        if (exchangeMetadata != null) {
            return gson.toJson(exchangeMetadata.getProperties());
        }
        return gson.toJson(new Metadata());
    }

    protected static class Metadata {

        private Map<String, String> gsnMetadata;
        private Map<String, String> exchangeMetadata;

        public Metadata() {

            gsnMetadata = getMetadata();
            exchangeMetadata = getExchangeMetadata();
        }

        public Map<String, String> getMetadata() {
            Map<String, String> metadata = Maps.newHashMap();
            metadata.put("Description1", "bla");
            return metadata;
        }

        public Map<String, String> getExchangeMetadata() {
            Map<String, String> metadata = Maps.newHashMap();
            metadata.put("Description2", "bla");
            return metadata;
        }

    }
}
