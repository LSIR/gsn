package ch.epfl.gsn.oai.rest.verbs;

import ch.epfl.gsn.oai.rest.ErrorOai;
import ch.epfl.gsn.oai.rest.OaiListRequestParameters;
import org.springframework.context.annotation.Scope;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by kryvych on 07/09/15.
 */
@Named
@Scope("prototype")
public class ListSets {

    private final TemplateHelper templateHelper;

    @Inject
    public ListSets(TemplateHelper templateHelper) {
        this.templateHelper = templateHelper;
    }

    public String getResponse(OaiListRequestParameters parameters) {
        return ErrorOai.NO_SET_HIERARCHY.generateMessage(templateHelper, "ListSets");
    }
}
