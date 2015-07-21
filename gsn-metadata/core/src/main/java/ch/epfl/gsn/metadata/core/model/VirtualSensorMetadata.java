package ch.epfl.gsn.metadata.core.model;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.annotation.Transient;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;

/**
 * Created by kryvych on 09/03/15.
 */
@Document(collection = "virtual_sensor_metadata")
public class VirtualSensorMetadata extends GSNMetadata {

    private GeoData geoData;

    private Set<ObservedProperty> observedProperties  = Sets.newHashSet();

    private WikiInfo wikiInfo;

    private String samplingFrequency;

    @Transient
    private ArrayList<ObservedProperty> sortedProperties;

    public VirtualSensorMetadata(String name, String server, Date fromDate, Date toDate, Point location, boolean isPublic) {
        super(name, server, fromDate, toDate,  location, isPublic);
    }


    public GeoData getGeoData() {
        return geoData;
    }


    public Set<ObservedProperty> getObservedProperties() {
        return observedProperties;
    }

    public List<ObservedProperty> getSortedProperties() {
        if (sortedProperties != null) {
            return sortedProperties;
        }

        sortedProperties = Lists.newArrayList(Collections2.filter(getObservedProperties(), new Predicate<ObservedProperty>() {
            @Override
            public boolean apply(ObservedProperty observedProperty) {
                return StringUtils.isNotEmpty(observedProperty.getName());
            }
        }));

        Collections.sort(sortedProperties, new Comparator<ObservedProperty>() {
            @Override
            public int compare(ObservedProperty o1, ObservedProperty o2) {
                if (o1.getName() != null && o2.getName() != null) {
                    return o1.getName().compareTo(o2.getName());
                } else {
                    return o1.getName() != null? 1:-1;
                }
            }
        });

        return sortedProperties;
    }

    public WikiInfo getWikiInfo() {
        return wikiInfo;
    }

    public String getSamplingFrequency() {
        return samplingFrequency;
    }

    @Override
    public boolean isGrid() {
        return false;
    }

    public void setGeoData(GeoData geoData) {
        this.geoData = geoData;
    }


    public void setWikiInfo(WikiInfo wikiInfo) {
        this.wikiInfo = wikiInfo;
    }


    public void setSamplingFrequency(String samplingFrequency) {
        this.samplingFrequency = samplingFrequency;
    }

    public boolean addObservedProperty(ObservedProperty observedProperty) {
        return observedProperties.add(observedProperty);
    }
}
