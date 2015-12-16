package ch.epfl.gsn.oai.impl;

import ch.epfl.gsn.oai.interfaces.Converter;
import ch.epfl.gsn.oai.interfaces.ConverterFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Set;

/**
 * Created by kryvych on 25/09/15.
 */
@Named
public class ConverterFactoryImpl extends ConverterFactory {

    private Set<Converter> converters;

    @Inject
    public ConverterFactoryImpl(Set<Converter> converters) {
        this.converters = converters;
    }


    @Override
    public Set<Converter> getConverters() {
        return converters;
    }
}

