package ch.epfl.gsn.metadata.web.services;

import com.google.common.collect.Lists;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Created by kryvych on 30/03/15.
 */
@Named
public class QueryBuilder {
    
    public Query build(SensorQuery sensorQuery) {
        List<Criteria> criteriaList = new ArrayList<Criteria>();

        if (sensorQuery.hasValidBoundingBox()) {
            Criteria criteria = where("location").within(sensorQuery.getBoundingBox());
            criteriaList.add(criteria);
        }
        if (sensorQuery.hasValidLocation()) {
            Criteria criteria = where("location").near(sensorQuery.getLocationPoint()).maxDistance(0.5);
//            NearQuery query = NearQuery.near(sensorQuery.getLocationPoint()).maxDistance(new Distance(50, Metrics.KILOMETERS));

            criteriaList.add(criteria);
        }
        if (sensorQuery.hasValidFromDate()) {
            criteriaList.add(where("fromDate").lte(sensorQuery.getFromDateParsed()));
            criteriaList.add(where("toDate").gte(sensorQuery.getFromDateParsed()));
        }
        if (sensorQuery.hasValidToDate()) {
            criteriaList.add(where("toDate").gte(sensorQuery.getToDateParsed()));
            criteriaList.add((where("fromDate").lte(sensorQuery.getToDateParsed())));
        }

        if (sensorQuery.isOnlyPublic()) {
            Criteria criteria = where("isPublic").is(Boolean.TRUE);
            criteriaList.add(criteria);
        }

        Set<String> observedProperties = sensorQuery.getObservedProperties();
        if (!observedProperties.isEmpty()) {
            if (!sensorQuery.isConjunction()) { //OR
                Criteria criteria = where("propertyNames").in(observedProperties);
                criteriaList.add(criteria);
            } else { //AND
                for (String observedProperty : observedProperties) {
                    Criteria criteria = where("propertyNames").is(observedProperty);
                    criteriaList.add(criteria);
                }
            }

        }

        if (sensorQuery.hasTopoQuery()) {
            criteriaList.addAll(buildGeoCriterias(sensorQuery));
        }

        criteriaList.add(buildOnlyWithMappedParametersCriteria());

        Criteria criteria = new Criteria();
        if (!criteriaList.isEmpty()) {
            criteria.andOperator(criteriaList.toArray(new Criteria[criteriaList.size()]));
        }


        Query query = new Query(criteria);

//        if (!observedProperties.isEmpty()) {
//            TextCriteria textCriteria = TextCriteria.forDefaultLanguage().
//                    matchingAny(observedProperties.toArray(new String[observedProperties.size()]));
//            query.addCriteria(textCriteria);
//        }

        return query;
    }

    protected Collection<Criteria> buildGeoCriterias(SensorQuery query) {

        List<Criteria> result = Lists.newArrayList();
        result.add(where("geoData.slope").gte(query.getSlopeMin()));
        result.add(where("geoData.slope").lte(query.getSlopeMax()));
        result.add(where("geoData.elevation").gte(query.getAltitudeMin()));
        result.add(where("geoData.elevation").lte(query.getAltitudeMax()));
        result.add(where("geoData.aspect").gte(query.getAspectMin()));
        result.add(where("geoData.aspect").lte(query.getAspectMax()));
        return result;
    }

    protected Criteria buildOnlyWithMappedParametersCriteria() {
        Criteria criteria = where("propertyNames").not().size(0);
        return criteria;
    }
}
