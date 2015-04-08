package ch.epfl.gsn.metadata.tools.geoservice;

import ch.epfl.gsn.metadata.core.model.GeoData;
import ch.epfl.gsn.metadata.mongodb.MongoApplicationConfig;
import ch.epfl.gsn.metadata.tools.taxonomy.MongoTaxonomyConfig;
import ch.epfl.gsn.metadata.tools.taxonomy.TermsUpdate;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Created by kryvych on 17/03/15.
 */
public class GeoDataReload {

    public static void main(String[] args) {

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ToolsConfiguration.class, MongoApplicationConfig.class);
        ctx.scan("ch.epfl.gsn.metadata");
        GeoDataUpdate service = ctx.getBean(GeoDataUpdate.class);


        long count = service.updateGeoData();
        System.out.println("count = " + count);
    }
}
