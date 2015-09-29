package ch.epfl.gsn.oai;

import ch.epfl.gsn.metadata.mongodb.MongoApplicationConfig;
import ch.epfl.gsn.oai.impl.DcOaiFormat;
import ch.epfl.gsn.oai.impl.DifConverter;
import ch.epfl.gsn.oai.impl.DifFormat;
import ch.epfl.gsn.oai.interfaces.Converter;
import ch.epfl.gsn.oai.interfaces.MetadataFormat;
import ch.epfl.gsn.oai.interfaces.OaiConfiguration;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.Set;

/**
 * Created by kryvych on 18/09/15.
 */
@Configuration
@ComponentScan("ch.epfl.gsn.oai")
@EnableMongoRepositories(basePackages = "ch.epfl.gsn.oai.model")
@Import({MongoApplicationConfig.class, PropertiesConfiguration.class})
public class OaiConfigurationImpl implements OaiConfiguration {

    @Autowired
    private DifConverter recordConverter;


    @Autowired
    private DifFormat difFormat;

    @Autowired
    private DcOaiFormat dcOaiFormat;


    @Override
    @Bean
    public Set<Converter> converters() {
        Set<Converter> converters = Sets.newHashSet();
        converters.add(recordConverter);

        return converters;
    }

    @Override
    @Bean
    public Set<MetadataFormat> formats() {
        Set<MetadataFormat> formats = Sets.newHashSet();
        formats.add(difFormat);
        formats.add(dcOaiFormat);
        return formats;
    }
}
