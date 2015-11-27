package ch.epfl.gsn.oai.rest.verbs;

import ch.epfl.gsn.oai.interfaces.*;
import ch.epfl.gsn.oai.rest.ErrorOai;
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
public class ListRecords extends ListVerb {

    private final ConverterFactory converterFactory;

    @Inject
    public ListRecords(TemplateHelper templateHelper, RecordAccessService recordAccessService, MetadataFormats metadataFormats, ConverterFactory converterFactory) {
        super(templateHelper, recordAccessService, metadataFormats);
        this.converterFactory = converterFactory;
    }

    @Override
    protected String formatContent(Set<Record> records, OaiListRequestParameters parameters) {

        MetadataFormat metadataFormat = metadataFormats.getByName(parameters.getMetadataPrefix());
        Converter converter = converterFactory.getConverter(metadataFormat.getClass());

        StringBuilder xmlRecords = new StringBuilder();
        for (Record record : records) {
            if(record == null || !converter.canConvertRecord(record)) {
                return ErrorOai.ID_DOES_NOT_EXIST.generateMessage(templateHelper, this.getClass().getSimpleName());
            }

            String metadata = converter.convert(record);
            xmlRecords.append(templateHelper.formatRecord(record, metadata)).append("\n");
        }

        String verbContent = xmlRecords.toString();

        return templateHelper.fillTopTmplate(this.getClass().getSimpleName(), verbContent);
    }
}
