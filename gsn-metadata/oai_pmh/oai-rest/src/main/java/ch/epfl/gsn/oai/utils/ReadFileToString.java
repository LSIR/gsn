package ch.epfl.gsn.oai.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by kryvych on 07/09/15.
 */

public class ReadFileToString {

    protected static final Logger logger = LoggerFactory.getLogger(ReadFileToString.class);

    public static String readFileFromClasspath(final String fileName) throws IOException {
        try {
            URL resource = ReadFileToString.class.getClassLoader()
                    .getResource(fileName);

            if (resource == null) {
                logger.error("File doesn't exists " + fileName);
                return StringUtils.EMPTY;
            }

            URI uri = resource.toURI();
            return new String(Files.readAllBytes(
                    Paths.get(uri)));
        } catch (IOException | URISyntaxException e) {
            throw new IOException(e);
        }
    }
}
