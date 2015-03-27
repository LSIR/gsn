package ch.epfl.gsn.metadata.web.rest;

import ch.epfl.gsn.metadata.core.model.VirtualSensorMetadata;
import ch.epfl.gsn.metadata.web.services.GeoJsonConverter;
import ch.epfl.gsn.metadata.web.services.VirtualSensorAccessService;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;
import java.util.Set;

/**
 * Created by kryvych on 26/03/15.
 */
@Controller
@RequestMapping("/metadata")
public class MetadataController {

    private VirtualSensorAccessService sensorAccessService;

    private GeoJsonConverter geoJsonConverter;

    @Inject
    public MetadataController(VirtualSensorAccessService sensorAccessService, GeoJsonConverter geoJsonConverter) {
        this.sensorAccessService = sensorAccessService;
        this.geoJsonConverter = geoJsonConverter;
    }

    @RequestMapping(value = "/virtualSensors", method = RequestMethod.GET, produces = "application/json")
    public
    @ResponseBody
    String getVirtualSensors() {
        Iterable<VirtualSensorMetadata> virtualSensorMetadatas = sensorAccessService.allSensors();
        Set<VirtualSensorMetadata> sensorMetadataSet = Sets.newHashSet(virtualSensorMetadatas);
        return geoJsonConverter.convertMeasurementRecords(sensorMetadataSet, false);

    }

//    @RequestMapping(value = "/measurementRecords/unfolded", method = RequestMethod.GET, produces = "application/json")
//    public
//    @ResponseBody
//    String getMeasurementRecordsUnfolded(MetaDataQuery query) {
//        System.out.println("query = " + query);
//
//        Collection<MeasurementRecord> allMeasurementRecords = queryService.findMeasurementRecords(query, true);
//
//        return converter.convertMeasurementRecords(allMeasurementRecords, false);
//    }
//
//    @RequestMapping(value = "/measurementRecords/measurementLocation/{measurementLocationName}", method = RequestMethod.GET, produces = "application/json")
//    public
//    @ResponseBody
//    String getMeasurementRecordsForMeasurementLocation(@PathVariable String measurementLocationName) {
//        System.out.println("measurementLocationName = " + measurementLocationName);
//        List<MeasurementRecord> allMeasurementRecords = queryService.findMeasurementRecordsByMeasurementLocation(measurementLocationName);
//
//        return converter.convertMeasurementRecords(allMeasurementRecords, false);
//    }
//
//    @RequestMapping(value = "/measurementRecords/{dbTableName}", method = RequestMethod.GET, produces = "application/json")
//    public
//    @ResponseBody
//    String getMeasurementRecordsForDbTableName(@PathVariable String dbTableName) {
//        System.out.println("dbTableName = " + dbTableName);
//        Collection<MeasurementRecord> allMeasurementRecords = queryService.findMeasurementRecordsForVirtualSensor(dbTableName);
//
//        return converter.convertMeasurementRecords(allMeasurementRecords, true);
//    }
//
//    @RequestMapping(value = "/measurementLocations", method = RequestMethod.GET, produces = "application/json")
//    public
//    @ResponseBody
//    String getMeasurementLocations(MetaDataQuery query) {
//
//        System.out.println("query = " + query);
//
//        List<MeasurementLocation> result = queryService.findAllMeasurementLocations();
//        Gson gson = new Gson();
//        return gson.toJson(result);
//
//    }
//
//    @RequestMapping(value = "measurementRecords/position", method = RequestMethod.GET, produces = "application/json")
//    public
//    @ResponseBody
//    String getMeasurementRecordsForLocation(@RequestParam String lat, @RequestParam String lon) {
//
//        Collection<MeasurementRecord> result = queryService.findMeasurementRecordByLocation(lat, lon);
//
//        return converter.convertMeasurementRecords(result, false);
//
//    }
}

