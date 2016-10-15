package ch.epfl.gsn.beans.json;

public class GeoJsonFeature {

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public GeoJsonProperties getProperties() {
        return properties;
    }

    public void setProperties(GeoJsonProperties properties) {
        this.properties = properties;
    }

    public GeoJsonGeometry getGeometry() {
        return geometry;
    }

    public void setGeometry(GeoJsonGeometry geometry) {
        this.geometry = geometry;
    }

    public int getTotal_size() {
        return total_size;
    }

    public void setTotal_size(int total_size) {
        this.total_size = total_size;
    }

    public int getPage_size() {
        return page_size;
    }

    public void setPage_size(int page_size) {
        this.page_size = page_size;
    }

    private String type;
    private GeoJsonProperties properties;
    private GeoJsonGeometry geometry;
    private int total_size;
    private int page_size;

}
