package gsn.utils;

import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Utils {

    private static final transient Logger logger = Logger.getLogger(Utils.class);

    public static Properties loadProperties(String path) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(path));
        } catch (IOException e) {
            logger.warn("Unable to load the property file: " + path);
            return null;
        }
        return properties;
    }

}
