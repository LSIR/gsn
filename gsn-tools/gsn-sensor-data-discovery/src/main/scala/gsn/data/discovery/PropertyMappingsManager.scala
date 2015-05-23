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
import gsn.data.discovery.DataManager
import gsn.data.discovery.util.DataFormatter
import java.io.FileInputStream
import gsn.data.discovery.util.ReadResource
import play.api.libs.json.Json
import com.typesafe.config._
import scala.xml.Node
import scala.xml.NodeSeq
import play.api.libs.json.JsValue

class PropertyMappingsManager(dataManager:DataManager, baseUri:String) extends ReadResource {
  
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
      if (dataManager.observedPropertyExistsByUri(obsProperty)) {
        // In this case, obsProperty is already a URI
        dataManager.addNewMapping(propertyToMap, obsProperty)
      } else if (dataManager.observedPropertyExistsByLabel(obsProperty)) {
        // Find the URI of the first property found with the given label
        val propertyUri = dataManager.findObservedPropertyByLabelExactMatch(obsProperty.replaceAll("_", "\\s").toLowerCase())
        dataManager.addNewMapping(propertyToMap, propertyUri)
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
      val vsName = DataFormatter.removeQuotes((json \ "properties" \ "vs_name").as[String].toLowerCase())
      val fields = json \ "properties" \ "fields" \\ "name"
      val longitude = extractOptionalStringFromJsValue((json \ "properties" \ "longitude"))
      val latitude = extractOptionalStringFromJsValue((json \ "properties" \ "latitude"))
      val altitude = extractOptionalStringFromJsValue((json \ "properties" \ "altitude"))
      
      addVirtualSensor(vsName, longitude, latitude, altitude, fields.map { x => x.as[String] } toList)
  }
  
  private def extractOptionalStringFromJsValue(jsVal:JsValue):Option[String] = {
    if (jsVal != null) Option(DataFormatter.removeQuotes(jsVal.as[String])) else None
  }
  
  
  /**
   * Add the virtual sensor found in the provided XML file to the model
   */
  def addVirtualSensorFromXML(file:File) {
    
      val mainNode = xml.XML.loadFile(file)
      val vsName = (mainNode \ "@name").text.toLowerCase()
      val fields = (mainNode \ "processing-class" \ "output-structure" \ "field").map {_ \ "@name"}
      val longitude = extractOptionalStringFromNodeSeq((mainNode \ "addressing" \ "predicate"), "longitude")
      val latitude = extractOptionalStringFromNodeSeq((mainNode \ "addressing" \ "predicate"), "latitude")
      val altitude = extractOptionalStringFromNodeSeq((mainNode \ "addressing" \ "predicate"), "altitude")
      
      addVirtualSensor(vsName, longitude, latitude, altitude, fields.map { x => x.text } toList)
  }
  
  private def extractOptionalStringFromNodeSeq(nodeSeq:NodeSeq, property:String):Option[String] = {
    val seq = nodeSeq.filter { x => extractAttributeValue(x).equalsIgnoreCase(property) }
    if (seq.size >= 1) Option(seq.head.text) else None
  }
  
  private def extractAttributeValue(n:Node):String = {
    n.attribute("key") match {
      case s if s.size >= 1 => s.head.text
      case _ => null
    }
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
        
      val ssnHasOutput = vsModel.createProperty(ssnUri, "hasOutput")
      
      for (value <- fields) {
        if (dataManager.mappingExists(value)) {
          val obsPropertyUri = dataManager.getMappingForProperty(value)
          newSensor.addProperty(ssnHasOutput, vsModel.createResource(obsPropertyUri))
        } else {
          println("WARNING: There is no mapping for property " + value)
        }
      }
      // Write all statements from the temporary model to fuseki
      vsModel.listStatements().toList().foreach { s => 
        dataManager.addNewVirtualSensorStatement(s)
      }
  }
}

object PropertyMappingsManager {
  
}