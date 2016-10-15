package ch.epfl.gsn.beans.json;


import java.io.Serializable;

public class GeoJsonProperties {
    public String getVs_name() {
        return vs_name;
    }

    public void setVs_name(String vs_name) {
        this.vs_name = vs_name;
    }

    public Serializable[][] getValues() {
        return values;
    }

    public void setValues(Serializable[][] values) {
        this.values = values;
    }

    public GeoJsonField[] getFields() {
        return fields;
    }

    public void setFields(GeoJsonField[] fields) {
        this.fields = fields;
    }

    public GeoJsonStats getStats() {
        return stats;
    }

    public void setStats(GeoJsonStats stats) {
        this.stats = stats;
    }

    public String getGeographical() {
        return geographical;
    }

    public void setGeographical(String geographical) {
        this.geographical = geographical;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    private String vs_name;
    private Serializable[][] values;
    private GeoJsonField[] fields;
    private GeoJsonStats stats;
    private String geographical;
    private String description;
}
