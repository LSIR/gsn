package ch.epfl.gsn.oai.rest.verbs;

import ch.epfl.gsn.oai.rest.ErrorOai;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by kryvych on 18/09/15.
 */
@Named
public class NotSupportedVerb {

    private final TemplateHelper templateHelper;

    @Inject
    public NotSupportedVerb(TemplateHelper templateHelper) {
        this.templateHelper = templateHelper;
    }

    public String getResponse(String verb) {

        return ErrorOai.BAD_VERB.generateMessage(templateHelper, verb);
    }
}
