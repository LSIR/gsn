package gsn.tests;

import com.hp.hpl.jena.query.*;

import com.hp.hpl.jena.rdf.model.*;

public class SparqlQuery {

    public static String MAIN_url = "http://www.swiss-experiment.ch/sparql/model";
    public static String TEST_url = "http://www.swiss-experiment.ch/sparql/model";

    public static String service_url = MAIN_url;

    public static String query_str =
            "PREFIX a: <http://128.178.156.248/index.php/Special:URIResolver/>\n" +
                    "PREFIX  xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                    "SELECT DISTINCT ?Project_name ?Station_name ?Sensor_serialno ?Start_date ?End_date ?Measured_Quantities ?Database_table_name  ?Database_parameter_name\n" +
                    "WHERE {\n" +
                    "  ?page a:Property-3AStation_name ?Station_name .\n" +
                    "  ?page a:Property-3ASensor_serialno ?Sensor_serialno .\n" +
                    "  ?page a:Property-3AProject_name ?Project_name .\n" +
                    "  ?page a:Property-3AProject_name \"Permasense\"^^xsd:String .\n" +
                    " ?page a:Property-3AStart_date ?Start_date .\n" +
                    " ?page a:Property-3AEnd_date ?End_date .\n" +
                    " ?page a:Property-3AMeasured_Quantities ?Measured_Quantities .\n" +
                    " ?page a:Property-3ADatabase_table_name  ?Database_table_name   .\n" +
                    " ?page a:Property-3ADatabase_parameter_name ?Database_parameter_name .\n" +
                    "\n" +
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

    public static void main(String[] args) {
        System.out.println(query_str);

        runRemoteQuery();

    }
}


