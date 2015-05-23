package gsn.data.discovery.util

import gsn.data._

import scala.xml.XML
import scala.xml.Text
import scala.xml.Null
import scala.xml.Elem
import scala.xml.Attribute
import org.joda.time.format.ISODateTimeFormat
import gsn.discoveryagent.VsResult

object XmlSerializer {
  
  def serializeToFile(data:List[VsResult]) = {
    val vsName = "vs_n" //TODO: Find a way to generate a unique name
    scala.xml.XML.save(vsName, ser(data))
  }
  
  private def ser(data:List[VsResult]):Elem = {
    
    val (latitude, longitude, altitude) = computeMeanLocation(data)
    
    val xml =
      <virtual-sensor name="vs1" priority="10">
          <processing-class>
          <class-name>gsn.vsensor.BridgeVirtualSensor</class-name>
          <init-params/>
          <output-structure>
          	<field name="column_name" unit="unit" type="type"/> <!-- name, unit and type TO REPLACE -->
          </output-structure>
          </processing-class>
          <description>
          <!-- TODO -->
          </description>
          <life-cycle pool-size="10"/>
          <addressing>
          <predicate key="geographical"><!-- What to put here ? --></predicate>
          {
            if (longitude.isDefined) <predicate key="LONGITUDE">{longitude.get}</predicate>
            if (latitude.isDefined) <predicate key="LATITUDE">{latitude.get}</predicate>
            if (altitude.isDefined) <predicate key="ALTITUDE">{altitude.get}</predicate>
          }
          </addressing>
          <storage history-size="5m"/>
          <streams>
						{
            var i = 0
						data.foreach{d => 
          	<stream name={"source"+i}>
							<source name={"source"+i} storage-size="1" sampling-rate="1">
        				<address wrapper="remote-rest">
          				<predicate key="HOST">{d.host}</predicate>
          				<predicate key="PORT">{d.port}</predicate>
          				<predicate key="QUERY">select * from {d.vsName}</predicate><!-- ??????????????????' -->
        				</address>
        				<query>SELECT column_name, timed FROM wrapper</query> <!-- ??????????????????' -->
      				</source>
      				<query>select * from {d.vsName}</query><!-- ??????????????????' -->
          	</stream>
              i += 1
            }
            }
          </streams>
        </virtual-sensor>
    xml
  }
  
  private def computeMeanLocation(data:List[VsResult]):(Option[String], Option[String], Option[String]) = {
    
    def getOptionString(count:Integer, value:Double):Option[String] = if (count > 0) Option(value.toString()) else None
    
    var mLong, mLat, mAlt = 0.0
    var countLong = 0
    var countLat = 0
    var countAlt = 0
    data.foreach { d => 
      if (d.longitude.isDefined && d.latitude.isDefined) {
        mLong += d.longitude.get.toDouble ; countLong += 1
        mLat += d.latitude.get.toDouble ; countLat += 1
      }
      if (d.altitude.isDefined) mAlt += d.altitude.get.toDouble ; countAlt += 1
    }
    mLong = mLong / countLong
    mLat = mLat / countLat
    mAlt = mAlt / countAlt
    
    (getOptionString(countLong, mLong), getOptionString(countLat, mLat), getOptionString(countAlt, mAlt))
  }
}