package ch.epfl.gsn.metadata.tools.mediawiki.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigInteger;

/**
 * Created by kryvych on 26/01/15.
 */

@Document(collection = "virtual_sensors")
public class VirtualSensor {
    @Id
    private BigInteger id;

    private String name;
    private boolean isPublic = true;

    public static VirtualSensorBuilder builder() {
        return new VirtualSensorBuilder();
    }

    protected VirtualSensor(String name, boolean isPublic) {
        this.name = name;
        this.isPublic = isPublic;
    }


    public BigInteger getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public static class VirtualSensorBuilder {
        private String name;
        private boolean isPublic = true;

        public VirtualSensorBuilder name(String name) {
            this.name = name;
            return this;
        }

        public VirtualSensorBuilder isPublic(boolean isPublic) {
            this.isPublic = isPublic;
            return this;
        }

        public VirtualSensor createVirtualSensor() {
            return new VirtualSensor(name, isPublic);
        }
    }
}
