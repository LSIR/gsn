package ch.epfl.gsn.metadata.tools.geoservice;

import ch.epfl.gsn.metadata.core.model.GeoData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.inject.Named;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;

/**
 * Created by kryvych on 24/03/15.
 */
@Named
@Scope("prototype")
public class LocationEnrichmentService {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    // http://montblanc.slf.ch/getTopo.php?lon=9.83182&lat=46.36076&format=csv
//    public static final String COORDINATES_URL = "http://osgl.ethz.ch/webservices/gsndem/get2?lon={0}&lat={1}";
    public static final String COORDINATES_URL = "http://montblanc.slf.ch/getTopo.php?lon={0}&lat={1}";


    public LocationEnrichmentService() {
    }



    public GeoData fetchGeoData(double lon, double lat){

        String location = MessageFormat.format(COORDINATES_URL, lon, lat);

        URL url = null;
        try {
            url = new URL(location);
        } catch (MalformedURLException e) {
            logger.error("Cannot get extra information for location " + lon + ":" + lat, e);
            return null;
        }
        try (InputStream stream = url.openStream()){

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document document = builder.parse(stream);
            NodeList dem = document.getElementsByTagName("dem");
            Element item = (Element) dem.item(0);

            return new GeoData(Double.parseDouble(item.getElementsByTagName("elevation").item(0).getTextContent()),
                    Double.parseDouble(item.getElementsByTagName("slope").item(0).getTextContent()),
                    Double.parseDouble(item.getElementsByTagName("aspect").item(0).getTextContent()));

        } catch (IOException | ParserConfigurationException| SAXException e) {
            logger.error("Cannot get extra information for location " + lon + ":" + lat, e);
            return null;
        }

    }


}

