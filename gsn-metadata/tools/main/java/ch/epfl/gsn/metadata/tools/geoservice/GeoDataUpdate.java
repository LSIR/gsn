package ch.epfl.gsn.metadata.tools.geoservice;

import ch.epfl.gsn.metadata.core.model.GeoData;
import ch.epfl.gsn.metadata.core.model.VirtualSensorMetadata;
import ch.epfl.gsn.metadata.core.repositories.VirtualSensorMetadataRepository;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by kryvych on 25/03/15.
 */
@Named
public class GeoDataUpdate {

    private LocationEnrichmentService locationEnrichmentService;
    private VirtualSensorMetadataRepository repository;

    @Inject
    public GeoDataUpdate(LocationEnrichmentService locationEnrichmentService, VirtualSensorMetadataRepository repository) {
        this.locationEnrichmentService = locationEnrichmentService;
        this.repository = repository;
    }

    public int updateGeoData() {
        int count = 0;
        Iterable<VirtualSensorMetadata> sensors = repository.findAll();
        for (VirtualSensorMetadata sensor : sensors) {
            if (sensor.getLocation() == null) {
                System.out.println("No coordinates for sensor: " + sensor.getName());
                continue;
            }
            GeoData geoData = locationEnrichmentService.fetchGeoData(sensor.getLocation().getX(), sensor.getLocation().getY());
            if (geoData != null) {
                sensor.setGeoData(geoData);
                repository.save(sensor);
                count++;
            } else {
                System.out.println("No extra geo data: " + sensor.getName());
            }
            if ((count % 100) == 0 ) {
                System.out.println("count = " + count);
            }
        }

        return count;
    }
}
