package ch.epfl.gsn.metadata.tools.taxonomy;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Set;

/**
 * Created by kryvych on 16/03/15.
 */
@Named
public class TaxonomyResolver {
    private TaxonomyRepository taxonomyRepository;

    @Inject
    public TaxonomyResolver(TaxonomyRepository repository) {
        this.taxonomyRepository = repository;
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
        TaxonomyProperty taxonomyProperty = taxonomyRepository.findByColumnName(columnName);
        return taxonomyProperty != null? taxonomyProperty.getTaxonomyName():null;

    }
}
