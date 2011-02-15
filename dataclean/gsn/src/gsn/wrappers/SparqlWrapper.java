package gsn.wrappers;

import org.apache.log4j.Logger;
import gsn.beans.DataField;
import gsn.beans.AddressBean;
import gsn.beans.DataTypes;
import gsn.utils.GSNRuntimeException;

import com.hp.hpl.jena.query.*;

import java.io.Serializable;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.*;
import java.text.ParseException;


/*
* Sparql Wrapper
*
* Reads semantic data from a Sparql-enabled server
*
* Requieres:
* - url of the server
* - rate (in milliseconds) for refresh
* - list of fields to export with their types
* - query specified in sparql
* All those parameters are specified as predicates in the xml file of the virtual sensor
*
* Currently supported types for fields are:
* - varchar() for strings
* - integer for integer numbers
* - double for floating numbers
* - bigint for dates (in ISO-8609 format)
*
* A sample VS file is provided in virtual-sensors/samples/sparql_vs.xml
*
* */
public class SparqlWrapper extends AbstractWrapper {

    private transient Logger logger = Logger.getLogger(this.getClass());

    private int DEFAULT_SAMPLING_RATE_IN_MSEC = 60000; //default rate, every 60 seconds.

    public static final String DEFAULT_SERVICE_URL = "http://www.swiss-experiment.ch/sparql/model";

    private int threadCounter = 0;

    private Map<String, String> listOfSparqlFields = new LinkedHashMap<String, String>(); // list of fields to export

    private DataField[] outputStructure;

    private int rate;
    private String url;
    private String query;
    private String fields;
    private static final byte DATA_TYPE_NONE = -1;

    public boolean initialize() {

        boolean init_result = true;
        setName("SparqlWrapper-Thread" + (++threadCounter));
        AddressBean addressBean = getActiveAddressBean();
        rate = addressBean.getPredicateValueAsInt("rate", DEFAULT_SAMPLING_RATE_IN_MSEC);
        url = addressBean.getPredicateValueWithDefault("url", DEFAULT_SERVICE_URL);

        query = addressBean.getPredicateValue("query");

        fields = addressBean.getPredicateValue("fields");

        if (query != null) {
            logger.info("Sparql query => " + query);
        } else {
            init_result = false;
            logger.error("No sparql query provided for VS " + addressBean.getVirtualSensorName());
        }

        if (fields != null) {
            parseFields(fields);
            logger.info("Sparql fields => " + fields);
        } else {
            init_result = false;
            logger.error("No sparql fields provided for VS " + addressBean.getVirtualSensorName());
        }

        return init_result;
    }

    private void parseFields(String fields) {
        String[] allFields = fields.trim().split(",");
        for (int i = 0; i < allFields.length; i++) {
            String[] keyAndValue = allFields[i].trim().split(":");
            logger.info("Field \"" + keyAndValue[0] + "\":\"" + keyAndValue[1] + "\"");
            String _key = keyAndValue[0].trim();
            String _value = keyAndValue[1].trim();
            listOfSparqlFields.put(_key, _value);
        }
        // initializing output structure
        outputStructure = new DataField[listOfSparqlFields.size()];
        int index = 0;
        for (String key : listOfSparqlFields.keySet()) {
            outputStructure[index] = new DataField(key, listOfSparqlFields.get(key));
            logger.debug(key + " => " + listOfSparqlFields.get(key));
            index++;
        }

        if (logger.isDebugEnabled())
            for (int j = 0; j < outputStructure.length; j++) {
                logger.debug("outputStructure[" + j + "] = " + outputStructure[j]);
            }
    }

