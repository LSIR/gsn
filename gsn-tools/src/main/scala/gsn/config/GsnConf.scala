package gsn.config

import xml._
import com.typesafe.config.ConfigFactory


case class GsnConf(monitorPort:Int,timeFormat:String, 
    zmqConf:ZmqConf,
    storageConf:StorageConf,slidingConf:Option[StorageConf])
    
object GsnConf extends Conf {
  def create(xml:Elem)=GsnConf(
    takeInt(xml \ "monitor-port").getOrElse(defaultGsn.monitorPort ),    
    take(xml \ "time-format").getOrElse(defaultGsn.timeFormat ),
    ZmqConf.create(xml),
    StorageConf.create((xml \ "storage").head),
    (xml \ "sliding").headOption.map(a=>StorageConf.create((a \ "storage").head))
  )
  def load(path:String)=create(XML.load(path))
}
 
case class ZmqConf(enabled:Boolean,proxyPort:Int,metaPort:Int) 
object ZmqConf extends Conf{
  def create(xml:scala.xml.Elem)=ZmqConf(
    takeBool(xml \ "zmq-enable").getOrElse(defaultZmq.enabled),
    takeInt(xml \ "zmqproxy").getOrElse(defaultZmq .proxyPort),
    takeInt(xml \ "zmqmeta").getOrElse(defaultZmq.metaPort ) )  
}

case class StorageConf(driver:String,url:String,
    user:String,pass:String,identifier:Option[String]) 
object StorageConf extends Conf{
  def create(xml:Node)=StorageConf(
    xml \@ "driver",
    xml \@ "url",
    xml \@ "user",
    xml \@ "password",
    xml.attribute("identifier").map(_.toString))  
}

