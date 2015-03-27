package ch.epfl.gsn.metadata.web;

import ch.epfl.gsn.metadata.mongodb.MongoApplicationConfig;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;

/**
 * Created by kryvych on 26/03/15.
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan
@Import(MongoApplicationConfig.class)
public class Application {

    @Bean(name = "configuration")
    public PropertiesFactoryBean configuration() {
        PropertiesFactoryBean bean = new PropertiesFactoryBean();
        bean.setLocation(new ClassPathResource("configuration.properties"));
        return bean;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
