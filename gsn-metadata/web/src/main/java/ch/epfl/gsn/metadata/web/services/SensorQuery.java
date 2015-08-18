package ch.epfl.gsn.metadata.web.services;

import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Point;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

/**
 * Created by kryvych on 30/03/15.
 */
public class SensorQuery {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private String measurementLocationName;
    private Set<String> observedProperties = Sets.newHashSet();
    private String fromDate;
    private String toDate;
    private double minLat = Double.NaN;
    private double minLon = Double.NaN;
    private double maxLat = Double.NaN;
    private double maxLon = Double.NaN;

    private double lat = Double.NaN;
    private double lon = Double.NaN;

    private double altitudeMin = 0;
    private double slopeMin = 0;
    private double aspectMin = 0;

    private double altitudeMax = 0;
    private double slopeMax = 0;
    private double aspectMax = 0;

    private Date fromDateParsed;
    private Date toDateParsed;

    private boolean onlyPublic = true;
    private boolean onlyWithData = true;

    private boolean conjunction = false;


    public boolean isConjunction() {
        return conjunction;
    }

    public void setConjunction(boolean conjunction) {
        this.conjunction = conjunction;
    }

    public void setMinLon(double minLon) {
        this.minLon = minLon;
    }


    public void setMaxLat(double maxLat) {
        this.maxLat = maxLat;
    }

    public void setMinLat(double minLat) {
        this.minLat = minLat;
    }


    public void setMaxLon(double maxLon) {
        this.maxLon = maxLon;
    }

    public double getAspectMin() {
        return aspectMin;
    }

    public void setAspectMin(double aspectMin) {
        this.aspectMin = aspectMin;
    }

    public double getAspectMax() {
        return aspectMax;
    }

    public void setAspectMax(double aspectMax) {
        this.aspectMax = aspectMax;
    }

    public Box getBoundingBox() {
        return new Box(new Point(minLon, minLat), new Point(maxLon, maxLat));
    }

    public Point getLocationPoint() {
        return new Point(lon, lat);
    }

    public String getToDate() {
        return toDate;
    }

    public void setToDate(String toDate) {
        this.toDate = toDate;
    }

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    public Set<String> getObservedProperties() {
        return observedProperties;
    }


    public void setObservedProperties(Set<String> observedProperties) {
        this.observedProperties = observedProperties;
    }


    public boolean hasValidBoundingBox() {
        return !Double.isNaN(minLat) && !Double.isNaN(minLon) && !Double.isNaN(maxLat) && !Double.isNaN(maxLon);
    }


    public boolean hasValidLocation() {
        return !Double.isNaN(lat) && !Double.isNaN(lon);
    }

    public String getMeasurementLocationName() {
        return measurementLocationName;
    }

    public void setMeasurementLocationName(String measurementLocationName) {
        this.measurementLocationName = measurementLocationName;
    }

    public boolean isOnlyPublic() {
        return onlyPublic;
    }

    public void setOnlyPublic(boolean onlyPublic) {
        this.onlyPublic = onlyPublic;
    }

    public boolean isOnlyWithData() {
        return onlyWithData;
    }

    public void setOnlyWithData(boolean onlyWithData) {
        this.onlyWithData = onlyWithData;
    }

    public boolean hasValidFromDate() {
        if (StringUtils.isNotEmpty(fromDate)) {
            try {
                fromDateParsed = DATE_FORMAT.parse(fromDate);
                return true;
            } catch (ParseException e) {
                return false;
            }
        }
        return false;
    }

    public boolean hasValidToDate() {
        if (StringUtils.isNotEmpty(toDate)) {
            try {
                toDateParsed = DATE_FORMAT.parse(toDate);
                return true;
            } catch (ParseException e) {
                return false;
            }
        }
        return false;
    }

    public Date getFromDateParsed() {
        return fromDateParsed;
    }

    public Date getToDateParsed() {
        return toDateParsed;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getAltitudeMin() {
        return altitudeMin;
    }

    public void setAltitudeMin(double altitudeMin) {
        this.altitudeMin = altitudeMin;
    }

    public double getSlopeMin() {
        return slopeMin;
    }

    public void setSlopeMin(double slopeMin) {
        this.slopeMin = slopeMin;
    }

    public double getAltitudeMax() {
        return altitudeMax;
    }

    public void setAltitudeMax(double altitudeMax) {
        this.altitudeMax = altitudeMax;
    }

    public double getSlopeMax() {
        return slopeMax;
    }

    public void setSlopeMax(double slopeMax) {
        this.slopeMax = slopeMax;
    }

    public boolean hasTopoQuery() {
        return (slopeMin + slopeMax + altitudeMax + altitudeMin + aspectMax + aspectMin) >0;
    }

    @Override
    public String toString() {
        return "SensorQuery{" +
                "measurementLocationName='" + measurementLocationName + '\'' +
                ", observedProperties=" + observedProperties +
                ", fromDate='" + fromDate + '\'' +
                ", toDate='" + toDate + '\'' +
                ", minLat=" + minLat +
                ", minLon=" + minLon +
                ", maxLat=" + maxLat +
                ", maxLon=" + maxLon +
                ", lat=" + lat +
                ", lon=" + lon +
                ", altitudeMin=" + altitudeMin +
                ", slopeMin=" + slopeMin +
                ", altitudeMax=" + altitudeMax +
                ", slopeMax=" + slopeMax +
                ", fromDateParsed=" + fromDateParsed +
                ", toDateParsed=" + toDateParsed +
                ", onlyPublic=" + onlyPublic +
                ", onlyWithData=" + onlyWithData +
                '}';
    }
}
