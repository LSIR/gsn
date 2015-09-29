package ch.epfl.gsn.oai.rest;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.io.ClassPathResource;

@SpringBootApplication
//===== Import here your Spring configuration file ======
@Import(ch.epfl.gsn.oai.OaiConfigurationImpl.class)
public class Application {

    @Bean(name = "templateConfiguration")
    public PropertiesFactoryBean configuration() {
        PropertiesFactoryBean bean = new PropertiesFactoryBean();
        bean.setLocation(new ClassPathResource("template.properties"));
        return bean;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
