package gsn.discoveryagent.util

import gsn.discoveryagent.VsResult
import gsn.discoveryagent.DataDiscoveryRequest
import gsn.discoveryagent.Target
import gsn.discoveryagent.Point
import gsn.discoveryagent.Location
import scala.xml.Elem
import java.io.File

object DataDiscoveryRequestParser {
  
  def parse(sourceFile:File):(DataDiscoveryRequest,List[Target]) = {
    val xml = scala.xml.XML.loadFile(sourceFile)
    
    (extractDDR(xml),extractTargets(xml)) 
  }
  
  private def extractDDR(mainElem:Elem):DataDiscoveryRequest = {
    
    val ddrNodeSeq = mainElem \ "dataDiscoveryRequest"
    
    val requestId = (ddrNodeSeq \ "requestId").text
    val obsProperty = (ddrNodeSeq \ "observedProperty").text
    
    val location = {
      val locationNode = (mainElem \ "location")
      if (locationNode != null) {
        val leftDownCornerLongitude = (ddrNodeSeq \ "location" \ "leftDownCorner" \ "longitude").text.toDouble
        val leftDownCornerLatitude = (ddrNodeSeq \ "location" \ "leftDownCorner" \ "latitude").text.toDouble
        
        val rightUpCornerLongitude = (ddrNodeSeq \ "location" \ "rightUpCorner" \ "longitude").text.toDouble
        val rightUpCornerLatitude = (ddrNodeSeq \ "location" \ "rightUpCorner" \ "latitude").text.toDouble
        
        Option(new Location(
            new Point(leftDownCornerLatitude, leftDownCornerLongitude), 
            new Point(rightUpCornerLatitude, rightUpCornerLongitude)))
      } else {
        None
      }
    }
    
    new DataDiscoveryRequest(requestId.toInt, obsProperty, location)
  }
  
  private def extractTargets(mainElem:Elem):List[Target] = {
    
     val targetsNodeSeq = (mainElem \ "targets" \ "target").map { targetNode =>  
       val identifier = (targetNode \ "identifier").text
       val host = (targetNode \ "host").text
       val port = (targetNode \ "port").text.toInt
       
       new Target(identifier, host, port)
     }
     
    targetsNodeSeq.toList
  }
  
}