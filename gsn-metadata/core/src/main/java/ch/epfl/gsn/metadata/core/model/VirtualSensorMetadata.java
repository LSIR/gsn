package ch.epfl.gsn.metadata.core.model;

import com.google.common.collect.Sets;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Set;

/**
 * Created by kryvych on 09/03/15.
 */
@Document(collection = "virtual_sensor_metadata")
public class VirtualSensorMetadata extends GSNMetadata {

    private GeoData geoData;

    private Set<ObservedProperty> observedProperties  = Sets.newHashSet();

    private WikiInfo wikiInfo;

    private String samplingFrequency;

    public VirtualSensorMetadata(String name, String server, Date fromDate, Date toDate, Point location, boolean isPublic) {
        super(name, server, fromDate, toDate,  location, isPublic);
    }


    public GeoData getGeoData() {
        return geoData;
    }


    public Set<ObservedProperty> getObservedProperties() {
        return observedProperties;
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

    public void setObservedProperties(Set<ObservedProperty> observedProperties) {
        this.observedProperties = observedProperties;
    }

    public void setPropertyNames(Set<String> propertyNames) {
        this.propertyNames = propertyNames;
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
