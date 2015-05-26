package ch.epfl.gsn.metadata.web.rest;

import ch.epfl.gsn.metadata.core.model.ObservedProperty;
import ch.epfl.gsn.metadata.core.model.VirtualSensorMetadata;
import ch.epfl.gsn.metadata.tools.taxonomy.TaxonomyResolver;
import ch.epfl.gsn.metadata.web.services.UIJsonConverter;
import ch.epfl.gsn.metadata.web.services.VirtualSensorAccessService;
import com.google.common.base.Joiner;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * Created by kryvych on 14/04/15.
 */
@Controller
@RequestMapping("/taxonomy")
public class TaxonomyController {

    private final TaxonomyResolver taxonomyResolver;
    private final VirtualSensorAccessService virtualSensorAccessService;

    private final UIJsonConverter uiJsonConverter;

    @Inject
    public TaxonomyController(TaxonomyResolver taxonomyResolver, VirtualSensorAccessService virtualSensorAccessService, UIJsonConverter uiJsonConverter) {
        this.taxonomyResolver = taxonomyResolver;
        this.virtualSensorAccessService = virtualSensorAccessService;
        this.uiJsonConverter = uiJsonConverter;
    }

    @RequestMapping(value = "/columnData", method = RequestMethod.GET)
    public
    @ResponseBody
    String getColumnName(@RequestParam String sensorName, @RequestParam Set<String> columnNames, HttpServletResponse response) {

        VirtualSensorMetadata sensorMetadata = virtualSensorAccessService.getVirtualSensorMetadata(sensorName);

        Map<String, ObservedProperty> propertyMap = new HashMap<>();

        for (ObservedProperty observedProperty : sensorMetadata.getObservedProperties()) {
            if (columnNames.contains(observedProperty.getColumnName())) {
                propertyMap.put(observedProperty.getColumnName(), observedProperty);

            }
        }

        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET");
        response.addHeader("Access-Control-Allow-Headers", "Content-Type");


        return uiJsonConverter.convertObservedProperties(propertyMap);
    }

    @RequestMapping(value = "/columnNames", method = RequestMethod.GET)
    public
    @ResponseBody
    String getColumnName(@RequestParam String property) {
        Collection<String> names = taxonomyResolver.getColumnNamesForTerm(property);
        return Joiner.on(", ").join(names);

    }

    @RequestMapping(value = "/taxonomyName", method = RequestMethod.GET)
    public
    @ResponseBody
    String getTaxonomyName(@RequestParam String columnName) {
        String name = taxonomyResolver.getTermForColumnName(columnName);
        return name;
    }


    @RequestMapping(value = "/terms", method = RequestMethod.GET)
    public
    @ResponseBody
    String getTaxonomyName() {
        Collection<String> terms = taxonomyResolver.getAllTerms();
        return Joiner.on(", ").join(terms);
    }



}
