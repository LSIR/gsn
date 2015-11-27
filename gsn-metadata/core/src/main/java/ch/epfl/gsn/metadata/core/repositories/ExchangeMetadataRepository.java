package ch.epfl.gsn.metadata.core.repositories;

import ch.epfl.gsn.metadata.core.model.exchange.ExchangeMetadata;
import org.springframework.data.repository.CrudRepository;

import java.math.BigInteger;

/**
 * Created by kryvych on 03/09/15.
 */
public interface ExchangeMetadataRepository extends CrudRepository<ExchangeMetadata, BigInteger> {

    ExchangeMetadata findOneByName(String name);
}
