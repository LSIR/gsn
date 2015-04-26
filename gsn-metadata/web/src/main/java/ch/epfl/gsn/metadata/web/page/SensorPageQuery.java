package ch.epfl.gsn.metadata.web.page;

import java.util.Set;

/**
 * Created by kryvych on 02/04/15.
 */
public class SensorPageQuery {
    private Set<String> sensors;

    private Set<String> parameters;

    private String from;

    private String to;

    public Set<String> getSensors() {
        return sensors;
    }

    public void setSensors(Set<String> sensors) {
        this.sensors = sensors;
    }

    public Set<String> getParameters() {
        return parameters;
    }

    public void setParameters(Set<String> parameters) {
        this.parameters = parameters;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }
}
