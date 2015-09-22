package ch.epfl.gsn.metadata.tools.updates;

import ch.epfl.gsn.metadata.core.services.ExchangeMatadataService;
import ch.epfl.gsn.metadata.mongodb.MongoApplicationConfig;
import ch.epfl.gsn.metadata.tools.taxonomy.MongoTaxonomyConfig;
import ch.epfl.gsn.metadata.tools.taxonomy.TaxonomyLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.FileNotFoundException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by kryvych on 03/09/15.
 */
public class ExchangeMetadataUpdate {

    static protected final Logger logger = LoggerFactory.getLogger(ExchangeMetadataUpdate.class);

    public static void main(String[] args) throws FileNotFoundException {

        if (args.length != 1) {
            System.out.println("Arguments: directory with metadata files");
            System.exit(1);
        }

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MongoApplicationConfig.class);
        ctx.scan("ch.epfl.gsn.metadata");
        ExchangeMatadataService exchangeMatadataService = ctx.getBean(ExchangeMatadataService.class);

        Path directory = Paths.get(args[0]);

        long count = exchangeMatadataService.write(directory);
        logger.info("loaded column mappings : " + count);
    }
}
