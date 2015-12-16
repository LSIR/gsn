package ch.epfl.gsn.oai.utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

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

            logger.info("Reading " + fileName);
            ClassPathResource classPathResource = new ClassPathResource(fileName);
            if (!classPathResource.exists()) {
                logger.error("File doesn't exists " + fileName);
                return StringUtils.EMPTY;
            }

            return IOUtils.toString(classPathResource.getInputStream());

        } catch (IOException e) {
            throw new IOException(e);
        }
    }
}
