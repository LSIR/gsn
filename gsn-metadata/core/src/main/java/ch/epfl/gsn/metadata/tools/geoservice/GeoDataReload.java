package ch.epfl.gsn.metadata.tools.geoservice;

import ch.epfl.gsn.metadata.mongodb.MongoApplicationConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Created by kryvych on 17/03/15.
 */
public class GeoDataReload {

    public static void main(String[] args) {

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MongoApplicationConfig.class);
        ctx.scan("ch.epfl.gsn.metadata");
        TopoDataUpdate service = ctx.getBean(TopoDataUpdate.class);


        long count = service.updateGeoData();
        System.out.println("count = " + count);
    }
}
