package ch.epfl.gsn.oai.rest.verbs;

import ch.epfl.gsn.oai.interfaces.Record;
import ch.epfl.gsn.oai.interfaces.RecordAccessService;
import ch.epfl.gsn.oai.rest.ErrorOai;
import ch.epfl.gsn.oai.interfaces.MetadataFormats;
import ch.epfl.gsn.oai.rest.OaiListRequestParameters;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import sun.plugin.converter.util.StdUtils;

import java.text.ParseException;
import java.util.Date;
import java.util.Set;

/**
 * Created by kryvych on 17/09/15.
 */
public abstract class ListVerb {
    protected static final Logger logger = LoggerFactory.getLogger(ListIdentifiers.class);
    protected final MetadataFormats metadataFormats;
    protected final TemplateHelper templateHelper;
    protected final RecordAccessService recordAccessService;

    public ListVerb(TemplateHelper templateHelper, RecordAccessService recordAccessService, MetadataFormats metadataFormats) {
        this.templateHelper = templateHelper;
        this.recordAccessService = recordAccessService;
        this.metadataFormats = metadataFormats;
    }

    public String getResponse(OaiListRequestParameters parameters) {

        String verb = this.getClass().getSimpleName();

        if(!metadataFormats.isSupportedFormat(parameters.getMetadataPrefix())) {
            return ErrorOai.CANNOT_DISSEMINATE_FORMAT.generateMessage(templateHelper, verb);
        }
        Date from;
        Date until;

        try {
            from = parameters.getFromDate();
            until = parameters.getUntilDate();
        } catch (ParseException e) {
            return ErrorOai.BAD_ARGUMENT.generateMessage(templateHelper, verb);
        }

        String resumptionToken = parameters.getResumptionToken();
        if(StringUtils.isNotEmpty(resumptionToken) && !recordAccessService.isValidResumptionToken(resumptionToken)) {
            return ErrorOai.BAD_RESUMPTION_TOKEN.generateMessage(templateHelper, verb);
        }

        Set<Record> records = recordAccessService.getRecords(from, until, resumptionToken);
        if (CollectionUtils.isEmpty(records)) {
            return ErrorOai.NO_RECORDS_MATCH.generateMessage(templateHelper, verb);
        }

        return formatContent(records, parameters);
    }

    protected abstract String formatContent(Set<Record> records, OaiListRequestParameters parameters);
}
