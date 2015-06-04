package ch.epfl.gsn.metadata.tools.geoservice;

import ch.epfl.gsn.metadata.mongodb.MongoApplicationConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Created by kryvych on 25/03/15.
 */
@Configuration
public class ToolsConfiguration {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        simpleClientHttpRequestFactory.setConnectTimeout(5000);
        simpleClientHttpRequestFactory.setReadTimeout(5000);

        return new RestTemplate(simpleClientHttpRequestFactory);


    }

}
