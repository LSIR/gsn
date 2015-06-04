package ch.epfl.gsn.metadata.core.services;

import ch.epfl.gsn.metadata.core.model.GSNMetadata;
import ch.epfl.gsn.metadata.core.model.GSNMetadataBuilder;
import ch.epfl.gsn.metadata.core.model.GridMetadata;
import ch.epfl.gsn.metadata.core.model.VirtualSensorMetadata;
import ch.epfl.gsn.metadata.core.model.gsnjson.VirtualSensor;
import ch.epfl.gsn.metadata.core.repositories.GridMetadataRepository;
import ch.epfl.gsn.metadata.core.repositories.MetadataRepositoryFactory;
import ch.epfl.gsn.metadata.core.repositories.VirtualSensorMetadataRepository;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Created by kryvych on 11/03/15.
 */
@Service
public class VirtualSensorPersistenceService {

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

    public int writeVirtualSensors(InputStream inputStream, String server) {

        int count = 0;
        virtualSensorMetadataRepository.deleteAll();

        try {
            List<VirtualSensor> sensors = reader.read(inputStream);

            for (VirtualSensor sensor : sensors) {

                if (!builder.isGrid(sensor)) {
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


}
