package ch.epfl.gsn.metadata.tools.taxonomy;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import org.springframework.data.mongodb.core.MongoTemplate;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created by kryvych on 16/03/15.
 */
@Named
public class TaxonomyResolver {
    private TaxonomyRepository taxonomyRepository;
    private MongoTemplate mongoTemplate;

    @Inject
    public TaxonomyResolver(TaxonomyRepository repository, MongoTemplate mongoTemplate) {
        this.taxonomyRepository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    public Set<String> getColumnNamesForTerm(String term) {
        List<TaxonomyProperty> properties = taxonomyRepository.findByTaxonomyName(term);
        return Sets.newConcurrentHashSet(Collections2.transform(properties, new Function<TaxonomyProperty, String>() {
            @Override
            public String apply(TaxonomyProperty taxonomyProperty) {
                return taxonomyProperty.getColumnName();
            }
        }));
    }

    public String getTermForColumnName(String columnName) {
        TaxonomyProperty taxonomyProperty = taxonomyRepository.findByColumnName(columnName.toLowerCase());
        return taxonomyProperty != null? taxonomyProperty.getTaxonomyName():null;

    }

    public Collection<String> getAllTerms() {
        return mongoTemplate.getCollection("taxonomy").distinct("taxonomyName");
    }
}
