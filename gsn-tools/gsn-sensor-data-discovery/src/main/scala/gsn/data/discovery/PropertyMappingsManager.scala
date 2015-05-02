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

class PropertyMappingsManager(propertiesManager:PropertiesManager, baseUri:String) extends ReadResource {
  
  val ssnUri = "http://purl.oclc.org/NET/ssnx/ssn#"
  
  /**
   * Create new mappings in Fuseki
   * The CSV file has 2 columns:
   * - the first one contains the properties, like air_tmp
   * . the second on contains the observed properties URIs, like http://purl.oclc.org/NET/ssnx/cf/cf-property#air_temperature
   */
  def importMappingsFromCsv(file:File) {
    
    val mappings = Source.fromFile(file).getLines().map { line => line.split(",") }.filter { a => a.length >= 2 }
    
    mappings.foreach { m => 
      val propertyToMap:String = m(0).toLowerCase();
      val obsPropertyURI:String = m(1);
      
      // Create the mapping only for existing observed properties
      if (propertiesManager.observedPropertyExists(obsPropertyURI)) {
        propertiesManager.addNewMapping(propertyToMap, obsPropertyURI)
      } else {
        println("WARNING:(mapping) Property: " + obsPropertyURI + " doesn't exist in the model: ignored");
      }
    }
  }
  
  /**
   * Add the virtual sensor found in the provided JSON file to the model
   */
  def addVirtualSensorFromJSON(file:File) {
    
      val vsModel = ModelFactory.createDefaultModel()
      
      val json = Json.parse(Source.fromFile(file).mkString)
      val virtualSensorName = PropertyMappingsManager.removeQuotes((json \ "properties" \ "vs_name").as[String].toLowerCase())
      val fields =   json \ "properties" \ "fields" \\ "name"

      val newSensor = vsModel.createResource(baseUri + virtualSensorName)
      
      val ssnSensorType = vsModel.createResource(ssnUri + "Sensor")
      newSensor.addProperty(RDF.`type`, ssnSensorType)
      
      val namedIndividualType = vsModel.createResource(OWL.getURI() + "NamedIndividual")
      newSensor.addProperty(RDF.`type`, namedIndividualType)
      
      val ssnObserves = vsModel.createProperty(ssnUri, "observes")
      val ssnHasOutput = vsModel.createProperty(ssnUri, "hasOutput")
      
      for (value <- fields) {
        if (propertiesManager.mappingExists(value.as[String])) {
          val obsPropertyUri = propertiesManager.getMappingForProperty(value.as[String])
          newSensor.addProperty(ssnHasOutput, vsModel.createResource(obsPropertyUri))
        } else {
          println("WARNING: There is no mapping for property " + value.as[String] + ": ignored")
        }
      }
    
      // Write new statements to database...
      vsModel.listStatements().toList().foreach { s => 
      propertiesManager.addNewVirtualSensorStatement(s)
    }
  }
  
  /**
   * Add the virtual sensor found in the provided XML file to the model
   */
  def addVirtualSensorFromXML(file:File) {
    
      val vsModel = ModelFactory.createDefaultModel()
      val mainNode = xml.XML.loadFile(file)
      val virtualSensorName = (mainNode \ "@name").text.toLowerCase()
      val fields = (mainNode \ "processing-class" \ "output-structure" \ "field").map {_ \ "@name"}
      
      val newSensor = vsModel.createResource(baseUri + virtualSensorName)
      
      val ssnSensorType = vsModel.createResource(ssnUri + "Sensor")
      newSensor.addProperty(RDF.`type`, ssnSensorType)
      
      val namedIndividualType = vsModel.createResource(OWL.getURI() + "NamedIndividual")
      newSensor.addProperty(RDF.`type`, namedIndividualType)
      
      val ssnObserves = vsModel.createProperty(ssnUri, "observes")
      val ssnHasOutput = vsModel.createProperty(ssnUri, "hasOutput")
      
      for (value <- fields) {
        if (propertiesManager.mappingExists(value.text)) {
          val obsPropertyUri = propertiesManager.getMappingForProperty(value.text)
          newSensor.addProperty(ssnHasOutput, vsModel.createResource(obsPropertyUri))
        } else {
          val log = "WARNING: There is no mapping for property " + value.text
          println(log)
        }
      }
      // Write new statements to database...
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
    str.substring(1, str.length()-1)
  }
}