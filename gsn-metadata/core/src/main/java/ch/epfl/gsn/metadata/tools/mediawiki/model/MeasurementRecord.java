package ch.epfl.gsn.metadata.tools.mediawiki.model;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.springframework.data.annotation.Id;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;

/**
 * Created by kryvych on 01/12/14.
 */
@Document(collection = "measurement_records")
public class MeasurementRecord {
    @Id
    private BigInteger id;

    private String wikiId;

    private String title;
    private String measurementLocationName;

    @DBRef
    private MeasurementLocation measurementLocation;

    @GeoSpatialIndexed
    private Point locationPoint;

    private String serialNumber;

    private Date fromDate;
    private Date toDate;
    private String samplingFrequency;

    private String server;
    private String organisation;
    private String email;

    private Collection<ObservedProperty> observedProperties;

    private String dbTableName;

    private RelativePosition relativePosition;

    private boolean isPublic = true;

    private boolean inGSN = false;


    private MeasurementRecord(String wikiId, String title, String measurementLocationName, String serialNumber, Date fromDate, Date toDate, String samplingFrequency, String server, String organisation, String email, Collection<ObservedProperty> observedProperties, String dbTableName, RelativePosition relativePosition, boolean isPublic) {
        this.wikiId = wikiId;
        this.title = title;
        this.measurementLocationName = measurementLocationName;
        this.serialNumber = serialNumber;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.samplingFrequency = samplingFrequency;
        this.server = server;
        this.organisation = organisation;
        this.email = email;
        this.observedProperties = observedProperties;
        this.dbTableName = dbTableName;
        this.relativePosition = relativePosition;
    }

    protected MeasurementRecord() {
    }

    public static Builder getBuilder() {
        return new Builder();
    }

    public BigInteger getId() {
        return id;
    }

    public String getWikiId() {
        return wikiId;
    }

    public String getMeasurementLocationName() {
        return measurementLocationName;
    }

    public MeasurementLocation getMeasurementLocation() {
        return measurementLocation;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getSamplingFrequency() {
        return samplingFrequency;
    }

    public String getServer() {
        return server;
    }

    public String getOrganisation() {
        return organisation;
    }

    public Collection<ObservedProperty> getObservedProperties() {
        return observedProperties;
    }

    public String getDbTableName() {
        return dbTableName;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public String getTitle() {
        return title;
    }

    public Point getLocationPoint() {
        return locationPoint;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }

    public void setMeasurementLocation(MeasurementLocation measurementLocation) {
        this.measurementLocation = measurementLocation;
        this.locationPoint = measurementLocation.getLocationPoint();
    }

    public RelativePosition getRelativePosition() {
        return relativePosition;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public boolean isInGSN() {
        return inGSN;
    }

    public void setInGSN(boolean inGSN) {
        this.inGSN = inGSN;
    }

    public Collection<String> getObservedPropertyNames() {
        return Sets.newHashSet(Collections2.transform(this.observedProperties, new Function<ObservedProperty, String>() {
            @Override
            public String apply(ObservedProperty observedProperty) {

                String media = (observedProperty.getMedia().equals("-") || observedProperty.getMedia().equals("NA")) ?
                        "undefined" : observedProperty.getMedia();
                return media + " : " + observedProperty.getName();
            }
        }));
    }

    @Override
    public String toString() {
        return "MeasurementRecord{" +
                "id=" + id +
                ", wikiId='" + wikiId + '\'' +
                ", title='" + title + '\'' +
                ", measurementLocationName='" + measurementLocationName + '\'' +
                ", measurementLocation=" + measurementLocation +
                ", serialNumber='" + serialNumber + '\'' +
                ", fromDate=" + fromDate +
                ", toDate=" + toDate +
                ", samplingFrequency='" + samplingFrequency + '\'' +
                ", server='" + server + '\'' +
                ", organisation='" + organisation + '\'' +
                ", observedProperties=" + observedProperties +
                ", dbTableName='" + dbTableName + '\'' +
                '}';
    }

    public String getEmail() {
        return email;
    }

    public static class Builder {
        private String wikiId;
        private String measurementLocationName;
        private String serialNumber;
        private String samplingFrequency;
        private String server;
        private String organisation;
        private Collection<ObservedProperty> observedProperties;
        private String dbTableName;
        private Date fromDate;
        private Date toDate;
        private String title;
        private String email;
        private RelativePosition relativePosition;
        private boolean isPublic;

        public Builder wikiId(String wikiId) {
            this.wikiId = wikiId;
            return this;
        }

        public Builder measurementLocatioName(String measurementLocationId) {
            this.measurementLocationName = measurementLocationId;
            return this;
        }

        public Builder serialNumber(String serialNumber) {
            this.serialNumber = serialNumber;
            return this;
        }

        public Builder samplingFrequency(String samplingFrequency) {
            this.samplingFrequency = samplingFrequency;
            return this;
        }

        public Builder server(String server) {
            this.server = server;
            return this;
        }

        public Builder organisation(String organisation) {
            this.organisation = organisation;
            return this;
        }

        public Builder observedProperties(Collection<ObservedProperty> observedProperties) {
            this.observedProperties = observedProperties;
            return this;
        }

        public Builder dbTableName(String dbTableName) {
            this.dbTableName = dbTableName;
            return this;
        }

        public Builder fromDate(Date fromDate) {
            this.fromDate = fromDate;
            return this;
        }

        public Builder toDate(Date toDate) {
            this.toDate = toDate;
            return this;
        }

        public Builder relativePosition(String x, String y, String z) {
            this.relativePosition = new RelativePosition(x, y, z);
            return this;
        }

        public MeasurementRecord createMeasurementRecord() {
            return new MeasurementRecord(wikiId, title, measurementLocationName, serialNumber, fromDate, toDate, samplingFrequency, server, organisation, email, observedProperties, dbTableName, relativePosition, isPublic);
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder isPublic(boolean isPublic) {
            this.isPublic = isPublic;
            return this;
        }
    }

    public static class RelativePosition {
        private String x;
        private String y;
        private String z;

        public RelativePosition(String x, String y, String z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public String getPosition() {
            if (x != null && y != null && z != null) {
                return Joiner.on(",").join(Lists.newArrayList(x, y, z));
            }
            return "";
        }

        public String getX() {
            return x;
        }

        public String getY() {
            return y;
        }

        public String getZ() {
            return z;
        }
    }


}
