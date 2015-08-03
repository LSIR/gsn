package ch.epfl.gsn.metadata.core.model;

import org.springframework.data.mongodb.core.index.TextIndexed;

/**
 * Created by kryvych on 01/12/14.
 */
public class ObservedProperty {

    @TextIndexed
    private String name = "";
    private String unit;
    private String columnName;
    private String type;

    public ObservedProperty(String name, String unit, String columnName, String type) {
        this.name = name;
        this.unit = unit;
        this.columnName = columnName;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public String getColumnName() {
        return columnName;
    }

    public String getType() {
        return type;
    }

    public void setName(String name) {
        this.name = name;
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
