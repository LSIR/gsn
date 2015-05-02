package gsn.data.discovery

import com.hp.hpl.jena.query.QueryExecutionFactory
import com.hp.hpl.jena.rdf.model.Statement
import com.hp.hpl.jena.update.UpdateExecutionFactory
import com.hp.hpl.jena.update.UpdateFactory
import com.hp.hpl.jena.vocabulary.RDF
import com.hp.hpl.jena.vocabulary.RDFS
import scala.collection.JavaConversions._
import java.net.ConnectException
import java.net.URL
import java.net.HttpURLConnection
import java.io.IOException
import com.sun.org.apache.xerces.internal.impl.PropertyManager

/**
 * Allows to add and get data from Fuseki
 */
class PropertiesManager(sparqlServiceProperties:String, 
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
    val name = "output_" + property.replaceAll("\\s", "_") + "_" + observedPropertyUri.split("#")(1)
    
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
      val obsProperty = getObservedPropertyLabel(sol.get("obsProperty").toString())
      details ++= List(("obsProperty", obsProperty))
    }
    queryExecProperty.close()
    
    (property, details.toList)
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
   * Checks whether the observed property exists in Fuseki
   */
  def observedPropertyExists(propertyUri:String):Boolean = {
    val query = prefixRdf + "ASK { <" + propertyUri + "> rdf:type ?o }"
    val queryExec = QueryExecutionFactory.sparqlService(sparqlServiceProperties + "/query", query)
    val result = queryExec.execAsk()
    queryExec.close()
    result
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

    return resultUri;
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
    val insert = "INSERT DATA {\n " +
      "<" + s.getSubject().getURI() + "> <" + s.getPredicate().getURI() + "> <" + s.getObject().toString() + "> " +
      "\n }"

      val request = UpdateFactory.create()
    request.add(insert)

    val updateProc = UpdateExecutionFactory.createRemote(request, sparqlServiceSensors + "/update")
    updateProc.execute()
  }
}

object PropertiesManager {
}
