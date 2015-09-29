package ch.epfl.gsn.oai.impl;

import ch.epfl.gsn.oai.OaiIdentifierBuilder;
import ch.epfl.gsn.oai.model.OaiRecordRepository;
import ch.epfl.gsn.oai.model.OsperRecord;
import ch.epfl.gsn.oai.interfaces.DataAccessException;
import ch.epfl.gsn.oai.interfaces.RecordAccessService;
import com.google.common.collect.Sets;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Date;
import java.util.Set;

/**
 * Created by kryvych on 25/09/15.
 */
@Named
public class RecordAccessServiceImpl implements RecordAccessService<OsperRecord> {

    private final OaiRecordRepository repository;
    private final MongoTemplate mongoTemplate;

    private final OaiIdentifierBuilder identifierBuilder;

    @Inject
    public RecordAccessServiceImpl(OaiRecordRepository repository, MongoTemplate mongoTemplate, OaiIdentifierBuilder identifierBuilder) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.identifierBuilder = identifierBuilder;
    }

    @Override
    public Set<OsperRecord> getRecords(Date from, Date to) throws DataAccessException {
        return getRecords(from, to, null);
    }

    @Override
    public Set<OsperRecord> getRecords(Date from, Date to, String resumptionToken) throws DataAccessException {
        if (from == null && to == null) {
            return Sets.newHashSet(repository.findAll());
        }

        if (from != null && to == null) {
            return Sets.newHashSet(repository.findByDateStampGreaterThan(from));
        }

        if (from == null) {
            return Sets.newHashSet(repository.findByDateStampLessThan(to));
        }

        return Sets.newHashSet(repository.findByDateStampBetween(from, to));

    }

    @Override
    public OsperRecord getRecord(String identifier) throws DataAccessException {
        return repository.findByName(identifierBuilder.extractSensorName(identifier));
    }

    @Override
    public boolean isValidResumptionToken(String resumptionToken) {
        return false;
    }

    @Override
    public Date getEarliestDatestamp() {

        Query query = new Query();
        query.with(new Sort(Sort.Direction.DESC, "dateStamp"));

        OsperRecord record = mongoTemplate.findOne(query, OsperRecord.class);
        if (record != null) {
            return record.getDateStamp();
        }
        throw new DataAccessException("No records available!");
    }
}
