package ch.epfl.gsn.oai.interfaces;

import java.util.Set;

/**
 * Created by kryvych on 18/09/15.
 */
public interface OaiConfiguration {

    Set<Converter> converters();

    Set<MetadataFormat> formats();

}
