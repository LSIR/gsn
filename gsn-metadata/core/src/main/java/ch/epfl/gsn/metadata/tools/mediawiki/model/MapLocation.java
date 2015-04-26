package ch.epfl.gsn.metadata.tools.mediawiki.model;

import ch.epfl.gsn.metadata.tools.mediawiki.model.Coordinate;
import org.springframework.data.annotation.Id;

import java.math.BigInteger;

/**
 * Created by kryvych on 01/12/14.
 */
public class MapLocation {

    @Id
    private BigInteger id;

    private String wikiId;
    private String title;

    private Coordinate coordinate;

    private double elevation;
    private double slope;
    private String aspect;

    public MapLocation(String wikiId, String title, Coordinate coordinate) {
        this.wikiId = wikiId;
        this.title = title;
        this.coordinate = coordinate;
    }

    public String getWikiId() {
        return wikiId;
    }

    public String getTitle() {
        return title;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public double getElevation() {
        return elevation;
    }

    public String getAspect() {
        return aspect;
    }

    public void setElevation(double elevation) {
        this.elevation = elevation;
    }

    public void setAspect(String aspect) {
        this.aspect = aspect;
    }

    public double getSlope() {
        return slope;
    }

    public void setSlope(double slope) {
        this.slope = slope;
    }

    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }
}
