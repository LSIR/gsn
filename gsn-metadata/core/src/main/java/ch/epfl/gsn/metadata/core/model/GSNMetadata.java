package ch.epfl.gsn.metadata.core.model;

import com.google.common.collect.Sets;
import org.springframework.data.annotation.Id;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;
import java.util.Set;

/**
 * Created by kryvych on 11/03/15.
 */
public abstract class GSNMetadata implements Serializable{
    @Id
    private BigInteger id;

    protected String name;

    @GeoSpatialIndexed
    protected Point location;
    protected Date fromDate;
    protected Date toDate;
    protected String server;
    protected Set<String> propertyNames = Sets.newHashSet();
    protected boolean isPublic = false;

    protected String description;

    public GSNMetadata(String name, String server, Date fromDate, Date toDate, Point location, boolean isPublic) {
        this.toDate = toDate;
        this.server = server;
        this.fromDate = fromDate;
        this.location = location;
        this.isPublic = isPublic;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Point getLocation() {
        return location;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public String getServer() {
        return server;
    }

    public Set<String> getPropertyNames() {
        return propertyNames;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public abstract boolean isGrid();

    public void addPropertyName(String propertyName) {
        propertyNames.add(propertyName);
    }

    public void clearPropertyNames() {
        propertyNames.clear();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "GSNMetadata{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", location=" + location +
                ", fromDate=" + fromDate +
                ", toDate=" + toDate +
                ", server='" + server + '\'' +
                ", propertyNames=" + propertyNames +
                ", isPublic=" + isPublic +
                ", description='" + description + '\'' +
                '}';
    }
}
