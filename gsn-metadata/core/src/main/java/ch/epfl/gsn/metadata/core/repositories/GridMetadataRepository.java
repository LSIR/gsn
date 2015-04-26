package ch.epfl.gsn.metadata.core.repositories;

import ch.epfl.gsn.metadata.core.model.GridMetadata;
import org.springframework.data.repository.CrudRepository;

import java.math.BigInteger;

/**
 * Created by kryvych on 12/03/15.
 */
public interface GridMetadataRepository extends CrudRepository<GridMetadata, BigInteger>{
}
