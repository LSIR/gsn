package ch.epfl.gsn.metadata.tools.updates;

import ch.epfl.gsn.metadata.mongodb.MongoApplicationConfig;
import ch.epfl.gsn.metadata.tools.mediawiki.MediaWikiReannotation;
import ch.epfl.gsn.metadata.tools.mediawiki.MongoWikiConfig;
import ch.epfl.gsn.metadata.tools.taxonomy.MongoTaxonomyConfig;
import ch.epfl.gsn.metadata.tools.taxonomy.TermsUpdate;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Created by kryvych on 24/03/15.
 */
public class MediaWikiUpdate {public static void main(String[] args) {

    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MongoApplicationConfig.class, MongoWikiConfig.class);
    ctx.scan("ch.epfl.gsn.metadata");
    MediaWikiReannotation service = ctx.getBean(MediaWikiReannotation.class);


    long count = service.updateWikiAnnotations();
    System.out.println("count = " + count);
}
}
