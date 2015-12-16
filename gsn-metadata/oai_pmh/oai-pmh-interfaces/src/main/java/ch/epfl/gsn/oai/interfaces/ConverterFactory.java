package ch.epfl.gsn.oai.interfaces;


import java.util.Set;

/**
 * Created by kryvych on 10/09/15.
 */
public abstract class ConverterFactory {


    public <M extends MetadataFormat> Converter getConverter(Class<M> format) {
        for (Converter converter : getConverters()) {
            if(converter.isForFormat(format)) {
                return converter;
            }
        }
        throw new DataAccessException("No converter for a given format " + format);
    }

    public abstract Set<Converter> getConverters();
}
