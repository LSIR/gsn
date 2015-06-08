package gsn.data.discovery

import com.hp.hpl.jena.query.QueryExecutionFactory


import com.hp.hpl.jena.rdf.model.Statement
import com.hp.hpl.jena.update.UpdateExecutionFactory
import com.hp.hpl.jena.update.UpdateFactory
import com.hp.hpl.jena.vocabulary.RDF
import com.hp.hpl.jena.vocabulary.RDFS
import com.hp.hpl.jena.query.QuerySolution
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import java.net.ConnectException
import java.net.URL
import java.net.HttpURLConnection
import java.io.IOException
import com.sun.org.apache.xerces.internal.impl.PropertyManager
import org.apache.commons.validator.routines.UrlValidator

import gsn.data.discovery.util.DataFormatter

import java.io.File
import java.io.InputStream
import org.apache.commons.io.IOUtils
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.RDFFormat
import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.rdf.model.ModelFactory
import com.hp.hpl.jena.rdf.model.Property
import com.hp.hpl.jena.rdf.model.Resource
import com.hp.hpl.jena.rdf.model.Statement
import com.hp.hpl.jena.vocabulary.OWL
import com.hp.hpl.jena.vocabulary.RDF
import scala.io.Source
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import gsn.data.discovery.util.DataFormatter
import java.io.FileInputStream
import play.api.libs.json.Json
import com.typesafe.config._
import scala.xml.Node
import scala.xml.NodeSeq
import play.api.libs.json.JsValue

/**
 * API for metadata store Fuseki
 */
