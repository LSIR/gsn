package ch.epfl.gsn.metadata.web.services;

import ch.epfl.gsn.metadata.core.model.VirtualSensorMetadata;
import ch.epfl.gsn.metadata.core.repositories.VirtualSensorMetadataRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.Iterator;

/**
 * Created by kryvych on 26/03/15.
 */
@Named
public class VirtualSensorAccessService {

    private VirtualSensorMetadataRepository sensorMetadataRepository;

    private MongoTemplate mongoTemplate;

    @Inject
    public VirtualSensorAccessService(VirtualSensorMetadataRepository sensorMetadataRepository, MongoTemplate mongoTemplate) {
        this.sensorMetadataRepository = sensorMetadataRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public Iterable<VirtualSensorMetadata> allSensors() {
        return sensorMetadataRepository.findAll();
    }

    public Iterable<VirtualSensorMetadata> findForQuery(Query query) {
        return mongoTemplate.find(query, VirtualSensorMetadata.class);
    }

    public VirtualSensorMetadata getVirtualSensorMetadata(String sensorName) {
        return sensorMetadataRepository.findOneByName(sensorName);
    }
}
