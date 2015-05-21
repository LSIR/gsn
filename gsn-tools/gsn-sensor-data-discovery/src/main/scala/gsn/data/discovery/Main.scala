import com.typesafe.config._

import gsn.data.discovery._
import java.io.File

object Main extends App {
  
  //VsGenerator.generate("/home/michael/dev/semester_project/virtual_sensors/", 1000)
  
  require(args.length >= 2, help())
  
  val mode = args(0)
  val virtualSensorsFolder = new File(args(1));
  val mappingsFilePath = new File(args(1))
  
  val conf = ConfigFactory.load("application.conf")
  val dataMgr = new DataManager(
      conf.getString("fusekiEndpoints.properties"),
      conf.getString("fusekiEndpoints.mappings"),
      conf.getString("fusekiEndpoints.virtualSensors"),
      conf.getString("baseUri"))
  val propertyMappingsMgr = new PropertyMappingsManager(dataMgr, conf.getString("baseUri"))
    
  mode match {
    case "--add-new-property-mappings" =>
      propertyMappingsMgr.importPropertyMappingsFromCsv(mappingsFilePath)
    case "--add-new-virtual-sensors" => 
      val jsonFiles = virtualSensorsFolder.listFiles().filter(_.getName.endsWith(".json"))
      for (file <- jsonFiles) {
        propertyMappingsMgr.addVirtualSensorFromJSON(file)
      }
      
      val xmlFiles = virtualSensorsFolder.listFiles().filter(_.getName.endsWith(".xml"))
      for (file <- xmlFiles) {
        propertyMappingsMgr.addVirtualSensorFromXML(file)
      }
    case "--help" | "-h" | _ => help()
  }
  
  private def help() {
    println("Various arguments needs to be provided.")
    println("Operation modes:")
    println("--add-new-property-mappings csvFilePath: add new property mappings to mappings model on Fuseki")
    println("--add-new-virtual-sensors virtualSensorsFolderPath: add new virtual sensors to Fuseki")
  }
}