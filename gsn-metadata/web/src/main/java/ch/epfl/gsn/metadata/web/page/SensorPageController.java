package ch.epfl.gsn.metadata.web.page;

import ch.epfl.gsn.metadata.core.model.VirtualSensorMetadata;
import ch.epfl.gsn.metadata.web.services.QueryBuilder;
import ch.epfl.gsn.metadata.web.services.SensorQuery;
import ch.epfl.gsn.metadata.web.services.VirtualSensorAccessService;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
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
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
* Created by kryvych on 01/04/15.
*/
@Controller
@RequestMapping("/web")
public class SensorPageController {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private VirtualSensorAccessService sensorAccessService;

    private WebJsonConverter geoJsonConverter;
    private QueryBuilder queryBuilder;
    private MetadataService metadataService;

    @Inject
    public SensorPageController(VirtualSensorAccessService sensorAccessService, WebJsonConverter geoJsonConverter, QueryBuilder queryBuilder, MetadataService metadataService) {
        this.sensorAccessService = sensorAccessService;
        this.geoJsonConverter = geoJsonConverter;
        this.queryBuilder = queryBuilder;
        this.metadataService = metadataService;
    }

    @RequestMapping(value = "/virtualSensorNames", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public
    @ResponseBody
    String getVirtualSensorNames(SensorQuery sensorQuery, HttpServletResponse response) {

        setResponseHeader(response);

        sensorQuery.setOnlyPublic(false);
        Query query = queryBuilder.build(sensorQuery);

        Iterable<VirtualSensorMetadata> virtualSensorMetadatas = sensorAccessService.findForQuery(query);

        Set<VirtualSensorMetadata> sensorMetadataSet = Sets.newHashSet(virtualSensorMetadatas);
        Collection<String> names = Collections2.transform(sensorMetadataSet, new Function<VirtualSensorMetadata, String>() {
            @Override
            public String apply(VirtualSensorMetadata virtualSensorMetadata) {
                return virtualSensorMetadata.getName();
            }
        });

        logger.info("query: " + sensorQuery + " results " + sensorMetadataSet.size());
        ArrayList result = new ArrayList(names);
        Collections.sort(result);
        return new Gson().toJson(result);

    }

    @RequestMapping(value = "/virtualSensorNamesWithPrivacy", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public
    @ResponseBody
    String getVirtualSensorNamesWithPrivacy(SensorQuery sensorQuery, HttpServletResponse response) {

        setResponseHeader(response);
        sensorQuery.setOnlyPublic(false);

        Query query = queryBuilder.build(sensorQuery);

        Iterable<VirtualSensorMetadata> virtualSensorMetadatas = sensorAccessService.findForQuery(query);

        Set<VirtualSensorMetadata> sensorMetadataSet = Sets.newHashSet(virtualSensorMetadatas);

        Collection<NameWithProperty<Boolean>> names = Collections2.transform(sensorMetadataSet, new Function<VirtualSensorMetadata, NameWithProperty<Boolean>>() {
            @Override
            public NameWithProperty<Boolean> apply(VirtualSensorMetadata virtualSensorMetadata) {
                return new NameWithProperty(virtualSensorMetadata.getName(), virtualSensorMetadata.isPublic());
            }
        });

        logger.info("query: " + sensorQuery + " results " + sensorMetadataSet.size());
        ArrayList result = new ArrayList(names);
        Collections.sort(result);
        return new Gson().toJson(result);

    }

    @RequestMapping(value = "/virtualSensors/{dbTableName}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public
    @ResponseBody
    String getSensorWithTableModel(@PathVariable String dbTableName, SensorQuery sensorQuery, HttpServletResponse response) {

        VirtualSensorMetadata virtualSensorMetadata = sensorAccessService.getVirtualSensorMetadata(dbTableName.toLowerCase());

        setResponseHeader(response);

        return geoJsonConverter.convertMeasurementRecords(Lists.newArrayList(virtualSensorMetadata), false);
    }

    @RequestMapping(value = "/virtualSensors", method = RequestMethod.GET, produces = "application/json")
    public
    @ResponseBody
    String getVirtualSensors(SensorQuery sensorQuery, HttpServletResponse response) {

        setResponseHeader(response);

        Query query = queryBuilder.build(sensorQuery);

        Iterable<VirtualSensorMetadata> virtualSensorMetadatas = sensorAccessService.findForQuery(query);
//        Iterable<VirtualSensorMetadata> virtualSensorMetadatas = sensorAccessService.allSensors();

        Set<VirtualSensorMetadata> sensorMetadataSet = Sets.newHashSet(virtualSensorMetadatas);
        logger.info("query: " + sensorQuery + " results " + sensorMetadataSet.size());
        return geoJsonConverter.convertMeasurementRecords(sensorMetadataSet, false);

    }
//    @RequestMapping(value = "/allSensorsTable", method = RequestMethod.GET, produces = "application/json")
//    public
//    @ResponseBody
//    String getTableModelForAllSensors(SensorQuery sensorQuery, HttpServletResponse response) {
//        System.out.println("sensorQuery = " + sensorQuery);
//
//
//        Query query = queryBuilder.build(sensorQuery);
//        Iterable<VirtualSensorMetadata> virtualSensorMetadata = sensorAccessService.findForQuery(query);
//
//        setResponseHeader(response);
//
//        return geoJsonConverter.writeTableModel(Lists.newArrayList(virtualSensorMetadata));
//    }

    @RequestMapping(value = "/metadata/{dbTableName}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public
    @ResponseBody
    String getMetadata(@PathVariable String dbTableName, HttpServletResponse response) {

        setResponseHeader(response);

        return metadataService.getMetadataJson(dbTableName);
    }

    private void setResponseHeader(HttpServletResponse response) {
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET");
        response.addHeader("Access-Control-Allow-Headers", "Content-Type");
    }

    protected static class NameWithProperty <T> implements Comparable<NameWithProperty>{
        private String name;
        private T property;

        public NameWithProperty(String name, T property) {
            this.name = name;
            this.property = property;
        }

        public T getProperty() {
            return property;
        }

        public String getName() {
            return name;
        }

        @Override
        public int compareTo(NameWithProperty nameWithProperty) {
            return this.name.compareTo(nameWithProperty.name);
        }
    }

}
