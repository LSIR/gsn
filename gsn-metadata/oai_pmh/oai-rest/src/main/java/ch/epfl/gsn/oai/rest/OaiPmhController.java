package ch.epfl.gsn.oai.rest;

import ch.epfl.gsn.oai.interfaces.DataAccessException;
import ch.epfl.gsn.oai.rest.verbs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

/**
 * Created by kryvych on 04/09/15.
 */
@RestController
@RequestMapping("/oai")
public class OaiPmhController {

    private final Identify identify;
    private final ListMetadataFormats listMetadataFormats;
    private final GetRecord getRecord;
    private final ListIdentifiers listIdentifiers;
    private final ListRecords listRecords;
    private final ListSets listSets;
    private final NotSupportedVerb notSupportedVerb;

    protected static final Logger logger = LoggerFactory.getLogger(OaiPmhController.class);

    @Inject
    public OaiPmhController(Identify identify, ListMetadataFormats listMetadataFormats, GetRecord getRecord, ListIdentifiers listIdentifiers, ListRecords listRecords, ListSets listSets, NotSupportedVerb notSupportedVerb) {
        this.identify = identify;
        this.listMetadataFormats = listMetadataFormats;
        this.getRecord = getRecord;
        this.listIdentifiers = listIdentifiers;
        this.listRecords = listRecords;
        this.listSets = listSets;
        this.notSupportedVerb = notSupportedVerb;
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, produces = "text/xml;charset=UTF-8")
    public
    @ResponseBody
    String verb(String verb) {
       return notSupportedVerb.getResponse(verb);
    }

    @RequestMapping(params = "verb=Identify", method = {RequestMethod.GET, RequestMethod.POST}, produces = "text/xml;charset=UTF-8")
    public
    @ResponseBody
    String identify() {
        logger.info("identify");

        return identify.getResponse();
    }

    @RequestMapping(params = "verb=ListMetadataFormats", method = {RequestMethod.GET, RequestMethod.POST}, produces = "text/xml;charset=UTF-8")
    public
    @ResponseBody
    String listMetadataFormats() {
        logger.info("ListMetadataFormats");

        return listMetadataFormats.getResponse();
    }

    //    http://arXiv.org/oai2?verb=GetRecord&identifier=oai:arXiv.org:cs/0112017&metadataPrefix=oai_dc
    @RequestMapping(params = "verb=GetRecord", method = {RequestMethod.GET, RequestMethod.POST}, produces = "text/xml;charset=UTF-8")
    public
    @ResponseBody
    String getRecord(OaiRecordRequestParameters parameters) {
        logger.info("GetRecord");

        return getRecord.getResponse(parameters);
    }

    @RequestMapping(params = "verb=ListRecords", method = {RequestMethod.GET, RequestMethod.POST}, produces = "text/xml;charset=UTF-8")
    public
    @ResponseBody
    String listRecords(OaiListRequestParameters parameters) {
        logger.info("ListRecords");

        return listRecords.getResponse(parameters);
    }

    @RequestMapping(params = "verb=ListIdentifiers", method = {RequestMethod.GET, RequestMethod.POST}, produces = "text/xml;charset=UTF-8")
    public
    @ResponseBody
    String listIdentifiers(OaiListRequestParameters parameters) {
        logger.info("ListIdentifiers");

        return listIdentifiers.getResponse(parameters);
    }

    @RequestMapping(params = "verb=ListSets", method = {RequestMethod.GET, RequestMethod.POST}, produces = "text/xml;charset=UTF-8")
    public
    @ResponseBody
    String listSets(OaiListRequestParameters parameters) {
        logger.info("ListSets");

       return listSets.getResponse(parameters);
    }


    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<String> handleMethodArgumentNotValidException(HttpServletRequest req, DataAccessException e) {
            logger.error("Error!", e);
        return new ResponseEntity<String>("Retry-After: 1200", HttpStatus.SERVICE_UNAVAILABLE);
    }
}

