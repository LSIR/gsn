package ch.epfl.gsn.metadata.web;

import ch.epfl.gsn.metadata.mongodb.MongoApplicationConfig;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;

/**
 * Created by kryvych on 26/03/15.
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan
@Import(MongoApplicationConfig.class)
//@SpringBootApplication
public class Application extends SpringBootServletInitializer {

    @Bean(name = "configuration")
    public PropertiesFactoryBean configuration() {
        PropertiesFactoryBean bean = new PropertiesFactoryBean();
        bean.setLocation(new ClassPathResource("configuration.properties"));
        return bean;
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);
    }

}
