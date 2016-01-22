package ch.epfl.gsn.metadata.tools.gsn;

import ch.epfl.gsn.metadata.core.services.VirtualSensorPersistenceService;
import ch.epfl.gsn.metadata.mongodb.MongoApplicationConfig;
import ch.epfl.gsn.metadata.tools.geoservice.TopoDataUpdate;
import ch.epfl.gsn.metadata.tools.taxonomy.MongoTaxonomyConfig;
import ch.epfl.gsn.metadata.tools.taxonomy.TermsUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by kryvych on 11/03/15.
 */
public class GSNImportTool {

    protected static final Logger logger = LoggerFactory.getLogger(GSNImportTool.class);

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Arguments: sensorsLocation (file or URL) serverURL. Example: file=sensors.json http://montblanc.slf.ch:22001/index.html");
            System.exit(1);
        }

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MongoApplicationConfig.class, MongoTaxonomyConfig.class);
        ctx.scan("ch.epfl.gsn.metadata");
        VirtualSensorPersistenceService gsnUpdateService = ctx.getBean(VirtualSensorPersistenceService.class);
        TermsUpdate termsUpdate = ctx.getBean(TermsUpdate.class);
        TopoDataUpdate topoService = ctx.getBean(TopoDataUpdate.class);



        try (InputStream stream = initInputStream(args[0])) {
            logger.info("====== UPDATING SENSORS =======");
            gsnUpdateService.update(stream, args[1]);
            logger.info("====== UPDATING TAXONOMY MAPPINGS =======");
            termsUpdate.updateTaxonomyTerms();
            logger.info("====== UPDATING TOPO DATA =======");
            topoService.updateGeoData();
        }


    }

    protected static InputStream initInputStream(String parameter) throws IOException {
        String location = parameter.substring(parameter.lastIndexOf('=') + 1).trim();
        if (parameter.startsWith("file=")) {
            try {
                return new FileInputStream(location);
            } catch (FileNotFoundException e) {
                logger.error("ERROR!!!");
                throw new IOException("Cannot find file " + location);
            }
        } else {
            try {
                URL url = new URL(location);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                // give it 15 seconds to respond
                connection.setReadTimeout(15 * 1000);
                connection.connect();

                // read the output from the server
                return connection.getInputStream();
            } catch (IOException e) {
                logger.error(e.getMessage());
                throw e;
            }
        }
    }
}
