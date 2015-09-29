package ch.epfl.gsn.oai.rest.verbs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by kryvych on 04/09/15.
 */
@Named
@Scope("prototype")
public class Identify{

    protected static final Logger logger = LoggerFactory.getLogger(Identify.class);

    private final TemplateHelper templateHelper;

    @Inject
    public Identify(TemplateHelper templateHelper) {
        this.templateHelper = templateHelper;
    }


    public String getResponse() {

           return templateHelper.fillTemplateWithDefaultParameters("identify.template");



//        return "<?xml version='1.0' encoding='UTF-8'?>\n" +
//                "<OAI-PMH xmlns='http://www.openarchives.org/OAI/2.0/' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n" +
//                "         xsi:schemaLocation='http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd'>\n" +
//                "    <responseDate>2015-09-04T13:05:53Z</responseDate>\n" +
//                "    <request>http://montblanc.slf.ch:8095/oai</request>\n" +
//                "    <Identify>\n" +
//                "        <repositoryName>SLF Repository</repositoryName>\n" +
//                "        <baseURL>http://montblanc.slf.ch:8095/oai</baseURL>\n" +
//                "        <protocolVersion>2.0</protocolVersion>\n" +
//                "        <adminEmail>nkryvych@gmail.com</adminEmail>\n" +
//                "        <earliestDatestamp>2000-01-01</earliestDatestamp>\n" +
//                "        <deletedRecord>transient</deletedRecord>\n" +
//                "        <granularity>YYYY-MM-DDThh:mm:ssZ</granularity>\n" +
//                "    </Identify>\n" +
//                "</OAI-PMH>";
    }

}
