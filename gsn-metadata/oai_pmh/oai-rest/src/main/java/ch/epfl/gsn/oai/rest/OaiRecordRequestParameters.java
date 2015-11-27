package ch.epfl.gsn.oai.rest;

/**
 * Created by kryvych on 07/09/15.
 */
public class OaiRecordRequestParameters {
    private String identifier;
    private String metadataPrefix;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getMetadataPrefix() {
        return metadataPrefix;
    }

    public void setMetadataPrefix(String metadataPrefix) {
        this.metadataPrefix = metadataPrefix;
    }
}
