package ch.epfl.gsn.metadata.tools.taxonomy;

import ch.epfl.gsn.metadata.mongodb.MongoApplicationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.FileNotFoundException;

/**
 * Created by kryvych on 16/03/15.
 */
public class TaxonomyImportTool {

    static protected final Logger logger = LoggerFactory.getLogger(TaxonomyImportTool.class);

    public static void main(String[] args) throws FileNotFoundException {

        if (args.length != 1) {
            System.out.println("Arguments: taxonomyFileLocation");
            System.exit(1);
        }

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MongoTaxonomyConfig.class);
        ctx.scan("ch.epfl.gsn.metadata");
        TaxonomyLoader service = ctx.getBean(TaxonomyLoader.class);


        long count = service.loadTaxonomy(args[0]);
        logger.info("loaded column mappings : " + count);
    }

}
