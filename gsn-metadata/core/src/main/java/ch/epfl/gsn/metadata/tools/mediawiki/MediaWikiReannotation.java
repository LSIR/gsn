package ch.epfl.gsn.metadata.tools.mediawiki;

import ch.epfl.gsn.metadata.core.model.GeoData;
import ch.epfl.gsn.metadata.core.model.RelativePosition;
import ch.epfl.gsn.metadata.core.model.VirtualSensorMetadata;
import ch.epfl.gsn.metadata.core.model.WikiInfo;
import ch.epfl.gsn.metadata.core.repositories.VirtualSensorMetadataRepository;
import ch.epfl.gsn.metadata.tools.mediawiki.model.MeasurementRecord;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

/**
 * Created by kryvych on 24/03/15.
 */
@Named
public class MediaWikiReannotation {

    private VirtualSensorMetadataRepository virtualSensorMetadataRepository;
    private MeasurementRecordRepository measurementRecordRepository;

    @Inject
    public MediaWikiReannotation(VirtualSensorMetadataRepository virtualSensorMetadataRepository, MeasurementRecordRepository measurementRecordRepository) {
        this.virtualSensorMetadataRepository = virtualSensorMetadataRepository;
        this.measurementRecordRepository = measurementRecordRepository;
    }

    public int updateWikiAnnotations() {
        int count =0;
        Iterable<VirtualSensorMetadata> sensors = virtualSensorMetadataRepository.findAll();
        for (VirtualSensorMetadata sensor : sensors) {
            List<MeasurementRecord> measurementRecords = measurementRecordRepository.findByDbTableName(sensor.getName().toLowerCase());
            if (measurementRecords.isEmpty()) {
                System.out.println(sensor.getName());
            }
            for (MeasurementRecord measurementRecord : measurementRecords) {

                MeasurementRecord.RelativePosition relativePosition = measurementRecord.getRelativePosition();
                WikiInfo wikiInfo = new WikiInfo(measurementRecord.getMeasurementLocationName(), measurementRecord.getMeasurementLocation().getDeploymentName(),
                        measurementRecord.getMeasurementLocation().getTitle(), new RelativePosition(relativePosition.getX(), relativePosition.getY(), relativePosition.getZ()));

                sensor.setWikiInfo(wikiInfo);
                sensor.setSamplingFrequency(measurementRecord.getSamplingFrequency());
                virtualSensorMetadataRepository.save(sensor);
                count ++;
                break;
            }
        }
        return count;
    }

}
