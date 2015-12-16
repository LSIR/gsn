package ch.epfl.gsn.metadata.core.services;

import ch.epfl.gsn.metadata.core.model.GSNMetadata;
import ch.epfl.gsn.metadata.core.model.GSNMetadataBuilder;
import ch.epfl.gsn.metadata.core.model.GridMetadata;
import ch.epfl.gsn.metadata.core.model.VirtualSensorMetadata;
import ch.epfl.gsn.metadata.core.model.gsnjson.VirtualSensor;
import ch.epfl.gsn.metadata.core.repositories.GridMetadataRepository;
import ch.epfl.gsn.metadata.core.repositories.MetadataRepositoryFactory;
import ch.epfl.gsn.metadata.core.repositories.VirtualSensorMetadataRepository;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

/**
 * Created by kryvych on 11/03/15.
 */
@Service
public class VirtualSensorPersistenceService {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private GSNVirtualSensorsReader reader;

    private VirtualSensorMetadataRepository virtualSensorMetadataRepository;

    private GridMetadataRepository gridMetadataRepository;

    private GSNMetadataBuilder builder;

    @Inject
    public VirtualSensorPersistenceService(GSNVirtualSensorsReader reader, VirtualSensorMetadataRepository virtualSensorMetadataRepository, GridMetadataRepository gridMetadataRepository, GSNMetadataBuilder builder) {
        this.reader = reader;

        this.virtualSensorMetadataRepository = virtualSensorMetadataRepository;
        this.gridMetadataRepository = gridMetadataRepository;
        this.builder = builder;
    }

    public int write(InputStream inputStream, String server) {

        int count = 0;
        virtualSensorMetadataRepository.deleteAll();

        try {
            List<VirtualSensor> sensors = reader.read(inputStream);

            for (VirtualSensor sensor : sensors) {

                if (!builder.isGrid(sensor) && builder.hasCoordinates(sensor)) {
                    VirtualSensorMetadata metadata = builder.buildVirtualSensorMetadata(sensor, server);
                    virtualSensorMetadataRepository.save(metadata);
                    count++;
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        return count;
    }


    public int update(InputStream inputStream, String server) throws IOException {

        int newCount = 0;

        List<VirtualSensor> sensors = reader.read(inputStream);

        Set<String> sensorNames = Sets.newHashSet();
        for (VirtualSensor sensor : sensors) {

            if (!builder.isGrid(sensor) && builder.hasCoordinates(sensor)) {
                VirtualSensorMetadata metadata = builder.buildVirtualSensorMetadata(sensor, server);
                VirtualSensorMetadata sensorMetadata = virtualSensorMetadataRepository.findOneByName(metadata.getName());
                if (sensorMetadata != null) {
                    sensorMetadata.update(metadata);
                    virtualSensorMetadataRepository.save(sensorMetadata);
                } else {
                    logger.info("Added new sensor: " + metadata.getName());
                    virtualSensorMetadataRepository.save(metadata);
                    newCount++;
                }
                sensorNames.add(metadata.getName());
            }

        }

        deleteSensors(sensorNames);

        return newCount;
    }

    public void deleteSensors(Set<String> serverSensors) {
        Iterable<VirtualSensorMetadata> dbSensors = virtualSensorMetadataRepository.findAll();
        for (VirtualSensorMetadata dbSensor : dbSensors) {
            if (!serverSensors.contains(dbSensor.getName())) {
                virtualSensorMetadataRepository.delete(dbSensor);
                logger.info("Removed sensor: " + dbSensor.getName());
            }
        }
    }

}
