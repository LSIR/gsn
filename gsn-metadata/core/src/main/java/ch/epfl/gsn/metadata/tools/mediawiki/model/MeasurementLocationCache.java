package ch.epfl.gsn.metadata.tools.mediawiki.model;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.springframework.context.annotation.Scope;

import javax.inject.Named;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Created by kryvych on 10/12/14.
 */
@Named
@Scope("singleton")
public class MeasurementLocationCache {
//    private Map<String, String> locationToDBtable = Maps.newHashMap();
//
//    private Multimap<String, MeasurementRecord> locationToRecords = ArrayListMultimap.create();

    private Map<String, MeasurementLocation> locationNameToLocation = Maps.newHashMap();

    private Set<String> observedProperties = Sets.newHashSet();

    private Map<String, ObservedProperty> nameToProperty = Maps.newHashMap();


//    public String putDBTableForLocation(String locationName, String dbTableName) {
//        return locationToDBtable.put(locationName, dbTableName);
//    }
//
//    public boolean putMeasurementRecordForLocation(String locationName, MeasurementRecord measurementRecord) {
//        return locationToRecords.put(locationName, measurementRecord);
//    }

    public MeasurementLocation putLocation(MeasurementLocation location) {
        return locationNameToLocation.put(location.getLocationName(), location);
    }

    public MeasurementLocation getLocation(String locationName) {
        return locationNameToLocation.get(locationName);
    }

//    public void addObservedProperty(String observedProperty) {
//        observedProperties.add(observedProperty);
//    }

    public void addObservedProperty(ObservedProperty observedProperty) {
        observedProperties.add(observedProperty.getName());
        nameToProperty.put(observedProperty.getName(), observedProperty);
    }

    public Set<String> getObservedPropertyNames() {
        return Collections.unmodifiableSet(observedProperties);
    }

    public Collection<ObservedProperty> getObservedProperties() {
        return nameToProperty.values();
    }

    public ObservedProperty getObservedProperty(String name) {
        return nameToProperty.get(name);
    }
}
