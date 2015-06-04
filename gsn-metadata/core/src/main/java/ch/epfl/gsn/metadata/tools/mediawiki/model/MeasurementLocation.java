package ch.epfl.gsn.metadata.tools.mediawiki.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Created by kryvych on 01/12/14.
 */
@Document(collection = "measurement_location")
public class MeasurementLocation {

    @Id
//    private BigInteger id;

    private ObjectId id;

    private String wikiId;
    private String title;

    private String locationName;

    private String deploymentName;

    private double[] location;

    @GeoSpatialIndexed
    private Point locationPoint;
    private double elevation;
    private double slope;

    private double aspect;

    private MeasurementLocation() {
    }

    private MeasurementLocation(String wikiId, String title, String locationName, String deploymentName, double[] location) {
        this.wikiId = wikiId;
        this.title = title;
        this.locationName = locationName;
        this.deploymentName = deploymentName;

        this.location = location;
        this.locationPoint = new Point(location[0], location[1]);
    }

    public static Builder getBuilder() {
        return new Builder();
    }

    public ObjectId getId() {
        return id;
    }

    public String getWikiId() {
        return wikiId;
    }

    public String getTitle() {
        return title;
    }

    public String getLocationName() {
        return locationName;
    }

    public String getDeploymentName() {
        return deploymentName;
    }


    public double[] getLocation() {
        return location;
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

    public void setElevation(double elevation) {
        this.elevation = elevation;
    }

    public void setSlope(double slope) {
        this.slope = slope;
    }

    public void setAspect(double aspect) {
        this.aspect = aspect;
    }

    public Point getLocationPoint() {
        return locationPoint;
    }

    public Coordinate getCoordinate() {
        return new Coordinate(location[0], location[1]);
    }

    @Override
    public String toString() {
        return "MeasurementLocation{" +
                "id=" + id +
                ", wikiId='" + wikiId + '\'' +
                ", title='" + title + '\'' +
                ", locationName='" + locationName + '\'' +
                ", deploymentName='" + deploymentName + '\'' +
                ", location=" + Arrays.toString(location) +
                ", locationPoint=" + locationPoint +
                ", elevation=" + elevation +
                ", slope=" + slope +
                ", aspect=" + aspect +
                '}';
    }

    public static  class Builder {
        private String wikiId;
        private String title;
        private String locationName;
        private String deploymentName;
        private double[] location;


        public Builder wikiId(String wikiId) {
            this.wikiId = wikiId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder locationName(String locationName) {
            this.locationName = locationName;
            return this;
        }

        public Builder deploymentName(String deploymentName) {
            this.deploymentName = deploymentName;
            return this;
        }

        public Builder location(double[] location)  {
            this.location = location;
            return this;
        }



        public MeasurementLocation createMeasurementLocation() {
            return new MeasurementLocation( wikiId, title, locationName, deploymentName, location);
        }
    }
}
