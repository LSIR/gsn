package ch.epfl.gsn.metadata.core.model;

/**
 * Created by kryvych on 10/03/15.
 */
public class Sensor {

    private String serialNumber;

    public Sensor(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getSerialNumber() {
        return serialNumber;
    }
}
