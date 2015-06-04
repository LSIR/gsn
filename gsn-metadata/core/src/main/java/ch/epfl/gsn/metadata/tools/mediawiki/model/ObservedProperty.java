package ch.epfl.gsn.metadata.tools.mediawiki.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigInteger;

/**
 * Created by kryvych on 01/12/14.
 */
public class ObservedProperty {

    @TextIndexed
    private String name;
    private String media;
    private String unit;
    private String columnName;

    public  ObservedProperty(String name, String media, String unit, String columnName) {
        this.name = name;
        this.media = media;
        this.unit = unit;
        this.columnName = columnName;
    }

    public String getName() {
        return name;
    }

    public String getMedia() {
        return media;
    }

    public String getUnit() {
        return unit;
    }

    public String getColumnName() {
        return columnName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ObservedProperty that = (ObservedProperty) o;

        if (columnName != null ? !columnName.equals(that.columnName) : that.columnName != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (columnName != null ? columnName.hashCode() : 0);
        return result;
    }
}
