package ch.epfl.gsn.oai;


import com.google.common.base.Splitter;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Properties;

/**
 * Created by kryvych on 29/09/15.
 */
@Named
public class OaiIdentifierBuilder {

    private final Properties osperConfiguration;

    @Inject
    public OaiIdentifierBuilder(Properties osperConfiguration) {
        this.osperConfiguration = osperConfiguration;
    }

    public String buildId(String sensorName) {
        return osperConfiguration.get("id.prefix") + sensorName;
    }

    public String extractSensorName(String oaiID) {
        List<String> strings = Splitter.on("/").splitToList(oaiID);
        return strings.get(strings.size() - 1);
    }
}
