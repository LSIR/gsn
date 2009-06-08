package gsn.wrappers;

import org.apache.log4j.Logger;
import gsn.beans.DataField;
import gsn.beans.AddressBean;

import com.hp.hpl.jena.query.*;

import java.io.Serializable;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.ParseException;


public class SparqlWrapper extends AbstractWrapper {

    private transient Logger logger = Logger.getLogger(this.getClass());

    private int DEFAULT_SAMPLING_RATE_IN_MSEC = 60000; //every 60 seconds.

    public static final String DEFAULT_SERVICE_URL = "http://www.swiss-experiment.ch/sparql/model";

    private int threadCounter = 0;

    private transient final DataField[] outputStructure = new DataField[]{
            new DataField("station_name", "varchar(100)", "station_name"),
            new DataField("sensor_serialno", "varchar(100)", "sensor_serialno"),
            new DataField("project_name", "varchar(100)", "project_name"),
            new DataField("start_date", "bigint", "start_date"),
            new DataField("end_date", "bigint", "end_date")
    };

    /*private transient final DataField[] outputStructure = new DataField[]{
            new DataField("action", "varchar(100)", "action"),
            new DataField("addinfo", "varchar(200)", "addinfo"),
            new DataField("start_date", "bigint", "start_date"),
            new DataField("end_date", "bigint", "end_date"),
            new DataField("parameter_name", "varchar(100)", "parameter_name")
    };*/

    private int rate;
    private String url;
    //private String query_file;
    private String query;

    public boolean initialize() {

        setName("SparqlWrapper-Thread" + (++threadCounter));
        AddressBean addressBean = getActiveAddressBean();
        rate = addressBean.getPredicateValueAsInt("rate", DEFAULT_SAMPLING_RATE_IN_MSEC);
        url = addressBean.getPredicateValueWithDefault("url", DEFAULT_SERVICE_URL);

        query = addressBean.getPredicateValue("query");

        logger.info("Sparql query => "+ query);

        return true;
    }

    public void run() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
        }

        while (isActive()) {
            try {
                runRemoteQueryWithIteration();
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
     * *****************************************************************************
     */

    public void runRemoteQueryWithIteration() {

        String station_name;
        String sensor_serialno;
        String start_date;
        String end_date;
        String project_name;
        long start_date_as_long;
        long end_date_as_long;

        Query arqQuery = QueryFactory.create(query); // create from text query
        //Query query = QueryFactory.read (query_file); // read from file

        QueryExecution qexec = QueryExecutionFactory.sparqlService(url, arqQuery);



        try {
            com.hp.hpl.jena.query.ResultSet results = qexec.execSelect();
            // Output query results
            //ResultSetFormatter.out(System.out, results, query);


            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();

                station_name = soln.getLiteral("Station_name").getString();
                sensor_serialno = soln.getLiteral("Sensor_serialno").getString();
                start_date = soln.getLiteral("Start_date").getString();
                end_date = soln.getLiteral("End_date").getString();
                project_name = soln.getLiteral("Project_name").getString();

                start_date_as_long = parse_date(start_date);
                //logger.info("start_date_as_long => " + start_date_as_long);

                end_date_as_long = parse_date(end_date);
                //logger.info("end_date_as_long => " + end_date_as_long);

                long epoch = System.currentTimeMillis();

                if (logger.isDebugEnabled()) {
                logger.warn("timestamp => " + epoch);
                logger.warn(
                        "* \""
                        + station_name
                        + "\"\t=>\t"
                        + sensor_serialno
                        + "\"\t=>\t"
                        + start_date
                        + "\"\t=>\t"
                        + project_name
                        + "\"\t=>\t"
                        + end_date
                );
                }

                postStreamElement(epoch, new Serializable[]{station_name, sensor_serialno, project_name, start_date_as_long, end_date_as_long});

                Thread.sleep(100); // avoid duplicate timestamps
            }


        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        } finally {
            qexec.close();
        }

    }

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
