package ch.epfl.gsn.metadata.core.model;

/**
 * Created by kryvych on 10/03/15.
 */
public class GeoData {

    private double elevation;
    private double slope;
    private double aspect;

    public GeoData(double elevation, double slope, double aspect) {
        this.elevation = elevation;
        this.slope = slope;
        this.aspect = aspect;
    }

    public double getElevation() {
        return elevation;
    }

    public double getSlope() {
        return slope;
    }

    public double getAspect() {
        return aspect;
    }
}
