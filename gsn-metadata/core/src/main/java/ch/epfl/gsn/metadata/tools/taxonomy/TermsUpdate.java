package ch.epfl.gsn.metadata.tools.taxonomy;

import ch.epfl.gsn.metadata.core.model.GeoData;
import ch.epfl.gsn.metadata.core.model.ObservedProperty;
import ch.epfl.gsn.metadata.core.model.VirtualSensorMetadata;
import ch.epfl.gsn.metadata.core.repositories.VirtualSensorMetadataRepository;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by kryvych on 16/03/15.
 */
@Named
public class TermsUpdate {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private VirtualSensorMetadataRepository virtualSensorMetadataRepository;
    private TaxonomyResolver taxonomyResolver;

    private Multimap<String, String> missingParameters = HashMultimap.create();

    @Inject
    public TermsUpdate(VirtualSensorMetadataRepository virtualSensorMetadataRepository, TaxonomyResolver taxonomyResolver) {
        this.virtualSensorMetadataRepository = virtualSensorMetadataRepository;
        this.taxonomyResolver = taxonomyResolver;
    }


    public int updateTaxonomyTerms() {
        int count = 0;
        missingParameters.clear();

        Iterable<VirtualSensorMetadata> sensors = virtualSensorMetadataRepository.findAll();
        for (VirtualSensorMetadata sensor : sensors) {
            sensor.clearPropertyNames();
            for (ObservedProperty observedProperty : sensor.getObservedProperties()) {
                String columnName = observedProperty.getColumnName();
                String term = taxonomyResolver.getTermForColumnName(columnName);

                if (StringUtils.isEmpty(term)) {
                    if (!columnName.equalsIgnoreCase("timestamp")) {
                        missingParameters.put(columnName, sensor.getName());
                        logger.info("Missing term for " + sensor.getName() + " : " + columnName);
                    }

                } else if (!term.equalsIgnoreCase("na")){
                    observedProperty.setName(term);
                    sensor.addPropertyName(term);
                }
            }
            virtualSensorMetadataRepository.save(sensor);
            count ++;
        }

        return count;
    }

    public Multimap<String, String> getMissingParameters() {
        return missingParameters;
    }
}
