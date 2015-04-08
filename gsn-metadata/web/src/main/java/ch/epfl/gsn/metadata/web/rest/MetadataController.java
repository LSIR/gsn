package ch.epfl.gsn.metadata.web.rest;

import ch.epfl.gsn.metadata.core.model.VirtualSensorMetadata;
import ch.epfl.gsn.metadata.core.repositories.VirtualSensorMetadataRepository;
import ch.epfl.gsn.metadata.web.services.GeoJsonConverter;
import ch.epfl.gsn.metadata.web.services.QueryBuilder;
import ch.epfl.gsn.metadata.web.services.SensorQuery;
import ch.epfl.gsn.metadata.web.services.VirtualSensorAccessService;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
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

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private VirtualSensorAccessService sensorAccessService;

    private GeoJsonConverter geoJsonConverter;
    private QueryBuilder queryBuilder;

    @Inject
    public MetadataController(VirtualSensorAccessService sensorAccessService, GeoJsonConverter geoJsonConverter, QueryBuilder queryBuilder) {
        this.sensorAccessService = sensorAccessService;
        this.geoJsonConverter = geoJsonConverter;
        this.queryBuilder = queryBuilder;
    }

    @RequestMapping(value = "/virtualSensors", method = RequestMethod.GET, produces = "application/json")
    public
    @ResponseBody
    String getVirtualSensors(SensorQuery sensorQuery) {
        System.out.println("sensorQuery = " + sensorQuery);

        Query query = queryBuilder.build(sensorQuery);

        Iterable<VirtualSensorMetadata> virtualSensorMetadatas = sensorAccessService.findForQuery(query);

        Set<VirtualSensorMetadata> sensorMetadataSet = Sets.newHashSet(virtualSensorMetadatas);
        logger.info("query: " + sensorQuery + " results " + sensorMetadataSet.size());
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
    @RequestMapping(value = "/virtualSensors/{dbTableName}", method = RequestMethod.GET, produces = "application/json")
    public
    @ResponseBody
    String getMeasurementRecordsForDbTableName(@PathVariable String dbTableName) {
        System.out.println("dbTableName = " + dbTableName);

        VirtualSensorMetadata virtualSensorMetadata = sensorAccessService.getVirtualSensorMetadata(dbTableName);

        return geoJsonConverter.convertMeasurementRecords(Lists.newArrayList(virtualSensorMetadata), true);
    }

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

