package ch.epfl.gsn.oai.load;

import ch.epfl.gsn.oai.OaiConfigurationImpl;
import ch.epfl.gsn.oai.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by kryvych on 28/09/15.
 */
public class RecordLoader {

    protected static final Logger logger = LoggerFactory.getLogger(RecordLoader.class);

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Arguments: directory with files for OAI service");
            System.exit(1);
        }

        Path metadataDir = Paths.get(args[0]);
        if (Files.notExists(metadataDir)) {
            throw new IOException("Directory " + args[0] + " doesn't exist");
        }

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(OaiConfigurationImpl.class);
        ctx.scan("ch.epfl.gsn.metadata");
        RecordLoadService recordLoadService = ctx.getBean(RecordLoadService.class);


        logger.info("====== LOADING RECORDS =======");
        int count = recordLoadService.write(metadataDir);
        logger.info("Found " + count + " metadata files.");


    }

}

