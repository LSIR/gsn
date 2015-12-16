package ch.epfl.gsn.oai.rest;

import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by kryvych on 07/09/15.
 */
public class OaiListRequestParameters {

    protected static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private String metadataPrefix;
    private String from;
    private String until;
    private String set;
    private String resumptionToken;

    public String getMetadataPrefix() {
        return metadataPrefix;
    }

    public void setMetadataPrefix(String metadataPrefix) {
        this.metadataPrefix = metadataPrefix;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getUntil() {
        return until;
    }

    public void setUntil(String until) {
        this.until = until;
    }

    public String getSet() {
        return set;
    }

    public void setSet(String set) {
        this.set = set;
    }

    public String getResumptionToken() {
        return resumptionToken;
    }

    public void setResumptionToken(String resumptionToken) {
        this.resumptionToken = resumptionToken;
    }

    public Date getFromDate() throws ParseException {
        if (StringUtils.isNotEmpty(from)) {
            return DATE_FORMAT.parse(from);
        }
        return null;
    }

    public Date getUntilDate() throws ParseException {
        if (StringUtils.isNotEmpty(until)) {
            return DATE_FORMAT.parse(until);
        }
        return null;
    }
}
