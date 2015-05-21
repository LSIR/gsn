package gsn.data.discovery

import com.hp.hpl.jena.query.QueryExecutionFactory

import com.hp.hpl.jena.rdf.model.Statement
import com.hp.hpl.jena.update.UpdateExecutionFactory
import com.hp.hpl.jena.update.UpdateFactory
import com.hp.hpl.jena.vocabulary.RDF
import com.hp.hpl.jena.vocabulary.RDFS
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import java.net.ConnectException
import java.net.URL
import java.net.HttpURLConnection
import java.io.IOException
import com.sun.org.apache.xerces.internal.impl.PropertyManager
import org.apache.commons.validator.routines.UrlValidator

import gsn.data.discovery.util.DataFormatter

/**
 * Allows to add and get data from Fuseki
 */
class DataManager(sparqlServiceProperties:String, 
    sparqlServiceMapping:String, 
    sparqlServiceSensors:String, 
    baseUri:String) {
  
  val ssxUri = "http://ssx.ch#"
  val ssnUri = "http://purl.oclc.org/NET/ssnx/ssn#"
  
  val ssnHasOutput = "<" + ssnUri + "hasOutput" + ">"
  
  val prefixSsx = "PREFIX ssx: <" + ssxUri + ">\n"
  val prefixSsn = "PREFIX ssn: <" + ssnUri + ">\n"
  val prefixRdf = "PREFIX rdf: <" + RDF.getURI() + ">\n"
  val prefixRdfs = "PREFIX rdfs: <" + RDFS.getURI() + ">\n"
  
  /**
   * Returns the observed property URI for the given property name
   */
  def getMappingForProperty(propertyName:String):String = {
    val query = prefixSsn + prefixSsx + 
      "SELECT ?s \n" + 
      "WHERE { \n" + 
      "?s ssx:dbField \"" + 
      propertyName + "\"\n"  + 
      "}";

    var resultUri = "NOT_FOUND";

    val queryExec = QueryExecutionFactory.sparqlService(sparqlServiceMapping + "/query", query);
    val results = queryExec.execSelect();

    // Takes the first result if any
    if (results.hasNext()) {
      resultUri = results.nextSolution().get("s").toString();
    }
    queryExec.close();
    resultUri;
  }
  
  /**
   * Add new mapping to Fuseki. Maps "property" to the observed property designated by its URI
   */
  def addNewMapping(property:String, observedPropertyUri:String) {
    val name = "output_" + property.replaceAll("\\s", "_") + "_" + DataFormatter.extractFragmentId(observedPropertyUri)
    
    val insert = "INSERT DATA {\n" + 
      "<" + baseUri + name + "> " +
                                    "<" + ssxUri + "dbField> \"" + property + "\" ; \n" +
                                    "<" + ssnUri + "forProperty> <" + observedPropertyUri + "> \n" +
      "}" 
                                    
    val request = UpdateFactory.create()
    request.add(insert)

    val updateProc = UpdateExecutionFactory.createRemote(request, sparqlServiceMapping + "/update")
    updateProc.execute()
  }

  /**
   * Checks whether a mapping exists for the provided property
   */
  def mappingExists(property:String):Boolean = {
    val query = "ASK { ?subject <" + ssxUri + "dbField> \"" + property + "\"}"
    val queryExec = QueryExecutionFactory.sparqlService(sparqlServiceMapping + "/query", query)
    val result = queryExec.execAsk()
    queryExec.close()
    result
  }

  /**
   * Returns the attributes of the output specified
   */
  def getOutputDetails(outputUri:String):(String, List[(String,String)]) = {
    val details = scala.collection.mutable.ListBuffer[(String,String)]()
    
    var property:String = ""
    val queryProperty = prefixSsx + "SELECT ?property \n WHERE { \n <" + outputUri + "> ssx:dbField ?property \n}"
    val queryExecProperty = QueryExecutionFactory.sparqlService(sparqlServiceMapping + "/query", queryProperty)
    val resultsProperty = queryExecProperty.execSelect();
    // Takes the first result if any
    if (resultsProperty.hasNext()) {
      val sol = resultsProperty.nextSolution();
      property = sol.get("property").toString()
    }
    queryExecProperty.close()
    
    val queryObsProperty = prefixSsn + "SELECT ?obsProperty WHERE { \n <" + outputUri + "> ssn:forProperty  ?obsProperty \n}"
    val queryExecObsProperty = QueryExecutionFactory.sparqlService(sparqlServiceMapping + "/query", queryObsProperty)
    val resultsObsProperty = queryExecObsProperty.execSelect()
    // Takes the first result if any
    if (resultsObsProperty.hasNext()) {
      val sol = resultsObsProperty.nextSolution()
      val obsPropertyUri = sol.get("obsProperty").toString()
      val obsPropertyLabel = getObservedPropertyLabel(sol.get("obsProperty").toString())
      details ++= List(("obsProperty", obsPropertyLabel))
      details ++= List(("obsPropertyUri", obsPropertyUri))
    }
    queryExecProperty.close()
    
    (property, details.toList)
  }
  
  def getUnitBySymbol(symbol:String):(String,String) = {
    val queryUnitStd = "SELECT ?unit ?label " +
                        "WHERE { " + 
                          "?unit <http://purl.oclc.org/NET/ssnx/qu/qu#symbol> ?symbol . " +
                          "?unit <http://www.w3.org/2000/01/rdf-schema#label> ?label . " +
                          "?unit <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?dim . " +
                          "?dim <http://www.w3.org/2000/01/rdf-schema#subClassOf> <http://purl.oclc.org/NET/ssnx/qu/qu#Unit> " +
                          "FILTER (?symbol = \"" + symbol + "\")" +
                        "}";
    val queryExecUnit = QueryExecutionFactory.sparqlService(sparqlServiceProperties + "/query", queryUnitStd)
    val resultsUnit = queryExecUnit.execSelect()
    var unitStdUri = ""
    var unitStd = ""
    // Takes the first result if any
    if (resultsUnit.hasNext()) {
      val s = resultsUnit.nextSolution()
      unitStdUri = s.get("unit").toString()
      unitStd = s.getLiteral("label").getString()
    }
    queryExecUnit.close()
    (unitStdUri,unitStd)
  }
  
  /**
   * Returns the mappings for the specified virtual sensor
   */
  def getMappingsForSensor(sensorName:String):Map[String,List[(String,String)]] = {
    val query = "SELECT ?output\n WHERE {\n <" + baseUri + sensorName + "> " + ssnHasOutput + " ?output \n}"
    val mappings = scala.collection.mutable.Map[String, List[(String,String)]]()
    val queryExec = QueryExecutionFactory.sparqlService(sparqlServiceSensors + "/query", query)
    val results =  queryExec.execSelect()
     
    for(r <- results; if r.get("output") != null && r.get("output").isURIResource()) {
      val (property,details) = getOutputDetails(r.get("output").toString())
      mappings.put(property, details)
    }
    queryExec.close()
    mappings.toMap
  }
  
  /**
   * Checks whether the observed property exists in Fuseki, based on its URI
   */
  def observedPropertyExistsByUri(propertyUri:String):Boolean = {
    if(checkUri(propertyUri)) {
      val query = prefixRdf + "ASK { <" + propertyUri + "> rdf:type ?o }"
      askQuery(query)
    } else {
      false
    }
  }
  
  /**
   * Checks whether the observed property exists in Fuseki, based on its label
   * Note: the label isn't a unique identifier of a property, therefore the method that takes the URI as parameter
   * should be preferred when possible.
   */
  def observedPropertyExistsByLabel(label:String):Boolean = {
    val query = prefixRdfs + "ASK { ?p rdfs:label \"" + label + "\" }"
    askQuery(query)
  }
  
  private def askQuery(query:String):Boolean = {
    val queryExec = QueryExecutionFactory.sparqlService(sparqlServiceProperties + "/query", query)
    val result = queryExec.execAsk()
    queryExec.close()
    result
  }
  
  private def checkUri(uri:String):Boolean = {
    val schemes = Array("http")
    val urlValidator = new UrlValidator(schemes)
    urlValidator.isValid(uri)
  }

  /**
   * Return the label of the observed property specified by the URI propertyUri
   */
  def getObservedPropertyLabel(propertyUri:String):String = {
     val query = prefixRdfs + "SELECT ?label \n WHERE { \n <" + propertyUri + "> rdfs:label ?label \n }"

     // Initialize with the name from the URI in case there is no label
    var label:String = parsePropertyNameFromUri(propertyUri)

    val qexec = QueryExecutionFactory.sparqlService(sparqlServiceProperties + "/query", query)
    val results = qexec.execSelect()

    if (results.hasNext()) {
      label = results.nextSolution().get("label").toString()
    }
    qexec.close()
    label
  }
  
  /**
   * Find and return the URI of the property having the exact label provided: return first result
   */
  def findObservedPropertyByLabelExactMatch(label:String):String = {
    val query = "PREFIX rdfs: <" + RDFS.getURI() + "> \n" +
      "SELECT ?s \n" + 
      "WHERE { \n" +"?s rdfs:label \"" + label + "\" \n}";

    val qexec = QueryExecutionFactory.sparqlService(sparqlServiceProperties + "/query", query);
    val results = qexec.execSelect();

    var resultUri = "NOT_FOUND"
    
    // Takes the first result if any
    if (results.hasNext()) {
      val sol = results.nextSolution().get("s");
      if (sol != null) {
        resultUri = sol.toString()
      }
    }
    qexec.close();

    resultUri;
  }
  
  /**
   * Retrieve the last part of the URI to get the property name, e.g: http:://slf.ch#air_temperature
   * will return air_temperature
   */
  private def parsePropertyNameFromUri(propertyUri:String):String = {
    val result = propertyUri.split("#")
    if (result.length >= 2) result(1) else propertyUri
  }

  /**
   * Checks whether the virtual sensor with the URI vsUri exists
   */
  def virtualSensorExists(vsUri:String):Boolean = {
    val query = prefixRdf + "ASK { <" + vsUri + "> rdf:type <" + ssnUri + "Sensor>}"
    val queryExec = QueryExecutionFactory.sparqlService(sparqlServiceSensors + "/query", query)
    val result = queryExec.execAsk()
    queryExec.close()
    result
  }

  /**
   * Insert new virtual sensor statement into Fuseki virtual sensors dataset
   */
  def addNewVirtualSensorStatement(s:Statement) = {
      var insert = "INSERT DATA {\n "
      if (s.getObject().isLiteral()) {
        insert += "<" + s.getSubject().getURI() + "> <" + s.getPredicate().getURI() + "> \"" + s.getLiteral().getString() + "\" "
      } else {
        insert += "<" + s.getSubject().getURI() + "> <" + s.getPredicate().getURI() + "> <" + s.getObject().toString() + "> "
      }
      insert += "\n }"

    val request = UpdateFactory.create()
    request.add(insert)

    val updateProc = UpdateExecutionFactory.createRemote(request, sparqlServiceSensors + "/update")
    updateProc.execute()
  }
  
  /**
   * Fetch all virtual sensor that observe the given property
   * return a list of tuples of the form (vs name, longitude, latitude, altitude)
   */
  def getVirtualSensorsForObservedProperty(obsPropUri:String):List[(String, String, Double, Double, Option[Double])] = {
    checkUri(obsPropUri)
    val query = "SELECT ?sensor ?column_name ?long ?lat ?alt \n" +
                "WHERE { \n" +
                    "?output <http://purl.oclc.org/NET/ssnx/ssn#forProperty> <"+obsPropUri+"> . \n" +
                    "?output <http://ssx.ch#dbField> ?column_name \n" +
                    "SERVICE <"+sparqlServiceSensors+"> { \n" +
                        "?sensor <http://purl.oclc.org/NET/ssnx/ssn#hasOutput> ?output . \n" +
                        "?sensor <http://www.w3.org/2003/01/geo/wgs84_pos#long> ?long . \n" +
                        "?sensor <http://www.w3.org/2003/01/geo/wgs84_pos#lat> ?lat . \n" +
                        "OPTIONAL { ?sensor <http://www.w3.org/2003/01/geo/wgs84_pos#alt> ?alt . } \n" +
                    "}" +
                "}"
                    
    val qexec = QueryExecutionFactory.sparqlService(sparqlServiceMapping + "/query", query);
    val results = qexec.execSelect();

    var resultsSeq = new ListBuffer[(String, String, Double, Double, Option[Double])]()
    
    while (results.hasNext()) {
      val s = results.nextSolution();
      val vsName = DataFormatter.extractFragmentId(s.get("sensor").toString())
      val columnName = DataFormatter.removeQuotes(s.get("column_name").toString())
      val long = DataFormatter.removeQuotes(s.get("long").toString()).toDouble
      val lat = DataFormatter.removeQuotes(s.get("lat").toString()).toDouble
      val alt:Option[Double] = if (s.get("alt") != null) Option(DataFormatter.removeQuotes(s.get("alt").toString()).toDouble) else None
      val tuple = (vsName, columnName, long, lat, alt)
      resultsSeq += tuple
    }
    qexec.close();

    resultsSeq.toList
  }
}

object DataManager {
  
}
