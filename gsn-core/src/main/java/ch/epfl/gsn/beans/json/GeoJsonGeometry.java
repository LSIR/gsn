package ch.epfl.gsn.beans.json;

public class GeoJsonGeometry {
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double[] getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(double[] coordinates) {
        this.coordinates = coordinates;
    }

    private String type;
    private double[] coordinates;
}
