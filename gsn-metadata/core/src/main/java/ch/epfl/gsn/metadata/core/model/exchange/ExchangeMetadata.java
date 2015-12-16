package ch.epfl.gsn.metadata.core.model.exchange;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.util.Map;
import java.util.Properties;

/**
 * Created by kryvych on 03/09/15.
 */
@Document(collection = "exchange_metadata")
public class ExchangeMetadata {
    @Id
    private BigInteger id;

    @Indexed
    private String name;
    private String data;

    protected static final Logger logger = LoggerFactory.getLogger(ExchangeMetadata.class);

    public ExchangeMetadata(String name, String data) {
        this.name = name;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public String getData() {
        return data;
    }

    public Map<String, String> getProperties() {
        final Properties properties = new Properties();
        try {
            properties.load(new StringReader(data));
            return Maps.fromProperties(properties);
        } catch (IOException e) {
            logger.error("Cannot load properties! ", e);
            return Maps.newHashMap();
        }
    }
}
