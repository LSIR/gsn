package ch.epfl.gsn.oai;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * Created by kryvych on 29/09/15.
 */
@Configuration
public class PropertiesConfiguration {

    @Bean(name = "osperConfiguration")
    public PropertiesFactoryBean recordConfiguration() {
        PropertiesFactoryBean bean = new PropertiesFactoryBean();
        bean.setLocation(new ClassPathResource("osper_oai.properties"));
        return bean;
    }
}
