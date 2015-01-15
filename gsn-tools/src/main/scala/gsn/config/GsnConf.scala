package gsn.config

import xml._
import com.typesafe.config.ConfigFactory

trait Conf{
  val defaults=ConfigFactory.load.getConfig("gsn")
  def take(seq:NodeSeq)=seq.headOption.map(_.text)
  def takeBool(seq:NodeSeq)=seq.headOption.map(_.text.toBoolean)
  def takeInt(seq:NodeSeq)=seq.headOption.map(_.text.toInt)
  def attBool(e:Elem,att:String,default:Boolean)=
     e.attribute(att).headOption.map(_.toString.toBoolean).getOrElse(default)
  def attInt(e:Node,att:String,default:Int)=
     e.attribute(att).headOption.map(_.toString.toInt).getOrElse(default)
  def att(e:Elem,att:String,default:String)=
     e.attribute(att).headOption.map(_.toString).getOrElse(default)
  
  
  lazy val zmq=defaults.getConfig("zmq")
  lazy val defaultZmq=ZmqConf(zmq.getBoolean("enabled"),
      zmq.getInt("proxyPort"),zmq.getInt("metaPort"))  

  lazy val ac=defaults.getConfig("ac")
  lazy val defaultAc=AcConf(ac.getBoolean("enabled"),ac.getInt("sslPort"),
      ac.getString("sslKeyStorePass"),ac.getString("sslKeyPass"))

  lazy val storage=defaults.getConfig("storage")
  lazy val defaultStorage=StorageConf(storage.getString("driver"),storage.getString("url"),
      storage.getString("user"),storage.getString("password"),None)

  lazy val defaultGsn=GsnConf(defaults.getString("name"),defaults.getString("author"),
      defaults.getString("description"),defaults.getString("email"),
      defaults.getInt("port"),defaults.getString("timeFormat"),
      defaultZmq,defaultAc,defaultStorage,None)
}

case class GsnConf(name:String,author:String,description:String,
    email:String,port:Int,timeFormat:String, 
    zmqConf:ZmqConf,accessControl:AcConf,storageConf:StorageConf,slidingConf:Option[StorageConf]) 
object GsnConf extends Conf {
  def create(xml:Elem)=GsnConf(
       take(xml \ "name").getOrElse(defaultGsn.name ),
    take(xml \ "author").getOrElse(defaultGsn.author ),
    take(xml \ "description").getOrElse(defaultGsn.description ),
    take(xml \ "email").getOrElse(defaultGsn.email ),
    takeInt(xml \ "port").getOrElse(defaultGsn.port ),    
    take(xml \ "time-format").getOrElse(defaultGsn.timeFormat ),
    ZmqConf.create(xml),AcConf.create(xml),
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

case class AcConf(enabled:Boolean,sslPort:Int,sslKeyStorePass:String,sslKeyPass:String) 
object AcConf extends Conf{
  def create(xml:Elem)=AcConf(
    takeBool(xml \ "access-control").getOrElse(defaultAc.enabled ),
    takeInt(xml \ "ssl-port").getOrElse(defaultAc.sslPort),
    take(xml \ "ssl-key-store-password").getOrElse(defaultAc.sslKeyStorePass),
    take(xml \ "ssl-key-password").getOrElse(defaultAc.sslKeyPass))    
}

case class StorageConf(driver:String,url:String,user:String,pass:String,
    identifier:Option[String]) 
object StorageConf extends Conf{
  def create(xml:Node)=StorageConf(
    xml \@ "driver",xml \@ "url",xml \@ "user",xml \@ "password",
    xml.attribute("identifier").map(_.toString))  
}