class MetadataManager(sparqlServiceProperties:String, 
                      sparqlServiceMapping:String, 
                      sparqlServiceSensors:String, 
                      baseUri:String) {
  
  val ssxUri = "http://ssx.ch#"
  val ssnUri = "http://purl.oclc.org/NET/ssnx/ssn#"
  val wgs84Uri = "http://www.w3.org/2003/01/geo/wgs84_pos#"
  
  val ssnHasOutput = "<" + ssnUri + "hasOutput" + ">"
  
  val prefixSsx = "PREFIX ssx: <" + ssxUri + ">\n"
  val prefixSsn = "PREFIX ssn: <" + ssnUri + ">\n"
  val prefixRdf = "PREFIX rdf: <" + RDF.getURI() + ">\n"
  val prefixRdfs = "PREFIX rdfs: <" + RDFS.getURI() + ">\n"
  
  /**
   * Create new resource output for each mapping
   * The CSV file has 2 columns:
   * - the first one contains the column names, like <i>air_tmp</i>
   * - the second on contains the observed properties URIs or the label, like <i>http://purl.oclc.org/NET/ssnx/cf/cf-property#air_temperature</i> 
   * or <i>air temperature</i> respectively
   */
  def importPropertyMappingsFromCsv(file:File) {
    
    val mappings = Source.fromFile(file).getLines().map { line => line.split(",") }.filter { a => a.length >= 2 }
    
    mappings.foreach { m => 
      val columnNamesToMap:String = m(0).toLowerCase();
      val obsProperty:String = m(1);
      
      // Create the output only for existing observed properties
      if (observedPropertyExistsByUri(obsProperty)) {
        // In this case, obsProperty is already a URI
        addNewOutput(columnNamesToMap, obsProperty)
      } else if (observedPropertyExistsByLabel(obsProperty)) {
        // Find the URI of the first property found with the given label
        val obsPropertyUri = findObservedPropertyByLabelExactMatch(obsProperty.replaceAll("_", "\\s").toLowerCase())
        addNewOutput(columnNamesToMap, obsPropertyUri)
      } else {
        println("WARNING: (Mapping ignored) Observed property: " + obsProperty + " doesn't exist in the model: no output created");
      }
    }
  }
  
  /**
   * Add the virtual sensor found in the provided JSON file to the model
   */
  def addVirtualSensorFromJSON(file:File) {
    
    def extractOptionalStringFromJsValue(jsVal:JsValue):Option[String] = {
      if (jsVal != null) Option(DataFormatter.removeQuotes(jsVal.as[String])) else None
    }
    
    val json = Json.parse(Source.fromFile(file).mkString)
    val vsName = DataFormatter.removeQuotes((json \ "properties" \ "vs_name").as[String].toLowerCase())
    val fields = json \ "properties" \ "fields" \\ "name"
    val longitude = extractOptionalStringFromJsValue((json \ "properties" \ "longitude"))
    val latitude = extractOptionalStringFromJsValue((json \ "properties" \ "latitude"))
    val altitude = extractOptionalStringFromJsValue((json \ "properties" \ "altitude"))
    
    addVirtualSensor(vsName, longitude, latitude, altitude, fields.map { x => x.as[String] } toList)
  }
  
  /**
   * Add the virtual sensor found in the provided XML file to the model
   */
  def addVirtualSensorFromXML(file:File) {
    
    def extractOptionalStringFromNodeSeq(nodeSeq:NodeSeq, property:String):Option[String] = {
      val seq = nodeSeq.filter { x => extractAttributeValue(x).equalsIgnoreCase(property) }
      if (seq.size >= 1) Option(seq.head.text) else None
    }
    
    def extractAttributeValue(n:Node):String = {
      n.attribute("key") match {
        case s if s.size >= 1 => s.head.text
        case _ => null
      }
    }
    
    val mainNode = xml.XML.loadFile(file)
    val vsName = (mainNode \ "@name").text.toLowerCase()
    val fields = (mainNode \ "processing-class" \ "output-structure" \ "field").map {_ \ "@name"}
    val longitude = extractOptionalStringFromNodeSeq((mainNode \ "addressing" \ "predicate"), "longitude")
    val latitude = extractOptionalStringFromNodeSeq((mainNode \ "addressing" \ "predicate"), "latitude")
    val altitude = extractOptionalStringFromNodeSeq((mainNode \ "addressing" \ "predicate"), "altitude")
    
    addVirtualSensor(vsName, longitude, latitude, altitude, fields.map { x => x.text } toList)
  }
  
  private def addVirtualSensor(vsName:String, 
                              longitude:Option[String], 
                              latitude:Option[String], 
                              altitude:Option[String], 
                              fields:List[String]) {
    
      val vsModel = ModelFactory.createDefaultModel()
      val newSensor = vsModel.createResource(baseUri + vsName)
      
      val ssnSensorType = vsModel.createResource(ssnUri + "Sensor")
      newSensor.addProperty(RDF.`type`, ssnSensorType)
      
      val namedIndividualType = vsModel.createResource(OWL.getURI() + "NamedIndividual")
      newSensor.addProperty(RDF.`type`, namedIndividualType)
      
      if (longitude.isDefined) {
        val longitudeProperty = vsModel.createProperty(wgs84Uri, "long")
        newSensor.addLiteral(longitudeProperty, longitude.get)
      }
      if (latitude.isDefined) {
        val latitudeProperty = vsModel.createProperty(wgs84Uri, "lat")
        newSensor.addProperty(latitudeProperty, latitude.get)
      }
      if (altitude.isDefined) {
        val altitudeProperty = vsModel.createProperty(wgs84Uri, "alt")
        newSensor.addProperty(altitudeProperty, altitude.get)
      }
      
      // Assign all outputs to the virtual sensor
      val ssnHasOutput = vsModel.createProperty(ssnUri, "hasOutput")
      
      for (value <- fields) {
        if (outputExists(value)) {
          val obsPropertyUri = getMappingForColumnName(value)
          newSensor.addProperty(ssnHasOutput, vsModel.createResource(obsPropertyUri))
        } else {
          println("WARNING: There is no mapping for property " + value)
        }
      }
      // Write all statements from the temporary model to Fuseki
      vsModel.listStatements().toList().foreach { s => 
        addNewVirtualSensorStatement(s)
      }
  }
  
  /**
   * Returns the observed property URI for the given column name
   */
  def getMappingForColumnName(columnName:String): String = {
    val query = prefixSsn + prefixSsx + 
      "SELECT ?s \n" + 
      "WHERE { \n" + 
      "?s ssx:dbField \"" + 
      columnName + "\"\n"  + 
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
   * Create new virtual sensor output
   * <li><i>columnName</i> is the name of the database field where the data of the output is stored
   * it corresponds to the name of the <i>field</i> in the virtual sensor definition file</li>
   * <li><i>observedPropertyURI</i> defines what produces the output</li>
   */
  def addNewOutput(columnName:String, observedPropertyUri:String) {
    val name = "output_" + columnName.replaceAll("\\s", "_") + "_" + DataFormatter.extractFragmentId(observedPropertyUri)
    
    val insert = "INSERT DATA {\n" + 
      "<" + baseUri + name + "> " +
                                    "<" + ssxUri + "dbField> \"" + columnName + "\" ; \n" +
                                    "<" + ssnUri + "forProperty> <" + observedPropertyUri + "> \n" +
      "}" 
                                    
    val request = UpdateFactory.create()
    request.add(insert)

    val updateProc = UpdateExecutionFactory.createRemote(request, sparqlServiceMapping + "/update")
    updateProc.execute()
  }

  /**
   * Checks whether an output exists for the provided column name
   */
  def outputExists(columnName:String): Boolean = {
    val query = "ASK { ?output <" + ssxUri + "dbField> \"" + columnName + "\"}"
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
    
    var columnName:String = ""
    val queryColumnName = prefixSsx + "SELECT ?columnName \n WHERE { \n <" + outputUri + "> ssx:dbField ?columnName \n}"
    val queryExecColumnName = QueryExecutionFactory.sparqlService(sparqlServiceMapping + "/query", queryColumnName)
    val resultsColumnName = queryExecColumnName.execSelect();
    
    // Takes the first result if any
    if (resultsColumnName.hasNext()) {
      val sol = resultsColumnName.nextSolution();
      columnName = sol.get("columnName").toString()
    }
    queryExecColumnName.close()
    
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
    queryExecColumnName.close()
    
    (columnName, details.toList)
  }
  
  /**
   * Returns the URI and label of the unit corresponding to the provided symbol 
   */
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
   * Returns the outputs of the specified virtual sensor
   */
  def getOutputsForSensor(sensorName:String): Map[String,List[(String,String)]] = {
    val query = "SELECT ?output\n WHERE {\n <" + baseUri + sensorName + "> " + ssnHasOutput + " ?output \n}"
    val outputs = scala.collection.mutable.Map[String, List[(String,String)]]()
    val queryExec = QueryExecutionFactory.sparqlService(sparqlServiceSensors + "/query", query)
    val results =  queryExec.execSelect()
     
    for(r <- results; if r.get("output") != null && r.get("output").isURIResource()) {
      val (property,details) = getOutputDetails(r.get("output").toString())
      outputs.put(property, details)
    }
    queryExec.close()
    outputs.toMap
  }
  
  /**
   * Checks whether the observed property exists in Fuseki, based on its URI
   */
  def observedPropertyExistsByUri(obsPropertyUri:String):Boolean = {
    if(MetadataManager.checkUri(obsPropertyUri)) {
      val query = prefixRdf + "ASK { <" + obsPropertyUri + "> rdf:type ?o }"
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
  
  /**
   * Return the label of the observed property specified by the URI propertyUri
   */
  def getObservedPropertyLabel(obsPropertyUri:String):String = {
     val query = prefixRdfs + "SELECT ?label \n WHERE { \n <" + obsPropertyUri + "> rdfs:label ?label \n }"

     // Initialize with the name from the URI in case there is no label
    var label:String = MetadataManager.parsePropertyNameFromUri(obsPropertyUri)

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
   * Fetch all virtual sensors that observe the given property
   * return a list of tuples of the form (vs name, column name, longitude, latitude, altitude)
   */
  def getVirtualSensorsForObservedProperty(obsPropUri:String):List[(String, String, Option[String], Option[String], Option[String])] = {
    MetadataManager.checkUri(obsPropUri)
    
    val query = "SELECT ?sensor ?columnName ?long ?lat ?alt \n" +
                "WHERE { \n" +
                    "?output <http://purl.oclc.org/NET/ssnx/ssn#forProperty> <"+obsPropUri+"> . \n" +
                    "?output <http://ssx.ch#dbField> ?columnName \n" +
                    "SERVICE <"+sparqlServiceSensors+"> { \n" +
                        "?sensor <http://purl.oclc.org/NET/ssnx/ssn#hasOutput> ?output . \n" +
                        "OPTIONAL { ?sensor <http://www.w3.org/2003/01/geo/wgs84_pos#long> ?long . } \n" +
                        "OPTIONAL { ?sensor <http://www.w3.org/2003/01/geo/wgs84_pos#lat> ?lat . } \n" +
                        "OPTIONAL { ?sensor <http://www.w3.org/2003/01/geo/wgs84_pos#alt> ?alt . } \n" +
                    "}" +
                "}"
                    
    val qexec = QueryExecutionFactory.sparqlService(sparqlServiceMapping + "/query", query);
    val results = qexec.execSelect();

    var resultsSeq = new ListBuffer[(String, String, Option[String], Option[String], Option[String])]()
    
    while (results.hasNext()) {
      val s = results.nextSolution();
      val vsName = DataFormatter.extractFragmentId(s.get("sensor").toString())
      val columnName = DataFormatter.removeQuotes(s.get("columnName").toString())
      val long = getOptionalStringFromResult(s, "long")
      val lat = getOptionalStringFromResult(s, "lat")
      val alt = getOptionalStringFromResult(s, "alt")
      val tuple = (vsName, columnName, long, lat, alt)
      resultsSeq += tuple
    }
    
    def getOptionalStringFromResult(result:QuerySolution, property:String):Option[String] = {
      val node = result.get(property)
      if (node != null) Option(DataFormatter.removeQuotes(node.toString())) else None
    }
    
    qexec.close();

    resultsSeq.toList
  }
}

object MetadataManager {
  /**
   * Retrieve the last part of the URI to get the property name, e.g: http:://slf.ch#air_temperature
   * will return air_temperature
   */
  private def parsePropertyNameFromUri(propertyUri:String):String = {
    DataFormatter.extractFragmentId(propertyUri)
  }
  
  private def checkUri(uri:String):Boolean = {
    val schemes = Array("http")
    val urlValidator = new UrlValidator(schemes)
    urlValidator.isValid(uri)
  }
}
