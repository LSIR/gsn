package gsn.vsensor;

import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.beans.DataTypes;
import gsn.storage.DataEnumerator;
import gsn.storage.StorageManager;
import org.apache.log4j.Logger;
import org.apache.commons.lang.StringEscapeUtils;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.*;

import java.util.TreeMap;
import java.util.Vector;
import java.sql.*;
import java.sql.Statement;
import java.io.Serializable;


public class PermasenseMapper extends AbstractVirtualSensor {

    private static final transient Logger logger = Logger.getLogger(PermasenseMapper.class);

    private static final String TABLE_NAME_PARAM = "table_name";
    private static final String META_DATA_TABLE_NAME = "allmetadata";

    private String table_name;

    public boolean initialize() {
        VSensorConfig vsensor = getVirtualSensorConfiguration();
        TreeMap<String, String> params = vsensor.getMainClassInitialParams();
        table_name = params.get(TABLE_NAME_PARAM).toLowerCase();

        if (table_name == null) {
            logger.warn("The parameter > " + TABLE_NAME_PARAM + " < wasn't found in the virtual sensor definition file");
            return false;
        }

        return true;
    }

    public void finalize() {

    }

    public void dataAvailable ( String inputStreamName , StreamElement data ) {


        //logger.warn(data.toString());
        long timed = data.getTimeStamp();
        String a[] = data.getFieldNames();
        Byte types[] = data.getFieldTypes();

        /*
        for (int i=0;i<a.length;i++) {
            logger.warn(i+" : "+types[i]+" : "+a[i]+" = "+data.getData(a[i]));
        }
        */

        int sensor_id = (Integer)data.getData("HEADER_ORIGINATORID");
        String location = getLocation(sensor_id,timed,"");
        //String measuredProperty = getMeasuredProperty(sensor_id,timed,this.table_name,"PAYLOAD_SIBADCDIFF0");

        //logger.warn("Location: " + location);
        //logger.warn("Maping: " + measuredProperty);

        int length_w_location = types.length + 1 ;

        Byte[] types_w_location = new Byte[length_w_location];
        Serializable[] serialazable_w_location = new Serializable[length_w_location];
        String[] outputStructure_w_location = new String[length_w_location];

        for (int i=0;i<length_w_location-1;i++) {
            types_w_location[i] = types[i];
            serialazable_w_location[i] = data.getData()[i];
            outputStructure_w_location[i] = data.getFieldNames()[i];
        }

        types_w_location[length_w_location-1] = DataTypes.VARCHAR;
        serialazable_w_location[length_w_location-1] = location;
        outputStructure_w_location[length_w_location-1] = "location";

        StreamElement data_w_location = new StreamElement(outputStructure_w_location,types_w_location,serialazable_w_location,timed);

        dataProduced( data_w_location );

        //logger.warn("posting: "+data_w_location);
		if ( logger.isDebugEnabled( ) ) logger.debug( "Data received under the name: " + inputStreamName );

	}

    /*
    * executes an SQL query
    * and returns the result as string
    * */
    public String executeQuery(String query) {

        String s="";

        //logger.warn("metadata query: "+query);

        Connection conn;
        Statement stmt;
        java.sql.ResultSet rs;
        try {
             conn = StorageManager.getInstance().getConnection();
             stmt = conn.createStatement();
             rs = stmt.executeQuery(query);
            while (rs.next()){
                 s= rs.getString(1);
            }
            //logger.warn("result: "+s);
            rs.close();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            logger.error("ERROR IN EXECUTING, query: " + query);
            logger.error(e.getMessage(), e);
            return "";
        } finally {
        }

        return s;
    }


    /*
    * synchronizes metadata table
    * access semantic wiki through sparql
    * */
    public void syncMetaDataTable() {
       // update table META_DATA_TABLE_NAME
    }

