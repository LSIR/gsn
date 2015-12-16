package ch.epfl.gsn.oai.interfaces;

import java.util.Set;

/**
 * Created by kryvych on 04/09/15.
 */
public abstract class MetadataFormats {


    public boolean isSupportedFormat(String formatName) {
        return getByName(formatName) != null;
    }

    public MetadataFormat getByName(String formatName) {
        for (MetadataFormat format : getFormats()) {
            if (format.getName().equals(formatName)) {
                return format;
            }
        }
        return null;
    }

    public abstract Set<MetadataFormat> getFormats();
}
