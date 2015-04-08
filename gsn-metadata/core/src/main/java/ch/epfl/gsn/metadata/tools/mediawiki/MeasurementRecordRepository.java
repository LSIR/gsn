package ch.epfl.gsn.metadata.tools.mediawiki;

import ch.epfl.gsn.metadata.tools.mediawiki.model.MeasurementRecord;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Point;
import org.springframework.data.repository.CrudRepository;

import java.math.BigInteger;
import java.util.List;

/**
 * Created by kryvych on 24/03/15.
 */
public interface MeasurementRecordRepository extends CrudRepository<MeasurementRecord, BigInteger> {

    List<MeasurementRecord> findByDbTableName(String dbTableName);


}
