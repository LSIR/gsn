package ch.epfl.gsn.metadata.tools.taxonomy;

import ch.epfl.gsn.metadata.mongodb.MongoApplicationConfig;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Created by kryvych on 08/01/15.
 */
@Configuration
@EnableMongoRepositories(basePackages = "ch.epfl.gsn.metadata.tools.taxonomy")
public class MongoTaxonomyConfig extends MongoApplicationConfig {
//    @Override
//    protected String getDatabaseName() {
//        return "metadata";
//    }
//
//    @Override
//    public Mongo mongo() throws Exception {
//        Mongo mongo =  new MongoClient();
//        mongo.setWriteConcern(WriteConcern.SAFE);
//        return mongo;
//    }
//
//    public @Bean
//    MongoTemplate mongoTemplate() throws Exception {
//        return new MongoTemplate(mongo(), "metadata");
//    }
}