    String getLocation(int sensor_id, long timed, String table_name) {

        StringBuilder sb = new StringBuilder();
        sb.append("select distinct location from ")
                .append(META_DATA_TABLE_NAME)
                .append(" where sensorid=")
                .append(sensor_id)
                .append(" and ")
                .append(timed)
                .append(" >= fromdate and ")
                .append(timed)
                .append(" < todate");

         return executeQuery(sb.toString());

        /*
        set @timed=1256753982105;
set @sensor_id=2007;
SELECT distinct location
FROM allmetadata
WHERE allmetadata.sensorid=@sensor_id and
      @timed>=fromdate and
  @timed<todate
        * */
/*
set @timed=1256753982105;
set @sensor_id=2007;
SELECT distinct @timed, allmetadata.location
FROM allmetadata
WHERE allmetadata.sensorid=@sensor_id and
      @timed>=allmetadata.fromdate and
	  @timed<allmetadata.todate

	  return allmetadata.location
	  */

    }

    String getMeasuredProperty(int sensor_id, long timed, String table_name, String parameter_name) {

        StringBuilder sb = new StringBuilder();
        sb.append("select distinct measured_property from ")
                .append(META_DATA_TABLE_NAME)
                .append(" where sensorid=")
                .append(sensor_id)
                .append(" and ")
                .append(timed)
                .append(" >= fromdate and ")
                .append(timed)
                .append(" < todate")
                .append(" and parameter_name=\"")
                .append(parameter_name)
                .append("\" and table_name=\"")
                .append(table_name)
                .append("\"");

         return executeQuery(sb.toString());

        /*
  set @db_param='PAYLOAD_SIBADCDIFF0';
  set @timed=1256753982105;
  set @sensor_id=2028;
  set @table_name='matterhorn_dozeradccomdiff_3933';
  SELECT distinct measured_property
  FROM allmetadata
  WHERE sensorid=@sensor_id and
        @timed>=fromdate and
        @timed<todate and
        parameter_name=@db_param and
        table_name=@table_name; */
    }

    // Sparql specific
    
    public static String MAIN_url = "http://www.swiss-experiment.ch/sparql/model";
    public static String TEST_url = "http://www.swiss-experiment.ch/sparql/model";

    public static String service_url = MAIN_url;

    public static String query_str =
            "PREFIX a: <http://128.178.156.248/index.php/Special:URIResolver/> \n" +
                    "PREFIX  xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                    "SELECT DISTINCT ?Project_name ?Station_name ?Sensor_serialno ?Start_date ?End_date\n" +
                    "WHERE {\n" +
                    "  ?page a:Property-3AStation_name ?Station_name .\n" +
                    "  ?page a:Property-3ASensor_serialno ?Sensor_serialno .\n" +
                    "  ?page a:Property-3AProject_name ?Project_name .\n" +
                    "  ?page a:Property-3AProject_name \"Permasense\"^^xsd:String .\n" +
                    " ?page a:Property-3AStart_date ?Start_date .\n" +
                    " ?page a:Property-3AEnd_date ?End_date .\n" +
                    "}";

    public static String query_str2 =
            "PREFIX a: <http://128.178.156.248/index.php/Special:URIResolver/> \n" +
                    "PREFIX  xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                    "SELECT DISTINCT ?Project_name ?Station_name ?Sensor_serialno ?Start_date ?End_date\n" +
                    "WHERE {\n" +
                    "  ?page a:Property-3AStation_name ?Station_name .\n" +
                    "  ?page a:Property-3ASensor_serialno ?Sensor_serialno .\n" +
                    "  ?page a:Property-3AProject_name ?Project_name .\n" +
                    "  ?page a:Property-3AProject_name \"Permasense\"^^xsd:String .\n" +
                    " ?page a:Property-3AStart_date ?Start_date .\n" +
                    " ?page a:Property-3AEnd_date ?End_date .\n" +
                    "}";

    public static void runRemoteQuery() {
        Query query = QueryFactory.create(query_str);

        QueryExecution qexec = QueryExecutionFactory.sparqlService(service_url, query);

        try {
            ResultSet results = qexec.execSelect();
            // Output query results
            ResultSetFormatter.out(System.out, results, query);
            /*
            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                RDFNode x = soln.get("varName");       // Get a result variable by name.
                Resource r = soln.getResource("VarR"); // Get a result variable - must be a resource
                Literal l = soln.getLiteral("VarL");   // Get a result variable - must be a literal
            }
            */
        } finally {
            qexec.close();
        }

    }
}
