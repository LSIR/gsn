package ch.epfl.gsn.metadata.web.services;

import ch.epfl.gsn.metadata.core.model.VirtualSensorMetadata;
import ch.epfl.gsn.metadata.core.repositories.VirtualSensorMetadataRepository;

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

    @Inject
    public VirtualSensorAccessService(VirtualSensorMetadataRepository sensorMetadataRepository) {
        this.sensorMetadataRepository = sensorMetadataRepository;
    }

    public Iterable<VirtualSensorMetadata> allSensors() {
        return sensorMetadataRepository.findAll();
    }

}
