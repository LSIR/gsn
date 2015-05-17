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
import gsn.data.discovery.PropertiesManager
import java.io.FileInputStream
import gsn.data.discovery.util.ReadResource
import play.api.libs.json.Json
import com.typesafe.config._
import scala.xml.Node

class PropertyMappingsManager(propertiesManager:PropertiesManager, baseUri:String) extends ReadResource {
  
  val ssnUri = "http://purl.oclc.org/NET/ssnx/ssn#"
  val wgs84Uri = "http://www.w3.org/2003/01/geo/wgs84_pos#"
  
  /**
   * Create new property mappings in Fuseki
   * The CSV file has 2 columns:
   * - the first one contains the properties, like air_tmp
   * - the second on contains the observed properties URIs or the label, like http://purl.oclc.org/NET/ssnx/cf/cf-property#air_temperature 
   * or air temperature respectively
   */
  def importPropertyMappingsFromCsv(file:File) {
    
    val mappings = Source.fromFile(file).getLines().map { line => line.split(",") }.filter { a => a.length >= 2 }
    
    mappings.foreach { m => 
      val propertyToMap:String = m(0).toLowerCase();
      val obsProperty:String = m(1);
      
      // Create the mapping only for existing observed properties
      if (propertiesManager.observedPropertyExistsByUri(obsProperty)) {
        // In this case, obsProperty is already a URI
        propertiesManager.addNewMapping(propertyToMap, obsProperty)
      } else if (propertiesManager.observedPropertyExistsByLabel(obsProperty)) {
        // Find the URI of the first property found with the given label
        val propertyUri = propertiesManager.findObservedPropertyByLabelExactMatch(obsProperty.replaceAll("_", "\\s").toLowerCase())
        propertiesManager.addNewMapping(propertyToMap, propertyUri)
      } else {
        println("WARNING:(mapping) Property: " + obsProperty + " doesn't exist in the model: ignored");
      }
    }
  }
  
  /**
   * Add the virtual sensor found in the provided JSON file to the model
   */
  def addVirtualSensorFromJSON(file:File) {
    
      val json = Json.parse(Source.fromFile(file).mkString)
      val vsName = PropertyMappingsManager.removeQuotes((json \ "properties" \ "vs_name").as[String].toLowerCase())
      val fields = json \ "properties" \ "fields" \\ "name"
      val longitude = PropertyMappingsManager.removeQuotes((json \ "properties" \ "longitude").as[String])
      val latitude = PropertyMappingsManager.removeQuotes((json \ "properties" \ "latitude").as[String])
      val altitude = PropertyMappingsManager.removeQuotes((json \ "properties" \ "altitude").as[String])
      
      addVirtualSensor(vsName, longitude, latitude, altitude, fields.map { x => x.as[String] } toList)
  }
  
  /**
   * Add the virtual sensor found in the provided XML file to the model
   */
  def addVirtualSensorFromXML(file:File) {
    
      val mainNode = xml.XML.loadFile(file)
      val vsName = (mainNode \ "@name").text.toLowerCase()
      val fields = (mainNode \ "processing-class" \ "output-structure" \ "field").map {_ \ "@name"}
      val longitude = {
        val elem = (mainNode \ "addressing" \ "predicate").filter { x => extractAttributeValue(x).equalsIgnoreCase("longitude")}
        if (elem.size >= 1) elem.head.text else ""
      }
      val latitude = {
        val elem = (mainNode \ "addressing" \ "predicate").filter { x => extractAttributeValue(x).equalsIgnoreCase("latitude")}
        if (elem.size >= 1) elem.head.text else ""
      }
      val altitude = {
        val elem = (mainNode \ "addressing" \ "predicate").filter { x => extractAttributeValue(x).equalsIgnoreCase("altitude")}
        if (elem.size >= 1) elem.head.text else ""
      }
      
      addVirtualSensor(vsName, longitude, latitude, altitude, fields.map { x => x.text } toList)
  }
  
  private def extractAttributeValue(n:Node):String = {
    n.attribute("key") match {
      case s if s.size >= 1 => s.head.text
      case _ => null
    }
  }
  
  private def addVirtualSensor(vsName:String, longitude:String, latitude:String, altitude:String, fields:List[String]) {
    
      val vsModel = ModelFactory.createDefaultModel()
      val newSensor = vsModel.createResource(baseUri + vsName)
      
      val ssnSensorType = vsModel.createResource(ssnUri + "Sensor")
      newSensor.addProperty(RDF.`type`, ssnSensorType)
      
      val namedIndividualType = vsModel.createResource(OWL.getURI() + "NamedIndividual")
      newSensor.addProperty(RDF.`type`, namedIndividualType)
      
      if (!longitude.equals("")) {
        val longitudeProperty = vsModel.createProperty(wgs84Uri, "long")
        newSensor.addLiteral(longitudeProperty, longitude)
      }
      if (!latitude.equals("")) {
        val latitudeProperty = vsModel.createProperty(wgs84Uri, "lat")
        newSensor.addProperty(latitudeProperty, latitude)
      }
      if (!altitude.equals("")) {
        val altitudeProperty = vsModel.createProperty(wgs84Uri, "alt")
        newSensor.addProperty(altitudeProperty, altitude)
      }
        
      val ssnObserves = vsModel.createProperty(ssnUri, "observes")
      val ssnHasOutput = vsModel.createProperty(ssnUri, "hasOutput")
      
      for (value <- fields) {
        if (propertiesManager.mappingExists(value)) {
          val obsPropertyUri = propertiesManager.getMappingForProperty(value)
          newSensor.addProperty(ssnHasOutput, vsModel.createResource(obsPropertyUri))
        } else {
          println("WARNING: There is no mapping for property " + value)
        }
      }
      // Write all statements from the temporary model to fuseki
      vsModel.listStatements().toList().foreach { s => 
        propertiesManager.addNewVirtualSensorStatement(s)
      }
  }
}

object PropertyMappingsManager {
  /**
   * Remove the first and last character of the provided String: it is needed for the parsing of JSON files
   * which return quotes with text
   */
  def removeQuotes(str:String):String = {
    if (str != null) str.replaceAll("\"","") else ""
  }
}