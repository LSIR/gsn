package ch.epfl.gsn.metadata.tools.mediawiki.model;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

/**
 * Created by kryvych on 24/11/14.
 */
public class Deployment extends MapLocation {


    private DeploymentPeriod dates;

    private Collection<MeasurementLocation> measurementLocations = new HashSet<MeasurementLocation>();


    public Deployment(String id, String title, Coordinate coordinates, Date from, Date to, Collection<MeasurementLocation> measurementLocations) {
        super(id, title, coordinates);
        this.dates = new DeploymentPeriod(from, to);
        this.measurementLocations = measurementLocations;
    }

    public Deployment(String id, String title, Coordinate coordinate, Date from, Date to) {
        super(id, title, coordinate);
        this.dates = new DeploymentPeriod(from, to);
    }

    public Date getFrom() {
        return dates.getFrom();
    }

    public Date getTo() {
        return dates.getTo();
    }

    public Collection<MeasurementLocation> getMeasurementLocations() {
        return measurementLocations;
    }

}
