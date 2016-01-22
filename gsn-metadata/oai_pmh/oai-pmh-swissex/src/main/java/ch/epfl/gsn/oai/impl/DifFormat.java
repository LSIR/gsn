package ch.epfl.gsn.oai.impl;


import ch.epfl.gsn.oai.interfaces.MetadataFormat;

import javax.inject.Named;

/**
 * Created by kryvych on 07/09/15.
 */
@Named
public class DifFormat implements MetadataFormat {

    @Override
    public String getName() {
        return "dif";
    }

    @Override
    public String getSchema() {
        return "http://gcmd.gsfc.nasa.gov/Aboutus/xml/dif/dif.xsd";
    }

    @Override
    public String getNamespace() {
        return "http://gcmd.gsfc.nasa.gov/Aboutus/xml/dif/";
    }

}
