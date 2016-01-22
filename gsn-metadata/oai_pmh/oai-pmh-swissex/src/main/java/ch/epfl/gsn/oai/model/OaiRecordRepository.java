package ch.epfl.gsn.oai.model;

import org.springframework.data.repository.CrudRepository;

import javax.inject.Named;
import java.math.BigInteger;
import java.util.Date;

/**
 * Created by kryvych on 25/09/15.
 */
@Named
public interface OaiRecordRepository extends CrudRepository<OsperRecord, BigInteger> {

    OsperRecord findByName(String name);

    Iterable<OsperRecord> findByDateStampBetween(Date from, Date to);

    Iterable<OsperRecord> findByDateStampGreaterThan(Date from);

    Iterable<OsperRecord> findByDateStampLessThan(Date to);


}
