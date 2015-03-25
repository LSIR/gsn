package ch.epfl.gsn.metadata.tools.taxonomy;

import ch.epfl.gsn.metadata.core.model.GeoData;
import ch.epfl.gsn.metadata.core.model.ObservedProperty;
import ch.epfl.gsn.metadata.core.model.VirtualSensorMetadata;
import ch.epfl.gsn.metadata.core.repositories.VirtualSensorMetadataRepository;
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

    @Inject
    public TermsUpdate(VirtualSensorMetadataRepository virtualSensorMetadataRepository, TaxonomyResolver taxonomyResolver) {
        this.virtualSensorMetadataRepository = virtualSensorMetadataRepository;
        this.taxonomyResolver = taxonomyResolver;
    }

    public int updateTaxonomyTerms() {
        int count = 0;
        Iterable<VirtualSensorMetadata> sensors = virtualSensorMetadataRepository.findAll();
        for (VirtualSensorMetadata sensor : sensors) {
            sensor.clearPropertyNames();
            for (ObservedProperty observedProperty : sensor.getObservedProperties()) {
                String term = taxonomyResolver.getTermForColumnName(observedProperty.getColumnName());
                if (StringUtils.isEmpty(term)) {
                    logger.info("Missing term for " + sensor.getName() + " : " + observedProperty.getColumnName());
                    System.out.println(sensor.getName() + " : " + observedProperty.getColumnName());
                } else {
                    observedProperty.setName(term);
                    sensor.addPropertyName(term);
                }
            }
            sensor.setGeoData(null);
            virtualSensorMetadataRepository.save(sensor);
            count ++;
        }

        return count;
    }

}
