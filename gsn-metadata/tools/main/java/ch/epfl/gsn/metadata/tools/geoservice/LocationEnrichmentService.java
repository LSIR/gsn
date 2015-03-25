package ch.epfl.gsn.metadata.tools.geoservice;

import ch.epfl.gsn.metadata.core.model.GeoData;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * Created by kryvych on 24/03/15.
 */
@Named
@Scope("prototype")
public class LocationEnrichmentService {

    // http://osgl.ethz.ch/webservices/gsndem/get2?lon=8.50693&lat=47.40803
    public static final String COORDINATES_URL = "http://osgl.ethz.ch/webservices/gsndem/get2?lon={0}&lat={1}";

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private RestTemplate restTemplate;

    @Inject
    public LocationEnrichmentService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    public GeoData fetchGeoData(double lon, double lat) {
        String url = MessageFormat.format(COORDINATES_URL, lat, lon);

        try {
            String result = restTemplate.getForObject(url, String.class);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document document = builder.parse(IOUtils.toInputStream(result, "UTF-8"));
            NodeList dem = document.getElementsByTagName("dem");
            Element item = (Element) dem.item(0);

            return new GeoData(Double.parseDouble(item.getElementsByTagName("elevation").item(0).getTextContent()),
            Double.parseDouble(item.getElementsByTagName("slope").item(0).getTextContent()),
                    Double.parseDouble(item.getElementsByTagName("aspect").item(0).getTextContent()));

        } catch (RestClientException | ParserConfigurationException | SAXException | IOException e) {
            logger.error("Cannot get extra information for location ", e);
            return null;
        }

    }

}

