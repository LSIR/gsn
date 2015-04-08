package ch.epfl.gsn.metadata.core.repositories;

import ch.epfl.gsn.metadata.core.model.GSNMetadata;
import ch.epfl.gsn.metadata.core.model.GridMetadata;
import org.springframework.data.repository.CrudRepository;

import javax.inject.Inject;
import javax.inject.Named;
import java.math.BigInteger;

/**
 * Created by kryvych on 13/03/15.
 */
@Named
public class MetadataRepositoryFactory {
    private GridMetadataRepository gridMetadataRepository;
    private VirtualSensorMetadataRepository virtualSensorMetadataRepository;

    @Inject
    public MetadataRepositoryFactory(GridMetadataRepository gridMetadataRepository, VirtualSensorMetadataRepository virtualSensorMetadataRepository) {
        this.gridMetadataRepository = gridMetadataRepository;
        this.virtualSensorMetadataRepository = virtualSensorMetadataRepository;
    }

    public <T extends GSNMetadata> CrudRepository<T, BigInteger>  getRepositoryForClass(T clazz) {
        if (clazz.equals(GridMetadata.class)) {
            return (CrudRepository<T, BigInteger>) gridMetadataRepository;
        }
        return (CrudRepository<T, BigInteger>) virtualSensorMetadataRepository;

    }

}
