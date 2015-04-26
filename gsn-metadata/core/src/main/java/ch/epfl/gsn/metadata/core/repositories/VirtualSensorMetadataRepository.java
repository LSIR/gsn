package ch.epfl.gsn.metadata.core.repositories;

import ch.epfl.gsn.metadata.core.model.VirtualSensorMetadata;
import ch.epfl.gsn.metadata.core.model.gsnjson.VirtualSensor;
import org.springframework.data.repository.CrudRepository;

import java.math.BigInteger;

/**
 * Created by kryvych on 12/03/15.
 */
public interface VirtualSensorMetadataRepository extends CrudRepository<VirtualSensorMetadata, BigInteger>{

    public VirtualSensorMetadata findOneByName(String name);
}
