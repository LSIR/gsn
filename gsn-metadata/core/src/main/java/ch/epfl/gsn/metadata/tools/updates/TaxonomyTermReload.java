package ch.epfl.gsn.metadata.tools.updates;

import ch.epfl.gsn.metadata.mongodb.MongoApplicationConfig;
import ch.epfl.gsn.metadata.tools.taxonomy.MongoTaxonomyConfig;
import ch.epfl.gsn.metadata.tools.taxonomy.TaxonomyLoader;
import ch.epfl.gsn.metadata.tools.taxonomy.TermsUpdate;
import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.FileNotFoundException;

/**
 * Created by kryvych on 17/03/15.
 */
public class TaxonomyTermReload {

    public static void main(String[] args) {

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MongoTaxonomyConfig.class, MongoApplicationConfig.class);
        ctx.scan("ch.epfl.gsn.metadata");
        TermsUpdate service = ctx.getBean(TermsUpdate.class);


        long count = service.updateTaxonomyTerms();
        System.out.println("count = " + count);

        Multimap<String, String> missingParameters = service.getMissingParameters();
        for (String parameter : missingParameters.keySet()) {
            System.out.println(parameter  + " , (" + Joiner.on(" ").join(missingParameters.get(parameter)) + ")");
        }
    }
}
