package ch.epfl.gsn.oai.interfaces;


/**
 * Created by kryvych on 07/09/15.
 */
public interface Converter<T extends Record> {

    String convert(T record);

    <M extends MetadataFormat>boolean isForFormat(Class<M> formatClass);

    boolean canConvertRecord(T record);
}
