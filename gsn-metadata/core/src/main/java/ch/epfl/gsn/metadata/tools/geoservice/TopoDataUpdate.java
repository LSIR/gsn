package ch.epfl.gsn.metadata.tools.geoservice;

import ch.epfl.gsn.metadata.core.model.GeoData;
import ch.epfl.gsn.metadata.core.model.VirtualSensorMetadata;
import ch.epfl.gsn.metadata.core.repositories.VirtualSensorMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by kryvych on 25/03/15.
 */
@Named
public class TopoDataUpdate {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private LocationEnrichmentService locationEnrichmentService;
    private VirtualSensorMetadataRepository repository;

    @Inject
    public TopoDataUpdate(LocationEnrichmentService locationEnrichmentService, VirtualSensorMetadataRepository repository) {
        this.locationEnrichmentService = locationEnrichmentService;
        this.repository = repository;
    }

    public int updateGeoData() {
        int count = 0;
        Iterable<VirtualSensorMetadata> sensors = repository.findAll();
        for (VirtualSensorMetadata sensor : sensors) {
            if (sensor.getLocation() == null) {
                logger.info("No coordinates for sensor: " + sensor.getName());
                continue;
            }

            if (sensor.getGeoData() != null) {
                continue;
            }

            GeoData geoData = locationEnrichmentService.fetchGeoData(sensor.getLocation().getX(), sensor.getLocation().getY());
            if (geoData != null) {
                sensor.setGeoData(geoData);
                repository.save(sensor);
                count++;
            } else {
                logger.info("No extra geo data: " + sensor.getName());
            }
            if ((count % 100) == 0 ) {
                logger.info("topo progress - updated " + count);
            }
        }

        return count;
    }
}
