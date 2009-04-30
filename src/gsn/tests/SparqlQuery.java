package gsn.tests;

import com.hp.hpl.jena.query.*;

import com.hp.hpl.jena.rdf.model.*;

public class SparqlQuery {

    public static String service_url = "http://www.swiss-experiment.ch/sparql/model";

    public static String query_str = "PREFIX a: <http://128.178.131.25/index.php/Special:URIResolver/>\n" +
            "PREFIX  xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "SELECT ?action ?addinfo ?start_date ?end_date WHERE {\n" +
            "\n" +
            "?page a:Property-3AStation_name \"wan5\"^^xsd:String .\n" +
            "?page a:Property-3ADatabase_parameter_name ?parameter_name .\n" +
            "?page1 a:Property-3ADatabase_parameter_name ?parameter_name .\n" +
            "?page1 a:Property-3AAction ?action .\n" +
            "?page1 a:Property-3AStart_date ?start_date .\n" +
            "?page1 a:Property-3AEnd_date ?end_date .\n" +
            "?page1 a:Property-3AAddinfo ?addinfo .\n" +
            "?page1 a:Property-3AStation_name ?station_name .\n" +
            "FILTER ( xsd:dateTime(?start_date) >= xsd:dateTime(\"0001-01-01T00:00:00\")) .\n" +
            "FILTER (xsd:dateTime(?end_date) <= xsd:dateTime(\"9999-12-31T23:59:59\")) .\n" +
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


