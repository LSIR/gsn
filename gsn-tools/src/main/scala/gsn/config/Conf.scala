package gsn.config

import scala.xml._
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
  lazy val defaultZmq=ZmqConf(
    zmq.getBoolean("enabled"),zmq.getInt("proxyPort"),zmq.getInt("metaPort"))  

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

  lazy val defaultFuseki=defaults.getConfig("fuseki")
  lazy val defaultGsnServiceProxyUrl=defaults.getConfig("gsnServiceProxyUrl")
}
