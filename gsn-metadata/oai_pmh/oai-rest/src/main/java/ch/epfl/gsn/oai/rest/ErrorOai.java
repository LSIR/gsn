package ch.epfl.gsn.oai.rest;

import ch.epfl.gsn.oai.rest.verbs.TemplateHelper;

import java.util.Map;

/**
 * Created by kryvych on 08/09/15.
 */
public enum ErrorOai {

    BAD_VERB("badVerb", "Unknown verb"),
    ID_DOES_NOT_EXIST("idDoesNotExist", "The value of the identifier argument is unknown or illegal in this repository."),
    CANNOT_DISSEMINATE_FORMAT("cannotDisseminateFormat", "The metadata format identified by the value given for the metadataPrefix argument is not supported by the item or by the repository."),
    NO_RECORDS_MATCH("noRecordsMatch", "The combination of the values of the from, until, set, and metadataPrefix arguments results in an empty list."),
    NO_METADATA_FORMATS("noMetadataFormats", "There are no metadata formats available for the specified item."),
    NO_SET_HIERARCHY("noSetHierarchy", "The repository does not support sets."),
    BAD_RESUMPTION_TOKEN("badResumptionToken", "The value of the resumptionToken argument is invalid or expired."),
    BAD_ARGUMENT("badArgument", "The request includes illegal arguments, is missing required arguments, includes a repeated argument, or values for arguments have an illegal syntax.");


    private String errorCode;
    private String errorText;

    ErrorOai(String errorCode, String errorText) {
        this.errorCode = errorCode;
        this.errorText = errorText;
    }

    public String generateMessage(TemplateHelper templateHelper, String verb) {
        Map<String, String> parameters = templateHelper.getCommonParameters();
        parameters.put("verb", "verb=\"" + verb + "\"");
        parameters.put("errorCode", this.errorCode);
        parameters.put("errorText", this.errorText);

        return templateHelper.fillTemplate("error.template", parameters);
    }


}