    public void run() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }

        while (isActive()) {
            try {
                executeRemoteSparqlQuery();
                Thread.sleep(rate);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }


    public DataField[] getOutputFormat() {
        return outputStructure;
    }


    public void finalize() {
        threadCounter--;
    }

    public String getWrapperName() {
        return "Sparql Wrapper";
    }


    /**
     *
     */

    public void executeRemoteSparqlQuery() {

        Query arqQuery = QueryFactory.create(query); // create from text query

        QueryExecution qexec = QueryExecutionFactory.sparqlService(url, arqQuery);

        try {
            com.hp.hpl.jena.query.ResultSet results = qexec.execSelect();

            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();

                long epoch = System.currentTimeMillis();

                List<Serializable> tuples = new Vector<Serializable>(); // list of tuples to publish by wrapper

                boolean validResult = true;

                for (String fieldName : listOfSparqlFields.keySet()) {
                    logger.debug("Accessing field " + fieldName + " : " + listOfSparqlFields.get(fieldName));
                    String dataTypeAsString = listOfSparqlFields.get(fieldName);

                    //Check if fieldName exists in the returned sparql result
                    if (soln.getLiteral(fieldName) == null) {
                        logger.warn("Field " + fieldName + " not found in Sparql result set");
                        validResult = false;
                    }

                    byte fieldtype = DATA_TYPE_NONE;

                    try {
                        fieldtype = DataTypes.convertTypeNameToGSNTypeID(dataTypeAsString);
                    }
                    catch (GSNRuntimeException e) {
                        validResult = false;
                        logger.warn(e.getMessage(), e);
                    }

                    switch (fieldtype) {

                        case DataTypes.BIGINT:
                            logger.debug("date => " + soln.getLiteral(fieldName).getString());
                            long _date = parse_date(soln.getLiteral(fieldName).getString());
                            if (_date==-1) {
                                validResult = false;
                                logger.warn("invalid date: "+parse_date(soln.getLiteral(fieldName).getString()));
                            }
                            tuples.add(_date);
                            break;

                        case DataTypes.DOUBLE:
                            logger.debug("double => " + soln.getLiteral(fieldName).getDouble());
                            double _double = soln.getLiteral(fieldName).getDouble();
                            tuples.add(_double);
                            break;

                        case DataTypes.INTEGER:
                            logger.debug("integer => " + soln.getLiteral(fieldName).getInt());
                            int _int = soln.getLiteral(fieldName).getInt();
                            tuples.add(_int);
                            break;

                        case DataTypes.VARCHAR:
                            logger.debug("varchar (string) => " + soln.getLiteral(fieldName).getString());
                            String _string = soln.getLiteral(fieldName).getString();
                            tuples.add(_string);
                            break;

                        //TODO: if needed, add other datatypes: smallint, char, etc...

                        case DATA_TYPE_NONE:
                        default:
                            logger.warn("Unknown data type: " + dataTypeAsString + " for field: "+ fieldName);
                            validResult = false;
                            break;
                    }
                }

                //List tuples
                logger.debug("Read (" + tuples.size() + ") tuples from Sparql result set");
                Serializable[] s = new Serializable[tuples.size()];
                int index = 0;
                for (Serializable tuple : tuples) {
                    s[index] = tuple;
                    logger.debug("tuple[" + index  + "]" + s[index]);
                    index++;

                }

                if (validResult)
                    postStreamElement(epoch, s);
                else
                    logger.warn("Invalid result for Sparql query");

                Thread.sleep(100); // avoid duplicate timestamps when publishing with postStreamElement
            }

        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        } finally {
            qexec.close();
        }

    }

    /*
    * returns epoch date, 
    * given string date in ISO-8609 format (as returned in sparql queries)
    *
    */
    private long parse_date(String s) {
        long l;

        Pattern datePattern = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})");
        Matcher dateMatcher = datePattern.matcher(s);
        if (dateMatcher.find()) {
            String yyyy = dateMatcher.group(1);
            String mm = dateMatcher.group(2);
            String dd = dateMatcher.group(3);

            String hh = dateMatcher.group(4);
            String mn = dateMatcher.group(5);
            String ss = dateMatcher.group(6);

            String sdate = dd + "/" + mm + "/" + yyyy + " " + hh + ":" + mn + ":" + ss;

            try {
                l = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").parse(sdate).getTime();
            } catch (ParseException e) {
                l = -1;
                e.printStackTrace();
                logger.warn(e.getMessage(), e);
            }

        } else l = -1;
        return l;
    }


}
