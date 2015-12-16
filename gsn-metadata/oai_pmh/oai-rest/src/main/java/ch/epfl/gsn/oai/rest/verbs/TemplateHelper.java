package ch.epfl.gsn.oai.rest.verbs;

import ch.epfl.gsn.oai.interfaces.DataAccessException;
import ch.epfl.gsn.oai.interfaces.Record;
import ch.epfl.gsn.oai.utils.ReadFileToString;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.springframework.context.annotation.Scope;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

/**
 * Created by kryvych on 08/09/15.
 */
@Named
@Scope()
public class TemplateHelper {

//    public final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public final String DATETIME_FORMAT;

    private final Properties templateConfiguration;

    @Inject
    public TemplateHelper(Properties templateConfiguration) {
        this.templateConfiguration = templateConfiguration;
        DATETIME_FORMAT =templateConfiguration.getProperty("datetime.format");
    }

    public Map<String, String> getCommonParameters() {
        Map<String, String> parameters = Maps.newHashMap();
        String date = formatDate(new Date());

        parameters.put("responseDate", date);
        parameters.put("earliestDatestamp", date);


        return parameters;
    }

    public String fillTemplate(String templateName, Map<String, String> parameters) {


        try {
            String template = ReadFileToString.readFileFromClasspath(templateConfiguration.getProperty(templateName));
            StrSubstitutor substitutor = new StrSubstitutor(parameters);
            return substitutor.replace(template);
        } catch (IOException e) {
            throw new DataAccessException(e);
        }

    }

    public String fillTemplateWithDefaultParameters(String templateName) {
      return fillTemplate(templateName, getCommonParameters());

    }

    public String formatDate(Date date) {
        return new SimpleDateFormat(DATETIME_FORMAT).format(date);
    }


    public String formatHeader(Record record) {
        Map<String, String> headerParmeters = Maps.newHashMap();
        headerParmeters.put("identifier", record.getOAIIdentifier());
        headerParmeters.put("date", formatDate(record.getDateStamp()));

        if (record.isDeleted()) {
            headerParmeters.put("status", "status=\"deleted\"");
        } else {
            headerParmeters.put("status", "");
        }
        return fillTemplate("header.template", headerParmeters);
    }

    public String formatRecord(Record record, String metadata) {

        String header = formatHeader(record);
        Map<String, String> recordParameters = Maps.newHashMap();
        recordParameters.put("header", header);
        recordParameters.put("metadata", metadata);

        return fillTemplate("record.template", recordParameters);
    }

    public String fillTopTmplate(String verb, String verbContent) {
        Map<String, String> commonParameters = getCommonParameters();
        commonParameters.put("verb", verb);
        commonParameters.put("verbContent", verbContent);

        return fillTemplate("common.template", commonParameters);
    }

}
