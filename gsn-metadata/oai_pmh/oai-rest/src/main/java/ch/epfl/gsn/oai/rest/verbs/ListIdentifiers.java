package ch.epfl.gsn.oai.rest.verbs;

import ch.epfl.gsn.oai.interfaces.MetadataFormats;
import ch.epfl.gsn.oai.interfaces.Record;
import ch.epfl.gsn.oai.interfaces.RecordAccessService;
import ch.epfl.gsn.oai.rest.OaiListRequestParameters;
import org.springframework.context.annotation.Scope;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Set;

/**
 * Created by kryvych on 07/09/15.
 */
@Named
@Scope("prototype")
public class ListIdentifiers extends ListVerb {


    @Inject
    public ListIdentifiers(MetadataFormats metadataFormats, TemplateHelper templateHelper, RecordAccessService recordAccessService) {
        super(templateHelper, recordAccessService, metadataFormats);
    }


    @Override
    protected String formatContent(Set<Record> records, OaiListRequestParameters parameters) {

        StringBuilder headers = new StringBuilder();
        for (Record record : records) {
            headers.append(templateHelper.formatHeader(record)).append("\n");
        }

        String verbContent = headers.toString();

        return templateHelper.fillTopTmplate(this.getClass().getSimpleName(), verbContent);
    }

}
